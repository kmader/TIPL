package spark.images


import tipl.formats.TImgRO
import tipl.spark.IOOps._
import tipl.spark.SparkGlobal
import tipl.tests._
import tipl.util.{D3float, D3int, TImgTools}

trait TImgROLike extends TImgRO {
  val dim: D3int
  val pos: D3int
  val offset: D3int
  val elSize: D3float

  override def getDim = {
    dim
  }

  override def getPos = {
    pos
  }

  override def getOffset = {
    offset
  }

  override def getElSize = {
    elSize
  }
}

object SimpleTests {

  println("Welcome to the Scala worksheet")
  //> Welcome to the Scala worksheet
  val x: Long = java.lang.Long.valueOf(1)
  //> x  : Long = 1
  val testImg = TestPosFunctions.wrapItAs(10,
    new TestPosFunctions.DiagonalPlaneAndDotsFunction(), TImgTools.IMAGETYPE_INT);
  //> testImg  : tipl.formats.TImgRO = tipl.formats.PureFImage@1328fa3c

  //testImg shapeAnalysis "test.csv"
  //val output= testImg.kvoronoi(testImg)


  val jsc = SparkGlobal.getContext("FunnyTests") //> 14/07/25 10:41:39 INFO SparkConf: Using
  // Spark's default log4j profile: org/
  //| apache/spark/log4j-defaults.properties
  //| 14/07/25 10:41:39 WARN SparkConf: In Spark 1.0 and later spark.local.dir wi
  //| ll be overridden by the value set by the cluster manager (via SPARK_LOCAL_D
  //| IRS in mesos/standalone and LOCAL_DIRS in YARN).
  //| 14/07/25 10:41:39 INFO SecurityManager: Changing view acls to: mader
  //| 14/07/25 10:41:39 INFO SecurityManager: SecurityManager: authentication dis
  //| abled; ui acls disabled; users with view permissions: Set(mader)
  //| 14/07/25 10:41:40 INFO Slf4jLogger: Slf4jLogger started
  //| 14/07/25 10:41:40 INFO Remoting: Starting remoting
  //| 14/07/25 10:41:41 INFO Remoting: Remoting started; listening on addresses :
  //| [akka.tcp://spark@10.104.3.238:54787]
  //| 14/07/25 10:41:41 INFO Remoting: Remoting now listens on addresses: [akka.t
  //| cp://spark@10.104.3.238:54787]
  //| 14/07/25 10:41:41 INFO SparkEnv: Registering MapOutputTracker
  //| 14/0
  //| Output exceeds cutoff limit.

  val imFolder = jsc.sc.byteFolder("/Users/mader/Dropbox/tipl/test/io_tests/rec8bit/*.tif")
  //> 14/07/25 10:41:43 INFO MemoryStore: ensureFreeSpace(177830) called with cur
  //| Mem=0, maxMem=572679782
  //| 14/07/25 10:41:43 INFO MemoryStore: Block broadcast_0 stored as values to m
  //| emory (estimated size 173.7 KB, free 546.0 MB)
  //| imFolder  : org.apache.spark.rdd.RDD[(String, Array[Byte])] = NewHadoopRDD[
  //| 0] at newAPIHadoopFile at IOOps.scala:44
  val fSlice = imFolder.first //> 14/07/25 10:41:43 INFO FileInputFormat: Total input paths to
  // process : 5
  //| 14/07/25 10:41:43 INFO CombineFileInputFormat: DEBUG: Terminated node alloc
  //| ation with : CompletedNodes: 1, size left: 545820
  //| 14/07/25 10:41:43 INFO SparkContext: Starting job: first at spark.images.Si
  //| mpleTests.scala:51
  //| 14/07/25 10:41:43 INFO DAGScheduler: Got job 0 (first at spark.images.Simpl
  //| eTests.scala:51) with 1 output partitions (allowLocal=true)
  //| 14/07/25 10:41:43 INFO DAGScheduler: Final stage: Stage 0(first at spark.im
  //| ages.SimpleTests.scala:51)
  //| 14/07/25 10:41:43 INFO DAGScheduler: Parents of final stage: List()
  //| 14/07/25 10:41:43 INFO DAGScheduler: Missing parents: List()
  //| 14/07/25 10:41:43 INFO DAGScheduler: Computing the requested partition loca
  //| lly
  //| 14/07/25 10:41:43 INFO NewHadoopRDD: Input split: Paths:/Users/mader/Dropbo
  //| x/tipl/test/io_tests/rec8bit/t4d_c01_afterHoleRM_cropUlm_0043.tif:0+109164,
  //| /Users/mader/Dropbox/tipl/
  //| Output exceeds cutoff limit.
  fSlice._1 //> res0: String = file:/Users/mader/Dropbox/tipl/test/io_tests/rec8bit/t4d_c01
  //| _afterHoleRM_cropUlm_0043.tif
  fSlice._2.length
  //> res1: Int = 109164

