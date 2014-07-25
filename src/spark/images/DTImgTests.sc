package spark.images


import tipl.tests._
import tipl.util.TImgTools
import tipl.util.TIPLOps._
import tipl.spark.IOOps._
import tipl.spark.DTImgOps._
import tipl.spark.DTImg
import tipl.spark.SparkGlobal

import tipl.formats.TImgRO

import java.io.Serializable
import tipl.util.D3int
import tipl.util.TImgTools.HasDimensions
import tipl.util.D3float

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.PairRDDFunctions._

object DTImgTests {
 val x: Long = java.lang.Long.valueOf(1)          //> x  : Long = 1
  val testImg = TestPosFunctions.wrapItAs(10,
				new TestPosFunctions.DiagonalPlaneAndDotsFunction(),TImgTools.IMAGETYPE_INT)
                                                  //> testImg  : tipl.formats.TImgRO = tipl.formats.PureFImage@42ab23aa
				
	
	val dtTool = DTImg.ConvertTImg(SparkGlobal.getContext,testImg,TImgTools.IMAGETYPE_INT)
                                                  //> 14/07/25 11:51:17 INFO SparkConf: Using Spark's default log4j profile: org/a
                                                  //| pache/spark/log4j-defaults.properties
                                                  //| 14/07/25 11:51:17 WARN SparkConf: In Spark 1.0 and later spark.local.dir wil
                                                  //| l be overridden by the value set by the cluster manager (via SPARK_LOCAL_DIR
                                                  //| S in mesos/standalone and LOCAL_DIRS in YARN).
                                                  //| 14/07/25 11:51:17 INFO SecurityManager: Changing view acls to: mader
                                                  //| 14/07/25 11:51:17 INFO SecurityManager: SecurityManager: authentication disa
                                                  //| bled; ui acls disabled; users with view permissions: Set(mader)
                                                  //| 14/07/25 11:51:18 INFO Slf4jLogger: Slf4jLogger started
                                                  //| 14/07/25 11:51:18 INFO Remoting: Starting remoting
                                                  //| 14/07/25 11:51:18 INFO Remoting: Remoting started; listening on addresses :[
                                                  //| akka.tcp://spark@10.104.3.238:55999]
                                                  //| 14/07/25 11:51:18 INFO Remoting: Remoting now listens on addresses: [akka.tc
                                                  //| p://spark@10.104.3.238:55999]
                                                  //| 14/07/25 11:51:18 INFO SparkEnv: Registering MapOutputTracker
                                                  //| 14/07/25 11:51:18 IN
                                                  //| Output exceeds cutoff limit.

    val outArr = dtTool.getPolyImage(1,TImgTools.IMAGETYPE_INT)
                                                  //> 14/07/25 11:51:20 INFO SparkContext: Starting job: first at DTImg.java:447
                                                  //| 14/07/25 11:51:20 INFO DAGScheduler: Got job 1 (first at DTImg.java:447) wit
                                                  //| h 1 output partitions (allowLocal=true)
                                                  //| 14/07/25 11:51:20 INFO DAGScheduler: Final stage: Stage 1(first at DTImg.jav
                                                  //| a:447)
                                                  //| 14/07/25 11:51:20 INFO DAGScheduler: Parents of final stage: List()
                                                  //| 14/07/25 11:51:20 INFO DAGScheduler: Missing parents: List()
                                                  //| 14/07/25 11:51:20 INFO DAGScheduler: Computing the requested partition local
                                                  //| ly
                                                  //| 14/07/25 11:51:20 INFO SparkContext: Job finished: first at DTImg.java:447, 
                                                  //| took 0.009063 s
                                                  //| 14/07/25 11:51:20 INFO SparkContext: Starting job: first at DTImg.java:447
                                                  //| 14/07/25 11:51:20 INFO DAGScheduler: Got job 2 (first at DTImg.java:447) wit
                                                  //| h 9 output partitions (allowLocal=true)
                                                  //| 14/07/25 11:51:20 INFO DAGScheduler: Final stage: Stage 2(first at DTImg.jav
                                                  //| a:447)
                                                  //| 14/07/25 11:51:20 INFO DAGScheduler: Parents of final stage: List()
                                                  //| 14/07/
                                                  //| Output exceeds cutoff limit.
    val nxtArr = outArr.asInstanceOf[Array[Int]]  //> nxtArr  : Array[Int] = Array(1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1,
                                                  //|  0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 
                                                  //| 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1
                                                  //| , 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1,
                                                  //|  0, 1, 0, 1, 0, 1, 0, 1)
    
    println("CurVal:"+nxtArr.mkString(","))       //> CurVal:1,1,1,0,1,0,1,0,1,0,1,1,0,1,0,1,0,1,0,1,1,0,1,0,1,0,1,0,1,0,0,1,0,1,0
                                                  //| ,1,0,1,0,1,1,0,1,0,1,0,1,0,1,0,0,1,0,1,0,1,0,1,0,1,1,0,1,0,1,0,1,0,1,0,0,1,0
                                                  //| ,1,0,1,0,1,0,1,1,0,1,0,1,0,1,0,1,0,0,1,0,1,0,1,0,1,0,1
}