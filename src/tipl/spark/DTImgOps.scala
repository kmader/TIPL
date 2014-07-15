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

/**
 * A collectiono of useful functions for DTImg classes to allow more complicated analyses
 * @author mader
 *
 */
@serializable object DTImgOps {
  
  @serializable implicit class RichDTImg[A<: Cloneable](ip: DTImg[A]) {
    val blockShift = (offset: D3int) =>{
    (p: (D3int,TImgBlock[A])) => {
      val nPos = p._1+offset
      (nPos,new TImgBlock[A](p._2.getClone(),nPos,offset))
    }
  }
    val srd = ip.baseImg.rdd
    /** a much simpler version of spreadSlices taking advantage of Scala's language features
     *  
     */
	def spreadSlices(windSize: Int,offSlices: Int=1) = {
	  var curOut = srd.mapValues(p => List(p))

	  for(curOffset <- 1 to offSlices ) {
	    val upSet = new D3int(0,0,+curOffset)
	    val up1 = srd.map(blockShift(upSet))
	    val downSet = new D3int(0,0,-curOffset)
	    val down1 = srd.map(blockShift(downSet))
	    
	    val tempOut = curOut.join(up1.join(down1))
	    curOut = tempOut.mapValues {p => p._1 ++ List(p._2._1,p._2._2)}
	  }
	  curOut
       
	} 
  }
}

