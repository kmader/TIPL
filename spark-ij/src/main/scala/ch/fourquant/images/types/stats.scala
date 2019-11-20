package ch.fourquant.images.types

import fourquant.imagej.IJHistogram

/**
 * If it has the same name, some scalac things get angry, probably a bug of some sorts
 * Error:scalac: error while loading ImageStatistics, illegal class file dependency between 'object ImageStatistics' and 'class ImageStatistics'
 */

case class HistogramCC(bin_centers: Array[Double], bin_counts: Array[Int]) {
  def this(hist: IJHistogram) = this(hist.bin_centers,hist.counts)
}





