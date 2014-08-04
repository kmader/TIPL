package spark.images
import org.apache.spark.rdd.RDD
import tipl.util.D3int
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.math
import tipl.util.TImgBlock
/** 
 *  A series of tools that are useful for image data
 */
object ImageTools {
	/** 
	 *  spread voxels out (S instead of T since S includes for component labeling the long as well (Long,T)
	 */
	def spread_voxels[S](pvec: (D3int, S), windSize: D3int = new D3int(1,1,1)) = {
      val pos = pvec._1
      val label = pvec._2
      for (x <- 0 to windSize.x; y <- 0 to windSize.y; z <- 0 to windSize.z) 
        yield (new D3int(pos.x + x, pos.y + y, pos.z + z), (label, (x == 0 & y == 0 & z == 0)))
    }
	def cl_merge_voxels[T](a: ((Long,T),Boolean), b: ((Long,T), Boolean)): ((Long,T), Boolean) = {
	  (
	      (
	      math.min(a._1._1, b._1._1), // lowest label
	      if(a._2) a._1._2 else b._1._2
	      )
	      , // if a is original then keep it otherwise keep b
	      a._2 | b._2 // does it exist in the original
	      ) 
	}
	
  /** a very general component labeling routine **/
  def compLabeling[T](inImg: RDD[(D3int,T)],windSize: D3int = new D3int(1,1,1)) = {
	 compLabelingCore(inImg,(inPoints: 
	     RDD[(D3int,(Long,T))]) => 
	       inPoints.
	       flatMap(spread_voxels(_,windSize)))
  }
	
 /** a very general component labeling routine using partitions to increase efficiency and minimize the amount of over the wire traffic **/
 def compLabelingWithPartitions[T](inImg: RDD[(D3int,T)],windSize: D3int = new D3int(1,1,1)) = {
	val partitionSpreadFunction = (inPoints: RDD[(D3int,(Long,T))]) => {  
      inPoints.mapPartitions{
        cPart => 
          val outVox = cPart.flatMap(spread_voxels[(Long,T)](_,windSize)).toList
          val grpSpreadPixels = outVox.groupBy(_._1)
          grpSpreadPixels.
          mapValues(inGroup => inGroup.map(_._2).reduce(cl_merge_voxels[T])).
          toIterator
      }
	}
	compLabelingCore(inImg,partitionSpreadFunction)

  }
 /**
  * Core routines for component labeling independent of implementation which is given by the pointSpreadFunctionc command
  */
 private[spark] def compLabelingCore[T](inImg: RDD[(D3int,T)],
     pointSpreadFunction: RDD[(D3int,(Long,T))] => RDD[(D3int,((Long,T),Boolean))]) = {
    // perform a threshold and relabel points
    var labelImg = inImg.zipWithUniqueId.map(inval => (inval._1._1,(inval._2,inval._1._2)))
    
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
      // if the list isn't the same as before, continue running since we need to wait for swaps to stop
      running = (curGroupList.deep != groupList.deep)
      groupList = curGroupList
      labelImg = newLabels
      iterations += 1
      val ngroup = groupList.map(_._2)
      println("****")
      println("Iter #" + iterations + ": Groups:" + groupList.length+", mean size:"+ngroup.sum*1.0/ngroup.length+", max size:"+ngroup.max)
      println("****")
    }
    labelImg

  }
 
}

case class D2int(x: Int, y: Int)

/**
 * A collection of tools for 2D imaging
 */
object ImageTools2D {
  /** create a Key-value Image with only points above a certain value **/
  def BlockImageToKVImage[T](sc: SparkContext,inImg: RDD[(D3int,TImgBlock[Array[T]])],threshold: T)(implicit num: Numeric[T])  = {
    inImg.mapValues{
      cSlice =>
        val cSliceArr = cSlice.get
        val imgDim = cSlice.getDim
        val imgPos = cSlice.getPos
        for{y<- 0 until imgDim.y;
            x<-0 until imgDim.x;
            oVal = cSliceArr(y*imgDim.x+x);
            if num.gt(oVal,threshold)
        } yield (new D3int(imgPos.x+x,imgPos.y+y,imgPos.z),oVal)
    }
  }
 
  /** a very general component labeling routine using partitions to increase efficiency and minimize the amount of over the wire traffic **/
  def compLabelingBySlice[T](inImg: RDD[(D3int,T)],windSize: D3int = new D3int(1,1,0),maxIters: Int = Integer.MAX_VALUE) = {
	  inImg.mapPartitions(inSlice => sliceCompLabel[T](inSlice,windSize,maxIters=maxIters),true)
  }
  
   /** a very general component labeling routine using partitions to increase efficiency and minimize the amount of over the wire traffic **/
  def compLabelingBySlicePart[T](inImg: RDD[(D3int,Iterator[(D3int,T)])],windSize: D3int = new D3int(1,1,0),maxIters: Int = Integer.MAX_VALUE) = {
	  inImg.mapPartitions(
	    inPart => 
	      inPart.map{inSlice => (inSlice._1,sliceCompLabel[T](inSlice._2,windSize))}
	    ,true)
  }
  
   /**
  * perform component labeling for slices separately 
  */
 private[ImageTools2D] def sliceCompLabel[T](inImg: Iterator[(D3int,T)],windSize: D3int,maxIters: Int = Integer.MAX_VALUE)  = {
    // perform a threshold and relabel points
    var labelImg = inImg.toList.zipWithIndex.map{inval => (inval._1._1,(inval._2.toLong,inval._1._2))}
    var groupList = Map(0L -> 0)
    var running = true
    var iterations = 0
    while (running & iterations<maxIters) {
      val spreadList = labelImg.flatMap{inVox => ImageTools.spread_voxels(inVox,windSize)}
      val newLabels = spreadList.groupBy(_._1).mapValues{
        voxList =>
          voxList.map{_._2}.reduce(ImageTools.cl_merge_voxels[T])
      }.filter(_._2._2). // keep only voxels which contain original pixels
        map(pvec => (pvec._1, pvec._2._1)).toList
      // make a list of each label and how many voxels are in it
      val curGroupList = newLabels.map(pvec => (pvec._2._1, 1)).groupBy(_._1).
      	mapValues{ voxList => voxList.map{_._2}.reduce(_ + _)}
      
      // if the list isn't the same as before, continue running since we need to wait for swaps to stop
      running = !(curGroupList == groupList)
      groupList = curGroupList
      labelImg = newLabels
      iterations += 1
    }
    labelImg.toIterator
  } 
  
  
}