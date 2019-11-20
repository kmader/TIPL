package tipl.spark

import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import tipl.settings.FilterSettings
import tipl.tests.TestPosFunctions
import tipl.util.{D3int, TImgSlice, TImgTools}


/**
 *
 * Created by mader on 10/23/14.
 */
class SThickness { //extends ImageRDD.LoadImagesAsDSImg {
  lazy val sc = tipl.spark.SparkGlobal.getContext(getPluginName).sc

  def getPluginName: String = "SThickness-Hildebrand"

  def execute(): Boolean = {
    //val distMap = this.getDSImg[Double](sc,TImgTools.IMAGETYPE_DOUBLE,0)
    val skObj = new SFilterScale
    //skObj.LoadImages(new Array[TImgRO](distMap))
    skObj.setParameter("-filter=" + FilterSettings.GRADIENT)
    //val filtImg = skObj.ExportDTImg(distMap).getBaseImg().rdd

    //val seedPts = distMap.getBaseImg()
    true
  }
}

object SThickness extends Serializable {
  val testRad = 500
  val testImg = TestPosFunctions.wrapItAs(testRad, new TestPosFunctions.BoxDistances(testRad, testRad,
    testRad),
    TImgTools.IMAGETYPE_DOUBLE)

  def main(args: Array[String]): Unit = {
    val sth = new SThickness
    val sc = sth.sc
    val tImg = new DSImg[Double](sc, testImg, testImg.getImageType)
    var thickMap = tImg.getBaseImg() //.persist(StorageLevel.DISK_ONLY)
    val basePts = DTImgOps.DTrddToKVrdd(thickMap, tImg.getPos, tImg.getDim)
    val allPts = basePts.filter(_._2 > testRad / 2.0).repartition(50).mapPartitionsWithIndex((i, j) =>
      List((i, j)).toIterator)
    val preStr = "Before Stats:" + basePts.map(_._2).stats()
    var postStr = ""
    for (i <- 0 to 50) {
      //thickMap.sparkContext.setCheckpointDir("/scratch/chk")
      val nPts = allPts.lookup(i)(0).toArray
      println("Points to scan:" + nPts.length + "\n\t" + nPts.mkString(","))
      thickMap = fillRadius(thickMap, nPts)
      val thkSts = DTImgOps.DTrddToKVrdd(thickMap, tImg.getPos, tImg.getDim).map(_._2).stats()
      postStr = ("Current Stats:" + thkSts)
      println(postStr)
    }

    sth.sc.stop()
    println(preStr)
    println(postStr)

  }

  def fillRadius(inImg: RDD[(D3int, TImgSlice[Array[Double]])], npts: Array[(D3int, Double)]) = {
    inImg.map {
      inKV =>
        val (sPos, tslice) = inKV
        val tdim = tslice.getDim
        val islice = tslice.get
        val oslice = islice.clone()
        var i = 0
        for ((cPt, radius) <- npts) {
          val fPt = cPt.asFloat
          while (i < oslice.length) {
            if (oslice(i) < radius) {
              val x = sPos.x + i % tdim.gx()
              val y = sPos.y + math.floor(i / tdim.gx())
              val cdist = fPt.distance(x, y, sPos.z)
              if (cdist <= radius) oslice(i) = radius
            }
            i += 1
          }
        }
        (sPos, new TImgSlice[Array[Double]](oslice, tslice.getPos,
          tslice.getDim))
    }
  }
}
