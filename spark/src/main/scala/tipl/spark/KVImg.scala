/**
 *
 */
package tipl.spark

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import scala.reflect.ClassTag
import tipl.util.D3int
import tipl.util.D3float
import tipl.util.TImgTools
import tipl.formats.TImgRO
import tipl.formats.TImg


/**
 * A KV Pair image where the key is the position and the value is the value
 * @author mader
 *
 */
class KVImg[T](dim: D3int, pos: D3int, elSize: D3float, imageType: Int, baseImg: RDD[(D3int, T)])(implicit lm: ClassTag[T])
  extends TImg.ATImg(dim, pos, elSize, imageType) with TImg {

  def this(inImg: TImgTools.HasDimensions, imageType: Int, baseImg: RDD[(D3int, T)])(implicit lm: ClassTag[T]) =
    this(inImg.getDim, inImg.getPos, inImg.getElSize, imageType, baseImg)(lm)


  def getBaseImg() = baseImg

  /* The function to collect all the key value pairs and return it as the appropriate array for a given slice
 * @see tipl.formats.TImgRO#getPolyImage(int, int)
 */
  override def getPolyImage(sliceNumber: Int, asType: Int): Object = {
    assert(TImgTools.isValidType(asType))
    val sliceSize = dim.x * dim.y

    val sliceAsList = baseImg.filter(_._1.z == (sliceNumber + pos.z)).map {
      curElement: (D3int, T) =>
        ((curElement._1.y - pos.y) * dim.x + curElement._1.x - pos.x, curElement._2);
    }.sortByKey(true)
    val tSlice = (0 until sliceSize).zip(new Array[T](sliceSize))
    // since particularly after thresholds many points are missing, we need to add them back before making a slice out of the data
    val allPoints = baseImg.sparkContext.parallelize(tSlice)
    val missingPoints = allPoints.subtractByKey(sliceAsList)
    val fixedList = sliceAsList.union(missingPoints)
    // convert this array into the proper output format
    TImgTools.convertArrayType(fixedList.map(_._2).collect(), imageType, asType, getSigned(), getShortScaleFactor())
  }

  /* (non-Javadoc)
* @see tipl.formats.TImgRO#getSampleName()
*/

  override def getSampleName() = baseImg.name

  override def inheritedAim(inImg: TImgRO): TImg = {
    val outImage = KVImg.ConvertTImg(baseImg.sparkContext, inImg, inImg.getImageType())
    outImage.appendProcLog("Merged with:" + getSampleName() + ":" + this + "\n" + getProcLog())
    outImage
  }


  /**
   * for manually specifying conversion functions
   */
  private[KVImg] def toKVType[V](newImageType: Int, convFunc: (T => V))(implicit gv: ClassTag[V]) = {
    new KVImg[V](this, newImageType, baseImg.mapValues(convFunc))
  }

  /**
   * for using the automatic functions
   */
  private[KVImg] def toKVAuto[V](newImageType: Int)(implicit gv: ClassTag[V]) = {
    new KVImg[V](this, newImageType, baseImg.mapValues(KVImg.makeConvFunc[V](imageType, newImageType)))
  }

  def toKVLong() =
    toKVAuto[Long](TImgTools.IMAGETYPE_LONG)
  def toKVBoolean() =
    toKVAuto[Boolean](TImgTools.IMAGETYPE_BOOL)
  def toKVFloat() =
    toKVAuto[Float](TImgTools.IMAGETYPE_FLOAT)
   def toKVDouble() =
    toKVAuto[Double](TImgTools.IMAGETYPE_DOUBLE)
    
   
   def saveAsParquetFile(path: String) = {
    import org.apache.spark.sql.SQLContext
    val sqlContext = new SQLContext(baseImg.sparkContext)
    // first convert the image to a double (it makes it easier for now, since otherwise sqlcontext goes crazy with javamirrors and type tags and all that
    val schemaTab = sqlContext.createSchemaRDD(toKVDouble.getBaseImg.map{inRow => KVImg.KVImgRowGeneric(inRow._1.x,inRow._1.y,inRow._1.z,inRow._2)})
    schemaTab.saveAsParquetFile(path)
  }


}


