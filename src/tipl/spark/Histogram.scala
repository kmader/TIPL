package tipl.spark

import java.io.Serializable
import scala.math._
import tipl.util.TImgTools
import tipl.formats.TImgRO
import org.apache.spark.rdd.PairRDDFunctions._
import org.apache.spark.SparkContext._

class Histogram extends Serializable {


}

object Histogram {
  def make(inImg: TImgRO, binCount: Int = 100) = {
    val asKV = KVImg.ConvertTImg(SparkGlobal.getContext("Histogram"), inImg, TImgTools.IMAGETYPE_FLOAT).getBaseImg
    val dv = asKV.map { npt => npt._2.asInstanceOf[Float]}
    val statview = dv.stats()
    val min = statview.min
    val max = statview.max
    val binFunc = (realVal: Float) => round((realVal - min) / (max - min) * binCount).intValue
    val ibinFunc = (binVal: Int) => (binVal / (binCount * 1.0) * (max - min) + min)
    val binz = dv.map { inval => (binFunc(inval), inval)}.groupByKey
    binz.mapValues {
      _.size
    }.sortByKey(true).map {
      inPt =>
        (ibinFunc(inPt._1), inPt._2)
    }

  }
}