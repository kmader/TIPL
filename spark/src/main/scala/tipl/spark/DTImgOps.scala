/**
 *
 */
package tipl.spark

import org.apache.spark.SparkContext._
import org.apache.spark.api.java.JavaPairRDD
import org.apache.spark.rdd.RDD
import tipl.formats.TImgRO
import tipl.tools.BaseTIPLPluginIn
import tipl.util.TIPLOps._
import tipl.util.{D3float, D3int, TImgSlice, TImgTools}
import tipl.util.TImgTools.HasDimensions

import scala.reflect.ClassTag
import scala.util.Sorting.stableSort
import scala.{specialized => spec}

/**
 * A collection of useful functions for DTImg classes to allow more complicated analyses
 * @author mader
 *
 */
object DTImgOps {
  private[spark] def DTImgToKVRDD(inImg: DTImg[_]) = {
    val imgType = inImg.getImageType
    DTrddToKVrdd(inImg.getBaseImg.rdd,imgType)
  }
  private[spark] def DTrddToKVrdd[V](inImg: RDD[(D3int,TImgSlice[V])],imgType: Int) = {
    inImg.flatMap {
      cPoint =>
        val pos = cPoint._1
        val dim = new D3int(cPoint._2.getDim,1)
        val obj = cPoint._2.get
        val outArr = TypeMacros.castArr(obj, imgType)
        for {z <- 0 until dim.z
             y <- 0 until dim.y
             x <- 0 until dim.x
        }
        yield (new D3int(pos.x + x, pos.y + y, pos.z + z), outArr((z * dim.y + y) * dim.x + x))
    }

    //val onImg = TypeMacros.correctlyTypeDTImg(inImg)
  }

  private[spark] def DTrddToKVrdd[@spec(Boolean, Byte, Short, Int, Long, Float,
    Double) T](inImg: RDD[(D3int,TImgSlice[Array[T]])]): RDD[(D3int, T)] = {
    inImg.flatMap {
      cPoint =>
        val pos = cPoint._1
        val dim = new D3int(cPoint._2.getDim,1)
        val obj = cPoint._2.get
        val outArr = obj
        for {z <- 0 until dim.z
             y <- 0 until dim.y
             x <- 0 until dim.x
        }
        yield (new D3int(pos.x + x, pos.y + y, pos.z + z), outArr((z * dim.y + y) * dim.x + x))
    }

    //val onImg = TypeMacros.correctlyTypeDTImg(inImg)
  }


  def DTImgToKV(inImg: DTImg[_]) = {
    new KVImg(inImg, inImg.getImageType, DTImgToKVRDD(inImg))
  }

  def DTImgToKVStrict[T, V](inImg: DTImg[T])(implicit V: ClassTag[V]): KVImg[V] = {
    val outImg = DTImgToKVRDD(inImg).mapValues {
      cValue => cValue.asInstanceOf[V]
    }
    new KVImg[V](inImg, inImg.getImageType, outImg)
  }


