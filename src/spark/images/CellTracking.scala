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
import tipl.util.D3float
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.{ Matrix, Matrices }
import org.apache.spark.mllib.linalg.distributed.{ IndexedRow, IndexedRowMatrix, RowMatrix }
import tipl.spark.KVImg
import tipl.spark.ShapeAnalysis
import java.io.BufferedWriter
import java.io.FileWriter
import tipl.tools.GrayVoxels
import tipl.util.ArgumentList
// ~/Dropbox/Informatics/spark/bin/spark-submit --class spark.images.CellTracking --executor-memory 4G --driver-memory 4G /Users/mader/Dropbox/tipl/build/TIPL_core.jar -tif=/Volumes/MacDisk/AeroFS/VardanData/ForKevin_VA/3D/Spheroid2/GFP 
// sparksubmit --class spark.images.CellTrackingSingle /afs/psi.ch/project/tipl/jar/TIPL_core.jar -tif=/gpfs/home/mader/SpheriodData/3D/Spheroid2/GFP/  -@masternode=spark://merlinc60:7077 -@sparkmemory=5G -cache -threshvalue=5000
/**
 * Class to hold the basic settings
 */
@serializable case class CellTrackingSettings(imgPath: ArgumentList.TypedPath, savePath: ArgumentList.TypedPath, checkpointResults: Boolean, cacheInput: Boolean, forcePartitions: Int, maxIters: Int, threshValue: Double)
// format for storing image statistics
@serializable case class cellImstats(min: Double, mean: Double, max: Double)
/**
 * Functions shared between spark and streaming versions
 */
object CellTrackingCommon {
  def getParameters(args: Array[String]) = {
    val p = SparkGlobal.activeParser(args)
    val imgPath = p.getOptionPath("tif", "./", "Directory with tiff projections, dark, and flat field images")
    val imgSuffix = p.getOptionPath("suffix", ".tif", "Suffix to be added to image path *.sss to find the image files")

    val savePath = p.getOptionPath("save", imgPath, "Directory for output")
    val checkpointResults = p.getOptionBoolean("checkpoint", false, "Write intermediate results as output")
    val cacheInput = p.getOptionBoolean("cache", false, "Cache Input")
    val threshValue = p.getOptionDouble("threshvalue", 500, "Threshold value for cells")
    val forcePartitions = p.getOptionInt("forcepart", -1, "Force partition count")
    val maxIters = p.getOptionInt("maxiters", Integer.MAX_VALUE, "Maximum number of iterations for component labeling")
    val smallestObjSize = p.getOptionInt("smallestobj", 0, "Smallest objects to keep")

    (CellTrackingSettings(imgPath.append("/*" + imgSuffix), savePath, checkpointResults, cacheInput, forcePartitions, maxIters, threshValue), p)
  }
  // calculate statistics for an array
  def arrStats(inArr: Array[Double]) = cellImstats(inArr.min, inArr.sum / (1.0 * inArr.length), inArr.max)

}

object CellTracking {

  def main(args: Array[String]) {
    val (settings, p) = CellTrackingCommon.getParameters(args)
    p.checkForInvalid()
    val sc = SparkGlobal.getContext("CellTrackingTool").sc

    // read in a directory of tiffs (as a live stream)
    val tiffSlices = sc.tiffFolder(settings.imgPath.getPath)

    // read the number from the filename
    val parseFilename = "(.*)DAPI([0-9]*)[.]tif".r
    val getFileNumber: (String => Long) = {
      filename =>
        parseFilename findFirstIn filename match {
          case Some(parseFilename(prefix, number)) => number.toLong
          case None => 0L
        }
    }
    // read the values as arrays of doubles
    val doubleSlices = tiffSlices.loadAs2D(sorted = false, nameToValue = Some(getFileNumber))

    var loadedSlices = if (settings.forcePartitions > 0) doubleSlices.repartition(settings.forcePartitions) else doubleSlices
    loadedSlices = if (settings.cacheInput) loadedSlices.cache() else loadedSlices

    if (settings.cacheInput)
      loadedSlices.foreach {
        inSlice =>
          println("W" + inSlice._1.getWidth())
      }

    val start = System.nanoTime()

    val bwImage = ImageTools2D.BlockImageToKVImage(sc, loadedSlices, settings.threshValue)
    val clImage = ImageTools2D.compLabelingBySlicePartJL(bwImage.mapValues { _.toIterator }, maxIters = settings.maxIters)
    println(clImage.mapValues { cSlice => cSlice.size }.collectAsMap.mkString("\t"))
    val sliceCLimage = clImage.map { inIter => (inIter._1.z, inIter._2.map { inVal => (inVal._1, inVal._2._1) }) }
    val shapes = sliceCLimage.
      mapValues {
        curSlice =>
          val pointList = curSlice.toList.groupBy(_._2)
          pointList.map {
            inGroup =>
              ShapeAnalysis.singleShape(inGroup._1, inGroup._2)
          }

      }
    val cCount = sc.accumulator(0)
    shapes.foreach {
      cSlice =>
        writeOutput(cSlice, settings.savePath + "/shape")
        cCount += 1
    }

    val end = System.nanoTime()
    val elapsedTime = (end - start) / 1000000000.0
    val fps = cCount.value / elapsedTime
    sc.stop

    println("Time Elapsed:" + elapsedTime + ", FPS:" + fps)
  }

