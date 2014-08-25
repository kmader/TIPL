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
	var pOutDim = new D3int(-1,-1,-1)
	var pOutPos = new D3int(-1,-1,-1)
	var pFindEdge = false
	var pIsMask = false
	var pFillBlanks = false

  override def setParameter(p: ArgumentParser, cPrefix: String): ArgumentParser = {
    
  	pFindEdge = p.getOptionBoolean(cPrefix + "find_edges",pFindEdge,
				"Find borders by scanning image")
		pOutDim = p.getOptionD3int(cPrefix + "dim", pOutDim,
				"Size / dimensions of output image")
		pOutPos = p.getOptionD3int(cPrefix + "pos", pOutPos,
				"Starting Position")
		pIsMask = p.getOptionBoolean(cPrefix + "ismask",pIsMask,
				"Is input image a mask")
	    pFillBlanks =  p.getOptionBoolean(cPrefix + "fillblanks",pFillBlanks,
				"Fill in blanks (only relevant for KVImg type)")
				
		println(getPluginName+"\tNew Range:"+pOutPos+" -- "+(pOutPos+pOutDim))
    return p
  }

  override def getPluginName() = {
    "Resize:Spark"
  }

  override def execute(): Boolean = {
    print("Starting Plugin..." + getPluginName);
    outImg = SResize.applyResize(inImage, pOutDim, pOutPos, pFindEdge)
    true
  }
  
  var inImage: TImgRO = null
  var outImg: TImg = null
  var objDim: D3int = null
  lazy val partitioner = SparkGlobal.getPartitioner(objDim);

  /**
   * The first image is the
   */
  override def LoadImages(inImages: Array[TImgRO]) = {
    inImage = inImages(0)
    objDim = inImage.getDim
    
  }

  override def ExportImages(templateImage: TImgRO): Array[TImg] = {
    val tiEx = TImgTools.makeTImgExportable(templateImage)
    return Array(outImg)
  }

}

