/**
 *
 */
package tipl.spark

import tipl.formats.TImgRO
import tipl.spark.TypeMacros._
import tipl.tests.TestPosFunctions
import tipl.tools.{BaseTIPLPluginIn, GrayAnalysis, GrayVoxels}
import tipl.util._

import scala.collection.JavaConversions._

/**
 * A spark based code to perform shape analysis similarly to the code provided GrayAnalysis
 * @author mader
 *
 */
class ShapeAnalysis extends BaseTIPLPluginIn with Serializable {


  @TIPLPluginManager.PluginInfo(pluginType = "ShapeAnalysis",
    desc = "Spark-based shape analysis",
    sliceBased = false, sparkBased = true)
  class saSparkFactory extends TIPLPluginManager.TIPLPluginFactory {
    override def get(): ITIPLPlugin = {
      new ShapeAnalysis
    }
  }


  override def setParameter(p: ArgumentParser, prefix: String): ArgumentParser = {
    analysisName = p.getOptionString(prefix + "analysis", analysisName, "Name of analysis")
    outputName = p.getOptionPath(prefix + "csvname", outputName, "Name of analysis")
    p
  }

  var analysisName = "Shape"
  var outputName = TIPLStorageManager.openPath("output.csv")

  override def getPluginName() = {
    "ShapeAnalysis:Spark"
  }

  var singleGV: Array[GrayVoxels] = Array()

  override def execute(): Boolean = {
    print("Starting Plugin..." + getPluginName)
    val filterFun = (ival: (D3int, Long)) => ival._2 > 0
    val gbFun = (ival: (D3int, Long)) => ival._2
    val gvList = labeledImage.getBaseImg. // get it into the scala format
      filter(filterFun). // remove zeros
      groupBy(gbFun). // group by value
      map(ShapeAnalysis.singleShape) // run shape analysis
    singleGV = gvList.collect()
    singleGV.foreach(x => print("Value " + x.getLabel + ", " + x.count))

    GrayAnalysis.ScalaLacunAnalysis(singleGV, labeledImage, outputName, analysisName, true)

    true
  }

  var labeledImage: KVImg[Long] = null

  override def LoadImages(inImages: Array[TImgRO]) = {
    labeledImage = inImages(0).toKV.toKVLong
  }

  override def getInfo(request: String): Object = {

    val output = GrayAnalysis.getInfoFromGVArray(singleGV, singleGV.length, request)
    if (output == null) super.getInfo(request)
    else output
  }

}


object ShapeAnalysis {
  /**
   * The following is the (static) function that turns a list of points into an analyzed shape
   */
  def singleShape(cPoint: (Long, Iterable[(D3int, Long)])) = {
    val label = cPoint._1
    val pointList = cPoint._2
    val cLabel = label.toInt
    val cVoxel = new GrayVoxels(cLabel)
    for (cpt <- pointList) {
      cVoxel.addVox(cpt._1.x, cpt._1.y, cpt._1.z, cLabel)
    }
    cVoxel.mean
    for (cpt <- pointList) {
      cVoxel.addCovVox(cpt._1.x, cpt._1.y, cpt._1.z, cLabel)
    }
    cVoxel.calcCOV
    cVoxel.diag
    for (cpt <- pointList) {
      cVoxel.setExtentsVoxel(cpt._1.x, cpt._1.y, cpt._1.z)
    }
    cVoxel
  }

  def meshObject(cPoint: (Long, Iterable[(D3int, Long)])): Double = {
    val cPts = cPoint._2.toList.map(_._1)
    val hullObj = tipl.ccgeom.ConvexHull3D.HullFromD3List(cPts)
    hullObj.getArea
  }
}


object SATest extends ShapeAnalysis {
  def main(args: Array[String]): Unit = {
    val testImg = TestPosFunctions.wrapItAs(10,
      new TestPosFunctions.DiagonalPlaneAndDotsFunction(), TImgTools.IMAGETYPE_INT)


    LoadImages(Array(testImg))
    setParameter("-csvname=" + true + "_testing.csv")
    execute()

  }
}
