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
@serializable abstract class SDTImg[T](dim: D3int, pos: D3int, elSize: D3float, imageType: Int)(implicit lm: ClassTag[T]) 
		extends TImg.ATImg(dim,pos,elSize,imageType) with TImg {
}
