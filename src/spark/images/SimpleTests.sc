package spark.images

//import tipl.spark._
import tipl.tests._
import tipl.util.TImgTools

import tipl.formats.TImgRO

import java.io.Serializable
import tipl.util.D3int
import tipl.util.TImgTools.HasDimensions
import tipl.util.D3float

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


import tipl.spark.ETImg._

object SimpleTests {
	
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
  val x: Long = java.lang.Long.valueOf(1)         //> x  : Long = 1
  val testImg = TestPosFunctions.wrapItAs(10,
				new TestPosFunctions.DiagonalPlaneAndDotsFunction(),TImgTools.IMAGETYPE_INT);
                                                  //> testImg  : tipl.formats.TImgRO = tipl.formats.PureFImage@4f9380c1
	val testF = Float.MaxValue                //> testF  : Float = 3.4028235E38
	var testR = testF+1                       //> testR  : Float = 3.4028235E38
	testImg shapeAnalysis "test.csv"          //> Fri Aug 01 00:00:00 PDT 2014 , Tue Jul 01 01:15:30 PDT 2014 : false
                                                  //| 14/07/01 01:15:30 INFO SparkConf: Using Spark's default log4j profile: org/a
                                                  //| pache/spark/log4j-defaults.properties
                                                  //| 14/07/01 01:15:30 WARN SparkConf: In Spark 1.0 and later spark.local.dir wil
                                                  //| l be overridden by the value set by the cluster manager (via SPARK_LOCAL_DIR
                                                  //| S in mesos/standalone and LOCAL_DIRS in YARN).
                                                  //| 14/07/01 01:15:30 INFO SecurityManager: SecurityManager, is authentication e
                                                  //| nabled: false are ui acls enabled: false users with view permissions: Set(ma
                                                  //| der)
                                                  //| 14/07/01 01:15:31 INFO Slf4jLogger: Slf4jLogger started
                                                  //| 14/07/01 01:15:31 INFO Remoting: Starting remoting
                                                  //| 14/07/01 01:15:31 INFO Remoting: Remoting started; listening on addresses :[
                                                  //| akka.tcp://spark@10.1.10.188:63765]
                                                  //| 14/07/01 01:15:31 INFO Remoting: Remoting now listens on addresses: [akka.tc
                                                  //| p://spark@10.1.10.188:63765]
                                                  //| 14/07/01 01:15:31 INFO SparkEnv: Registering MapOutputTracker
                                                  //| 1
                                                  //| Output exceeds cutoff limit.
	//val output= testImg.kvoronoi(testImg)
	
  //val jsc = SparkGlobal.getContext("FunnyTests")
  
	//val dtestImg = DTImg.ConvertTImg(jsc,testImg,TImgTools.IMAGETYPE_INT)
		
	//val kvCount = kvImg.getBaseImg().count

  //val kvOut = kvImg.getBaseImg().first
}