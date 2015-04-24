package tests


import fourquant.utils.SilenceLogs
import org.apache.spark.sql.SQLContext
import org.scalatest.{FunSuite, Matchers}
import spark.sql.{SQLD3int, SQLD3I, CoreOps, SQLTImgRO}
import tests.SQLTools.{NamedPosition, NamedTImgRO}
import tipl.spark.SparkGlobal
import tipl.tests.TestPosFunctions
import tipl.util.TImgTools

/**
 * Created by mader on 4/23/15.
 */
class SQLTests extends FunSuite with Matchers with SilenceLogs with Serializable {
    lazy val sc = SparkGlobal.getContext("SQLTests").sc
  test("Binary Type") {

  }
  test("Basic SQL Position Test") {
    val ntir = sc.parallelize(1 to 10).map{
      k => NamedPosition("k"+k,SQLD3I(k,k+1,k+2))
    }
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    ntir.toDF.registerTempTable("Positions")
    val t = sqlContext.sql("SELECT name,pos from Positions")
    t.count shouldBe 10
    println(t.collect.mkString("\n"))
    t.first.getString(0) shouldBe "k1"
    val t2 = sqlContext.sql("SELECT name,pos.x,pos.y from Positions")
    println(t2.collect.mkString("\n"))
  }
  test("Basic SQL Image Test") {
    val ntir = sc.parallelize(1 to 10).map{
      k => NamedTImgRO("k"+k,SQLTools.makeTestImage(k))
    }
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    ntir.toDF.registerTempTable("Images")
    val t = sqlContext.sql("SELECT name,img from Images")
    t.count shouldBe 10
    println(t.collect.mkString("\n"))
    t.first.getString(0) shouldBe "k1"
  }

  test("Field SQL Tests") {
    val ntir = sc.parallelize(1 to 10).map{
      k => NamedTImgRO("k"+k,SQLTools.makeTestImage(k))
    }
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    ntir.toDF.registerTempTable("Images")
    val t = sqlContext.sql("SELECT name,img.pos,img.dim,img from Images")
    t.count shouldBe 10
    println(t.collect.mkString("\n"))
    t.first.getString(0) shouldBe "k1"
  }


}

object SQLTools extends Serializable {
  case class NamedTImgRO(name: String, img: SQLTImgRO)
  case class NamedPosition(name: String, pos: SQLD3int)
  def makeTestImage(size: Int) = CoreOps.makeImg(
    TestPosFunctions.wrapItAs(size, new TestPosFunctions.LinesFunction, TImgTools.IMAGETYPE_FLOAT)
  )
}
