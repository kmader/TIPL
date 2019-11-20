package fourquant

import java.io.File
import javax.imageio.ImageIO

import fourquant.arrays.{BreezeOps, Positions}
import fourquant.io.BufferedImageOps
import fourquant.io.BufferedImageOps.ArrayImageMapping
import fourquant.io.ImageIOOps._
import fourquant.labeling.ConnectedComponents
import fourquant.labeling.ConnectedComponents.LabelCriteria
import fourquant.shape.EllipsoidAnalysis
import fourquant.shape.EllipsoidAnalysis.Indexable
import fourquant.tiles.TilingStrategies
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkConf, SparkContext}

import scala.reflect.ClassTag

/**
 * Created by mader on 4/15/15.
 */
class demo  {
  def useLocal: Boolean = true

  def useCloud: Boolean = false

  def bigTests: Boolean = false

  def defaultPersistLevel = StorageLevel.MEMORY_AND_DISK

  System.setProperty("java.io.tmpdir","/scratch/")
  lazy val localPath = if(bigTests) {
    "/Volumes/WORKDISK/scratch/"
  } else {
    "/scratch/"
  }

  lazy val sconf = {

    val nconf = if(useLocal) {
      new SparkConf().setMaster("local[4]").set("spark.local.dir",localPath)
    } else {
      new SparkConf(). //"spark://MacBook-Air.local:7077"
        set("java.io.tmpdir","/scratch/").set("spark.local.dir","/scratch/")
    }
    nconf.
      //set("spark.executor.memory", "20g").
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
          println("Jar File missing")
      }
      //tsc.addJar("/Users/mader/Dropbox/Informatics/spark-imageio/assembly/target/spio-assembly
      // -0.1-SNAPSHOT.jar")
      //tsc.addJar("/Users/mader/Dropbox/Informatics/spark-imageio/target/spark-imageio-1" +
       // ".0-SNAPSHOT-tests.jar")
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

  def run() = {
    import fourquant.io.BufferedImageOps.implicits.charImageSupport
    val (tileImage,nonZeroEntries) = makeSimpleTiledImageHelperGeneric[Char](esriImage,256,256,120)

    tileImage.foreach{
      case((filename,x,y),img) =>
        val outPath = testDataDir+File.separator+"tiles"+
          File.separator
        new java.io.File(outPath).mkdirs()

        val bm = BufferedImageOps.fromArrayToImage(img)
          val outImageName = x.toString+"_"+y.toString+".png"
        ImageIO.write(bm,"png",new File(outPath+outImageName))
    }

    val nzCount = nonZeroEntries.count
    implicit val doubleLabelCrit = new LabelCriteria[Double] {
      override def matches(a: Double, b: Double): Boolean = Math.abs(a-b)<0.75 // match only
      // points with the same value
    }
    import Positions._
    val compLabel = ConnectedComponents.Labeling2DChunk(nonZeroEntries, (1, 1))
    val components = compLabel.map(_._2._1).countByValue()

    println(("Non-Zero Count", nzCount, "Components", components.size))
    print("Components:" + components.mkString(", "))


    implicit val obvInd = new Indexable[(Long,Double)] {
      override def toIndex(a: (Long,Double)): Long = a._1
    }
    val shapeAnalysis = EllipsoidAnalysis.runIntensityShapeAnalysis(compLabel)

    val shapeInformation = shapeAnalysis.collect

    println(shapeInformation.mkString("\n"))


    val areaInfo = shapeInformation.map(_.area)
    print(areaInfo.mkString(", "))

    val sqlContext = new org.apache.spark.sql.SQLContext(sc)


  }
}

object demo {

  def main(args: Array[String]): Unit = {
    new demo().run()
  }


}
