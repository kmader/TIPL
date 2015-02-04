package tipl.spark

import org.apache.spark.Partitioner
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.scalatest.{Matchers, FunSuite}

import scala.collection.mutable.ArrayBuffer
import Matchers._
import scala.reflect.ClassTag
import scala.util.control.Exception.allCatch

case class IOPartitioner[T](keyList: Seq[T], numPartitions: Int, overlap: Int)(
  implicit tm: Ordering[T]) {
  val outputPartitions = keyList.sorted.zipWithIndex.
    groupBy { i =>
    val slicePerPartition = Math.floor((keyList.length - 0.0) / (numPartitions * 1.0))
    Math.floor((i._2) / (slicePerPartition * 1.0 + 0.5)).toInt
  }.
    map(cpt => (cpt._1, cpt._2.map(_._1)))
  val prePartitions = outputPartitions.toSeq.flatMap {
    case (partId, keys) =>
      val baseOut = ArrayBuffer((partId, keys))
      // allows the overlap to extend beyond a single partition
      var (lastPart, partOffset) = (0, 1)
      while (lastPart < overlap) {
        if (partId >= partOffset) {
          baseOut += ((partId - partOffset, keys.take(overlap - lastPart)))
        }
        if ((partId + partOffset) < numPartitions) {
          baseOut += ((partId + partOffset, keys.takeRight(overlap - lastPart)))
        }
        partOffset += 1
        lastPart += keys.length
      }
      baseOut
  }
  val inputPartitions = prePartitions.groupBy(_._1).
    mapValues(_.map(_._2).reduce(_ ++ _))

  private def toTypeT(s: Any): Option[T] = (allCatch opt s.asInstanceOf[T])
  private def toInteger(s: Any): Option[Int] = (allCatch opt s.asInstanceOf[Int])

  def getInputPartitions(key: T) = inputPartitions.filter(_._2.contains(key)).map(_._1)

  def getOutputPartition(key: T) = outputPartitions.filter(_._2.contains(key)).headOption.map(_._1)

  def getInputPartitioner(): Partitioner = new Partitioner() {
    override def numPartitions: Int = numPartitions

    override def getPartition(key: Any): Int = toInteger(key) match {
      case Some(tkey) => tkey
      case None => throw new
          IllegalAccessError("Given key:" + key + " did not match type of partitioner:" + this)
    }
  }

  def getOutputPartitioner(): Partitioner = new Partitioner() {
    override def getPartition(key: Any): Int = toTypeT(key) match {
      case Some(tkey) => getOutputPartition(tkey).get
      case None => throw new
          IllegalAccessError("Given key:" + key + " did not match type of partitioner:" + this)
    }

    override def numPartitions: Int = numPartitions
  }

}

trait neighborMap[K,A,B] extends Serializable {
  def apply(key: K, pts: Seq[(K,A)]): B
}


/**
 * A repartitioned dataset designed to make doing neighborhood operations in spark easier and
 * more performant
 * @param srdd
 * @param partitions
 * @param overlap
 * @param km
 * @tparam K
 * @tparam V
 */
case class iopRDD[K : ClassTag,V: ClassTag](srdd: RDD[(K,V)],partitions: Int, overlap: Int)(
  implicit km: Ordering[K]) {
  lazy val keyList = srdd.map(_._1).collect
  lazy val ioPart = IOPartitioner(keyList,partitions,overlap)
  lazy val inputRDD = srdd.flatMap{
    case (key,value) => for(cPart <- ioPart.getInputPartitions(key)) yield (cPart,(key,value))
  }.groupByKey.partitionBy(ioPart.getInputPartitioner())

  def map[B: ClassTag](nm: neighborMap[K,V,B]): RDD[(K,B)] = {
    inputRDD.flatMap {
      case (partId, partList) =>
        ioPart.outputPartitions.get(partId) match {
          case Some(keys) => keys.map(ik => (ik,nm(ik,partList.toSeq)))
          case None => Seq[(K,B)]()
        }
    }
  }
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
  test("Partitioner has the correct number") {
    val ip = IOPartitioner(longList.map(_._1),4,2)
    ip.outputPartitions.size shouldBe (4)
    ip.inputPartitions.size shouldBe (4)
  }
  test("Create simple partitioner") {
    val ip = IOPartitioner(longList.map(_._1),4,2)
    println("Output:" + ip.outputPartitions.mapValues(_.mkString(",")).mkString("\n"))
    println("Input:" + ip.inputPartitions.mapValues(_.mkString(",")).mkString("\n"))
    ip.outputPartitions(0) should contain (5)
    ip.outputPartitions(0) shouldNot contain (6)
    ip.inputPartitions(0) should contain (5)
    ip.inputPartitions(0) should contain (6)
    ip.inputPartitions(0) shouldNot contain (8)

    ip.outputPartitions(3) should contain (20)
    ip.outputPartitions(3) shouldNot contain (15)
    ip.inputPartitions(3) should contain (15)

  }



}
