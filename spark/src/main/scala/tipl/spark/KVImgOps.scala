/**
  * A series of tools for processing with KVImg objects, including the required spread and collect
  * functions which extend the RDD directly
  */
package tipl.spark

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import tipl.util.TImgTools
import tipl.formats.PureFImage
import tipl.util.D3int
import tipl.tools.BaseTIPLPluginIn
import tipl.formats.TImgRO
import tipl.util.TIPLOps.NeighborhoodOperation
import java.lang.{Double => JDouble}

import scala.reflect.ClassTag

/**
  * Some tools for the Key-value pair image to make working with them easier,
  * writing code in java is just too much of a pain in the ass
  * @author mader
  *
  */
object KVImgOps extends Serializable {
  def createFromPureFun(sc: SparkContext, objToMirror: TImgTools.HasDimensions,
    inpf: PureFImage.PositionFunction): KVImg[Double] = {
    val objDim = objToMirror.getDim
    val objPos = objToMirror.getPos
    val xrng = objPos.x until (objPos.x + objDim.x)
    val yrng = objPos.y until (objPos.y + objDim.y)
    val zrng = objPos.z until (objPos.z + objDim.z)
    val wrappedImage = sc.parallelize(zrng). // parallelize over the slices
      flatMap(z => {
        for (x <- xrng; y <- yrng) // for each slice create each point in the image
          yield (new D3int(x, y, z),
          inpf.get(Array[JDouble](x.doubleValue, y.doubleValue, z.doubleValue)))
      })

    KVImg.fromRDD[Double](objToMirror, TImgTools.IMAGETYPE_DOUBLE, wrappedImage)
  }

  /**
    * Go directly from a PositionFunction to a KVImg
    */
  implicit class RichPositionFunction(inpf: PureFImage.PositionFunction) {
    def asKVImg(sc: SparkContext, objToMirror: TImgTools.HasDimensions): KVImg[Double] = {
      createFromPureFun(sc, objToMirror, inpf)
    }
  }

  trait ExtraImplicits extends Serializable {
    /**
      * These implicits create conversions from a value for which an implicit Numeric
      * exists to the inner class which creates infix operations.  Once imported, you
      * can write methods as follows:
      * {{{
      *  def plus[T: WellSupportedType](x: T, y: T) = x + y
      * }}}
      */
    implicit def infixWSTOps[T](x: T)(implicit num: WellSupportedType[T]):
    WellSupportedType[T]#Ops = new num.Ops(x)

    implicit def infixNumericWSTOps[T](x: T)(implicit num: NumericWellSupportedType[T]):
    NumericWellSupportedType[T]#Ops = new num.Ops(x)
  }

  /**
    * A type with the standard operations supported
    * @tparam T
    */
  trait WellSupportedType[T] extends Serializable {
    def plus(a: T, b: T): T

    def times(a: T, b: T): T

    def negate(a: T): T

    def invert(a: T): T

    def abs(a: T): T

    def minus(a: T, b: T): T = plus(a, negate(b))

    def divide(a: T, b: T): T = times(a, invert(b))

    // operations to make repl usage easier
    class Ops(lhs: T) {
      def +(rhs: T) = plus(lhs, rhs)

      def -(rhs: T) = minus(lhs, rhs)

      def *(rhs: T) = times(lhs, rhs)

      def unary_-() = negate(lhs)

      def abs(): T = WellSupportedType.this.abs(lhs)
    }

    implicit def mkWSTOps(lhs: T): Ops = new Ops(lhs)
  }

  /**
    * An extension of wellsupportedtype that is a number of some kind (can be synthesized from
    * primitives, etc)
    * @tparam T
    */
  trait NumericWellSupportedType[T] extends WellSupportedType[T] with Ordering[T] {
    // standard conversions
    def toDouble(a: T): Double

    def toLong(a: T): Long

    // default conversions
    def toFloat(a: T): Float = toDouble(a).toFloat

    def toInt(a: T): Int = toLong(a).toInt

    def abs(a: T) = if (a > negate(a)) a else negate(a)

    def fromDouble(a: Double): T

    def fromLong(a: Long): T

    /**
      * A more useful definition of divide from standard types
      */
    override def divide(a: T, b: T): T = fromDouble(a.toDouble / b.toDouble)

    class CreateOps(lhs: T) {
      def toInt(): Int = NumericWellSupportedType.this.toInt(lhs)

      def toLong(): Long = NumericWellSupportedType.this.toLong(lhs)

      def toFloat(): Float = NumericWellSupportedType.this.toFloat(lhs)

      def toDouble(): Double = NumericWellSupportedType.this.toDouble(lhs)
    }

    implicit def mkNumericOps(lhs: T): CreateOps = new CreateOps(lhs)

  }

  trait IntIsWST extends NumericWellSupportedType[Int] {
    def plus(a: Int, b: Int): Int = a + b

    def times(a: Int, b: Int): Int = a * b

