package spark.images

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import tipl.util.{TImgSlice, D3int}

import scala.reflect.ClassTag

/**
 * A series of tools that are useful for image data
 */
object ImageTools extends Serializable {
  /**
   * spread voxels out (S instead of T since S includes for component labeling the long as well
   * (Long,T)
   */
  def spread_voxels[S](pvec: (D3int, S),
                       windSize: D3int = new D3int(1, 1, 1),
                      startAtZero: Boolean = true
                        ) = {
    val pos = pvec._1
    val label = pvec._2
    val (sx,sy,sz) = if (startAtZero) (0,0,0) else (-windSize.x,-windSize.y,-windSize.z)
    val (ex,ey,ez) = (windSize.x,windSize.y,windSize.z)

      /**
       * the voxels are spread to new pixels at pos+(x,y,z)
       * at each of these pixels there is a feature vector length prod(windSize), or double that
       * if startAtZero=false
       * the indices of the feature vector are defined as
       *  [(cpos.z-sz)*(ey-sy+1) + (cpos.sy-sy)]*(ex-sx+1) + cpos.x-sx
       *  so new_pos = pos+(x,y,z) and cpos is new_pos-pos = (-x,-y,-z)
       * the second value is the index
        */

    //

    //
    for (x <- sx to ex; y <- sy to ey; z <- sz to ez)
    yield (new D3int(pos.x + x, pos.y + y, pos.z + z),
      (label,

        ((-z-sz)*(ey-sy+1)+(-y-sy))*(ex-sx+1)+(-x-sx)

        ))
  }
  def spread_voxels_bin[S](pvec: (D3int, S),
                           windSize: D3int = new D3int(1, 1, 1),
                           startAtZero: Boolean = true
                            ) = {
    val zeroInd = spread_zero(windSize,startAtZero)
    spread_voxels(pvec,windSize,startAtZero).map(
      kv =>
        (kv._1,(kv._2._1,kv._2._2==zeroInd))
    )
  }

  /**
   * The zero (0,0,0) index for the spread array given a window size and whether or not it starts
   * at zero
   * @param windSize the size of the window
   * @param startAtZero start at 0 or -windSize
   * @return the index of the zero point
   */
  def spread_zero(windSize: D3int = new D3int(1,1,1),
                  startAtZero: Boolean = true) = spread_index(new D3int(0,0,0),windSize,startAtZero)

  def spread_index(pos: D3int,windSize: D3int,
                   startAtZero: Boolean): Int = {
    if(startAtZero) {
      0
    } else {
      val (sx,sy,sz) = (-windSize.x,-windSize.y,-windSize.z)
      val (ex,ey,ez) = (windSize.x,windSize.y,windSize.z)
      ((pos.z-sz)*(ey-sy+1)+(pos.y-sy))*(ex-sx+1)+(pos.x-sx)
    }
  }

  /**
   * Transform a position RDD into a feature vector RDD
   * @param irdd image structure to operate on
   * @param windSize size of the window
   * @param defValue default value for filling the pixels outside
   * @tparam S the type of the image data
   */
  def neighbor_feature_vector[S](irdd: RDD[(D3int, S)],
                                 windSize: D3int = new D3int(1, 1, 1),
                                  defValue: S)(implicit sm: ClassTag[S]) = {
    val spreadZero = ImageTools.spread_zero(windSize,false)
    val spreadRdd = irdd.
      flatMap(ImageTools.spread_voxels(_,windSize,false)).
      groupByKey().
      filter( // remove edges
        kv =>
          kv._2.filter(_._2==spreadZero).size==1 // exactly one original point
      )

    val feat_vec_len = (2*windSize.x+1)*(2*windSize.y+1)*(2*windSize.z+1)
    val fvRdd = spreadRdd.map(
      kv => {
        val outArr = Array.fill[S](feat_vec_len)(defValue)
        val iter = kv._2.toIterator
        while (iter.hasNext) {
          val outVal = iter.next
          outArr(outVal._2) = outVal._1
        }
        (kv._1, outArr)
      }
    )
  }

  /**
   * spread slices out
   */
  def spread_slices[S](pvec: (D3int, S), zSize: Int) = {
    val pos = pvec._1
    val label = pvec._2
    for (z <- -zSize to zSize)
    yield (new D3int(pos.x, pos.y, pos.z + z), (label, z == 0))
  }

  /**
   * spread blocks out
   * @param pvec is the current block
   * @param zSize is the spreading to perform
   */
  def spread_blocks[S](pvec: (D3int, TImgSlice[S]), zSize: Int) = {
    val pos = pvec._1
    val origblock = pvec._2

    for (z <- -zSize to zSize)
    yield (new D3int(pos.x, pos.y, pos.z + z), origblock)
  }