  def writeOutput(cSlice: (Int, Iterable[GrayVoxels]), outPath: String) = {
    val outFile = new java.io.File(outPath + cSlice._1 + ".csv")
    val writer = new BufferedWriter(new FileWriter(outFile))
    writer.write(GrayVoxels.getHeaderString("Test", false, true) + "\n")
    cSlice._2.foreach {
      cShape =>
        val cLine = cShape.mkString(new D3float(1.0f), false, true)
        writer.write(cLine + "\n")
    }
    //Close writer
    writer.close();
  }
}

object CellTrackingSingle {

  def main(args: Array[String]) {
    val (settings, p) = CellTrackingCommon.getParameters(args)
    p.checkForInvalid()
    val sc = SparkGlobal.getContext("CellTrackingTool").sc

    // read in a directory of tiffs (as a live stream)
    val tiffSlices = sc.tiffFolder(settings.imgPath.getPath)

    // read the number from the filename
    val parseFilename = "(.*)GFP([0-9]*)[.]tif".r
    val getFileNumber: (String => Long) = {
      filename =>
        parseFilename findFirstIn filename match {
          case Some(parseFilename(prefix, number)) => number.toLong
          case None => 0L
        }
    }
    // read the values as arrays of doubles
    val doubleSlices = tiffSlices.loadAs2D(sorted = false, nameToValue = Some(getFileNumber))

    var loadedSlices = if (settings.forcePartitions > 0) doubleSlices.repartition(settings.forcePartitions) else doubleSlices
    loadedSlices = if (settings.cacheInput) loadedSlices.cache() else loadedSlices

    if (settings.cacheInput)
      loadedSlices.foreach {
        inSlice =>
          println("W" + inSlice._1.getWidth())
      }

    val start = System.nanoTime()

    val bwImage = ImageTools2D.BlockImageToKVImageDouble(sc, loadedSlices, settings.threshValue)
    val cCount = sc.accumulator(0)
    bwImage.foreach {
      curSlice =>
        val clSlice = ImageTools2D.sliceCompLabel[Double](curSlice._2.toIterator, new D3int(1, 1, 0), settings.maxIters)

        val slCLsl = (curSlice._1.z, clSlice.map { inVal => (inVal._1, inVal._2._1) })
        val pointList = slCLsl._2.toList.groupBy(_._2)
        val shapes = pointList.map {
          inGroup =>
            ShapeAnalysis.singleShape(inGroup._1, inGroup._2)
        }
        CellTracking.writeOutput((slCLsl._1, shapes), settings.savePath + "/shape")
        cCount += 1

    }

    val end = System.nanoTime()
    val elapsedTime = (end - start) / 1000000000.0
    val fps = cCount.value / elapsedTime
    sc.stop

    println("Time Elapsed:" + elapsedTime + ", FPS:" + fps)
  }
}

object CellTrackingStreaming {
  def main(args: Array[String]) {
    import org.apache.spark.streaming.{ Seconds, StreamingContext }
    import org.apache.spark.streaming.StreamingContext._
    val (settings, p) = CellTrackingCommon.getParameters(args)

    p.checkForInvalid()
    val ssc = SparkGlobal.getContext("CellTrackingTool").sc.toStreaming(30)
    // read in a directory of tiffs (as a live stream)
    val tiffSlices = ssc.tiffFolder(settings.imgPath.getPath()).filter(_._1 contains ".tif")
    // read the values as arrays of doubles
    val doubleSlices = tiffSlices.loadAsValues

    // structure for statSlices is (filename,(cellImstats,imArray))
    val statSlices = doubleSlices.mapValues {
      cArr => (CellTrackingCommon.arrStats(cArr.get), cArr.get)
    }
  }

}