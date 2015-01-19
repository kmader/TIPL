package tipl.streaming

import java.awt.Dimension
import java.awt.image.BufferedImage
import javax.swing.{JFrame, JPanel}

import com.github.sarxos.webcam.{Webcam, WebcamPanel}
import ij.gui.{Plot, PlotWindow}
import ij.{ImagePlus, WindowManager}
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.Seconds
import tipl.ij.Spiji
import tipl.ij.scripting.ImagePlusIO.PortableImagePlus
import tipl.spark.SparkGlobal
import tipl.streaming.LiveImagePanel.BISourceInformation

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ArrayBuffer

/**
 * Created by mader on 1/17/15.
 */
object StreamingWebcam {

  import org.apache.spark.streaming.receiver.Receiver
  /**
   * Generic receiver device with multiple reading threads
   */
  abstract case class PortableImagePlusReceiver[A](sl: StorageLevel,delay: Int = 50,
                                                    ithreads: Int = 1)
    extends Receiver[(A,PortableImagePlus)](sl) {
    var streamingThread: ArrayBuffer[Thread] = new ArrayBuffer[Thread]()


    override def onStart(): Unit = {
      startImageSource()
      for(i<- 1 to ithreads) streamingThread+=createImageThread()
      streamingThread.map(_.start())
    }

    def addThread() = {
      val t = createImageThread()
      t.start()
      streamingThread+=t
    }

    override def onStop(): Unit = {
      streamingThread.map(_.stop())
      stopImageSource()
    }
    private def createImageThread() = {
      val pipObj = this
      new Thread() {
        def wrapImage(bi: BufferedImage) = new PortableImagePlus(new ImagePlus(bi.toString(),bi))
        override def run(): Unit = {
          while(true) {
            val (key,biImg) = getImageBuffer()
            pipObj.store((key,wrapImage(biImg)))
            Thread.sleep(pipObj.delay)
          }
        }
      }
    }
    def startImageSource(): Unit
    def stopImageSource(): Unit
    def getImageBuffer(): (A,BufferedImage)
  }

  class PanelFlasher(ip: JPanel) extends Thread {
    val parent = ip.getParent()
    override def run(): Unit = {
      parent match {
        case jf: JFrame =>
          jf.setTitle("Reading")
          Thread.sleep(50)
          jf.setTitle("Waiting")
        case _ => "Nothing"
      }

    }
  }

  class WebcamReceiver(storage: StorageLevel, delay: Int = 50, nthreads: Int = 1,
                        showPanel: Boolean = true) extends
  PortableImagePlusReceiver[Long](storage,delay,nthreads){
    lazy val webcam = Webcam.getDefault()
    lazy val wcPanel = getPanel()
    def startImageSource(): Unit = {
      webcam.open();
      if(showPanel) {
        val window = new JFrame("Input Webcam Panel");
        window.add(wcPanel)
        window.setResizable(true)
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        window.pack()
        window.setVisible(true)
      }
    }
    def stopImageSource(): Unit = {webcam.close()}
    def getImageBuffer() = {
      val out = (System.currentTimeMillis(),webcam.getImage)
      if(showPanel) new PanelFlasher(wcPanel).start()
      out
    }
    def getPanel() = {
      val panel = new WebcamPanel(webcam)
      panel.setFPSDisplayed(true)
      panel.setDisplayDebugInfo(true)
      //panel.setImageSizeDisplayed(true)
      panel.setMirrored(true)
      panel
    }
  }



  def showPanel() = {
    val window = new JFrame("Test webcam panel");

    val biSrcInfo = new BISourceInformation {
      override def getDimension: Dimension = new Dimension(640,480)
      override def isReady: Boolean = true
      override def name(): String = "Preview"
    }
    val prevPanel = new LiveImagePanel(biSrcInfo)
    window.add(prevPanel)

    val tdObj: TriggerDraw = prevPanel
    // so it isnt empty
    tdObj.drawImageTrigger(
      ij.IJ.createImage("hye",640,480,1,8).getBufferedImage
    )

    window.setResizable(true)
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    window.pack()
    window.setVisible(true)
    //webpanel.stop
    (window,tdObj)
  }
  def showResults(title: String, inRDD: RDD[(Double,PortableImagePlus)]): Unit =
    showResultsStr(title,inRDD.map(iv => ("Capture:"+iv._1+"s",iv._2)))

