package fourquant.imagej

import java.io._

import fourquant.imagej.ImagePlusIO.{ImageLog, LogEntry}
import fourquant.imagej.ParameterSweep.ImageJSweep
import fourquant.imagej.PortableImagePlus.IJMetaData
import fourquant.imagej.Spiji.{PIPOps, PIPTools}
import ij.ImagePlus
import ij.plugin.PlugIn
import ij.plugin.filter.PlugInFilter
import ij.process.ImageProcessor
import org.apache.spark.annotation.Experimental
import org.apache.spark.fourquant.PipUDT
import org.apache.spark.sql.types._


/**
 * Since ImagePlus is not serializable this class allows for it to be serialized and thus used
 * in operations besides map in Spark.
  *
  * @param baseData either an array of the correct type or an imageplus object
 */
@SQLUserDefinedType(udt = classOf[PipUDT])
class PortableImagePlus(var baseData: Either[ImagePlus,(PortableImagePlus.IJMetaData,AnyRef)],
                        var imgLog: ImageLog) extends Serializable {

  val pipStoreSerialization = false

  /**
   * if only one entry is given, create a new log from the one entry
    *
    * @param logEntry single entry (usually creation)
   */
  def this(bd: Either[ImagePlus,(PortableImagePlus.IJMetaData,AnyRef)], logEntry: LogEntry) =
    this(bd,new ImageLog(logEntry))

  def this(inImage: ImagePlus, oldLog: ImageLog) =
    this(Left(inImage),oldLog)

  @deprecated("this should not be used if imagelog information is available","1.0")
  def this(inImage: ImagePlus) =
    this(inImage,new ImageLog(LogEntry.create(inImage)))




  @deprecated("should not be used, since images should always have a log","1.0")
  def this(inArray: AnyRef, cal: PortableImagePlus.IJMetaData) =
    this(Right((cal,
      inArray)),
      LogEntry.createFromArray("SpijiArray",inArray))


  @deprecated("should only be used when a source is not known","1.0")
  def this(inProc: ImageProcessor) =
    this(
      new ImagePlus(File.createTempFile("img","").getName,inProc)
    )

  def this(inProc: ImageProcessor, oldLog: ImageLog) = this(
    new ImagePlus(File.createTempFile("img","").getName,inProc))

  private def calcImg: ImagePlus =
    baseData match {
      case Left(tImg) => tImg
      case Right((ijmd,tArr)) => {
        val oImage = Spiji.createImage(File.createTempFile("img","").getName,tArr,false)
        val nImage = ijmd.toImagePlus(oImage)
        nImage
      }
    }

  private def calcArray: AnyRef =
    baseData match {
      case Left(tImg) =>
        Spiji.setTempCurrentImage(tImg)
        Spiji.getCurrentImage
      case Right(tArr) =>
        tArr._2
    }

  private def calcMetaData: IJMetaData =
    baseData match {
      case Left(tImg) => IJMetaData.fromImagePlus(tImg)
      case Right(tArr) =>
        tArr._1
    }
  lazy val curImg = calcImg
  lazy val curArr = calcArray
  lazy val curMetaData = calcMetaData

  def getImg() = curImg
  def getArray() = curArr
  def getMetaData = curMetaData

  override def toString(): String = {
    val nameFcn = (inCls: String) => this.getClass().getSimpleName()+"["+inCls+"]"

    baseData match {
      case Left(img) if img != null => nameFcn(img.toString)
      case Right(carr)  => nameFcn(s"${carr._1}, {${carr._2}}")
      case other_data => nameFcn("Null Image")
    }

  }

  /**
    * Run a plugin and return a new image
    *
    * @param cmd the name of the command
    * @param args the arguments (optional)
    * @return
    */
  def run(cmd: String, args: String = ""): PortableImagePlus = {
    import PortableImagePlus.implicits._
    new PortableImagePlus(curImg.run(cmd,args),
      this.imgLog.appendAndCopy(LogEntry.ijRun(cmd,args))
    )
  }

  /**
    * get the image and the table
    *
    * @param cmd
    * @param args
    * @return an image and a table
    */
  def runWithTable(cmd: String, args: String = ""): (PortableImagePlus, IJResultsTable) = {
    import PortableImagePlus.implicits._
    val (outImPlus,outTable) = curImg.runWithTable(cmd,args)
    (new PortableImagePlus(outImPlus,
      this.imgLog.appendAndCopy(LogEntry.ijRun(cmd,args))),
      outTable)
  }

  def runAsPlugin(cmd: String, args: String = ""): Either[PlugIn,PlugInFilter] = {
    import PortableImagePlus.implicits._
    curImg.runAsPlugin(cmd,args)
  }

  def getImageStatistics() = {
    import PortableImagePlus.implicits._
    curImg.getImageStatistics()
  }




  def getMeanValue() =
    getImageStatistics().mean

  @Experimental
  def analyzeParticles() = {
    IJResultsTable.fromCL(Some(curImg))
  }

  /**
    * Apply an operation to all image processors in an image
 *
    * @param procOp the operation to apply to each processor
    */
  @Experimental
  def processorForEach(procOp: ((ImageProcessor) => ImageProcessor),logEntry: LogEntry) = {

    val newImage = getImg().duplicate()
    if (newImage.getNSlices() > 1) {
      val cStack = newImage.getStack()
      val nStack = newImage.createEmptyStack()
      for(i <- 1 to getImg().getNSlices()) {
       nStack.addSlice(procOp(cStack.getProcessor(i)))
      }
      newImage.setStack(nStack)
      // stack process
    } else {
      newImage.setProcessor(procOp(newImage.getProcessor()))
    }

    val newLog = imgLog.appendAndCopy(logEntry)
    new PortableImagePlus(newImage,imgLog)
  }



  /**
    * Apply a fixed offset to an image
    *
    * @param offset shift the image by the given amount (default is for CT images)
    * @return
    */
  @Experimental
  def applyOffset(offset: Int = -1024) = {
    val procOp = (ip: ImageProcessor) => {val np = ip.convertToFloatProcessor(); np.add(offset);  np}
    processorForEach(procOp,LogEntry(PIPOps.ADD,PIPTools.IMAGEJ,offset.toString))
  }

  /**
   * Create a histogram for the given image
    *
    * @param range the minimum and maximum values for the histogram
   * @param bins the number of bins
   * @return a histogram case class (compatible with SQL)
   */
  @Experimental
  def getHistogram(range: Option[(Double,Double)] = None, bins: Int = IJHistogram.histInterpCount) =
    IJHistogram.fromIJ(Some(curImg),range,bins)

  /**
   * average two portableimageplus objects together
    *
    * @note works for floating point images of the same size
   * @param ip2 second image
   *            @param rescale is the rescaling factor for the combined pixels
   * @return new image with average values
   */
  @Experimental
  def average(ip2: PortableImagePlus,rescale: Double = 2): PortableImagePlus = {
    val outProc = ip2.getImg().getProcessor.
      duplicate().convertToFloatProcessor()
    val curArray = curImg.getProcessor.convertToFloatProcessor().
      getPixels().asInstanceOf[Array[Float]]
    val opixs = outProc.getPixels.asInstanceOf[Array[Float]]
    var i = 0
    while(i<opixs.length) {
      opixs(i)=((opixs(i)+curArray(i))/rescale).toFloat
      i+=1
    }
    outProc.setPixels(opixs)
    new PortableImagePlus(outProc,
      ImageLog.merge(this.imgLog,ip2.imgLog,"AVERAGE","rescale=%f".format(rescale))
    )
  }

  @Experimental
  def multiply(rescale: Double): PortableImagePlus = {
    val procOp = (ip: ImageProcessor) => {val np = ip.convertToFloatProcessor; np.multiply(rescale); np}
    processorForEach(procOp,LogEntry(PIPOps.OTHER,PIPTools.SPARK,"multiply",
      "rescale=%f".format(rescale)))
  }

  @Experimental
  def subtract(bgImage: PortableImagePlus): PortableImagePlus = {
    val outProc = curImg.getProcessor.duplicate().convertToFloatProcessor()
    val bgArray = bgImage.getImg().getProcessor.convertToFloatProcessor().
      getPixels().asInstanceOf[Array[Float]]
    val opixs = outProc.getPixels.asInstanceOf[Array[Float]]
    var i = 0
    while(i<opixs.length) {
      opixs(i)-=bgArray(i)
      i+=1
    }
    outProc.setPixels(opixs)
    new PortableImagePlus(outProc,
      ImageLog.merge(this.imgLog,bgImage.imgLog,"SUBTRACT","")
    )
  }

  def ++(ip2: PortableImagePlus): PortableImagePlus = {
    val outImg = ip2.getImg().duplicate()
    val outStack = outImg.getImageStack
    val curStack = curImg.getImageStack
    for(i <- 1 to curStack.getSize)
      outStack.addSlice(curStack.getSliceLabel(i),
        curStack.getProcessor(i))
    new PortableImagePlus(outImg,
      ImageLog.merge(this.imgLog,ip2.imgLog,"APPEND","")
    )
  }

  // custom serialization
  @throws[IOException]("if the file doesn't exist")
  private def writeObject(oos: ObjectOutputStream): Unit = {
    oos.writeObject(imgLog)
    oos.writeObject(curMetaData)
    oos.writeObject(curArr)
  }
  @throws[IOException]("if the file doesn't exist")
  @throws[ClassNotFoundException]("if the class cannot be found")
  private def readObject(in: ObjectInputStream): Unit =  {
    imgLog = in.readObject.asInstanceOf[ImageLog]
    val calibration = in.readObject().asInstanceOf[IJMetaData]
    val data = in.readObject()
    baseData = Right((calibration,data))
  }
  @throws(classOf[ObjectStreamException])
  private def readObjectNoData: Unit = {
    throw new IllegalArgumentException("Cannot have a dataless PortableImagePlus");
  }
}

