/**
 *
 */
package tipl.spark

import org.apache.spark.SparkContext._
import org.apache.spark.rdd.PairRDDFunctions._
import org.apache.spark.rdd.RDD
import tipl.util.ArgumentParser
import tipl.tools.BaseTIPLPluginIn
import scala.math.sqrt
import tipl.formats.TImgRO.CanExport
import tipl.util.TIPLGlobal
import tipl.formats.TImg
import tipl.util.TImgTools
import tipl.formats.TImgRO
import tipl.tools.BaseTIPLPluginIO
import tipl.tools.IVoronoiTransform
import tipl.util.D3int
import tipl.formats.PureFImage
import tipl.tests.TestPosFunctions
import tipl.spark.TypeMacros._
import tipl.spark.DTImgOps._
import scala.reflect.ClassTag
import org.apache.spark.api.java.JavaPairRDD

import tipl.util.TIPLOps._
import tipl.util.TImgBlock

/**
 * A spark based code to resize an image
 * @author mader
 *
 */
class SResize extends BaseTIPLPluginIO {
  var pOutDim = new D3int(-1, -1, -1)
  var pOutPos = new D3int(-1, -1, -1)
  var pFindEdge = false
  var pIsMask = false
  var pFillBlanks = false

  override def setParameter(p: ArgumentParser, cPrefix: String): ArgumentParser = {

    pFindEdge = p.getOptionBoolean(cPrefix + "find_edges", pFindEdge,
      "Find borders by scanning image")
    pOutDim = p.getOptionD3int(cPrefix + "dim", pOutDim,
      "Size / dimensions of output image")
    pOutPos = p.getOptionD3int(cPrefix + "pos", pOutPos,
      "Starting Position")
    pIsMask = p.getOptionBoolean(cPrefix + "ismask", pIsMask,
      "Is input image a mask")
    pFillBlanks = p.getOptionBoolean(cPrefix + "fillblanks", pFillBlanks,
      "Fill in blanks (only relevant for KVImg type)")

    println(getPluginName + "\tNew Range:" + pOutPos + " -- " + (pOutPos + pOutDim))
    p
  }

  override def getPluginName() = {
    "Resize:Spark"
  }

  override def execute(): Boolean = {
    print("Starting Plugin..." + getPluginName)
    outImg = SResize.applyResize(inImage, pOutDim, pOutPos, pFindEdge)
    true
  }

  var inImage: TImgRO = null
  var outImg: TImg = null
  var objDim: D3int = null
  lazy val partitioner = SparkGlobal.getPartitioner(objDim)

  /**
   * The first image is the
   */
  override def LoadImages(inImages: Array[TImgRO]) = {
    inImage = inImages(0)
    objDim = inImage.getDim

  }

  override def ExportImages(templateImage: TImgRO): Array[TImg] = {
    val tiEx = TImgTools.makeTImgExportable(templateImage)
    Array(outImg)
  }

}

object SResize {

  case class sstats(minx: Int, maxx: Int, miny: Int, maxy: Int, count: Int)

  def applyResize[A](inImg: TImgRO, outDim: D3int, outPos: D3int, findEdge: Boolean)(implicit aa: ClassTag[A]) = {

    val imClass = TImgTools.imageTypeToClass(inImg.getImageType)
    inImg match {
      case dImg: DTImg[_] if imClass == TImgTools.IMAGECLASS_LABEL => dtResize(dImg.asDTInt, outDim, outPos, findEdge)
      case dImg: DTImg[_] if imClass == TImgTools.IMAGECLASS_VALUE => dtResize(dImg.asDTDouble, outDim, outPos, findEdge)
      case dImg: DTImg[_] if imClass == TImgTools.IMAGECLASS_BINARY => dtResize(dImg.asDTBool, outDim, outPos, findEdge)
      case kvImg: KVImg[A] => kvResize(kvImg, outDim, outPos)
      case normImg: TImgRO if imClass == TImgTools.IMAGECLASS_LABEL =>
        val dnormImg = normImg.toDTLabels
        val resizeImg = dtResize(dnormImg, outDim, outPos, findEdge)
        TImgTools.ChangeImageType(resizeImg, normImg.getImageType())
      case normImg: TImgRO if imClass == TImgTools.IMAGECLASS_VALUE =>
        val dnormImg = normImg.toDTValues
        val resizeImg = dtResize(dnormImg, outDim, outPos, findEdge)
        TImgTools.ChangeImageType(resizeImg, normImg.getImageType())
      case normImg: TImgRO if imClass == TImgTools.IMAGECLASS_BINARY =>
        val dnormImg = normImg.toDTBinary
        val resizeImg = dtResize(dnormImg, outDim, outPos, findEdge)
        TImgTools.ChangeImageType(resizeImg, normImg.getImageType())
      case normImg: TImgRO if imClass == TImgTools.IMAGECLASS_OTHER => throw new IllegalArgumentException(" Image Type Other is not supported yet inside Resize:Spark :" + inImg.getImageType)
    }
  }

