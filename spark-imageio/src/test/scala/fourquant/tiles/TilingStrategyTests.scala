package fourquant.tiles

import fourquant.tiles.TilingStrategies.{SimpleGridStrategy, Simple2DGrid}
import org.scalatest.{Matchers, FunSuite}

/**
 * Created by mader on 4/11/15.
 */
class TilingStrategyTests extends FunSuite with Matchers {
  for ((tileName, tileStrat) <- Array[(String,TilingStrategy2D)](
    ("2D Grid", new Simple2DGrid()),
    ("ND Grid", new SimpleGridStrategy())
  ) ) {
    test(tileName + " Tile ROI Generation") {

      val simpletest = tileStrat.createTiles2D(10, 10, 10, 10)
      simpletest.length shouldBe 1

      val ttest = tileStrat.createTiles2D(35, 35, 10, 10)
      println(ttest.mkString("\n"))
      ttest.length shouldBe 16

      val astest = tileStrat.createTiles2D(35, 35, 10, 5)
      astest.length shouldBe 28
      println(astest.mkString("\n"))
    }
  }

  test("3D Tiling (more than that we shouldnt need") {
    val ts = new SimpleGridStrategy()
    val simple = ts.createTiles(Array(1000L,1000L,1000L),Array(250L,250L,250L))
    simple.length shouldBe 64
    println(simple.map(av => (av._1.mkString(","),av._2.mkString(","))).mkString("\n"))
  }

  test("2D slices over 3D Tiling") {
    val ts = new SimpleGridStrategy()
    val simple = ts.createTiles(Array(100L,100L,100L),Array(50L,50L))
    simple.length shouldBe 400
    println(simple.map(av => (av._1.mkString(","),av._2.mkString(","))).mkString("\n"))
  }
}
