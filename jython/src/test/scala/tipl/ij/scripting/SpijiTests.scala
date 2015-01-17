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
  def makeImages(sc: SparkContext, fact: Int = 1) =
    sc.
      parallelize(1 to SpijiTests.imgs).
      map(i => ("/Users/mader/imgs/"+i.toString,Array.fill[Int](SpijiTests.width,SpijiTests
      .height)(fact*(i-1)*1000+1000))).
      mapValues(a => new PortableImagePlus(a))



  test("Creating synthetic images") {
    implicit val ijs = SpijiTests.ijs
    sc = getSpark("Spiji")
    val imgList = makeImages(sc)
    imgList runAll("Add Noise")
    imgList saveImages("/Users/mader/imgs",".jpg")
  }

  test("Image Immutability Test : Noise") {
    implicit val ijs = SpijiTests.ijs
    sc = getSpark("Spiji")
    val imgList = makeImages(sc,0).cache
    // execute them in the wrong order
    val wwNoise = imgList.runAll("Add Noise").runAll("Add Noise").
      getStatistics().values.takeSample(false,1)(0).stdDev
    val wNoise = imgList.runAll("Add Noise").getStatistics().values.takeSample(false,1)(0).stdDev
    val woNoise = imgList.getStatistics().values.takeSample(false,1)(0).stdDev
    print("None:"+woNoise+"\tOnce:"+wNoise+"\tTwice:"+wwNoise)
    assert(woNoise<1e-3,"Standard deviation of image is almost 0")
    assert(wNoise>woNoise,"With noise is more than without noise")
    assert(wwNoise>wNoise," With double noise is more then just noise")
  }

  test("Image Immutability Test : Threshold ") {
    implicit val ijs = SpijiTests.ijs
    sc = getSpark("Spiji")
    val imgList = makeImages(sc,0).cache

    // execute them in the wrong order
    val wNoisewThreshTwice = imgList.runAll("Add Noise").
      runAll("applyThreshold","lower=0 upper=1000").runAll("applyThreshold","lower=0 upper=1000").
      getStatistics().values.takeSample(false,1)(0)
    val wNoisewThresh = imgList.runAll("Add Noise").runAll("applyThreshold","lower=0 upper=1000").
      getStatistics().values.takeSample(false,1)(0)
    val wNoise = imgList.runAll("Add Noise").
      getStatistics().values.takeSample(false,1)(0)
    val woNoise = imgList.getStatistics().values.takeSample(false,1)(0)
    print("\tNone:"+woNoise+"\n\tNoise:"+wNoise+"\n\tThresh:"+wNoisewThresh+"\n\tTThres" +
      ":"+wNoisewThreshTwice)
    assert(woNoise.mean==1000,"Mean of standard image should be 1000")
    assert(woNoise.stdDev<1e-3,"Std of standard image should be 0")
    assert(wNoise.stdDev>1,"With noise is should have a greater than 1 std")
    assert(wNoisewThresh.mean<255," With a threshold the mean should be below 255")
    assert(wNoisewThresh.stdDev>1," With a threshold the std should be greater than 0")
    assert(wNoisewThreshTwice.mean==255," With a second very loose threshold the mean should be 255")
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
