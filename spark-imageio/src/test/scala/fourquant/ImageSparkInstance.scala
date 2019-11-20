package fourquant

import fourquant.arrays.{BreezeOps, Positions}
import fourquant.io.BufferedImageOps.ArrayImageMapping
import fourquant.io.ImageIOOps._
import fourquant.io.ImageTestFunctions
import fourquant.tiles.TilingStrategies
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkConf, SparkContext}

import scala.reflect.ClassTag


/**
 * Created by mader on 4/14/15.
 */
trait ImageSparkInstance {

  def useLocal: Boolean

  def bigTests: Boolean

  def useCloud: Boolean

  def defaultPersistLevel = StorageLevel.MEMORY_AND_DISK

  val driverLocalDir = if (bigTests) "/Volumes/WORKDISK/scratch/" else "/scratch/"

  System.setProperty("java.io.tmpdir",driverLocalDir)
  System.setProperty("spark.driver.allowMultipleContexts", "true");

  lazy val sconf = {

    val nconf = if(useLocal) {
      new SparkConf().
        setMaster("local[4]").
        set("spark.local.dir",driverLocalDir)
    } else {
      new SparkConf().
        setMaster("spark://merlinc60:7077"). //"spark://MacBook-Air.local:7077"
        set("java.io.tmpdir","/scratch/").
        set("spark.local.dir","/scratch/")
    }
    nconf.
      set("spark.executor.memory", "4g").
      setAppName(this.getClass().getCanonicalName)
  }
  lazy val sc = {
    println(sconf.toDebugString)
    var tsc = new SparkContext(sconf)
    if (!useLocal) {
      SparkContext.jarOfClass(this.getClass()) match {
        case Some(jarFile) =>
          println("Adding "+jarFile)
          tsc.addJar(jarFile)
        case None =>
          println(this.getClass()+" jar file missing")
      }
      tsc.addJar("/Users/mader/Dropbox/Informatics/spark-imageio/assembly/target/spio-assembly-0.1-SNAPSHOT.jar")
      tsc.addJar("/Users/mader/Dropbox/Informatics/spark-imageio/target/spark-imageio-1.0-SNAPSHOT-tests.jar")
    }
    if (useCloud) {
      tsc.hadoopConfiguration.set("fs.s3n.awsAccessKeyId", "AKIAJM4PPKISBYXFZGKA")
      tsc.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey",
        "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")
    }
    tsc
  }

  lazy val testDataDir = if(useLocal) {
    "/Users/mader/Dropbox/Informatics/spark-imageio/test-data/"
  } else {
    "/scratch/"
  }

  lazy val esriImage = if (!useCloud) {
    testDataDir + "Hansen_GFC2014_lossyear_00N_000E.tif"
  } else {
    "s3n://geo-images/"+"Hansen_GFC2014_lossyear_00N_000E.tif"
  }


  def makeSimpleTiledImageHelper(imgPath: String, tileWidth: Int = 10, tileHeight: Int = 25,
  partitions: Int = 80)(implicit aim: ArrayImageMapping[Double]) =
    makeSimpleTiledImageHelperGeneric[Double](imgPath,
    tileWidth, tileHeight,partitions)

  def makeSimpleTiledImageHelperGeneric[T : ArrayImageMapping](imgPath: String,
                                           tileWidth: Int = 10, tileHeight: Int = 25,
                                 partitions: Int = 80)(implicit nm: Numeric[T],
    ct: ClassTag[T], cttt: ClassTag[Array[Array[T]]], ctt: ClassTag[Array[T]]) = {
    import TilingStrategies.Grid._


    val tiledImage = sc.readTiledImage[T](imgPath, tileWidth, tileHeight, partitions).
      persist(defaultPersistLevel)

    val fTile = tiledImage.first

    import BreezeOps._
    import Positions._

    (tiledImage,tiledImage.
      sparseThresh(_>0).persist(defaultPersistLevel))
  }



  def makeSimpleTiledImage(tileWidth: Int = 10, tileHeight: Int = 25,
                           partitions: Int = 80)(implicit
  aim: ArrayImageMapping[Double]) = {
    val imgPath = ImageTestFunctions.makeImagePath(50,50,"tif",
      testDataDir)
    makeSimpleTiledImageHelper(imgPath,tileWidth,tileHeight,partitions)
  }

  def makeRegionTiledImage(tileWidth: Int = 10, tileHeight: Int = 25,
                           partitions: Int = 80, xdim: Int = 80, ydim: Int = 80)(
    implicit aim: ArrayImageMapping[Double]
    ) = {
    val imgPath = ImageTestFunctions.makeImagePathRegions(xdim,ydim,"tif",
      testDataDir)
    makeSimpleTiledImageHelper(imgPath,tileWidth,tileHeight,partitions)
  }






}