  val rimFolder = imFolder.toTiffSlices //> 14/07/25 10:41:43 INFO SparkContext: Starting job:
  // first at IOOps.scala:54
  //| 14/07/25 10:41:43 INFO DAGScheduler: Got job 1 (first at IOOps.scala:54) wi
  //| th 1 output partitions (allowLocal=true)
  //| 14/07/25 10:41:43 INFO DAGScheduler: Final stage: Stage 1(first at IOOps.sc
  //| ala:54)
  //| 14/07/25 10:41:43 INFO DAGScheduler: Parents of final stage: List()
  //| 14/07/25 10:41:43 INFO DAGScheduler: Missing parents: List()
  //| 14/07/25 10:41:43 INFO DAGScheduler: Computing the requested partition loca
  //| lly
  //| 14/07/25 10:41:43 INFO NewHadoopRDD: Input split: Paths:/Users/mader/Dropbo
  //| x/tipl/test/io_tests/rec8bit/t4d_c01_afterHoleRM_cropUlm_0043.tif:0+109164,
  //| /Users/mader/Dropbox/tipl/test/io_tests/rec8bit/t4d_c01_afterHoleRM_cropUlm
  //| _0044.tif:0+109164,/Users/mader/Dropbox/tipl/test/io_tests/rec8bit/t4d_c01_
  //| afterHoleRM_cropUlm_0045.tif:0+109164,/Users/mader/Dropbox/tipl/test/io_tes
  //| ts/rec8bit/t4d_c01_afterHoleRM_cropUlm_0046.tif:0+109164,/Us
  //| Output exceeds cutoff limit.

  val t = new ImageFriendlySparkContext(jsc.sc)
  //> t  : tipl.spark.IOOps.ImageFriendlySparkContext = tipl.spark.IOOps$ImageFri
  //| endlySparkContext@43257214
  val pathS3 = "s3n://AKIAJCRWDNQANLUY4UBQ:9pSm22K8ycmQC444SwbFWXfp1Tuiqq+5K9auzJF+@4quant-images" +
    "/Spheroid1/DAPI/*.tif"
  //> pathS3  : String = s3n://AKIAJCRWDNQANLUY4UBQ:9pSm22K8ycmQC444SwbFWXfp1Tuiq
  //| q+5K9auzJF+@4quant-images/Spheroid1/DAPI/*.tif
  val testPath = "/Users/mader/Dropbox/tipl/test/io_tests/rec8bit/*.tif"
  //> testPath  : String = /Users/mader/Dropbox/tipl/test/io_tests/rec8bit/*.tif
  val largePath = "/Volumes/MacDisk/AeroFS/VardanData/ForKevin_VA/3D/Spheroid1/DAPI/*.tif"
  //> largePath  : String = /Volumes/MacDisk/AeroFS/VardanData/ForKevin_VA/3D/Sph
  //| eroid1/DAPI/*.tif

  val b = t.byteFolder(testPath) //> 14/07/25 10:41:44 INFO MemoryStore: ensureFreeSpace(177830)
  // called with cur
  //| Mem=177830, maxMem=572679782
  //| 14/07/25 10:41:44 INFO MemoryStore: Block broadcast_1 stored as values to m
  //| emory (estimated size 173.7 KB, free 545.8 MB)
  //| b  : org.apache.spark.rdd.RDD[(String, Array[Byte])] = NewHadoopRDD[2] at n
  //| ewAPIHadoopFile at IOOps.scala:44
  //val g = rddToUnReadRDD(b).toTiffSlices
  //val allImages = g.loadAsValues.getBaseImg.rdd.map(_._2.get).flatMap(p => p).stats

}