package fourquant.imagej

import java.util

import fourquant.imagej.IJSqlTest.NamedArray
import fourquant.io.ScifioOps
import io.scif.config.SCIFIOConfig
import io.scif.img.{SCIFIOImgPlus, ImgOpener}
import org.apache.spark.LocalSparkContext
import org.apache.spark.LocalSparkContext.SilenceLogs
import org.apache.spark.sql.SQLContext
import org.scalatest.{Matchers, FunSuite}
import scala.collection.JavaConversions._
import net.imagej.ImageJ

import scala.collection.mutable.ArrayBuffer

/**
  * Created by mader on 4/8/16.
  */
class IjRDDTests extends FunSuite with LocalSparkContext with Matchers with SilenceLogs  {
  val conList = ArrayBuffer(
    ("LocalSpark", (a: String) => getNewSpark("local[2]", a))
  )

  implicit val ijs = SpijiTests.ijs
  import SpijiTests.makeTestImages
  import fourquant.imagej.scOps._

  test("Read IJ Stack") {
    val ij = new net.imagej.ImageJ()

    val dcmImage = ij.scifio().datasetIO().open(SpijiTests.first_local_img,
      new io.scif.config.SCIFIOConfig().groupableSetGroupFiles(true)
    )
    println(s" Dimensions ${(0 to dcmImage.numDimensions()).map(dcmImage.dimension(_))}")
    println("Metadata")
    println("Properties")
    println(dcmImage.getProperties.mkString(","))

    dcmImage.dimension(2) shouldBe 68



  }
  test("Read single stack") {
    // read one image and have it discover the rest

    val sic = new SCIFIOConfig()
    println(s"Default groupable ${sic.groupableIsGroupFiles()}")
    val sicGrp = sic.groupableSetGroupFiles(true).imgOpenerSetOpenAllImages(true)

    val io = new ImgOpener()


    val inImagesGrp: util.List[SCIFIOImgPlus[_]] = io.openImgs(SpijiTests.first_local_img,sicGrp)

    val sicNoGrp = sic.groupableSetGroupFiles(false).imgOpenerSetOpenAllImages(false)
    val inImagesNoGrp = io.openImgs(SpijiTests.first_local_img,sicNoGrp)
    println(s"Image sizes: Group:${inImagesGrp.size()}, No Group: ${inImagesNoGrp.size()}")

    inImagesNoGrp.size() shouldBe 1
    inImagesGrp.size() shouldBe 1

    val imDim = (0 to inImagesGrp.head.numDimensions()).map(inImagesGrp.head.dimension(_))
    val imNDim = (0 to inImagesNoGrp.head.numDimensions()).map(inImagesGrp.head.dimension(_))


    println(s"Image sizes: Group:$imDim, No Group: $imNDim")
    println("Metadata")
    println(inImagesGrp.head.getMetadata.getAll.mkString(","))
    println("Properties")
    println(inImagesGrp.head.getProperties.mkString(","))


    imDim(2) shouldBe 68

  }

  for ((conName, curCon) <- conList) {
    val sc = curCon("Spiji")

    test(conName + ": Load Image Stack") {
      val test = sc.loadImages(SpijiTests.first_local_img,5)
      test.count shouldBe 1
      val img = test.first()._2
      val is = img.getImageStatistics()

      is.pts shouldBe 262144

    }

    test(conName + ": Load Directory Full of Images") {
      val test = sc.loadImages(s"${SpijiTests.local_io_path}/*.dcm",5)
      test.count shouldBe 68
      val is = test.first()._2.getImageStatistics()
      is.pts shouldBe 262144

    }
  }

}
