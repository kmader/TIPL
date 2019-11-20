package fourquant.shape

import fourquant.ImageSparkInstance
import fourquant.arrays.Positions
import fourquant.io.ImageTestFunctions
import fourquant.labeling.ConnectedComponents
import fourquant.labeling.ConnectedComponents.LabelCriteria
import fourquant.shape.EllipsoidAnalysis.{Indexable, ShapeInformation2D}
import fourquant.utils.SilenceLogs
import org.scalatest.{FunSuite, Matchers}

/**
 * Created by mader on 4/14/15.
 */
class ShapeTests extends FunSuite with Matchers with SilenceLogs with ImageSparkInstance {
  override def useLocal: Boolean = true

  override def bigTests: Boolean = false

  override def useCloud: Boolean = true

  test("Image Shape Analysis") {
    import fourquant.io.BufferedImageOps.implicits.directDoubleImageSupport
    val nonZeroEntries = makeSimpleTiledImage()._2
    val nzCount = nonZeroEntries.count()

    implicit val doubleLabelCrit = new LabelCriteria[Double] {
      override def matches(a: Double, b: Double): Boolean = Math.abs(a-b)<0.75 // match only
      // points with the same value
    }
    import Positions._
    val compLabel = ConnectedComponents.Labeling2DChunk(nonZeroEntries, (1, 1))
    val components = compLabel.map(_._2._1).countByValue()

    println(("Non-Zero Count", nzCount, "Components", components.size))
    print("Components:" + components.mkString(", "))

    components.size shouldBe 3

    implicit val obvInd = new Indexable[(Long,Double)] {
      override def toIndex(a: (Long,Double)): Long = a._1
    }
    val shapeAnalysis = EllipsoidAnalysis.runShapeAnalysis(compLabel)
    shapeAnalysis.count shouldBe 3

    val shapeInformation = shapeAnalysis.mapValues(
      iv => ShapeInformation2D(iv)
    ).sortByKey(true).collect

    println(shapeInformation.mkString("\n"))

    shapeInformation(0)._2.area shouldBe 51
    shapeInformation(1)._2.area shouldBe 143
    shapeInformation(2)._2.area shouldBe 144

  }

  test("Region Shape Analysis") {
    import fourquant.io.BufferedImageOps.implicits.directDoubleImageSupport
    val nonZeroEntries = makeRegionTiledImage()._2
    val nzCount = nonZeroEntries.count()

    implicit val doubleLabelCrit = new LabelCriteria[Double] {
      override def matches(a: Double, b: Double): Boolean = Math.abs(a-b)<0.75 // match only
      // points with the same value
    }
    import Positions._
    val compLabel = ConnectedComponents.Labeling2DChunk(nonZeroEntries, (1, 1))
    val components = compLabel.map(_._2._1).countByValue()

    println(("Non-Zero Count", nzCount, "Components", components.size))
    print("Components:" + components.mkString(", "))

    components.size shouldBe 3

    implicit val obvInd = new Indexable[(Long,Double)] {
      override def toIndex(a: (Long,Double)): Long = a._1
    }
    val shapeAnalysis = EllipsoidAnalysis.runShapeAnalysis(compLabel)
    shapeAnalysis.count shouldBe 3

    val shapeInformation = shapeAnalysis.mapValues(
      iv => ShapeInformation2D(iv)
    ).sortBy(_._2.area).collect

    println(shapeInformation.mkString("\n"))

    shapeInformation(0)._2.area shouldBe 25
    shapeInformation(1)._2.area shouldBe 200
    shapeInformation(2)._2.area shouldBe 400

  }

  if (bigTests) {
    val imgFactor = 40
    val imgSize = 200*imgFactor
    val objCount = imgFactor*imgFactor
    val layerCount = objCount * 3
    test("Big Region Shape Analysis") {
      import fourquant.io.BufferedImageOps.implicits.charImageSupport
      val imgPath = ImageTestFunctions.makeImagePathRegions(imgSize,imgSize,"tif",
        testDataDir)
      val (tileImage,nonZeroEntries) =
        makeSimpleTiledImageHelperGeneric[Char](imgPath,tileWidth=imgSize/10,
          tileHeight=imgSize/10)

      val nzCount = nonZeroEntries.count()

      implicit val doubleLabelCrit = new LabelCriteria[Double] {
        override def matches(a: Double, b: Double): Boolean = Math.abs(a-b)<0.75 // match only
        // points with the same value
      }
      import Positions._
      val compLabel = ConnectedComponents.Labeling2DChunk(nonZeroEntries, (1, 1))
      val components = compLabel.map(_._2._1).countByValue()

      println(("Non-Zero Count", nzCount, "Components", components.size))
      print("Components:" + components.mkString(", "))

      components.size shouldBe layerCount

      implicit val obvInd = new Indexable[(Long,Double)] {
        override def toIndex(a: (Long,Double)): Long = a._1
      }
      val shapeAnalysis = EllipsoidAnalysis.runShapeAnalysis(compLabel)
      shapeAnalysis.count shouldBe layerCount

      val shapeInformation = shapeAnalysis.mapValues(
        iv => ShapeInformation2D(iv)
      ).sortBy(_._2.area,false).collect

      println(shapeInformation.mkString("\n"))

      shapeInformation(0)._2.area shouldBe 400
      shapeInformation(1)._2.area shouldBe 400
      val areaInfo = shapeInformation.map(_._2.area)
      // only 3 different values
      areaInfo.distinct.length shouldBe 3
      areaInfo.distinct(0) shouldBe 400
      areaInfo.distinct(1) shouldBe 200
      areaInfo.distinct(2) shouldBe 25

    }
  }
  if (bigTests) {
    test("On the real image data") {
      import fourquant.io.BufferedImageOps.implicits.charImageSupport
      val (tileImage, nonZeroEntries) = makeSimpleTiledImageHelperGeneric[Char](esriImage, 500, 500, 120)

      val nzCount = nonZeroEntries.count
      implicit val doubleLabelCrit = new LabelCriteria[Double] {
        override def matches(a: Double, b: Double): Boolean = Math.abs(a - b) < 0.75 // match only
        // points with the same value
      }
      import Positions._
      val compLabel = ConnectedComponents.Labeling2DChunk(nonZeroEntries, (1, 1))
      val components = compLabel.map(_._2._1).countByValue()

      println(("Non-Zero Count", nzCount, "Components", components.size))
      print("Components:" + components.mkString(", "))


      implicit val obvInd = new Indexable[(Long, Double)] {
        override def toIndex(a: (Long, Double)): Long = a._1
      }
      val shapeAnalysis = EllipsoidAnalysis.runShapeAnalysis(compLabel)


      val shapeInformation = shapeAnalysis.mapValues(
        iv => ShapeInformation2D(iv)
      ).sortBy(_._2.area, false).collect

      println(shapeInformation.mkString("\n"))


      val areaInfo = shapeInformation.map(_._2.area)

      shapeInformation(0)._2.area shouldBe 400
      shapeInformation(1)._2.area shouldBe 400

      // only 3 different values
      areaInfo.distinct.length shouldBe 3
      areaInfo.distinct(0) shouldBe 400
      areaInfo.distinct(1) shouldBe 200
      areaInfo.distinct(2) shouldBe 25
    }
  }


}
