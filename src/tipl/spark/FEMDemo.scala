

/*
 A very basic implementation of Finite Element Analysis using Spark
 */
package tipl.spark

import scala.annotation.tailrec
import scala.math._
import scala.reflect.ClassTag
import scala.util._

import org.apache.spark._
import org.apache.spark.serializer._
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.graphx._
import org.apache.spark.graphx.Graph
import org.apache.spark.graphx.Edge
import org.apache.spark.graphx.impl.GraphImpl

/** A collection of graph generating functions. */
object FEMDemo {
  /**
   * A class for storing the image vertex information to prevent excessive tuple-dependence
   */
  @serializable case class ImageVertex(index: Int,pos: (Int,Int) = (0,0),value: Int = 0,original: Boolean = false)
  val extractPoint = (idx: Int, inArr: Array[Array[Int]],xwidth: Int,ywidth: Int) => {
      val i=Math.floor(idx*1f/xwidth).toInt
      val j=idx%xwidth
      new ImageVertex(idx,(i,j),inArr(i)(j))
  }
  def pointDist(a: ImageVertex,b: ImageVertex) = {
    Math.sqrt(
        (a.pos._1-b.pos._1)^2+(a.pos._2-b.pos._2)^2
        )
    
  }
  def spreadVertices(pvec: ImageVertex, windSize: Int = 1) = {
    val wind=(-windSize to windSize)
    val pos=pvec.pos
    for(x<-wind; y<-wind) 
      yield new ImageVertex(pvec.index,(pos._1+x,pos._2+y),pvec.value,(x==0) & (y==0))
    }
  
  def twoDArrayToGraph(sc: SparkContext, inArr: Array[Array[Int]]): Graph[ImageVertex, Double] = {
    val ywidth=inArr.length
    val xwidth=inArr(0).length
    val vertices = sc.parallelize(0 until xwidth*ywidth).map{
      idx => extractPoint(idx,inArr,xwidth,ywidth)
    }
    val fvertices: RDD[(VertexId, ImageVertex)] = vertices.map(cpt => (cpt.index,cpt))
    val edges = vertices.flatMap{
      cpt => spreadVertices(cpt,1)
    }.groupBy(_.pos).filter{
      // at least one original point
      ptList => ptList._2.map(_.original).reduce(_ || _)
    }.flatMap{
      combPoint => {
        val pointList=combPoint._2
        val centralPoint = pointList.filter(_.original).head
        val neighborPoints = pointList.filter(!_.original)
        for(nDex<-neighborPoints) yield Edge[Double](centralPoint.index,nDex.index,pointDist(centralPoint,nDex))
      }
    }
    
    Graph[ImageVertex, Double](fvertices,edges)
    
  }
  def main(args: Array[String]):Unit = {
	val p = SparkGlobal.activeParser(args)
	val imSize = p.getOptionInt("size", 50,
      "Size of the image to run the test with");
	val easyRow = Array(0,1,1,0)
	val testImg = Array(easyRow,easyRow,easyRow);
	twoDArrayToGraph(SparkGlobal.getContext(),testImg)

  }
  
}