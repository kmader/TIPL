package tipl.ij.scripting

import org.apache.spark.SparkContext
import org.scalatest.FunSuite
import tipl.ij.scripting.ImagePlusIO.PortableImagePlus
import org.apache.spark.SparkContext._
import tipl.ij.scripting.scOps._
/**
 * Ensure the distributed fiji code works as expected including reading and writing files, basic
 * plugin support, and histogram / table analysis
 * Created by mader on 1/16/15.
 */
class SpijiTests extends FunSuite with LocalSparkContext {
  def makeImages(sc: SparkContext) =
    sc.
      parallelize(1 to SpijiTests.imgs).
      map(i => ("/Users/mader/imgs/"+i.toString,Array.fill[Int](SpijiTests.width,SpijiTests
      .height)(i*1000))).
      mapValues(a => new PortableImagePlus(a))



  test("Creating synthetic images") {
    implicit val ijs = SpijiTests.ijs
    sc = getSpark("Spiji")
    val imgList = makeImages(sc)
    imgList runAll("Add Noise")
    imgList saveImages("/Users/mader/imgs",".jpg")
  }

  test("Image Immutability Test") {
    implicit val ijs = SpijiTests.ijs
    sc = getSpark("Spiji")
    val imgList = makeImages(sc).cache
    // execute them in the wrong order
    val wwNoise = imgList.runAll("Add Noise").runAll("Add Noise").
      getStatistics().first._2.stdDev
    val wNoise = imgList.runAll("Add Noise").getStatistics().first._2.stdDev
    val woNoise = imgList.getStatistics().first._2.stdDev
    print("None:"+woNoise+"\tOnce:"+wNoise+"\tTwice:"+wwNoise)
    assert(woNoise<1e-3,"Standard deviation of image is almost 0")
    assert(wNoise>woNoise,"With noise is more than without noise")
    assert(wwNoise>wNoise," With double noise is more then just noise")

  }

  test("Testing Parameter Sweep") {
    implicit val ijs = SpijiTests.ijs
    sc = getSpark("Spiji")
    val imgList = makeImages(sc)
    imgList.runAll("Add Noise").
      runRange("Median...","radius=1.0","radius=5.0").
      saveImagesLocal(".jpg")

  }


}
object SpijiTests extends Serializable {
  val width = 100
  val height = 50
  val imgs = 20
  val ijs = ImageJSettings("/Applications/Fiji.app/")
}
