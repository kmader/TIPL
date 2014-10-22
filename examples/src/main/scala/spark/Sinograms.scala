package spark.images

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import tipl.spark.IOOps._
import tipl.util.TImgSlice
import tipl.util.TImgTools
import tipl.spark.SparkGlobal
import breeze.linalg._
import tipl.util.D3int
import tipl.util.TypedPath
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.{ Matrix, Matrices }
import org.apache.spark.mllib.linalg.distributed.{ IndexedRow, IndexedRowMatrix, RowMatrix }

// Command line argument to run the script
// ~/Dropbox/Informatics/spark/bin/spark-submit --class spark.images.Sinograms --executor-memory
// 4G --driver-memory 4G /Users/mader/Dropbox/tipl/build/TIPL_core.jar
// -tif=/Volumes/WORKDISK/WorkData/StreamingTests/tinytif/ -@spark
/**
 * Class to hold the basic settings
 */
case class SinogramSettings(imgPath: String, savePath: TypedPath,
                                          checkpointResults: Boolean, maxDarkVal: Double,
                                          maxProjVal: Double, minFlatVal: Double, maxSino: Int)
  extends Serializable

// format for storing image statistics
case class ImStats(min: Double, mean: Double, max: Double) extends Serializable

/**
 * Functions shared between spark and streaming versions
 */
object SinogramCommon {
  def getParameters(args: Array[String]) = {
    val p = SparkGlobal.activeParser(args)
    val imgPath = p.getOptionPath("tif", "./", "Directory with tiff projections, dark, "+
      "and flat field images")
    val imgSuffix = p.getOptionString("suffix", ".tif", "Suffix to be added to image path *.sss "+
      "to find the image files")

    val savePath = p.getOptionPath("save", imgPath, "Directory for output")
    val checkpointResults = p.getOptionBoolean("checkpoint", false, "Write intermediate results "+
      "as output")
    val maxDarkVal = p.getOptionDouble("maxdark", 700, "Maximum value for dark images")
    val maxProjVal = p.getOptionDouble("maxproj", 1750, "Maximum value for projection images")
    val minFlatVal = p.getOptionDouble("minflat", maxProjVal, "Minimum value for a flat image")
    val maxSinogramNum = p.getOptionInt("maxsinogram", Integer.MAX_VALUE,
      "Maximum sinogram to generate")

    (SinogramSettings(imgPath+"/*"+imgSuffix, savePath, checkpointResults, maxDarkVal,
      maxProjVal, minFlatVal, maxSinogramNum), p)
  }

  // calculate statistics for an array
  def arrStats(inArr: Array[Double]) = ImStats(inArr.min, inArr.sum / (1.0 * inArr.length),
    inArr.max)

  /**
   * Uses pattern matching to identify slice types and then processes reach subgroup accordingly
   */
  // classify the slices based on their mean intensity
  def labelSlice(settings: SinogramSettings)(inSlice: (D3int, (ImStats, Array[Double]))) = {
    val sliceType = inSlice._2._1.mean match {
      case c: Double if c < settings.maxDarkVal => 0 // dark
      case c: Double if c < settings.maxProjVal => 2 // proj
      case c: Double if c >= settings.minFlatVal => 1 // flat field
      case _ => -1 // images to throw away
    }
    (sliceType, (DenseVector(inSlice._2._2), inSlice._1))
  }
}

