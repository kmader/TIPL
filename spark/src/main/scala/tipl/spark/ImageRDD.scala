package tipl.spark

import org.apache.spark.rdd.RDD
import tipl.formats.TImgRO.ATImgRO
import tipl.formats.{TImg, TImgRO}
import tipl.util.{D3float, D3int, TImgBlock, TImgTools, TypedPath}

import scala.reflect.ClassTag
import scala.{specialized => spec}

trait TImgROLike extends TImgRO {

  def baseSize: TImgTools.HasDimensions
  def imageType: Int

  lazy val baseDimensions: TImgRO = new ATImgRO(baseSize,imageType) {
    println("Warning: Creating mock TImgRO class!")
    override def getPolyImage(sliceNumber: Int, asType: Int): AnyRef = null

    override def getSampleName: String = "Mock TImgRO class, should not be used for anything at all"
  }

  def getDim(): D3int = baseDimensions.getDim()

  def getElSize: D3float = baseDimensions.getElSize

  def getOffset: D3int = baseDimensions.getOffset

  def getPos: D3int = baseDimensions.getPos

  def getProcLog: String = baseDimensions.getProcLog

  def appendProcLog(inData: String): String = baseDimensions.appendProcLog(inData)

  def getCompression: Boolean = baseDimensions.getCompression

  def getImageType: Int = baseDimensions.getImageType

  def getPath: TypedPath = baseDimensions.getPath

  def getShortScaleFactor: Float = baseDimensions.getShortScaleFactor

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
    baseDimensionsRW.inheritedAim(imgArray,idim,offset)

  def inheritedAim(imgArray: Array[Char], idim: D3int, offset: D3int): TImg =
    baseDimensionsRW.inheritedAim(imgArray,idim,offset)

  def inheritedAim(imgArray: Array[Float], idim: D3int, offset: D3int): TImg =
    baseDimensionsRW.inheritedAim(imgArray,idim,offset)

  def inheritedAim(imgArray: Array[Int], idim: D3int, offset: D3int): TImg =
    baseDimensionsRW.inheritedAim(imgArray,idim,offset)

  def inheritedAim(imgArray: Array[Short], idim: D3int, offset: D3int): TImg =
    baseDimensionsRW.inheritedAim(imgArray,idim,offset)

  def inheritedAim(inAim: TImgRO): TImg = baseDimensionsRW.inheritedAim(inAim)

  def setDim(inData: D3int) = baseDimensionsRW.setDim(inData)

  def setElSize(inData: D3float) = baseDimensionsRW.setElSize(inData)

  def setOffset(inData: D3int) = baseDimensionsRW.setOffset(inData)

  def setPos(inData: D3int) = baseDimensionsRW.setPos(inData)

  def InitializeImage(dPos: D3int, cDim: D3int, dOffset: D3int, elSize: D3float, imageType: Int): Boolean =
    baseDimensionsRW.InitializeImage(dPos,cDim,dOffset,elSize,imageType)

  def setCompression(inData: Boolean) = baseDimensionsRW.setCompression(inData)

  def setShortScaleFactor(ssf: Float) = baseDimensionsRW.setShortScaleFactor(ssf)

  def setSigned(inData: Boolean) = baseDimensionsRW.setSigned(inData)

}

/**
 * Created by mader on 10/13/14.
 */
abstract class ImageRDD[@spec(Boolean, Byte, Short, Int, Long, Float, Double) T,U]
  (dim: D3int, pos: D3int, elSize: D3float, imageType: Int, baseImg: RDD[(D3int, TImgBlock[Array[T]])],path: TypedPath)
  (implicit lm: ClassTag[T])
  extends RDD[(D3int, TImgBlock[Array[T]])](baseImg) {


}