  implicit class RichDtRDD[A](srd: RDD[(D3int, TImgSlice[A])]) extends
  NeighborhoodOperation[TImgSlice[A], TImgSlice[A]] {

    private[RichDtRDD] def blockShift(offset: D3int) = {
      (p: (D3int, TImgSlice[A])) => {
        val nPos = p._1 + offset
        (nPos, new TImgSlice[A](p._2.getClone(), nPos, offset))
      }
    }

    /**
     * Wrap a RDD as a DTImg
     * @param elSize the size of the voxels
     */
    def wrap(elSize: D3float = new D3float(1.0)): DTImg[A] = {
      val fele = srd.first
      val sPos = fele._1
      val odim = fele._2.getDim
      val ndim = new D3int(odim, srd.count.asInstanceOf[Int])
      val baseImg = TImgTools.SimpleDimensions(ndim, elSize, sPos)
      val imgtype = TImgTools.identifySliceType(fele._2.get)
      wrap(baseImg, imgtype)

    }

    /**
     * Wrap a RDD as a DTImg
     * @param baseObj the image to mirror
     * @param imtype the type of the image
     */
    def wrap(baseObj: HasDimensions, imtype: Int): DTImg[A] = {
      DTImg.WrapRDD[A](baseObj, JavaPairRDD.fromRDD(srd), imtype)
    }

    def spreadSlices(windSize: D3int): (RDD[(D3int, Iterable[TImgSlice[A]])]) = {
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

    /**
     * simple apply a function to a slice
     */
    def apply[B](mapFun: (Double => B))(implicit B: ClassTag[B]) = {
      srd.mapValues { inBlock =>
        val outArray = (for (cval <- inBlock.getAsDouble()) yield mapFun(cval)).toArray
        new TImgSlice[Array[B]](outArray, inBlock)
      }
    }

    def blockOperation(windSize: D3int, kernel: Option[BaseTIPLPluginIn.morphKernel],
                       mapFun: (Iterable[TImgSlice[A]] => TImgSlice[A])): RDD[(D3int,
      TImgSlice[A])] = {
      val spread = srd.spreadSlices(windSize).collectSlices(windSize, kernel, mapFun)
      spread
    }

  }


  /**
   * A class of a spread RDD image (after a flatMap/spread operation)
   */
  implicit class SpreadRDD[A](srd: RDD[(D3int, Iterable[TImgSlice[A]])]) extends Serializable {
    def collectSlices(windSize: D3int, kernel: Option[BaseTIPLPluginIn.morphKernel],
                      coFun: (Iterable[TImgSlice[A]] => TImgSlice[A])) = {
      srd.mapValues(coFun)
    }
  }


  implicit class RichDTImg[A](ip: DTImg[A]) extends Serializable {

    val srd = ip.getBaseImg.rdd

    /** a much simpler version of spreadSlices taking advantage of Scala's language features
      *
      */
    def spreadSlices(windSize: D3int) = srd.spreadSlices(windSize)

    def sizeCheck(inImg: HasDimensions) = {
      if (!TImgTools.CheckSizes2(ip, inImg)) throw new IllegalArgumentException("Image sizes must" +
        " match! d1:" + ip.getDim + ", p1:" + ip.getPos + "\t d2:" + inImg.getDim + " p2:" +
        inImg.getPos)
    }

    /**
     * generic implementation of an operator
     * @param inImg to combine with the ip image forming the classes
     * @param combFunc function to use to combine the two images
     *
     */
    def combineImages[B](inImg: DTImg[B], combFunc: ((Double, Double) => Double)):
    DTImg[Array[Double]] = {
      sizeCheck(inImg)
      val aImg = ip.getBaseImg.rdd
      val bImg = inImg.getBaseImg.rdd
      val jImg = aImg.join(bImg).mapValues {
        inImages =>
          val aArr = inImages._1.getAsDouble()
          val bArr = inImages._2.getAsDouble()
          val outArray = new Array[Double](aArr.length)
          for (i <- 0 until aArr.length) outArray(i) = combFunc(aArr(i), bArr(i))
          new TImgSlice[Array[Double]](outArray, inImages._1) // inherit from first block
      }
      DTImg.WrapRDD[Array[Double]](ip, JavaPairRDD.fromRDD(jImg), TImgTools.IMAGETYPE_DOUBLE)
    }

    def +[B](inImg: DTImg[B]) = combineImages(inImg, _ + _)

    def -[B](inImg: DTImg[B]) = combineImages(inImg, _ - _)

    def *[B](inImg: DTImg[B]) = combineImages(inImg, _ * _)

    def /[B](inImg: DTImg[B]) = combineImages(inImg, _ / _)

  }


  /**
   * A smarter conversion function
   */
  def fromTImgRO(inImg: TImgRO) = {
    val imClass = TImgTools.imageTypeToClass(inImg.getImageType)
    inImg match {
      case dImg: DTImg[_] if imClass == TImgTools.IMAGECLASS_LABEL => dImg.asDTLong
      case dImg: DTImg[_] if imClass == TImgTools.IMAGECLASS_VALUE => dImg.asDTDouble
      case dImg: DTImg[_] if imClass == TImgTools.IMAGECLASS_BINARY => dImg.asDTBool
      case normImg: TImgRO if imClass == TImgTools.IMAGECLASS_LABEL =>
        DTImg.ConvertTImg[Array[Long]](SparkGlobal.getContext("DTImgOps"), normImg,
          TImgTools.IMAGETYPE_LONG)
      case normImg: TImgRO if imClass == TImgTools.IMAGECLASS_VALUE =>
        DTImg.ConvertTImg[Array[Double]](SparkGlobal.getContext("DTImgOps"), normImg,
          TImgTools.IMAGETYPE_DOUBLE)
      case normImg: TImgRO if imClass == TImgTools.IMAGECLASS_BINARY =>
        DTImg.ConvertTImg[Array[Boolean]](SparkGlobal.getContext("DTImgOps"), normImg,
          TImgTools.IMAGETYPE_BOOL)
      case normImg: TImgRO if imClass == TImgTools.IMAGECLASS_OTHER => throw new
          IllegalArgumentException(" Image Type Other is not supported yet inside Resize:Spark :"
            + inImg.getImageType)
    }
  }

  def fromKVImg[T](inImg: KVImg[T])(implicit T: ClassTag[T]): DTImg[Array[T]] = {
    val dim = inImg.getDim
    val pos = inImg.getPos
    val elSize = inImg.getElSize
    val sliceSize = dim.x * dim.y
    var rddObj = inImg.getBaseImg().map { posVal => ((posVal._1.z, (posVal._1.y - pos.y) * dim.x
      + posVal._1.x - pos.x), posVal._2)}
    if (inImg.getBaseImg.count.toInt != dim.prod) {
      // fill in holes since voxels are missing
      val allPix = rddObj.sparkContext.parallelize(0 until dim.z).flatMap {
        cPoint =>
          val emptySlice = new Array[T](sliceSize)
          Array.fill(sliceSize)(cPoint).zip(0 until sliceSize).zip(emptySlice)
      }
      val missingPixels = allPix.subtractByKey(rddObj)
      rddObj = rddObj.union(missingPixels)
    }
    val slices = rddObj.map { posVal => (new D3int(pos.x, pos.y, pos.z + posVal._1._1),
      (posVal._1._2, posVal._2))}.groupByKey
    val sslices = slices.map {
      imgVec =>
        val sList = stableSort(imgVec._2.toList, (e1: (Int, T), e2: (Int,
          T)) => e1._1 < e2._1).map {
          _._2
        }
        (imgVec._1, new TImgSlice[Array[T]](sList, imgVec._1, dim))
    }


    DTImg.WrapRDD[Array[T]](inImg, JavaPairRDD.fromRDD(sslices), inImg.getImageType())

  }

}


