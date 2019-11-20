package tipl.ij.scripting

import fourquant.imagej.ImagePlusIO.{ImageLog, LogEntry}
import fourquant.imagej.PortableImagePlus.IJMetaData
import fourquant.imagej.{IJCalibration, PortableImagePlus}
import fourquant.imagej.Spiji.PIPOps
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.{Matchers, FunSuite}

/**
 * Created by mader on 1/20/15.
 */


class ImageLogTests extends FunSuite with Matchers {


  implicit class jsonToString(jsonText: JValue) {
    def show(): String = pretty(render(jsonText))
  }


  val width = 10
  val height = 10

  lazy val createImage = new PortableImagePlus(
    Array.fill[Int](width, height)(1000),IJMetaData.emptyMetaData)

  test("LogEntry to JSON") {
    val le = LogEntry.create("TestSource", "TestInfo")
    val jsle = le.toJSON()
    val jstxt = jsle.show
    println(jstxt)
    assert(LogEntry.fromJSON(jsle).le_eq(le), "Matches before and after serialization")
  }

  test("Log to JSON and back manually") {
    val slog = createImage.imgLog
    val js = slog.toJSONString()
    val logJV = parse(js)
    logJV.children.length shouldBe slog.ilog.length
    val newlog = new ImageLog(logJV.children.map(LogEntry.fromJSON(_)))
    newlog.ilog.length shouldBe slog.ilog.length
    newlog.log_eq(slog) shouldBe true
  }

  test("Log to JSON and back functions") {
    val slog = createImage.imgLog

    // try json first
    val jvs = slog.toJSON()
    val jslog = ImageLog.fromJSON(jvs)
    jslog.ilog.length shouldBe slog.ilog.length
    jslog.log_eq(slog) shouldBe true

    // now try json as strings
    val js = slog.toJSONString()
    val newlog = ImageLog.fromJSONString(js)
    newlog.ilog.length shouldBe slog.ilog.length
    newlog.log_eq(slog) shouldBe true
  }

  test("Merge log entries") {
    val dblImg = createImage ++ createImage
    println(createImage.imgLog.toJsStrArray().mkString("\n\t"))
    println(dblImg.imgLog.toJsStrArray().mkString("\n\t"))
    assert(dblImg.imgLog(0).opType == PIPOps.MERGE_STORE, "first command is merge store")
    assert(createImage.imgLog.log_eq(dblImg.imgLog(0).children),
      "first child of the first image is equal to the image")

    assert(createImage.imgLog.log_eq(dblImg.imgLog(1).children),
      "first child is equal to the image")
    assert(dblImg.imgLog(2).opType == PIPOps.MERGE, "third command is merge")

  }

}