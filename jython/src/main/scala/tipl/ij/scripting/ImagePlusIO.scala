package tipl.ij.scripting

import java.io._

import ij.plugin.PlugIn
import ij.plugin.filter.PlugInFilter
import ij.process.ImageProcessor
import ij.{WindowManager, ImagePlus}
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.lib.input.{CombineFileSplit, CombineFileRecordReader}
import org.apache.hadoop.mapreduce.{TaskAttemptContext, InputSplit}
import org.apache.spark.annotation.Experimental
import org.apache.spark.input.{BinaryRecordReader, BinaryFileInputFormat}
import tipl.blocks.ParameterSweep.ImageJSweep
import tipl.formats.TImgRO
import tipl.ij.{ImageStackToTImg, Spiji}

object ImagePlusIO {
  /**
   * Should immutablity of imageplus be ensured at the cost of performance and memory
   */
  val ensureImmutability: Boolean = true

  /**
   * The new (Hadoop 2.0) InputFormat for imagej files (not be to be confused with the
   * recordreader
   * itself)
   */
  class ImagePlusFileInputFormat extends BinaryFileInputFormat[PortableImagePlus] {
    override def createRecordReader(split: InputSplit, taContext: TaskAttemptContext) = {
      new CombineFileRecordReader[String, PortableImagePlus](split.asInstanceOf[CombineFileSplit],
        taContext, classOf[ImagePlusRecordReader])
    }
  }


  class ImagePlusRecordReader(
                               split: CombineFileSplit,
                               context: TaskAttemptContext,
                               index: Integer)
    extends BinaryRecordReader[PortableImagePlus](split, context, index) {

    def parseByteArray(path: Path,inArray: Array[Byte]) =
      new PortableImagePlus(Spiji.loadImageFromByteArray(inArray,path.toString().
        split("[.]").last))
  }

  /**
   * Since ImagePlus is not serializable this class allows for it to be serialized and thus used
   * in operations besides map in Spark.
   * @param baseData either an array of the correct type or an imageplus object
   */
  class PortableImagePlus(var baseData: Either[ImagePlus,AnyRef]) extends Serializable {

    def this(inImage: ImagePlus) = this(Left(inImage))
    def this(inArray: AnyRef) = this(Right(inArray))
    def this(inProc: ImageProcessor) = this(Left(
      new ImagePlus(File.createTempFile("img","").getName,inProc)))

    private def calcImg: ImagePlus =
      baseData match {
        case Left(tImg) => tImg
        case Right(tArr) => Spiji.createImage(File.createTempFile("img","").getName,tArr,false)
      }

    private def calcArray: AnyRef =
      baseData match {
        case Left(tImg) =>
          WindowManager.setTempCurrentImage(tImg)
          Spiji.getCurrentImage
        case Right(tArr) => tArr
      }

    lazy val curImg = calcImg
    lazy val curArr = calcArray
    def getImg() = curImg
    def getArray() = curArr

    override def toString(): String = {
      val nameFcn = (inCls: String) => this.getClass().getSimpleName()+"["+inCls+"]"
      baseData.fold(
        img => nameFcn(img.toString),
        arr => nameFcn(scala.runtime.ScalaRunTime.stringOf(arr))
      )
    }

    // useful commands in imagej
    def run(cmd: String, args: String = ""): PortableImagePlus = {
      lazy val pargs = ImageJSweep.parseArgsWithDelim(args," ")
      val localImgCopy = if(ensureImmutability) curImg.duplicate() else curImg

      WindowManager.setTempCurrentImage(localImgCopy)
      cmd match {
        case "setThreshold" | "applyThreshold" =>
          import ImageJSweep.argMap
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
      new PortableImagePlus(WindowManager.getCurrentImage())
    }

    def runAsPlugin(cmd: String, args: String = ""): Either[PlugIn,PlugInFilter] = {
      WindowManager.setTempCurrentImage(curImg)
      Spiji.runCommandAsPlugin(cmd,args) match {
        case plug: PlugIn =>
          Left(plug)
        case plugfilt: PlugInFilter =>
          Right(plugfilt)
      }
    }
    case class ImageStatistics(min: Double,mean: Double, stdDev: Double,
                               max: Double, pts: Long) extends Serializable
    def getImageStatistics() ={
      val istat = curImg.getStatistics
      ImageStatistics(istat.min,istat.mean,istat.stdDev,istat.max,istat.longPixelCount)
    }


    def getTImgRO(): TImgRO =
      ImageStackToTImg.FromImagePlus(curImg)

    /**
     * average two portableimageplus objects together
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
      new PortableImagePlus(outProc)
    }

    @Experimental
    def multiply(rescale: Double): PortableImagePlus = {
      val outProc = curImg.getProcessor.
        duplicate().convertToFloatProcessor()
      outProc.multiply(rescale)
      new PortableImagePlus(outProc)
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
      new PortableImagePlus(outProc)
    }

    def ++(ip2: PortableImagePlus): PortableImagePlus = {
      val outImg = ip2.getImg().duplicate()
      val outStack = outImg.getImageStack
      val curStack = curImg.getImageStack
      for(i <- 1 to curStack.getSize)
        outStack.addSlice(curStack.getSliceLabel(i),
      curStack.getProcessor(i))
      new PortableImagePlus(outImg)
    }

    // custom serialization
    @throws[IOException]("if the file doesn't exist")
    private def writeObject(oos: ObjectOutputStream): Unit = {
      oos.writeObject(curArr)
    }
    @throws[IOException]("if the file doesn't exist")
    @throws[ClassNotFoundException]("if the class cannot be found")
    private def readObject(in: ObjectInputStream): Unit =  {
      baseData = Right(in.readObject())
    }
    @throws(classOf[ObjectStreamException])
    private def readObjectNoData: Unit = {
      throw new IllegalArgumentException("Cannot have a dataless protableimageplus");
    }
  }
}
/**
 * Created by mader on 1/16/15.
 */
class ImagePlusIO {

}
