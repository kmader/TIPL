/**
 *
 */
package tipl.spark
import org.apache.spark.SparkContext._
import org.apache.spark.api.java.JavaPairRDD
import org.apache.spark.rdd.RDD
import scala.reflect.ClassTag
import tipl.util.TImgBlock
import tipl.util.D3int
import tipl.util.TIPLOps._
import tipl.tools.BaseTIPLPluginIn
import scala.util.Sorting.stableSort
/**
 * A collectiono of useful functions for DTImg classes to allow more complicated analyses
 * @author mader
 *
 */
object DTImgOps {


  def DTImgToKV(inImg: DTImg[_]) = {
    val imgType = inImg.getImageType
    inImg.getBaseImg.rdd.flatMap {
      cPoint =>
        val pos = cPoint._1
        val dim = cPoint._2.getDim
        val obj = cPoint._2.get
        val outArr = TypeMacros.castArr(obj, imgType)
        for {z <- 0 until dim.z;
             y <- 0 until dim.y;
             x <- 0 until dim.x
        }
        yield (new D3int(pos.x + x, pos.y + y, pos.z + z), outArr((z * dim.y + y) * dim.x + x))
    }
    //val onImg = TypeMacros.correctlyTypeDTImg(inImg)
  }

  def DTImgToKVStrict[T, V](inImg: DTImg[T]): RDD[(D3int, V)] = {
    DTImgToKV(inImg).mapValues {
      cValue => cValue.asInstanceOf[V]
    }
  }

  @serializable implicit class RichDtRDD[A](srd: RDD[(D3int, TImgBlock[A])]) extends
  NeighborhoodOperation[TImgBlock[A], TImgBlock[A]] {
    val blockShift = (offset: D3int) => {
      (p: (D3int, TImgBlock[A])) => {
        val nPos = p._1 + offset
        (nPos, new TImgBlock[A](p._2.getClone(), nPos, offset))
      }

    }

    def spreadSlices(windSize: D3int): (RDD[(D3int, Iterable[TImgBlock[A]])]) = {
      var curOut = srd.mapValues(p => Iterable(p))

      for (curOffset <- 1 to windSize.z) {
        val upSet = new D3int(0, 0, +curOffset)
        val up1 = srd.map(blockShift(upSet))
        val downSet = new D3int(0, 0, -curOffset)
        val down1 = srd.map(blockShift(downSet))

        val tempOut = curOut.join(up1.join(down1))
        curOut = tempOut.mapValues { p => p._1 ++ Iterable(p._2._1, p._2._2)}
      }
      curOut

    }

    def blockOperation(windSize: D3int, kernel: Option[BaseTIPLPluginIn.morphKernel], mapFun: (Iterable[TImgBlock[A]] => TImgBlock[A])): RDD[(D3int, TImgBlock[A])] = {
      val spread = srd.spreadSlices(windSize).collectSlices(windSize, kernel, mapFun)
      spread
    }


  }

  /**
   * A class of a spread RDD image (after a flatMap/spread operation)
   */
  @serializable implicit class SpreadRDD[A](srd: RDD[(D3int, Iterable[TImgBlock[A]])]) {
    def collectSlices(windSize: D3int, kernel: Option[BaseTIPLPluginIn.morphKernel], coFun: (Iterable[TImgBlock[A]] => TImgBlock[A])) = {
      srd.mapValues(coFun)
    }
  }

  @serializable implicit class RichDTImg[A](ip: DTImg[A]) {

    val srd = ip.baseImg.rdd

    /** a much simpler version of spreadSlices taking advantage of Scala's language features
      *
      */
  
    def spreadSlices(windSize: D3int) = {
      srd.spreadSlices(windSize)
    }
  }

  

  def fromKVImg[T](inImg: KVImg[T])(implicit T: ClassTag[T]): DTImg[Array[T]] = {
    val dim = inImg.getDim
    val pos = inImg.getPos
    val elSize = inImg.getElSize
    val sliceSize = dim.x * dim.y
    var rddObj = inImg.getBaseImg().map { posVal => ((posVal._1.z, (posVal._1.y - pos.y) * dim.x + posVal._1.x - pos.x), posVal._2)}
    if (inImg.getBaseImg.count.toInt != dim.prod) {
      // fill in holes since voxels are missing
      val allPix = rddObj.sparkContext.parallelize(0 until dim.z).flatMap {
        cPoint =>
          val emptySlice = new Array[T](sliceSize)
          (Array.fill(sliceSize)(cPoint).zip(0 until sliceSize).zip(emptySlice))
      }
      val missingPixels = allPix.subtractByKey(rddObj)
      rddObj = rddObj.union(missingPixels)
    }
    val slices = rddObj.map { posVal => (new D3int(pos.x, pos.y, pos.z + posVal._1._1), (posVal._1._2, posVal._2))}.groupByKey
    val sslices = slices.map {
      imgVec =>
        val sList = stableSort(imgVec._2.toList, (e1: (Int, T), e2: (Int, T)) => e1._1 < e2._1).map {
          _._2
        }
        (imgVec._1, new TImgBlock[Array[T]](sList, imgVec._1, dim))
    }


    DTImg.WrapRDD[Array[T]](inImg, JavaPairRDD.fromRDD(sslices), inImg.getImageType());

  }
}

