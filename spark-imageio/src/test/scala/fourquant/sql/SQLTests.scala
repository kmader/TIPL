package fourquant.sql

import fourquant.ImageSparkInstance
import org.apache.spark.fourquant.{PosDataUDT, ByteArrayTile, DoubleArrayTile, SQLTypes}
import SQLTypes.ArrayTile
import fourquant.utils.SilenceLogs
import org.apache.spark.mllib.linalg
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.GenericMutableRow
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Row, SQLContext, SaveMode}
import org.scalatest.{FunSuite, Matchers}

/**
 * A series of tests to make sure
 * 1) I am using SparkSQL correctly
 * 2) If the API changes, it is immediately visible, particularly with support for UDT and UDFs
 * Created by mader on 4/21/15.
 */
class SQLTests extends FunSuite with Matchers with ImageSparkInstance with SilenceLogs with
Serializable {
  import fourquant.sql.SQLTestTools._
  override def useLocal: Boolean = true

  override def bigTests: Boolean = false

  override def useCloud: Boolean = false

  test("Vector type test") { // ensure everything works on simple vectors first
    val sList = sc.parallelize(0 to 10).map(i=>VectorWrapper("hai:"+i,Vectors.dense(i)))
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    val df = sList.toDF
    df.registerTempTable("Positions")
    val sQuery = sqlContext.sql("SELECT * FROM Positions")
    println(sQuery.collect().mkString("\n"))
    sQuery.count shouldBe 11
    sQuery.first.getString(0) shouldBe "hai:0"
  }



  test("String UDF") {
    val sList = sc.parallelize(0 to 10).map(i=>VectorWrapper("hai:"+i,Vectors.dense(i)))
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    val df = sList.toDF

    df.registerTempTable("Positions")

    sqlContext.udf.register("LenOfString",(s: String) => s.length)
    val sQuery = sqlContext.sql("SELECT LenOfString(name) FROM Positions")
    sQuery.count shouldBe 11
    sQuery.first.getInt(0) shouldBe 5
    sQuery.collect.reverse.head.getInt(0) shouldBe 6

  }
  test("Vector UDF") {
    val sList = sc.parallelize(0 to 10).map(i=>VectorWrapper("hai:"+i,Vectors.dense(i)))
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    val df = sList.toDF
    df.registerTempTable("Positions")

    sqlContext.udf.register("VectorSum",(s: linalg.Vector) => s.toArray.sum+1)
    val sQuery = sqlContext.sql("SELECT VectorSum(vec) FROM Positions")
    sQuery.count shouldBe 11
    sQuery.first.getDouble(0) shouldBe 1.0+-1e-9
    sQuery.collect.reverse.head.getDouble(0) shouldBe 11.0+-1e-9


  }

  test("UDT PosData dataframe test") {
    val sList = sc.parallelize(0 to 10).map{
      (i: Int) => NamedPosition("PosName:"+i,PosData(i,i+1,i+2))
    }

    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    sList.toDF

  }

  test("UDT PosData SQL Test") {
    val sList = sc.parallelize(0 to 10).map{
      (i: Int) => NamedPosition("PosName:"+i,PosData(i,i+1,i+2))
    }
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    val df = sList.toDF
    df.registerTempTable("Positions")
    val sQuery = sqlContext.sql("SELECT * FROM Positions")
    println(sQuery.collect().mkString("\n"))
  }

  test("UDT PosData UDF Test") {
    val sList = sc.parallelize(0 to 10).map{
      (i: Int) => NamedPosition("PosName:"+i,PosData(i,i+1,i+2))
    }
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    val df = sList.toDF
    df.registerTempTable("Positions")

    sqlContext.udf.register("GetX",(s: PosData) => s.getX)
    sqlContext.udf.register("GetY",(s: PosData) => s.getY)

    val sQuery = sqlContext.sql("SELECT GetX(position),GetY(position) FROM Positions")
    println(sQuery.collect().mkString("\n"))
    sQuery.count shouldBe 11
    sQuery.first.getInt(0) shouldBe 0
    sQuery.first.getInt(1) shouldBe 1
    sQuery.collect.reverse.head.getInt(0) shouldBe 10

  }
  // IO Tests
  for(format<-Seq("parquet","json")) {
    test("Vector IO Tests: "+format) {
      val sList = sc.parallelize(0 to 10).map(i=>VectorWrapper("hai:"+i,Vectors.dense(i)))
      val sqlContext = new SQLContext(sc)
      import sqlContext.implicits._
      val df = sList.toDF
      val frRead = df.collect()
      df.write.format(format).mode(SaveMode.Overwrite).save(testDataDir+"/vectorTable."+format)
      val reRead = sqlContext.load(testDataDir+"/vectorTable."+format,format)
      println(frRead.zip(reRead.collect()).map(a => a._1 +"\t"+a._2).mkString("\n"))
      reRead.count shouldBe 11
      reRead.first.getString(0) shouldBe "hai:0"

    }
    test("ArrayTile IO Tests: "+format) {
      val sList = sc.parallelize(0 to 10).map{
        (i: Int) => NamedArrayTile("PosName:"+i,DoubleArrayTile(1,2,Array(1.0,2.0)))
      }
      val sqlContext = new SQLContext(sc)
      import sqlContext.implicits._
      val df = sList.toDF
      val frRead = df.collect()
      df.write.format(format).mode(SaveMode.Overwrite).save(testDataDir+"/arrayTable."+format)
      val reRead = sqlContext.load(testDataDir+"/arrayTable."+format,format)
      println(frRead.zip(reRead.collect()).map(a => a._1 +"\t"+a._2).mkString("\n"))
      reRead.count shouldBe 11
      reRead.first.getString(0) shouldBe "PosName:0"
      reRead.first.getAs[ArrayTile[Double]](1).getRows shouldBe 1
      reRead.first.getAs[ArrayTile[Double]](1).getCols shouldBe 2
    }
  }


}


@SQLUserDefinedType(udt = classOf[PosDataUDT])
trait PosData extends Serializable {
  def getX: Int
  def getY: Int
  def getZ: Int
}



//README needs a different name otherwise the ._ import screws everything up
object SQLTestTools extends Serializable {

  case class NamedArrayTile(name: String, dat: DoubleArrayTile)

  case class NamedBArrayTile(name: String, bt: ByteArrayTile)
  case class VectorWrapper(name: String, vec: linalg.Vector)

  case class NamedPosition(name: String, position: PosData)

  class ImageType extends DataType {
    override def defaultSize: Int = ???
    override def asNullable = this
  }
  object PosData extends Serializable {
    def apply(x: Int, y: Int, z: Int) = new PosData {
      override def getX: Int = x
      override def getY: Int = y
      override def getZ: Int = z
    }
  }


}
