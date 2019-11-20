package org.apache.spark.fourquant

import fourquant.imagej.ImageStatistics
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.{GenericMutableRow, MutableRow}
import org.apache.spark.sql.types._

/**
 * the Sparksql user-defined type for the the ImageStatistics
 */
class ImageStatisticsUDT extends UserDefinedType[ImageStatistics] {
  /** some serious cargo-cult, no idea hwo this is actually used **/
  override def sqlType: StructType = {
    StructType(
      Seq(
        StructField("min",DoubleType, nullable=false),
        StructField("mean",DoubleType, nullable=false),
        StructField("stdev",DoubleType, nullable=false),
        StructField("max",DoubleType, nullable=false),
        StructField("pts",LongType, nullable=false)
      )
    )
  }

  override def serialize(obj: ImageStatistics): MutableRow = {
    val row = new GenericMutableRow(5)
    obj match {
      case pData: ImageStatistics =>
        row.setDouble(0,pData.min)
        row.setDouble(1,pData.mean)
        row.setDouble(2,pData.stdDev)
        row.setDouble(3,pData.max)
        row.setLong(4,pData.pts)
      case _ =>
        throw new RuntimeException("The given object:"+obj+" cannot be serialized by "+this)
    }
    row
  }

  override def deserialize(datum: Any): ImageStatistics = {
    datum match {
      case r: InternalRow =>
        require(r.numFields==5,s"Wrong row-length given ${r.numFields} instead of 5")

        ImageStatistics(r.getDouble(0),r.getDouble(1),r.getDouble(2),r.getDouble(3),r.getLong(4))
      case _ =>
        throw new RuntimeException("The given object:"+datum+" cannot be deserialized by "+this)
    }

  }

  override def equals(o: Any) = o match {
    case v: ImageStatistics => true
    case _ => false
  }

  override def hashCode = 9571285
  override def typeName = "ImageStatistics["+"]"
  override def asNullable = this

  override def userClass = classOf[ImageStatistics]
}
