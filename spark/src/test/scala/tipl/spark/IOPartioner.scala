package tipl.spark

import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD
import org.scalatest.FunSuite

import scala.collection.mutable.ArrayBuffer

case class IOPartitioner[T](keyList: Seq[T], numPartitions: Int, overlap: Int)(
  implicit tm: Ordering[T]) extends Partitioner {
  val outputPartitions = keyList.sorted.zipWithIndex.groupBy(i => (i._2-1) / numPartitions).
    map(cpt => (cpt._1,cpt._2.map(_._1)))
  val prePartitions = outputPartitions.flatMap {
    case (partId, keys) =>
      val baseOut = ArrayBuffer((partId,keys))
      // allows the overlap to extend beyond a single partition
      var (lastPart,partOffset) = (0,1)
      while(lastPart<overlap) {
        if(partId>=partOffset) {
          baseOut ++= Seq((partId-partOffset,keys.take(overlap-lastPart)))
        }
        if((partId+partOffset)<numPartitions) {
          baseOut ++= Seq((partId+partOffset,keys.takeRight(overlap-lastPart)))
        }
        partOffset+=1
        lastPart+=keys.length
      }
      baseOut
  }
  val inputPartitions = prePartitions.groupBy(_._1).
    mapValues(_.map(_._2).reduce(_ ++ _))

  override def getPartition(key: Any): Int = key match {
    case tkey: T => getOutputPartition(tkey).get
    case _ => throw new
        IllegalAccessError("Given key:"+key+" did not match type of partitioner:"+this)
  }

  def getInputPartitions(key: T) = inputPartitions.filter(_._2.contains(key)).map(_._1)
  def getOutputPartition(key: T) = outputPartitions.filter(_._2.contains(key)).headOption.map(_._1)


}


object IOPartitioner extends Serializable {

  def overpartition[V](inRdd: RDD[(Int,V)],overlap: Int) = {
    val pts = inRdd.partitions.length
    inRdd.mapPartitionsWithIndex {
      case (partId, partConts) => partConts
    }
  }
}



/**
 * Created by mader on 2/4/15.
 */
class IOPartionerTests extends FunSuite { //with LocalSparkContext {
  val longList = (0 to 20).map(i => (i,i.toString))
  test("Create simple partitioner") {
    val ip = IOPartitioner(longList.map(_._1),4,2)
    println("Output:" + ip.outputPartitions.mapValues(_.mkString(",")).mkString("\n"))
    println("Preinput:" + ip.prePartitions.mapValues(_.mkString(",")).mkString("\n"))
    println("Input:" + ip.inputPartitions.mapValues(_.mkString(",")).mkString("\n"))
  }

}
