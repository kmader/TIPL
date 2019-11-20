package ch.fourquant.images.debug

import ch.fourquant.images.AbstractImageSource
import fourquant.imagej.PortableImagePlus
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

/**
 * Created by mader on 10/9/15.
 */
class DefaultSource extends AbstractImageSource {
  override def readImagesFromPath(sc: SparkContext, path: String, parameters: Map[String,
    String]): () => RDD[(String, PortableImagePlus)] = {
    val count = parameters.getOrElse("count","5").toInt
    import fourquant.imagej.scOps._
    () => sc.createEmptyImages(path, count, 10, 11,
      (i: Int) => (i - 1) * 1000 + 1000, None)

  }
}
