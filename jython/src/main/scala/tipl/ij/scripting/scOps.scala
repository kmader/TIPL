package tipl.ij.scripting

import ij.{WindowManager, ImagePlus, ImageStack, IJ}
import org.apache.spark.rdd.RDD
import tipl.formats.TImgRO
import tipl.ij.Spiji
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

  trait PortableImgPlus extends Serializable {
    def getImg: ImagePlus
    def getArray: Any
    @throws[IOException]("if the file doesn't exist")
    protected def writeObject(oos: ObjectOutputStream): Unit = {

    }
    @throws[IOException]("if the file doesn't exist")
    @throws[ClassNotFoundException]("if the class cannot be found")
    protected def readObject(in: ObjectInputStream): Unit  {

    }
    @throws(classOf[ObjectStreamException])
    protected def readObjectNoData: Unit = {
      throw new IllegalArgumentException("This function has not been implemented yet");
    }

  }

  class PortableImagePlus(var baseData: Either[ImagePlus,AnyRef]) extends Serializable {
    def this(inImage: ImagePlus) = this(Left(inImage))
    def this(inArray: AnyRef) = this(Right(inArray))

    private def getImg: ImagePlus =
      baseData match {
        case Left(tImg) => tImg
        case Right(tArr) => Spiji.createImage(File.createTempFile("img","").getName,tArr,false)
      }

    private def getArray: AnyRef =
    baseData match {
      case Left(tImg) =>
        WindowManager.setTempCurrentImage(tImg)
        Spiji.getCurrentImage
      case Right(tArr) => tArr
    }

    lazy val curImg = getImg
    lazy val curArr = getArray

    // custom serialization
    @throws[IOException]("if the file doesn't exist")
    protected def writeObject(oos: ObjectOutputStream): Unit = {
      oos.writeObject(curArr)
    }
    @throws[IOException]("if the file doesn't exist")
    @throws[ClassNotFoundException]("if the class cannot be found")
    protected def readObject(in: ObjectInputStream): Unit =  {
      baseData = Right(in.readObject())
    }
    @throws(classOf[ObjectStreamException])
    protected def readObjectNoData: Unit = {
      throw new IllegalArgumentException("Cannot have a dataless protableimageplus");
    }
  }


  class ArraybackedPortableImgPlus(getArray: () => AnyRef, getImage: () => ImagePlus) {

    @transient
    lazy val curImg = getImage()
    lazy val curArr = getArray()
    def this(inArray: AnyRef) = {
      this(
        getArray=() => inArray,
        getImage=() => Spiji.createImage(File.createTempFile("img","").getName,inArray,false)
      )
    }

    def this(inImage: ImagePlus) = {
      this({
        WindowManager.setTempCurrentImage(inImage)
        Spiji.getCurrentImage
      })
    }

    def run(cmd: String, args: String = ""): ImagePlus = {
      WindowManager.setTempCurrentImage(curImg)
      Spiji.run(cmd,args)
      WindowManager.getCurrentImage()
    }

    def asArray() = curArr

    // custom serialization
    @throws[IOException]("if the file doesn't exist")
    protected def writeObject(oos: ObjectOutputStream): Unit = {
      oos.writeObject(curArr)
    }
    @throws[IOException]("if the file doesn't exist")
    @throws[ClassNotFoundException]("if the class cannot be found")
    protected def readObject(in: ObjectInputStream): Unit =  {
      val inArray = in.readObject()
      getArray=() => inArray
      getImage=() => Spiji.createImage(File.createTempFile("img","").getName,inArray,false)
    }
    @throws(classOf[ObjectStreamException])
    protected def readObjectNoData: Unit = {
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
