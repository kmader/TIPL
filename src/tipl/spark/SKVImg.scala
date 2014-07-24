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

/**
 * Scala Version of KVImg
 * @author mader
 *
 */
@serializable class SKVImg[T](dim: D3int, pos: D3int, elSize: D3float, imageType: Int,baseImg: RDD[(D3int,T)])(implicit lm: ClassTag[T]) 
		extends TImg.ATImg(dim,pos,elSize,imageType) with TImg {
	
  
  
      /* The function to collect all the key value pairs and return it as the appropriate array for a given slice
     * @see tipl.formats.TImgRO#getPolyImage(int, int)
     */
    override def getPolyImage(sliceNumber: Int, asType: Int) = {
        assert (TImgTools.isValidType(asType))
        
        val sliceSize = dim.x * dim.y;
        
        val sliceAsList = baseImg.filter(_._1.z==(sliceNumber+pos.z)).map{
          curElement: (D3int,T) =>
            ((curElement._1.y - pos.y) * dim.x + curElement._1.x - pos.x, curElement._2);
        }.sortByKey(true).map(_._2).collect()
        
        val outSlice = asType match {
          case TImgTools.IMAGETYPE_BOOL => new Array[Boolean](sliceSize)
          case TImgTools.IMAGETYPE_CHAR => new Array[Char](sliceSize)
          case TImgTools.IMAGETYPE_INT => new Array[Int](sliceSize)
          case TImgTools.IMAGETYPE_LONG => new Array[Long](sliceSize)
          case TImgTools.IMAGETYPE_FLOAT => new Array[Float](sliceSize)
          case TImgTools.IMAGETYPE_DOUBLE => new Array[Double](sliceSize)
          case _ => throw new IllegalArgumentException(imageType+" is not a known image type")
        }
        sliceAsList.foreach{
          cPt: (Int,T) =>
            val curVal: Double = imageType match {
              case TImgTools.IMAGETYPE_BOOL => if (imageType.asInstanceOf[Boolean]) 1.0 else 0.0
              case TImgTools.IMAGETYPE_CHAR => imageType.asInstanceOf[Byte].doubleValue()
              case TImgTools.IMAGETYPE_SHORT => imageType.asInstanceOf[Short].doubleValue()
              case TImgTools.IMAGETYPE_INT => imageType.asInstanceOf[Int].doubleValue()
              case TImgTools.IMAGETYPE_LONG => imageType.asInstanceOf[Long].doubleValue()
              case TImgTools.IMAGETYPE_FLOAT => imageType.asInstanceOf[Float].doubleValue()
              case TImgTools.IMAGETYPE_DOUBLE => imageType.asInstanceOf[Double].doubleValue()
              case _ => throw new IllegalArgumentException(imageType+" is not a known image type")
            }
            
            outSlice(cPt._1) = asType match {
              case TImgTools.IMAGETYPE_BOOL => (curVal>0)
              case TImgTools.IMAGETYPE_CHAR => curVal.byteValue
              case TImgTools.IMAGETYPE_INT => curVal.intValue
              case TImgTools.IMAGETYPE_LONG => curVal.longValue
              case TImgTools.IMAGETYPE_FLOAT => curVal.floatValue
              case TImgTools.IMAGETYPE_DOUBLE => curVal
              case _ => throw new IllegalArgumentException(imageType+" is not a known image type")
            }
        }
        outSlice

        }
        // convert this array into the proper output format
        return TImgTools.convertArrayType(curSlice, cImageType, asType, getSigned(), getShortScaleFactor());
    }
}