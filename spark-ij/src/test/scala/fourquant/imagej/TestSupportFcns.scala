package fourquant.imagej

import fourquant.imagej.scOps._
import org.apache.spark.SparkContext

/**
 * A few simple functions to generate test data
 */
object TestSupportFcns extends Serializable {
  val width = 100
  val height = 50
  val imgs = 5
  val localMasterWorkers = false
  val runLocal = true
  val runLocalCluster = false
  val ijs = ImageJSettings("/Applications/Fiji.app/", showGui = false, runLaunch = false, record
    = false)

  val headlessIJ = ImageJSettings("/",showGui = false, runLaunch = false, record=false, forceHeadless = true)

  def makeTestImages(sc: SparkContext, fact: Int = 1,
                     imgs: Int, width: Int, height: Int) =
    sc.createEmptyImages("/Users/mader/imgs/", imgs, width, height,
      (i: Int) => fact * (i - 1) * 1000 + 1000, Some(ijs))

}