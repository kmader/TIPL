package fourquant.io

import java.awt.image.BufferedImage
import java.io._
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream

import fourquant.tiles.TilingStrategies
import fourquant.tiles.TilingStrategies.Simple2DGrid
import org.apache.commons.io.output.ByteArrayOutputStream
import org.scalatest.{FunSuite, Matchers}
import org.tukaani.xz.SeekableFileInputStream

class ImageIOTests extends FunSuite with Matchers {

  val testDataDir = "/Users/mader/Dropbox/Informatics/spark-imageio/test-data/"
  val bigImage = testDataDir + "Hansen_GFC2014_lossyear_00N_000E.tif"
  val tiledGeoImage = testDataDir + "LC81390452014295LGN00_B1.TIF"
  val verbose = false
  val heavy = false

  test("Load a geo-image with JAI") {
    //val tgFact = new GeoTIFFFactory()


    val p = new SeekableFileInputStream(
      new File(tiledGeoImage)
    )
    //val td = new GeoTIFFDirectory()
    //val td2 = new GeoTIFFDirectory()
    //tgFact.createDirectory()
    //val tgImg = tgFact.createDirectory()
  }

  test("Load a tile from a test image multiple times") {
    val is = ImageTestFunctions.makeVSImg(500, 500, "tif")

    ImageIOOps.readTile(is, None, 0, 0, 100, 100, None) match {
      case Some(cTile) =>
        cTile.getWidth shouldBe 100
        cTile.getHeight shouldBe 100
        println("Loaded tile is :" + cTile)
      case None =>
        throw new IllegalArgumentException("Cannot be empty")
    }

    ImageIOOps.readTile(is, None, 0, 0, 100, 50, None) match {
      case Some(cTile) =>
        cTile.getHeight shouldBe 50
        cTile.getWidth shouldBe 100
      case None =>
        throw new IllegalArgumentException("Cannot be empty")
    }

  }

  test("Make a very simple test image") {
    val newFile = ImageTestFunctions.makeImagePath(500,500,"tif",testDataDir)

  }

  test("Load tile from big image multiple times") {
    val is = ImageIOOps.createStream(
      new FileInputStream(new File(bigImage))
    )

    ImageIOOps.readTile(is, None, 0, 0, 100, 100, None) match {
      case Some(cTile) =>
        cTile.getWidth shouldBe 100
        cTile.getHeight shouldBe 100
        println("Loaded tile is :" + cTile)
      case None =>
        throw new IllegalArgumentException("Cannot be empty")
    }

    ImageIOOps.readTile(is, None, 0, 0, 100, 50, None) match {
      case Some(cTile) =>
        cTile.getHeight shouldBe 50
        cTile.getWidth shouldBe 100
      case None =>
        throw new IllegalArgumentException("Cannot be empty")
    }

  }

  test("Over-read a tile") {
    val is = ImageTestFunctions.makeVSImg(50, 50, "tif")
    ImageIOOps.readTileDouble(is, Some("tif"), 0, 0, 100, 100, None) match {
      case Some(cTile) =>
        cTile.length shouldBe 50
        cTile(0).length shouldBe 50
      case None =>
        throw new IllegalArgumentException("Cannot be empty")
    }
  }

  test("Read a tile outside of image") {
    val is = ImageTestFunctions.makeVSImg(50, 50, "png")

    ImageIOOps.readTileDouble(is, Some("png"), 100, 100, 100, 100, None).isEmpty shouldBe true
  }


  test("Load array data from tile") {
    val is = ImageTestFunctions.makeVSImg(500, 500, "tif")

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

    ImageIOOps.readTileDouble(is, Some("tif"), 10, 5, 25, 30, None) match {
      case Some(cTile) =>
        cTile.length shouldBe 30
        cTile(0).length shouldBe 25
        cTile(5)(0) shouldBe 255.0 +- 0.1
        println("Loaded tile is :" + cTile)

      case None =>
        throw new IllegalArgumentException("Cannot be empty")
    }

  }




  test("Read size of big image") {
    val is = ImageIOOps.createStream(
      new FileInputStream(new File(bigImage))
    )

    val iif = ImageIOOps.getImageInfo(is,None)
    println(iif)
    iif.height shouldBe 40000
    iif.width shouldBe 40000
    iif.count shouldBe 1
  }


  test("Load several tiles from a big image") {
    val is = ImageIOOps.createStream(
      new FileInputStream(new File(bigImage))
    )
    val tileStrat = new Simple2DGrid()
    val ttest = tileStrat.createTiles2D(35, 35, 10, 10)
    val tiles = ttest.flatMap(inPos => ImageIOOps.readTile(is, Some("tif"),
      inPos._1, inPos._2, inPos._3, inPos._4,None))
    tiles.length shouldBe 16
    all(tiles.map(_.getWidth())) shouldBe 10
    all(tiles.map(_.getHeight())) shouldBe 10


    val atest = tileStrat.createTiles2D(10, 10, 4, 3)
    val stiles = atest.flatMap(inPos => ImageIOOps.readTile(is, Some("tif"),
      inPos._1, inPos._2, inPos._3, inPos._4,None))
    stiles.length shouldBe 12
    all(stiles.map(_.getWidth())) shouldBe 4
    all(stiles.map(_.getHeight())) shouldBe 3
  }

