
/*
 * A few basic tests for Spark
 */

package tipl.tests

import org.apache.spark.ui.tipl.JSONTools
import org.apache.spark.ui.tipl.JSONTools._
import tipl.blocks.FoamThresholdBlock
import tipl.spark.SparkGlobal


/**
 * A set of tests to make sure the webgui works as expected
 */
object WebTests {
  def main(args: Array[String]): Unit = {
    println(JSONTools.toJS(JsonTests.blankHist))
    println(JSONTools.toJS(JsonTests.multiHist))
    println(JSONTools.toJS(JSONTools.pluginsAsJSON))
    val b = new FoamThresholdBlock
    val p = b.setParameter(SparkGlobal.activeParser(args))
    println(JSONTools.toJS(p))
  }

  object JsonTests {
    import org.json4s.DefaultFormats
    // json portions of code
    private implicit val format = DefaultFormats
    import org.json4s.JsonAST._
    lazy val blankHist: JValue = JSONTools.histToJSON(Seq[(Double,Long)]((0,0)))
    val multiHist: JValue = JSONTools.histToJSON(Array[(Double,Long)](
      (0.0,5),(1,10),(2.0,20),(3.0,0)
    ))


  }
}
