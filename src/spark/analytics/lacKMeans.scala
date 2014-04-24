/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Random
import org.apache.spark.SparkContext
import org.apache.spark.util.Vector
import org.apache.spark.SparkContext._
System.setProperty("spark.local.dir","/scratch")
System.setProperty("spark.speculation","true")
System.setProperty("spark.executor.memory","4G")
/**
 * K-means clustering.
 */
@serializable object SparkKMeans {
  val R = 1000     // Scaling factor
  val rand = new Random(42)
    
  def parseVector(line: String): Vector = {
    new Vector(line.split(' ').map(_.toDouble))
  }
  
  def closestPoint(p: Vector, centers: Array[Vector]): Int = {
    var index = 0
    var bestIndex = 0
    var closest = Double.PositiveInfinity
  
    for (i <- 0 until centers.length) {
      val tempDist = p.squaredDist(centers(i))
      if (tempDist < closest) {
        closest = tempDist
        bestIndex = i
      }
    }
  
    bestIndex
  }
  def main(args: Array[String]) {
  val sc = new SparkContext(args(0), "SparkLocalKMeans",
      System.getenv("SPARK_HOME"), SparkContext.jarOfClass(this.getClass))
  	main(sc,args)
  }
  def main(sc: SparkContext, args: Array[String]): org.apache.spark.rdd.RDD[org.apache.spark.util.Vector] = {
    if (args.length < 3) {
        System.err.println("Usage: SparkLocalKMeans <k> <convergeDist>")
        System.exit(1)
    }
    
    
    val allLacun=sc.textFile("/gpfs/home/mader/Data*/*/*/lacun_4.csv")
val lacAsCols=allLacun.map(_.split(','))
val lacNoHeader=lacAsCols.filter(! _(0).startsWith("//"))
val lacHeader=lacAsCols.filter(_(0).startsWith("//"))

val headers=lacHeader.take(2)
val txtHeader=headers(1).map(_.replaceAll("//","").toUpperCase.trim())


def asDouble(chunk: String): Double = try { chunk.toDouble } catch { case _ => -1}
def parseVector(line: Array[String]): Vector = { try { new Vector(line.map(_.toDouble)) } catch { case _ => Vector() }}


val lacVec=lacNoHeader.map(parseVector(_))
val goodCount=lacVec.take(1)(0).length
val goodLac=lacVec.filter(_.length==goodCount)

val goodCols=txtHeader.zipWithIndex.filter(p => Array("VOLUME","PROJ_PCA1","PROJ_PCA2","PROJ_PCA3","NEIGHBORS","DENSITY_CNT") contains p._1).map(_._2)



val dataRaw=goodLac.map(row => new Vector(goodCols.map(colIndex => row(colIndex))))
val data=dataRaw.map(row => new Vector(Array(row.elements(3),row.elements(4),row.elements(5)):+(row.elements(0)-row.elements(2))/row.elements(0)*100)).cache()

    
    val K = args(1).toInt
    val convergeDist = args(2).toDouble
  
    //val kPoints = data.takeSample(withReplacement = false, K, 42).toArray
    val kPoints = data.take(K).toArray
    var tempDist = 1.0

    while(tempDist > convergeDist) {
      val closest = data.map (p => (closestPoint(p, kPoints), (p, 1)))
      
      val pointStats = closest.reduceByKey{case ((x1, y1), (x2, y2)) => (x1 + x2, y1 + y2)}
      
      val newPoints = pointStats.map {pair => (pair._1, pair._2._1 / pair._2._2)}.collectAsMap()
      
      tempDist = 0.0
      for (i <- 0 until K) {
        tempDist += kPoints(i).squaredDist(newPoints(i))
      }
      
      for (newP <- newPoints) {
        kPoints(newP._1) = newP._2
      }
      println("Finished iteration (delta = " + tempDist + ")")
    }

    println("Final centers:")
    kPoints.foreach(println)
    data
  }
}
val toy=SparkKMeans.main(sc,Array("spark://merlinl02:7077","10",".00001"))
