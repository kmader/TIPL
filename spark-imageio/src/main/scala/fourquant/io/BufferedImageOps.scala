package fourquant.io

import java.awt.image.{BufferedImage, DataBufferByte, Raster}

import org.apache.spark.annotation.Experimental
import org.nd4j.linalg.factory.Nd4j

import scala.reflect.ClassTag

/**
 * Operations for dealing with bufferedimage objects and arrays, a very generic interface for
 * converting back and forth so other rescaling tools can easily be placed on top
 * Created by mader on 4/11/15.
 */
object BufferedImageOps {

  val ALPHA_POS = 0
  val RED_POS = 1
  val GREEN_POS = 2
  val BLUE_POS = 3


  /**
   * Convert the value from a buffered image to an array of type T (and back, but they needn't
   * both be implemented full
   * @tparam T
   */
  trait ImageMapping[T] extends Serializable {

    def fromFloat(d: Float): T
    def fromInt(d: Int): T
    def fromByte(b: Byte): T
    def fromARGB(d: Array[Byte]): T

    def toFloat(d: T): Float
    def toInt(d: T): Int
    def toByte(b: T) : Byte
    def toARGB(d: T): Array[Byte]
  }


  /**
   * Convert an array from a buffered image to an array of type T
   * @tparam T
   */
  trait ArrayImageMapping[T] extends ImageMapping[T] {
    implicit def ct: ClassTag[T]

    def fromFloatArr(d: Array[Float]): Array[T] = d.map(fromFloat(_))
    def fromIntArr(d: Array[Int]): Array[T] = d.map(fromInt(_))
    def fromByteArr(b: Array[Byte]): Array[T] = b.map(fromByte(_))
    def fromARGBArr(d: Array[Array[Byte]]): Array[T] = d.map(fromARGB(_))

    def toFloatArr(d: Array[T]): Array[Float] = d.map(toFloat(_))
    def toIntArr(d: Array[T]): Array[Int] = d.map(toInt(_))
    def toByteArr(d: Array[T]): Array[Byte] = d.map(toByte(_))
    def toARGBArr(d: Array[T]): Array[Array[Byte]] = d.map(toARGB(_))
  }


  /**
   * An image without color information so the toByte and fromByte options are mirrored to
   * ARGB commands
   * @tparam T
   */
  trait GrayscaleImage[T] extends ImageMapping[T] {
    override def fromARGB(d: Array[Byte]): T ={
      val meanColor = (d(RED_POS).toDouble+d(BLUE_POS).toDouble+d(GREEN_POS).toDouble)/3.0
      fromByte(meanColor.toByte)
    }
    override def toARGB(d: T): Array[Byte] = Array.fill(4)(toByte(d))
  }

  class SimpleDoubleImageMapping(
    implicit val oct: ClassTag[Double]) extends
  ArrayImageMapping[Double] with GrayscaleImage[Double] {
    override def fromFloat(d: Float): Double = d

    override def fromInt(d: Int): Double = d.toDouble

    override def fromFloatArr(d: Array[Float]) = d.map(_.toDouble)

    override def fromIntArr(d: Array[Int]) = d.map(_.toDouble)

    override def ct: ClassTag[Double] = oct

    override def toFloat(d: Double): Float = d.toFloat

    override def toInt(d: Double): Int = d.toInt

    override def fromByte(b: Byte): Double = b.toDouble

    override def toByte(b: Double): Byte = b.toByte //TODO Fix
  }


  class ScaledDoubleImageMapping(val min: Double = 0, val max: Double = 255.0)(
    implicit val oct: ClassTag[Double]) extends
  ArrayImageMapping[Double] with GrayscaleImage[Double] {

    def checkRange(i: Double) = Math.min(Math.max(i,min),max)

    override def fromFloat(d: Float): Double = d // these do not scale

    override def fromFloatArr(d: Array[Float]) = d.map(_.toDouble)

    override def ct: ClassTag[Double] = oct

    override def toFloat(d: Double): Float = d.toFloat

    override def fromInt(d: Int): Double =
      (d-Int.MinValue)*(max-min)/(Int.MaxValue.toDouble-Int.MinValue.toDouble)+min

    override def toInt(d: Double): Int =
      Math.min(
        (checkRange(d)-min)*(Int.MaxValue.toDouble-Int.MinValue.toDouble)/(max-min)+Int.MinValue
        .toDouble,
        Int.MaxValue).toInt


    override def fromByte(b: Byte): Double =
        (b-Byte.MinValue)*(max-min)/(Byte.MaxValue.toDouble-Byte.MinValue.toDouble)+min

    override def toByte(b: Double): Byte =
      Math.min(
        (checkRange(b)-min)*(Byte.MaxValue.toDouble-Byte.MinValue.toDouble)/(max-min)+Byte
        .MinValue.toDouble,
        Byte.MaxValue).toByte
  }

  @Experimental
  @deprecated("Not sure if this works at all","0.0")
  class SimpleByteImageMapping(implicit val oct: ClassTag[Byte]) extends
  ArrayImageMapping[Byte] with GrayscaleImage[Byte] {
    override implicit def ct: ClassTag[Byte] = oct

    override def toFloat(d: Byte): Float = d.toFloat

    override def toInt(d: Byte): Int = d.toInt

    override def fromInt(d: Int): Byte = (d & 0xff).toByte


    override def fromFloat(d: Float): Byte = d.toByte //TODO not a good idea
    override def fromByte(b: Byte): Byte = b

    override def toByte(b: Byte): Byte = b
  }

