package fourquant.imagej

import ch.fourquant.images.types.NamedSQLImage
import fourquant.imagej.IJSqlTest.{NamedArray}
import org.apache.spark.LocalSparkContext
import org.apache.spark.LocalSparkContext.SilenceLogs
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.hive.HiveContext

import org.scalatest.{FunSuite, Matchers}

import scala.collection.mutable.ArrayBuffer

/**
 * Ensure the SparkSQL functionality works with simple images
 * Created by mader on 5/5/15.
 */
class IJSqlTest extends FunSuite with LocalSparkContext with Matchers with SilenceLogs {

  val conList = ArrayBuffer(
    ("LocalSpark", (a: String) => getNewSpark("local[8]", a))
  )
  if (!SpijiTests.runLocal) conList.remove(0)

  if (SpijiTests.runLocalCluster) conList +=
    (
      "LocalCluster",
      (a: String) => getNewSpark("local-cluster[2,2,512]", a)
      )
  if (SpijiTests.localMasterWorkers) conList +=("DistributedSpark", (a: String) =>
    getNewSpark("spark://MacBook-Air.local:7077", a))

  for ((conName, curCon) <- conList) {
    implicit val ijs = SpijiTests.ijs
    import SpijiTests.makeTestImages
    import fourquant.imagej.scOps._

    val sc = curCon("Spiji")

    test(conName + ": Dataframe Explode Test") {
      val test = sc.parallelize(0 to 10).map(i => NamedArray("SQ:"+i,(0 to i).toArray))
      val sq = new SQLContext(sc)
      import sq.implicits._
      test.toDF().registerTempTable("TestTable")

      val cc = sq.sql(
        """
          SELECT sample, intArray as nums FROM TestTable
        """.stripMargin
      ).explode("nums","singlenum") { num: TraversableOnce[Int] => num }.select('sample,'singlenum)

      cc.select('sample).distinct.count shouldBe 11
      cc.select('singlenum).distinct.count shouldBe 11
      //println(cc.mkString("\n"))

      cc.count shouldBe (for(i <- 0 to 10; j<-0 to i) yield 1).length
    }

    test(conName + ": Hive Lateral View Explode Test") {
      val test = sc.parallelize(0 to 10).map(i => NamedArray("SQ:"+i,(0 to i).toArray))
      val sq = new HiveContext(sc)
      import sq.implicits._
      test.toDF().registerTempTable("TestTable")

      val cc = sq.sql(
        """
          |SELECT sample, nums FROM TestTable LATERAL VIEW explode(intArray) splod AS nums
        """.stripMargin
      ).collect

      //SELECT id, part.lock, part.key FROM mytable EXTERNAL VIEW explode(parts) parttable AS part;

      cc.length shouldBe (for(i <- 0 to 10; j<-0 to i) yield 1).length
    }

    test(conName + ": Basic Stats Operations") {
      implicit val ijs = SpijiTests.ijs
      val sq = new SQLContext(sc)
      import sq.implicits._
      val imgList = makeTestImages(sc, 1, SpijiTests.imgs, SpijiTests.width, SpijiTests.height).
        map(kv => NamedSQLImage(kv._1,kv._2)).toDF

      imgList.count() shouldBe SpijiTests.imgs

      imgList.registerTempTable("Images")

      sq.registerImageJ(ijs)

      val istats = sq.sql("SELECT sample,stats(image) FROM Images").collect()
      istats.length shouldBe SpijiTests.imgs
      println(istats.mkString("\n\t"))

    }

    test(conName + ": Hive Stats Operations") {
      implicit val ijs = SpijiTests.ijs
      val sq = new HiveContext(sc)
      import sq.implicits._
      val imgList = makeTestImages(sc, 1, SpijiTests.imgs, SpijiTests.width, SpijiTests.height).
        map(kv => NamedSQLImage(kv._1,kv._2)).toDF

      imgList.count() shouldBe SpijiTests.imgs

      imgList.registerTempTable("Images")

      sq.registerImageJ(ijs)

      val istats = sq.sql("SELECT sample,stats(image) FROM Images").collect()
      istats.length shouldBe SpijiTests.imgs
      println(istats.mkString("\n\t"))

    }



    test(conName + ": Noise Operations") {
      implicit val ijs = SpijiTests.ijs
      val sq = new SQLContext(sc)
      import sq.implicits._
      val imgList = makeTestImages(sc, 1, SpijiTests.imgs, SpijiTests.width, SpijiTests.height).
        map(kv => NamedSQLImage(kv._1,kv._2)).toDF

      imgList.count() shouldBe SpijiTests.imgs

      imgList.registerTempTable("Images")

      sq.registerImageJ(ijs)

      val medstats = sq.sql("SELECT sample,stats(image)," +
        "stats(run(image,\"Add Noise\")) " +
        "FROM Images").collect()
      medstats.length shouldBe SpijiTests.imgs
      println(medstats.mkString("\n\t"))

    }

    test(conName + ": Multiple Operations") {
      implicit val ijs = SpijiTests.ijs
      val sq = new SQLContext(sc)
      import sq.implicits._
      val imgList = makeTestImages(sc, 1, SpijiTests.imgs, SpijiTests.width, SpijiTests.height).
        map(kv => NamedSQLImage(kv._1,kv._2)).toDF

      imgList.count() shouldBe SpijiTests.imgs

      imgList.registerTempTable("Images")

      sq.registerImageJ(ijs)

      sq.sql(
        """
          SELECT sample,run(image,"Add Noise") AS nsImg FROM Images
        """.stripMargin
      ).registerTempTable("NoisyImages")

      sq.sql("""
               |SELECT sample,run2(nsImg,"Median...","radius=3") AS fImg
               |FROM NoisyImages
             """.stripMargin
      ).registerTempTable("FilteredImages")

      val medstats = sq.sql(
        """
          SELECT sample,stats(fImg) FROM FilteredImages
        """.stripMargin
      ).collect

      medstats.length shouldBe SpijiTests.imgs
      println(medstats.mkString("\n\t"))

    }

    test(conName + ": Seperate Table Creation Operations") {
      implicit val ijs = SpijiTests.ijs
      val sq = new SQLContext(sc)
      import sq.implicits._
      val imgList = makeTestImages(sc, 1, SpijiTests.imgs, SpijiTests.width, SpijiTests.height).
        map(kv => NamedSQLImage(kv._1,kv._2)).toDF

      imgList.count() shouldBe SpijiTests.imgs

      imgList.registerTempTable("Images")

      sq.registerImageJ(ijs)

      //createParquetFile[NamedSQLImage]("temp.parquet").registerAsTable("people")
      val cl = sq.sql(
        """
          |CREATE TABLE NoisyImages AS
          |SELECT sample,run(image,"Add Noise") AS nsImg FROM Images
        """.stripMargin).collect

      println(cl.mkString("\n"))

      sq.sql("""
        |CREATE TABLE FiltImg AS
        |SELECT sample,run2(nsImg,"Median...","radius=3") AS fImg
        |FROM NoisyImages
             """.stripMargin
      )
      val medstats = sq.sql(
        """
          | SELECT sample,stats(fImg) FROM FiltImg
        """.stripMargin).collect

      medstats.length shouldBe SpijiTests.imgs
      println(medstats.mkString("\n\t"))

    }
    test(conName + ": Hive Seperate Table Creation Operations") {
      implicit val ijs = SpijiTests.ijs
      val sq = new HiveContext(sc)

      import sq.implicits._
      val imgList = makeTestImages(sc, 1, SpijiTests.imgs, SpijiTests.width, SpijiTests.height).
        map(kv => NamedSQLImage(kv._1,kv._2)).toDF

      imgList.count() shouldBe SpijiTests.imgs

      imgList.registerTempTable("Images")

      sq.registerImageJ(ijs)

      //createParquetFile[NamedSQLImage]("temp.parquet").registerAsTable("people")
      val cl = sq.sql(
        """
          |INSERT INTO TABLE NoisyImages
          |SELECT sample,run(image,"Add Noise") AS nsImg FROM Images
        """.stripMargin).collect

      println(cl.mkString("\n"))

      sq.sql("""
               |CREATE TABLE FiltImg AS
               |SELECT sample,run2(nsImg,"Median...","radius=3") AS fImg
               |FROM NoisyImages
             """.stripMargin
      )
      val medstats = sq.sql(
        """
          | SELECT sample,stats(fImg) FROM FiltImg
        """.stripMargin).collect

      medstats.length shouldBe SpijiTests.imgs
      println(medstats.mkString("\n\t"))

    }

  }
}

object IJSqlTest extends Serializable {
  case class NamedArray(sample: String, intArray: Array[Int])

}