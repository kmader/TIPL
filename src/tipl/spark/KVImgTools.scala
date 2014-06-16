/**
 *
 */
package tipl.spark

import tipl.formats.TImgRO

import tipl.formats.PureFImage;
import org.apache.spark.api.java.function.PairFunction;
import tipl.formats.TImgRO;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgBlock;
import tipl.util.TImgTools;
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

/**
 * Some tools for the Key-value pair image to make working with them easier, writing code in java is just too much of a pain in the ass
 * @author mader
 *
 */
object KVImgTools {
  def createFromPureFun[T <: Number](sc: SparkContext,objToMirror: TImgTools.HasDimensions,inpf: PureFImage.PositionFunction,imageType: Integer): KVImg[T] = {
  val objDim = objToMirror getDim
  val objPos = objToMirror getPos
  val xrng=objPos.x to (objPos.x+objDim.x)
  val yrng=objPos.y to (objPos.y+objDim.y)
  val zrng=objPos.z to (objPos.z+objDim.z)
  val wrappedImage=sc.parallelize(zrng). // parallelize over the slices
  flatMap(z => {
    for(x<-xrng;y<-yrng) // for each slice create each point in the image
      yield (new D3int(x,y,z),
          new java.lang.Double(inpf.get(Array(x.doubleValue,y.doubleValue,z.doubleValue))))
  }).toJavaRDD
  
  KVImg.FromRDD[T](objToMirror,imageType,wrappedImage)
	
  }
		
}