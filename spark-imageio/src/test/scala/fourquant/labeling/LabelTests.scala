package fourquant.labeling

import fourquant.ImageSparkInstance
import fourquant.arrays.Positions
import fourquant.labeling.ConnectedComponents.LabelCriteria
import fourquant.utils.SilenceLogs
import org.apache.spark.rdd.RDD
import org.scalatest.{FunSuite, Matchers}

/**
 * Created by mader on 4/14/15.
 */
class LabelTests extends FunSuite with Matchers with SilenceLogs with ImageSparkInstance {
  def useLocal = true
  def bigTests = false
  def useCloud: Boolean = false


  test("Spread Point Test") {
    import Positions._
    val (ptpos,ptval) = (("hello",5,10),"myname")

    val onespread = ConnectedComponents.spreadPoints(ptpos,ptval,(1,1),true)
    onespread.length shouldBe 9

    val (orig,other) = onespread.partition(_._3)
    orig.length shouldBe 1
    other.length shouldBe 8

    onespread.map(_._1.getX).min shouldBe 4
    onespread.map(_._1.getX).max shouldBe 6
    onespread.map(_._1.getY).min shouldBe 9
    onespread.map(_._1.getY).max shouldBe 11


    val twothreespread = ConnectedComponents.spreadPoints(ptpos,ptval,(2,3),false)
    twothreespread.length shouldBe (3*4)
    twothreespread.map(_._1.getX).min shouldBe 5
    twothreespread.map(_._1.getX).max shouldBe 7
    twothreespread.map(_._1.getY).min shouldBe 10
    twothreespread.map(_._1.getY).max shouldBe 13
  }

  test("Collapse Point Test") {
    val pts = Seq(
      ("0",(2L,"val0"),false),
      ("1",(3L,"val1"),false),
      ("2",(4L,"val2"),true)
    )
    implicit val strComp = new LabelCriteria[String] {
      override def matches(a: String, b: String): Boolean = true // always match
    }
    ConnectedComponents.collapsePoint[String,String](pts) match {
      case Some(((position,(label,pointValue)),swaps)) =>
        swaps shouldBe 1
        position shouldBe "2"
        label shouldBe 2L
        pointValue shouldBe "val2"
      case None => throw new RuntimeException("Failed!")
    }

    val samePts = Seq(
      ("0",(2L,"val0"),true),
      ("1",(3L,"val1"),false),
      ("2",(4L,"val2"),false)
    )

    ConnectedComponents.collapsePoint[String,String](samePts) match {
      case Some(((position,(label,pointValue)),swaps)) =>
        swaps shouldBe 0
        position shouldBe "0"
        label shouldBe 2L
        pointValue shouldBe "val0"
      case None => throw new RuntimeException("Failed!")
    }

    val differentPhases = Seq(
      ("0",(2L,"val0"),false),
      ("1",(3L,"val2"),false),
      ("2",(4L,"val2"),true)
    )
    val differentCriteria = new LabelCriteria[String] {
      override def matches(a: String, b: String): Boolean = a.contentEquals(b) // only if the
      // strings  match
    }

    ConnectedComponents.collapsePoint[String,String](differentPhases)(differentCriteria) match {
      case Some(((position,(label,pointValue)),swaps)) =>
        swaps shouldBe 1
        position shouldBe "2"
        label shouldBe 3L
        pointValue shouldBe "val2"
      case None => throw new RuntimeException("Failed!")
    }


    val emptyPts = Seq(
      ("0",(2L,"val0"),false),
      ("1",(3L,"val1"),false),
      ("2",(4L,"val2"),false)
    )
    ConnectedComponents.collapsePoint[String,String](emptyPts).isEmpty shouldBe true

  }

