package spark.images

import tipl.spark._
import tipl.tests._
import tipl.util.TImgTools

object SimpleTests {
  println("Welcome to the Scala worksheet")
  //> Welcome to the Scala worksheet
  val x: Long = java.lang.Long.valueOf(1)
  //> x  : Long = 1
  val testImg = TestPosFunctions.wrapItAs(10,
    new TestPosFunctions.DiagonalPlaneAndDotsFunction(), TImgTools.IMAGETYPE_INT);
  //> testImg  : tipl.formats.TImgRO = tipl.formats.PureFImage@14bea551
  val jsc = SparkGlobal.getContext("FunnyTests") //> 14/06/12 21:02:21 INFO SparkConf: Using Spark's default log4j profile: org/a
  //| pache/spark/log4j-defaults.properties
  //| 14/06/12 21:02:21 WARN SparkConf: In Spark 1.0 and later spark.local.dir wil
  //| l be overridden by the value set by the cluster manager (via SPARK_LOCAL_DIR
  //| S in mesos/standalone and LOCAL_DIRS in YARN).
  //| 14/06/12 21:02:22 WARN Utils: Your hostname, kbook.local resolves to a loopb
  //| ack address: 127.0.0.1, but we couldn't find any external IP address!
  //| 14/06/12 21:02:22 WARN Utils: Set SPARK_LOCAL_IP if you need to bind to anot
  //| her address
  //| 14/06/12 21:02:22 INFO SecurityManager: SecurityManager, is authentication e
  //| nabled: false are ui acls enabled: false users with view permissions: Set(ma
  //| der)
  //| 14/06/12 21:02:23 INFO Slf4jLogger: Slf4jLogger started
  //| 14/06/12 21:02:24 INFO Remoting: Starting remoting
  //| 14/06/12 21:02:24 INFO Remoting: Remoting started; listening on addresses :[
  //| akka.tcp://spark@localhost:53017]
  //| 14/
  //| Output exceeds cutoff limit.

  val dtestImg = DTImg.ConvertTImg(jsc, testImg, TImgTools.IMAGETYPE_INT)
  //> dtestImg  : tipl.spark.DTImg[Nothing] = tipl.spark.DTImg@4e9e7777

  val fdim = dtestImg.getBaseImg.first
  //> 14/06/12 21:02:45 INFO SparkContext: Starting job: first at spark.images.Sim
  //| pleTests.scala:15
  //| 14/06/12 21:02:45 INFO DAGScheduler: Got job 0 (first at spark.images.Simple
  //| Tests.scala:15) with 1 output partitions (allowLocal=true)
  //| 14/06/12 21:02:45 INFO DAGScheduler: Final stage: Stage 0 (first at spark.im
  //| ages.SimpleTests.scala:15)
  //| 14/06/12 21:02:45 INFO DAGScheduler: Parents of final stage: List()
  //| 14/06/12 21:02:45 INFO DAGScheduler: Missing parents: List()
  //| 14/06/12 21:02:45 INFO DAGScheduler: Computing the requested partition local
  //| ly
  //| 14/06/12 21:02:45 INFO SparkContext: Job finished: first at spark.images.Sim
  //| pleTests.scala:15, took 0.059522 s
  //| fdim  : (tipl.util.D3int, tipl.util.TImgBlock[Nothing]) = (0,0,0,TBF:sl=0,ob
  //| j=tipl.formats.PureFImage@14bea551)
  val kvImg = dtestImg.asKV() //> kvImg  : tipl.spark.KVImg[_ <: Number] = tipl.spark.KVImg@61ad39


  //val kvCount = kvImg.getBaseImg().count

  //val kvOut = kvImg.getBaseImg().first
}