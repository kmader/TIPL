package fourquant.io

import fourquant.io.ScifioOps.FQImgOpener
import io.scif.img.ImgOpener
import net.imglib2.`type`.numeric.integer.IntType
import net.imglib2.`type`.numeric.real.DoubleType
import org.scalatest.{FunSuite, Matchers}
import scala.collection.JavaConversions._
/**
 * Created by mader on 4/17/15.
 */
class ScifioTests extends FunSuite with Matchers {
val testDataDir = "/Users/mader/Dropbox/Informatics/spark-imageio/test-data/"
val esriImage = testDataDir + "Hansen_GFC2014_lossyear_00N_000E.tif"
val tiledGeoImage = testDataDir + "LC81390452014295LGN00_B1.TIF"
val verbose = false
val heavy = false

  val fqOpen = new FQImgOpener(new ImgOpener())
  test("Open a ESRI Geoimage") {
    val (creader,meta) = ScifioOps.readPath(esriImage)
    println(("meta",meta,"images", meta.getImageCount,"im0",meta.get(0),
      "planes",meta.get(0).getPlaneCount,"lengths",
      meta.get(0).getAxesLengths.mkString(","))
    )

    meta.getImageCount shouldBe 1
    meta.get(0).getPlaneCount shouldBe 1
    val al = meta.get(0).getAxesLengths
    al(0) shouldBe 40000
    al(1) shouldBe 40000

    val smallRegion = fqOpen.openRegion2D(tiledGeoImage,new DoubleType(),1000,1000,100,100)

    smallRegion.headOption match {
      case Some(img) =>

        val dim = {
          val td = new Array[Long](img.getImg().numDimensions())
          img.getImg().dimensions(td)
          td
        }
        println("Dim:"+dim.mkString(","))
      case None => assert(false,"Needs to read at least one image")
    }
  }
  test("Open A NASA Geoimage") {

    val (creader,meta) = ScifioOps.readPath(tiledGeoImage)
    println(("meta",meta,"images", meta.getImageCount,"im0",meta.get(0),
      "planes",meta.get(0).getPlaneCount,"lengths",
      meta.get(0).getAxesLengths.mkString(","))
    )

    meta.getImageCount shouldBe 1
    meta.get(0).getPlaneCount shouldBe 1
    val al = meta.get(0).getAxesLengths
    al(0) shouldBe 7621
    al(1) shouldBe 7791

    val smallRegion = fqOpen.openRegion2D(tiledGeoImage,new DoubleType(),1000,1000,100,100)

    smallRegion.headOption match {
      case Some(img) =>
        val dim = {
          val td = new Array[Long](img.getImg().numDimensions())
          img.getImg().dimensions(td)
          td
        }
        println("Dim:"+dim.mkString(","))
      case None => assert(false,"Needs to read at least one image")
    }

    for(i<-0 until creader.getImageCount) {
      println(("img",i,
        "plane-count",creader.getPlaneCount(i)
        ))
    }
  }

  test("Reading small byte region of nasa image") {
    val valRegion = ScifioOps.readRegion(tiledGeoImage, Array(1000L,1000L),Array(100L,100L))
    valRegion.length shouldBe (100*100*2)
    println(("Out-length",valRegion.length,"bytes per pixel",valRegion.length/(100*100)))
  }

  test("Reading small image from nasa image") {
    import fourquant.io.ScifioOps.ImgWithDim
    val valImage = ScifioOps.readRegionAsImg(tiledGeoImage, Array(1000L,1000L),Array(100L,100L),
    new IntType())
    println("My Value image:"+valImage+" c:")
    valImage.getPrimitiveArray[Array[Float]] match { //TODO why the hell is this a float???
      case Some(intyArray) =>
        intyArray.length shouldBe (100*100)
        println("Stats",(intyArray.min,intyArray.max,intyArray.filter(_>0).length,intyArray.sum))
        intyArray.min shouldBe 0
        intyArray.max shouldBe 65392
        intyArray.filter(_>0).length shouldBe 3745 // nonzero elements
      case None =>
        assert(false,"Should have an integer array underneath")
    }
  }
}
