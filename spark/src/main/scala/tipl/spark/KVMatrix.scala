package tipl.spark

import org.apache.spark.mllib.linalg.distributed.DistributedMatrix
import org.apache.spark.rdd.RDD
import cern.colt.matrix._

import cern.colt.matrix.linalg._
import cern.jet.math._
import tipl.util.{ID3int, D3int}


/**
 * 
 * @param baseMat the underlying list of matrices
 */
case class KVMatrix(baseMat: RDD[DoubleMatrix2D]) extends SliceableImage[D3int] {
  val alg = Algebra.DEFAULT
  val factory1D = DoubleFactory1D.dense
  val factory2D = DoubleFactory2D.dense

  // class for n x d 2D matrices stored as RDDs of 1 x d 1D matrices
  // efficient only when n >> d
  def + (other: KVMatrix) : KVMatrix = {
    // add two RDD matrices element-wise
    KVMatrix(baseMat.zip(other.baseMat).map{case (x1,x2) => x1.assign(x2,Functions.plus)})
  }

  def - (other: KVMatrix) : KVMatrix = {
    // subtract two RDD matrices element-wise
    KVMatrix(this.baseMat.zip(other.baseMat).map{case (x1,x2) => x1.assign(x2,Functions.minus)})
  }

  def * (other: DoubleMatrix2D) : KVMatrix = {
    // multiply an RDD matrix by a smaller matrix
    KVMatrix(this.baseMat.map(x => alg.mult(alg.transpose(other),x)))
  }

  def / (other: DoubleMatrix2D) : KVMatrix = {
    // right matrix division
    this * alg.transpose(alg.inverse(alg.transpose(other)))
  }

  def toStringArray : Array[String] = {
    return this.baseMat.toArray().map(x => x.toArray().mkString(" "))
  }

  def outerProd(vec1: DoubleMatrix1D, vec2: DoubleMatrix1D): DoubleMatrix2D = {
    val out = factory2D.make(vec1.size,vec2.size)
    alg.multOuter(vec1,vec2,out)
    return out
  }

  //TODO implement this function
  override def getNSlice(gDimA: Int, gDimB: Int, gInd: D3int): Option[DistributedMatrix] = ???

}

object KVMatrix {
  val alg = Algebra.DEFAULT
  trait NDNumeric[T] extends Numeric[T] {

    @deprecated("NDNumerics do not support these functions","1.0")
    override def toDouble(x: T): Double =
      throw new IllegalArgumentException(x.toString+" cannot be converted to double")
    @deprecated("NDNumerics do not support these functions","1.0")
    override def toFloat(x: T): Float =
      throw new IllegalArgumentException(x.toString+" cannot be converted to float")
    @deprecated("NDNumerics do not support these functions","1.0")
    override def toInt(x: T): Int =
      throw new IllegalArgumentException(x.toString+" cannot be converted to int")
    @deprecated("NDNumerics do not support these functions","1.0")
    override def fromInt(x: Int): T =
      throw new IllegalArgumentException(x.toString+" cannot be converted from an int")
    @deprecated("NDNumerics do not support these functions","1.0")
    override def toLong(x: T): Long =
      throw new IllegalArgumentException(x.toString+" cannot be converted from a long")
  }
  /**
   * Makes using standard functions with these matrices much easier
   */
  implicit val matrix2DNum = new NDNumeric[DoubleMatrix2D] {
    override def plus(x: DoubleMatrix2D, y: DoubleMatrix2D): DoubleMatrix2D =
      x.assign(y,Functions.plus)

    override def negate(x: DoubleMatrix2D): DoubleMatrix2D = x.assign(-1)

    override def times(x: DoubleMatrix2D, y: DoubleMatrix2D): DoubleMatrix2D =
      alg.mult(alg.transpose(y),x)

    override def minus(x: DoubleMatrix2D, y: DoubleMatrix2D): DoubleMatrix2D =
      x.assign(y,Functions.minus)

    override def compare(x: DoubleMatrix2D, y: DoubleMatrix2D): Int =
      x.cardinality() compareTo y.cardinality()
  }

  implicit class FancyDoubleMatrix2D(baseMat: DoubleMatrix2D)(implicit dm:
  NDNumeric[DoubleMatrix2D]) {
    def + (other: DoubleMatrix2D) : DoubleMatrix2D = dm.plus(baseMat,other)
    def - (other: DoubleMatrix2D) : DoubleMatrix2D = dm.minus(baseMat,other)
    def * (other: DoubleMatrix2D) : DoubleMatrix2D = dm.times(baseMat,other)

  }

  def apply[P <: ID3int](si: SliceableImage[P]) = si match {
    case kvm: KVMatrix => kvm
    case _ => throw new IllegalArgumentException("Other types are not supported yet")
    //TODO support other types
  }
}