object PortableImagePlus extends Serializable {
  /**
   * Should immutablity of imageplus be ensured at the cost of performance and memory
   */
  val ensureImmutability: Boolean = true

  /**
    * All of the relevant associated metadata for an imageplus object so it can be serialized and tossed around
    *
    * @param ijc
    */
  case class IJMetaData(ijc: IJCalibration, info_str: String) {
    /**
      * apply the metadata to an imageplus object
 *
      * @param imp
      * @return imageplus object with metadata applied
      */
    def toImagePlus(imp: ImagePlus): ImagePlus = {
      imp.setCalibration(ijc.asCalibration(imp))
      //calib.setImage(oImage)
      imp.setProperty("Info",info_str)
      imp
    }

  }

  object IJMetaData extends Serializable {
    /**
      * encapsulate all the metadata from a imageplus object
 *
      * @param imp
      * @return
      */
    def fromImagePlus(imp: ImagePlus) = {
      val ijc = new IJCalibration(imp.getCalibration())
      val info = imp.getInfoProperty match {
        case null => ""
        case a: String => a
      }
      IJMetaData(ijc,info)
    }

    /**
      * create a new empty metadata field (should not normally be used
 *
      * @return
      */
    @deprecated("Should only be used for synthetic or otherwise artificially generated images","1.0")
    def emptyMetaData() = {
      new IJMetaData(new IJCalibration(),"")
    }
  }

