package spark.images

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import tipl.util.{D3int, ID2int, TImgSlice}

import scala.reflect.ClassTag

case class D2int(var x: Int,var  y: Int) extends ID2int {
  override def gx(): Int = x

  override def setPos(x: Int, y: Int): Unit = {
    this.x=x
    this.y=y
  }

  override def gy(): Int = y
}
/**
 * A collection of tools for 2D imaging
 */
object ImageTools2D extends Serializable {


  /**
   * Basic 2D image interface (very basic)
   */
  trait ITImg2D extends Serializable {
    def getPos: ID2int
    def getDim: ID2int
    def getImage(): AnyRef
    def getRegion(pos: ID2int, dim: ID2int): AnyRef
  }


  /**
   * A strongly typed slice with region already implemented
   * @tparam T
   */
  trait TTImg2D[@specialized(Boolean, Byte, Short, Int, Long, Float, Double) T] extends
  ITImg2D {
    implicit val ct: ClassTag[T]
    def getImage(): Array[T]
    override def getRegion(pos: ID2int, dim: ID2int): Array[T] = {
      val size = dim.gx()*dim.gy()
      val region = new Array[T](size)
      val image = getImage()
      val startX = scala.math.max(pos.gx,getPos.gx)
      val finishX = scala.math.min(pos.gx+dim.gx,getPos.gx+getDim.gx)
      val startY = scala.math.max(pos.gy,getPos.gy)
      val finishY = scala.math.min(pos.gy+dim.gy,getPos.gy+getDim.gy)

      var j = startY
      while(j<=finishY) {
        var i = startX
        while(i<=finishX) {
          var ridx = (j-pos.gy)*dim.gx+(i-pos.gx) // region index
          var iidx = (j-getPos.gy)*getDim.gx+(i-getPos.gx) // image index
          region(ridx)=image(iidx)
          i+=1
        }
        j+=1
      }
      region
    }
  }

  object TestTTImg {
    class TestImage(implicit val ct: ClassTag[Double]) extends TTImg2D[Double] {

      override def getImage(): Array[Double] =  Array[Double](1,2,3,4)

      override def getPos: ID2int = D2int(0,0)

      override def getDim: ID2int = D2int(4,1)

    }

    val bob = new TestImage
    println(bob.getRegion(D2int(0,0),D2int(5,5)).mkString(", "))
  }
  /** create a Key-value Image with only points above a certain value **/
  def BlockImageToKVImage[T](sc: SparkContext, inImg: RDD[(D3int, TImgSlice[Array[T]])],
                             threshold: T)(implicit num: Numeric[T]) = {
    inImg.mapValues {
      cSlice =>
        val cSliceArr = cSlice.get
        val imgDim = new D3int(cSlice.getDim,1)
        val imgPos = cSlice.getPos
        for {
          y <- 0 until imgDim.y
          x <- 0 until imgDim.x
          oVal = cSliceArr(y * imgDim.x + x)
          if num.gt(oVal, threshold)
        } yield (new D3int(imgPos.x + x, imgPos.y + y, imgPos.z), oVal)
    }
  }

  def SliceToKVList[S](cSlice: TImgSlice[S],
                      slicePos: Option[Int] = None,
                       threshold: Double = Double.NegativeInfinity) = {
    val cSliceArr = cSlice.getAsDouble()
    val imgDim = cSlice.getDim
    val imgPos = cSlice.getPos
    val z = slicePos.getOrElse(imgPos.z)
    for {
      y <- 0 until imgDim.gy
      x <- 0 until imgDim.gx
      oVal = cSliceArr(y * imgDim.gx + x)
      if oVal >= threshold
    } yield (new D3int(imgPos.x + x, imgPos.y + y, z), oVal)
  }

  /** create a Key-value Image with only points above a certain value **/
  def BlockImageToKVImageDouble(sc: SparkContext, inImg: RDD[(D3int, TImgSlice[Array[Double]])],
                                threshold: Double) = {
    inImg.mapValues(SliceToKVList(_,threshold=threshold))
  }


