/**
 *
 */
package tipl.spark

import tipl.formats.TImg
import tipl.formats.TImgRO
import java.io.Serializable
import tipl.util.D3int
import tipl.util.TImgTools.HasDimensions
import tipl.util.D3float
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import tipl.util.TImgTools



/**
 * @author mader
 *
 */
abstract class KVImage[T <: Number](idim: D3int,ipos: D3int=new D3int(0,0,0),ioffset: D3int= new D3int(0,0,0),ielSize: D3float=new D3float(1f,1f,1f),baseImg: RDD[(D3int, T)],iimageType: Integer) 
extends TImg.ATImg(idim,ipos,ioffset,ielSize,iimageType) {
  
  
  def this(tHD: HasDimensions,bImg: RDD[(D3int, T)],itype:Integer) = 
    this(tHD.getDim,tHD.getPos,tHD.getElSize,bImg,itype)
}

object junk {
  val isCurSlice = ((arg0: (D3int,_)) => (arg0..1.z==sliceNumber))
  val slicePosToIndex = (arg0: (D3int,_)) => ((arg0._1.z*cDim.y+arg0._1.y)*cDim.x+arg0._1.x,arg0._2)
  
		
  /* The function to collect all the key value pairs and return it as the appropriate array for a given slice
	 * @see tipl.formats.TImgRO#getPolyImage(int, int)
	 */
	override def  getPolyImage(sliceNumber: Integer, asType: Integer):Object = {
		assert(TImgTools.isValidType(asType));
		val cDim:D3int=getDim();
		val sliceSize:Integer=cDim.x*cDim.y
		val cImageType:Integer = imageType
		
		val sliceAsList: List = baseImg.filter(isCurSlice).map(slicePosToIndex)
		.sortByKey().collect();
		
		// first create an array of the current type
		val curSlice=cImageType match {
		  case TImgTools.IMAGETYPE_BOOL: new Array[Boolean](sliceSize)
		  case TImgTools.IMAGETYPE_CHAR: new Array[Character](sliceSize)
		  case TImgTools.IMAGETYPE_SHORT: new Array[Short](sliceSize)
		  case TImgTools.IMAGETYPE_INT: new Array[Integer](sliceSize)
		  case TImgTools.IMAGETYPE_FLOAT: new Array[Float](sliceSize)
		  case TImgTools.IMAGETYPE_LONG: new Array[Long](sliceSize)
		  case _: throw new IllegalArgumentException("Image type :"+TImgTools.getImageTypeName(imageType)+" is not yet supported")
		}
		
		// now copy the list into this array
		for (val curElement : sliceAsList) {
			val index=curElement._1.intValue();
			cImageType match {
			case TImgTools.IMAGETYPE_BOOL: 
				((boolean[]) curSlice)[index]=((Byte) curElement._2).byteValue()>0;
				break;
			case TImgTools.IMAGETYPE_CHAR: 
				((char[]) curSlice)[index]=(char) ((Byte) curElement._2).byteValue();
				break;
			case TImgTools.IMAGETYPE_INT: 
				((int[]) curSlice)[index]=((Integer) curElement._2).intValue();
				break;
			case TImgTools.IMAGETYPE_FLOAT: 
				((float[]) curSlice)[index]=((Float) curElement._2).floatValue();
				break;
			case TImgTools.IMAGETYPE_DOUBLE: 
				((double[]) curSlice)[index]=((Double) curElement._2).doubleValue();
				break;
			case TImgTools.IMAGETYPE_LONG: 
				((long[]) curSlice)[index]=((Long) curElement._2).longValue();
				break;
		
			}
		}
		// convert this array into the proper output format
		return TImgTools.convertArrayType(curSlice, cImageType, asType, getSigned(), getShortScaleFactor());
	}
  
  override def setDim(inData: D3int) = {throw new IllegalArgumentException("Dimensions cannot be changed");}
   override def setOffset(inData: D3int) = {throw new IllegalArgumentException("Dimensions cannot be changed");}
    override def setPos(inData: D3int) = {throw new IllegalArgumentException("Dimensions cannot be changed");}
 

    
}
