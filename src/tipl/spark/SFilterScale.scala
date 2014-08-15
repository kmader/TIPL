/**
 *
 */
package tipl.spark

import tipl.util.ArgumentParser
import tipl.tools.BaseTIPLPluginIn
import tipl.tools.GrayAnalysis
import tipl.tests.TestPosFunctions
import tipl.util.TIPLPluginManager
import tipl.util.TImgTools
import tipl.util.D3int
import scala.collection.JavaConversions._
import tipl.formats.TImgRO
import tipl.tools.GrayVoxels
import tipl.util.ITIPLPlugin
import tipl.spark.TypeMacros._

/**
 * A spark based code to perform filters similarly to the code provided VFilterScale
 * @author mader
 *
 */
class SFilterScale extends BaseTIPLPluginIn with Serializable {
  @TIPLPluginManager.PluginInfo(pluginType = "Filter",
    desc = "Spark-based filtering and scale",
    sliceBased = false, sparkBased = true)
  val myFactory: TIPLPluginManager.TIPLPluginFactory = new TIPLPluginManager.TIPLPluginFactory() {
    override def get(): ITIPLPlugin = {
      return new SFilterScale;
    }
  };

  override def setParameter(p: ArgumentParser, prefix: String): ArgumentParser = {
		  //
    return p
  }



  override def getPluginName() = {
    "FilterScale:Spark"
  }


  var singleGV: Array[GrayVoxels] = Array();

  override def execute(): Boolean = {
    print("Starting Plugin..." + getPluginName);
    val filterFun = (ival: (D3int, Long)) => ival._2 > 0
    val gbFun = (ival: (D3int, Long)) => ival._2
    val gvList = labeledImage.getBaseImg. // get it into the scala format
      filter(filterFun). // remove zeros
      groupBy(gbFun). // group by value
      map(ShapeAnalysis.singleShape) // run shape analysis
    singleGV = gvList.collect()

    true
  }

  var labeledImage: KVImg[Long] = null

  override def LoadImages(inImages: Array[TImgRO]) = {
    labeledImage = inImages(0).toKV.toKVLong
  }

  override def getInfo(request: String): Object = {

    val output = GrayAnalysis.getInfoFromGVArray(singleGV, singleGV.length, request);
    if (output == null) return super.getInfo(request);
    else return output;
  }


}

object SFilterScale {

}

object SFilterTest extends SFilterScale {
  def main(args: Array[String]): Unit = {
    val testImg = TestPosFunctions.wrapItAs(10,
      new TestPosFunctions.DiagonalPlaneAndDotsFunction(), TImgTools.IMAGETYPE_INT);


    LoadImages(Array(testImg))
    setParameter("-csvname=" + true + "_testing.csv");
    execute();

  }
}