  def compLabelingBySlice[T](inImg: RDD[(D3int, T)], windSize: D3int = new D3int(1, 1, 0),
                             maxIters: Int = Integer.MAX_VALUE) = {
    inImg.mapPartitions(inSlice => sliceCompLabel[T](inSlice, windSize, maxIters = maxIters), true)
  }

  def compLabelingBySlicePart[T](inImg: RDD[(D3int, Iterator[(D3int, T)])],
                                 windSize: D3int = new D3int(1, 1, 0),
                                 maxIters: Int = Integer.MAX_VALUE) = {
    inImg.mapPartitions(
      inPart =>
        inPart.map { inSlice => (inSlice._1, sliceCompLabel[T](inSlice._2, windSize))}, true)
  }

  /**
   * perform component labeling for slices separately
   */
  def sliceCompLabel[T](inImg: Iterator[(D3int, T)], windSize: D3int,
                        maxIters: Int = Integer.MAX_VALUE) = {
    // perform a threshold and relabel points
    var labelImg = inImg.toList.zipWithIndex.map { inval => (inval._1._1, (inval._2.toLong,
      inval._1._2))}
    var groupList = Map(0L -> 0)
    var running = true
    var iterations = 0
    while (running & iterations < maxIters) {
      val spreadList = labelImg.flatMap { inVox => ImageTools.spread_voxels_bin(inVox, windSize)}
      val newLabels = spreadList.groupBy(_._1).mapValues {
        voxList =>
          voxList.map {
            _._2
          }.reduce(ImageTools.cl_merge_voxels[T])
      }.filter(_._2._2). // keep only voxels which contain original pixels
        map(pvec => (pvec._1, pvec._2._1)).toList
      // make a list of each label and how many voxels are in it
      val curGroupList = newLabels.map(pvec => (pvec._2._1, 1)).groupBy(_._1).
        mapValues { voxList => voxList.map {
        _._2
      }.reduce(_ + _)
      }

      // if the list isn't the same as before, continue running since we need to wait for swaps
      // to stop
      running = !(curGroupList == groupList)
      groupList = curGroupList
      labelImg = newLabels
      iterations += 1
    }
    labelImg.toIterator
  }

    /** a very general component labeling routine using partitions to increase efficiency and
      * minimize the amount of over the wire traffic
      *
      */

  def compLabelingBySlicePartJL[T](inImg: RDD[(D3int, Iterator[(D3int, T)])],
                                   windSize: D3int = new D3int(1, 1, 0),
                                   maxIters: Int = Integer.MAX_VALUE) = {
    inImg.mapPartitions(
      inPart =>
        inPart.map { inSlice => (inSlice._1, compLabelingSliceJoinList[T](inSlice._2,
          windSize))}, true)
  }

  private[spark] def compLabelingSliceJoinList[T](inImg: Iterator[(D3int, T)],
                                                  windSize: D3int = new D3int(1, 1, 0)) = {
    // perform a threshold and relabel points
    var labelImg = inImg.zipWithIndex.map(inval => (inval._1._1, (inval._2.toLong, inval._1._2)))

    var running = true
    var iterations = 0
    while (running) {
      val spreadList = labelImg.flatMap(ImageTools.spread_voxels_bin(_, windSize))
      val mergeList = spreadList.toList.groupBy(_._1).map {
        cKeyValues =>
          val cPoints = cKeyValues._2
          val isTrue = for (cPt <- cPoints) yield cPt._2._2

          (for (cPt <- cPoints) yield cPt._2._1._1,
            isTrue.reduce(_ || _))
      }.filter(_._2)
      val replList = mergeList.flatMap {
        mlVal =>
          val mlList = mlVal._1.toList
          val minComp = mlList.min
          for (cVal <- mlList; if cVal != minComp) yield (cVal, minComp)
      }.toMap

      val newLabels = labelImg.map {
        cVox => (cVox._1, (replList.getOrElse(cVox._2._1, cVox._2._1), cVox._2._2))
      }

      labelImg = newLabels
      iterations += 1
      println("****")
      println("Iter #" + iterations + ": Replacements:" + replList.size)
      println("****")
      running = replList.size > 0
    }
    labelImg
  }
}
