package tipl.spark

import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.{Partition, SparkContext}
import tipl.formats.{TImg, TImgRO}
import tipl.util._

import scala.collection.mutable.{Map => MuMap}
import scala.reflect.ClassTag
import scala.{specialized => spec}

private[spark]
class DSImgPartition(val prev: Partition, val startIndex: Long)
  extends Partition with Serializable {
  override val index: Int = prev.index
}


class FlatDSImg[@spec(Boolean, Byte, Short, Int, Long, Float, Double) T]
(dim: D3int, pos: D3int, elSize: D3float, imageType: Int, baseImg: IndexedSeq[(D3int,
  TImgSlice[Array[T]])], path: TypedPath)
(implicit lm: ClassTag[T])
  extends TImg.ATImg(dim, pos, elSize, imageType) {
  override def getPolyImage(sliceNumber: Int, asType: Int): AnyRef =
    TImgTools.convertArrayType(baseImg(sliceNumber)._2.get(), getImageType, asType, getSigned,
      getShortScaleFactor)

  override def getSampleName: String = path.getPath()
}


/**
 * A scala version of the DTImg class, designed for handling images. It is vastly superior to
 * DTImg since it uses the same types as KVImg
 * @note The class takes advantage of the specialized annotation to allow for more natural
 *       expression of each slice as an array rather than a generic
 *       future extensions should expand this specialization to take advantage of this functionality
 *       Created by mader on 10/13/14.
 */
class DSImg[@spec(Boolean, Byte, Short, Int, Long, Float, Double) T]
(dim: D3int, pos: D3int, elSize: D3float, imageType: Int, baseImg: RDD[(D3int,
  TImgSlice[Array[T]])], path: TypedPath, var desiredPartitions: Int = 100)
(implicit lm: ClassTag[T])
  extends TImg.ATImg(dim, pos, elSize, imageType) {

  def this(hd: TImgTools.HasDimensions, baseImg: RDD[(D3int, TImgSlice[Array[T]])],
           imageType: Int)(implicit lm: ClassTag[T]) =
    this(hd.getDim, hd.getPos, hd.getElSize, imageType, baseImg,
      TIPLStorageManager.createVirtualPath("Nothing")
    )(lm)

  /**
   * Secondary constructor directly from TImg data
   * @param sc
   * @param cImg
   * @param imageType
   * @param lm
   * @return
   */
  def this(sc: SparkContext, cImg: TImgRO, imageType: Int)(implicit lm: ClassTag[T]) =
    this(cImg.getDim, cImg.getPos, cImg.getElSize, imageType, DSImg.MigrateImage[T](sc, cImg,
      imageType), cImg.getPath())(lm)

  def getBaseImg() = baseImg


  /**
   * A fairly simple operation of filtering the RDD for the correct slice and returning that slice
   */
  override def getPolyImage(sliceNumber: Int, asType: Int): AnyRef = {
    if ((sliceNumber < 0) || (sliceNumber >= getDim.z)) throw new IllegalArgumentException(this
      .getSampleName + ": Slice requested (" + sliceNumber + ") exceeds image dimensions " + getDim)
    val zPos: Int = getPos.z + sliceNumber

    val outSlices = this.baseImg.lookup(new D3int(getPos.x, getPos.y, zPos))
    if (outSlices.size != 1) {
      throw new IllegalArgumentException(this.getSampleName + ", lookup failed (#" + outSlices
        .size + " found):" + sliceNumber + " (z:" + zPos + "), of " + getDim + " of #" + this
        .baseImg.count + " blocks")
    }
    val curSlice = outSlices(0).get
    return TImgTools.convertArrayType(curSlice, getImageType, asType, getSigned,
      getShortScaleFactor)
  }

  override def getSampleName: String = path.getPath()

  def spreadSlices(zSlices: Int) = {
    baseImg.flatMap {
      inBlock =>
        for (curSlice <- -zSlices to zSlices) yield DSImg.BlockShifter(new D3int(0, 0,
          curSlice))(inBlock)
    }
  }
  import DSImg.FancyTImgSlice

  def changeTypes[V](outType: Int)(implicit ctv: ClassTag[V],num: Numeric[V]): DSImg[V] = {
    new DSImg[V](this,getBaseImg.mapValues(_.changeType[V](outType)),outType)
  }

}

