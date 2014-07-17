/**
 *
 */
package tipl.spark
import tipl.spark.DTImg
import org.apache.spark.SparkContext._
import tipl.util.D3int
import tipl.util.TImgBlock
import tipl.util.TIPLOps._
import org.apache.spark.rdd.PairRDDFunctions._
import scala.collection.mutable.ArrayBuffer
import org.apache.spark.rdd.RDD
import tipl.tools.BaseTIPLPluginIn

/**
 * A collectiono of useful functions for DTImg classes to allow more complicated analyses
 * @author mader
 *
 */
@serializable object DTImgOps {
  @serializable implicit class RichDtRDD[A<: Cloneable](srd: RDD[(D3int,TImgBlock[A])]) extends
  NeighborhoodOperation[TImgBlock[A],TImgBlock[A]]{
    val blockShift = (offset: D3int) =>{
    (p: (D3int,TImgBlock[A])) => {
      val nPos = p._1+offset
      (nPos,new TImgBlock[A](p._2.getClone(),nPos,offset))
    }
  }
  def spreadSlices(windSize: D3int): (RDD[(D3int,Iterable[TImgBlock[A]])]) = {
	  var curOut = srd.mapValues(p => Iterable(p))

	  for(curOffset <- 1 to windSize.z) {
	    val upSet = new D3int(0,0,+curOffset)
	    val up1 = srd.map(blockShift(upSet))
	    val downSet = new D3int(0,0,-curOffset)
	    val down1 = srd.map(blockShift(downSet))
	    
	    val tempOut = curOut.join(up1.join(down1))
	    curOut = tempOut.mapValues {p => p._1 ++ Iterable(p._2._1,p._2._2)}
	  }
	  curOut
       
	} 
  def blockOperation(windSize: D3int,kernel: Option[BaseTIPLPluginIn.morphKernel],mapFun: (Iterable[TImgBlock[A]] => TImgBlock[A])): RDD[(D3int,TImgBlock[A])] = {
    val spread = srd.spreadSlices(windSize).collectSlices(windSize, kernel, mapFun)
    spread
  }
  
    
  }
  /**
   * A class of a spread RDD image (after a flatMap/spread operation)
   */
  @serializable implicit class SpreadRDD[A<: Cloneable](srd: RDD[(D3int,Iterable[TImgBlock[A]])]) {
    def collectSlices(windSize: D3int,kernel: Option[BaseTIPLPluginIn.morphKernel],coFun: (Iterable[TImgBlock[A]] => TImgBlock[A])) = {
      srd.mapValues(coFun)
    }
  }
  @serializable implicit class RichDTImg[A<: Cloneable](ip: DTImg[A]) {
    
    val srd = ip.baseImg.rdd
    /** a much simpler version of spreadSlices taking advantage of Scala's language features
     *  
     */
    def spreadSlices(windSize: D3int) = {
      srd.spreadSlices(windSize)
    }
  }
}

