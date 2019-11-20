package fourquant.imagej

import fourquant.imagej.ImagePlusIO.{ImageLog, LogEntry}
import fourquant.imagej.PortableImagePlus.IJMetaData
import fourquant.io.hadoop.ByteOutputFormat
import ij.ImagePlus
import org.apache.hadoop.io.{BytesWritable, NullWritable}
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapreduce.lib.input.{FileInputFormat => NewFileInputFormat}
import org.apache.hadoop.mapreduce.{InputFormat => NewInputFormat, Job => NewHadoopJob}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SQLContext, SparkSession}

import scala.reflect.ClassTag


trait FijiInit {
  def setupSpark(sc: SparkContext, partRun: Boolean): Unit
  def setupFiji(): Unit
}

/**
 * Tools for making previewing and exploring data in FIJI from Spark easy
  *
  * @author mader
 *
 */
object scOps {

  def StartFiji(ijPath: String, show: Boolean = false,
                runLaunch: Boolean = true, record: Boolean = true): Unit = {
    Spiji.start(ijPath, show, runLaunch)
    if (record) Spiji.startRecording()
  }





def SetupImageJInPartition(ijs: ImageJSettings): Unit = ijs.setupFiji


  def loadImages(path: String, partitions: Int)(implicit sc: SparkContext,
                                                ijs: ImageJSettings) = {
    sc.loadImages(path,partitions)
  }

