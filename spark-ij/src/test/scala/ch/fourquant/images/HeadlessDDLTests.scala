package ch.fourquant.images

import fourquant.imagej.{scOps, TestSupportFcns}
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkContext, LocalSparkContext}
import org.scalatest.{Matchers, FunSuite}

/**
  * Created by mader on 1/28/16.
  */
class HeadlessDDLTests extends FunSuite with Matchers with LocalSparkContext {
  val sc = getNewSpark("local[4]", "DDLTests")
  //implicit val ijs = TestSupportFcns.ijs
  val contextList = Array(
    ("SparkSQL Headless", (iv: SparkContext) => new SQLContext(iv), TestSupportFcns.headlessIJ), // test headless
    ("HiveQL Headless", (iv: SparkContext) => new HiveContext(sc), TestSupportFcns.headlessIJ) // text hiveql
  )
  for ((cmName, contextMapping, curIjs) <- contextList) {
    val sq = contextMapping(sc)

    test(s"$cmName : Listing all plugins") {
      import scOps._

      sq.registerImageJ(curIjs)

      val plugList = sq.sql("SELECT listplugins()")
      plugList.count shouldBe 1

      val outList = plugList.first().getList[String](0)
      outList.toArray.foreach(println(_))
      outList.size() should be > 100
    }


    test(cmName + ": Create Database Test") {
      sq.sql(
        s"""
           |CREATE TEMPORARY TABLE DebugImages
           |USING ch.fourquant.images.debug
           |OPTIONS (path "SimpleNeedednt Be Path-like", count "7", table "simple")
                        """.stripMargin.replaceAll("\n", " "))

      sq.sql("SHOW TABLES").collect().foreach(println(_))
      sq.tableNames().length shouldBe 1
      val tfi = sq.table("DebugImages")
      tfi.printSchema()

      tfi.schema(0).name shouldBe "sample"

      tfi.schema(1).name shouldBe "image"
      tfi.schema(1).dataType.typeName shouldBe "PortableImagePlusSQL"

      val oTab = sq.sql("SELECT sample,image FROM DebugImages")

      oTab.foreach(println(_))
      oTab.
        count shouldBe 7



    }
  }
}
