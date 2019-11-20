/**
 *
 */
package tipl.spark

import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.distributed.{CoordinateMatrix, DistributedMatrix, MatrixEntry}
import org.apache.spark.rdd.RDD
import tipl.formats.{TImg, TImgRO}
import tipl.tools.TypedSliceLookup
import tipl.util._

import scala.reflect.ClassTag
import scala.{specialized => spec}


/**
 * A KV Pair image where the key is the position and the value is the value
 *
 * @author mader
 * @param dim
 * @param pos
 * @param elSize
 * @param imageType
 * @param baseImg
 * @param paddingVal the value to assign points removed from the image
 * @param lm         needed for making arrays of that type to implement typedslicelookup
 * @tparam T
 */
class KVImg[@spec(Boolean, Byte, Short, Int, Long, Float, Double) T](dim: D3int, pos: D3int,
                                                                     elSize: D3float,
                                                                     imageType: Int,
                                                                     baseImg: RDD[(D3int,
                                                                       T)], paddingVal: T)(
                                                                      implicit lm: ClassTag[T])
  extends TImg.ATImg(dim, pos, elSize, imageType) with TImg with TypedSliceLookup[T] {

  /**
   * Direct conversion from TImgRO data
   *
   * @param sc
   * @param cImg
   * @param imageType
   * @param lm
   * @return
   */
  def this(sc: SparkContext, cImg: TImgRO, imageType: Int, paddingVal: T)(implicit lm: ClassTag[T]) =
    this(cImg, imageType, KVImg.TImgToKVRdd[T](sc, cImg, imageType), paddingVal)(lm)

  def map[U: ClassTag](f: ((D3int, T)) => (D3int, U), newPaddingVal: U,
                       newDim: D3int = dim, newPos:
                       D3int = pos, newElSize: D3float = elSize, newImageType: Int = imageType): KVImg[U] =
    new KVImg[U](newDim, newPos, newElSize, newImageType, getBaseImg.map(f), newPaddingVal)

  def mapValues[U: ClassTag](f: (T) => U, newImageType: Int = imageType)(implicit
                                                                         nm: Numeric[U]): KVImg[U] =
    new KVImg[U](dim, pos, elSize, newImageType, getBaseImg.mapValues(f), nm.zero)

  def filter(f: ((D3int, T)) => Boolean): KVImg[T] =
    new KVImg[T](dim, pos, elSize, imageType, getBaseImg.filter(f), paddingVal)

  override def getSampleName() = baseImg.name

  @deprecated("this function is not supported in Spark Image Layer", "1.0")
  override def inheritedAim(inImg: TImgRO): TImg =
    throw new IllegalArgumentException("Not a supported function")

  def toKVLong() =
    toKVAuto[Long](TImgTools.IMAGETYPE_LONG)


  /* (non-Javadoc)
* @see tipl.formats.TImgRO#getSampleName()
*/

  def toKVBoolean() =
    toKVAuto[Boolean](TImgTools.IMAGETYPE_BOOL, false)

  /**
   * for using the automatic functions
   */
  private[KVImg] def toKVAuto[V](newImageType: Int, newPaddingVal: V)(implicit gv: ClassTag[V]) = {
    mapValues[V](KVImg.makeConvFunc[V](imageType, newImageType),
      newPaddingVal = newPaddingVal,
      newImageType = newImageType)
  }

  def toKVFloat() =
    toKVAuto[Float](TImgTools.IMAGETYPE_FLOAT)

  def saveAsParquetFile(path: String) = {
    import org.apache.spark.sql.SQLContext
    val sqlContext = new SQLContext(baseImg.sparkContext)
    // first convert the image to a double (it makes it easier for now,
    // since otherwise sqlcontext goes crazy with javamirrors and type tags and all that
    val schemaTab = sqlContext.createDataFrame(toKVDouble.getBaseImg.map { inRow =>
      KVImg
        .KVImgRowGeneric(inRow._1.x, inRow._1.y, inRow._1.z, inRow._2)
    })
    schemaTab.saveAsParquetFile(path)
  }

  def getBaseImg() = baseImg

  def toKVDouble() =
    toKVAuto[Double](TImgTools.IMAGETYPE_DOUBLE)

  /**
   * for using the automatic functions
   *
   * @note if it is numeric support it automatically
   */
  private[KVImg] def toKVAuto[V](newImageType: Int)(implicit gv: ClassTag[V], nm: Numeric[V]) = {
    mapValues[V](KVImg.makeConvFunc[V](imageType, newImageType),
      newPaddingVal = nm.zero,
      newImageType = newImageType)
  }

  def mapValues[U: ClassTag](f: (T) => U, newPaddingVal: U, newImageType: Int = imageType): KVImg[U] =
    new KVImg[U](dim, pos, elSize, newImageType, getBaseImg.mapValues(f), newPaddingVal)

  /**
   * Create a numeric KVImg out of the current image which supports more operations based on the
   * tools implemented in numeric
   *
   * @param nm the automatically filled in numeric class for the given type
   * @return a @NumericKVImg
   */
  def toNumeric(implicit nm: Numeric[T]) = new NumericKVImg[T](dim, pos, elSize, imageType, baseImg)

  /**
   * @note this will always return a slice, even if the result is outside the bounds of the image
   */
  override def getSlice(sliceNum: Int): Option[Array[T]] = {
    val sliceSize = dim.x * dim.y

    val sliceAsList = baseImg.filter(_._1.z == (sliceNum + pos.z)).map {
      curElement: (D3int, T) =>
        ((curElement._1.y - pos.y) * dim.x + curElement._1.x - pos.x, curElement._2);
    }.sortByKey(true).collect
    val outSlice = Array.fill[T](sliceSize)(paddingVal)
    for (cv <- sliceAsList) outSlice(cv._1) = cv._2
    Some(outSlice)
  }

  /**
   * for manually specifying conversion functions
   */
  private[KVImg] def toKVType[V](newImageType: Int, convFunc: (T => V), paddingVal: V)(
    implicit gv: ClassTag[V]) = {
    new KVImg[V](this, newImageType, baseImg.mapValues(convFunc), paddingVal)
  }

  def this(inImg: TImgTools.HasDimensions, imageType: Int, baseImg: RDD[(D3int,
    T)], paddingVal: T)(implicit lm: ClassTag[T]) =
    this(inImg.getDim, inImg.getPos, inImg.getElSize, imageType, baseImg, paddingVal)(lm)
}

