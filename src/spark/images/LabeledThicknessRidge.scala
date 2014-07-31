/**
 *
 */
package spark.images
import tipl.spark.SparkGlobal
import tipl.spark.KVImgOps
import tipl.util.TImgTools
import tipl.formats.TImgRO
import tipl.formats.FNImage
import tipl.util.D3int
import org.apache.spark.SparkContext
import spark.images.ImageTools


/**
 * A script to create a labeled thickness ridge from a ridge and a thickness image.
 * @author mader
 *
 */
object LabeledThicknessRidge {
    def getParameters(args: Array[String]) = {
      val p = SparkGlobal.activeParser(args)
      val ridgeFile = p.getOptionPath("ridge", "ridge.tif", "The ridge map")
      val thickFile = p.getOptionPath("thickness", "thickmap.tif", "The thickness map")
      val savePath = p.getOptionPath("save", ridgeFile+"_cl", "Directory for output")
      (ridgeFile,thickFile,savePath,p)
  }
	def main(args: Array[String]) {
	  val (ridgeFile,thickFile,savePath,p) = getParameters(args)
	  val ridgeImg = TImgTools.ReadTImg(ridgeFile)
	  val thickImg = TImgTools.ReadTImg(thickFile)
	  val combinedImg = mergeRidgeThick(ridgeImg,thickImg)
	  val sc = SparkGlobal.getContext("LabeledThicknessRidge").sc
	  val justRidge = KVImgOps.TImgROToKVThresh(sc, combinedImg, 0).getBaseImg
	  val compLabeledRidge = ImageTools.compLabeling(justRidge)
	  compLabeledRidge.map{pvec => (pvec._1.x,pvec._1.y,pvec._1.z,pvec._2._1,pvec._2._2)}.saveAsTextFile(savePath)
	  
	  
	}
	def mergeRidgeThick(ridge: TImgRO, thick: TImgRO): TImgRO = {
	  new FNImage(Array(ridge,thick),TImgTools.IMAGETYPE_FLOAT,new FNImage.MaskImage(0))
	}

}