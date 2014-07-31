package spark.images
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK_SER

object imageBasics extends Serializable {
  def main(args: Array[String]) {

    System.setProperty("spark.executor.memory", "40G")
    val sc = new SparkContext(args(0), "FilterTool")
    runFilter(sc, args(1), true)
  }

  def roi(pvec: ((Int, Int, Int), Double)) = {
    pvec._1._1 >= 0 & pvec._1._2 >= 0 & pvec._1._3 >= 0 &
      pvec._1._1 < 100 & pvec._1._2 < 100 & pvec._1._3 < 100
  }

  def noRoi(pvec: ((Int, Int, Int), Double)) = {
    true
  }


  def tinyroi(pvec: ((Int, Int, Int), Double)) = {
    pvec._1._1 >= 0 & pvec._1._2 >= 0 & pvec._1._3 >= 0 &
      pvec._1._1 < 10 & pvec._1._2 < 10 & pvec._1._3 < 10
  }

  def runFilter(sc: SparkContext, fileName: String, useROI: Boolean) = {
    val textImg = sc.textFile(fileName)
    // convert csv to position, value
    val rImg = textImg.map(_.split(",")).map(cLine => ((cLine(0).toInt, cLine(1).toInt, cLine(2).toInt), cLine(3).toDouble))

    // define volume of interest


    val roiFun = if (useROI) roi _ else noRoi _

    val defpers = MEMORY_AND_DISK_SER
    val roiImg = rImg.filter(roiFun).persist(defpers)

    // perform a box filter
    def spread_voxels(pvec: ((Int, Int, Int), Double), windSize: Int = 1) = {
      val wind = (-windSize to windSize)
      val pos = pvec._1
      val scalevalue = pvec._2 / (wind.length * wind.length * wind.length)
      for (x <- wind; y <- wind; z <- wind) yield ((pos._1 + x, pos._2 + y, pos._3 + z), scalevalue)
    }

    val filtImg = roiImg.flatMap(cvec => spread_voxels(cvec)).filter(roiFun).reduceByKey(_ + _)
    filtImg
  }

  def compLabeling(sc: SparkContext, inImg: RDD[((Int, Int, Int), Double)]) = {
    // perform a threshold and relabel points
    var labelImg = inImg.filter(_._2 > 0).map(pvec => (pvec._1, pvec._1._1.toLong * pvec._1._2.toLong * pvec._1._3.toLong + 1))

    def spread_voxels(pvec: ((Int, Int, Int), Long), windSize: Int = 1) = {
      val wind = (0 to windSize) // only need to scan positively
      val pos = pvec._1
      val label = pvec._2
      for (x <- wind; y <- wind; z <- wind) yield ((pos._1 + x, pos._2 + y, pos._3 + z), (label, (x == 0 & y == 0 & z == 0)))
    }
    var groupList = Array((0L, 0))
    var running = true
    var iterations = 0
    while (running) {
      val newLabels = labelImg.
        flatMap(spread_voxels(_, 1)).
        reduceByKey((a, b) => (math.min(a._1, b._1), (a._2 | b._2))).
        filter(_._2._2). // keep only voxels which contain original pixels
        map(pvec => (pvec._1, pvec._2._1))
      // make a list of each label and how many voxels are in it
      val curGroupList = newLabels.map(pvec => (pvec._2, 1)).
        reduceByKey(_ + _).sortByKey(true).collect
      // if the list isn't the same as before, continue running since we need to wait for swaps to stop
      running = (curGroupList.deep != groupList.deep)
      groupList = curGroupList
      labelImg = newLabels
      iterations += 1
      print("Iter #" + iterations + ":" + groupList.mkString(","))
    }
    groupList

  }

}

//filterImage.runFilter (sc,"./block*.csv",false).take(1)