  test("Load all tiles from a big image") {
    val is = ImageIOOps.createStream(
      new FileInputStream(new File(bigImage))
    )
    val info = ImageIOOps.getImageInfo(is,None)
    val tileStrat = new Simple2DGrid()
    val alltilepositions = tileStrat.createTiles2D(info.width, info.height, 1000, 1000)
    println(alltilepositions.mkString("\n"))
    alltilepositions.length shouldBe 1600
    val subpos = alltilepositions.take(10)

    val tiles = subpos.flatMap(inPos =>
      ImageIOOps.readTile(is, Some("tif"), inPos._1, inPos._2, inPos._3, inPos._4,None))
    tiles.length shouldBe 10
    all(tiles.map(_.getWidth())) shouldBe 1000
    all(tiles.map(_.getHeight())) shouldBe 1000

  }

  test("Load Real Data from the big image") {
    val is = ImageIOOps.createStream(
      new FileInputStream(new File(bigImage))
    )
    import fourquant.io.BufferedImageOps.implicits.charImageSupport
    ImageIOOps.readTileArray[Char](is, Some("tif"),36000,6000,2000,2000,None) match {
      case Some(cTile) =>
        cTile.flatten.min shouldBe 0
        cTile.flatten.max shouldBe 13
        cTile.flatten.map(_.toDouble).sum shouldBe 144647.0 +- 0.5
        println("Non Zero Elements:"+cTile.flatten.filter(_>0).length)
      case None =>
        false shouldBe true
    }
  }

  test("Read image as double tiles") {
    import TilingStrategies.Grid._
    import fourquant.io.BufferedImageOps.implicits.directDoubleImageSupport
    val is = ImageTestFunctions.makeVSImg(250, 500, "tif")
    val uniTile = ImageIOOps.readImageAsTiles[Double](is, Some("tif"), 250, 500)
    uniTile.length shouldBe 1
    val uniSum = uniTile.map(_._2.map(i => i.sum).sum).sum

    val imTiles = ImageIOOps.readImageAsTiles[Double](is, Some("tif"), 50, 50)
    imTiles.length shouldBe 50
    val t2 = imTiles.filter(kv => kv._1 ==(50, 50))
    t2.length shouldBe 1
    t2.headOption match {
      case Some(cTile) =>
        //println(cTile._1+" =>\n"+cTile._2.map(_.mkString(",")).mkString("\n"))

        cTile._2(0)(0) shouldBe 255.0 +- 0.5
      case None =>
        false shouldBe true
    }

    val manySum = imTiles.map(_._2.map(i => i.sum).sum).sum

    uniSum shouldBe manySum +- 0.1

  }

  test("Read image as char tiles") {
    import TilingStrategies.Grid._
    import fourquant.io.BufferedImageOps.implicits.charImageSupport
    val is = ImageTestFunctions.makeVSImg(30, 30, "tif")
    val imTiles = ImageIOOps.readImageAsTiles[Char](is, Some("tif"), 10, 10)
    imTiles.length shouldBe 9
    if (verbose) {
      imTiles.foreach(
        cTile => println(cTile._1 + " =>\n" + cTile._2.map(_.map(_.toInt).mkString(",")).
          mkString("\n"))
      )
    }

    val t2 = imTiles.filter(kv => kv._1 ==(20, 20))
    t2.length shouldBe 1
    t2.headOption match {
      case Some(cTile) =>

        cTile._2(0)(0).toInt shouldBe 255
        cTile._2(0)(3).toInt shouldBe 0
      case None =>
        false shouldBe true
    }

  }

  test("Read geo-tiled images") {
    val is = ImageIOOps.createStream(
      new FileInputStream(new File(tiledGeoImage))
    )
    import fourquant.io.BufferedImageOps.implicits.charImageSupport
    ImageIOOps.readTileArray[Char](is, Some("tif"),2000,2000,100,100,None) match {
      case Some(cTile) =>
        cTile.flatten.min shouldBe 0
        cTile.flatten.max shouldBe 13
        cTile.flatten.map(_.toDouble).sum shouldBe 144647.0 +- 0.5
        println("Non Zero Elements:"+cTile.flatten.filter(_>0).length)
      case None =>
        false shouldBe true
    }
  }
  if (heavy) {
    test("Read char tiles from a big image") {
      import TilingStrategies.Grid._
      import fourquant.io.BufferedImageOps.implicits.charImageSupport
      val is = ImageIOOps.createStream(
        new FileInputStream(new File(bigImage))
      )

      val imTiles = ImageIOOps.readImageAsTiles[Char](is, Some("tif"), 5000, 5000)
      imTiles.length shouldBe 64
      imTiles.headOption match {
        case Some(cTile) =>
          println(cTile._1+" => "+cTile._2.flatten.map(_.toDouble).sum)
        case None =>
          false shouldBe true
      }

    }
  }

