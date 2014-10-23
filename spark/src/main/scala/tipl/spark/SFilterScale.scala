/**
 *
 */
package tipl.spark

import org.apache.spark.SparkContext._
import org.apache.spark.api.java.JavaPairRDD
import org.apache.spark.rdd.RDD
import spark.images.ImageTools
import tipl.formats.{TImg, TImgRO}
import tipl.settings.FilterSettings
import tipl.settings.FilterSettings.filterGenerator
import tipl.spark.TypeMacros._
import tipl.tests.TestPosFunctions
import tipl.tools.{BaseTIPLPluginIO, BaseTIPLPluginIn}
import tipl.util.{ArgumentParser, D3int, TImgSlice, TImgTools}

import scala.collection.GenTraversableOnce
import scala.collection.JavaConversions._

/**
 * A spark based code to perform filters similarly to the code provided VFilterScale
 * @author mader
 *
 */
class SFilterScale extends BaseTIPLPluginIO with FilterSettings.HasFilterSettings {

  private var filtSettings = new FilterSettings

  override def setFilterSettings(in: FilterSettings) = {
    filtSettings = in
  }

  override def getFilterSettings() = filtSettings

  override def setParameter(p: ArgumentParser, prefix: String): ArgumentParser = {
    filtSettings.setParameter(p, prefix)
  }

  override def getPluginName() = {
    "FilterScale:Spark"
  }

  override def execute(): Boolean = {
    print("Starting Plugin..." + getPluginName)

    val sfg: filterGenerator = filtSettings.scalingFilterGenerator
    val up = filtSettings.upfactor
    val dn = filtSettings.downfactor
    val insize = inputImage.getDim

    outputSize = new D3int((insize.x * up.x) / dn.x, (insize.y * up.y) / dn.y,
      (insize.z * up.z) / dn.z)
    val imRdd: RDD[(D3int, TImgSlice[Array[Double]])] = inputImage.getBaseImg.rdd

    outputImage = SFilterScale.partBasedFilterMap(imRdd, up,
      ImageTools.spread_blocks_gen(_, up, dn), sfg)
    true
  }

  var inputImage: DTImg[Array[Double]] = null
  var outputImage: RDD[(D3int, TImgSlice[Array[Double]])] = null
  var outputSize: D3int = null

  override def LoadImages(inImages: Array[TImgRO]) = {
    inputImage = inImages(0).toDTValues
  }

  override def ExportImages(templateIm: TImgRO): Array[TImg] = {
    Array(ExportDTImg(templateIm))
  }

  def ExportDTImg(templateIm: TImgRO): DTImg[Array[Double]] = {
    val jpr = JavaPairRDD.fromRDD(outputImage)
    val outImage = DTImg.WrapRDD[Array[Double]](templateIm, jpr, TImgTools.IMAGETYPE_DOUBLE)
    outImage.setDim(outputSize)
    outImage
  }

  override def getInfo(request: String): Object = {
    request.toLowerCase() match {
      case "outtype" => new java.lang.Integer(filtSettings.oimageType);
      case _ => super.getInfo(request);
    }
  }

}


object SFilterScale {


  case class partialFilter[T](newpos: D3int, block: TImgSlice[T], finished: Boolean)


  /**
   * a basic spread function
   */
  val simple_spread = (windSize: D3int) =>
    (pvec: (D3int, TImgSlice[Array[Double]])) =>
      ImageTools.spread_blocks(pvec, windSize.z)

