package fourquant.imagej

import ch.fourquant.images.types.SerDeserHelper
import fourquant.imagej.PortableImagePlus.IJMetaData
import fourquant.imagej.scOps._
import ij.measure.Calibration
import org.apache.spark.{LocalSparkContext, SparkContext}
import org.scalatest.{FunSuite, Matchers}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

object SpijiTests extends Serializable {
  val width = 100
  val height = 50
  val imgs = 5
  val localMasterWorkers = false
  val runLocal = true
  val runLocalCluster = false

  val local_io_tests = true
  // getClass().getResource("images/dicom/test_dir/000000.dcm").getFile()
  val local_io_path = "images/dicom/test_dir/"

  val first_local_img = s"${local_io_path}/000000.dcm"


  val ijs = ImageJSettings("/Applications/Fiji.app/", showGui = false, runLaunch = false,
    record = false)

  def makeTestImages(sc: SparkContext, fact: Int = 1,
                     imgs: Int, width: Int, height: Int) =
    sc.createEmptyImages("/Users/mader/imgs/", imgs, width, height,
      (i: Int) => fact * (i - 1) * 1000 + 1000, Some(ijs))

}

class FullSpijiTests extends AbsSpijiTests {
  override def getIJS(): ImageJSettings = SpijiTests.ijs.copy(showGui = true,runLaunch = true)
}

class LaunchedSpijiTests extends AbsSpijiTests {
  override def getIJS(): ImageJSettings = SpijiTests.ijs.copy(showGui = false,runLaunch = true)
}

class HeadlessSpijiTests extends AbsSpijiTests {
  override def getIJS(): ImageJSettings = SpijiTests.ijs.copy(showGui = false,runLaunch = false)
}


/**
 * Ensure the distributed fiji code works as expected including reading and writing files, basic
 * plugin support, and histogram / table analysis
 * Created by mader on 1/16/15.
 */

abstract class AbsSpijiTests extends FunSuite with Matchers {
  def getIJS(): ImageJSettings

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
    getIJS.setupFiji()

