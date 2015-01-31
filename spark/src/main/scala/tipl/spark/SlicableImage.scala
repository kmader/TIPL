package tipl.spark

import org.apache.spark.mllib.linalg.distributed._
import tipl.formats.TImgRO
import tipl.util.{D3int, ID3int}

/**
 * A higher dimensional object that can be sliced in basic ways
 * @note unsupported ways return a None and must be handled in the calling function
 * Created by mader on 1/28/15.
 */
trait SliceableImage[P <: ID3int] {
  def getNSlice(gDimA: Int, gDimB: Int, gInd: P): Option[DistributedMatrix]
  def getZSlice(gInd: P) = getNSlice(0,1,gInd)
}


trait NaiveSliceableImage extends SliceableImage[D3int] {
  this: TImgRO =>

}


/**
 * implicit classes to make working with MLLib linalg structures easier
 */
object SliceableOps {
  implicit class tiplDistributedMatrix(tdm: DistributedMatrix) {
    def toRM() = tdm match {
      case drm: RowMatrix => drm
      case kvm: CoordinateMatrix => kvm.toRowMatrix()
      case irm: IndexedRowMatrix => irm.toRowMatrix()
      case _ => throw new IllegalArgumentException(tdm+
        " cannot be automatically converted to a row matrix")
    }
    def toIRM() = tdm match {
      case kvm: CoordinateMatrix => kvm.toIndexedRowMatrix()
      case irm: IndexedRowMatrix => irm
      case _ => throw new IllegalArgumentException(tdm+
        " cannot be automatically converted to a indexed row matrix")
    }
    def toCM() = tdm match {
      case kvm: CoordinateMatrix => kvm
      case irm: IndexedRowMatrix =>
        new CoordinateMatrix(
          irm.rows.mapPartitions{
            irPart =>
              for(crow <-irPart; ccol <- 0 until irm.numCols.toInt)
              yield new MatrixEntry(crow.index,ccol,crow.vector(ccol))
          }
        )

      case _ => throw new IllegalArgumentException(tdm+
        " cannot be automatically converted to a coordinate matrix")
    }
  }
}

object SlicingOps {
  def keyFilter[T](basePos: ID3int, aCol: Int) = aCol match {
    case D3int.AXIS_X =>
      val keepVal = basePos.gx
      (in: (ID3int,T)) => in._1.gx==keepVal
    case D3int.AXIS_Y =>
      val keepVal = basePos.gy
      (in: (ID3int,T)) => in._1.gy==keepVal
    case D3int.AXIS_Z =>
      val keepVal = basePos.gz
      (in: (ID3int,T)) => in._1.gz==keepVal
    case _ =>
      throw new IndexOutOfBoundsException(aCol+" index is not supported in positionFilter")
  }
  def positionFilter(basePos: ID3int, aCol: Int) = aCol match {
    case D3int.AXIS_X =>
      val keepVal = basePos.gx
      (in: ID3int) => in.gx==keepVal
    case D3int.AXIS_Y =>
      val keepVal = basePos.gy
      (in: ID3int) => in.gy==keepVal
    case D3int.AXIS_Z =>
      val keepVal = basePos.gz
      (in: ID3int) => in.gz==keepVal
    case _ =>
      throw new IndexOutOfBoundsException(aCol+" index is not supported in positionFilter")
  }

  def kvPairToMatrixEntry[T](rowCol: Int, colCol: Int, gInd: ID3int)(implicit nm: Numeric[T]) = {
    (rowCol,colCol) match {
      case (D3int.AXIS_X,D3int.AXIS_Y) =>
        Some(
        (in: (ID3int,T)) => MatrixEntry(in._1.gx-gInd.gx,in._1.gy-gInd.gy,nm.toDouble(in._2))
        )
      case (D3int.AXIS_X,D3int.AXIS_Z) =>
        Some(
        (in: (ID3int,T)) => MatrixEntry(in._1.gx-gInd.gx,in._1.gz-gInd.gz,nm.toDouble(in._2))
        )
      case (D3int.AXIS_Y,D3int.AXIS_X) =>
        Some(
        (in: (ID3int,T)) => MatrixEntry(in._1.gy-gInd.gy,in._1.gx-gInd.gx,nm.toDouble(in._2))
        )
      case (D3int.AXIS_Y,D3int.AXIS_Z) =>
        Some(
        (in: (ID3int,T)) => MatrixEntry(in._1.gy-gInd.gy,in._1.gz-gInd.gz,nm.toDouble(in._2))
        )
      case (D3int.AXIS_Z,D3int.AXIS_X) =>
        Some(
        (in: (ID3int,T)) => MatrixEntry(in._1.gz-gInd.gz,in._1.gx-gInd.gx,nm.toDouble(in._2))
        )
      case (D3int.AXIS_Z,D3int.AXIS_Y) =>
        Some(
        (in: (ID3int,T)) => MatrixEntry(in._1.gz-gInd.gz,in._1.gy-gInd.gy,nm.toDouble(in._2))
        )
      case _ =>
        None
    }
  }


  def getMissing3DIndex(rowCol: Int, colCol: Int) = {
    Set(rowCol,colCol) match {
      case a if !a.contains(D3int.AXIS_X) => Some(D3int.AXIS_X)
      case a if !a.contains(D3int.AXIS_Y) => Some(D3int.AXIS_Y)
      case a if !a.contains(D3int.AXIS_Z) => Some(D3int.AXIS_Z)
      case _ => None
    }
  }

}


