package tipl.ij.scripting

import org.apache.spark.SparkContext._
import org.apache.spark.{LocalSparkContext, SparkContext}
import org.scalatest.{FunSuite, Matchers}
import tipl.ij.Spiji
import tipl.ij.scripting.ImagePlusIO.PortableImagePlus
import tipl.ij.scripting.scOps._

import scala.collection.mutable.ArrayBuffer

object SpijiTests extends Serializable {
  val width = 100
  val height = 50
  val imgs = 5
  val localMasterWorkers = false
  val runLocal = true
  val runLocalCluster = false
  val ijs = ImageJSettings("/Applications/Fiji.app/",showGui=false,runLaunch=false,record=false)

  def makeTestImages(sc: SparkContext, fact: Int = 1,
                     imgs: Int, width: Int, height: Int) =
    sc.createEmptyImages("/Users/mader/imgs/",imgs,width,height,
      (i: Int) => fact*(i-1)*1000+1000,Some(ijs))

}

/**
 * Ensure the distributed fiji code works as expected including reading and writing files, basic
 * plugin support, and histogram / table analysis
 * Created by mader on 1/16/15.
 */

class SpijiTests extends FunSuite with Matchers {
  test("ImageJ Tests") {
    assert(
      !Spiji.getCommandList.contains("Auto Threshold"),
      "Auto Threshold is not intalled with ImageJ")

    assert(
      Spiji.getCommandList.contains("Add Noise"),
      "Add Noise is part of ImageJ")
    assert(
      Spiji.getCommandList.contains("Median..."),
      "Median is part of ImageJ")
  }

  test("Fiji Setup Tests") {
    SpijiTests.ijs.setupFiji()
    assert(
      Spiji.getCommandList.contains("Auto Threshold"),
      "Auto Threshold is part of FIJI")
  }

  test("Single Autothreshold") {
    SpijiTests.ijs.setupFiji()

    val tImg = new PortableImagePlus(Array.fill[Int](SpijiTests.width, SpijiTests.height)(0))

    val rt = tImg.run("Add Noise").run("Auto Threshold", "method=IsoData white setthreshold").
      run("Convert to Mask").analyzeParticles()

    println(rt.header.mkString(","))
    assert(rt.numObjects > 0, "More than 0 objects")
    assert(rt.mean("Empty Column").isEmpty, "Made up column names should be empty")
    assert(rt.mean("Area").get < 10.0, "Noise should be small single pixels")
  }
  test("Quick Shape") {
    val tImg = new PortableImagePlus(Array.fill[Int](SpijiTests.width, SpijiTests.height)(5))

    val cTable = tImg.run("applyThreshold", "lower=10 upper=20").run("Convert to Mask")
      .analyzeParticles()


    assert(cTable.mean("Area").get < 20.0, "Noise should be small single pixels")

    val dTable = tImg.run("applyThreshold", "lower=0 upper=20").run("Convert to Mask")
      .analyzeParticles()

    assert(cTable.numObjects == 1, "Should have one object")
  }
}

class DistributedSpijiTests extends FunSuite with LocalSparkContext with Matchers {
  import tipl.ij.scripting.SpijiTests.makeTestImages
  val conList = ArrayBuffer(
    ("LocalSpark",(a: String) => getSpark("local[8]",a))
  )
  if(!SpijiTests.runLocal) conList.remove(0)

  if(SpijiTests.runLocalCluster) conList+=("LocalCluster",(a: String) => getSpark("local-cluster[2,2,512]",a))
  if(SpijiTests.localMasterWorkers) conList+=("DistributedSpark",(a: String) =>
    getSpark("spark://MacBook-Air.local:7077",a))


