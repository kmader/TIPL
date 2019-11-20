package fourquant.labeling

import fourquant.arrays.ArrayPosition
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

/**
 * Created by mader on 4/13/15.
 */
object ConnectedComponents extends Serializable {
  trait LabelCriteria[T] extends Serializable {
    def matches(a: T, b: T): Boolean
  }
  object implicits extends Serializable {
    implicit val IntLabelCriteria = new LabelCriteria[Int] {
      override def matches(a: Int, b: Int): Boolean = (a==b)
    }
    implicit val BoolLabelCriteria = new LabelCriteria[Boolean] {
      override def matches(a: Boolean, b: Boolean): Boolean = (a==b)
    }
  }


  def ConcurrentMap(tuples: Array[(Long, Long)]) = ???

  def Labeling2DChunk[A: ArrayPosition, B: LabelCriteria](rdd: RDD[(A,B)],
                                                          window: (Int, Int) = (1, 1),
                                                           fullRange: Boolean = true,
                                                           optimizeList: Boolean = true)(
    implicit act: ClassTag[A], bct: ClassTag[B]) = {
    var curRdd = rdd.zipWithUniqueId().map(kv => (kv._1._1,(kv._2,kv._1._2)))

    var swapCounts = 1
    var iter = 0

    while (swapCounts>0 & iter<Int.MaxValue) {
      val mergeList = collection.mutable.Map(curRdd.flatMap{
        case (pos,value) => spreadPoints(pos,value,window,fullRange)
      }.groupBy {
        kvpos =>
          val cPos = implicitly[ArrayPosition[A]].getPos(kvpos._1)
          (cPos(0),cPos(1))
      }.mapPartitions {
        cPart =>
          for(
            (pos,values) <- cPart;
            mergeGroup <- collapsePointToMerge(values.toSeq)
          )
            yield mergeGroup
      }.collect(): _*)

      // optimize list if needed
      if (optimizeList) {
        var optSwaps = 1
        while (optSwaps>0) {
          optSwaps=0
          for((oldKey,oldValue) <- mergeList;
              newValue <- mergeList.get(oldValue)) {
            mergeList(oldKey)=newValue
            optSwaps+=1
          }
          //println("Optimize Swaps:"+optSwaps)
        }
      }
      swapCounts = mergeList.size
      curRdd = curRdd.mapValues{
        case (label,lvalue) => (mergeList.getOrElse(label,label),lvalue)
      }

      iter+=1

      println("Current Iteration:"+iter+", swaps:"+swapCounts)
    }
    curRdd
  }

  def Labeling2D[A: ArrayPosition, B: LabelCriteria](rdd: RDD[(A,B)],
                                                     window: (Int, Int) = (1,1),
                                                    fullrange: Boolean = true,
                                                      maxIters: Int = Int.MaxValue)(
      implicit act: ClassTag[A], bct: ClassTag[B]) = {
    var curRdd = rdd.zipWithUniqueId().map(kv => (kv._1._1,(kv._2,kv._1._2)))

    var swapCounts = 1
    var iter = 0

    while (swapCounts>0 & iter<maxIters) {
      val newRdd = curRdd.flatMap{
        case (pos,value) => spreadPoints(pos,value,window,fullrange)
      }.groupBy {
        kvpos =>
          val cPos = implicitly[ArrayPosition[A]].getPos(kvpos._1)
          (cPos(0),cPos(1))
      }.mapPartitions {
        cPart =>
          for(
            (pos,values) <- cPart;
            (colKV,cswaps) <- collapsePoint(values.toSeq)
          )
            yield (colKV,cswaps)
      }

      swapCounts = newRdd.map(_._2).sum().toInt
      curRdd = newRdd.map(_._1)

      iter+=1

      println("Current Iteration:"+iter+", swaps:"+swapCounts)
    }
    curRdd
  }

  private [labeling] def spreadPoints[A: ArrayPosition, B](pos: A, pValue: B,
                                                           window: (Int, Int),
                                                            fullrange: Boolean) = {
    val xpts = (if (fullrange) -window._1 else 0) to window._1
    val ypts = (if (fullrange) -window._2 else 0) to window._2
    for(x<- xpts; y<- ypts; samePoint = (x==0) & (y==0))
      yield (implicitly[ArrayPosition[A]].add(pos,Array(x,y)),pValue,samePoint)
  }

  private[labeling] def collapsePoint[A, B: LabelCriteria](values: Seq[(A,(Long,B),Boolean)]) = {
    val (origPt,otherPts) = values.partition(_._3)
      origPt.headOption match {
      case Some(hPoint) =>
        val oldLabel = hPoint._2._1
        val newLabel = (otherPts.
          filter(i => implicitly[LabelCriteria[B]].matches(hPoint._2._2,i._2._2)). // if they match
          map(_._2._1) ++ Seq(oldLabel)).min
        Some(((hPoint._1,(newLabel,hPoint._2._2))),if(oldLabel==newLabel) 0 else 1)
      case None => None
    }
  }

  private[labeling] def collapsePointToMerge[A, B: LabelCriteria](values: Seq[(A,(Long,B),Boolean)])
  = {
    val (origPt,otherPts) = values.partition(_._3)
    origPt.headOption match {
      case Some(hPoint) =>
        val oldLabel = hPoint._2._1
        val newLabel = (otherPts.
          filter(i => implicitly[LabelCriteria[B]].matches(hPoint._2._2,i._2._2)). // if they match
          map(_._2._1) ++ Seq(oldLabel)).min
        if (newLabel==oldLabel) None
        else Some(oldLabel,newLabel)
      case None => None
    }
  }
}