  object implicits extends Serializable {
    implicit class cleverImagePlus(curImg: ImagePlus) {
      def run(cmd: String, args: String = ""): ImagePlus = {
        lazy val pargs = ImageJSweep.parseArgsWithDelim(args," ")

        val oldCal = new IJCalibration(curImg.getCalibration)
        //NOTE the duplicate command does not copy the calibration
        val localImgCopy = if(ensureImmutability) curImg.duplicate() else curImg
        //NOTE the calibration is copied more reliably through copyAttributes than setCalibration (no idea why)
        //localImgCopy.copyAttributes(curImg)
        localImgCopy.setCalibration(oldCal.asCalibration(curImg))

        //println("ORIG:\t"+curImg.getCalibration()+"\nCOPY:\t"+localImgCopy.getCalibration())

        Spiji.setTempCurrentImage(localImgCopy)
        cmd match {
          case "setThreshold" | "applyThreshold" =>
            import fourquant.imagej.ParameterSweep.ImageJSweep.argMap // for the implicit
          // conversions getDbl
          val lower = pargs.getDbl("lower",Double.MinValue)
            val upper = pargs.getDbl("upper",Double.MaxValue)
            Spiji.setThreshold(lower,upper)
            cmd match {
              case "applyThreshold" =>
                Spiji.run("Convert to Mask")
              case _ =>
                Unit
            }
          case _ =>
            Spiji.run(cmd,args)
        }

        val outImage = Spiji.getCurImage()
        //outImage.copyAttributes(curImg)
        outImage.setCalibration(oldCal.asCalibration(outImage))

        //println("ORIG:\t"+curImg.getCalibration()+"\nIOUT:\t"+outImage.getCalibration())
        outImage
      }

      /**
        * A function to run a command and keep both the output image and the results table in a tuple
        *
        * @return the image followed by the output table
        */
      def runWithTable(cmd: String, args: String = "") = {
        IJResultsTable.clearTable()
        (run(cmd,args),
          IJResultsTable.fromIJ())
      }

      def runAsPlugin(cmd: String, args: String = ""): Either[PlugIn,PlugInFilter] = {
        Spiji.setTempCurrentImage(curImg)
        Spiji.runCommandAsPlugin(cmd,args) match {
          case plug: PlugIn =>
            Left(plug)
          case plugfilt: PlugInFilter =>
            Right(plugfilt)
        }
      }

      def getImageStatistics() = {
        val istat = curImg.getStatistics
        ImageStatistics(istat.min, istat.mean, istat.stdDev, istat.max, istat.pixelCount)
      }

    }
  }
}

