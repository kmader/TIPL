package fourquant.io

import fourquant.ImageSparkInstance
import fourquant.arrays.{BreezeOps, Positions}
import fourquant.io.ImageIOOps._
import fourquant.io.SparkImageIOTests.GeoPoint
import fourquant.labeling.ConnectedComponents
import fourquant.labeling.ConnectedComponents.LabelCriteria
import fourquant.tiles.{TilingStrategies, TilingStrategy2D}
import fourquant.utils.SilenceLogs
import org.scalatest.{FunSuite, Matchers}

class SparkImageIOTests extends FunSuite with Matchers with ImageSparkInstance with SilenceLogs {
  override def useLocal: Boolean = true

  override def bigTests: Boolean = false

  override def useCloud: Boolean = true
  if (useLocal) {
    test("Read a small test image locally") {
      val imgFileName = ImageTestFunctions.makeImage(500, 500, "tif")
      val littleTif = sc.binaryFiles(imgFileName).first()._2

      val is = ImageIOOps.createStream(littleTif.open)
      ImageIOOps.readTileDouble(is, Some("tif"), 0, 0, 100, 200, None) match {
        case Some(cTile) =>
          cTile.length shouldBe 200
          cTile(0).length shouldBe 100
          cTile(10)(10) shouldBe 255.0 +- 0.1
          val allPix = cTile.flatten

          allPix.min shouldBe 0.0 +- 0.1
          allPix.max shouldBe 255.0 +- 0.1
          (allPix.sum / 255) shouldBe 299.0 +- .5

          println("Loaded tile is :" + cTile)
        case None =>
          throw new IllegalArgumentException("Cannot be empty")
      }
    }
  }
  test("Read image from a portable-data-stream open") {

    val bigTif = sc.binaryFiles(esriImage).first()._2
    val is = ImageIOOps.createStream(bigTif.open)
    import fourquant.io.BufferedImageOps.implicits.charImageSupport
    ImageIOOps.readTileArray[Char](is, Some("tif"),36000,6000,2000,2000, None) match {
      case Some(cTile) =>
        cTile.flatten.min shouldBe 0
        cTile.flatten.max shouldBe 13
        cTile.flatten.map(_.toDouble).sum shouldBe 144647.0 +- 0.5
        println("Non Zero Elements:"+cTile.flatten.filter(_>0).length)
      case None =>
        false shouldBe true
    }
  }
  test("Read image from a portable-data-stream cached") {

    import fourquant.utils.IOUtils.LocalPortableDataStream
    val bigTif = sc.binaryFiles(esriImage).mapValues(_.cache).first()._2
    val is = ImageIOOps.createStream(bigTif.getUseful())
    import fourquant.io.BufferedImageOps.implicits.charImageSupport
    ImageIOOps.readTileArray[Char](is, Some("tif"),36000,6000,2000,2000, None) match {
      case Some(cTile) =>
        cTile.flatten.min shouldBe 0
        cTile.flatten.max shouldBe 13
        cTile.flatten.map(_.toDouble).sum shouldBe 144647.0 +- 0.5
        println("Non Zero Elements:"+cTile.flatten.filter(_>0).length)
      case None =>
        false shouldBe true
    }
  }

  test("Load image in big tiles") {
    import TilingStrategies.Grid._
    val tImg = sc.readTiledDoubleImage(esriImage,
      1000, 1000, 100)
    //tImg.count shouldBe 1600
    tImg.first._2.length shouldBe 1000
    tImg.first._2(0).length shouldBe 1000
  }

  test("Spot Check real Data from the big image as double") {
    import fourquant.io.BufferedImageOps.implicits.directDoubleImageSupport
    // just one tile
    implicit val ts = new TilingStrategy2D() {
      override def createTiles2D(fullWidth: Int, fullHeight: Int, tileWidth: Int, tileHeight:
      Int): Array[(Int, Int, Int, Int)] = Array((36000,8000,2000,2000))
    }
    val tImg = sc.readTiledImage[Double](testDataDir + "Hansen_GFC2014_lossyear_00N_000E.tif",
      2000,2000,100)

    import BreezeOps._
    tImg.count shouldBe 1
    tImg.getTileStats.collect.headOption match {
      case Some((cKey,cTile)) =>
        cTile.min shouldBe 0
        cTile.max shouldBe 13
        (cTile.mean*cTile.count) shouldBe 1785.0 +- 0.5
        println("Non Zero Elements:"+cTile.nzcount)
      case None =>
        false shouldBe true
    }
  }
  test("Spot Check real Data from the big image as char") {
    import fourquant.io.BufferedImageOps.implicits.charImageSupport
    // just one tile
    implicit val ts = new TilingStrategy2D() {
      override def createTiles2D(fullWidth: Int, fullHeight: Int, tileWidth: Int, tileHeight:
      Int): Array[(Int, Int, Int, Int)] = Array((36000,8000,2000,2000))
    }
    val tImg = sc.readTiledImage[Char](testDataDir + "Hansen_GFC2014_lossyear_00N_000E.tif",
      2000,2000,100)
    import BreezeOps._
    tImg.count shouldBe 1
    tImg.getTileStats.collect.headOption match {
      case Some((cKey,cTile)) =>
        cTile.min shouldBe 0
        cTile.max shouldBe 13
        (cTile.mean*cTile.count) shouldBe 1785.0 +- 0.5
        println("Non Zero Elements:"+cTile.nzcount)

      case None =>
        false shouldBe true
    }
  }


