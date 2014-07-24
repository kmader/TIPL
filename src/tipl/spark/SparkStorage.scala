/**
 *
 */
package tipl.spark

import tipl.util.ITIPLStorage
import tipl.util.TIPLStorage
import tipl.formats.TImgRO
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.PairRDDFunctions._
import tipl.util.TIPLOps._
import tipl.spark.IOOps._
import tipl.formats.TImg
import tipl.formats.TiffFolder.TIFSliceReader

/**
 * A SparkBased version of the Storage Module which loads and processes images using Spark instead of the old VirtualAim basis
 * @author mader
 *
 */
abstract class SparkStorage extends ITIPLStorage {
	override def readTImg(path: String, readFromCache: Boolean, saveToCache: Boolean): TImgRO = {
		val sc = SparkGlobal.getContext().sc
		val bf = sc.byteFolder(path+"/*.tif") // keep it compatible with the older version
		
				
		val tifLoad = bf.toTiffSlices
		//val outImage = tifLoad.load
		val ssd = SlicesToDTImg(tifLoad)
		
		
		
		//val realImage = tifLoad.loadAsValues
		return ssd.load
		//null
	}
}
object SparkStorage {
  trait DeadTImg extends TImg {
    
  }
}