import tipl.util.D3int

import scala.{specialized => spec}
object DSImg {



  implicit class FancyTImgSlice[T : ClassTag](ts: TImgSlice[Array[T]]) {
    /**
     * Concatenate two images together for multivalued data
     * @param otherSlice the other slice (must be the same size)
     * @tparam V the type
     * @return a new slice with a tuple2 type
     */
    def ++[V : ClassTag](otherSlice: TImgSlice[Array[V]]):
    TImgSlice[Array[(T,V)]] = {
      TImgSlice.doSlicesSizeMatch(ts,otherSlice)
      val combArr = ts.get.zip(otherSlice.get)
      new TImgSlice[Array[(T,V)]](
        combArr.toArray,
        ts
      )
    }

    /**
     * Add two numeric supported slices together
     * @param otherSlice
     * @param numt
     * @param numv
     * @tparam V
     * @return double image
     */
    def +[V : ClassTag](otherSlice: TImgSlice[Array[V]])(
      implicit numt: Numeric[T], numv: Numeric[V]) = {
      TImgSlice.doSlicesSizeMatch(ts,otherSlice)
      val combArr = ts.get.zip(otherSlice.get).
        map(i =>
        numt.toDouble(i._1)+numv.toDouble(i._2)
        )
      new TImgSlice[Array[Double]](
        combArr.toArray,
        ts
      )
    }

    def changeType[V](outType: Int)(implicit ctv: ClassTag[V],numv: Numeric[V]): TImgSlice[Array[V]]
    = {
      TImgTools.isValidType(outType)
      new TImgSlice[Array[V]](
        TypeMacros.castArr(ts.getAsDouble,outType).asInstanceOf[Array[V]]
        ,ts)
    }

  }

  abstract class SlicePartitioner(minVal: Int,
                                  maxVal: Int, partitions: Int) extends
  org.apache.spark.Partitioner {
    type T
    require(minVal<maxVal)
    require(partitions>0)

    def this(minVal: Int, maxVal: Int) = this(minVal,maxVal,SparkGlobal.calculatePartitions
      (maxVal-minVal))

    override def numPartitions: Int = Math.min(partitions,maxVal-minVal)
    override def getPartition(key: Any): Int = {
      val sliceId = key match {
        case slice: D3int => slice.gz()
        case slice: Int => slice
        case slice: Number => slice.doubleValue()
      }
      if (sliceId>=minVal & sliceId<=maxVal) {
        Math.floor(((sliceId - minVal)) / (maxVal - minVal + 1) * numPartitions).toInt
      } else {
        throw new IllegalArgumentException("Slice not in range of partitioner:" + this)
      }
    }
    /**
     * A list of all the keys
     * @return
     */
    def getAllKeys(): Array[T]
  }

  case class D3IntSlicePartitioner(pos: D3int, dim: D3int) extends SlicePartitioner(pos.z,pos
    .z+dim.z) {
    type T = D3int

    override def getAllKeys(): Array[T] = (for(ipos <- pos.z to (pos.z+dim.z)) yield new
        D3int(pos.x,
      pos.y,ipos))
      .toArray
  }

  case class IntSlicePartitioner(minVal: Int, maxVal: Int) extends SlicePartitioner(minVal,maxVal) {
    type T = Int
    def getAllKeys(): Array[Int] = (for(i <- minVal to maxVal) yield i)
      .toArray
  }

  var futureTImgMigrate = false

