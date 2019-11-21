/**
 *
 */
package tipl.spark

import tipl.formats.TImgRO
import tipl.formats.TImg
import tipl.util.ITIPLStorage
import tipl.spark.IOOps._
import tipl.util.TypedPath

/**
 * A SparkBased version of the Storage Module which loads and
 * processes images using Spark instead of the old VirtualAim basis
 *
 * @author mader
 *
 */
@deprecated("Not a usuable system for future storage, remove", "1.0")
class SSTImg(baseImg: TImgRO) extends TImg.ATImg(baseImg, baseImg.getImageType) {

  override def getPolyImage(sliceNumber: Int, asType: Int) =
    baseImg.getPolyImage(sliceNumber, asType)

  override def getSampleName() = baseImg.getSampleName

  override def inheritedAim(inAim: TImgRO) =
    DTImg.ConvertTImg(SparkGlobal.getContext, inAim, inAim.getImageType)
}

@deprecated("Not a usuable system for future storage, remove", "1.0")
abstract class SparkStorage extends ITIPLStorage {
  override def readTImg(path: TypedPath, readFromCache: Boolean, saveToCache: Boolean): TImg = {
    val sc = SparkGlobal.getContext().sc
    // keep it compatible with the older version
    val bf = sc.binaryFiles(path.append("/*.tif").getPath).mapValues(_.toArray)

    val tifLoad = bf.toTiffSlices
    //val outImage = tifLoad.load
    val ssd = SlicesToDTImg(tifLoad)

    //val realImage = tifLoad.loadAsValues
    new SSTImg(null) //ssd.load)
    //null
  }
}


object SparkStorage {

  trait DeadTImg extends TImg {

  }


}