    assert(
      Spiji.getCommandList.contains("Auto Threshold"),
      "Auto Threshold is part of FIJI")
  }


  for (thrshCmd <-
       Array(
         ("applyThreshold", "lower=0 upper=20"),
         ("Auto Threshold", "method=IsoData white setthreshold")
       )
  ) {
    test(s"Simple Noise: $thrshCmd") {
      getIJS.setupFiji()

      val tImg = new PortableImagePlus(Array.fill[Int](SpijiTests.width, SpijiTests.height)(0),PortableImagePlus
        .IJMetaData.emptyMetaData())

      val rt = tImg.run("Add Noise").
        run(thrshCmd._1,thrshCmd._2).
        run("Convert to Mask").
        analyzeParticles()

      println(rt)

      assert(rt.numObjects > 0, "More than 0 objects")
      assert(rt.mean("Empty Column").isEmpty, "Made up column names should be empty")
      assert(rt.numObjects > 0, "There should be more than a single object")
      assert(rt.sum("Area").get < (SpijiTests.width * SpijiTests.height),
        "The noise shouldnt fill up the entire image")
      assert(rt.min("Area").get < rt.max("Area").get, "Minimum area should be less than maximum")
      assert(rt.min("Area").get > 0, "Minumum should be greater than 0")
    }
    test(s"Preserving Calibration Information: $thrshCmd") {
      val tCalib = new IJCalibration()
      tCalib.pixelWidth =   .99
      tCalib.pixelHeight = 0.98
      tCalib.pixelDepth = 0.97
      tCalib.setFunction(Calibration.STRAIGHT_LINE,Array(0,-0.5),"Golds")
      tCalib.make16BitCTable()
      tCalib.xOrigin =   .7

      //tCalib.setCTable(new Array[Float](65536),"Golds")

      tCalib.setUnit("mm")

      println(s"Before\t$tCalib")
      //tCalib.setValueUnit("Golds")

      val tImg = new PortableImagePlus(Array.fill[Int](SpijiTests.width, SpijiTests.height)(5),IJMetaData(tCalib,""))

      val ntImg = SerDeserHelper.typedSerialize(tImg).get

      val cTable = tImg.
        run(thrshCmd._1,thrshCmd._2)

      println("Original, Serialized, Tabular, Processed")
      println(s"$tCalib =? \n${ntImg.getMetaData.ijc} =? \n${tImg.getImg.getCalibration} =? \n${cTable
          .getMetaData.ijc}")

      tImg.getImg.getCalibration.toString shouldBe tCalib.toString
      ntImg.getImg.getCalibration.toString shouldBe tCalib.toString
      cTable.getMetaData.ijc.toString shouldBe tCalib.toString

    }
    test(s"Reusing Images: $thrshCmd") {
      val tImg = new PortableImagePlus(Array.fill[Int](SpijiTests.width, SpijiTests.height)(5),
        PortableImagePlus.IJMetaData.emptyMetaData())

      val cTable = tImg.
        run(thrshCmd._1,thrshCmd._2).
        run("Convert to Mask").
        analyzeParticles()

      assert(cTable.mean("Area").isDefined, "Area column should be present")
      assert(cTable.mean("Area").get < 20.0, "Noise should be small single pixels")

      val dTable = tImg.
        run(thrshCmd._1,thrshCmd._2).
        run("Convert to Mask").
        analyzeParticles()

      assert(cTable.numObjects == 1, "Should have one object")
    }

  }

  test("Test Results Table with Hist Command") {
    val tImg = new PortableImagePlus(Array.fill[Int](SpijiTests.width, SpijiTests.height)(5),
      PortableImagePlus.IJMetaData.emptyMetaData())
    val (outImg,outTable) = tImg
      .runWithTable("Histogram")

    println(s"Header: ${outTable.header.mkString(",")}")
    println(s"All Values\n${outTable.rows.map(_.mkString(",")).mkString("\n")}")
    println(s"Area: ${outTable.getColumn("Area")}")
    println(s"${outTable.toMap.mapValues(_.mkString(",")).mkString("\n")}")
  }


  test("Histogram Analysis") {
    val tImg = new PortableImagePlus(Array.fill[Int](SpijiTests.width, SpijiTests.height)(5),
      PortableImagePlus.IJMetaData.emptyMetaData())
    val simpHist = tImg.getHistogram(Some((0.0,10.0)),bins=3)
    println(simpHist)
    simpHist.bin_centers(0) shouldBe 0.0+-0.1
    simpHist.bin_centers(2) shouldBe 10.0+-0.1
    simpHist.counts(0) shouldBe 0
    simpHist.counts(2) shouldBe 0
    simpHist.counts(1) shouldBe (SpijiTests.width*SpijiTests.height)
  }

  for(imgType <- Array("8-bit","16-bit","32-bit","RGB Color")) {
    test(s"Histogram Analysis $imgType") {
      val tImg = new PortableImagePlus(Array.fill[Int](SpijiTests.width, SpijiTests.height)(5),
        PortableImagePlus.IJMetaData.emptyMetaData())
      val simpHist = tImg.
        run(imgType).
        getHistogram(Some((0.0,10.0)),bins=3)

      println(simpHist)
      simpHist.bin_centers(0) shouldBe 0.0+-0.1
      simpHist.bin_centers(2) shouldBe 10.0+-0.1
      val tcount = (SpijiTests.width*SpijiTests.height)
      val zbin = if(!imgType.contains("16-bit")) tcount else 0
      val obin = if(!imgType.contains("16-bit")) 0 else tcount
      simpHist.counts(0) shouldBe zbin
      simpHist.counts(1) shouldBe obin
      simpHist.counts(2) shouldBe 0

    }
  }

  test("Histogram Distance") {
    val noiseFree = new PortableImagePlus(Array.fill[Int](SpijiTests.width, SpijiTests.height)(5),
      PortableImagePlus.IJMetaData.emptyMetaData())
    val noiseFreeShift =
      new PortableImagePlus(Array.fill[Int](SpijiTests.width, SpijiTests.height)(50),IJMetaData.emptyMetaData())
    val noisyImage = noiseFree.run("Add Noise")

    val nfHist = noiseFree.getHistogram()
    val nfsHist = noiseFreeShift.getHistogram()
    val noHist = noisyImage.getHistogram()

    println("NF:\t"+nfHist)
    println("NFS:\t"+nfsHist)
    println("NO:\t"+noHist)

    println("Diff:"+(noHist-nfHist))
    println("Diff:"+(nfsHist-nfHist))

    nfHist-nfHist shouldBe 0.0+-0.01
    noHist-nfHist should be > 0.0
    noHist-nfHist should be < 1.0
    nfsHist-nfHist shouldBe 1.0+-0.01
  }
  def testStack[T](ivals: Array[T])(implicit ct: ClassTag[T]) = {
    val imageArray = ivals.map(i =>
      new PortableImagePlus(Array.fill[T](SpijiTests.width, SpijiTests.height)(i),
        PortableImagePlus.IJMetaData.emptyMetaData()).getImg()
    )

    val imStack = Spiji.createStackFromImagePlusArr(imageArray.toArray)
    println(s"${imStack.getStackSize} stack size, ${imStack.getNSlices} nslices, ${imStack.getSlice}")
    imStack.getStackSize shouldBe 11
    for(i <- 1 to 11) {
      val cProc = imStack.getStack().getProcessor(i).convertToShortProcessor(false)
      println(s"$i -> $cProc, val:${cProc.get(0,0)}")
      cProc.get(0,0) shouldBe (i-1)
    }

    val pipStack = new PortableImagePlus(imStack)
    val fullArray = pipStack.getArray()
    println(s"Reading out full array: $fullArray")
    val tArr = fullArray.asInstanceOf[Array[Array[Array[Int]]]]

    tArr(0)(0)(0) shouldBe 0
    tArr.length shouldBe imStack.getHeight
    tArr(0).length shouldBe imStack.getWidth
    tArr(0)(0).length shouldBe imStack.getStackSize
  }

  test("Building Stacks Integer") {
    testStack[Int]((0 to 10).toArray)
  }

  test("Building Stacks Short") {
    testStack[Short]((0 to 10).toArray.map(_.toShort))
  }

}


