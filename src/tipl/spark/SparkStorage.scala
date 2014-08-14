/**
 *
 */
package tipl.spark

/**
 * A SparkBased version of the Storage Module which loads and processes images using Spark instead of the old VirtualAim basis
 * @author mader
 *
 */

class SSTImg(baseImg: TImgRO) extends ATImg(baseImg, baseImg.getImageType) {

  override def getPolyImage(sliceNumber: Int, asType: Int) = baseImg.getPolyImage(sliceNumber, asType)

  override def getSampleName() = baseImg.getSampleName

  override def inheritedAim(inAim: TImgRO) = DTImg.ConvertTImg(SparkGlobal.getContext, inAim, inAim.getImageType)
}

abstract class SparkStorage extends ITIPLStorage {
  override def readTImg(path: String, readFromCache: Boolean, saveToCache: Boolean): TImg = {
    val sc = SparkGlobal.getContext().sc
    val bf = sc.byteFolder(path + "/*.tif") // keep it compatible with the older version


    val tifLoad = bf.toTiffSlices
    //val outImage = tifLoad.load
    val ssd = SlicesToDTImg(tifLoad)


    //val realImage = tifLoad.loadAsValues
    return new SSTImg(null) //ssd.load)
    //null
  }
}

object SparkStorage {

  trait DeadTImg extends TImg {

  }

}