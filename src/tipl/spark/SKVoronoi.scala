/**
 *
 */
package tipl.spark

import tipl.formats.TImgRO
import tipl.formats.TImg
import tipl.formats.PureFImage
import tipl.util.TImgTools
import java.lang.Long
import tipl.util.D3int
import tipl.tools.GrayVoxels
import tipl.util.TIPLPluginManager
import tipl.util.ITIPLPlugin
import tipl.tools.GrayAnalysis
import tipl.util.ArgumentParser
import tipl.tests.TestPosFunctions
import tipl.tools.BaseTIPLPluginIO
import tipl.util.TIPLGlobal

/**
 * A spark based code to perform shape analysis similarly to the code provided GrayAnalysis
 * @author mader
 *
 */
class SKVoronoi extends BaseTIPLPluginIO {

  	var preserveLabels = false;

	var alreadyCopied = false;
	
	var includeEdges=false;
	
	var maxUsuableDistance = -1.0
	  
	override def setParameter(p: ArgumentParser, prefix: String): ArgumentParser = {
	  val  args: ArgumentParser = super.setParameter(p, prefix);
		preserveLabels = args.getOptionBoolean(prefix + "preservelabels", preserveLabels,
				"Preserve the labels in old image");
		alreadyCopied = args.getOptionBoolean(prefix + "alreadycopied", alreadyCopied,
				"Has the image already been copied");
		includeEdges = args.getOptionBoolean(prefix + "includeedges", includeEdges,
				"Include the edges");
		maxUsuableDistance = args.getOptionDouble(prefix + "maxdistance", maxUsuableDistance,
				"The maximum distance to run the voronoi tesselation until");
		return args;
	}

			override def getPluginName() = {  "kVoronoi:Spark" }

			/**
			 * The following is the (static) function that turns a list of points into an analyzed shape
			 */
			val singleShape = (cPoint: (Long,Iterable[(D3int,Long)])) => {
				val label = cPoint._1
						val pointList = cPoint._2
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
				cVoxel.diag
				for(cpt <- pointList) {
					cVoxel.setExtentsVoxel(cpt._1.x, cpt._1.y, cpt._1.z)
				}
				cVoxel
			}
			
			override def execute():Boolean = { 
					print("Starting Plugin..."+getPluginName);
					val filterFun = (ival: (D3int,Long)) => ival._2>0
					val gbFun = (ival: (D3int,Long)) => ival._2
					val gvList=labeledImage.getBaseImg.rdd
					true
			}
			
			val fillImage = (tempObj: TImgTools.HasDimensions,defValue: java.lang.Double) => {
			 val defFloat=new PureFImage.ConstantValue(defValue)
			 
			  KVImgTools.createFromPureFun[java.lang.Float](SparkGlobal.getContext(getPluginName()), tempObj, defFloat , TImgTools.IMAGETYPE_FLOAT)
			}
			var labeledImage: KVImg[Long] = null
			var distanceImage: KVImg[java.lang.Float] = null
			/**
			 * The first image is the 
			 */
			override def LoadImages(inImages: Array[TImgRO]) = {
			  var labelImage=inImages(0)
			  var maskImage=if(inImages.length>1) inImages(1) else null
				labeledImage = labelImage match {
					case m: KVImg[_] => m.toKVLong
					case m: DTImg[_] => m.asKV().toKVLong()
					case m: TImgRO => KVImg.ConvertTImg(SparkGlobal.getContext(getPluginName()), m, TImgTools.IMAGETYPE_INT).toKVLong()
				}
			  
				distanceImage = maskImage match {
					case m: KVImg[_] => m.toKVFloat
					case m: DTImg[_] => m.asKV().toKVFloat
					case m: TImgRO => KVImg.ConvertTImg(SparkGlobal.getContext(getPluginName()), m, TImgTools.IMAGETYPE_INT).toKVFloat()
					case _ => fillImage(labeledImage,-1)
				}
			}
			override def ExportImages(templateImage: TImgRO):Array[TImg] = {
			  return Array(labeledImage,distanceImage)
			}


}

object SKTest extends SKVoronoi {
	def main(args: Array[String]):Unit = {
		val testImg = TestPosFunctions.wrapItAs(10,
				new TestPosFunctions.DiagonalPlaneAndDotsFunction(),TImgTools.IMAGETYPE_INT);

		
		LoadImages(Array(testImg))
		setParameter("-csvname="+true+"_testing.csv");
		execute();

	}
}

