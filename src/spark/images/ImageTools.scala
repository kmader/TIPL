package spark.images
import org.apache.spark.rdd.RDD
import tipl.util.D3int
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.math
/** 
 *  A series of tools that are useful for image data
 */
object ImageTools {
  
	def spread_voxels[T](pvec: (D3int, T), windSize: D3int = new D3int(1,1,1)) = {
      val pos = pvec._1
      val label = pvec._2
      for (x <- 0 to windSize.x; y <- 0 to windSize.y; z <- 0 to windSize.z) 
        yield (new D3int(pos.x + x, pos.y + y, pos.z + z), (label, (x == 0 & y == 0 & z == 0)))
    }
  /** a very general component labeling routine **/
	def compLabeling[T](inImg: RDD[(D3int,T)],windSize: D3int = new D3int(1,1,1)) = {
    // perform a threshold and relabel points
    var labelImg = inImg.zipWithUniqueId.map(inval => (inval._1._1,(inval._2,inval._1._2)))
    		

    var groupList = Array((0L, 0))
    var running = true
    var iterations = 0
    while (running) {
      val newLabels = labelImg.
        flatMap(spread_voxels(_,windSize))
       val nextStep = newLabels.
        reduceByKey{
            (a, b) => 
              (
                  (
                      math.min(a._1._1, b._1._1), // lowest label
                      if(a._2) a._1._2 else b._1._2
                  )
                  , // if a is original then keep it otherwise keep b
                  a._2 | b._2 // does it exist in the original
                  ) 
      }.
        filter(_._2._2). // keep only voxels which contain original pixels
        map(pvec => (pvec._1, pvec._2._1))
      // make a list of each label and how many voxels are in it
      val curGroupList = newLabels.map(pvec => (pvec._2._1._1, 1)).
        reduceByKey(_ + _).sortByKey(true).collect
      // if the list isn't the same as before, continue running since we need to wait for swaps to stop
      running = (curGroupList.deep != groupList.deep)
      groupList = curGroupList
      labelImg = newLabels.map(pvec => (pvec._1,pvec._2._1))
      iterations += 1
      print("Iter #" + iterations + ":" + groupList.mkString(","))
    }
    labelImg

  }
}