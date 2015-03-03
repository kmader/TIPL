package tipl.ij.scripting

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.FunSuite
import _root_.tipl.ij.Spiji.PIPOps
import _root_.tipl.ij.scripting.ImagePlusIO.{PortableImagePlus, LogEntry}

/**
 * Created by mader on 1/20/15.
 */


  class ImageLogTests extends FunSuite {

    implicit class jsonToString(jsonText: JValue) {
      def show(): String = pretty(render(jsonText))
    }
    val width = 10
  val height = 10

    lazy val createImage = new PortableImagePlus(
      Array.fill[Int](width,height)(1000))

    test("LogEntry to JSON") {
      val le = LogEntry.create("TestSource", "TestInfo")
      val jsle = le.toJSON()
      val jstxt = jsle.show
      println(jstxt)
      assert(LogEntry.fromJSON(jsle).le_eq(le),"Matches before and after serialization")
    }

    test("Merge log entries") {
      val dblImg = createImage ++ createImage
      println(createImage.imgLog.toJsStrArray().mkString("\n\t"))
      println(dblImg.imgLog.toJsStrArray().mkString("\n\t"))
      assert(dblImg.imgLog(0).opType == PIPOps.MERGE_STORE,"first command is merge store")
      assert(createImage.imgLog.log_eq(dblImg.imgLog(0).children),
        "first child of the first image is equal to the image")

      assert(createImage.imgLog.log_eq(dblImg.imgLog(1).children),
        "first child is equal to the image")
      assert(dblImg.imgLog(2).opType == PIPOps.MERGE,"third command is merge")


    }

  }
  class ImagePlusIOTests extends FunSuite {

  }