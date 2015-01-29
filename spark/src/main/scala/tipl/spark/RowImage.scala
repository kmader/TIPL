package tipl.spark

import org.apache.spark.mllib.linalg
import org.apache.spark.mllib.linalg.distributed.{DistributedMatrix, IndexedRow, IndexedRowMatrix}
import org.apache.spark.rdd.RDD
import tipl.formats.TImg
import tipl.tools.TypedSliceLookup
import tipl.util.{D3float, D3int, ID3int}

import scala.{specialized => spec}

/**
 *
 * @param dim
 * @param pos
 * @param elSize
 * @param imageType
 * @param baseImg
 */
class RowImage(dim: D3int, pos: D3int,
                elSize: D3float,
                imageType: Int, baseImg: RDD[(ID3int,linalg.Vector)]) extends
TImg.ATImg(dim, pos, elSize, imageType) with TypedSliceLookup[Double] with
SliceableImage[Double, D3int] {

  override def getSlice(sliceNum: Int): Option[Array[Double]] = {
    val slicePos = sliceNum+pos.gz
    rowDirection match {
      case 0 =>
        val starty = pos.gy
        val outArr = Array.fill[Double](getDim.gx*getDim.gy)(0)
        val indRows = baseImg.filter(_._1.gz==slicePos).map{
          case (spos,sdata) => ((spos.gy-starty)*getDim.gx,sdata)
        }.collect
        indRows.foreach{
          case (spos,sdata) =>
            var i = spos
            val epos = i+getDim.gx
            while (i<epos) {
              outArr(i)=sdata(i-spos)
              i+=1
            }
        }
        Some(outArr)
      case _ => None
    }
  }


  override def getSampleName: String = baseImg.name

  val rowDirection: Int = D3int.AXIS_X

  def numRows(): Long = rowDirection match {
    case D3int.AXIS_X => getDim().gy
    case D3int.AXIS_Y => getDim().gz
    case D3int.AXIS_Z => getDim().gx
  case _ => throw new ArrayIndexOutOfBoundsException("Not supported direction")
  }

  def numCols(): Long = rowDirection match {
    case D3int.AXIS_X => getDim().gz
    case D3int.AXIS_Y => getDim().gx
    case D3int.AXIS_Z => getDim().gy
    case _ => throw new ArrayIndexOutOfBoundsException("Not supported direction")
  }

  override def getNSlice(gDimA: Int, gDimB: Int, gInd: D3int): Option[DistributedMatrix] = {
    (gDimA,gDimB) match {
      case (D3int.AXIS_X,D3int.AXIS_Y) =>

        Some(new IndexedRowMatrix(
          baseImg.filter(SlicingOps.keyFilter(gInd,D3int.AXIS_Z)).map{
          case (spos,sdata) =>
            IndexedRow(spos.gy-gInd.gy,sdata)
        },getDim.y,getDim.x))
      case _ => None
    }
  }
}