class DistributedSpijiTests extends FunSuite with LocalSparkContext with Matchers {

  import SpijiTests.makeTestImages

  val conList = ArrayBuffer(
    ("LocalSpark", (a: String) => getNewSpark("local[8]", a))
  )
  if (!SpijiTests.runLocal) conList.remove(0)

  if (SpijiTests.runLocalCluster) conList +=
    (
      "LocalCluster",
      (a: String) => getNewSpark("local-cluster[2,2,512]", a)
      )
  if (SpijiTests.localMasterWorkers) conList +=("DistributedSpark", (a: String) =>
    getNewSpark("spark://MacBook-Air.local:7077", a))


  for ((conName, curCon) <- conList) {
    implicit val ijs = SpijiTests.ijs
    val sc = curCon("Spiji")
    test(conName + ":Creating synthetic images") {
      implicit val ijs = SpijiTests.ijs
      val imgList = makeTestImages(sc, 1, SpijiTests.imgs, SpijiTests.width, SpijiTests.height)
      imgList runAll ("Add Noise")
      imgList saveImages("/Users/mader/imgs", ".jpg")
    }

    test(conName + ":Image Immutability Test : Noise") {
      implicit val ijs = SpijiTests.ijs
      val imgList = makeTestImages(sc, 0, SpijiTests.imgs, SpijiTests.width, SpijiTests.height)
        .cache
      // execute them in the wrong order
      val wwNoise = imgList.runAll("Add Noise").runAll("Add Noise").
        getStatistics().values.takeSample(false, 1)(0).stdDev
      val wNoise = imgList.runAll("Add Noise").getStatistics().values.takeSample(false, 1)(0).stdDev
      val woNoise = imgList.getStatistics().values.takeSample(false, 1)(0).stdDev
      print("None:" + woNoise + "\tOnce:" + wNoise + "\tTwice:" + wwNoise)
      assert(woNoise < 1e-3, "Standard deviation of image is almost 0")
      assert(wNoise > woNoise, "With noise is more than without noise")
      assert(wwNoise > wNoise, " With double noise is more then just noise")
    }

    test(conName + ":RDD vs Standard") {

      implicit val ijs = SpijiTests.ijs
      val imgList = makeTestImages(sc, 0, SpijiTests.imgs, SpijiTests.width, SpijiTests.height)
        .cache
      // write the rdd to a list and process it
      val procList = imgList.collect.
        map(iv => (iv._1, iv._2.run("Median...", "radius=3"))).toMap
      // process it in rdd space
      val procRdd = imgList.runAll("Median...", "radius=3").collect.toMap
      assert(procList.keySet.size == procRdd.keySet.size, "Same number of images")
      for (ckey <- procList.keySet) {
        println(procList(ckey).toString)
        println(procRdd(ckey).toString)
        compareImages(procList(ckey), procRdd(ckey))
      }
    }

    test(conName + ":Noisy RDD vs Standard") {
      implicit val ijs = SpijiTests.ijs
      val imgList = makeTestImages(sc, 0, SpijiTests.imgs, SpijiTests.width, SpijiTests.height)
        .cache
      // write the rdd to a list and process it
      val procList = imgList.collect.
        map(iv => (iv._1, iv._2.run("Add Noise").getImageStatistics())).toMap
      // process it in rdd space
      val procRdd = imgList.runAll("Add Noise").getStatistics().collect.toMap
      assert(procList.keySet.size == procRdd.keySet.size, "Same number of images")
      for (ckey <- procList.keySet) {
        assert(procList(ckey) compareTo(procRdd(ckey), 30e-2), "Assure difference is less than 20%")
      }
    }

    test(conName + ":Image Immutability Test : Threshold ") {
      implicit val ijs = SpijiTests.ijs
      val imgList = makeTestImages(sc, 0, SpijiTests.imgs, SpijiTests.width, SpijiTests.height)
        .cache

      // execute them in the wrong order
      val wNoisewThreshTwice = imgList.runAll("Add Noise").
        runAll("applyThreshold", "lower=0 upper=1000").runAll("applyThreshold", "lower=0 " +
        "upper=1000").
        getStatistics().values.takeSample(false, 1)(0)
      val wNoisewThresh = imgList.runAll("Add Noise").runAll("applyThreshold", "lower=0 " +
        "upper=1000").
        getStatistics().values.takeSample(false, 1)(0)
      val wNoise = imgList.runAll("Add Noise").
        getStatistics().values.takeSample(false, 1)(0)
      val woNoise = imgList.getStatistics().values.takeSample(false, 1)(0)
      print("\tNone:" + woNoise + "\n\tNoise:" + wNoise + "\n\tThresh:" + wNoisewThresh +
        "\n\tTThres" +
        ":" + wNoisewThreshTwice)
      assert(woNoise.mean == 1000, "Mean of standard image should be 1000")
      assert(woNoise.stdDev < 1e-3, "Std of standard image should be 0")
      assert(wNoise.stdDev > 1, "With noise is should have a greater than 1 std")
      assert(wNoisewThresh.mean < 255, " With a threshold the mean should be below 255")
      assert(wNoisewThresh.stdDev > 1, " With a threshold the std should be greater than 0")
      assert(wNoisewThreshTwice.mean == 255, " With a second very loose threshold the mean should" +
        " be 255")
    }

    test(conName + ": Testing Parameter Sweep") {

      val imgList = makeTestImages(sc, 1, SpijiTests.imgs, SpijiTests.width, SpijiTests.height)
      imgList.runAll("Add Noise").
        runRange("Median...", "radius=1.0", "radius=5.0").
        saveImagesLocal(".jpg")

    }

    test(conName + ": Testing Log Values") {
      val imgList = makeTestImages(sc, 1, SpijiTests.imgs, SpijiTests.width, SpijiTests.height)
      val oLogs = imgList.runAll("Add Noise").
        runRange("Median...", "radius=1.0", "radius=5.0").mapValues(_.imgLog)
      oLogs.collect().foreach {
        case (key, value) =>
          println(key + "=\t" + value.toJsStrArray.mkString("\n\t[", "\t", "]"))
      }
    }

    test(conName + ": Testing Autothreshold and Component Label") {
      val imgList = makeTestImages(sc, 1, SpijiTests.imgs, SpijiTests.width, SpijiTests.height)
      val oLogs = imgList.runAll("Add Noise").
        runAll("Auto Threshold", "method=IsoData white setthreshold").
        runAll("Analyze Particles...", "display clear")

    }

  }

  def compareImages(p1: PortableImagePlus, p2: PortableImagePlus, cutOff: Double = 1e-2): Unit = {
    p1.getImg().getDimensions should equal(p2.getImg().getDimensions())

    val is1 = p1.getImageStatistics()
    val is2 = p2.getImageStatistics()
    println("Stats1\t"+is1+"\n"+"Stats2\t"+is2)
    is1.mean should equal(is2.mean+-0.5)
    is1.min should equal(is2.min+-0.5)
    is1.pts should equal(is2.pts)
    //assert(is1.compareTo(is2, cutOff), "Image statistics should be within " + cutOff * 100 + "%")
  }
}