object SResize {
  def applyResize[A](inImg: TImgRO, outDim: D3int, outPos: D3int, findEdge: Boolean)(implicit aa: ClassTag[A]) = {
    inImg match {
      case dImg: DTImg[Array[A]] => dtResize(dImg,outDim,outPos)
      case kvImg: KVImg[A] => kvResize(kvImg,outDim,outPos)
      case normImg: TImgRO if (TImgTools.imageTypeToClass(inImg.getImageType)==TImgTools.IMAGECLASS_LABEL) => 
        val dnormImg = DTImg.ConvertTImg[Array[Long]](SparkGlobal.getContext("SResize"), normImg, TImgTools.IMAGETYPE_LONG)
        val resizeImg = dtResize(dnormImg,outDim,outPos)
        TImgTools.ChangeImageType(resizeImg,normImg.getImageType())
     case normImg: TImgRO if (TImgTools.imageTypeToClass(inImg.getImageType)==TImgTools.IMAGECLASS_VALUE) => 
        val dnormImg = DTImg.ConvertTImg[Array[Double]](SparkGlobal.getContext("SResize"), normImg, TImgTools.IMAGETYPE_DOUBLE)
        val resizeImg = dtResize(dnormImg,outDim,outPos)
        TImgTools.ChangeImageType(resizeImg,normImg.getImageType())
     case normImg: TImgRO if (TImgTools.imageTypeToClass(inImg.getImageType)==TImgTools.IMAGECLASS_BINARY) => 
        val dnormImg = DTImg.ConvertTImg[Array[Boolean]](SparkGlobal.getContext("SResize"), normImg, TImgTools.IMAGETYPE_BOOL)
        val resizeImg = dtResize(dnormImg,outDim,outPos)
        TImgTools.ChangeImageType(resizeImg,normImg.getImageType())
    }
  }
  /**
   * Resize code for the KVImg
   */
  def kvResize[A](aImg: KVImg[A], outDim: D3int, outPos: D3int)(implicit aa: ClassTag[A]) = {
    val finalPos = outPos+outDim
        val resImg = aImg.getBaseImg.filter{
          inVals =>
            val inPos = inVals._1
            ((inPos.x>=outPos.x) && (inPos.x<=finalPos.x) &&
                (inPos.y>=outPos.y) && (inPos.y<=finalPos.y) &&
                (inPos.z>=outPos.z) && (inPos.z<=finalPos.z))
        }
        KVImg.fromRDD[A](
            TImgTools.SimpleDimensions(outDim,aImg.getElSize, outPos), 
            aImg.getImageType, resImg)
  }
     /**
     * Resize a DTImg
     * @param basePos is the new upper corner position
     * @param baseDim is the new dimensions
     */
  def dtResize[B <: Any](dImg: DTImg[Array[B]],baseDim: D3int, basePos: D3int)(implicit aa: ClassTag[B]) = {

    val finalPos = basePos+baseDim
    // remove the empty z slices
      val zFilter = dImg.getBaseImg.rdd.filter{inBlock => 
        val cPos = inBlock._1
        (cPos.z>=basePos.z && cPos.z<finalPos.z)
      }
    
    val outDim = new D3int(baseDim.x,baseDim.y,1)
    val sliceLength = baseDim.x*baseDim.y
      val resImg = zFilter.map{inBlock =>
        val oldPos = inBlock._1
        val oldDim = inBlock._2.getDim
        val oldSlice = inBlock._2.get
        val outPos = new D3int(basePos.x,basePos.y,oldPos.z)
        
        val outSlice = new Array[B](sliceLength)
        for(iy <- 0 until baseDim.y; ix <- 0 until baseDim.x) { // ix and iy are in the output slice
          val newInd = iy*baseDim.x+ix
          // absolute position coordinates
          val ax = ix+outPos.x
          val ay = iy+outPos.y
          // back to the original slice coordinates and index
          val ox = ax - oldPos.x
          val oy = ay - oldPos.y
          val oldInd = oy*oldDim.x+ox
          if(oldInd>0 && oldInd<oldSlice.length) outSlice(newInd)=oldSlice(oldInd)
        }
        (outPos,new TImgBlock[Array[B]](outSlice,outPos,outDim))
      }
    // add the missing z slices
    val oldImgPos = dImg.getPos
    val missingSlices = zFilter.sparkContext.
    parallelize(basePos.z until finalPos.z, baseDim.z).
    map{z => 
      val outPos = new D3int(basePos.x,basePos.y,z)
      (outPos,outPos)}
    val combSlices = missingSlices.leftOuterJoin(resImg).mapValues{
      inVals =>
        inVals._2 match {
          // if the slice is present return it as it is
          case hasSlice: Some[TImgBlock[Array[B]]] => hasSlice.get
          // otherwise create a new empty slice
          case None => new TImgBlock[Array[B]](new Array[B](sliceLength),inVals._1,outDim)
        }
    }
    DTImg.WrapRDD[Array[B]](
            TImgTools.SimpleDimensions(baseDim,dImg.getElSize, basePos),
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
  def dtResizeWithoutGenerics[A](dImg: DTImg[A],baseDim: D3int, basePos: D3int)(implicit aa: ClassTag[A]) = {
    val finalPos = basePos+baseDim
    val imType = dImg.getImageType
      val zFilter = dImg.getBaseImg.rdd.filter{inBlock => 
        val cPos = inBlock._1
        (cPos.z>=basePos.z && cPos.z<=finalPos.z)
      }
      val resImg = zFilter.map{inBlock =>
        val oldPos = inBlock._1
        val oldDim = inBlock._2.getDim
        val oldSlice = inBlock._2.get
        val oldSliceLen = oldDim.x*oldDim.y
        val outPos = new D3int(basePos.x,basePos.y,oldPos.z)
        val outDim = new D3int(baseDim.x,baseDim.y,1)
        
        val outSlice = TypeMacros.makeImgBlock(baseDim.x*baseDim.y, imType)
        for(iy <- 0 until baseDim.y; ix <- 0 until baseDim.x) { // ix and iy are in the output slice
          val newInd = iy*baseDim.x+ix
          // absolute position coordinates
          val ax = ix+outPos.x
          val ay = iy+outPos.y
          // back to the original slice coordinates and index
          val ox = ax - oldPos.x
          val oy = ay - oldPos.y
          val oldInd = oy*oldDim.x+ox
          if(oldInd>=0 && oldInd<oldSliceLen) TypeMacros.arraySetter(outSlice,newInd,oldSlice,oldInd,imType)
        }
        (outPos,new TImgBlock[A](outSlice.asInstanceOf[A],outPos,outDim))
      }
        DTImg.WrapRDD[A](
            TImgTools.SimpleDimensions(baseDim,dImg.getElSize, basePos),
            JavaPairRDD.fromRDD(resImg),
            dImg.getImageType())
  }
}

object SResizeTest extends SKVoronoi {
  def main(args: Array[String]): Unit = {
    val p = SparkGlobal.activeParser(args)
    val imSize = p.getOptionInt("size", 50,
      "Size of the image to run the test with");
    val testImg = TestPosFunctions.wrapItAs(imSize,
      new TestPosFunctions.DotsFunction(), TImgTools.IMAGETYPE_INT);

    LoadImages(Array(testImg))

    setParameter(p, "")

    p.checkForInvalid()
    execute();

  }
}

