/**
 *
 */
package tipl.streaming

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import tipl.spark.IOOps._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.StreamingContext._
import tipl.spark.SparkGlobal
import breeze.linalg._
import org.apache.spark.rdd.RDD

/**
 * @author mader
 *
 */
object FlatFieldCorrection {
  def main(args: Array[String]): Unit = {
    val p = SparkGlobal.activeParser(args)
    val sc = SparkGlobal.getContext("FlatFieldCorrection").sc
    val ssc = sc.toStreaming(10)
    // check the folder every 10s for new images
    // read in a directory of tiffs (as a live stream)
    val tiffSlices = ssc.tiffFolder("/Volumes/WORKDISK/WorkData/StreamingTests/tif").filter(_._1
      contains ".tif")
    // read the values as arrays of doubles
    val doubleSlices = tiffSlices.loadAsValues


    // format for storing image statistics
    case class ImStats(min: Double, mean: Double, max: Double) extends Serializable


    // structure for statSlices is (filename,(imstats,imArray))
    val statSlices = doubleSlices.mapValues {
      cArr => (ImStats(cArr.get.min, cArr.get.sum / (cArr.getDim.gx*cArr.getDim.gy), cArr.get.max),
        cArr.get())
    }
    val darks = statSlices.filter { cImg => cImg._2._1.mean < 700}
    val flats = statSlices.filter { cImg => cImg._2._1.mean > 1750}
    val projs = statSlices.filter { cImg => cImg._2._1.mean >= 700 & cImg._2._1.mean <= 1750}

    def labelSlice(inSlice: (String, (ImStats, Array[Double]))) = {
      val sliceType = inSlice._2._1.mean match {
        case c: Double if c < 700 => 0 // dark
        case c: Double if c < 1750 => 2 // proj
        case c: Double if c >= 1750 => 1 // flat field
      }
      (sliceType, (DenseVector(inSlice._2._2), inSlice._1))
    }

    val groupedSlices = statSlices.map(labelSlice)

    // the rdd-based code for each time step
    def calcAvgImg(inRDD: RDD[(Int, (DenseVector[Double], String))]) = {
      val allImgs = inRDD.map { cvec => cvec._2._1}.map(invec => (invec, 1))
      allImgs.reduce { (vec1, vec2) => (vec1._1 + vec2._1, vec1._2 + vec2._2)}
    }

    def correctProj(curProj: DenseVector[Double], darkImg: (DenseVector[Double], Int),
                    flatImg: (DenseVector[Double], Int)) = {
      val darkVec = if (darkImg._2 > 0) darkImg._1 / (1.0 * darkImg._2) else curProj * 0.0
      val flatVec = if (flatImg._2 > 0) flatImg._1 / (1.0 * flatImg._2)
      else curProj * 0.0 +
        curProj.max
      (curProj - darkVec) / (flatVec - darkVec)
    }

    def arrStats(inArr: Array[Double]) = ImStats(inArr.min, inArr.sum / (1.0 * inArr.length),
      inArr.max)

    groupedSlices.foreachRDD { rdd =>
      val avgDark = calcAvgImg(rdd.filter(_._1 == 0))
      val avgFlat = calcAvgImg(rdd.filter(_._1 == 1))
      val projs = rdd.filter(_._1 == 2).map { evec => (evec._2._2, evec._2._1)}.
        mapValues { proj => arrStats(correctProj(proj, avgDark, avgFlat).toArray)}
      projs.saveAsTextFile("/Volumes/WORKDISK/WorkData/StreamingTests/cor_projs.txt")

    }
    ssc.start()
    ssc.awaitTermination()
  }
}