  import Positions._
  {
    implicit val dlc = new LabelCriteria[Double] {
      override def matches(a: Double, b: Double): Boolean = true //Math.abs(a-b)<0.75
    }
    val lcritName = "Match-All"
    for((cclName,cclFunc) <- Seq(
      ("Point-based",
        (x: RDD[((String,Int,Int),Double)], wind: (Int,Int)) =>
          ConnectedComponents.Labeling2D(x,wind)),
      ("Chunk-based",
        (x: RDD[((String,Int,Int),Double)], wind: (Int,Int)) =>
          ConnectedComponents.Labeling2DChunk(x,wind))
    )) {
      test("TCL Test:" + cclName+"-"+lcritName) {

        val ptList = Seq(
          (("hello", 5, 10), 0.0),
          (("hello", 5, 11), 1.0),
          (("hello", 5, 12), 2.0)
        )
        val ptRdd = sc.parallelize(ptList)

        val compImg = cclFunc(ptRdd, (1, 1))
        val comps = compImg.map(_._2._1).countByValue()

        println(compImg.collect.mkString("\n"))
        comps.size shouldBe 1
      }
    }
  }
  {
    implicit val dlc = new LabelCriteria[Double] {
      override def matches(a: Double, b: Double): Boolean = Math.abs(a-b)<0.75
    }
    val lcritName = "Match-OnePt"
    for((cclName,cclFunc) <- Seq(
      ("Point-based",
        (x: RDD[((String,Int,Int),Double)], wind: (Int,Int)) =>
          ConnectedComponents.Labeling2D(x,wind)),
      ("Chunk-based",
        (x: RDD[((String,Int,Int),Double)], wind: (Int,Int)) =>
          ConnectedComponents.Labeling2DChunk(x,wind))
    )) {
      test("TCL Test:" + cclName+"-"+lcritName) {

        val ptList = Seq(
          (("hello", 5, 10), 0.0),
          (("hello", 5, 11), 1.0),
          (("hello", 5, 12), 2.0)
        )
        val ptRdd = sc.parallelize(ptList)

        val compImg = cclFunc(ptRdd, (1, 1))
        val comps = compImg.map(_._2._1).countByValue()

        println(compImg.collect.mkString("\n"))
        comps.size shouldBe 3
      }
    }
  }

  test("Quick Component Labeling") {
    import fourquant.io.BufferedImageOps.implicits.directDoubleImageSupport
    val nonZeroEntries = makeSimpleTiledImage()._2
    val nzCount = nonZeroEntries.count()

    { // unique namespace for implicit variables
      implicit val doubleLabelCrit = new LabelCriteria[Double] {
        override def matches(a: Double, b: Double): Boolean = true //match everything
      }
      val compLabel = ConnectedComponents.Labeling2D(nonZeroEntries, (1, 1))
      val components = compLabel.map(_._2._1).countByValue()

      println(("Non-Zero Count", nzCount, "Components", components.size))
      print("Components:" + components.mkString(", "))

      components.size shouldBe 1
      nzCount shouldBe 338
    }

    {
      implicit val doubleLabelCrit = new LabelCriteria[Double] {
        override def matches(a: Double, b: Double): Boolean = Math.abs(a-b)<0.75 // match only
        // points with the same value
      }
      val compLabel = ConnectedComponents.Labeling2D(nonZeroEntries, (1, 1))
      val components = compLabel.map(_._2._1).countByValue()

      println(("Non-Zero Count", nzCount, "Components", components.size))
      print("Components:" + components.mkString(", "))

      components.size shouldBe 3
      nzCount shouldBe 338
    }
  }
  for(opt<-Seq(true,false);fullRange<-Seq(true,false)) {
    test("Chunk CL" + (if(opt) " Optimized" else "")+ (if(fullRange) "-Full" else "-Half")) {
      import fourquant.io.BufferedImageOps.implicits.directDoubleImageSupport
      val nonZeroEntries = makeSimpleTiledImage()._2
      val nzCount = nonZeroEntries.count()

      { // unique namespace for implicit variables
      implicit val doubleLabelCrit = new LabelCriteria[Double] {
          override def matches(a: Double, b: Double): Boolean = true //match everything
        }
        val compLabel = ConnectedComponents.Labeling2DChunk(nonZeroEntries, (1, 1),
          fullRange=fullRange,optimizeList=true)
        val components = compLabel.map(_._2._1).countByValue()

        println(("Non-Zero Count", nzCount, "Components", components.size))
        print("Components:" + components.mkString(", "))

        components.size shouldBe 1
        nzCount shouldBe 338
      }

      {
        implicit val doubleLabelCrit = new LabelCriteria[Double] {
          override def matches(a: Double, b: Double): Boolean = Math.abs(a-b)<0.75 // match only
          // points with the same value
        }
        val compLabel = ConnectedComponents.Labeling2DChunk(nonZeroEntries, (1, 1))
        val components = compLabel.map(_._2._1).countByValue()

        println(("Non-Zero Count", nzCount, "Components", components.size))
        println("Components:" + components.mkString(", "))

        components.size shouldBe 3
        nzCount shouldBe 338
      }
    }

  }


}
