package tipl.ij.scripting

import ij.{IJ, ImagePlus, ImageStack}
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{BytesWritable, NullWritable}
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapreduce.lib.input.{FileInputFormat => NewFileInputFormat}
import org.apache.hadoop.mapreduce.{InputFormat => NewInputFormat, Job => NewHadoopJob}
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.{OldBinaryFileRDD, RDD}
import tipl.blocks.ParameterSweep
import tipl.formats.TImgRO
import tipl.ij.Spiji
import tipl.ij.scripting.ImagePlusIO.{ImagePlusFileInputFormat, PortableImagePlus}
import tipl.spark.DSImg
import tipl.spark.hadoop.ByteOutputFormat
import tipl.util.TImgSlice.TImgSliceAsTImg
import tipl.util.{D3int, TImgSlice, TImgTools}

import scala.reflect.ClassTag

class rddImage extends ImageStack {

}

/**
 * Tools for making previewing and exploring data in FIJI from Spark easy
 * @author mader
 *
 */
object scOps {
  import tipl.spark.IOOps._

  def StartFiji(ijPath: String, show: Boolean = false): Unit = {
    Spiji.start(ijPath, show)
    Spiji.startRecording()
  }

  /**
   * A class which hangs around and keeps all of the imagej settings (so they can be sent to
   * workers)
   * @param showGui
   * @param ijPath
   */
  case class ImageJSettings(ijPath: String, showGui: Boolean = false) extends Serializable
  def SetuImageJInPartition(ijs: ImageJSettings): Unit = StartFiji(ijs.ijPath,ijs.showGui)

  def loadImages(path: String, partitions: Int)(implicit sc: SparkContext) = {

    val ioData = sc.binaryFiles(path,partitions)
    val sliceNames = ioData.map(_._1).collect()
    // get the names and partition before the files are copy and read (since this is expensive
    // as is moving around large serialized objects
      ioData.partitionBy(DSImg.NamedSlicePartitioner(sliceNames,partitions)).
      map{
      case (pname, pdsObj) =>
        (pname,new PortableImagePlus(Spiji.loadImageFromInputStream(pdsObj.open(),
          pname.split("[.]").last)))
    }
  }

  // macro related utilities
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
   * An ImageJ based RDD object
   * @param inRDD the RDD containing the images
   * @tparam A the type of the key (usually string or path, can also contain identifiers)
   * @tparam B portableimageplus or a subclass
   */
  implicit class imageJRDD[A : ClassTag,B <: PortableImagePlus : ClassTag](inRDD: RDD[(A,B)]) {
    /**
     * Runs the given command and arguments on all of the images in a list
     * @param cmd the imagej macro-style command to run (example "Add Noise")
     * @param args the arguments for the macro-style command (example "radius=3")
     * @return the updated images (lazy evaluated)
     */
    def runAll(cmd: String, args: String = "")(implicit fs: ImageJSettings) = {
      inRDD.mapPartitions{
        kvlist =>
          SetuImageJInPartition(fs)
          kvlist.map(kv => (kv._1,kv._2.run(cmd,args)))
      }
    }

    /**
     * Runs a range of different parameters on each image
     * @param cmd the command to run (example "Median...")
     * @param startingArgs starting argument (example "radius=1"
     * @param endingArgs ending argument (example "radius=5")
     * @param steps number of steps between
     * @return updated list of images with appended paths
     */
    def runRange(cmd: String, startingArgs: String, endingArgs: String,steps: Int = 5 )(
      implicit fs: ImageJSettings) = {
      val swSteps = ParameterSweep.ImageJSweep.ImageJMacroStepsToSweep(
        Array(startingArgs,endingArgs),
        delim=" "
      )
      val swPath = ParameterSweep.ImageJSweep.SweepToPath(swSteps,false,delim=" ")
      println("Running Parameter Sweep on =>"+swSteps.mkString("\n\t"))
      val swArgsPath = swSteps.zip(swPath)
      inRDD.mapPartitions{
        kvlist =>
          SetuImageJInPartition(fs)
          kvlist.flatMap{
            case (basepath,imgobj) =>
              for((args,pathsuffix) <- swArgsPath) yield (basepath+pathsuffix,imgobj.run(cmd,args))
          }
      }
    }

    /**
     * Saves the images locally using ImageJ
     * @param suffix text to add to the existing path
     */
    def saveImagesLocal(suffix: String)(implicit fs: ImageJSettings) = {
      inRDD.foreachPartition{
        imglist =>
          SetuImageJInPartition(fs)
          imglist.foreach {
            case(filename,imgobj) =>
              Spiji.saveImage(imgobj.getImg,filename+suffix)
          }
      }
    }

    /**
     * Save images in a hadoop style using a directory with subnames (part-0000 ...),
     * @note these files have no extension so the filenames/ metadata should be stored elsewhere
     *       @note it is recommended to use saveImageLocal for most cases
     * @param path the folder to write the images too
     * @param newSuffix the suffix (for writing the correct filetype
     */
    def saveImages(path: String,newSuffix: String)(implicit fs: ImageJSettings) = {
      val format = classOf[ByteOutputFormat[NullWritable, BytesWritable]]
      val jobConf = new JobConf(inRDD.context.hadoopConfiguration)
      val namelist = inRDD.keys.collect
      inRDD.partitionBy(new DSImg.NamedSlicePartitioner(namelist)).
        mapPartitions {
        imglist =>
          SetuImageJInPartition(fs)
          imglist.map {
            case (keyobj, imgobj) =>
              (
                NullWritable.get(),
              new BytesWritable(Spiji.saveImageAsByteArray(imgobj.getImg(),newSuffix))
                )
          }
      }.saveAsHadoopFile(path, classOf[NullWritable], classOf[BytesWritable], format)
    }

  }


  /**
   * Add imageplus reader to the sparkcontext
   */
  implicit class ImageJFriendlySparkContext(sc: SparkContext) {

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
