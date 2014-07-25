package spark.images

import tipl.tests._
import tipl.util.TImgTools
import tipl.util.TIPLOps._
import tipl.spark.IOOps._
import tipl.spark.KVImgOps._
import tipl.spark.SparkGlobal

import tipl.formats.TImgRO

import java.io.Serializable
import tipl.util.D3int
import tipl.util.TImgTools.HasDimensions
import tipl.util.D3float

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.PairRDDFunctions._

object KVImgTests {
  val testImg = TestPosFunctions.wrapItAs(10,
				new TestPosFunctions.DiagonalPlaneAndDotsFunction(),TImgTools.IMAGETYPE_INT)
                                                  //> testImg  : tipl.formats.TImgRO = tipl.formats.PureFImage@5b65afa5
				

	val kvTool = testImg.toKV()               //> 14/07/25 18:48:26 INFO SparkConf: Using Spark's default log4j profile: org/a
                                                  //| pache/spark/log4j-defaults.properties
                                                  //| 14/07/25 18:48:26 WARN SparkConf: In Spark 1.0 and later spark.local.dir wil
                                                  //| l be overridden by the value set by the cluster manager (via SPARK_LOCAL_DIR
                                                  //| S in mesos/standalone and LOCAL_DIRS in YARN).
                                                  //| 14/07/25 18:48:27 WARN Utils: Your hostname, kbook.local resolves to a loopb
                                                  //| ack address: 127.0.0.1, but we couldn't find any external IP address!
                                                  //| 14/07/25 18:48:27 WARN Utils: Set SPARK_LOCAL_IP if you need to bind to anot
                                                  //| her address
                                                  //| 14/07/25 18:48:27 INFO SecurityManager: Changing view acls to: mader
                                                  //| 14/07/25 18:48:27 INFO SecurityManager: SecurityManager: authentication disa
                                                  //| bled; ui acls disabled; users with view permissions: Set(mader)
                                                  //| 14/07/25 18:48:27 INFO Slf4jLogger: Slf4jLogger started
                                                  //| 14/07/25 18:48:27 INFO Remoting: Starting remoting
                                                  //| 14/07/25 18:48:28 INFO Remoting: Remoting started; listening on address
                                                  //| Output exceeds cutoff limit.
     val fval = kvTool.getBaseImg.first           //> 14/07/25 18:48:44 INFO SparkContext: Starting job: first at spark.images.KVI
                                                  //| mgTests.scala:27
                                                  //| 14/07/25 18:48:44 INFO DAGScheduler: Got job 1 (first at spark.images.KVImgT
                                                  //| ests.scala:27) with 1 output partitions (allowLocal=true)
                                                  //| 14/07/25 18:48:44 INFO DAGScheduler: Final stage: Stage 1(first at spark.ima
                                                  //| ges.KVImgTests.scala:27)
                                                  //| 14/07/25 18:48:44 INFO DAGScheduler: Parents of final stage: List()
                                                  //| 14/07/25 18:48:44 INFO DAGScheduler: Missing parents: List()
                                                  //| 14/07/25 18:48:44 INFO DAGScheduler: Computing the requested partition local
                                                  //| ly
                                                  //| 14/07/25 18:48:44 INFO SparkContext: Job finished: first at spark.images.KVI
                                                  //| mgTests.scala:27, took 0.008746 s
                                                  //| fval  : (tipl.util.D3int, Any) = (0,0,0,1)
     
    // val gval = kvTool.toKVLong().getBaseImg.first
    val outArr = kvTool.getPolyImage(1,TImgTools.IMAGETYPE_INT)
                                                  //> 14/07/25 18:48:44 INFO SparkContext: Starting job: sortByKey at KVImg.scala:
                                                  //| 38
                                                  //| 14/07/25 18:48:44 INFO DAGScheduler: Got job 2 (sortByKey at KVImg.scala:38)
                                                  //|  with 10 output partitions (allowLocal=false)
                                                  //| 14/07/25 18:48:44 INFO DAGScheduler: Final stage: Stage 2(sortByKey at KVImg
                                                  //| .scala:38)
                                                  //| 14/07/25 18:48:44 INFO DAGScheduler: Parents of final stage: List()
                                                  //| 14/07/25 18:48:44 INFO DAGScheduler: Missing parents: List()
                                                  //| 14/07/25 18:48:44 INFO DAGScheduler: Submitting Stage 2 (MappedRDD[3] at map
                                                  //|  at KVImg.scala:35), which has no missing parents
                                                  //| 14/07/25 18:48:44 INFO DAGScheduler: Submitting 10 missing tasks from Stage 
                                                  //| 2 (MappedRDD[3] at map at KVImg.scala:35)
                                                  //| 14/07/25 18:48:44 INFO TaskSchedulerImpl: Adding task set 2.0 with 10 tasks
                                                  //| 14/07/25 18:48:44 INFO TaskSetManager: Starting task 2.0:0 as TID 10 on exec
                                                  //| utor localhost: localhost (PROCESS_LOCAL)
                                                  //| 14/07/25 18:48:44 INFO TaskSetManager: Serialized task 2.0:0 as 3483 bytes 
                                                  //| Output exceeds cutoff limit.
    
    
    val nxtArr = outArr.asInstanceOf[Array[Int]]
    
    println("CurVal:"+nxtArr.mkString(","))
}