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
import tipl.tools.BaseTIPLPluginIn

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
	val stepSize = 0.5f
	
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

	 val spread_voxels = 
	   (pvec: (D3int,(Long,Float)), windSize: D3int,kernel: Option[BaseTIPLPluginIn.morphKernel]) => {
            val windx=(-windSize.x to windSize.x)
            val windy=(-windSize.y to windSize.y)
            val windz=(-windSize.z to windSize.z)
            val pos=pvec._1
            for(x<-windx; y<-windy; z<-windz;
            if(kernel.map(_.inside(0,0,pos.x, pos.x+x, pos.y, pos.y+y, pos.z, pos.z+z)).getOrElse(true)))
                yield (new D3int(pos.x+x,pos.y+y,pos.z+z),(pvec._2,(x==0 & y==0 & z==0)))
        }
	   /**
	    * only keep points do not originate from a real point
	    */
	   val remove_edges = (pvec: (D3int,Iterable[((Long,Float),Boolean)])) => {
	     for(cEle <- pvec._2; if(cEle._2)) yield true
	     false
	   }
	   val compare_points = (a: ((Long,Float),Boolean),b: ((Long,Float),Boolean)) 
	   => a._1._2.compare(b._1._2)
	   
	   val collect_voxels = 
	     (pvec: Iterable[((Long,Float),Boolean)]) => {
	       pvec.minBy (_._1._2)
	     }
	     val get_change = (avec: (D3int,((Long,Float),Boolean))) => avec._2._2
	override def execute():Boolean = { 
			print("Starting Plugin..."+getPluginName);
			val curKernel: Option[BaseTIPLPluginIn.morphKernel] = 
			  if (neighborKernel == null)  
			  None
			else 
			 Some(neighborKernel)
			
			var changes = 1L
			var curDist = stepSize
			var iter=0
			while(((curDist<=maxUsuableDistance) | maxUsuableDistance>0)  & changes>0) {
			  val spreadObj = labeledDistanceMap.
			   flatMap(spread_voxels(_,neighborSize,curKernel)).
			  groupByKey.filter(remove_edges).mapValues(collect_voxels)
			  changes = spreadObj.filter(get_change).count
			  print("Iter:"+iter+", Changes:"+changes)
			  
			}
			true
	}

	val fillImage = (tempObj: TImgTools.HasDimensions,defValue: java.lang.Double) => {
		val defFloat=new PureFImage.ConstantValue(defValue)
		
		KVImgTools.createFromPureFun[java.lang.Float](SparkGlobal.getContext(getPluginName()), tempObj, defFloat , TImgTools.IMAGETYPE_FLOAT)
	}
	var labeledDistanceMap: RDD[(D3int,(Long,Float))] = null
			/**
			 * The first image is the 
			 */
			override def LoadImages(inImages: Array[TImgRO]) = {
			var labelImage=inImages(0)
			var maskImage=if(inImages.length>1) inImages(1) else null

					val labeledImage = (labelImage match {
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
					val maskDistance=initialDistances.
							leftOuterJoin(maskedImage).mapValues(maskPlusDist)
					labeledDistanceMap = labeledImage.join(maskDistance)

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
