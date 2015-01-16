package tipl.ij.scripting

import ij.{IJ, ImagePlus, ImageStack}
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.lib.input.{FileInputFormat => NewFileInputFormat}
import org.apache.hadoop.mapreduce.{InputFormat => NewInputFormat, Job => NewHadoopJob}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.{OldBinaryFileRDD, RDD}
import tipl.formats.TImgRO
import tipl.ij.Spiji
import tipl.ij.scripting.ImagePlusIO.{ImagePlusFileInputFormat, PortableImagePlus}
import tipl.util.TImgSlice.TImgSliceAsTImg
import tipl.util.{D3int, TImgSlice, TImgTools}

class rddImage extends ImageStack {

}

/**
 * Tools for making previewing and exploring data in FIJI from Spark easy
 * @author mader
 *
 */
object scOps {

  def StartFiji() = {
    Spiji.start()
    Spiji.startRecording()

  }

  def getRecentCommand() = {
    println("Only run commands are current supported")
    Spiji.getCommands().filter(_.contains("run")).last
  }

  def getLastCommand() = {
    Spiji.getLastCommand().split("\n").filter(_.contains("run"))
  }
  def getLastCommandAsSweepInput() = {
    val lc = getLastCommand()
    lc.map{
      cmd =>
        val fxStr = cmd.replace(");","").replace("run(","").replace("\"","").split(",")
        (fxStr.head,fxStr.tail.mkString(","))
    }.map{
      case(cmd,args) => args.trim().split(" ").map(cmd+":"+_).reduce(_ + " " + _)
    }.reduce(_ + "_" + _)

  }

  /**
   * Add imageplus reader to the sparkcontext
   */
  implicit class ImageJFriendlySparkContext(sc: SparkContext) {
    import tipl.spark.IOOps._
    val defMinPart = sc.defaultMinPartitions

    def ijByteFile(path: String, minPartitions: Int = sc.defaultMinPartitions): RDD[(String,
      PortableImagePlus)] = {
      val job = new NewHadoopJob(sc.hadoopConfiguration)
      NewFileInputFormat.addInputPath(job, new Path(path))
      val updateConf = job.getConfiguration
      new OldBinaryFileRDD(
        sc,
        classOf[ImagePlusFileInputFormat],
        classOf[String],
        classOf[PortableImagePlus],
        updateConf,
        minPartitions).setName(path)
    }

    def ijFile(path: String, minPartitions: Int = sc.defaultMinPartitions): RDD[(String,
      PortableImagePlus)] = {
      sc.binaryFiles(path,minPartitions).map{
        case (pname, pdsObj) =>
          (pname,new PortableImagePlus(Spiji.loadImageFromInputStream(pdsObj.open(),
            pname.split("[.]").last)))
      }
    }

  }

  implicit def ImagePlusToPortableImagePlus(imp: ImagePlus) = new PortableImagePlus(imp)



  implicit class ijConvertableBlock[T](inBlock: TImgSlice[T]) {
    def asTImg() = {
      new TImgSliceAsTImg(inBlock)
    }
  }

  implicit class rddImage[V](inImg: RDD[(D3int,TImgSlice[V])]) {
    def show() = {

    }
  }

  /**
   * Read the image as a TImg file
   */
  def OpenImg(path: String): TImgRO = TImgTools.ReadTImg(path)
  def GetCurrentImage() = {
    val curImage = IJ.getImage()
    tipl.ij.ImageStackToTImg.FromImagePlus(curImage)
  }



}
