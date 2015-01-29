package tipl.ij.scripting

import ij.{ImagePlus, WindowManager}
import tipl.ij.Spiji

/**
 * Created by mader on 1/28/15.
 */
case class IJResultsTable(header: Array[String], rows: IndexedSeq[Array[Double]]) {
  lazy val hmap = header.zipWithIndex.toMap
  def getRowValues(rowId: Int) = {
    rowId match {
      case i if i<numObjects => Some(rows(i).zip(header).map(_.swap).toMap)
      case _ => None
    }
  }
  def getColumn(nm: String) = {
    hmap.get(nm) match {
      case Some(colNum) => Some(rows.map(_(colNum)))
      case None => None
    }
  }
  def mean(colName: String) = getColumn(colName).map(col => (col.sum)/numObjects)
  def min(colName: String) = getColumn(colName).map(col => (col.min))
  def max(colName: String) = getColumn(colName).map(col => (col.max))

  def numObjects = rows.length
}

object IJResultsTable {
  val measurementsString =
    "area mean standard modal min centroid center perimeter bounding fit shape feret's " +
    "integrated median skewness kurtosis area_fraction stack redirect=None decimal=3"
  def fromCL(cImg: Option[ImagePlus] = None) = {
    cImg.map(WindowManager.setTempCurrentImage(_))
    Spiji.run(
      "Set Measurements...",
      measurementsString
    )
    Spiji.run("Analyze Particles...", "display clear")
    fromIJ()
  }

  def fromIJ() = {
    val header = Spiji.getListColumns()
    val rows = Spiji.getResultsTable().asInstanceOf[Array[Array[Double]]].toIndexedSeq

    IJResultsTable(header,rows)
  }
}
