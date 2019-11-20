package fourquant.arrays

import org.apache.spark.rdd.RDD
import org.nd4j.linalg.api.ndarray.INDArray

import scala.reflect.ClassTag

/**
 * Created by mader on 4/13/15.
 */
object ArrayOps extends Serializable {
  trait NumericArray[T] extends Numeric[T] {

  }
  implicit class arrayRDD2D[A : ClassTag, B: Numeric](rdd: RDD[(A,Array[Array[B]])])(
    implicit ct: ClassTag[Array[Array[B]]])
    //extends Numeric[RDD[(A,Array[Array[B]])]]
    {
    lazy val dbRdd = rdd.mapValues(_.map(_.map(v => implicitly[Numeric[B]].toDouble(v))))
    def toDoubleRDD() = dbRdd
    def +[F: Numeric](rddB: RDD[(A,Array[Array[F]])])(
      implicit ft: ClassTag[Array[Array[F]]]) = {
      (rdd.toDoubleRDD).doublePlus(rddB.toDoubleRDD)
    }
  }

  implicit class doubleArrayRDD2D[A: ClassTag](rdd: RDD[(A,Array[Array[Double]])]) {
    def doublePlus(rddB: RDD[(A,Array[Array[Double]])]) = {
      rdd.join(rddB).mapValues{
        case (aVal,bVal) =>
          val outArr = Array.fill(aVal.length,aVal(0).length)(0.0)
          var i = 0
          while(i<aVal.length) {
            var j = 0
            while(j<aVal(i).length) {
              outArr(i)(j) = aVal(i)(j)+bVal(i)(j)
            }
            i+=1
          }
      }
    }
  }

}



object NDArrayOps {
  implicit class NDArray[A: ClassTag](rdd: RDD[(A,INDArray)]) {

  }
}
