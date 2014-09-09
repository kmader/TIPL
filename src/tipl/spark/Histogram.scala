package tipl.spark

import java.io.Serializable
import scala.math._
import tipl.util.TImgTools
import tipl.formats.TImgRO
import org.apache.spark.rdd.PairRDDFunctions._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import ij.gui.HistogramWindow
import ij.process.ImageProcessor
import ij.ImagePlus
import tipl.tests.TestPosFunctions
import tipl.ij.TImgToImagePlus

class SpHistogram(inImg: TImgRO, bins: Int, windname: String = "Histogram") extends HistogramWindow(windname,new ImagePlus(),bins) with Serializable {
	lazy val (histTable,hmin,hmax) = SpHistogram.make(inImg,bins)
	lazy val curStats = imp.getStatistics(27, bins, hmin, hmax)
	
	override def showHistogram(junkImp: ImagePlus, bins: Int, minVal: Double, maxVal: Double): Unit = {
	  super.setup()
	  // extract the histogram array from the rdd
	  curStats.histogram = histTable.sortByKey(true).map(_._2).collect.toArray
	  this.imp =  TImgToImagePlus.MakeImagePlus(inImg)
	  showHistogram(this.imp,curStats)
	}
}

object SpHistogram {
  /**
   * Create a histogram from an image with the selected number of bins
   */
  def make(inImg: TImgRO, binCount: Int = 100): (RDD[(Double,Int)],Double,Double) = {
    val asKV = KVImg.ConvertTImg(SparkGlobal.getContext("Histogram"), inImg, TImgTools.IMAGETYPE_FLOAT).getBaseImg
    val dv = asKV.map { npt => npt._2.asInstanceOf[Float]}
    val statview = dv.stats()
    val min = statview.min
    val max = statview.max
    val binFunc = (realVal: Float) => round((realVal - min) / (max - min) * binCount).intValue
    val ibinFunc = (binVal: Int) => (binVal / (binCount * 1.0) * (max - min) + min)
    val binz = dv.map { inval => (binFunc(inval), inval)}.groupByKey
    (binz.mapValues {
      _.size
    }.sortByKey(true).map {
      inPt =>
        (ibinFunc(inPt._1), inPt._2)
    },min,max)
    
  }
  
  def show(inImg: TImgRO,bins:Int = 100,name: String = "Histogram Demo") = {

    val outWindow = new SpHistogram(inImg,bins, name)
  }
  
  def main(args: Array[String]): Unit = {
    val p = SparkGlobal.activeParser(args)
    val boxSize = p.getOptionInt("boxsize", 8, "The dimension of the image used for the analysis");

      val testImg = TestPosFunctions.wrapIt(boxSize,
                new TestPosFunctions.SphericalLayeredImage(boxSize / 2, boxSize / 2, boxSize / 2, 0, 1, 2))
      show(testImg)
  }
  
}