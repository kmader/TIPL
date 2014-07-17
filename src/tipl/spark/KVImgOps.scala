/**
 * A series of tools for processing with KVImg objects, including the required spread and collect functions which extend the RDD directly
 */
package tipl.spark

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.PairRDDFunctions._
import tipl.formats.PureFImage
import tipl.util.{D3int, TImgTools}
import org.apache.spark.rdd.RDD
import tipl.spark.KVImg
import tipl.tools.BaseTIPLPluginIn

/**
 * Some tools for the Key-value pair image to make working with them easier, writing code in java is just too much of a pain in the ass
 * @author mader
 *
 */
 @serializable object KVImgOps {
  def createFromPureFun[T <: Number](sc: SparkContext,objToMirror: TImgTools.HasDimensions,inpf: PureFImage.PositionFunction,imageType: Integer): KVImg[T] = {
    val objDim = objToMirror getDim
    val objPos = objToMirror getPos
    val xrng = objPos.x to (objPos.x + objDim.x)
    val yrng = objPos.y to (objPos.y + objDim.y)
    val zrng = objPos.z to (objPos.z + objDim.z)
    val wrappedImage = sc.parallelize(zrng). // parallelize over the slices
      flatMap(z => {
      for (x <- xrng; y <- yrng) // for each slice create each point in the image
      yield (new D3int(x,y,z),
        new java.lang.Double(inpf.get(Array(x.doubleValue, y.doubleValue, z.doubleValue))))
    }).toJavaRDD
    
    KVImg.FromRDD[T](objToMirror, imageType, wrappedImage)
  }
  /**
   * Go directly from a PositionFunction to a KVImg
   */
  implicit class RichPositionFunction[T<: Number](inpf: PureFImage.PositionFunction) {
    def asKVImg(sc: SparkContext,objToMirror: TImgTools.HasDimensions,imageType: Integer): KVImg[T] = {
      createFromPureFun(sc,objToMirror,inpf,imageType)
    }
  }
  
  @serializable implicit class RichKvRDD[A<: Number](srd: RDD[(D3int,A)]) {
  
  val spread_voxels_sl = (windSize: Int) => { 
    spread_voxels(new D3int(windSize,windSize,windSize),None)
  }
    /**
   * A generic voxel spread function for a given window size and kernel
   * 
   */
  val spread_voxels = (windSize: D3int, kernel: Option[BaseTIPLPluginIn.morphKernel]) => { 
    (pvec: (D3int, A)) => {
    val pos = pvec._1

      val windx = (-windSize.x to windSize.x)
      val windy = (-windSize.y to windSize.y)
      val windz = (-windSize.z to windSize.z)
      for (x <- windx; y <- windy; z <- windz;
           if (kernel.map(_.inside(0, 0, pos.x, pos.x + x, pos.y, pos.y + y, pos.z, pos.z + z)).getOrElse(true)))
      yield (new D3int(pos.x + x, pos.y + y, pos.z + z),
        (pvec._2, (x == 0 & y == 0 & z == 0)))
    } 
  }
  
  def spreadSlices(windSize: Int,offSlices: Int=1): RDD[(D3int,Iterable[(A,Boolean)])] = {
	srd.flatMap(spread_voxels_sl(windSize)).groupByKey
	} 
    
  }
  /**
   * A class of a spread RDD image (after a flatMap/spread operation)
   */
  @serializable implicit class SpreadRDD[A<: Number](srd: RDD[(D3int,Iterable[(A,Boolean)])]) {
    def collectSlices(coFun: (Iterable[(A,Boolean)] => A)) = {
      srd.mapValues(coFun)
    }
  }
  @serializable implicit class RichKVImg[A<: Number](ip: KVImg[A]) {
    val srd = ip.getBaseImg().rdd
    def getInterface() = {
    	
    }
  }
    
  

}