  test("Test Previews") {
    import TilingStrategies.Grid._
    import fourquant.io.BufferedImageOps.implicits.directDoubleImageSupport
    val imgPath = ImageTestFunctions.makeImagePath(50,50,"tif",
      "/Users/mader/Dropbox/Informatics/spark-imageio/test-data/")

    val tiledImage = sc.readTiledImage[Double](imgPath, 10, 25, 80).cache
    import fourquant.tiles.Previews.implicits.previewImage

    val myImg = tiledImage.base64Preview(0.25)
    val bString = myImg.first
    println(bString)
    bString._2.length should be > 10
  }

  test("Test tile preview") {
    import TilingStrategies.Grid._
    import fourquant.io.BufferedImageOps.implicits.directDoubleImageSupport
    val imgPath = ImageTestFunctions.makeImagePath(50,50,"tif",
      "/Users/mader/Dropbox/Informatics/spark-imageio/test-data/")

    val tiledImage = sc.readTiledImage[Double](imgPath, 10, 25, 80).cache
    import fourquant.tiles.Previews.implicits.previewTiles
    import fourquant.arrays.Positions._
    val myImg = tiledImage.tilePreview(1/5.0,10,10)
    println(myImg)
    myImg.length() should be > 10
  }

  test("Quick Component Labeling") {
    import TilingStrategies.Grid._

    val imgPath = ImageTestFunctions.makeImagePath(50,50,"tif",
      "/Users/mader/Dropbox/Informatics/spark-imageio/test-data/")

    val tiledImage = sc.readTiledDoubleImage(imgPath, 10, 25, 80).cache

    val fTile = tiledImage.first

    fTile._2(0).length shouldBe 10
    fTile._2.length shouldBe 25

    val lengthImage = tiledImage.mapValues(_.length)

    import BreezeOps._
    import Positions._

    val tileCount = tiledImage.count()

    val bm = tiledImage.toMatrixRDD()


    val nonZeroEntries = tiledImage.sparseThresh(_>0).cache

    implicit val doubleLabelCrit = new LabelCriteria[Double] {
      override def matches(a: Double, b: Double): Boolean = true//Math.abs(a-b)<0.75
    }
    val compLabel = ConnectedComponents.Labeling2D(nonZeroEntries,(3,3))

    val components = compLabel.map(_._2._1).countByValue()

    val nzCount = nonZeroEntries.count()

    val histogram = nonZeroEntries.map(_._2).histogram(20)


    println(("Tile Count",tileCount,"Non-Zero Count",nzCount,"Components",components.size))

    println("Histogram"+" "+histogram._1.zip(histogram._2).mkString(" "))
    print("Components:"+components.mkString(", "))

    tileCount shouldBe 300
    nzCount shouldBe 2088
  }

  test("Tiny Image Tiling and Thresholding Test") {
    import TilingStrategies.Grid._

    val imgPath = ImageTestFunctions.makeImage(100,100,"tif")

    sc.addFile(imgPath)

    val tiledImage = sc.readTiledDoubleImage(imgPath.split("/").reverse.head,
        10, 20, 80).cache

    val fTile = tiledImage.first

    fTile._2(0).length shouldBe 10
    fTile._2.length shouldBe 20

    val lengthImage = tiledImage.mapValues(_.length)

    import BreezeOps._
    import Positions._

    val tileCount = tiledImage.count()

    val bm = tiledImage.toBlockMatrix()


    val nonZeroEntries = tiledImage.sparseThresh(_>0)


    val nzCount = nonZeroEntries.count()

    val entries = bm.toCoordinateMatrix().entries.filter(_.value>0)

    val entryCount = entries.count

    val histogram = nonZeroEntries.map(_._2).histogram(20)


    println(("Tile Count",tileCount,"Non-Zero Count",nzCount, "Entries Count",entryCount))

    println("Histogram"+" "+histogram._1.zip(histogram._2).mkString(" "))
    println("Sampling:"+entries.takeSample(false,20).mkString("\n"))

    tileCount shouldBe 500
    nzCount shouldBe 298
    nzCount shouldBe entryCount
  }