/**
 * A KVImg where a known Numeric class exists
 *
 * @param nm
 * @tparam T
 */
class NumericKVImg[T](dim: D3int, pos: D3int, elSize: D3float, imageType: Int,
                      baseImg: RDD[(D3int, T)])(implicit lm: ClassTag[T], nm: Numeric[T]) extends
  KVImg[T](dim, pos, elSize, imageType, baseImg, nm.zero)(lm) with SliceableImage[D3int] {

  def toDouble() = new NumericKVImg[Double](dim, pos, elSize, TImgTools.IMAGETYPE_DOUBLE,
    baseImg.mapValues(nm.toDouble(_)))

  def toLong() = new NumericKVImg[Long](dim, pos, elSize, TImgTools.IMAGETYPE_LONG,
    baseImg.mapValues(nm.toLong(_)))

  override def getNSlice(gDimA: Int, gDimB: Int, gInd: D3int): Option[DistributedMatrix] = {
    val filtFun = SlicingOps.getMissing3DIndex(gDimA, gDimB) match {
      case Some(ind) => Some(SlicingOps.keyFilter[T](gInd, ind))
      case _ => None
    }
    val mapFun = SlicingOps.kvPairToMatrixEntry[T](gDimA, gDimB, gInd)

    (filtFun, mapFun) match {
      case (Some(filtOp), Some(mapOp)) =>
        Some(
          new CoordinateMatrix(
            baseImg.filter(filtOp).map(mapOp)
          )
        )
      case _ =>
        println("Axes combination " + (gDimA, gDimB) + " not supported")
        None
    }
  }

  private def asSliceArray(gDimA: Int, gDimB: Int, gInd: D3int) = {
    //TODO implement the rest of this function / decide what it is useful for
    (gDimA, gDimB) match {
      case (D3int.AXIS_X, D3int.AXIS_Y) =>
        Some(
          baseImg.map {
            case (spos, sval) =>
              (spos.gz - spos.gz,
                MatrixEntry(spos.gx - gInd.gx, spos.gy - gInd.gy, nm.toDouble(sval))
              )
          }
        )
      case _ =>
        println("Axes combination " + (gDimA, gDimB) + " not supported")
        None
    }
  }
}


object KVImg {


  def ConvertTImg[T: ClassTag](sc: SparkContext, inImg: TImgRO, imType: Int, paddingVal: T) =
    new KVImg[T](sc, inImg, imType, paddingVal)