  /**
   * Resize code for the KVImg
   */
  def kvResize[A](aImg: KVImg[A], outDim: D3int, outPos: D3int)(implicit aa: ClassTag[A]) = {
    val finalPos = outPos + outDim
    val resImg = aImg.getBaseImg.filter {
      inVals =>
        val inPos = inVals._1
        (inPos.x >= outPos.x) && (inPos.x <= finalPos.x) &&
          (inPos.y >= outPos.y) && (inPos.y <= finalPos.y) &&
          (inPos.z >= outPos.z) && (inPos.z <= finalPos.z)
    }
    KVImg.fromRDD[A](
      TImgTools.SimpleDimensions(outDim, aImg.getElSize, outPos),
      aImg.getImageType, resImg)
  }
  /**
   * Resize a DTImg
   * @param ibasePos is the new upper corner position
   * @param ibaseDim is the new dimensions
   */
  def dtResize[B <: AnyVal](dImg: DTImg[Array[B]], ibaseDim: D3int, ibasePos: D3int, find_edges: Boolean)(implicit aa: ClassTag[B]) = {
    var basePos = ibasePos
    var baseDim = ibaseDim
    var finalPos = basePos + baseDim
    if (find_edges) {
      val imgObj = dImg.asDTBool.getBaseImg.rdd

      val sliceStats = imgObj.map(inpt => (inpt._1.z, inpt._2)). // get rid of the d3int
        mapValues {
          inBlock =>
            val ptArray = inBlock.get()
            val bPos = inBlock.getPos
            val bDim = inBlock.getDim
            var minx = bDim.x
            var miny = bDim.y
            var maxx = 0
            var maxy = 0
            var count = 0
            for (
              iy <- 0 until bDim.y;
              ix <- 0 until bDim.x;
              idx = iy * bDim.x + ix
              if ptArray(idx)
            ) {
              if (ix < minx) minx = ix
              if (ix > maxx) maxx = ix
              if (iy < miny) miny = iy
              if (iy > maxy) maxy = iy
              count += 1
            }
            sstats(minx + bPos.x, maxx + bPos.x, miny + bPos.y, maxy + bPos.y, count)
        }.filter(_._2.count > 0)
      basePos = new D3int(
        sliceStats.map(_._2.minx).min(),
        sliceStats.map(_._2.miny).min(),
        sliceStats.map(_._1).min())
      finalPos = new D3int(
        sliceStats.map(_._2.maxx).max() + 1,
        sliceStats.map(_._2.maxy).max() + 1,
        sliceStats.map(_._1).max() + 1)
      baseDim = finalPos - basePos
      println("Find Edges Successful: pos-" + basePos + " -- " + finalPos)
    }

    // remove the empty z slices
    val zFilter = dImg.getBaseImg.rdd.filter { inBlock =>
      val cPos = inBlock._1
      cPos.z >= basePos.z && cPos.z < finalPos.z
    }

    val outDim = new D3int(baseDim.x, baseDim.y, 1)
    val sliceLength = baseDim.x * baseDim.y
    val resImg = zFilter.map { inBlock =>

      val oldPos = inBlock._1

      val oldDim = inBlock._2.getDim
      val oldSlice = inBlock._2.get
      val outPos = new D3int(basePos.x, basePos.y, oldPos.z) // the slices get cropped by z stays the same value

      val outSlice = new Array[B](sliceLength)
      for (iy <- 0 until baseDim.y; ix <- 0 until baseDim.x) { // ix and iy are in the output slice
        val newInd = iy * baseDim.x + ix
        // absolute position coordinates
        val ax = ix + outPos.x
        val ay = iy + outPos.y
        // back to the original slice coordinates and index
        val ox = ax - oldPos.x
        val oy = ay - oldPos.y
        val oldInd = oy * oldDim.x + ox
        if (oldInd > 0 && oldInd < oldSlice.length) outSlice(newInd) = oldSlice(oldInd)
      }
      (outPos, new TImgBlock[Array[B]](outSlice, outPos, outDim))
    }
    // add the missing z slices
    val oldImgPos = dImg.getPos
    val missingSlices = zFilter.sparkContext.
      parallelize(basePos.z until finalPos.z, baseDim.z).
      map { z =>
        val outPos = new D3int(basePos.x, basePos.y, z)
        (outPos, outPos)
      }
    // combine all the slices with the resized old image and then merge them
    val combSlices = missingSlices.leftOuterJoin(resImg).mapValues {
      inVals =>
        inVals._2 match {
          // if the slice is present return it as it is
          case hasSlice: Some[TImgBlock[Array[B]]] => hasSlice.get
          // otherwise create a new empty slice
          case None => new TImgBlock[Array[B]](new Array[B](sliceLength), inVals._1, outDim)
        }
    }
    DTImg.WrapRDD[Array[B]](
      TImgTools.SimpleDimensions(baseDim, dImg.getElSize, basePos),
      JavaPairRDD.fromRDD(combSlices),
      dImg.getImageType())
  }