  /**
   * import an image from an existing TImgRO by reading in every slice (this
   * is no manually done and singe core..)
   *
   * @param sc
   * @param cImg
   * @param imgType
   * @return
   */
  def MigrateImage[@spec(Boolean, Byte, Short, Int, Long, Float, Double) U](sc: SparkContext,
                                                                            cImg: TImgRO,
                                                                            imgType: Int):
  RDD[(D3int, TImgSlice[Array[U]])] = {
    assert((TImgTools.isValidType(imgType)))
    val imgDim: D3int = cImg.getDim
    val imgPos: D3int = cImg.getPos
    val sliceDim: D3int = new D3int(imgDim.x, imgDim.y, 1)
    val zero: D3int = new D3int(0)
    val partitionCount: Int = SparkGlobal.calculatePartitions(cImg.getDim.z)
    sc.parallelize(0 until imgDim.z). // create indices with correct partitioning
      map { curSlice =>
      val nPos = new D3int(imgPos.x, imgPos.y, imgPos.z + curSlice)
      (nPos,
        if (futureTImgMigrate) new TImgSlice.TImgSliceFromImage[Array[U]](cImg, curSlice,
          imgType, nPos, sliceDim, zero)
        else new TImgSlice[Array[U]](cImg.getPolyImage(curSlice, imgType).asInstanceOf[Array[U]],
          nPos, sliceDim)
        )
    }.partitionBy(new D3IntSlicePartitioner(imgPos,imgDim))

  }
  /**
   * A simple block shifting function
   *
   * @tparam T the type of of the slice involved
   * @author mader
   */
  def BlockShifter[T](inOffset: D3int) = {
    (inData: (D3int, TImgSlice[T])) =>
      val inSlice: TImgSlice[T] = inData._2
      val nOffset: D3int = inOffset
      val oPos: D3int = inData._1
      val nPos: D3int = new D3int(oPos.x + nOffset.x, oPos.y + nOffset.y, oPos.z + nOffset.z)
      (nPos, new TImgSlice[T](inSlice.getClone, nPos, inSlice.getDim, nOffset))
  }

  def posMapFun(pos: D3int, dim: D3int) = {
    posVal: D3int =>
      (posVal.z, (posVal.y - pos.y) * dim.x
        + posVal.x - pos.x)
  }

  /**
   * Create a new KVImg from the given DSImg
   * @param inImg the image to be converted
   * @param tm classtag for the type
   * @tparam T the type of the values (Array[T] for the dsimg slices)
   * @return an KVImg
   */
  def toKVImg[@spec(Boolean, Byte, Short, Int, Long, Float, Double) T](inImg: DSImg[T])
                                                                      (implicit tm: ClassTag[T]):
  KVImg[T] = {

    val imgType = inImg.getImageType
    new KVImg(inImg, imgType,
      DTImgOps.DTrddToKVrdd(inImg.getBaseImg, imgType, inImg.getPos, inImg.getDim)
    )
  }

  def fromKVImg[@spec(Boolean, Byte, Short, Int, Long, Float, Double) T](inImg: KVImg[T])
                                                                        (implicit T: ClassTag[T],
                                                                          num: Numeric[T]):
      DSImg[T] = {
    val dim = inImg.getDim
    val pos = inImg.getPos
    val elSize = inImg.getElSize
    val sliceSize = dim.x * dim.y
    val indFun = posMapFun(pos,dim)

    var rddObj = inImg.getBaseImg().
      mapPartitions(
      inPoints => {
        val outMap = MuMap[Int, Array[T]]()
        while (inPoints.hasNext) {
          val cPt = inPoints.next()
          val (z, ind) = indFun(cPt._1)
          outMap.getOrElseUpdate(z, new Array[T](sliceSize))(ind)=cPt._2
        }
        outMap.toIterator
      }
    ,true).
      groupByKey(SparkGlobal.getPartitioner(inImg)) // the slices might have somehow ended up on
      // multiple partitions
    val sslices = rddObj.map(
      inSlice => {
        val (id,slices) = inSlice
        val oPos = new D3int(pos.x,pos.y,id)
        val fArray = slices.reduce(
          (slA,slB) => {
            var i=0;
            while(i<slA.length) {
              slA(i)=num.plus(slA(i),slB(i))
              i+=1
            }
            slA
          }
        )
        (oPos,new TImgSlice[Array[T]](fArray,oPos,dim))
      }
    )
    new DSImg[T](dim,pos,inImg.getElSize,inImg.getImageType,sslices,inImg.getPath)
  }

}