  /**
   * Push the list of images from the driver machine to the Spark Cluster (only if they are not
   * available on the cluster / shared file system
    *
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
      ijs.map(_.setupSpark(sc,true))
      sc.
        parallelize(0 until imgs).
        map(
          i => (prefix+i.toString,
          new PortableImagePlus(Array.fill[A](width, height)(indFun(i)),IJMetaData.emptyMetaData))
      )
    }

    def loadImages(path: String, partitions: Int)(implicit ijs: ImageJSettings) = {
      ijs.setupSpark(sc)
      val ioData = sc.binaryFiles(path,partitions)
      val sliceNames = ioData.map(_._1).collect()
      // get the names and partition before the files are copy and read (since this is expensive
      // as is moving around large serialized objects

      //TODO add back partitionBy(DSImg.NamedSlicePartitioner(sliceNames,partitions))

      ioData.
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
      *
      * @param paths the list of paths to open
     * @param partitions desired partition count
     * @param ijs settings for configuring imagej
     * @return imageplus rdd
     */
    def loadImagesLocally(paths: Array[String],partitions: Int)(implicit ijs: ImageJSettings) = {
      //TODO partitionBy(DSImg.NamedSlicePartitioner(paths,partitions)).
      sc.parallelize(paths).map(p => (p,"placeholder")).
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
      *
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
      )
        //TODO add back .partitionBy(DSImg.NamedSlicePartitioner(paths,partitions))
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
    *
    * @param inRDD
   * @tparam A
   * @tparam B
   */
  implicit class ijOpsRDD[A : ClassTag,B <: PortableImagePlus : ClassTag](inRDD: RDD[(A,B)]) {
    /**
     * Runs the given command and arguments on all of the images in a list
      *
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
    /*
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

  /**
   * The IO operations for an ImageJ based RDD object
    *
    * @param inRDD the RDD containing the images
   * @tparam A the type of the key (usually string or path, can also contain identifiers)
   * @tparam B portableimageplus or a subclass
   */
  implicit class ijIORDD[A : ClassTag,B <: PortableImagePlus : ClassTag](inRDD: RDD[(A,B)]) {
    /**
     * The command to save images (if path does not contain a url hdfs://, s3://, http://) the
     * file is saved using the saveImagesLocal where the path will be prepended (best if it is
     * empty)
      *
      * @note path must contain and ending seperator for local operation
     *       @note if (A) contains an hadoop path this save will not work since all the files
     *             have to be in the same path
     * @param suffix the suffix to append so imagej knows the filetype
     * @param path the path to prepend (if needed)
     */
    def saveImage(suffix: String,path: String = "")(implicit fs: ImageJSettings) = {
      if (path.contains("://")) {
        saveImages(path,suffix)
      }
      else inRDD.map( kv => (path+kv._1.toString,kv._2)).saveImagesLocal(suffix)
    }
    /**
     * Saves the images locally using ImageJ
      *
      * @param suffix text to add to the existing path
     */
    protected[imagej] def saveImagesLocal(suffix: String)(implicit fs: ImageJSettings) = {
      inRDD.foreachPartition{
        imglist =>
          SetupImageJInPartition(fs)
          imglist.foreach {
            case(filename,imgobj) =>
              Spiji.saveImage(imgobj.getImg,filename+suffix)
          }
      }
    }

    /**
     * Save images in a hadoop style using a directory with subnames (part-0000 ...),
      *
      * @note these files have no extension so the filenames/ metadata should be stored elsewhere
     *       @note it is recommended to use saveImageLocal for most cases
     * @param path the folder to write the images too
     * @param newSuffix the suffix (for writing the correct filetype
     */
    protected[imagej] def saveImages(path: String,newSuffix: String)(
      implicit fs: ImageJSettings) = {
      val format = classOf[ByteOutputFormat[NullWritable, BytesWritable]]
      val jobConf = new JobConf(inRDD.context.hadoopConfiguration)
      val namelist = inRDD.keys.collect
      //TODO reimplement partitionBy(new DSImg.NamedSlicePartitioner(namelist)).
      inRDD.
        mapPartitions {
        imglist =>
          SetupImageJInPartition(fs)
          imglist.map {
            case (keyobj, imgobj) =>
              (
                NullWritable.get(),
                new BytesWritable(Spiji.saveImageAsByteArray(imgobj.getImg(),newSuffix))
                )
          }
      }.
        saveAsHadoopFile(path, classOf[NullWritable], classOf[BytesWritable], format)
    }
  }

  /**
   * Add imageplus reader to the sparkcontext
   */
  implicit class ImageJFriendlySparkContext(sc: SparkContext) {
    val defMinPart = sc.defaultMinPartitions

    def ijFile(path: String, minPartitions: Int = sc.defaultMinPartitions): RDD[(String,
      PortableImagePlus)] = {
      sc.binaryFiles(path,minPartitions).map{
        case (pname, pdsObj) =>
          (pname,new PortableImagePlus(Spiji.loadImageFromInputStream(pdsObj.open(),
            pname.split("[.]").last)))
      }
    }

  }
  @deprecated("should be avoided, but makes some operations easier","1.0")
  implicit def ImagePlusToPortableImagePlus(imp: ImagePlus) = new PortableImagePlus(imp)

  @deprecated("implicits have been moved to sub-object, also SQLContext has been deprecated","1.0")
  implicit class ImageJFriendlySQLContext(sq: SQLContext) {
    /**
      * setup imagej in all instances and register plugins
      *
      * @param fs
      */
    @deprecated("implicits have been moved and sqlcontext deprecated","1.0")
    def registerImageJ(implicit fs: ImageJSettings): Unit = {
      fs.setupSpark(sq.sparkContext)
      registerImageJFunctions(fs)
    }

    /** add all the needed udfs to the sqlcontext **/
    @deprecated("implicits have been moved and sqlcontext deprecated","1.0")
    def registerImageJFunctions(implicit fs: ImageJSettings): Unit = {
      SQLFunctions.registerImageJ(sq.udf,fs)
      SQLFunctions.registerDebugFunctions(sq.udf,fs)
    }
  }

  object implicits extends Serializable {

    /**
      * A SparkSession with ImageJ functions integrated
      * @param ss
      */
    implicit class ImageJFriendlySession(ss: SparkSession) {
      /**
        * setup imagej in all instances and register plugins
        *
        * @param fs
        */
      def registerImageJ(implicit fs: ImageJSettings): Unit = {

        fs.setupSpark(ss.sparkContext)
        registerImageJFunctions(fs)
      }

      /** add all the needed udfs to the sqlcontext **/
      def registerImageJFunctions(implicit fs: ImageJSettings): Unit = {
        SQLFunctions.registerImageJ(ss.udf,fs)
        SQLFunctions.registerDebugFunctions(ss.udf,fs)
      }
    }

  }



}
