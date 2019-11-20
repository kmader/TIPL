package fourquant.arrays

import breeze.linalg.{Matrix => _, _}
import org.apache.spark.mllib.linalg.distributed.BlockMatrix
import org.apache.spark.mllib.linalg.{Matrices, Matrix}
import org.apache.spark.rdd.RDD

import scala.language.implicitConversions

import scala.reflect.ClassTag

/**
 * A collection of tools to integrate more easily with Breeze and MLLib
 * Created by mader on 4/13/15.
 */
object BreezeOps extends Serializable { // have the proper conversions for positions automatically

  case class TileStats(mean: Double, min: Double, max: Double, count: Long, nzcount: Long)

  implicit class denseVectorRDD[A : ClassTag, B: Numeric](rdd: RDD[(A,DenseVector[B])]) {

  }

  implicit class denseMatrixRDD[A : ClassTag, B: Numeric](rdd: RDD[(A,DenseMatrix[B])])
    extends Serializable {
    lazy val doubleRdd = rdd.mapValues(im => im.mapValues(v => implicitly[Numeric[B]].toDouble
      (v)))

    def asDouble = doubleRdd

    def +[C: Numeric](rddB: RDD[(A,DenseMatrix[C])]) = doubleRdd.join(rddB.asDouble).mapValues{
      case (aMat,bMat) =>
        aMat+bMat
    }
  }

  def array2DtoMatrix[B: Numeric](arr: Array[Array[B]])(
    implicit bbt: ClassTag[Array[B]], bbbt: ClassTag[B]
    ) = {
    Matrices.dense(arr(0).length, arr.length,
      arr.flatten[B].map(v => implicitly[Numeric[B]].toDouble(v)))
  }
  implicit class array2DRDD[A: ClassTag, B: Numeric](rdd: RDD[(A,Array[Array[B]])])(
    implicit bt: ClassTag[Array[Array[B]]], bbt: ClassTag[Array[B]], bbbt: ClassTag[B]) extends
      Serializable {

    def toMatrixRDD() = arrayRDD2DtoMatrixRDD(rdd)


    def getTileStats() = rdd.mapValues{
      cTile =>
        val flatVec = cTile.flatten[B].map(implicitly[Numeric[B]].toDouble(_))
        TileStats(mean=flatVec.sum/flatVec.length,
                  min=flatVec.min,
                  max=flatVec.max,
                  count=flatVec.length,
                  nzcount=flatVec.filter(_>0).sum.toLong)
    }


  }

  implicit def arrayRDD2DtoMatrixRDD[A: ClassTag, B: Numeric](rdd: RDD[(A,Array[Array[B]])])(
            implicit bt: ClassTag[Array[Array[B]]], bbt: ClassTag[Array[B]], bbbt: ClassTag[B])
     = rdd.mapValues(array2DtoMatrix(_))

  implicit class matrixThreshold(im: Matrix) extends Serializable {

    def sparseThreshold(f: (Double) => Boolean) = {
      im.toArray.zipWithIndex.filter{
        case (dblVal,idx) => f(dblVal)
      }.map{
        case (dblVal,idx) =>
          ((idx % im.numRows, Math.round(idx/im.numRows).toInt),dblVal)
      }
    }
  }
  implicit class matrixRDD[A: ClassTag](mrdd: RDD[(A, Matrix)]) extends Serializable {
    /**
     * Apply a threshold to the data
     * @param f the threshold function
     * @return
     */
    def threshold(f: (Double) => Boolean) = {
        mrdd.mapValues(im => new DenseMatrix(im.numRows,im.numCols,im.toArray.map(f)))
    }
    def localSparseThresh(f: (Double) => Boolean) =
      mrdd.mapValues(_.sparseThreshold(f))

    def apply(f: (Double) => Double) =
      mrdd.mapValues(im => Matrices.dense(im.numRows,im.numCols,im.toArray.map(f)))

  }

  implicit class locMatrixRDD[A: ArrayPosition](mrdd: RDD[(A, Matrix)])(
                                               implicit at: ClassTag[A]
    ) extends Serializable {
    import Positions._

    def sparseThresh(f: (Double) => Boolean) = {
      mrdd.localSparseThresh(f).flatMap{
        case (cPos,spArray) =>
          for(cPt <- spArray) yield(
            implicitly[ArrayPosition[A]].add(cPos, Array(cPt._1._1,cPt._1._2)),
            cPt._2)
      }
    }

    def toBlockMatrix(rowsPerBlock: Option[Int] = None, colsPerBlock: Option[Int] = None) = {
      val bm = mrdd.map{
        case (kpos,mat) => ((kpos.getX,kpos.getY),mat)
      }
      (rowsPerBlock,colsPerBlock) match {
        case (Some(rpb),Some(cpb)) =>
          new BlockMatrix(bm,rpb,cpb)
        case (_,_) =>
          val (rpb,cpb) = bm.map(kv => (kv._2.numRows,kv._2.numCols)).first
          new BlockMatrix(bm,rpb,cpb)
      }
    }

  }

  implicit def arrayRDD2DtoLocMatrixRDD[A: ArrayPosition, B: Numeric](
                 rdd: RDD[(A, Array[Array[B]])])(
    implicit at: ClassTag[A], bt: ClassTag[Array[Array[B]]], bbt: ClassTag[Array[B]],
    bbbt: ClassTag[B])
  = locMatrixRDD(rdd.toMatrixRDD())


}