  class SimpleCharImageMapping(implicit val oct: ClassTag[Char]) extends
  ArrayImageMapping[Char] with GrayscaleImage[Char] {
    override implicit def ct: ClassTag[Char] = oct

    override def toFloat(d: Char): Float = d.toFloat

    override def toInt(d: Char): Int = d.toInt

    override def fromInt(d: Int) = (d & 0xff).toChar

    override def fromFloat(d: Float) = d.toChar //TODO not a good idea

    override def fromByte(b: Byte): Char = b.toChar

    override def toByte(b: Char): Byte = b.toByte
  }

  object implicits {
    /**
     * @note directDoubleImageSupport.fromByte(directDoubleImageSupport.toByte(1000)) is -24
     */
    implicit val directDoubleImageSupport = new SimpleDoubleImageMapping
    implicit val byteImageSupport = new SimpleByteImageMapping
    implicit val charImageSupport = new SimpleCharImageMapping

  }

  /**
   * Implement all of the basic conversion functions on a bufferimage
   * @param bm
   */
  implicit class fqBufferImage(bm: BufferedImage) {

    def asArray[T : ArrayImageMapping]: Array[T] = {
      val activeRaster: Raster = bm.getData
      val sliceSize = activeRaster.getWidth * activeRaster.getHeight
      bm.getColorModel.getPixelSize match {
        case i if i<=16 =>
          // i (1: boolean, 8: byte, 16: short)
          val gi = activeRaster.getPixels(0, 0, activeRaster.getWidth, activeRaster.getHeight,
            new Array[Int](sliceSize))
          implicitly[ArrayImageMapping[T]].fromIntArr(gi)
        case 24 => //color images
          val ggb = convertTo2DARGB(bm)
          implicitly[ArrayImageMapping[T]].fromARGBArr(ggb)
        case 32 => //float
          val gf = activeRaster.getPixels(0, 0, activeRaster.getWidth, activeRaster.getHeight,
            new Array[Float](sliceSize))
          implicitly[ArrayImageMapping[T]].fromFloatArr(gf)
      }
    }

    def as2DArray[T : ArrayImageMapping]: Array[Array[T]] = {
      val width = bm.getWidth
      val height = bm.getHeight
      implicit val ct = implicitly[ArrayImageMapping[T]].ct
      val result = Array.ofDim[T](height,width)
      var pixel: Int = 0
      var row: Int = 0
      var col: Int = 0
      val flatArray = asArray[T]
      while (pixel < flatArray.length) {
        result(row)(col) = flatArray(pixel)
        col += 1
        pixel += 1
        if (col == width) {
          col = 0
          row += 1
        }
      }
      result
    }

    def asNDImage() = {
      val arr1 = Nd4j.create(Array[Float](1,2,3,4),Array(2,2))
    }

  }


  /**
   * @note taken from http://stackoverflow.com/questions/6524196/java-get-pixel-array-from-image
   * @param image
   * @return
   */
  private def convertTo2DARGB(image: BufferedImage): Array[Array[Byte]] = {
    val pixels: Array[Byte] = (image.getRaster.getDataBuffer.asInstanceOf[DataBufferByte]).getData
    val width = image.getWidth
    val height = image.getHeight
    val hasAlphaChannel = image.getAlphaRaster != null

    val result = Array.ofDim[Byte](height*width,4)

    if (hasAlphaChannel) {
      val pixelLength: Int = 4
        var pixel: Int = 0
        var row: Int = 0
        var col: Int = 0
        while (pixel < pixels.length) {
          {

            val alpha = (pixels(pixel).toInt & 0xff)
            val blue = (pixels(pixel+1).toInt & 0xff)
            val green = (pixels(pixel + 2).toInt & 0xff)
            val red = (pixels(pixel + 3).toInt & 0xff)

            result(col)(ALPHA_POS) = alpha.toByte
            result(col)(RED_POS) = red.toByte
            result(col)(GREEN_POS) = green.toByte
            result(col)(BLUE_POS) = blue.toByte

            col += 1
          }
          pixel += pixelLength
        }
    } else {
      val pixelLength: Int = 3
        var pixel: Int = 0
        var row: Int = 0
        var col: Int = 0
        while (pixel < pixels.length) {
          {
            var argb: Int = 0
            val blue = (pixels(pixel).toInt & 0xff)
            val green = (pixels(pixel + 1).toInt & 0xff)
            val red = (pixels(pixel + 2).toInt & 0xff)

            result(col)(ALPHA_POS) = 0
            result(col)(RED_POS) = red.toByte
            result(col)(GREEN_POS) = green.toByte
            result(col)(BLUE_POS) = blue.toByte
            col += 1
          }
          pixel += pixelLength
        }
    }
    result
  }

  /**
   * Convert an array to an image using the ArrayImageMapping helper
   * @param inArr
   * @tparam B
   * @return
   */
  def fromArrayToImage[@specialized(Double, Char, Boolean)
  B : ArrayImageMapping](inArr: Array[Array[B]]) = {
    val (xdim,ydim) = (inArr(0).length,inArr.length)
    val bm = new BufferedImage(xdim,ydim,BufferedImage.TYPE_BYTE_GRAY)
    val cRaster = bm.getRaster()
    cRaster.getDataBuffer() match {
      case dbb: DataBufferByte =>
        val rawArray = dbb.getData()
        var i = 0
        while(i < ydim) {
          System.arraycopy(implicitly[ArrayImageMapping[B]].toByteArr(inArr(i)),0,
            rawArray,i*xdim,xdim)
          i+=1
        }
      case _ =>
        throw new RuntimeException("Not supported")
    }
    bm
  }




}
