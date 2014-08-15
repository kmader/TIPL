/**
 *
 */
package tipl.spark

import tipl.util.ArgumentParser
import tipl.tools.BaseTIPLPluginIn
import tipl.tools.BaseTIPLPluginIO
import tipl.tools.GrayAnalysis
import tipl.tests.TestPosFunctions
import tipl.util.TIPLPluginManager
import tipl.settings.FilterSettings
import tipl.util.TImgTools
import tipl.util.D3int
import scala.collection.JavaConversions._
import tipl.formats.TImgRO
import tipl.tools.GrayVoxels
import tipl.util.ITIPLPlugin
import tipl.spark.TypeMacros._
import tipl.formats.TImg
import org.apache.spark.rdd.RDD
import tipl.util.TImgBlock
import tipl.settings.FilterSettings.filterGenerator
import spark.images.ImageTools
import org.apache.spark.SparkContext._
import org.apache.spark.api.java.JavaPairRDD

/**
 * A spark based code to perform filters similarly to the code provided VFilterScale
 * @author mader
 *
 */
@serializable class SFilterScale extends BaseTIPLPluginIO with FilterSettings.HasFilterSettings {

  private var filtSettings = new FilterSettings
  override def setFilterSettings(in: FilterSettings) = {filtSettings=in}
  override def getFilterSettings() = filtSettings
  
  override def setParameter(p: ArgumentParser, prefix: String): ArgumentParser = {
    return filtSettings.setParameter(p,prefix)
  }



  override def getPluginName() = {
    "FilterScale:Spark"
  }


 

  override def execute(): Boolean = {
    print("Starting Plugin..." + getPluginName);
    
    val sfg: filterGenerator = filtSettings.scalingFilterGenerator
    val up = filtSettings.upfactor
    val dn = filtSettings.downfactor
    
    val imRdd: RDD[(D3int,TImgBlock[Array[Double]])] = inputImage.getBaseImg.rdd
    outputImage = SFilterScale.partBasedFilterMap(imRdd,up,sfg)
    true
  }

  var inputImage: DTImg[Array[Double]] = null
  var outputImage: RDD[(D3int,TImgBlock[Array[Double]])]  = null

  override def LoadImages(inImages: Array[TImgRO]) = {
    inputImage = inImages(0).toDTValues
  }
  
  override def ExportImages(templateIm: TImgRO): Array[TImg] = {
    val jpr =  JavaPairRDD.fromRDD(outputImage)
    Array(DTImg.WrapRDD[Array[Double]](templateIm,jpr, TImgTools.IMAGETYPE_DOUBLE))
  }

  override def getInfo(request: String): Object = {
    val output = null
    if (output == null) return super.getInfo(request);
    else return output;
  }


}

object SFilterScale {
  case class partialFilter[T](newpos: D3int, block: TImgBlock[T],finished: Boolean)
  /**
   * A partition based filter command that should be more
   */
  def partBasedFilterMap(inImg: RDD[(D3int,TImgBlock[Array[Double]])],
      windSize: D3int, filt: filterGenerator) = {
      val neededSlices = 2*windSize.z+1
	  val outImg = inImg.mapPartitions{
	    cPartition =>
	      val allSlices = cPartition.flatMap(ImageTools.spread_blocks(_, windSize.z)).toList.groupBy(_._1)
	      // operate on each slice (if possible)
	      allSlices.flatMap{
	        cSlices =>
	          val pos = cSlices._1
	          val sliceList = cSlices._2.toList
	          val finished = (sliceList.length == neededSlices)
	          val outVal = if (finished) {		
	            List(partBasedFilterReduce(sliceList,windSize,filt))
	          } else {
	            for(curSlice <- sliceList) yield (pos,partialFilter[Array[Double]](cSlices._1,curSlice._2,false))
	          }
	          outVal.toIterator
	      }.toIterator
	      
	  }
      val betweenSlices = outImg.filter(!_._2.finished).
      mapValues(_.block). // remove the partialFilter status
      map(inPair => (inPair._1,(inPair._1,inPair._2))). // format for the reduce command
      groupByKey.map{
        inPairs =>
          val cSlices = inPairs._2
          partBasedFilterReduce(cSlices.toList,windSize,filt)
      }
      (outImg.filter(_._2.finished) ++ betweenSlices).mapValues(_.block)
  }
  
  def partBasedFilterReduce(inSpreadImg: List[(D3int,TImgBlock[Array[Double]])],
      windSize: D3int, filt: filterGenerator): (D3int,partialFilter[Array[Double]])
      = {
    val templateImg = inSpreadImg.head
    val sliceLen = templateImg._2.get.length
    // process blocks
	val kernelList = (for(i<- 0 until sliceLen) yield filt.make()).toArray
	//TODO hard code it for now
	val mKernel = BaseTIPLPluginIn.fullKernel
	for (curSlice<-inSpreadImg) {
				val cPos = curSlice._1
                val curBlock = curSlice._2
                val curArr = curBlock.get
                val curDim = curBlock.getDim
                val blockPos = curBlock.getPos
                val zp = blockPos.z
                    for (yp <- 0 until curDim.y) {
                      for (xp <- 0 until curDim.x) {
                            val off =  (yp)* curDim.x + xp;
                            val curKernel = kernelList(off);
                            for (cPos <- BaseTIPLPluginIn.getScanPositions(mKernel, new D3int(xp, yp, zp), 
                                curBlock.getOffset(), off, curDim, windSize)) {
                                curKernel.addpt(xp, cPos.x, yp, cPos.y, zp, cPos.z,
                                        curArr(cPos.offset))
                            }
                        }
                    }
            }
	val outSlice = kernelList.map{_.value()}.toArray
	val inBlock = templateImg._2
	val outBlock = new TImgBlock[Array[Double]](outSlice,inBlock.getPos,inBlock.getDim)
	
	(templateImg._1,partialFilter[Array[Double]](templateImg._1,outBlock,false))
  }
}

object SFilterTest extends SFilterScale {

  
  def main(args: Array[String]): Unit = {
    val testImg = TestPosFunctions.wrapItAs(10,
      new TestPosFunctions.DiagonalPlaneAndDotsFunction(), TImgTools.IMAGETYPE_INT);


    LoadImages(Array(testImg))
    setParameter("-csvname=" + true + "_testing.csv");
    execute();

  }
}