    def negate(a: Int) = -a

    def invert(a: Int) = 1 / a

    // numeric commands

    def toDouble(a: Int): Double = a.toDouble

    def toLong(a: Int): Long = a.toLong

    def fromDouble(a: Double): Int = a.toInt

    def fromLong(a: Long): Int = a.toInt

  }

  implicit object IntIsWST extends IntIsWST with Ordering.IntOrdering

  trait ShortIsWST extends NumericWellSupportedType[Short] {
    def plus(a: Short, b: Short): Short = (a + b).toShort

    def times(a: Short, b: Short): Short = (a * b).toShort

    def negate(a: Short) = (-a).toShort

    def invert(a: Short) = (1 / a).toShort

    // numeric commands

    def toDouble(a: Short): Double = a.toDouble

    def toLong(a: Short): Long = a.toLong

    def fromDouble(a: Double): Short = a.toShort

    def fromLong(a: Long): Short = a.toShort
  }

  implicit object ShortIsWST extends ShortIsWST with Ordering.ShortOrdering

  trait LongIsWST extends NumericWellSupportedType[Long] {
    def plus(a: Long, b: Long): Long = a + b

    def times(a: Long, b: Long): Long = a * b

    def negate(a: Long) = -a

    def invert(a: Long) = 1 / a

    // numeric commands

    def toDouble(a: Long): Double = a.toDouble

    def toLong(a: Long): Long = a.toLong

    def fromDouble(a: Double): Long = a.toLong

    def fromLong(a: Long): Long = a.toLong
  }

  implicit object LongIsWST extends LongIsWST with Ordering.LongOrdering

  trait FloatIsWST extends NumericWellSupportedType[Float] {
    def plus(a: Float, b: Float): Float = a + b

    def times(a: Float, b: Float): Float = a * b

    def negate(a: Float) = -a

    def invert(a: Float) = 1 / a

    // numeric commands

    def toDouble(a: Float): Double = a.toDouble

    def toLong(a: Float): Long = a.toLong

    def fromDouble(a: Double): Float = a.toFloat

    def fromLong(a: Long): Float = a.toFloat
  }

  implicit object FloatIsWST extends FloatIsWST with Ordering.FloatOrdering

  trait DoubleIsWST extends NumericWellSupportedType[Double] {
    def plus(a: Double, b: Double): Double = a + b

    def times(a: Double, b: Double): Double = a * b

    def negate(a: Double) = -a

    def invert(a: Double) = 1 / a

    // numeric commands

    def toDouble(a: Double): Double = a.toDouble

    def toLong(a: Double): Long = a.toLong

    def fromDouble(a: Double): Double = a.toDouble

    def fromLong(a: Long): Double = a.toDouble
  }

  implicit object DoubleIsWST extends DoubleIsWST with Ordering.DoubleOrdering

  def rddMergeCommand[A, B](a: RDD[(D3int, A)], b: RDD[(D3int, A)], mergeOp: ((A, A) => B),
    stdOp: (A => B))(implicit cls: ClassTag[A], clsb: ClassTag[B]): RDD[(D3int, B)] = {
    //TODO this is fine for transitive operations but for division and subtraction it could be
    // tricky
    val combined = a.union(b).groupByKey()
    val singleElements = combined.filter(_._2.size == 1).mapValues(inval => stdOp(inval.head))
    val doubleElements = combined.filter(_._2.size == 2).mapValues { invals =>
      mergeOp(invals
        .head, invals.tail.head)
    }
    singleElements.union(doubleElements)
  }

  trait WellSupportedKVRDD[B, A] extends WellSupportedType[RDD[(B, A)]] {
    val ncls: WellSupportedType[A]
    implicit val cta: ClassTag[A]
    implicit val ctb: ClassTag[B]

    private def mergeRdd(a: RDD[(D3int, A)], b: RDD[(D3int, A)], mergeOp: ((A, A) => A)) =
      rddMergeCommand[A, A](a, b, mergeOp, ((iv: A) => iv))

    def plus(a: RDD[(D3int, A)], b: RDD[(D3int, A)]) = mergeRdd(a, b, ncls.plus)

    def times(a: RDD[(D3int, A)], b: RDD[(D3int, A)]) = mergeRdd(a, b, ncls.times)

    def negate(a: RDD[(D3int, A)]) = a.mapValues(ncls.negate(_))

    def invert(a: RDD[(D3int, A)]) = a.mapValues(ncls.invert(_))

    def abs(a: RDD[(D3int, A)]) = a.mapValues(ncls.abs(_))
  }