object Sinograms {
  def main(args: Array[String]) {
    val (settings, p) = SinogramCommon.getParameters(args)
    p.checkForInvalid()
    val sc = SparkGlobal.getContext("SinogramTool").sc

    // read in a directory of tiffs (as a live stream)
    val tiffSlices = sc.tiffFolder(settings.imgPath)

    // read the values as arrays of doubles
    val doubleSlices = tiffSlices.loadAs2D(false)

    // structure for statSlices is (filename,(imstats,imArray))
    val statSlices = doubleSlices.mapValues {
      cArr =>
        val cEles = cArr.get
        (SinogramCommon.arrStats(cEles), cEles)
    }
    val groupedSlices = statSlices.map(SinogramCommon.labelSlice(settings))
    // for averaging together flats and darks
    def calcAvgImg(inRDD: RDD[(Int, (DenseVector[Double], D3int))]) = {
      val allImgs = inRDD.map { cvec => cvec._2._1 }.map(invec => (invec, 1))
      allImgs.reduce { (vec1, vec2) => (vec1._1 + vec2._1, vec1._2 + vec2._2) }
    }
    // for correcting projections and not crashing if the flats or darks are missing
    def correctProj(curProj: DenseVector[Double], darkImg: (DenseVector[Double], Int),
                    flatImg: (DenseVector[Double], Int)) = {
      val darkVec = if (darkImg._2 > 0) darkImg._1 / (1.0 * darkImg._2) else curProj * 0.0
      val flatVec = if (flatImg._2 > 0) flatImg._1 / (1.0 * flatImg._2) else curProj * 0.0 +
        curProj.max
      (curProj - darkVec) / (flatVec - darkVec)
    }
    if (settings.checkpointResults) {
      groupedSlices.mapValues {
        slice =>
          SinogramCommon.arrStats(slice._1.toArray)
      }.saveAsTextFile(settings.savePath+"all_imgs.hdtxt")
    }

    val avgDark = calcAvgImg(groupedSlices.filter(_._1 == 0))
    println(("Dark Image:#", avgDark._2, SinogramCommon.arrStats(avgDark._1.toArray)))
    val avgFlat = calcAvgImg(groupedSlices.filter(_._1 == 1))
    println(("Flat Image:#", avgFlat._2, SinogramCommon.arrStats(avgFlat._1.toArray)))
    val projs = groupedSlices.filter(_._1 == 2).map { evec => (evec._2._2, evec._2._1) }.
      mapValues { proj => correctProj(proj, avgDark, avgFlat) }.
      persist(SparkGlobal.getSparkPersistence())
    // just write out the statistics 
    if (settings.checkpointResults) projs.mapValues { proj =>
      SinogramCommon.arrStats(proj.toArray)
    }.saveAsTextFile(settings.savePath+"cor_projs.hdtxt")
    // mllib implementation
    val objSize = doubleSlices.first()._2.getDim()
    val projCount = projs.map(_._1.z).max
    // sort projects by filename and replace with an index
    val idProjs = projs.
      map(inval => (inval._2, inval._1.z)).
      map { inProj => (inProj._2, new DenseMatrix(objSize.gx, objSize.gy, inProj._1.toArray)) }.
      repartition(3 * sc.getExecutorStorageStatus.length)
    // calculate the projCount (largest dimension of the output array

    // convert into rows
    val idSino = idProjs.mapPartitions {
      inPart =>
        val firstProj = inPart.next
        var outSino = (0 until scala.math.min(firstProj._2.cols, settings.maxSino)).
          map { c => (c, DenseMatrix.zeros[Double](objSize.gx, projCount + 1)) }

        for (curProj <- inPart ++ Seq(firstProj)) {
          for (c <- 0 until outSino.length) outSino(c)._2(::, curProj._1) := curProj._2(::, c)
        }
        outSino.iterator
    }.reduceByKey(_ + _)

    // write the sinograms to disk as csv files
    idSino.foreach { csino =>
      csvwrite(new java.io.File(settings.savePath+"sino"+csino._1+""+
        ".csv"), csino._2)
    }
    sc.stop
  }
}

object SinogramStreaming {
  def main(args: Array[String]) {
    import org.apache.spark.streaming.{ Seconds, StreamingContext }
    import org.apache.spark.streaming.StreamingContext._
    val (settings, p) = SinogramCommon.getParameters(args)

    p.checkForInvalid()
    val ssc = SparkGlobal.getContext("SinogramTool").sc.toStreaming(30)
    // read in a directory of tiffs (as a live stream)
    val tiffSlices = ssc.tiffFolder(settings.imgPath).filter(_._1 contains ".tif")
    // read the values as arrays of doubles
    val doubleSlices = tiffSlices.loadAsValues

    // structure for statSlices is (filename,(imstats,imArray))
    val statSlices = doubleSlices.mapValues {
      cArr => (SinogramCommon.arrStats(cArr.get), cArr.get)
    }
  }

}