package tipl.spark

import scala.reflect.ClassTag
import scala.collection.JavaConversions._
import tipl.formats.TImgRO
import tipl.formats.TImg
import tipl.tools.BaseTIPLPluginIn._
import tipl.tools.VFilterScale
import tipl.util.D3int
import tipl.util.D3float
import tipl.tools.FilterScale.filterGenerator
import tipl.spark.SKVoronoi
import tipl.spark.ShapeAnalysis
import org.apache.spark.rdd.RDD
import tipl.tools.BaseTIPLPluginIn
import tipl.spark.DTImg
import tipl.spark.hadoop.WholeTiffFileInputFormat
import tipl.spark.hadoop.WholeByteInputFormat
import tipl.formats.TReader.TSliceReader
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.PairRDDFunctions._
import tipl.formats.TiffFolder.TIFSliceReader
import tipl.util.TImgTools
import tipl.util.TImgBlock
import org.apache.spark.api.java.JavaPairRDD

/**
 * IOOps is a cover for all the functions related to reading in and writing out Images in Spark
 *  it is closely coupled to SparkStorage which then replaces the standard functions in TIPL with the Spark-version
 */
object IOOps {
  /**
   * Add the appropriate image types to the SparkContext
   */
  implicit class ImageFriendlySparkContext(sc: SparkContext) {
    val defMinPart = sc.defaultMinPartitions
    def tiffFolder(path: String)  = { // DTImg[U]
    		val rawImg = sc.newAPIHadoopFile(path, classOf[WholeTiffFileInputFormat], classOf[String], classOf[TSliceReader])
    		rawImg
    			//	.map(pair => pair._2.toString).setName(path)
    }
    def byteFolder(path: String) = {
      sc.newAPIHadoopFile(path, classOf[WholeByteInputFormat], classOf[String], classOf[Array[Byte]])
    }
  }
  
 /** 
 *  Now the spark heavy classes linking Byte readers to Tiff Files
 */

 implicit class UnreadTiffRDD[T<: RDD[(String,Array[Byte])]](srd: T) {
  def toTiffSlices() = {
    val tSlice = srd.first
    val decoders = TIFSliceReader.IdentifyDecoderNames(tSlice._2)
    val outRdd = srd.mapValues{new TIFSliceReader(_,decoders(0))}
    outRdd
  }
}
  
  implicit class TypedReader[T<: TImgRO](cImg: TImgRO) {
    def readSlice(sliceNumber: Int, asType: Int) = {
      TImgTools.isValidType(asType)
    	cImg.getPolyImage(sliceNumber,asType) match {
          case a: Array[Int] => a
          case a: Array[Boolean] => a
          case a: Array[Double] => a
          case a: Array[Byte] => a
          case a: Array[Char] => a
          case a: Array[Float] => a
          case a: Array[Short] => a
          case a: Array[Long] => a
          case _ => throw new IllegalArgumentException("Type Not Found:"+asType+" "+TImgTools.getImageTypeName(asType)) 
        }
    }
  }
  def castAsImageFormat(obj: Any, asType: Int) = {
    TImgTools.isValidType(asType)
    asType match {
      case TImgTools.IMAGETYPE_BOOL => obj.asInstanceOf[Array[Boolean]]
      case TImgTools.IMAGETYPE_CHAR => obj.asInstanceOf[Array[Char]]
      case TImgTools.IMAGETYPE_SHORT => obj.asInstanceOf[Array[Short]]
      case TImgTools.IMAGETYPE_INT => obj.asInstanceOf[Array[Int]]
      case TImgTools.IMAGETYPE_LONG => obj.asInstanceOf[Array[Long]]
      case TImgTools.IMAGETYPE_FLOAT => obj.asInstanceOf[Array[Float]]
      case TImgTools.IMAGETYPE_DOUBLE => obj.asInstanceOf[Array[Double]]
      case _ => throw new IllegalArgumentException("Type Not Found:"+asType+" "+TImgTools.getImageTypeName(asType)) 
    }
  }
  implicit class SlicesToDTImg[T <: TSliceReader](srd:  RDD[(String,T)])(implicit lm: ClassTag[T])  {

    def loadAsBinary() = {
      processSlices[Array[Boolean]](TImgTools.IMAGETYPE_BOOL,inObj => inObj.asInstanceOf[Array[Boolean]])
    }
    def loadAsLabels() = {
      processSlices[Array[Long]](TImgTools.IMAGETYPE_LONG,inObj => inObj.asInstanceOf[Array[Long]])
    }
    def loadAsValues() = {
      processSlices[Array[Double]](TImgTools.IMAGETYPE_DOUBLE,inObj => inObj.asInstanceOf[Array[Double]])
    }
    /**
     * Automatically chooses the type based on the input image (lossless)
     */
    def load() = {
      val tImg = srd.first._2
      tImg.getImageType() match {
      	case TImgTools.IMAGETYPE_BOOL => loadAsBinary()
      	case TImgTools.IMAGETYPE_CHAR => loadAsLabels()
      	case TImgTools.IMAGETYPE_SHORT => loadAsLabels()
      	case TImgTools.IMAGETYPE_INT => loadAsLabels()
      	case TImgTools.IMAGETYPE_LONG => loadAsLabels()
      	case TImgTools.IMAGETYPE_FLOAT => loadAsValues()
      	case TImgTools.IMAGETYPE_DOUBLE => loadAsValues()
      	case _ => throw new IllegalArgumentException("Type Not Found:"+ tImg.getImageType()+" "+TImgTools.getImageTypeName( tImg.getImageType())) 
      }
    }
    private[IOOps] def processSlices[A](asType: Int,transFcn: (Any => A)) = {
      TImgTools.isValidType(asType)
      // sort by file name and then remove the filename
      val srdSorted = srd.sortByKey(true, srd.count.toInt).map(_._2)
      // keep the first element for reference
      val fst = srdSorted.first
      val pos = fst.getPos
      val dim = fst.getDim
      val timgDim = new D3int(dim.x,dim.y,srdSorted.count.toInt)
      val srdArr = srdSorted.
      map(cval => cval.polyReadImage(asType))
      val srdMixed = srdArr.zipWithIndex.map{
            cBnd => 
              val cPos = new D3int(pos.x,pos.y,pos.z+cBnd._2.toInt)
              (cPos,(cBnd._1,pos,dim))
          }
      
      val outRdd = srdMixed.mapValues{
        cBnd =>
          new TImgBlock[A](transFcn(cBnd._1),cBnd._2,cBnd._3)
      }
      
      DTImg.WrapRDD[A](fst,JavaPairRDD.fromRDD(outRdd),asType)
      }
    }
}