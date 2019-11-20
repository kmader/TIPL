package fourquant.imagej


import ij.plugin.filter.Analyzer
import ij.{ImagePlus, WindowManager}
import org.apache.spark.fourquant.IJResultsTableUDT
import org.apache.spark.sql.types

import scala.collection.mutable
import scala.util.Try

/**
  * Created by mader on 1/28/15.
  */
@types.SQLUserDefinedType(udt = classOf[IJResultsTableUDT])
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
      case Some(colNum) =>
        Some(
          rows.map(_ match {
            case crow if colNum<crow.length => crow(colNum)
            case _ => 0 // missing values are 0 for now
          }
          )
        )
      case None => None
    }
  }

  /**
    * The mean value of a column
    *
    * @param colName
    * @return
    */
  def mean(colName: String) = sum(colName).map(_/numObjects)

  /**
    * The sum of a column (if it exists)
    *
    * @param colName
    * @return
    */
  def sum(colName: String) = getColumn(colName).map(col => (col.sum))

  /**
    * The minimum value of a column (if it exists)
    *
    * @param colName
    * @return
    */
  def min(colName: String) = getColumn(colName).map(col => (col.min))
  /**
    * The maximum value of a column (if it exists)
    *
    * @param colName
    * @return
    */
  def max(colName: String) = getColumn(colName).map(col => (col.max))

  def numObjects = rows.length

  def toString(xrows: Int = 5, xcols: Int = 5) = {
    val subhead = (1 to xcols).zip(header).map(_._2)
    val subrows = (1 to xrows). // keep only the first rows
      zip(rows.map(cr => cr.zip(1 to xcols).map(_._1))). // keep only the first columns
      map(iv => (Array(iv._1.toDouble) ++ iv._2).mkString("\t"))
    ("#"++subhead).mkString("\t")+"\n"+
      subrows.mkString("\n")
  }

  override def toString = toString()

  def toMap() = {
    hmap.map{
      case(cKey,cInd) =>
        (cKey,
          getColumn(cKey).
            map(_.toArray).
            getOrElse(new Array[Double](0)))
    }.toMap
  }
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

  /**
    * Clear the table before running an analysis
    */
  def clearTable() = {
    Analyzer.getResultsTable.reset()
  }
  def fromIJ() = {
    val rt = Analyzer.getResultsTable
    val omm = mutable.Map[String,Array[Double]]()
    var i = 0
    while(rt.columnExists(i)) {
      val colName = rt.getColumnHeading(i)
      val colVals = rt.getColumnAsDoubles(i)
      omm(colName)=colVals
      i+=1

    }
    val colNames = omm.keys.toArray
    val rowCount = omm.values.head.length
    // a mutable state free way of generating this list
    val rowValues = for(row <- 0 until rowCount;
                        outRow = for ((nkey,ncol) <- omm) yield Try{ncol(row)}.getOrElse(0.0)
    ) yield (outRow.toArray)

    IJResultsTable(colNames,rowValues) //TODO rewrite this to not need the silly row-based format
  }
  @deprecated("should not be used since it's implementation is buggy","1.0")
  def fromSpiji() = {
    val header = Spiji.getListColumns()

    val rows = Spiji.getResultsTable().asInstanceOf[Array[Array[Double]]].toIndexedSeq

    IJResultsTable(header,rows)
  }
}