  /** Load a parquet file
   *
   */
  def loadFromParquetFile(path: String, elSize: D3float = new D3float(1.0f)):
  KVImg[Double] = {
    import org.apache.spark.sql.SQLContext
    val sqlContext = new SQLContext(SparkGlobal.getContext("KVImg Loading Pqrquet"))
    val schemaTab = sqlContext.parquetFile(path)
    val kvTab = schemaTab.map { curRow =>
      val cPos = new D3int(curRow.getInt(0), curRow.getInt(1), curRow.getInt(2))
      (cPos, curRow.getDouble(3))
    }
    val (pos, dim) = inferShape(kvTab.map(_._1))
    new KVImg(dim, pos, elSize, TImgTools.IMAGETYPE_DOUBLE, kvTab, 0.0)
  }

  /**
   * Infer the shape from a key-value pair RDD
   */
  def inferShape(fMat: RDD[D3int]) = {
    val posx = fMat.map(_.x).min
    val posy = fMat.map(_.y).min
    val posz = fMat.map(_.z).min
    val dimx = fMat.map(_.x).max - posx
    val dimy = fMat.map(_.y).max - posy
    val dimz = fMat.map(_.z).max - posz
    (new D3int(posx, posy, posz), new D3int(dimx, dimy, dimz))
  }

  /**
   * Transform the DTImg into a KVImg
   */
  def fromDTImg[T, V](inImg: DTImg[T])(implicit lm: ClassTag[T], gm: ClassTag[V], nm: Numeric[V]):
  KVImg[V] = fromDTImg(inImg, nm.zero)

  def fromDTImg[T, V](inImg: DTImg[T], paddingVal: V)(implicit lm: ClassTag[T], gm: ClassTag[V]):
  KVImg[V] = {
    DTImgOps.DTImgToKVStrict[T, V](inImg, paddingVal)
  }

  def fromDTImgBlind(inImg: DTImg[_]) = inImg.getImageType() match {
    case TImgTools.IMAGETYPE_BOOL => DTImgOps.DTImgToKVStrict(inImg
      .asInstanceOf[DTImg[Array[Boolean]]], false)
    case TImgTools.IMAGETYPE_CHAR => DTImgOps.DTImgToKVStrict(inImg
      .asInstanceOf[DTImg[Array[Byte]]], 0)
    case TImgTools.IMAGETYPE_SHORT => DTImgOps.DTImgToKVStrict(inImg
      .asInstanceOf[DTImg[Array[Short]]], 0)
    case TImgTools.IMAGETYPE_INT => DTImgOps.DTImgToKVStrict(inImg
      .asInstanceOf[DTImg[Array[Int]]], 0)
    case TImgTools.IMAGETYPE_LONG => DTImgOps.DTImgToKVStrict(inImg
      .asInstanceOf[DTImg[Array[Long]]], 0L)
    case TImgTools.IMAGETYPE_FLOAT => DTImgOps.DTImgToKVStrict(inImg
      .asInstanceOf[DTImg[Array[Float]]], 0f)
    case TImgTools.IMAGETYPE_DOUBLE => DTImgOps.DTImgToKVStrict(inImg
      .asInstanceOf[DTImg[Array[Double]]], 0)
    case m: Int => throw new IllegalArgumentException("Unknown type:" + m)
  }

  def fromRDD[T](objToMirror: TImgTools.HasDimensions, imageType: Int,
                 wrappedImage: RDD[(D3int, T)], paddingVal: T)(implicit lm: ClassTag[T]) = {
    new KVImg[T](objToMirror, imageType, wrappedImage, paddingVal)
  }

  def fromRDD[T](objToMirror: TImgTools.HasDimensions, imageType: Int,
                 wrappedImage: RDD[(D3int, T)])(implicit lm: ClassTag[T], nm: Numeric[T]) = {
    new KVImg[T](objToMirror, imageType, wrappedImage, nm.zero)
  }

  private[spark] def TImgToKVRdd[T](sc: SparkContext, inImg: TImgRO,
                                    imType: Int)(implicit lm: ClassTag[T]) = {
    new DSImg[T](sc, inImg, imType).getBaseImg().
      flatMap {
        cPoint =>
          val pos = cPoint._1
          val dim = new D3int(cPoint._2.getDim, 1)
          val curSlice = cPoint._2.get
          for {z <- 0 until dim.z
               y <- 0 until dim.y
               x <- 0 until dim.x
               }
            yield (new D3int(pos.x + x, pos.y + y, pos.z + z), curSlice((z * dim.y + y) * dim.x + x))
      }
  }

  private[KVImg] def makeConvFunc[T](inType: Int, outType: Int) = {
    inVal: Any =>
      TypeMacros.fromDouble(TypeMacros.castEleToDouble(inVal, inType),
        outType).asInstanceOf[T]
  }

  case class KVImgRowGeneric(x: Int, y: Int, z: Int, value: Double) extends Serializable

}