  /**
   * spread blocks out according to an upscale and downscale factor
   * @param pvec is the current block
   * @param up is the upscaling factor (blocks spread from -up.z to up.z
   * @param dn is the downscaling factor the output position will be scaled by this number
   */
  def spread_blocks_gen[S](pvec: (D3int, TImgSlice[S]), up: D3int, dn: D3int) = {

    val pos = pvec._1
    val origblock = pvec._2

    for (z <- -up.z to up.z)
    yield (new D3int(pos.x, pos.y, pos.z), origblock)
  }

  def cl_merge_voxels[T](a: ((Long, T), Boolean), b: ((Long, T), Boolean)): ((Long, T), Boolean) = {
    (
      (
        math.min(a._1._1, b._1._1), // lowest label
        if (a._2) a._1._2 else b._1._2), // if a is original then keep it otherwise keep b
      a._2 | b._2 // does it exist in the original
      )
  }

  /** a very general component labeling routine **/
  def compLabeling[T](inImg: RDD[(D3int, T)], windSize: D3int = new D3int(1, 1, 1)) = {
    compLabelingCore(inImg, (inPoints: RDD[(D3int, (Long, T))]) =>
      inPoints.
        flatMap(spread_voxels_bin(_, windSize)))
  }

    /** a very general component labeling routine using partitions to increase efficiency
      * minimize the amount of over the wire traffic
      *
      */
  def compLabelingWithPartitions[T](inImg: RDD[(D3int, T)], windSize: D3int = new D3int(1, 1,
    1)) = {
    val partitionSpreadFunction = (inPoints: RDD[(D3int, (Long, T))]) => {
      inPoints.mapPartitions {
        cPart =>
          val outVox = cPart.flatMap(spread_voxels_bin[(Long, T)](_, windSize)).toList
          val grpSpreadPixels = outVox.groupBy(_._1)
          grpSpreadPixels.
            mapValues(inGroup => inGroup.map(_._2).reduce(cl_merge_voxels[T])).
            toIterator
      }
    }

    compLabelingCore(inImg, partitionSpreadFunction)

  }

  /**
   * Core routines for component labeling independent of implementation which is given by the
   * pointSpreadFunctionc command
   */
  private[spark] def compLabelingCore[T](inImg: RDD[(D3int, T)],
                                         pointSpreadFunction: RDD[(D3int, (Long,
                                           T))] => RDD[(D3int, ((Long, T), Boolean))]) = {
    // perform a threshold and relabel points
    var labelImg = inImg.zipWithUniqueId.map(inval => (inval._1._1, (inval._2, inval._1._2)))

    var groupList = Array((0L, 0))
    var running = true
    var iterations = 0
    while (running) {
      val spreadList = pointSpreadFunction(labelImg)
      val newLabels = spreadList.reduceByKey(cl_merge_voxels).
        filter(_._2._2). // keep only voxels which contain original pixels
        map(pvec => (pvec._1, pvec._2._1))
      // make a list of each label and how many voxels are in it
      val curGroupList = newLabels.map(pvec => (pvec._2._1, 1)).
        reduceByKey(_ + _).sortByKey(true).collect
      // if the list isn't the same as before, continue running since we need to wait for swaps
      // to stop
      running = curGroupList.deep != groupList.deep
      groupList = curGroupList
      labelImg = newLabels
      iterations += 1
      val ngroup = groupList.map(_._2)
      println("****")
      println("Iter #" + iterations + ": Groups:" + groupList.length + ", " +
        "mean size:" + ngroup.sum * 1.0 / ngroup.length + ", max size:" + ngroup.max)
      println("****")
    }
    labelImg

  }

  /**
   * Core routines for component labeling independent of implementation which is given by the
   * pointSpreadFunctionc command
   */
  private[spark] def compLabelingJoinList[T](inImg: RDD[(D3int, T)],
                                             windSize: D3int = new D3int(1, 1, 1)) = {
    // perform a threshold and relabel points
    var labelImg = inImg.zipWithUniqueId.map(inval => (inval._1._1, (inval._2, inval._1._2)))

    var running = true
    var iterations = 0
    while (running) {
      val spreadList = labelImg.flatMap(spread_voxels_bin(_, windSize))
      val mergeList = spreadList.groupByKey.map {
        cKeyValues =>
          val cPoints = cKeyValues._2
          val isTrue = for (cPt <- cPoints) yield cPt._2
          (for (cPt <- cPoints) yield cPt._1._1,
            isTrue.reduce(_ || _))
      }.filter(_._2)
      val replList = mergeList.flatMap {
        mlVal =>
          val mlList = mlVal._1.toList
          val minComp = mlList.min
          for (cVal <- mlList; if cVal != minComp) yield (cVal, minComp)
      }.distinct.collect.toMap
      running = replList.size > 0
      val newLabels = labelImg.mapValues {
        cVox => (replList.getOrElse(cVox._1, cVox._1), cVox._2)
      }

      labelImg = newLabels
      iterations += 1
      println("****")
      println("Iter #" + iterations + ": Replacements:" + replList.size)
      println("****")
    }
    labelImg

  }

}


case class D2int(x: Int, y: Int) extends Serializable


/**
 * A collection of tools for 2D imaging
 */
object ImageTools2D extends Serializable {
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