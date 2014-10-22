package tipl.spark

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._
import tipl.formats.{ TImg, TImgRO }
import tipl.util.{TImgBlock, D3float, D3int, TImgSlice, TImgTools, TypedPath}

import scala.reflect.ClassTag
import scala.{ specialized => spec }

/**
  * an abstract class with all of the standard TImgRO functions but getPoly
  *
  * @author mader
  */
object ATImgRO {
  private final val serialVersionUID: Long = -8883859233940303695L
}

trait TImgROLike extends TImgRO {

  def baseSize: TImgTools.HasDimensions

  def imageType: Int

  var baseDimensions: TImgRO = new TImgRO.ATImgRO(baseSize, imageType) {
    println("Warning: Creating mock TImgRO class!")

    override def getPolyImage(sliceNumber: Int, asType: Int): AnyRef = null

    override def getSampleName: String = "Mock TImgRO class, should not be used for anything at all"
  }

  def getDim(): D3int = baseSize.getDim()

  def getElSize: D3float = baseSize.getElSize

  def getOffset: D3int = baseSize.getOffset

  def getPos: D3int = baseSize.getPos

  def getProcLog: String = baseSize.getProcLog

  def appendProcLog(inData: String): String = baseDimensions.appendProcLog(inData)

  def getCompression: Boolean = baseDimensions.getCompression

  def getImageType: Int = baseDimensions.getImageType

  def getPath: TypedPath = baseDimensions.getPath

  def getShortScaleFactor: Float = baseSize.getShortScaleFactor

  def getSigned: Boolean = baseDimensions.getSigned

  def isFast: Int = baseDimensions.isFast

  def isGood: Boolean = baseDimensions.isGood
}

trait TImgLike extends TImgROLike with TImg {

  /**
    * Create a new object by wrapping the RO in a ATImg
    */
  val baseDimensionsRW: TImg = new TImg.ATImgWrappingTImgRO(baseDimensions)

  def inheritedAim(imgArray: Array[Boolean], idim: D3int, offset: D3int): TImg =
    baseDimensionsRW.inheritedAim(imgArray, idim, offset)

  def inheritedAim(imgArray: Array[Char], idim: D3int, offset: D3int): TImg =
    baseDimensionsRW.inheritedAim(imgArray, idim, offset)

  def inheritedAim(imgArray: Array[Float], idim: D3int, offset: D3int): TImg =
    baseDimensionsRW.inheritedAim(imgArray, idim, offset)

  def inheritedAim(imgArray: Array[Int], idim: D3int, offset: D3int): TImg =
    baseDimensionsRW.inheritedAim(imgArray, idim, offset)

  def inheritedAim(imgArray: Array[Short], idim: D3int, offset: D3int): TImg =
    baseDimensionsRW.inheritedAim(imgArray, idim, offset)

  def inheritedAim(inAim: TImgRO): TImg = baseDimensionsRW.inheritedAim(inAim)

  def setDim(inData: D3int) = baseDimensionsRW.setDim(inData)

  def setElSize(inData: D3float) = baseDimensionsRW.setElSize(inData)

  def setOffset(inData: D3int) = baseDimensionsRW.setOffset(inData)

  def setPos(inData: D3int) = baseDimensionsRW.setPos(inData)

  def InitializeImage(dPos: D3int, cDim: D3int, dOffset: D3int, elSize: D3float,
    imageType: Int): Boolean =
    baseDimensionsRW.InitializeImage(dPos, cDim, dOffset, elSize, imageType)

  def setCompression(inData: Boolean) = baseDimensionsRW.setCompression(inData)

  def setShortScaleFactor(ssf: Float) = baseDimensionsRW.setShortScaleFactor(ssf)

  def setSigned(inData: Boolean) = baseDimensionsRW.setSigned(inData)

}

/**
  * Created by mader on 10/13/14.
  */
abstract class ImageRDD[@spec(Boolean, Byte, Short, Int, Long, Float, Double) T](dim: D3int,
  pos: D3int,
  elSize: D3float,
  imageType: Int, path: TypedPath, baseImg: Either[RDD[(D3int, TImgBlock[T])], RDD[(D3int,
  T)]])(implicit lm: ClassTag[T])
  extends TImg.ATImg(dim, pos, elSize, imageType) {

  def this(newImg: ImageRDD[_],baseImg: Either[RDD[(D3int, TImgBlock[T])], RDD[(D3int,
    T)]])(implicit lm: ClassTag[T]) = this(newImg.getDim,newImg.getPos,newImg.getElSize,newImg.getImageType,newImg.getPath,
    baseImg)

  def map[U](f: (D3int, T) => U)(implicit um: ClassTag[U]): ImageRDD[U] = {
    val newBase = baseImg.fold(
      { dsObj =>
        val bob = dsObj.mapValues(inVal => inVal.get())
        Left(bob.map{
          inKV =>
            val (pos,slice) = inKV
            (pos,slice.map(f(pos,_)))
        })
      }, {
        kvObj =>
          Right(kvObj.map(ikv => (ikv._1,f(ikv._1,ikv._2))))
      })
    new ImageRDD[U](this,newBase)
  }
  def map[U: ClassTag](f: (D3int, Array[T]) => Array[U]): ImageRDD[U] = ???

}

object ImageRDD {

}