  def showResultsStr(title: String, inRDD: RDD[(String,PortableImagePlus)]): Unit = {
    inRDD.collect.foreach {
      case (ctime,img) =>
        Spiji.getListImages.contains(title) match {
          case true =>
            val curImg = WindowManager.getImage(title)
            val cs = curImg.getStack
            cs.addSlice(ctime,img.getImg.getProcessor)
            curImg.setStack(cs)
            curImg.setSlice(curImg.getStackSize())
            curImg.repaintWindow()
            //curImg.show("Updated")
          case false =>
            val tempImg = img.getImg.duplicate
            tempImg.setTitle(title)
            tempImg.show()
        }
    }

  }
  def main(args: Array[String]): Unit = {
    import org.apache.spark.streaming.StreamingContext._
    //import org.apache.spark.streaming.StreamingContext._
    //import org.apache.spark.SparkContext._
    import org.apache.spark.SparkContext._
    import tipl.spark.IOOps._
    val p = SparkGlobal.activeParser(args)
    val wrThreads = p.getOptionInt("webthreads",1,"Number of webcam threads to use",1,20)
    val wrDelay = p.getOptionInt("webdelay",500,"Delay between reading images")
    val strTime = p.getOptionInt("streamtime",6,"Number of seconds to group together",1,100)
    val showEdges = false
    val showMedian = true
    val showThreshold = false
    p.checkForInvalid()

    val wr = new WebcamReceiver(StorageLevel.MEMORY_ONLY,wrDelay,wrThreads)
    val sc = SparkGlobal.getContext("StreamingWebcamDemo").sc
    val ssc = sc.toStreaming(strTime)

    val imgList = ssc.receiverStream(wr)
    val startTime = System.currentTimeMillis()


    val allImgs = imgList.map{
      case (systime,img) =>
        val ntime = (systime-startTime)/1000.0
          val rtime = "CS %2.1f s".format(ntime)
          val imp = img.getImg
          val ip = imp.getProcessor()
          ip.drawString(rtime,30,0)
        (ntime,new PortableImagePlus(ip))
    }

    val filtImgs = allImgs.mapValues(_.run("Median...","radius=3"))
    val edgeImgs = filtImgs.mapValues(_.run("Find Edges"))

    val eventImages = filtImgs.//window(Seconds(12),Seconds(4)).
      transform{
      inImages =>
        val totImgs = inImages.count()
        val bgImage = inImages.values.reduce(_.average(_,1.0f)).multiply(1.0/totImgs)
        val corImage = inImages.map {
          case (inTime,inImage) =>
            val corImage = inImage.subtract(bgImage)
            (corImage.getImageStatistics().mean,(inTime,corImage))
        }
        corImage
    }

    // apply a threshold to the images
    val threshImgs = edgeImgs.map(kv => (("edges",kv._1),kv._2)).
      union(filtImgs.map(kv => (("median",kv._1),kv._2))).
      mapValues {
      cImg => cImg.
        run("applyThreshold", "lower=100 upper=255").run("8-bit")
    }.map(kv => (kv._1._1,(kv._1._2,kv._2.getImageStatistics().mean/255*100))).
      groupByKeyAndWindow(Seconds(strTime*3))


    val pwMap = new TrieMap[String,PlotWindow]()
    val xyMap = new TrieMap[String,Seq[(Double,Double)]]()

    threshImgs.foreachRDD{
      curRDD =>
        curRDD.collect().foreach{
          case (pname,newpdata) =>

            val pdata = (xyMap.getOrElse(pname,Seq.empty[(Double,Double)]) ++
              newpdata).toSeq.sortBy(_._1)
            xyMap.put(pname,pdata)

            val xd = pdata.map(_._1).toArray
            val yd = pdata.map(_._2).toArray
            val np = new Plot(pname,"Time (s)","Segmented (%)", xd,yd)

            np.addPoints(xd,yd,Plot.CIRCLE)
            np.setLimits(0,xd.max,0,100)
            val pwin = pwMap.getOrElseUpdate(pname,np.show())
            pwin.drawPlot(np)
        }
    }

    // outlier detection
    val epwMap = new TrieMap[String,PlotWindow]()
    val exyMap = new TrieMap[String,Seq[(Double,Double)]]()
    val ename = "Outlier Detection Scores"
    eventImages.map(ikv => (ikv._2._1,ikv._1)).foreachRDD {
      curRDD =>
        val newpdata = curRDD.collect()
        val pdata = (exyMap.getOrElse(ename, Seq.empty[(Double, Double)]) ++
          newpdata).toSeq.sortBy(_._1)
        exyMap.put(ename, pdata)

        val xd = pdata.map(_._1).toArray
        val yd = pdata.map(_._2).toArray
        val np = new Plot(ename, "Time (s)", "Event Value", xd, yd)

        np.addPoints(xd, yd, Plot.CIRCLE)
        val pwin = epwMap.getOrElseUpdate(ename, np.show())
        pwin.drawPlot(np)
    }

    if (showMedian) filtImgs.foreachRDD(showResults("median_filtered",_))
    filtImgs.map(_._2).reduce(_.average(_)). // reduceByWindow(,Seconds(strTime*3),Seconds
    // (strTime*2)
      map(img => (img.toString(),img)).foreachRDD(showResultsStr("time_filtered",_))

    eventImages.filter(iv => Math.abs(iv._1)>20).map(iv => ("S:%2.0f".format(iv._1)+
      ",T:%2.0f".format(iv._2._1), iv._2._2)).
      foreachRDD(showResultsStr("outlier",_))
    if (showEdges) edgeImgs.foreachRDD(showResults("find_edges",_))

    ssc.start()
    ssc.awaitTermination()

  }
}


