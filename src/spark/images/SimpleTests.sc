package spark.images


import tipl.tests._
import tipl.util.TImgTools
import tipl.util.TIPLOps._
import tipl.spark.SparkGlobal

import tipl.formats.TImgRO

import java.io.Serializable
import tipl.util.D3int
import tipl.util.TImgTools.HasDimensions
import tipl.util.D3float

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.PairRDDFunctions._

trait TImgROLike extends TImgRO {
  val dim: D3int
  val pos: D3int
  val offset: D3int
  val elSize: D3float
  override def getDim = {dim}
  override def getPos = {pos}
  override def getOffset = {offset}
  override def getElSize = {elSize}
}



import tipl.util.TIPLOps._

object SimpleTests {
	
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
  val x: Long = java.lang.Long.valueOf(1)         //> x  : Long = 1
  val testImg = TestPosFunctions.wrapItAs(10,
				new TestPosFunctions.DiagonalPlaneAndDotsFunction(),TImgTools.IMAGETYPE_INT);
                                                  //> testImg  : tipl.formats.TImgRO = tipl.formats.PureFImage@2fb02a96

	//testImg shapeAnalysis "test.csv"
	//val output= testImg.kvoronoi(testImg)
	
	
  val jsc = SparkGlobal.getContext("FunnyTests")  //> 14/07/24 18:21:16 INFO SparkConf: Using Spark's default log4j profile: org/
                                                  //| apache/spark/log4j-defaults.properties
                                                  //| 14/07/24 18:21:16 WARN SparkConf: In Spark 1.0 and later spark.local.dir wi
                                                  //| ll be overridden by the value set by the cluster manager (via SPARK_LOCAL_D
                                                  //| IRS in mesos/standalone and LOCAL_DIRS in YARN).
                                                  //| 14/07/24 18:21:17 INFO SecurityManager: Changing view acls to: mader
                                                  //| 14/07/24 18:21:17 INFO SecurityManager: SecurityManager: authentication dis
                                                  //| abled; ui acls disabled; users with view permissions: Set(mader)
                                                  //| 14/07/24 18:21:18 INFO Slf4jLogger: Slf4jLogger started
                                                  //| 14/07/24 18:21:18 INFO Remoting: Starting remoting
                                                  //| 14/07/24 18:21:18 INFO Remoting: Remoting started; listening on addresses :
                                                  //| [akka.tcp://spark@public-docking-etx-0150.ethz.ch:50899]
                                                  //| 14/07/24 18:21:18 INFO Remoting: Remoting now listens on addresses: [akka.t
                                                  //| cp://spark@public-docking-etx-0150.ethz.ch:50899]
                                                  //| 14/07/24 18:21:18 INFO SparkEnv: 
                                                  //| Output exceeds cutoff limit.

  val imFolder = jsc.sc.byteFolder("/Users/mader/Dropbox/tipl/test/io_tests/rec8bit/*.tif")
                                                  //> 14/07/24 18:21:20 INFO MemoryStore: ensureFreeSpace(177830) called with cur
                                                  //| Mem=0, maxMem=572679782
                                                  //| 14/07/24 18:21:20 INFO MemoryStore: Block broadcast_0 stored as values to m
                                                  //| emory (estimated size 173.7 KB, free 546.0 MB)
                                                  //| imFolder  : org.apache.spark.rdd.RDD[(String, Array[Byte])] = NewHadoopRDD[
                                                  //| 0] at newAPIHadoopFile at TIPLOps.scala:46
  val fSlice = imFolder.first                     //> 14/07/24 18:21:20 INFO FileInputFormat: Total input paths to process : 5
                                                  //| 14/07/24 18:21:20 INFO CombineFileInputFormat: DEBUG: Terminated node alloc
                                                  //| ation with : CompletedNodes: 1, size left: 545820
                                                  //| 14/07/24 18:21:20 INFO SparkContext: Starting job: first at spark.images.Si
                                                  //| mpleTests.scala:49
                                                  //| 14/07/24 18:21:20 INFO DAGScheduler: Got job 0 (first at spark.images.Simpl
                                                  //| eTests.scala:49) with 1 output partitions (allowLocal=true)
                                                  //| 14/07/24 18:21:20 INFO DAGScheduler: Final stage: Stage 0(first at spark.im
                                                  //| ages.SimpleTests.scala:49)
                                                  //| 14/07/24 18:21:20 INFO DAGScheduler: Parents of final stage: List()
                                                  //| 14/07/24 18:21:20 INFO DAGScheduler: Missing parents: List()
                                                  //| 14/07/24 18:21:20 INFO DAGScheduler: Computing the requested partition loca
                                                  //| lly
                                                  //| 14/07/24 18:21:20 INFO NewHadoopRDD: Input split: Paths:/Users/mader/Dropbo
                                                  //| x/tipl/test/io_tests/rec8bit/t4d_c01_afterHoleRM_cropUlm_0043.tif:0+109164,
                                                  //| /Users/mader/Dropbox/tipl/
                                                  //| Output exceeds cutoff limit.
  fSlice._1                                       //> res0: String = file:/Users/mader/Dropbox/tipl/test/io_tests/rec8bit/t4d_c01
                                                  //| _afterHoleRM_cropUlm_0043.tif
  fSlice._2.length                                //> res1: Int = 109164
  
