package fourquant.sql

import fourquant.ImageSparkInstance
import fourquant.io.ImageIOOps._
import fourquant.io.ImageTestFunctions
import fourquant.sql.ImageAnalysisTools.{PosArrayTile, NamedTile}
import fourquant.sql.SQLTestTools.NamedArrayTile
import org.apache.spark.fourquant.{DoubleArrayTile, SQLTypes}
import SQLTypes.{ArrayTile, udf}
import fourquant.tiles.TilingStrategies
import fourquant.utils.SilenceLogs
import org.apache.spark.sql.SQLContext
import org.scalatest.{FunSuite, Matchers}

/**
 * Created by mader on 4/21/15.
 */
class SQLImageAnalysisTests extends FunSuite with Matchers with ImageSparkInstance with SilenceLogs with
Serializable {

  override def useLocal: Boolean = true

  override def bigTests: Boolean = false

  override def useCloud: Boolean = false

  def countPoints(in: Array[Array[Boolean]]): Int = in.flatten.map(if(_) 1 else 0).sum
  test ("Class Test") {
    val simpleArr = Array(Array(true,true,false))
    countPoints(simpleArr) shouldBe 1
    val emptyArr = Array(new Array[Boolean](0))
    countPoints(emptyArr) shouldBe 0
  }

  test("ArrayTile SQL Test") {

    val sList = sc.parallelize(0 to 10).map{
      (i: Int) => NamedArrayTile("PosName:"+i,DoubleArrayTile(1,2,Array(1.0,2.0)))
    }

    val sqlContext = new SQLContext(sc)

    import sqlContext.implicits._
    val df = sList.toDF
    df.registerTempTable("Tiles")

    val sQuery = sqlContext.sql("SELECT * FROM Tiles")
    println(sQuery.collect().mkString("\n"))
    sQuery.count shouldBe 11
    udf.registerAll(sqlContext)
    val mQuery = sqlContext.sql("SELECT name,pixelCount(threshold(dat,0.0)) FROM Tiles")
    println(mQuery.collect().mkString("\n"))
    mQuery.first.getInt(1) shouldBe 2

  }

  test("SparseThresh SQL Test") {

    val sList = sc.parallelize(0 to 10).map{
      (i: Int) => PosArrayTile(i,-i,DoubleArrayTile(1,2,Array(1.0,2.0)))
    }

    val sqlContext = new SQLContext(sc)

    import sqlContext.implicits._
    val df = sList.toDF
    df.registerTempTable("Tiles")

    val sQuery = sqlContext.sql("SELECT * FROM Tiles")
    println(sQuery.collect().mkString("\n"))
    sQuery.count shouldBe 11
    udf.registerAll(sqlContext)
    val mQuery = sqlContext.sql("SELECT x,y,sparseThreshold(x,y,tile,0.0) FROM Tiles")
    println(mQuery.collect().mkString("\n"))
    mQuery.first.getInt(0) shouldBe 0

  }


  if (useLocal) {
    test("Tiny Image Tiling and Thresholding Test") {
      import TilingStrategies.Grid._

      val imgPath = ImageTestFunctions.makeImage(100,100,"tif")

      val tiledImage = sc.readTiledDoubleImage(imgPath,
        10, 20, 80).map{
        case (pos,inArr) =>
          NamedTile(pos._1,pos._2,pos._3,DoubleArrayTile(inArr))
      }.cache

      val sq = new SQLContext(sc)
      import sq.implicits._
      tiledImage.toDF.registerTempTable("Tiles")

      udf.registerAll(sq)

      val fTable = sq.sql("SELECT * FROM Tiles")

      val fTile = fTable.first

      fTile.getAs[ArrayTile[Double]](3).getRows shouldBe 20
      fTile.getAs[ArrayTile[Double]](3).getCols shouldBe 10

      val tileCount = fTable.count()



      val nonZeroEntries = sq.sql("""
        SELECT x,y,pixelCount(threshold(tile,0.0)) FROM Tiles
        WHERE pixelCount(threshold(tile,0.0))>0
                                  """)
      val nzTiles = nonZeroEntries.count
      println(nonZeroEntries.head(5).mkString("\n"))

      val nzCount = sq.sql("SELECT SUM(pixelCount(threshold(tile,0.0))) FROM Tiles").head.getLong(0)

      println(("Tile Count",tileCount,
        "Non Zero Tiles",nzTiles,
        "Non-Zero Count",nzCount
        ))

      tileCount shouldBe 50
      nzTiles shouldBe 18
      nzCount shouldBe 298
    }


  }


}

object ImageAnalysisTools {
  case class NamedTile(name: String, x: Int, y: Int, tile: DoubleArrayTile)
  case class PosArrayTile(x: Int, y: Int, tile: DoubleArrayTile)
}
