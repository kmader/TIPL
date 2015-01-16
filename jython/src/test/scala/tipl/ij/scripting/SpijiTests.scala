package tipl.ij.scripting

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



  test("Creating synthetic images") {
    implicit val ijs = SpijiTests.ijs
    sc = getSpark("Spiji")
    val imgList = sc.
      parallelize(1 to SpijiTests.imgs).
      map(i => (i.toString,Array.fill[Int](SpijiTests.width,SpijiTests.height)(i*1000))).
      mapValues(a => new PortableImagePlus(a))
    imgList runAll("Add Noise")
    imgList saveImages("/Users/mader/imgs",".jpg")
  }

  test("Testing Parameter Sweep") {
    implicit val ijs = SpijiTests.ijs
    sc = getSpark("Spiji")
    val imgList = sc.
      parallelize(1 to SpijiTests.imgs).
      map(i => ("/Users/mader/imgs/"+i.toString,Array.fill[Int](SpijiTests.width,SpijiTests
      .height)(i*1000))).
      mapValues(a => new PortableImagePlus(a))
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
