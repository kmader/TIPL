package fourquant.io

import fourquant.ImageSparkInstance
import org.scalatest.{FunSuite, Matchers}
/**
 * Created by mader on 9/14/15.
 */
class SingleImageIO extends FunSuite with Matchers with ImageSparkInstance {
  val seqFile = "/Users/mader/Dropbox/Informatics/spark-imageio/test-data/test.hsf"
  val imgFolder = "/Users/mader/Dropbox/Informatics/spark-imageio/test-data/test*.tif"
  test("Read in a single image") {
    val is = ImageTestFunctions.makeVSImg(50, 50, "tif")
    ImageIOOps.readWholeImage(is, Some("tif"), None) match {
      case Some(cImage) =>
        cImage.getWidth shouldBe 50
        cImage.getHeight shouldBe 50
      case None =>
        throw new IllegalArgumentException("Cannot be empty")
    }

    ImageIOOps.readWholeImageArrayDouble(is, Some("tif"), None) match {
      case Some(cImage) =>
        cImage.length shouldBe 50
        cImage(0).length shouldBe 50
      case None =>
        throw new IllegalArgumentException("Cannot be empty")
    }
  }

  test("Read single image locally") {
    import java.io._
    val is = ImageIOOps.createStream(
      new FileInputStream(new File
    ("/Users/mader/Dropbox/4Quant/Projects/RadarImagesDSmall/walps_figure/melting-summary-1.png"))
    )
    ImageIOOps.readWholeImage(is, Some("png"), None) match {
      case Some(cImage) =>
        cImage.getWidth shouldBe 1344
        cImage.getHeight shouldBe 960
      case None =>
        throw new IllegalArgumentException("Cannot be empty")
    }
  }

  test("Read single image array locally") {
    import java.io._
    val is = ImageIOOps.createStream(
      new FileInputStream(new File
      ("/Users/mader/Dropbox/Informatics/spark-imageio/test-data/regions_80_80.tif"))
    )
    ImageIOOps.readWholeImageArrayDouble(is, Some("tif"), None) match {
      case Some(cImage) =>
        cImage.length shouldBe 80
        cImage(0).length shouldBe 80
      case None =>
        throw new IllegalArgumentException("Cannot be empty")
    }
  }

  test("Read single jpg image array locally") {
    import java.io._
    val is = ImageIOOps.createStream(
      new FileInputStream(new File
      ("/Users/mader/Dropbox/Personal/MahmPhotos/146.JPG"))
    )
    ImageIOOps.readWholeImageArrayDouble(is, Some("jpg"), None) match {
      case Some(cImage) =>
        cImage.length shouldBe 2448
        cImage(0).length shouldBe 3264
      case None =>
        throw new IllegalArgumentException("Cannot be empty")
    }
  }

  test("Read a sequence file") {
    import ImageIOOps._
    sc.readImageSequenceDouble(seqFile,5).foreach{
      cv =>
        println(cv._1+": "+cv._2)
    }
  }

  test("Read a folder of images") {

    import ImageIOOps._
    sc.readWholeImagesDouble(imgFolder,5).foreach{
      cv =>
        println(cv._1+": "+cv._2)
    }
  }

  override def useLocal: Boolean = true

  override def bigTests: Boolean = false

  override def useCloud: Boolean = false
}