  val rimFolder = imFolder.toTiffSlices           //> 14/07/24 18:21:21 INFO SparkContext: Starting job: first at TiffIO.scala:53
                                                  //| 
                                                  //| 14/07/24 18:21:21 INFO DAGScheduler: Got job 1 (first at TiffIO.scala:53) w
                                                  //| ith 1 output partitions (allowLocal=true)
                                                  //| 14/07/24 18:21:21 INFO DAGScheduler: Final stage: Stage 1(first at TiffIO.s
                                                  //| cala:53)
                                                  //| 14/07/24 18:21:21 INFO DAGScheduler: Parents of final stage: List()
                                                  //| 14/07/24 18:21:21 INFO DAGScheduler: Missing parents: List()
                                                  //| 14/07/24 18:21:21 INFO DAGScheduler: Computing the requested partition loca
                                                  //| lly
                                                  //| 14/07/24 18:21:21 INFO NewHadoopRDD: Input split: Paths:/Users/mader/Dropbo
                                                  //| x/tipl/test/io_tests/rec8bit/t4d_c01_afterHoleRM_cropUlm_0043.tif:0+109164,
                                                  //| /Users/mader/Dropbox/tipl/test/io_tests/rec8bit/t4d_c01_afterHoleRM_cropUlm
                                                  //| _0044.tif:0+109164,/Users/mader/Dropbox/tipl/test/io_tests/rec8bit/t4d_c01_
                                                  //| afterHoleRM_cropUlm_0045.tif:0+109164,/Users/mader/Dropbox/tipl/test/io_tes
                                                  //| ts/rec8bit/t4d_c01_afterHoleRM_cropUlm_0046.tif:0+1
                                                  //| Output exceeds cutoff limit.
  import tipl.util.TIPLOps._
	val t = new ImageFriendlySparkContext(jsc.sc)
                                                  //> t  : tipl.util.TIPLOps.ImageFriendlySparkContext = tipl.util.TIPLOps$ImageF
                                                  //| riendlySparkContext@6b6faef7
val pathS3 = "s3n://AKIAJCRWDNQANLUY4UBQ:9pSm22K8ycmQC444SwbFWXfp1Tuiqq+5K9auzJF+@4quant-images/Spheroid1/DAPI/*.tif"
                                                  //> pathS3  : String = s3n://AKIAJCRWDNQANLUY4UBQ:9pSm22K8ycmQC444SwbFWXfp1Tuiq
                                                  //| q+5K9auzJF+@4quant-images/Spheroid1/DAPI/*.tif
val testPath = "/Users/mader/Dropbox/tipl/test/io_tests/rec8bit/*.tif"
                                                  //> testPath  : String = /Users/mader/Dropbox/tipl/test/io_tests/rec8bit/*.tif
val largePath = "/Volumes/MacDisk/AeroFS/VardanData/ForKevin_VA/3D/Spheroid1/DAPI/*.tif"
                                                  //> largePath  : String = /Volumes/MacDisk/AeroFS/VardanData/ForKevin_VA/3D/Sph
                                                  //| eroid1/DAPI/*.tif

val b = t.byteFolder(testPath)                    //> 14/07/24 18:21:21 INFO MemoryStore: ensureFreeSpace(177830) called with cur
                                                  //| Mem=177830, maxMem=572679782
                                                  //| 14/07/24 18:21:21 INFO MemoryStore: Block broadcast_1 stored as values to m
                                                  //| emory (estimated size 173.7 KB, free 545.8 MB)
                                                  //| b  : org.apache.spark.rdd.RDD[(String, Array[Byte])] = NewHadoopRDD[2] at n
                                                  //| ewAPIHadoopFile at TIPLOps.scala:46
val g = rddToUnReadRDD(b).toTiffSlices            //> 14/07/24 18:21:21 INFO FileInputFormat: Total input paths to process : 5
                                                  //| 14/07/24 18:21:21 INFO CombineFileInputFormat: DEBUG: Terminated node alloc
                                                  //| ation with : CompletedNodes: 1, size left: 545820
                                                  //| 14/07/24 18:21:21 INFO SparkContext: Starting job: first at TiffIO.scala:53
                                                  //| 
                                                  //| 14/07/24 18:21:21 INFO DAGScheduler: Got job 2 (first at TiffIO.scala:53) w
                                                  //| ith 1 output partitions (allowLocal=true)
                                                  //| 14/07/24 18:21:21 INFO DAGScheduler: Final stage: Stage 2(first at TiffIO.s
                                                  //| cala:53)
                                                  //| 14/07/24 18:21:21 INFO DAGScheduler: Parents of final stage: List()
                                                  //| 14/07/24 18:21:21 INFO DAGScheduler: Missing parents: List()
                                                  //| 14/07/24 18:21:21 INFO DAGScheduler: Computing the requested partition loca
                                                  //| lly
                                                  //| 14/07/24 18:21:21 INFO NewHadoopRDD: Input split: Paths:/Users/mader/Dropbo
                                                  //| x/tipl/test/io_tests/rec8bit/t4d_c01_afterHoleRM_cropUlm_0043.tif:0+109164,
                                                  //| /Users/mader/Dropbox/tipl/test/io_tests/rec8bit/t4d_c01_afterHoleRM_cropUlm
 val allImages = g.loadAsValues.getBaseImg.rdd.map(_._2.get).flatMap(p => p).stats
                                                  //> 14/07/24 18:21:21 INFO SparkContext: Starting job: count at TIPLOps.scala:9
                                                  //| 3
                                                  //| 14/07/24 18:21:21 INFO DAGScheduler: Got job 3 (count at TIPLOps.scala:93) 
                                                  //| with 1 output partitions (allowLocal=false)
                                                  //| 14/07/24 18:21:21 INFO DAGScheduler: Final stage: Stage 3(count at TIPLOps.
                                                  //| scala:93)
                                                  //| 14/07/24 18:21:21 INFO DAGScheduler: Parents of final stage: List()
                                                  //| 14/07/24 18:21:21 INFO DAGScheduler: Missing parents: List()
                                                  //| 14/07/24 18:21:21 INFO DAGScheduler: Submitting Stage 3 (MappedValuesRDD[3]
                                                  //|  at mapValues at TiffIO.scala:55), which has no missing parents
                                                  //| 14/07/24 18:21:21 INFO DAGScheduler: Submitting 1 missing tasks from Stage 
                                                  //| 3 (MappedValuesRDD[3] at mapValues at TiffIO.scala:55)
                                                  //| 14/07/24 18:21:21 INFO TaskSchedulerImpl: Adding task set 3.0 with 1 tasks
                                                  //| 14/07/24 18:21:21 INFO TaskSetManager: Starting task 3.0:0 as TID 0 on exec
                                                  //| utor localhost: localhost (PROCESS_LOCAL)
                                                  //| 14/07/24 18:21:21 INFO TaskSetManager: Seri
                                                  //| Output exceeds cutoff limit.

}