  val fillImage = (tempObj: TImgTools.HasDimensions, defValue: Double) => {
    val defFloat = new PureFImage.ConstantValue(defValue)
    KVImgOps.createFromPureFun(SparkGlobal.getContext("SResize"), tempObj, defFloat).toKVFloat
  }

  /**
   * Resize a DTImg without using the generic B tag
   * longer solution
   * @param basePos is the new upper corner position
   * @param baseDim is the new dimensions
   */
  def dtResizeWithoutGenerics[A](dImg: DTImg[A], baseDim: D3int, basePos: D3int)(implicit aa: ClassTag[A]) = {
    val finalPos = basePos + baseDim
    val imType = dImg.getImageType
    val zFilter = dImg.getBaseImg.rdd.filter { inBlock =>
      val cPos = inBlock._1
      cPos.z >= basePos.z && cPos.z <= finalPos.z
    }
    val resImg = zFilter.map { inBlock =>
      val oldPos = inBlock._1
      val oldDim = inBlock._2.getDim
      val oldSlice = inBlock._2.get
      val oldSliceLen = oldDim.x * oldDim.y
      val outPos = new D3int(basePos.x, basePos.y, oldPos.z)
      val outDim = new D3int(baseDim.x, baseDim.y, 1)

      val outSlice = TypeMacros.makeImgBlock(baseDim.x * baseDim.y, imType)
      for (iy <- 0 until baseDim.y; ix <- 0 until baseDim.x) { // ix and iy are in the output slice
        val newInd = iy * baseDim.x + ix
        // absolute position coordinates
        val ax = ix + outPos.x
        val ay = iy + outPos.y
        // back to the original slice coordinates and index
        val ox = ax - oldPos.x
        val oy = ay - oldPos.y
        val oldInd = oy * oldDim.x + ox
        if (oldInd >= 0 && oldInd < oldSliceLen) TypeMacros.arraySetter(outSlice, newInd, oldSlice, oldInd, imType)
      }
      (outPos, new TImgBlock[A](outSlice.asInstanceOf[A], outPos, outDim))
    }
    DTImg.WrapRDD[A](
      TImgTools.SimpleDimensions(baseDim, dImg.getElSize, basePos),
      JavaPairRDD.fromRDD(resImg),
      dImg.getImageType())
  }
}

object SResizeTest extends SKVoronoi {
  def main(args: Array[String]): Unit = {
    val p = SparkGlobal.activeParser(args)
    val imSize = p.getOptionInt("size", 50,
      "Size of the image to run the test with")
    val testImg = TestPosFunctions.wrapItAs(imSize,
      new TestPosFunctions.DotsFunction(), TImgTools.IMAGETYPE_INT)

    LoadImages(Array(testImg))

    setParameter(p, "")

    p.checkForInvalid()
    execute()

  }
}

