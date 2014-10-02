/**
 * A series of tools for processing with KVImg objects, including the required spread and collect functions which extend the RDD directly
 */
package tipl.spark

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import tipl.util.TImgTools
import tipl.formats.PureFImage
import tipl.util.D3int
import tipl.tools.BaseTIPLPluginIn
import tipl.formats.TImgRO
import tipl.util.TIPLOps.NeighborhoodOperation


/**
 * Some tools for the Key-value pair image to make working with them easier, writing code in java is just too much of a pain in the ass
 * @author mader
 *
 */
@serializable object KVImgOps {
  def createFromPureFun(sc: SparkContext, objToMirror: TImgTools.HasDimensions, inpf: PureFImage.PositionFunction): KVImg[Double] = {
    val objDim = objToMirror getDim
    val objPos = objToMirror getPos
    val xrng = objPos.x until (objPos.x + objDim.x)
    val yrng = objPos.y until (objPos.y + objDim.y)
    val zrng = objPos.z until (objPos.z + objDim.z)
    val wrappedImage = sc.parallelize(zrng). // parallelize over the slices
      flatMap(z => {
      for (x <- xrng; y <- yrng) // for each slice create each point in the image
      yield (new D3int(x, y, z),
        inpf.get(Array(x.doubleValue, y.doubleValue, z.doubleValue)))
    })

    KVImg.fromRDD[Double](objToMirror, TImgTools.IMAGETYPE_DOUBLE, wrappedImage)
  }

  /**
   * Go directly from a PositionFunction to a KVImg
   */
  implicit class RichPositionFunction(inpf: PureFImage.PositionFunction) {
    def asKVImg(sc: SparkContext, objToMirror: TImgTools.HasDimensions): KVImg[Double] = {
      createFromPureFun(sc, objToMirror, inpf)
    }
  }

  @serializable implicit class RichKvRDD[A <: Number](srd: RDD[(D3int, A)]) extends NeighborhoodOperation[(A, Boolean), A] {

    /**
     * A generic voxel spread function for a given window size and kernel
     *
     */
    val spread_voxels = (windSize: D3int, kernel: Option[BaseTIPLPluginIn.morphKernel]) => {
      (pvec: (D3int, A)) => {
        val pos = pvec._1

        val windx = (-windSize.x to windSize.x)
        val windy = (-windSize.y to windSize.y)
        val windz = (-windSize.z to windSize.z)
        for (
          x <- windx; y <- windy; z <- windz;
          if (kernel.map(_.inside(0, 0, pos.x, pos.x + x, pos.y, pos.y + y, pos.z, pos.z + z)).getOrElse(true))
        ) yield (new D3int(pos.x + x, pos.y + y, pos.z + z),
          (pvec._2, (x == 0 & y == 0 & z == 0)))
      }
    }

    def spreadPoints(windSize: D3int, kernel: Option[BaseTIPLPluginIn.morphKernel]): RDD[(D3int, Iterable[(A, Boolean)])] = {
      srd.flatMap(spread_voxels(windSize, kernel)).groupByKey
    }

    def blockOperation(windSize: D3int, kernel: Option[BaseTIPLPluginIn.morphKernel], mapFun: (Iterable[(A, Boolean)] => A)): RDD[(D3int, A)] = {
      val spread = srd.spreadPoints(windSize, kernel).collectPoints(mapFun)
      spread
    }

    // pixel level operations
    def >(threshVal: Double): RDD[(D3int, Boolean)] = {
      srd.map { pvec: (D3int, A) => (pvec._1, pvec._2.doubleValue > threshVal)}
    }

    def +(threshVal: Double): RDD[(D3int, Double)] = {
      srd.map { pvec: (D3int, A) => (pvec._1, pvec._2.doubleValue + threshVal)}
    }

    def *(threshVal: Double): RDD[(D3int, Double)] = {
      srd.map { pvec: (D3int, A) => (pvec._1, pvec._2.doubleValue * threshVal)}
    }

    def <(threshVal: Double): RDD[(D3int, Boolean)] = {
      srd.map { pvec: (D3int, A) => (pvec._1, pvec._2.doubleValue < threshVal)}
    }

    def ==(threshVal: Long): RDD[(D3int, Boolean)] = {
      srd.map { pvec: (D3int, A) => (pvec._1, pvec._2.longValue > threshVal)}
    }

    // image level operations
    /**
     * Convert two images to doubles and then perform a left outer join on them
     */
    def combineAsDouble[B <: Number](imgB: RDD[(D3int, B)]): RDD[(D3int, Iterable[Double])] = {
      val imgAdouble = srd.map { pvec: (D3int, A) => (pvec._1, pvec._2.doubleValue)}
      val imgBdouble = imgB.map { pvec: (D3int, B) => (pvec._1, pvec._2.doubleValue)}
      imgAdouble.union(imgBdouble).groupByKey
    }

    /**
     * Performs a reduce step on multiple images collapsed into position, list of point format
     */
    def collapseAsDouble[B <: Number](imgB: RDD[(D3int, B)], redFun: (Double, Double) => Double): RDD[(D3int, Double)] = {
      combineAsDouble(imgB).mapValues { pvec: Iterable[Double] => pvec.reduce(redFun)}
    }

    def +[B <: Number](imgB: RDD[(D3int, B)]): RDD[(D3int, Double)] = {
      collapseAsDouble(imgB, _ + _)
    }

    def *[B <: Number](imgB: RDD[(D3int, B)]): RDD[(D3int, Double)] = {
      collapseAsDouble(imgB, _ * _)
    }

  }

  /**
   * A class of a spread RDD image (after a flatMap/spread operation)
   */
  @serializable implicit class SpreadRDD[A <: Number](srd: RDD[(D3int, Iterable[(A, Boolean)])]) {
    def collectPoints(coFun: (Iterable[(A, Boolean)] => A)) = {
      srd.mapValues(coFun)
    }
  }

  @serializable implicit class RichKVImg[A <: Number](ip: KVImg[A]) {
    val srd = ip.getBaseImg

    def +[B <: Number](imgB: KVImg[B]): KVImg[Double] = {

      val outImg = srd + imgB.getBaseImg()
      val javaImg = outImg.mapValues {
        _.doubleValue
      }
      KVImg.fromRDD[Double](ip, TImgTools.IMAGETYPE_DOUBLE, javaImg)
    }

    def getInterface() = {

    }
  }

  /** create a KVImage with only points above a certain value **/
  def TImgROToKVThresh(sc: SparkContext, inImg: TImgRO, threshold: Double) = {
    val imgDim = inImg.getDim
    val imgPos = inImg.getPos
    val kvRdd = sc.parallelize(0 until imgDim.z).flatMap {
      cSlice =>
        val cSliceArr = inImg.getPolyImage(cSlice, TImgTools.IMAGETYPE_DOUBLE).asInstanceOf[Array[Double]]
        for {
          y <- 0 until imgDim.y;
          x <- 0 until imgDim.x;
          oVal = cSliceArr(y * imgDim.x + x);
          if oVal > threshold
        } yield (new D3int(imgPos.x + x, imgPos.y + y, imgPos.z + cSlice), oVal)
    }
    new KVImg(inImg, TImgTools.IMAGETYPE_DOUBLE, kvRdd)
  }

}