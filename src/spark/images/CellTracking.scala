package spark.images

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.PairRDDFunctions._
import tipl.spark.IOOps._
import spark.images.ImageTools2D
import tipl.util.TImgBlock
import tipl.util.TImgTools
import tipl.spark.SparkGlobal
import breeze.linalg._
import tipl.util.D3int
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.{Matrix, Matrices}
import org.apache.spark.mllib.linalg.distributed.{IndexedRow, IndexedRowMatrix, RowMatrix}
import tipl.spark.KVImg
import tipl.spark.ShapeAnalysis
import java.io.BufferedWriter
import java.io.FileWriter
// ~/Dropbox/Informatics/spark/bin/spark-submit --class spark.images.CellTracking --executor-memory 4G --driver-memory 4G /Users/mader/Dropbox/tipl/build/TIPL_core.jar -tif=/Volumes/MacDisk/AeroFS/VardanData/ForKevin_VA/3D/Spheroid2/GFP 
/**
 * Class to hold the basic settings
 */
@serializable case class CellTrackingSettings(imgPath: String,savePath: String, checkpointResults: Boolean, threshValue: Double)
	// format for storing image statistics
@serializable case class cellImstats(min: Double, mean: Double, max: Double)
/**
 * Functions shared between spark and streaming versions
 */
object CellTrackingCommon  {
  def getParameters(args: Array[String]) = {
  val p = SparkGlobal.activeParser(args)
	val imgPath = p.getOptionPath("tif", "./", "Directory with tiff projections, dark, and flat field images")
	val imgSuffix = p.getOptionPath("suffix", ".tif", "Suffix to be added to image path *.sss to find the image files")
	
	val savePath = p.getOptionPath("save", imgPath, "Directory for output")
	val checkpointResults = p.getOptionBoolean("checkpoint", false, "Write intermediate results as output")
	val threshValue = p.getOptionDouble("threshvalue", 500, "Threshold value for cells")

	val smallestObjSize =  p.getOptionInt("smallestobj",0, "Smallest objects to keep")
	
	(CellTrackingSettings(imgPath+"/*"+imgSuffix,savePath,checkpointResults,threshValue),p)
  }
  // calculate statistics for an array
  def arrStats(inArr: Array[Double]) = cellImstats(inArr.min,inArr.sum/(1.0*inArr.length),inArr.max)
  

}

object CellTracking  {
  def main(args: Array[String]) {
	 val (settings,p) = CellTrackingCommon.getParameters(args)
	p.checkForInvalid()
	val sc = SparkGlobal.getContext("CellTrackingTool").sc
	
	// read in a directory of tiffs (as a live stream)
	val tiffSlices = sc.tiffFolder(settings.imgPath)
	
	// read the number from the filename
	val parseFilename = "(.*)GFP([0-9]*)[.]tif".r
	val getFileNumber: (String => Long) = {
	  filename =>
	    parseFilename findFirstIn filename match {
	    	case Some(parseFilename(prefix,number)) => number.toLong
	    	case None=> 0L
	    }
	}
	
	// read the values as arrays of doubles
	val doubleSlices = tiffSlices.loadAs2D(sorted=false,nameToValue=Some(getFileNumber))
	
	val bwImage = ImageTools2D.BlockImageToKVImage(sc, doubleSlices, settings.threshValue)
	val clImage = ImageTools2D.compLabelingBySlice(bwImage)
	val sliceCLimage = clImage.map{inVal => (inVal._1,inVal._2._1)}.groupBy(_._1.z)
	val shapes = sliceCLimage.
		mapValues{
		 	curSlice =>
		 	  val pointList = curSlice.toList.groupBy(_._2)
		 	  pointList.map{
		 	    inGroup =>
		 	      ShapeAnalysis.singleShape(inGroup._1,inGroup._2)
		 	  }
		 	  
	}
	val outText = shapes.mapValues{allShapes => allShapes.map(_.toString(",")).mkString("\\n")}
	outText.foreach{
	  cSlice => 
	  	val outFile = new java.io.File(settings.savePath+"/shape"+cSlice._1+".csv")
	  	val writer = new BufferedWriter(new FileWriter(outFile))
	  	writer.write(cSlice._1)
	    //Close writer
	  	writer.close();
	}
    sc.stop
  }
}

object CellTrackingStreaming {
  def main(args: Array[String]) {
  import org.apache.spark.streaming.{Seconds, StreamingContext}
  import org.apache.spark.streaming.StreamingContext._
  val (settings,p) = CellTrackingCommon.getParameters(args)
  
  p.checkForInvalid()
  val ssc = SparkGlobal.getContext("CellTrackingTool").sc.toStreaming(30)
  // read in a directory of tiffs (as a live stream)
  val tiffSlices = ssc.tiffFolder(settings.imgPath).filter(_._1 contains ".tif")
  // read the values as arrays of doubles
  val doubleSlices = tiffSlices.loadAsValues

  // structure for statSlices is (filename,(cellImstats,imArray))
  val statSlices = doubleSlices.mapValues{
	  cArr => (CellTrackingCommon.arrStats(cArr.get),cArr.get)
   }
  }
  
}