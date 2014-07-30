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
import tipl.spark.hadoop.TiffFileInputFormat
import org.apache.spark.input.ByteInputFormat
import tipl.formats.TReader.TSliceReader
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.PairRDDFunctions._
import tipl.formats.TiffFolder.TIFSliceReader
import tipl.util.TImgTools
import tipl.util.TImgBlock
import org.apache.spark.api.java.JavaPairRDD
import tipl.spark.SparkGlobal
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.dstream.DStream
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.{InputFormat => NewInputFormat, Job => NewHadoopJob}
import org.apache.hadoop.mapreduce.lib.input.{FileInputFormat => NewFileInputFormat}
import org.apache.spark.rdd.BinaryFileRDD

/**
 * IOOps is a cover for all the functions related to reading in and writing out Images in Spark
 *  it is closely coupled to SparkStorage which then replaces the standard functions in TIPL with the Spark-version
 */
object IOOps {
  /**
   * Add the byte reading to the SparkContext
   */
  implicit class ImageFriendlySparkContext(sc: SparkContext) {
    val defMinPart = sc.defaultMinPartitions
    
    
    def tiffFolder(path: String, minPartitions: Int = sc.defaultMinPartitions): RDD[(String, TIFSliceReader)] = {
    	val job = new NewHadoopJob(sc.hadoopConfiguration)
    	NewFileInputFormat.addInputPath(job, new Path(path))
    	val updateConf = job.getConfiguration
    	new BinaryFileRDD(
    			sc,
    			classOf[TiffFileInputFormat],
    			classOf[String],
    			classOf[TIFSliceReader],
    			updateConf,
    			minPartitions).setName(path)
  }
    def byteFolder(path: String, minPartitions: Int = sc.defaultMinPartitions): RDD[(String, Array[Byte])] = {
    	val job = new NewHadoopJob(sc.hadoopConfiguration)
    	NewFileInputFormat.addInputPath(job, new Path(path))
    	val updateConf = job.getConfiguration
    	new BinaryFileRDD(
    			sc,
    			classOf[ByteInputFormat],
    			classOf[String],
    			classOf[Array[Byte]],
    			updateConf,
    			minPartitions).setName(path)
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
     * Read slices as a block of 2D objects instead of a 3D stack
     */
    def loadAs2D(sorted: Boolean = true) = {
      parseSlices[Array[Double]](srd,TImgTools.IMAGETYPE_DOUBLE,inObj => inObj.asInstanceOf[Array[Double]],sorted)._1
    }
    def loadAs2DLabels(sorted: Boolean = true) = {
      parseSlices[Array[Long]](srd,TImgTools.IMAGETYPE_LONG,inObj => inObj.asInstanceOf[Array[Long]],sorted)._1
    }
    def loadAs2DBinary(sorted: Boolean = true) = {
      parseSlices[Array[Boolean]](srd,TImgTools.IMAGETYPE_BOOL,inObj => inObj.asInstanceOf[Array[Boolean]],sorted)._1
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
      val (outRdd,firstImage) = parseSlices[A](srd,asType,transFcn,true,srd.count.toInt)
      val timgDim = new D3int(firstImage.getDim.x,firstImage.getDim.y,srd.count.toInt)
      val efImg = TImgTools.MakeEditable(firstImage);
      efImg.setDim(timgDim)
      DTImg.WrapRDD[A](efImg,JavaPairRDD.fromRDD(outRdd),asType)
    }
    private[IOOps] def parseSlices[A](srdIn: RDD[(String,T)],asType: Int,transFcn: (Any => A),sorted: Boolean, partitions: Int = 20) = {
      TImgTools.isValidType(asType)
      // sort by file name and then remove the filename
      val srdSorted = (if(sorted) srdIn.sortByKey(true, partitions) else srdIn).
      	map(_._2).zipWithIndex 
      					
      // keep the first element for reference
      val fst = srdSorted.first._1
      val spos = fst.getPos
      val dim = fst.getDim
      
      val srdMixed = srdSorted.
      map(cval => (cval._2,cval._1.polyReadImage(asType))).   
      	map{
            cBnd => 
              val cPos = new D3int(spos.x,spos.y,spos.z+cBnd._1.toInt)
              (cPos,(cBnd._2,cPos,dim))
          }
      
      val outArr = srdMixed.mapValues{
        cBnd =>
          new TImgBlock[A](transFcn(cBnd._1),cBnd._2,cBnd._3)
      }
      (outArr,fst)
      }
    
    }
  
  /**
   * Streaming Code
   */
  
      /**
   * Add the byte reading and tiff file to the StreamingSparkContext
   */
  implicit class ImageFriendlyStreamingContext(ssc: StreamingContext) {

    /**
   * Create a input stream that monitors a Hadoop-compatible filesystem
   * for new files and reads them as a byte stream. Files must be written to the
   * monitored directory by "moving" them from another location within the same
   * file system. File names starting with . are ignored.
   * @param directory HDFS directory to monitor for new file
   */
	  def byteFolder(directory: String) = 
	    ssc.fileStream[String, Array[Byte], ByteInputFormat](directory)
	  
	  def tiffFolder(directory: String) = 
	    ssc.fileStream[String, TIFSliceReader, TiffFileInputFormat](directory)

  }
  

  /**
   * Convert easily between sparkcontext and a streaming context
   */
  implicit class StreamingContextFromSpark(sc: SparkContext) {
	  def toStreaming(itiming: Duration): StreamingContext = new StreamingContext(sc,itiming)
	  def toStreaming(isecs: Long): StreamingContext = toStreaming(Seconds(isecs))
  }
  
  
    implicit class StreamSliceToDTImg[T <: TSliceReader](srd: DStream[(String,T)])(implicit lm: ClassTag[T])  {

    def loadAsBinary() = {
      processSlices[Array[Boolean]](TImgTools.IMAGETYPE_BOOL,inObj => inObj.asInstanceOf[Array[Boolean]])
    }
    def loadAsLabels() = {
      processSlices[Array[Long]](TImgTools.IMAGETYPE_LONG,inObj => inObj.asInstanceOf[Array[Long]])
    }
    def loadAsValues() = {
      processSlices[Array[Double]](TImgTools.IMAGETYPE_DOUBLE,inObj => inObj.asInstanceOf[Array[Double]])
    }

    private[IOOps] def processSlices[A](asType: Int,transFcn: (Any => A)) = {
      TImgTools.isValidType(asType)
      // keep everything since we do not know how much information is coming
      srd.
      mapValues{cval => 
        (cval.polyReadImage(asType),cval.getPos(),cval.getDim())
        }.mapValues{
        cBnd =>
          new TImgBlock[A](transFcn(cBnd._1),cBnd._2,cBnd._3)
      }
      
      }
    }

  
  
  
}