  if (bigTests) {
    test("load image as double") {
      import TilingStrategies.Grid._
      import fourquant.io.BufferedImageOps.implicits.directDoubleImageSupport
      val tImg = sc.readTiledImage[Double](testDataDir + "Hansen_GFC2014_lossyear_00N_000E.tif",
        2000,2000,100)
      //tImg.count shouldBe 64
      import BreezeOps._
      val results = tImg.getTileStats.cache()
      results.filter(_._2.nzcount>0).foreach { cTile => println(cTile._1+" => "+cTile._2)}
      val resTable = results.collect()
      val nzCount  = resTable.filter(_._2.nzcount>0).length
      nzCount shouldBe 13
      println("Final Results (nzTiles:"+nzCount+"): "+resTable.mkString(", "))
    }

    test("load image as char") {
      import TilingStrategies.Grid._
      import fourquant.io.BufferedImageOps.implicits.charImageSupport
      val tImg = sc.readTiledImage[Char](testDataDir + "Hansen_GFC2014_lossyear_00N_000E.tif",
        2000,2000,100)
      //tImg.count shouldBe 64
      import BreezeOps._
      val results = tImg.getTileStats.cache()
      results.filter(_._2.nzcount>0).foreach { cTile => println(cTile._1+" => "+cTile._2)}
      val resTable = results.collect()
      val nzCount  = resTable.filter(_._2.nzcount>0).length
      nzCount shouldBe 13
      println("Final Results (nzTiles:"+nzCount+"): "+resTable.mkString(", "))
    }

    test("Full Image Tiling and Thresholding Test") {
      import TilingStrategies.Grid._

      val tiledImage = {
        var tempImg = sc.readTiledDoubleImage(testDataDir + "Hansen_GFC2014_lossyear_00N_000E.tif",
          1500, 1500, 80)
        if (useLocal) {
          tempImg
        } else {
          tempImg.cache
        }
      }
      val fTile = tiledImage.first
      fTile._2.length shouldBe 1500
      fTile._2(0).length shouldBe 1500

      val lengthImage = tiledImage.mapValues(_.length)

      import BreezeOps._
      import Positions._

      val tileCount = tiledImage.count()
      val nonZeroEntries = if (useLocal) tiledImage.sparseThresh(_>0) else
        tiledImage.sparseThresh(_>0).cache

      val nzCount = nonZeroEntries.count()

      val histogram = nonZeroEntries.map(_._2).histogram(20)
      val sampleData = nonZeroEntries.map(kv => ((kv._1.getX,kv._1.getY),kv._2)).
        takeSample(false,20)
        .mkString("\n")

      println("Histogram"+" "+histogram._1.zip(histogram._2).mkString(" "))
      println("Sampling:"+sampleData)

      println(("Tile Count",tileCount,"Non-Zero Count",nzCount))



      // spark sql
      val sqlContext = new org.apache.spark.sql.SQLContext(sc)
      import sqlContext.implicits._



      val nzDataFrame = nonZeroEntries.map{
        case (pkey,pvalue) => GeoPoint(pkey._1,pkey._2,pkey._3,(pvalue+2000).toInt)
      }.toDF()

      nzDataFrame.registerTempTable("geopoints")
      nzDataFrame.write.parquet("map_points")

      tileCount shouldBe 200
      nzCount shouldBe 235439
    }
  }

  if (useCloud) {
    test("Cloud Test") {
      import TilingStrategies.Grid._
      sc.hadoopConfiguration.set("fs.s3n.awsAccessKeyId", "AKIAJM4PPKISBYXFZGKA")
      sc.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey",
        "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")

      val tiledImage = sc.readTiledDoubleImage("s3n://geo-images/*.tif", 1000, 2000, 80)
      val fTile = tiledImage.first
      fTile._2.length shouldBe 2000
      fTile._2(0).length shouldBe 1000

      val lengthImage = tiledImage.mapValues(_.length)

      import BreezeOps._
      import Positions._

      val nonZeroEntries = tiledImage.sparseThresh(_>0)

      val nzCount = nonZeroEntries.count()
      val tileCount = tiledImage.count()
      println(("Tile Count",tileCount,"Non-Zero Count",nzCount))

      tileCount shouldBe 800
      nzCount shouldBe 235439
    }
  }


}
object SparkImageIOTests extends Serializable {
  case class GeoPoint(name: String, i: Int, j: Int, year: Int)
}