  /**
   * A partition based filter command that should be more
   */
  def partBasedFilterMap(inImg: RDD[(D3int, TImgSlice[Array[Double]])],
                         upWindSize: D3int,
                         spread_fun: (((D3int, TImgSlice[Array[Double]])) =>
                           GenTraversableOnce[(D3int, TImgSlice[Array[Double]])]),
                         filt: filterGenerator) = {
    val neededSlices = 2 * upWindSize.z + 1
    val outImg = inImg.mapPartitions {
      cPartition =>
        val allSlices = cPartition.flatMap(spread_fun).toList.groupBy(_._1)
        // operate on each slice (if possible)
        allSlices.flatMap {
          cSlices =>
            val pos = cSlices._1
            val sliceList = cSlices._2.toList
            val finished = sliceList.length == neededSlices
            val outVal = if (finished) {
              List(partBasedFilterReduce(sliceList, upWindSize, filt))
            }
            else {
              for (curSlice <- sliceList)
              yield (pos, partialFilter[Array[Double]](cSlices._1, curSlice._2, false))
            }
            outVal.toIterator
        }.toIterator
    }
    val betweenSlices = outImg.filter(!_._2.finished).
      mapValues(_.block). // remove the partialFilter status
      map(inPair => (inPair._1, (inPair._1, inPair._2))). // format for the reduce command
      groupByKey.map {
      inPairs =>
        val cSlices = inPairs._2
        partBasedFilterReduce(cSlices.toList, upWindSize, filt)
    }
    (outImg.filter(_._2.finished) ++ betweenSlices).mapValues(_.block)
  }

  def partBasedFilterReduce(inSpreadImg: List[(D3int, TImgSlice[Array[Double]])],
                            upWindSize: D3int,
                            filt: filterGenerator): (D3int, partialFilter[Array[Double]]) = {
    val templateImg = inSpreadImg.head
    val sliceLen = templateImg._2.get.length
    // process blocks
    val kernelList = (for (i <- 0 until sliceLen) yield filt.make()).toArray
    //TODO hard code it for now
    val mKernel = BaseTIPLPluginIn.fullKernel
    for (curSlice <- inSpreadImg) {
      val cPos = curSlice._1
      val curBlock = curSlice._2
      val curArr = curBlock.get
      val curDim = new D3int(curBlock.getDim,1)
      val blockPos = curBlock.getPos
      val zp = blockPos.z
      for (yp <- 0 until curDim.y) {
        for (xp <- 0 until curDim.x) {
          val off = yp * curDim.x + xp
          val curKernel = kernelList(off)
          for (
            cPos <- BaseTIPLPluginIn.getScanPositions(mKernel, new D3int(xp, yp, zp),
              curBlock.getOffset(), off, curDim, upWindSize)
          ) {
            curKernel.addpt(xp, cPos.x, yp, cPos.y, zp, cPos.z,
              curArr(cPos.offset))
          }
        }
      }
    }
    val outSlice = kernelList.map {
      _.value()
    }.toArray
    val inBlock = templateImg._2
    val outBlock = new TImgSlice[Array[Double]](outSlice, inBlock.getPos, inBlock.getDim)

    (templateImg._1, partialFilter[Array[Double]](templateImg._1, outBlock, false))
  }
}


object SFilterTest {

  def main(args: Array[String]): Unit = {
    var p = SparkGlobal.activeParser(args)
    val boxSize = p.getOptionInt("boxsize", 8, "The dimension of the image used for the analysis")
    val iters = p.getOptionInt("iters", 5, "The number of iterations to use for the filter")
    val filtTool = new SFilterScale
    p = filtTool.setParameter(p)

    var testImg = TestPosFunctions.wrapIt(boxSize,
      new TestPosFunctions.SphericalLayeredImage(boxSize / 2, boxSize / 2, boxSize / 2, 0, 1, 2))

    for (i <- 0 to iters) {
      filtTool.LoadImages(Array(testImg))
      filtTool.execute()
      testImg = filtTool.ExportImages(testImg)(0)
      val outImg = testImg.asInstanceOf[DTImg[Array[Double]]]
      val outRdd = outImg.getBaseImg.rdd
      val justArr = outRdd.map {
        _._2.get
      }
      val imgStats = justArr.aggregate((0.0, 0))((lastSum, inArr) =>
        (lastSum._1 + inArr.sum / inArr.length, lastSum._2 + 1),
        (a, b) => (a._1 + b._1, a._2 + b._2))
      println("Iteration #" + i + " complete\t" + imgStats)
    }
  }
}

