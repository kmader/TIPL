package fourquant

import fourquant.io.ImageIOOps
import fourquant.utils.SilenceLogs
import org.scalatest.{FunSuite, Matchers}

/**
 * Created by mader on 4/17/15.
 */
class SimpleCloudTests extends FunSuite with Matchers with ImageSparkInstance with SilenceLogs {
  override def useLocal: Boolean = false

  override def bigTests: Boolean = false

  override def useCloud: Boolean = true
  test("Get driver readers") {
    val testFile = sc.binaryFiles("s3n://geo-images/test/regions_1600_1600.tif").first()._2
    val readerList = ImageIOOps.getReaderList(ImageIOOps.createStream(testFile.open()),Some("tif"))
    println(readerList.mkString(","))
    readerList.length should be >= 2
  }

test("Get Worker Readers") {

  val simpleList = sc.parallelize(1 to 4,4)
  var testFile = sc.binaryFiles("s3n://geo-images/test/regions_1600_1600.tif")
   if (false) testFile = testFile.cartesian(simpleList).map(a => (a._1._1,a._1._2))
      val allInfo = testFile.mapValues{
    pdsObj =>
      ImageIOOps.getReaderList(ImageIOOps.createStream(pdsObj.open()),Some("tif")).map(_.toString)
  }.collect()

  println(allInfo.map(_._2.mkString(",")).mkString("\n"))
}

  var testImages = collection.mutable.ArrayBuffer[String](
    "s3n://geo-images/test/regions_1600_1600.tif")
  if (bigTests) testImages ++= Seq("s3n://geo-images/*.tif")
  for (curImg <- testImages) {
    test("Image Shape Analysis:"+curImg) {
      // setup s3
      sc.hadoopConfiguration.set("fs.s3n.awsAccessKeyId", "AKIAJM4PPKISBYXFZGKA")
      sc.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey","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")
      // import
      import fourquant.arrays.BreezeOps._
      import fourquant.arrays.Positions._
      import fourquant.io.BufferedImageOps.implicits._
      import fourquant.io.ImageIOOps._
      import fourquant.labeling.ConnectedComponents
      import fourquant.labeling.ConnectedComponents.LabelCriteria
      import fourquant.shape.EllipsoidAnalysis
      import fourquant.shape.EllipsoidAnalysis.Indexable
      import fourquant.tiles.TilingStrategies._
      import Grid._
      implicit val doubleLabelCrit = new LabelCriteria[Double] {
        override def matches(a: Double, b: Double): Boolean = Math.abs(a-b)<0.75 // match only
        // points with the same value
      }
      implicit val obvInd = new Indexable[(Long,Double)] {
        override def toIndex(a: (Long,Double)): Long = a._1
      }

      // Read in the image data, now from a local store, but can just as easily be read from s3 or anywhere else
      val geoImage = sc.readTiledImage[Char](curImg,
        400,400, 100)
      //val geoImage = sc.readTiledImage[Char](testDataDir+"regions_1600_1600.tif",100,100,50)
      // Segment out all of the non-zero points
      val pointImage = geoImage.sparseThresh(_>0).cache
      val ptCount = pointImage.count()
      println("Total Points:"+ptCount)

      ptCount.toInt should be > 0


      // Label each chunk using connected component labeling with a 3 x 3 window
      val uniqueRegions = ConnectedComponents.Labeling2DChunk(pointImage)
      val regCount = uniqueRegions.count()
      regCount.toInt should be > 0

      // Run a shape analysis to measure the position, area, and intensity of each identified region
      val shapeAnalysis = EllipsoidAnalysis.runIntensityShapeAnalysis(uniqueRegions)
      val avgArea = shapeAnalysis.map(_.area).mean()
      avgArea should be > 1.0

      println((curImg,"pt:",ptCount,"regions:",regCount,"area:",avgArea))
    }
  }

}
