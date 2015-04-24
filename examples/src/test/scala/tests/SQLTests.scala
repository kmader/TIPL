package tests


import fourquant.utils.SilenceLogs
import org.apache.spark.sql.SQLContext
import org.scalatest.{FunSuite, Matchers}
import tests.SQLTools.NamedTImgRO
import tipl.formats.TImgRO
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
  test("Basic SQL Test") {
    val ntir = sc.parallelize(1 to 10).map{
      k => NamedTImgRO("k"+k,SQLTools.makeTestImage(k))
    }
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    ntir.toDF.registerTempTable("Images")
    val t = sqlContext.sql("SELECT name,img from Images")
    t.count shouldBe 10
    t.first.getString(0) shouldBe "1"
  }
}

object SQLTools extends Serializable {
  case class NamedTImgRO(name: String, img: TImgRO)


  def makeTestImage(size: Int): TImgRO = TestPosFunctions.wrapItAs(size, new TestPosFunctions
  .LinesFunction, TImgTools.IMAGETYPE_FLOAT)
}