  implicit class RichKvRDD[A](srd: RDD[(D3int, A)])(implicit incls: WellSupportedType[A],
    icta: ClassTag[A], ictb: ClassTag[D3int])
    extends NeighborhoodOperation[(A, Boolean), A] with WellSupportedKVRDD[D3int, A] {
    val ncls = incls
    val cta = icta
    val ctb = ictb

    /**
      * A generic voxel spread function for a given window size and kernel
      *
      */
    val spread_voxels = (windSize: D3int, kernel: Option[BaseTIPLPluginIn.morphKernel]) => {
      (pvec: (D3int, A)) =>
        {
          val pos = pvec._1

          val windx = -windSize.x to windSize.x
          val windy = -windSize.y to windSize.y
          val windz = -windSize.z to windSize.z
          for (
            x <- windx;
            y <- windy;
            z <- windz
            if kernel.map(_.inside(0, 0, pos.x, pos.x + x, pos.y, pos.y + y, pos.z,
              pos.z + z)).getOrElse(true)
          ) yield (new D3int(pos.x + x, pos.y + y, pos.z + z),
            (pvec._2, x == 0 & y == 0 & z == 0))
        }
    }

    def spreadPoints(windSize: D3int, kernel: Option[BaseTIPLPluginIn.morphKernel]):
    RDD[(D3int, Iterable[(A, Boolean)])] = {
      srd.flatMap(spread_voxels(windSize, kernel)).groupByKey
    }

    def blockOperation(windSize: D3int, kernel: Option[BaseTIPLPluginIn.morphKernel],
      mapFun: (Iterable[(A, Boolean)] => A)): RDD[(D3int, A)] = {
      val spread = spreadPoints(windSize, kernel).collectPoints(mapFun)
      spread
    }
  }

  implicit class NumericRichKvRDD[A](srd: RDD[(D3int, A)])
                                    (implicit incls: NumericWellSupportedType[A], icta: ClassTag[A])
    extends RichKvRDD[A](srd) {
    def >(threshVal: A): RDD[(D3int, Boolean)] = srd.mapValues {
      incls.gt(_, threshVal)
    }

    def <(threshVal: A): RDD[(D3int, Boolean)] = srd.mapValues {
      incls.lt(_, threshVal)
    }

    def ==(threshVal: A): RDD[(D3int, Boolean)] = srd.mapValues {
      incls.equiv(_, threshVal)
    }

    def +(ival: A) = srd.mapValues(incls.plus(_, ival))

    def -(ival: A) = srd.mapValues(incls.minus(_, ival))

    def *(ival: A) = srd.mapValues(incls.times(_, ival))

    def /(ival: A) = srd.mapValues(incls.divide(_, ival))

    /**
      * A better implemented division functions using double as an intermediate step
      */
    override def divide(a: RDD[(D3int, A)], b: RDD[(D3int, A)]) =
      rddMergeCommand[Double, A](a.mapValues(incls.toDouble(_)), b.mapValues(incls.toDouble(_)),
        (u: Double, v: Double) => incls.fromDouble(u * v), incls.fromDouble)
  }

  /**
    * A class of a spread RDD image (after a flatMap/spread operation)
    */
  implicit class SpreadRDD[A](srd: RDD[(D3int, Iterable[(A, Boolean)])])
                             (implicit ncls: WellSupportedType[A]) {
    def collectPoints(coFun: (Iterable[(A, Boolean)] => A)) = {
      srd.mapValues(coFun)
    }
  }

  /** create a KVImage with only points above a certain value **/
  def TImgROToKVThresh(sc: SparkContext, inImg: TImgRO, threshold: Double) = {
    val imgDim = inImg.getDim
    val imgPos = inImg.getPos
    val kvRdd = sc.parallelize(0 until imgDim.z).flatMap {
      cSlice =>
        val cSliceArr = inImg.getPolyImage(cSlice, TImgTools.IMAGETYPE_DOUBLE)
          .asInstanceOf[Array[Double]]
        for {
          y <- 0 until imgDim.y
          x <- 0 until imgDim.x
          oVal = cSliceArr(y * imgDim.x + x)
          if oVal > threshold
        } yield (new D3int(imgPos.x + x, imgPos.y + y, imgPos.z + cSlice), oVal)
    }
    new KVImg[Double](inImg, TImgTools.IMAGETYPE_DOUBLE, kvRdd,0.0)
  }

  // vector field support

  import org.apache.spark.mllib.linalg.{ Vector, Vectors }

  trait VectorIsWST extends WellSupportedType[Vector] {
    def plus(a: Vector, b: Vector): Vector = a + b

    def times(a: Vector, b: Vector): Vector = a * b

    def negate(a: Vector) = -a

    def invert(a: Vector) = {
      val nData = a.toArray
      var i = 0;
      while (i < nData.length) {
        nData(i) = 1 / nData(i)
        i += 1
      }
      Vectors.dense(nData)
    }

    def abs(a: Vector): Vector = {
      val nData = a.toArray
      var i = 0;
      while (i < nData.length) {
        nData(i) = math.abs(nData(i))
        i += 1
      }
      Vectors.dense(nData)
    }
  }

  implicit object VectorIsWST extends VectorIsWST

}