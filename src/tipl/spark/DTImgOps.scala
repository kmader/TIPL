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
import tipl.util.TImgTools

/**
 * A collectiono of useful functions for DTImg classes to allow more complicated analyses
 * @author mader
 *
 */
@serializable object DTImgOps {
  def DTImgToKV[T](inImg: DTImg[T])  = {
    val imgType = inImg.getImageType
    inImg.getBaseImg.rdd.flatMap{
      cPoint =>
        val pos = cPoint._1
        val dim = cPoint._2.getDim
        val obj = cPoint._2.get
        val outArr = imgType match {
        		case TImgTools.IMAGETYPE_BOOL => obj.asInstanceOf[Array[Boolean]]
       			case TImgTools.IMAGETYPE_CHAR => obj.asInstanceOf[Array[Char]]
       			case TImgTools.IMAGETYPE_SHORT => obj.asInstanceOf[Array[Short]]
       			case TImgTools.IMAGETYPE_INT => obj.asInstanceOf[Array[Int]]
       			case TImgTools.IMAGETYPE_LONG => obj.asInstanceOf[Array[Long]]
       			case TImgTools.IMAGETYPE_FLOAT => obj.asInstanceOf[Array[Float]]
       			case TImgTools.IMAGETYPE_DOUBLE => obj.asInstanceOf[Array[Double]]
       			case _ => throw new IllegalArgumentException("Type Not Found:"+imgType+" "+TImgTools.getImageTypeName(imgType)) 
        }
        for(z<-0 to dim.z;y<- 0 to dim.y;x<-0 to dim.x) 
          yield (new D3int(pos.x+x,pos.y+y,pos.z+z),outArr((z*dim.y+y)*dim.x+x))
    }
  }
  def DTImgToKVStrict[T,V](inImg: DTImg[T]): RDD[(D3int,V)] = {
    DTImgToKV[T](inImg).mapValues{
      cValue => cValue.asInstanceOf[V]
    }
  }
  @serializable implicit class RichDtRDD[A](srd: RDD[(D3int,TImgBlock[A])]) extends
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
  @serializable implicit class SpreadRDD[A](srd: RDD[(D3int,Iterable[TImgBlock[A]])]) {
    def collectSlices(windSize: D3int,kernel: Option[BaseTIPLPluginIn.morphKernel],coFun: (Iterable[TImgBlock[A]] => TImgBlock[A])) = {
      srd.mapValues(coFun)
    }
  }
  @serializable implicit class RichDTImg[A](ip: DTImg[A]) {
    
    val srd = ip.baseImg.rdd
    /** a much simpler version of spreadSlices taking advantage of Scala's language features
     *  
     */
    def spreadSlices(windSize: D3int) = {
      srd.spreadSlices(windSize)
    }
  }
}

