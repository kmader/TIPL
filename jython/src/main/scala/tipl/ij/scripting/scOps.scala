package tipl.ij.scripting

import ij.plugin.PlugIn
import ij.plugin.filter.PlugInFilter
import ij.{WindowManager, ImagePlus, ImageStack, IJ}
import org.apache.spark.rdd.RDD
import tipl.formats.TImgRO
import tipl.ij.{ImageStackToTImg, Spiji}
import tipl.util.TImgSlice.TImgSliceAsTImg
import tipl.util.{D3int, TImgTools, TImgSlice}
import java.io._



class rddImage extends ImageStack {

}

/**
 * Tools for making previewing and exploring data in FIJI from Spark easy
 * @author mader
 *
 */
object scOps {

  implicit def ImagePlusToPortableImagePlus(imp: ImagePlus) = new PortableImagePlus(imp)


  /**
   * Since ImagePlus is not serializable this class allows for it to be serialized and thus used
   * in operations besides map in Spark.
   * @param baseData either an array of the correct type or an imageplus object
   */
  class PortableImagePlus(var baseData: Either[ImagePlus,AnyRef]) extends Serializable {
    def this(inImage: ImagePlus) = this(Left(inImage))
    def this(inArray: AnyRef) = this(Right(inArray))

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
      WindowManager.setTempCurrentImage(curImg)
      Spiji.run(cmd,args)
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

    def getTImgRO(): TImgRO =
      ImageStackToTImg.FromImagePlus(curImg)

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
