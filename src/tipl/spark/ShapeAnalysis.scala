/**
 *
 */
package tipl.spark

import tipl.formats.TImgRO
import tipl.formats.TImg
import tipl.util.TImgTools
import tipl.tools.BaseTIPLPluginIn
import java.lang.Long
import tipl.util.D3int
import tipl.tools.GrayVoxels
import tipl.util.TIPLPluginManager
import tipl.util.ITIPLPlugin
import tipl.tools.GrayAnalysis
import tipl.util.ArgumentParser

/**
 * A spark based code to perform shape analysis similarly to the code provided GrayAnalysis
 * @author mader
 *
 */
class ShapeAnalysis extends BaseTIPLPluginIn {
	@TIPLPluginManager.PluginInfo(pluginType = "ShapeAnalysis",
			desc="Spark-based shape analysis",
			sliceBased=false,sparkBased=true)
	val myFactory: TIPLPluginManager.TIPLPluginFactory = new TIPLPluginManager.TIPLPluginFactory() {
		override def get():ITIPLPlugin = {
			return new ShapeAnalysis;
		}
	};
  
	override def setParameter(p: ArgumentParser, prefix: String): ArgumentParser = {
	  	analysisName = p.getOptionString(prefix+"analysis",analysisName,"Name of analysis")
		outputName = p.getOptionPath(prefix+"csvname",outputName,"Name of analysis")
		return p
	}
	var labeledImage: KVImg[Long] = null
	var analysisName = "Shape"
	var outputName="output.csv"
	
	override def getPluginName() = {  "ShapeAnalysis:Spark" }
	
	/**
	 * The following is the function that turns a list of points into an analyzed shape
	 */
	private def singleShape(label: Long,pointList: Iterable[(D3int,Long)]): GrayVoxels = {
	  val cLabel = label.toInt
	  val cVoxel=new GrayVoxels(cLabel)
	  for(cpt <- pointList) {
	    cVoxel.addVox(cpt._1.x, cpt._1.y, cpt._1.z, cLabel)
	  }
	  cVoxel.mean
	   for(cpt <- pointList) {
	    cVoxel.addCovVox(cpt._1.x, cpt._1.y, cpt._1.z, cLabel)
	  }
	  cVoxel.calcCOV
	  return cVoxel
	}
	
	override def execute():Boolean = { 
	  print("Starting Plugin..."+getPluginName);
	  val gvList=labeledImage.getBaseImg.rdd. // get it into the scala format
	  filter(_._2>0). // remove zeros
	  groupBy(_._2). // group by value
	  map(labelPoints => (singleShape(labelPoints._1,labelPoints._2))) // run shape analysis
	  val singleGV=gvList.collect()
	  
	  GrayAnalysis.ScalaLacunAnalysis(singleGV,labeledImage,"analysis.csv","Shape",true);

	  true
	 }
	
	override def LoadImages(inImages: Array[TImgRO]) = {
	  labeledImage = inImages(0) match {
	    case m: KVImg[_] => m.toKVLong
	    case m: DTImg[_] => m.asKV().toKVLong()
	    case m: TImgRO => KVImg.ConvertTImg(SparkGlobal.getContext(getPluginName()), m, TImgTools.IMAGETYPE_INT).toKVLong()
	  }
	}
	

}