object KVImg {
  case class KVImgRowGeneric(x: Int, y: Int, z: Int, value: Double) extends Serializable

  def ConvertTImg(sc: SparkContext, inImg: TImgRO, imType: Int) = {
    val curImg = DTImg.ConvertTImg(sc, inImg, imType)
    new KVImg(inImg, imType, DTImgOps.DTImgToKVRDD(curImg))
  }
  
  /** Load a parquet file
   *  
   */
   def loadFromParquetFile(path: String, elSize: D3float = new D3float(1.0f)):
	   KVImg[Double] = {
    import org.apache.spark.sql.SQLContext
    val sqlContext = new SQLContext(SparkGlobal.getContext("KVImg Loading Pqrquet"))
    val schemaTab = sqlContext.parquetFile(path)
    val kvTab = schemaTab.map{ curRow => 
      val cPos = new D3int(curRow.getInt(0),curRow.getInt(1),curRow.getInt(2))
      (cPos,curRow.getDouble(3))
      }
    val (pos,dim) = inferShape(kvTab.map(_._1))
    new KVImg(dim, pos, elSize, TImgTools.IMAGETYPE_DOUBLE,kvTab)
  }
   /**
    * Infer the shape from a key-value pair RDD
    */
   def inferShape(fMat: RDD[D3int]) = {
     val posx = fMat.map(_.x).min
     val posy = fMat.map(_.y).min
     val posz = fMat.map(_.z).min
     val dimx = fMat.map(_.x).max-posx
     val dimy = fMat.map(_.y).max-posy
     val dimz = fMat.map(_.z).max-posz
     (new D3int(posx,posy,posz),new D3int(dimx,dimy,dimz))
   }

  /**
   * Transform the DTImg into a KVImg
   */
  def fromDTImg[T, V](inImg: DTImg[T])(implicit lm: ClassTag[T], gm: ClassTag[V]): KVImg[V] = {
	  DTImgOps.DTImgToKVStrict[T, V](inImg)
  }

  def fromDTImgBlind(inImg: DTImg[_]) = inImg.getImageType() match {
    case TImgTools.IMAGETYPE_BOOL => DTImgOps.DTImgToKVStrict(inImg.asInstanceOf[DTImg[Array[Boolean]]])
    case TImgTools.IMAGETYPE_CHAR => DTImgOps.DTImgToKVStrict(inImg.asInstanceOf[DTImg[Array[Byte]]])
    case TImgTools.IMAGETYPE_SHORT => DTImgOps.DTImgToKVStrict(inImg.asInstanceOf[DTImg[Array[Short]]])
    case TImgTools.IMAGETYPE_INT => DTImgOps.DTImgToKVStrict(inImg.asInstanceOf[DTImg[Array[Int]]])
    case TImgTools.IMAGETYPE_LONG => DTImgOps.DTImgToKVStrict(inImg.asInstanceOf[DTImg[Array[Long]]])
    case TImgTools.IMAGETYPE_FLOAT => DTImgOps.DTImgToKVStrict(inImg.asInstanceOf[DTImg[Array[Float]]])
    case TImgTools.IMAGETYPE_DOUBLE => DTImgOps.DTImgToKVStrict(inImg.asInstanceOf[DTImg[Array[Double]]])
    case m: Int => throw new IllegalArgumentException("Unknown type:" + m)
  }

  def fromRDD[T](objToMirror: TImgTools.HasDimensions, imageType: Int, wrappedImage: RDD[(D3int, T)])(implicit lm: ClassTag[T]) = {
    new KVImg[T](objToMirror, imageType, wrappedImage)
  }

  private[KVImg] def makeConvFunc[T](inType: Int, outType: Int) = {
    inVal: Any => TypeMacros.fromDouble(TypeMacros.castEleToDouble(inVal, inType), outType).asInstanceOf[T]
  }
  

}