package tipl.spark

import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.{Partition, SparkContext}
import tipl.formats.{TImg, TImgRO}
import tipl.util._

import scala.reflect.ClassTag
import scala.{specialized => spec}

private[spark]
class DSImgPartition(val prev: Partition, val startIndex: Long)
  extends Partition with Serializable {
  override val index: Int = prev.index
}

/**
 * A scala version of the DTImg class, designed for handling images. It is vastly superior to DTImg since it uses the same types as KVImg
 * @note The class takes advantage of the specialized annotation to allow for more natural expression of each slice as an array rather than a generic
 *       future extensions should expand this specialization to take advantage of this functionality
 * Created by mader on 10/13/14.
 */
class DSImg[@spec(Boolean, Byte, Short, Int, Long, Float, Double) T]
(dim: D3int, pos: D3int, elSize: D3float, imageType: Int, baseImg: RDD[(D3int, TImgBlock[Array[T]])],path: TypedPath)
                                                                     (implicit lm: ClassTag[T])
  extends TImg.ATImg(dim,pos,elSize,imageType) {

  def this(hd: TImgTools.HasDimensions, baseImg: RDD[(D3int, TImgBlock[Array[T]])], imageType: Int)(implicit lm: ClassTag[T]) =
  this(hd.getDim,hd.getPos,hd.getElSize,imageType,baseImg,TypedPath.virtualPath("Nothing"))(lm)
  /**
   * Secondary constructor directly from TImg data
   * @param sc
   * @param cImg
   * @param imageType
   * @param lm
   * @return
   */
  def this(sc: SparkContext, cImg: TImgRO, imageType: Int)(implicit lm: ClassTag[T]) =
    this(cImg.getDim,cImg.getPos,cImg.getElSize,imageType,DSImg.MigrateImage[T](sc,cImg,imageType),cImg.getPath())(lm)

  def getBaseImg() = baseImg
  /**
   * A fairly simple operation of filtering the RDD for the correct slice and returning that slice
   */
  override def getPolyImage(sliceNumber: Int, asType: Int): AnyRef = {
    if ((sliceNumber < 0) || (sliceNumber >= getDim.z)) throw new IllegalArgumentException(this.getSampleName + ": Slice requested (" + sliceNumber + ") exceeds image dimensions " + getDim)
    val zPos: Int = getPos.z + sliceNumber

    val outSlices = this.baseImg.lookup(new D3int(getPos.x, getPos.y, zPos))
    if (outSlices.size != 1)
      throw new IllegalArgumentException(this.getSampleName + ", lookup failed (#" + outSlices.size + " found):" + sliceNumber + " (z:" + zPos + "), of " + getDim + " of #" + this.baseImg.count + " blocks")
    val curSlice = outSlices(0).get
    return TImgTools.convertArrayType(curSlice, getImageType, asType, getSigned, getShortScaleFactor)
  }

  override def getSampleName: String = path.getPath()

  def spreadSlices(zSlices: Int) = {
    baseImg.flatMap {
      inBlock =>
        for(curSlice <- -zSlices to zSlices) yield DSImg.BlockShifter(new D3int(0,0,curSlice))(inBlock)
    }
  }
}

object DSImg {
  val futureTImgMigrate = false
  /**
   * import an image from an existing TImgRO by reading in every slice (this
   * is no manually done and singe core..)
   *
   * @param sc
   * @param cImg
   * @param imgType
   * @return
   */
  def MigrateImage[@spec(Boolean, Byte, Short, Int, Long, Float, Double) U](sc: SparkContext, cImg: TImgRO, imgType: Int):
  RDD[(D3int, TImgBlock[Array[U]])] = {
    assert((TImgTools.isValidType(imgType)))
    val imgDim: D3int = cImg.getDim
    val imgPos: D3int = cImg.getPos
    val sliceDim: D3int = new D3int(imgDim.x, imgDim.y, 1)
    val zero: D3int = new D3int(0)
    val partitionCount: Int = SparkGlobal.calculatePartitions(cImg.getDim.z)
    sc.parallelize(0 until imgDim.z,partitionCount). // create indices with correct partitioning
      map{curSlice =>
      val nPos = new D3int(imgPos.x,imgPos.y,imgPos.z+curSlice)
      (nPos,
        if (futureTImgMigrate) new TImgBlock.TImgBlockFromImage[Array[U]](cImg, curSlice, imgType, nPos, sliceDim, zero)
        else new TImgBlock[Array[U]](cImg.getPolyImage(curSlice, imgType).asInstanceOf[Array[U]], nPos, sliceDim)
      )
    }

  }

  /**
   * A simple block shifting function
   *
   * @param <T>
   * @author mader
   */
  def BlockShifter[T](inOffset: D3int) = {
    (inData: (D3int, TImgBlock[T])) =>
        val inSlice: TImgBlock[T] = inData._2
        val nOffset: D3int = inOffset
        val oPos: D3int = inData._1
        val nPos: D3int = new D3int(oPos.x + nOffset.x, oPos.y + nOffset.y, oPos.z + nOffset.z)
         (nPos, new TImgBlock[T](inSlice.getClone, nPos, inSlice.getDim, nOffset))
  }


}
