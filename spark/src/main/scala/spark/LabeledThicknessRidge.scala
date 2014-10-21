/**
 *
 */
package spark.images

import org.apache.spark.SparkContext._
import tipl.formats.{FNImage, TImgRO}
import tipl.spark.{KVImgOps, SparkGlobal}
import tipl.util.{D3int, TImgTools}

/**
 * A script to create a labeled thickness ridge from a ridge and a thickness image.
 * @author mader
 *
 */
object LabeledThicknessRidge {
  def getParameters(args: Array[String]) = {
    val p = SparkGlobal.activeParser(args)
    val ridgeFile = p.getOptionPath("ridge", "ridge.tif", "The ridge map")
    val labelFile = p.getOptionPath("label", "bubblelabels.tif", "The labels to use")
    val thickFile = p.getOptionPath("thickness", "thickmap.tif", "The thickness map")
    val neighbors = p.getOptionD3int("neighbors", new D3int(1, 1, 1),
      "The neighborhood for connecting ridge points")
    val savePath = p.getOptionPath("save", ridgeFile + "_cl", "Directory for output")
    (ridgeFile, labelFile, thickFile, neighbors, savePath, p)
  }

  def main(args: Array[String]) {
    val (ridgeFile, labelFile, thickFile, neighbors, savePath, p) = getParameters(args)
    p.checkForInvalid()
    val ridgeImg = TImgTools.ReadTImg(ridgeFile)
    val labelImg = TImgTools.ReadTImg(labelFile)
    val thickImg = TImgTools.ReadTImg(thickFile)
    val labelMaskedImg = maskImage(ridgeImg, labelImg, TImgTools.IMAGETYPE_INT)
    val thickMaskedImg = maskImage(ridgeImg, thickImg)
    val sc = SparkGlobal.getContext("LabeledThicknessRidge").sc
    val labeledRidge = KVImgOps.TImgROToKVThresh(sc, labelMaskedImg, 0).getBaseImg
    val thickRidge = KVImgOps.TImgROToKVThresh(sc, thickMaskedImg, 0).getBaseImg
    val compLabeledRidge = labeledRidge.join(thickRidge)
    // We want the bubble labels and the skeleton labels (subpores within pores)
    val dclRidge = ImageTools.compLabeling(compLabeledRidge)
    // x, y, z, pore group, pore subgroup, thickness
    dclRidge.map {
      pvec =>
        pvec._1.x + "," + pvec._1.y + "," + pvec._1.z + "," + pvec._2._2._1 + "," +
          "" + pvec._2._1 + "," + pvec._2._2._2
    }.saveAsTextFile(savePath.getPath)
  }

  def maskImage(mask: TImgRO, value: TImgRO, asType: Int = TImgTools.IMAGETYPE_FLOAT): TImgRO = {
    new FNImage(Array(mask, value), asType, new FNImage.MaskImage(0))
  }

}