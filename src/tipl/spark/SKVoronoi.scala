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
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._

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

			
			override def execute():Boolean = { 
					print("Starting Plugin..."+getPluginName);
					true
			}
			
			val fillImage = (tempObj: TImgTools.HasDimensions,defValue: java.lang.Double) => {
			 val defFloat=new PureFImage.ConstantValue(defValue)
			 
			  KVImgTools.createFromPureFun[java.lang.Float](SparkGlobal.getContext(getPluginName()), tempObj, defFloat , TImgTools.IMAGETYPE_FLOAT)
			}
			var labeledImage: RDD[(D3int,Long)] = null
			var distanceImage: RDD[(D3int,Float)] = null
			/**
			 * The first image is the 
			 */
			override def LoadImages(inImages: Array[TImgRO]) = {
			  var labelImage=inImages(0)
			  var maskImage=if(inImages.length>1) inImages(1) else null
			  
				labeledImage = (labelImage match {
					case m: KVImg[_] => m.toKVLong
					case m: DTImg[_] => m.asKV().toKVLong()
					case m: TImgRO => KVImg.ConvertTImg(SparkGlobal.getContext(getPluginName()), m, TImgTools.IMAGETYPE_INT).toKVLong()
				}).getBaseImg.rdd
				
				// any image filled with all negative distances
			  val initialDistances = fillImage(labelImage,-1).getBaseImg.rdd
			  val inMask = (ival: (D3int,java.lang.Float)) => ival._2>0
			  val toBoolean = (ival: java.lang.Float) => true
			  val maskedImage = (maskImage match {
					case m: KVImg[_] => m.toKVFloat
					case m: DTImg[_] => m.asKV().toKVFloat
					case m: TImgRO => KVImg.ConvertTImg(SparkGlobal.getContext(getPluginName()), m, TImgTools.IMAGETYPE_INT).toKVFloat()
					case _ => fillImage(labelImage,1)
				}).getBaseImg.rdd.filter(inMask).mapValues(toBoolean)
				
				// combine the images to create the starting distance map
				val maskPlusDist = (inVal: (java.lang.Float,Option[Boolean])) => {
				  inVal._2.map(inBool => 0f).getOrElse(inVal._1.floatValue)
				}
				val mergedImage=initialDistances.
				leftOuterJoin(maskedImage).mapValues(maskPlusDist)
				
			}
				
			override def ExportImages(templateImage: TImgRO):Array[TImg] = {
			 /** return Array(new KVImg(templateImage,TImgTools.IMAGETYPE_LONG,labeledImage.toJavaRDD),
			      new KVImg(templateImage,TImgTools.IMAGETYPE_FLOAT,distanceImage.toJavaRDD))
			
			* 
			*/
			  return null;
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

