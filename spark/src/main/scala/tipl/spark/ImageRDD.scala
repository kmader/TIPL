package tipl.spark

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import tipl.formats.{TImg, TImgRO}
import tipl.util.{ITIPLPluginIO, D3float, D3int, TImgSlice, TImgTools, TypedPath}

import scala.reflect.ClassTag
import scala.{specialized => spec}

/**
 * an abstract class with all of the standard TImgRO functions but getPoly
 *
 * @author mader
 */
trait TImgROLike extends TImgRO {

  var baseDimensions: TImgRO = new TImgRO.ATImgRO(baseSize, imageType) {
    println("Warning: Creating mock TImgRO class!")

    override def getPolyImage(sliceNumber: Int, asType: Int): AnyRef = null

    override def getSampleName: String = "Mock TImgRO class, should not be used for anything at all"
  }

  def baseSize: TImgTools.HasDimensions

  def imageType: Int

  def getDim(): D3int = baseSize.getDim()

  def getElSize: D3float = baseSize.getElSize

  def getOffset: D3int = baseSize.getOffset

  def getPos: D3int = baseSize.getPos

  def getProcLog: String = baseSize.getProcLog

  def appendProcLog(inData: String): String = baseDimensions.appendProcLog(inData)

  def getCompression: Boolean = baseDimensions.getCompression

  def getImageType: Int = baseDimensions.getImageType

  def getPath: TypedPath = baseDimensions.getPath

  def getShortScaleFactor: Float = baseSize.getShortScaleFactor

  def getSigned: Boolean = baseDimensions.getSigned

  def isFast: Int = baseDimensions.isFast

  def isGood: Boolean = baseDimensions.isGood
}


trait TImgLike extends TImgROLike with TImg {

  /**
   * Create a new object by wrapping the RO in a ATImg
   */
  val baseDimensionsRW: TImg = new TImg.ATImgWrappingTImgRO(baseDimensions)

  def inheritedAim(imgArray: Array[Boolean], idim: D3int, offset: D3int): TImg =
    baseDimensionsRW.inheritedAim(imgArray, idim, offset)

  def inheritedAim(imgArray: Array[Char], idim: D3int, offset: D3int): TImg =
    baseDimensionsRW.inheritedAim(imgArray, idim, offset)

  def inheritedAim(imgArray: Array[Float], idim: D3int, offset: D3int): TImg =
    baseDimensionsRW.inheritedAim(imgArray, idim, offset)

  def inheritedAim(imgArray: Array[Int], idim: D3int, offset: D3int): TImg =
    baseDimensionsRW.inheritedAim(imgArray, idim, offset)

  def inheritedAim(imgArray: Array[Short], idim: D3int, offset: D3int): TImg =
    baseDimensionsRW.inheritedAim(imgArray, idim, offset)

  def inheritedAim(inAim: TImgRO): TImg = baseDimensionsRW.inheritedAim(inAim)

  def setDim(inData: D3int) = baseDimensionsRW.setDim(inData)

  def setElSize(inData: D3float) = baseDimensionsRW.setElSize(inData)

  def setOffset(inData: D3int) = baseDimensionsRW.setOffset(inData)

  def setPos(inData: D3int) = baseDimensionsRW.setPos(inData)

  def InitializeImage(dPos: D3int, cDim: D3int, dOffset: D3int, elSize: D3float,
                      imageType: Int): Boolean =
    baseDimensionsRW.InitializeImage(dPos, cDim, dOffset, elSize, imageType)

  def setCompression(inData: Boolean) = baseDimensionsRW.setCompression(inData)

  def setShortScaleFactor(ssf: Float) = baseDimensionsRW.setShortScaleFactor(ssf)

  def setSigned(inData: Boolean) = baseDimensionsRW.setSigned(inData)

}


/**
 * Created by mader on 10/13/14.
 */
abstract class ImageRDD[@spec(Boolean, Byte, Short, Int, Long, Float, Double) T](dim: D3int,
                                                                                 pos: D3int,
                                                                                 elSize: D3float,
                                                                                 imageType: Int,
                                                                                 path: TypedPath)
                                                                                (implicit lm:
                                                                                ClassTag[T])
  extends TImg.ATImg(dim, pos, elSize, imageType) {
  type objType

  def getRdd(): RDD[(D3int, objType)]

  /**
   * A map by point on all the points in the image
   *
   * @param f
   * @param newType
   * @tparam U
   * @return
   */
  def pointMap[U: ClassTag](f: (D3int, T) => U, newType: Int): ImageRDD[U]

  /**
   * A map by slice on all slices in the image
   *
   * @param f
   * @param newType
   * @tparam U
   * @return
   */
  def sliceMap[U: ClassTag](f: (D3int, Array[T]) => Array[U], newType: Int): ImageRDD[U]

  override def getSampleName: String = getRdd().dependencies.mkString(",")

}


object ImageRDD {


  trait LoadImagesAsDSImg extends ITIPLPluginIO {
    var inImgs: Array[TImgRO] = null

    def LoadImages(inImgs: Array[TImgRO]): Unit = {
      this.inImgs = inImgs
    }

    def getDSImg[T](sc: SparkContext, imgType: Int, index: Int)(implicit tm: ClassTag[T]): DSImg[T]
    = {
      require(inImgs != null)
      require(index >= 0 && index < inImgs.length)
      new DSImg[T](sc, inImgs(index), imgType)
    }

    def ExportDSImg[T](templateIm: TImgRO): DSImg[T]

    override def ExportImages(templateIm: TImgRO): Array[TImg] = {
      Array(ExportDSImg(templateIm))
    }

  }

  trait LoadImagesAsKVImg {
    var inImgs: Array[TImgRO] = null