  for((conName,curCon) <- conList) {

    test(conName+":Creating synthetic images") {
      implicit val ijs = SpijiTests.ijs
      sc = curCon("Spiji")
      val imgList = makeTestImages(sc,1,SpijiTests.imgs,SpijiTests.width,SpijiTests.height)
      imgList runAll("Add Noise")
      imgList saveImages("/Users/mader/imgs",".jpg")
    }

    test(conName+":Image Immutability Test : Noise") {
      implicit val ijs = SpijiTests.ijs
      sc = curCon("Spiji")
      val imgList =  makeTestImages(sc,0,SpijiTests.imgs,SpijiTests.width,SpijiTests.height).cache
      // execute them in the wrong order
      val wwNoise = imgList.runAll("Add Noise").runAll("Add Noise").
        getStatistics().values.takeSample(false,1)(0).stdDev
      val wNoise = imgList.runAll("Add Noise").getStatistics().values.takeSample(false,1)(0).stdDev
      val woNoise = imgList.getStatistics().values.takeSample(false,1)(0).stdDev
      print("None:"+woNoise+"\tOnce:"+wNoise+"\tTwice:"+wwNoise)
      assert(woNoise<1e-3,"Standard deviation of image is almost 0")
      assert(wNoise>woNoise,"With noise is more than without noise")
      assert(wwNoise>wNoise," With double noise is more then just noise")
    }

    test(conName+":RDD vs Standard") {
      sc = curCon("Spiji")
      implicit val ijs = SpijiTests.ijs
      val imgList =  makeTestImages(sc,0,SpijiTests.imgs,SpijiTests.width,SpijiTests.height).cache
      // write the rdd to a list and process it
      val procList = imgList.collect.
        map(iv => (iv._1,iv._2.run("Median...","radius=3"))).toMap
      // process it in rdd space
      val procRdd = imgList.runAll("Median...","radius=3").collect.toMap
      assert(procList.keySet.size==procRdd.keySet.size,"Same number of images")
      for(ckey<- procList.keySet) {
        println(procList(ckey).toString)
        println(procRdd(ckey).toString)
        compareImages(procList(ckey),procRdd(ckey))
      }
    }

    test(conName+":Noisy RDD vs Standard") {
      sc = curCon("Spiji")
      implicit val ijs = SpijiTests.ijs
      val imgList =  makeTestImages(sc,0,SpijiTests.imgs,SpijiTests.width,SpijiTests.height).cache
      // write the rdd to a list and process it
      val procList = imgList.collect.
        map(iv => (iv._1,iv._2.run("Add Noise").getImageStatistics())).toMap
      // process it in rdd space
      val procRdd = imgList.runAll("Add Noise").getStatistics().collect.toMap
      assert(procList.keySet.size==procRdd.keySet.size,"Same number of images")
      for(ckey<- procList.keySet) {
        assert(procList(ckey) compareTo(procRdd(ckey),30e-2),"Assure difference is less than 20%")
      }
    }

    test(conName+":Image Immutability Test : Threshold ") {
      implicit val ijs = SpijiTests.ijs
      sc = curCon("Spiji")
      val imgList =  makeTestImages(sc,0,SpijiTests.imgs,SpijiTests.width,SpijiTests.height).cache

      // execute them in the wrong order
      val wNoisewThreshTwice = imgList.runAll("Add Noise").
        runAll("applyThreshold","lower=0 upper=1000").runAll("applyThreshold","lower=0 upper=1000").
        getStatistics().values.takeSample(false,1)(0)
      val wNoisewThresh = imgList.runAll("Add Noise").runAll("applyThreshold","lower=0 upper=1000").
        getStatistics().values.takeSample(false,1)(0)
      val wNoise = imgList.runAll("Add Noise").
        getStatistics().values.takeSample(false,1)(0)
      val woNoise = imgList.getStatistics().values.takeSample(false,1)(0)
      print("\tNone:"+woNoise+"\n\tNoise:"+wNoise+"\n\tThresh:"+wNoisewThresh+"\n\tTThres" +
        ":"+wNoisewThreshTwice)
      assert(woNoise.mean==1000,"Mean of standard image should be 1000")
      assert(woNoise.stdDev<1e-3,"Std of standard image should be 0")
      assert(wNoise.stdDev>1,"With noise is should have a greater than 1 std")
      assert(wNoisewThresh.mean<255," With a threshold the mean should be below 255")
      assert(wNoisewThresh.stdDev>1," With a threshold the std should be greater than 0")
      assert(wNoisewThreshTwice.mean==255," With a second very loose threshold the mean should be 255")
    }

    test(conName+":Testing Parameter Sweep") {
      implicit val ijs = SpijiTests.ijs
      sc = curCon("Spiji")
      val imgList =  makeTestImages(sc,1,SpijiTests.imgs,SpijiTests.width,SpijiTests.height)
      imgList.runAll("Add Noise").
        runRange("Median...","radius=1.0","radius=5.0").
        saveImagesLocal(".jpg")

    }

    test(conName+":Testing Log Values") {
      implicit val ijs = SpijiTests.ijs
      sc = curCon("Spiji")
      val imgList =  makeTestImages(sc,1,SpijiTests.imgs,SpijiTests.width,SpijiTests.height)
      val oLogs = imgList.runAll("Add Noise").
        runRange("Median...", "radius=1.0", "radius=5.0").mapValues(_.imgLog)
      oLogs.collect().foreach {
        case (key, value) =>
          println(key + "=\t" + value.toJsStrArray.mkString("\n\t[", "\t", "]"))
      }
    }

    test(conName+": Testing Autothreshold and Component Label") {
      implicit val ijs = SpijiTests.ijs
      sc = curCon("Spiji")
      val imgList = makeTestImages(sc,1,SpijiTests.imgs,SpijiTests.width,SpijiTests.height)
      val oLogs = imgList.runAll("Add Noise").
        runAll("Auto Threshold", "method=IsoData white setthreshold").
        runAll("Analyze Particles...", "display clear")

    }
    resetSparkContext()
  }


  def compareImages(p1: PortableImagePlus,p2: PortableImagePlus, cutOff: Double = 1e-5): Unit = {
    p1.getImg().getDimensions should equal (p2.getImg().getDimensions())
    val is1 = p1.getImageStatistics
    val is2 = p2.getImageStatistics()
    assert(is1.compareTo(is2,cutOff),"Image statistics should be within "+cutOff*100+"%")
  }
}

