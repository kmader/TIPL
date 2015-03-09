package tipl.ij.scripting

import _root_.tipl.blocks.ParameterSweep
import _root_.tipl.formats.TImgRO
import _root_.tipl.spark.DSImg
import _root_.tipl.util.TImgSlice.TImgSliceAsTImg
import _root_.tipl.util.{D3int, TIPLGlobal, TImgSlice, TImgTools}
import fourquant.imagej.ImagePlusIO.{ImageLog, LogEntry, PortableImagePlus}
import fourquant.imagej.Spiji
import ij.{IJ, ImagePlus}
import org.apache.hadoop.mapreduce.lib.input.{FileInputFormat => NewFileInputFormat}
import org.apache.hadoop.mapreduce.{InputFormat => NewInputFormat, Job => NewHadoopJob}
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag


trait FijiInit {
  def setupSpark(sc: SparkContext): Unit
  def setupFiji(): Unit
}

/**
 * Tools for making previewing and exploring data in FIJI from Spark easy
 * @author mader
 *
 */
object scOps {

  def StartFiji(ijPath: String, show: Boolean = false,
                 runLaunch: Boolean = true, record: Boolean = true): Unit = {
    Spiji.start(ijPath, show,runLaunch)
    if(record) Spiji.startRecording()
  }

  val forceHeadless = false


  /**
   * A class which hangs around and keeps all of the imagej settings (so they can be sent to
   * workers)
   * @param showGui
   * @param ijPath
   */
  case class ImageJSettings(ijPath: String,
                            showGui: Boolean = false,
                           runLaunch: Boolean = true,
                           record: Boolean = false
                             ) extends FijiInit {
    override def setupSpark(sc: SparkContext) = {
      if(!showGui) {
        if (forceHeadless) TIPLGlobal.forceHeadless();
        sc.setLocalProperty("java.awt.headless","false")
      }
    }
    override def setupFiji() = {
      if(!showGui) {
        if (forceHeadless) TIPLGlobal.forceHeadless();
        System.setProperty("java.awt.headless","false")
      }
      StartFiji(ijPath,showGui,runLaunch,record)
    }
  }


  def SetupImageJInPartition(ijs: ImageJSettings): Unit = ijs.setupFiji


  def loadImages(path: String, partitions: Int)(implicit sc: SparkContext,
                                                ijs: ImageJSettings) = {
    sc.loadImages(path,partitions)
  }

  /**
   * Push the list of images from the driver machine to the Spark Cluster (only if they are not
   * available on the cluster / shared file system
   * @param paths
   * @param ijs imagej setup information
   * @param parallel load images on driver machine in parallel (much faster)
   * @return a list of images in an RDD
   */
  def pushImages(paths: Array[String],partitions: Int, parallel: Boolean = true)(
    implicit sc: SparkContext,ijs: ImageJSettings) =
    sc.loadImagesDriver(paths,partitions,parallel=parallel)

  implicit class imageJSparkContext(sc: SparkContext) {
    def createEmptyImages[A : ClassTag](prefix: String, imgs: Int, width: Int, height: Int,
                          indFun: (Int) => A, ijs: Option[ImageJSettings] = None) = {
      // set everything up correctly if it is present
      ijs.map(_.setupSpark(sc))
      sc.
        parallelize(0 until imgs).
        map(
          i => (prefix+i.toString,
          new PortableImagePlus(Array.fill[A](width, height)(indFun(i))))
      )
    }

    def loadImages(path: String, partitions: Int)(implicit ijs: ImageJSettings) = {
      ijs.setupSpark(sc)
      val ioData = sc.binaryFiles(path,partitions)
      val sliceNames = ioData.map(_._1).collect()
      // get the names and partition before the files are copy and read (since this is expensive
      // as is moving around large serialized objects

      ioData.partitionBy(DSImg.NamedSlicePartitioner(sliceNames,partitions)).
        mapPartitions{
        partList =>
          SetupImageJInPartition(ijs)
          partList.map {
            case (pname, pdsObj) =>
              (pname, new PortableImagePlus(Spiji.loadImageFromInputStream(pdsObj.open(),
                pname.split("[.]").last),
                new ImageLog(LogEntry.loadImages("loadImages",pname))
              )
                )
          }
      }
    }

    /**
     * to load images available on a local (shared with all workers) filesystem
     * @param paths the list of paths to open
     * @param partitions desired partition count
     * @param ijs settings for configuring imagej
     * @return imageplus rdd
     */
    def loadImagesLocally(paths: Array[String],partitions: Int)(implicit ijs: ImageJSettings) = {
      sc.parallelize(paths).map(p => (p,"placeholder")).
        partitionBy(DSImg.NamedSlicePartitioner(paths,partitions)).
        mapPartitions{
        partList =>
          SetupImageJInPartition(ijs)
          partList.map {
            case (pname, pdsObj) =>
              (pname, new PortableImagePlus(
                Spiji.loadImageFromPath(pname),
                new ImageLog(LogEntry.loadImages("loadImagesLocally",pname))
              )
                )
          }
      }
    }

    /**
     * load images which are only available on the driver machine to the cluster (useful in cloud
     * situtations where much of the data is located locally)
     * @param paths list of files to load
     * @param partitions number of partitions of the data on the cluster
     * @param parallel use a parallel array map implementation to load with multiple threads on
     *                 the driver (might be unsafe for some filetypes
     * @return imageplus rdd
     */
    def loadImagesDriver(paths: Array[String],partitions: Int, parallel: Boolean = true)(
      implicit ijs: ImageJSettings) = {
      SetupImageJInPartition(ijs)
      val loadFcn = (cpath: String) => (cpath,
        new PortableImagePlus(Spiji.loadImageFromPath(cpath),
          new ImageLog(LogEntry.loadImages("loadImagesDriver",cpath))
        )
        )
      val pathList = if (parallel) {
        paths.par.map(loadFcn).toArray
      } else {
        paths.map(loadFcn)
      }
      sc.parallelize(
        pathList
      ).partitionBy(DSImg.NamedSlicePartitioner(paths,partitions))
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
   * Just the operations on the imageplus objects
   * @param inRDD
   * @tparam A
   * @tparam B
   */
  implicit class ijOpsRDD[A : ClassTag,B <: PortableImagePlus : ClassTag](inRDD: RDD[(A,B)]) {
    /**
     * Runs the given command and arguments on all of the images in a list
     * @param cmd the imagej macro-style command to run (example "Add Noise")
     * @param args the arguments for the macro-style command (example "radius=3")
     * @return the updated images (lazy evaluated)
     */
    def runAll(cmd: String, args: String = "")(implicit fs: ImageJSettings) = {
      inRDD.mapPartitions{
        kvlist =>
          SetupImageJInPartition(fs)
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
          SetupImageJInPartition(fs)
          kvlist.flatMap{
            case (basepath,imgobj) =>
              for((args,pathsuffix) <- swArgsPath) yield (basepath+pathsuffix,imgobj.run(cmd,args))
          }
      }
    }

    def getStatistics() = {
      inRDD.mapValues(_.getImageStatistics())
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
    _root_.tipl.ij.ImageStackToTImg.FromImagePlus(curImage)
  }



}
