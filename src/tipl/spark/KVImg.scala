/**
 *
 */
package tipl.spark

import tipl.formats.TImg
import tipl.util.D3float
import tipl.util.D3int
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.PairRDDFunctions._
import scala.reflect.ClassTag
import tipl.util.TImgTools
import tipl.formats.TImgRO

/**
 * A KV Pair image where the key is the position and the value is the value
 * @author mader
 *
 */
@serializable class KVImg[T](dim: D3int, pos: D3int, elSize: D3float, imageType: Int,baseImg: RDD[(D3int,T)])(implicit lm: ClassTag[T]) 
		extends TImg.ATImg(dim,pos,elSize,imageType) with TImg {
  
	def this(inImg: TImgTools.HasDimensions, imageType: Int, baseImg: RDD[(D3int,T)])(implicit lm: ClassTag[T]) = 
	  this(inImg.getDim,inImg.getPos,inImg.getElSize,imageType,baseImg)(lm)
  def getBaseImg() = baseImg
      /* The function to collect all the key value pairs and return it as the appropriate array for a given slice
     * @see tipl.formats.TImgRO#getPolyImage(int, int)
     */
    override def getPolyImage(sliceNumber: Int, asType: Int): Object = {
        assert (TImgTools.isValidType(asType))
        val sliceSize = dim.x * dim.y;
        
        val sliceAsList = baseImg.filter(_._1.z==(sliceNumber+pos.z)).map{
          curElement: (D3int,T) =>
            ((curElement._1.y - pos.y) * dim.x + curElement._1.x - pos.x, curElement._2);
        }.sortByKey(true)
        val tSlice = (0 to sliceSize).zip(new Array[T](sliceSize))
        // since particularly after thresholds many points are missing, we need to add them back before making a slice out of the data
        val allPoints = baseImg.sparkContext.parallelize(tSlice)
        val missingPoints = allPoints.subtractByKey(sliceAsList)
        val fixedList = sliceAsList.union(missingPoints)
        // convert this array into the proper output format
        TImgTools.convertArrayType(fixedList.map(_._2).collect(), imageType, asType, getSigned(), getShortScaleFactor());
      }
        /* (non-Javadoc)
     * @see tipl.formats.TImgRO#getSampleName()
     */
    
    override def getSampleName() = baseImg.name
 
     override def inheritedAim(inImg: TImgRO): TImg = {
        val outImage = KVImg.ConvertTImg(baseImg.sparkContext, inImg, inImg.getImageType());
        outImage.appendProcLog("Merged with:" + getSampleName() + ":" + this + "\n" + getProcLog());
        outImage;
    }
    
    
    /**
     * for manually specifying conversion functions
     */
    private[KVImg] def toKVType[V](newImageType: Int,convFunc: (T => V))(implicit gv: ClassTag[V]) = {
      new KVImg[V](this,newImageType,baseImg.mapValues(convFunc))
    }
    /**
     * for using the automatic functions
     */
    private[KVImg] def toKVAuto[V](newImageType: Int)(implicit gv: ClassTag[V]) = {
      new KVImg[V](this,newImageType,baseImg.mapValues(KVImg.makeConvFunc[V](imageType,newImageType)))
    }
    def toKVLong() = 
      toKVAuto[Long](TImgTools.IMAGETYPE_LONG)
    
    def toKVFloat() = 
      toKVAuto[Float](TImgTools.IMAGETYPE_FLOAT)

    
    
    
}
object KVImg {
  def ConvertTImg(sc: SparkContext, inImg: TImgRO, imType: Int) = {
    fromDTImg(DTImg.ConvertTImg(sc,inImg, imType))
  }
      /**
     * Transform the DTImg into a KVImg
     */
    def fromDTImg[T,V](inImg: DTImg[T]) = {
      val stp = new DTImg.SliceToPoints[T](inImg.getImageType())
      val kvBase = inImg.getBaseImg().flatMapToPair(stp)
      new KVImg[Number](inImg.getDim(), inImg.getPos(), inImg.getElSize(), inImg.getImageType(), kvBase.rdd);

    }
    def FromRDD[T](objToMirror: TImgTools.HasDimensions, imageType: Int, wrappedImage: RDD[(D3int,T)])(implicit lm: ClassTag[T]) = {
      new KVImg[T](objToMirror,imageType,wrappedImage)
    }
    /**
     * converts everything we know about to a double
     */
    private[KVImg] def toDouble(inVal: Any,imType: Int): Double = imType match {
              case TImgTools.IMAGETYPE_BOOL => if (inVal.asInstanceOf[Boolean]) 1.0 else 0.0
              case TImgTools.IMAGETYPE_CHAR => inVal.asInstanceOf[Byte].doubleValue()
              case TImgTools.IMAGETYPE_SHORT => inVal.asInstanceOf[Short].doubleValue()
              case TImgTools.IMAGETYPE_INT => inVal.asInstanceOf[Int].doubleValue()
              case TImgTools.IMAGETYPE_LONG => inVal.asInstanceOf[Long].doubleValue()
              case TImgTools.IMAGETYPE_FLOAT => inVal.asInstanceOf[Float].doubleValue()
              case TImgTools.IMAGETYPE_DOUBLE => inVal.asInstanceOf[Double]
              case _ => throw new IllegalArgumentException(imType+" is not a known image type")
    }
    private[KVImg] def fromDouble(inVal: Double, outType: Int) = outType match {
              case TImgTools.IMAGETYPE_BOOL => inVal>0
              case TImgTools.IMAGETYPE_CHAR => inVal.byteValue
              case TImgTools.IMAGETYPE_SHORT => inVal.shortValue
              case TImgTools.IMAGETYPE_INT => inVal.intValue
              case TImgTools.IMAGETYPE_LONG => inVal.longValue
              case TImgTools.IMAGETYPE_FLOAT => inVal.floatValue
              case TImgTools.IMAGETYPE_DOUBLE => inVal
              case _ => throw new IllegalArgumentException(outType+" is not a known image type")
    }
    private[KVImg] def makeConvFunc[T](inType: Int, outType: Int) = {
      inVal: Any => fromDouble(toDouble(inVal,inType),outType).asInstanceOf[T]
    }
    
}