  test("Test the boundary images") {
    val is = ImageIOOps.createStream(
      new FileInputStream(new File(bigImage))
    )
    val info = ImageIOOps.getImageInfo(is,None)
    val tileStrat = new Simple2DGrid()
    val alltilepositions = tileStrat.createTiles2D(info.width, info.height, 333, 333)
    alltilepositions.length shouldBe 14641
    val subpos = alltilepositions.takeRight(3)

    val tiles = subpos.flatMap(inPos =>
      ImageIOOps.readTile(is, Some("tif"), inPos._1, inPos._2, inPos._3, inPos._4, None))

    println(tiles.mkString("\n"))
    tiles.length shouldBe 3

    all(tiles.map(_.getWidth())) shouldBe 40
    all(tiles.map(_.getHeight())) shouldBe 40

  }

}


object ImageTestFunctions extends Serializable {
  def makeImageData(xdim: Int, ydim: Int, os: OutputStream, format: String): OutputStream = {
    val emptyImage = new BufferedImage(xdim, ydim, BufferedImage.TYPE_BYTE_GRAY)
    val g = emptyImage.getGraphics()
    //g.drawString("Hey!",50,50)
    for (i <- 0 to xdim) g.drawRect(i, i, 1, 1)
    ImageIO.write(emptyImage, format, os)
    os
  }

  def makeImageDataColor(xdim: Int, ydim: Int, os: OutputStream, format: String): OutputStream = {
    val emptyImage = new BufferedImage(xdim, ydim, BufferedImage.TYPE_BYTE_GRAY)
    val g = emptyImage.getGraphics()
    for (i <- 0 to xdim) g.drawRect(i, i, 1, 1)
    g.setColor(java.awt.Color.DARK_GRAY)
    for (i <- 0 to xdim) g.drawRect(i-2, i, 1, 1)
    g.setColor(java.awt.Color.LIGHT_GRAY)
    for (i <- 0 to xdim) g.drawRect(i+2, i, 1, 1)
    ImageIO.write(emptyImage, format, os)
    os
  }

  def makeImageDataRegions(xdim: Int, ydim: Int, os: OutputStream, format: String):
  OutputStream = {
    val emptyImage = new BufferedImage(xdim, ydim, BufferedImage.TYPE_BYTE_GRAY)
    val g = emptyImage.getGraphics()
    val bigBoxSize: Int = 35
    val bigBoxSpace: Int = Math.min(xdim,200)
    for (xoff <- 0 to Math.ceil(xdim * 1.0 / bigBoxSpace).toInt;
         yoff <- 0 to Math.ceil(ydim * 1.0 / bigBoxSpace).toInt;
         xpos = (xoff+0.5) * bigBoxSpace - bigBoxSize/2.0;
         ypos = (yoff+0.5) * bigBoxSpace- bigBoxSize/2.0) {
      g.fillRect(xpos.toInt + 5, ypos.toInt + 5, bigBoxSize - 5 * 2, bigBoxSize - 5 * 2)
      g.setColor(java.awt.Color.DARK_GRAY)
      g.fillRect(xpos.toInt + 10, ypos.toInt + 10, bigBoxSize - 10 * 2, bigBoxSize - 10 * 2)
      g.setColor(java.awt.Color.LIGHT_GRAY)
      g.fillRect(xpos.toInt + 15, ypos.toInt + 15, bigBoxSize - 15 * 2, bigBoxSize - 15 * 2)
    }
    ImageIO.write(emptyImage, format, os)
    os
  }

  def makeImagePathRegions(xdim: Int, ydim: Int, format: String, folder: String) = {
    val outFile = new File(folder+File.separator+"regions_"+xdim+"_"+ydim+"."+format)
    makeImageDataRegions(xdim, ydim, new FileOutputStream(outFile), format)
    println(format+" file written:" + outFile.getAbsolutePath)
    outFile.getAbsolutePath
  }

  def makeImagePath(xdim: Int, ydim: Int, format: String, folder: String) = {
    val outFile = new File(folder+File.separator+"test_"+xdim+"_"+ydim+"."+format)
    makeImageDataColor(xdim, ydim, new FileOutputStream(outFile), format)
    println(format+" file written:" + outFile.getAbsolutePath)
    outFile.getAbsolutePath
  }

  def makeImage(xdim: Int, ydim: Int, format: String): String = {
    val tempFile = File.createTempFile("junk", "." + format)
    makeImageData(xdim, ydim, new FileOutputStream(tempFile), format)
    println(format+" file written:" + tempFile.getAbsolutePath)
    tempFile.getAbsolutePath
  }

  def makeVImg(xdim: Int, ydim: Int, format: String): InputStream = {
    val baos = new ByteArrayOutputStream()
    makeImageData(xdim, ydim, baos, format)
    new ByteArrayInputStream(baos.toByteArray)
  }

  def makeVSImg(xdim: Int, ydim: Int, format: String): ImageInputStream =
    ImageIOOps.createStream(makeVImg(xdim, ydim, format))

}