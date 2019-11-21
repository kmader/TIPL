package tipl.spark

import org.apache.commons.math3.linear.RealMatrix
import org.apache.spark.mllib.linalg.distributed.DistributedMatrix
import org.apache.spark.rdd.RDD

import tipl.util.{ID3int, D3int}


/**
 *
 * @param baseMat the underlying list of matrices
 */
case class KVMatrix(baseMat: RDD[RealMatrix]) extends SliceableImage[D3int] {


  // class for n x d 2D matrices stored as RDDs of 1 x d 1D matrices
  // efficient only when n >> d
  def +(other: KVMatrix): KVMatrix = {
    // add two RDD matrices element-wise
    KVMatrix(baseMat.zip(other.baseMat).map { case (x1, x2) => x1.add(x2) })
  }

  def -(other: KVMatrix): KVMatrix = {
    // subtract two RDD matrices element-wise
    KVMatrix(baseMat.zip(other.baseMat).map { case (x1, x2) => x1.add(x2.scalarMultiply(-1)) })
  }

  def *(other: KVMatrix): KVMatrix = {
    // multiply an RDD matrix by a smaller matrix
    KVMatrix(baseMat.zip(other.baseMat).map { case (x1, x2) => x1.multiply(x2) })
  }


  //TODO implement this function
  override def getNSlice(gDimA: Int, gDimB: Int, gInd: D3int): Option[DistributedMatrix] = ???

}

object KVMatrix {

  def apply[P <: ID3int](si: SliceableImage[P]) = si match {
    case kvm: KVMatrix => kvm
    case _ => throw new IllegalArgumentException("Other types are not supported yet")
    //TODO support other types
  }

  /**
   * Makes using standard functions with these matrices much easier
   */
  implicit val matrix2DNum = new NDNumeric[RealMatrix] {
    override def plus(x: RealMatrix, y: RealMatrix): RealMatrix =
      x.add(y)

    override def negate(x: RealMatrix): RealMatrix = x.scalarMultiply(-1)

    override def times(x: RealMatrix, y: RealMatrix): RealMatrix =
      x.multiply(y)

    override def minus(x: RealMatrix, y: RealMatrix): RealMatrix =
      x.add(y.scalarMultiply(-1))

    /**
     *
     * @note based on the trace of the matrices
     */
    override def compare(x: RealMatrix, y: RealMatrix): Int =
      x.getTrace compareTo y.getTrace()
  }

  implicit class FancyRealMatrix(baseMat: RealMatrix)(implicit dm:
  NDNumeric[RealMatrix]) {
    def +(other: RealMatrix): RealMatrix = dm.plus(baseMat, other)

    def -(other: RealMatrix): RealMatrix = dm.minus(baseMat, other)

    def *(other: RealMatrix): RealMatrix = dm.times(baseMat, other)

  }

  trait NDNumeric[T] extends Numeric[T] {

    @deprecated("NDNumerics do not support these functions", "1.0")
    override def toDouble(x: T): Double =
      throw new IllegalArgumentException(x.toString + " cannot be converted to double")

    @deprecated("NDNumerics do not support these functions", "1.0")
    override def toFloat(x: T): Float =
      throw new IllegalArgumentException(x.toString + " cannot be converted to float")

    @deprecated("NDNumerics do not support these functions", "1.0")
    override def toInt(x: T): Int =
      throw new IllegalArgumentException(x.toString + " cannot be converted to int")

    @deprecated("NDNumerics do not support these functions", "1.0")
    override def fromInt(x: Int): T =
      throw new IllegalArgumentException(x.toString + " cannot be converted from an int")

    @deprecated("NDNumerics do not support these functions", "1.0")
    override def toLong(x: T): Long =
      throw new IllegalArgumentException(x.toString + " cannot be converted from a long")
  }

}