    def LoadImages(inImgs: Array[TImgRO]): Unit = {
      this.inImgs = inImgs
    }

    def getKVImg[T](sc: SparkContext, imgType: Int, index: Int)(implicit tm: ClassTag[T],
                                                                nm: Numeric[T]): KVImg[T] =
      getKVImg[T](sc, imgType, index, nm.zero)

    def getKVImg[T](sc: SparkContext, imgType: Int, index: Int, paddingVal: T)(implicit tm:
    ClassTag[T]): KVImg[T]
    = {
      require(inImgs != null)
      require(index >= 0 && index < inImgs.length)
      new KVImg[T](sc, inImgs(index), imgType, paddingVal)
    }
  }

  class PointRDD[T](dim: D3int,
                    pos: D3int,
                    elSize: D3float,
                    imageType: Int,
                    path: TypedPath,
                    baseImg: RDD[(D3int, T)])(implicit lm: ClassTag[T]) extends ImageRDD[T](dim,
    pos, elSize, imageType, path) {

    override type objType = T

    def pointMap[U](f: (D3int, T) => U, newType: Int)(implicit um: ClassTag[U]): ImageRDD[U] = {
      new PointRDD[U](dim, pos, elSize, newType, path.append(f.toString()),
        baseImg.map(ikv => (ikv._1, f(ikv._1, ikv._2))))
    }

    /**
     * A map by slice on all slices in the image
     *
     * @param f
     * @param newType
     * @tparam U
     * @return
     */
    override def sliceMap[U: ClassTag](f: (D3int, Array[T]) => Array[U],
                                       newType: Int): ImageRDD[U] = {
      val sliceSize = dim.x * dim.y
      val sliceDim = new D3int(getDim(), 1)
      val sliceAsList = baseImg.map {
        curElement: (D3int, T) => // create indices
          (curElement._1.z, ((curElement._1.y - pos.y) * dim.x + curElement._1.x - pos.x,
            curElement._2))
      }.groupBy(_._1).map {
        iVal =>
          val (curSlice, nList) = iVal
          val oKey = new D3int(getPos(), curSlice)
          val fullSlice = new Array[T](sliceSize)
          nList.foreach {
            indVal =>
              fullSlice(indVal._2._1) = indVal._2._2
          }
          (oKey,
            new TImgSlice[Array[U]](f(oKey, fullSlice), oKey, sliceDim))
      }
      new SliceRDD[U](dim, pos, elSize, newType, path.append(f.toString()), sliceAsList)

    }

    /* The function to collect all the key value pairs and return it as the appropriate array for a
 given slice
* @see tipl.formats.TImgRO#getPolyImage(int, int)
*/
    override def getPolyImage(sliceNumber: Int, asType: Int): AnyRef = {
      assert(TImgTools.isValidType(asType))
      val sliceSize = dim.x * dim.y

      val sliceAsList = baseImg.filter(_._1.z == (sliceNumber + pos.z)).map {
        curElement: (D3int, T) =>
          ((curElement._1.y - pos.y) * dim.x + curElement._1.x - pos.x, curElement._2)
      }.sortByKey(true)
      val tSlice = (0 until sliceSize).zip(new Array[T](sliceSize))
      // since particularly after thresholds many points are missing,
      // we need to add them back before making a slice out of the data
      val allPoints = baseImg.sparkContext.parallelize(tSlice)
      val missingPoints = allPoints.subtractByKey(sliceAsList)
      val fixedList = sliceAsList.union(missingPoints)
      // convert this array into the proper output format
      TImgTools.convertArrayType(fixedList.map(_._2).collect(), imageType, asType, getSigned(),
        getShortScaleFactor())
    }

    override def getRdd(): RDD[(D3int, objType)] = baseImg //.mapValues(_.asInstanceOf[AnyRef])

  }

  class SliceRDD[T](dim: D3int,
                    pos: D3int,
                    elSize: D3float,
                    imageType: Int,
                    path: TypedPath,
                    baseImg: RDD[(D3int, TImgSlice[Array[T]])])(implicit lm: ClassTag[T]) extends
    ImageRDD[T](dim, pos, elSize, imageType, path) {

    override type objType = TImgSlice[Array[T]]

    def pointMap[U](f: (D3int, T) => U, newType: Int)(implicit um: ClassTag[U]): ImageRDD[U] = {
      new SliceRDD[U](dim, pos, elSize, newType, path.append(f.toString()),
        baseImg.
          map {
            inKV =>
              val (pos, block) = inKV
              val slice = block.get()
              (pos, new TImgSlice[Array[U]](slice.map(cval => f(pos, cval)), block.getPos,
                block.getDim))
          })

    }

    /**
     * A map by slice on all slices in the image
     *
     * @param f
     * @param newType
     * @tparam U
     * @return
     */
    override def sliceMap[U: ClassTag](f: (D3int, Array[T]) => Array[U],
                                       newType: Int): ImageRDD[U] = {
      new SliceRDD[U](dim, pos, elSize, newType, path.append(f.toString()),
        baseImg.map {
          inKV =>
            val (pos, slice) = inKV
            (pos, new TImgSlice[Array[U]](f(pos, slice.get), slice.getPos,
              slice.getDim))
        })

    }

    override def getPolyImage(sliceNumber: Int, asType: Int): AnyRef = {
      if ((sliceNumber < 0) || (sliceNumber >= getDim.z)) throw new IllegalArgumentException(this
        .getSampleName + ": Slice requested (" + sliceNumber + ") exceeds image dimensions " +
        getDim)
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

    override def getRdd(): RDD[Tuple2[D3int, objType]] = baseImg

  }

}
