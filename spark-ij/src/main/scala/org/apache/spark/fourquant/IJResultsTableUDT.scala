package org.apache.spark.fourquant

import fourquant.imagej.{IJResultsTable, ImageStatistics}
import org.apache.spark.sql.catalyst.expressions.GenericMutableRow
import org.apache.spark.sql.catalyst.util.GenericArrayData
import org.apache.spark.sql.catalyst.{InternalRow, util => cat_util}
import org.apache.spark.sql.types.{UserDefinedType, _}

object IJResultsTableUDT extends Serializable {
  def toMap(pData: IJResultsTable) = {
    val keys = pData.header.map(_.asInstanceOf[Any])

    val rows = pData.header.map(rowName => {
      val rowVals = pData.getColumn(rowName) match {
        case Some(iArr) => iArr.toArray
        case None => new Array[Double](0)
      }
      new GenericArrayData(rowVals.map(_.asInstanceOf[Any])).asInstanceOf[Any] // sparksql hates typed arrays
    }) // since arrays are invariant in scala
    cat_util.ArrayBasedMapData(keys,rows)
  }
}


/**
  * the Sparksql user-defined type for the the results table
  */
class IJResultsTableUDT extends UserDefinedType[IJResultsTable] {
  //TODO add tests to make sure this is serialized in a sensible manner (also might be useful to restructure field
  // ordering
  /** some serious cargo-cult, no idea hwo this is actually used **/
  override def sqlType: StructType = {
    StructType(
      Seq(
        StructField("columns",MapType(StringType,ArrayType(DoubleType)))
      )
    )
  }

  override def serialize(obj: IJResultsTable): InternalRow = {
    val row = new GenericMutableRow(1)
    obj match {
      case pData: IJResultsTable =>
        //TODO stolen from VectorUDT, make a nicer one
        val mapOutput = IJResultsTableUDT.toMap(pData)
        row.update(0, mapOutput)
      case _ =>
        throw new RuntimeException("The given object:"+obj+" cannot be serialized by "+this)
    }
    row
  }

  override def deserialize(datum: Any): IJResultsTable = {
    datum match {
      case r: InternalRow =>
        require(r.numFields==1,"Wrong row-length given "+r.numFields+" instead of 1")
        val outMap = r.getMap(0)

        val header = outMap.keyArray().toArray[String](StringType).asInstanceOf[Array[String]]
        // the arraydata / arraytype methods are horrendous and the data needs to be manually extracted and retyped
        val rows = outMap.valueArray().toArray[ArrayType](ArrayType(DoubleType))
          .asInstanceOf[Array[ArrayType]]
          .map(_.asInstanceOf[cat_util.ArrayData].toDoubleArray())

        IJResultsTable(
          header,
          rows
        )
      case _ =>
        throw new RuntimeException("The given object:"+datum+" cannot be deserialized by "+this)
    }

  }

  override def equals(o: Any) = o match {
    case v: ImageStatistics => true
    case _ => false
  }

  override def hashCode = 8775309
  override def typeName = "IJResultsTable["+"]"
  override def asNullable = this

  override def userClass = classOf[IJResultsTable]
}
