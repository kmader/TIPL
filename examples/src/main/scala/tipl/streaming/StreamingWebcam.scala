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

  import org.apache.spark.streaming.receiver.Receiver;
  /**
   * Streaming Code
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
      parent.setName("Reading")
      Thread.sleep(50)
      parent.setName("Waiting")
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
      panel.setImageSizeDisplayed(true)
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

  def showResults(title: String, inRDD: RDD[(Double,PortableImagePlus)]): Unit = {
    inRDD.collect.foreach {
      case (ctime,img) =>
        Spiji.getListImages.contains(title) match {
          case true =>
            val curImg = WindowManager.getImage(title)
            curImg.getStack.addSlice("Capture:"+ctime+"s",img.getImg.getProcessor)
            curImg.setSlice(curImg.getStackSize())
            curImg.repaintWindow()
            //curImg.show("Updated")
          case false =>
            val tempImg = img.getImg
            tempImg.setTitle(title)
            tempImg.show()
        }
    }

  }
  def main(args: Array[String]): Unit = {
    import org.apache.spark.streaming.StreamingContext._
    import tipl.spark.IOOps._
    val p = SparkGlobal.activeParser(args)
    p.checkForInvalid()

    val wr = new WebcamReceiver(StorageLevel.MEMORY_ONLY,100)
    val sc = SparkGlobal.getContext("StreamingWebcam").sc
    val ssc = sc.toStreaming(3)

    val imgList = ssc.receiverStream(wr)
    val startTime = System.currentTimeMillis()

    //val (webWind,tdObj) = showPanel()


    val allImgs = imgList.map{
      case (systime,img) =>
        //val stats = img.getImageStatistics()
        val ntime = (systime-startTime)/1000.0
          val rtime = "CS %2.1f s".format(ntime)
          val imp = img.getImg
          val ip = imp.getProcessor()
          ip.drawString(rtime,30,0)
        imp.setProcessor(ip)
        (ntime,new PortableImagePlus(imp))
    }

    val filtImgs = allImgs.mapValues(_.run("Median...","radius=3"))
    val noisyImgs = allImgs.mapValues(_.run("Add Noise"))

    val threshImgs = noisyImgs.map(kv => (("noisy",kv._1),kv._2)).
        union(filtImgs.map(kv => (("median",kv._1),kv._2))).mapValues {
      cImg => cImg.
        run("applyThreshold", "lower=0 upper=100").run("8-bit")
    }.map(kv => (kv._1._1,(kv._1._2,kv._2.getImageStatistics().mean/255*100))).
      groupByKeyAndWindow(Seconds(6))

    val pwMap = new TrieMap[String,PlotWindow]()
    val xyMap = new TrieMap[String,Seq[(Double,Double)]]()

    threshImgs.foreachRDD{
      curRDD =>
        curRDD.collect().foreach{
          case (pname,newpdata) =>
            val odata = xyMap.getOrElse(pname,Seq.empty[(Double,Double)])
            val pdata = (odata ++ newpdata).toSeq.sortBy(_._1)
            xyMap.put(pname,pdata)

            val xd = pdata.map(_._1).toArray
            val yd = pdata.map(_._2).toArray
            val np = new Plot(pname,"Time (s)","Segmented (%)", xd,yd)

            np.addPoints(xd,yd,Plot.CIRCLE)
            np.setLimits(0,xd.max,0,100)
            val pwin = pwMap.getOrElseUpdate(pname,np.show())
            pwin.drawPlot(np)
            //pwin.addPoints(xd,yd,PlotWindow.BOX)
            //pwin.setLimits(0,xd.max,0,100)
        }
    }

    //threshImgs.foreachRDD(showResults("combined_threshold",_))
    filtImgs.foreachRDD(showResults("median_filtered",_))
    noisyImgs.foreachRDD(showResults("added_noise",_))
    ssc.start()

    ssc.awaitTermination()

  }
}


