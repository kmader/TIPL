package fourquant.imagej

import ij.ImagePlus
import ij.process.ImageProcessor
import org.apache.spark.annotation.Experimental

/**
 * A class for storing histogram information
 */
case class IJHistogram(bin_centers: Array[Double], counts: Array[Int]) {
  //TODO add sql udt
  def toPairs() = bin_centers.zip(counts)
  override def toString() = {
    ("Hist:\n"+toPairs().filter(_._2>0).map(ij => ij._1+"\t"+ij._2).mkString("\n\t"))
  }

  def interp(newMin: Double, newMax: Double, newCount: Int) = {
    val centStep = (newMax-newMin)/(newCount-1)
    val newCents = newMin.to(newMax,centStep).toArray
    IJHistogram(newCents,IJHistogram.histConverter(bin_centers,counts,newCents))
  }

  def normalized_counts() = {
    val total = counts.sum.toDouble
    counts.map(_/total)
  }

  /**
   * Histogram subtraction for a simple distance metric
   * @param ij2 the compared distance metric
   * @return the difference between bins normalized by the total number of pixels (0..1)
   */
  @Experimental
  def -(ij2: IJHistogram) = {
    val colMin = (bin_centers++ij2.bin_centers).min
    val colMax = (bin_centers++ij2.bin_centers).max
    val newMe = this.interp(colMin,colMax,IJHistogram.histInterpCount).normalized_counts
    val newYou = ij2.interp(colMin,colMax,IJHistogram.histInterpCount).normalized_counts

    newMe.zip(newYou).map(ab => Math.abs(ab._1 - ab._2)).sum/2.0
  }
}

object IJHistogram extends Serializable {

  val histInterpCount = 10000
  val PERMISSIVE = true

  /**
   * convert one spacing of histogram to another (very discrete)
   * @param recCents
   * @param recCount
   * @param newCents
   * @return
   */
  def histConverter(recCents: Array[Double], recCount: Array[Int], newCents: Array[Double]) = {
    val recPairs = recCents.zip(recCount)
    val centStep = (newCents.max-newCents.min)/newCents.length // assume even spacing

    newCents.map{
      centPos =>
        recPairs.
          filter(rp => Math.abs(rp._1-centPos)<(centStep/2.0)). // keep only the window pts
          map(_._2).sum // keep the count and sum it
    }
  }


  /**
   * Prevents acquiring a imageprocessor (like 32-bit) which is incapable of creating useful,
   * histogram, requires updating soon
   * @param nip
   */
  implicit class RobustImagePlus(nip: ImagePlus) {
    /**
     * @note Converts the image to 16-bit if a histogram cannot be produced from the processor
     * @return
     */
    def getHistProcessor(): ImageProcessor = {
      val cip = nip.getProcessor()
      Option(cip.getHistogram()) match {
        case Some(hist) => cip
        case None =>
          import PortableImagePlus.implicits._
          nip.run("16-bit").getProcessor()
      }
    }
  }
  /**
   * Since the ImageProcessor class throws back nulls, improperly sized arrays and all sorts of
   * unusable garbage some of the functions need to be improved
   * @param ip
   */
  implicit class RobustImageProcessor(ip: ImageProcessor) {
    def getSmartHistogram(cRange: (Double,Double), bins: Int) = {
      ip.setHistogramRange(cRange._1,cRange._2)
      ip.setHistogramSize(bins)
      val histArr = Option(ip.getHistogram()) // java and its shitty npes

      val centStep = (cRange._2-cRange._1)/(bins-1)
      val outCents = cRange._1.to(cRange._2,centStep).toArray
      val outBins = histArr match {
        case Some(properOutput) if (properOutput.length==ip.getHistogramSize()) =>
          properOutput

        case Some(poorlySized) if (poorlySized.length>0) => // resize
          val recBins =
            ip.minValue().to(ip.maxValue(),
              (ip.maxValue()-ip.minValue())/poorlySized.length).toArray
          histConverter(recBins,poorlySized,outCents)
        case None if PERMISSIVE =>
          System.err.println(s"Histogram Returned an Invalid Result: "+ip)
          outCents.map(_.toInt*0)
        case None if !(PERMISSIVE) =>
          throw new RuntimeException(
            s"Histogram Returned an Invalid Result:"+ip
          )
      }

      (outCents,outBins)
    }
  }

  def fromIJ(inImg: Option[ImagePlus] = None, inRange: Option[(Double,Double)] = None,
              bins: Int = 60000) = {
    val cProc = inImg.getOrElse(Spiji.getCurImage).getHistProcessor.
      duplicate() // don't screw the old one

    val cRange = inRange.getOrElse((cProc.minValue,cProc.maxValue))
    assert(cRange._1<cRange._2,"Histogram maximum should be above minimum")

    val (centArr,countArr) = cProc.getSmartHistogram(cRange,bins)

    assert(cProc.getHistogramMin==cRange._1,"Minimum value is not set properly")
    assert(cProc.getHistogramMax==cRange._2,"Maximum value is not set properly")
    assert(countArr.length==bins, "Bin Count should match output")
    IJHistogram(
      centArr,
      countArr
    )
  }
}
