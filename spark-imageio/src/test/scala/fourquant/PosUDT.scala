package org.apache.spark.fourquant


import fourquant.sql.PosData
import fourquant.sql.PosData
import fourquant.sql.SQLTestTools.PosData
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.GenericMutableRow
import org.apache.spark.sql.types.ArrayType
import org.apache.spark.sql.types.IntegerType
import org.apache.spark.sql.types.StructField
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.types.UserDefinedType

class PosDataUDT extends UserDefinedType[PosData] {

  override def sqlType: StructType = {
    StructType(
      Seq(
        StructField("x",IntegerType,nullable=false),
        StructField("y",IntegerType,nullable=false),
        StructField("z",IntegerType,nullable=false),
        StructField("pos",ArrayType(IntegerType,containsNull=false),nullable=false)
      )
    )
  }

  override def serialize(obj: PosData): InternalRow = {
    val row = new GenericMutableRow(4)
    obj match {
      case pData: PosData =>
        row.setInt(0,pData.getX)
        row.setInt(1,pData.getY)
        row.setInt(2,pData.getZ)
        row.update(3,Seq(pData.getX,pData.getY,pData.getZ))
      case _ =>
        throw new RuntimeException("The given object:"+obj+" cannot be serialized by "+this)
    }
    row
  }

  override def deserialize(datum: Any): PosData = {
    datum match {
      case v: PosData =>
        System.err.println("Something strange happened, or was never serialized")
        v
      case r: InternalRow =>
        require(r.numFields==4,"Wrong row-length given "+r.numFields+" instead of 4")
        val x = r.getInt(0)
        val y = r.getInt(1)
        val z = r.getInt(2)
        val pos = r.getArray(3).toArray[Int](IntegerType)
        PosData(x,y,z)
    }
  }

  override def userClass: Class[PosData] = classOf[PosData]

  override def equals(o: Any) = o match {
    case v: PosData => true
    case _ => false
  }

  override def hashCode = 5577269
  override def typeName = "position"
  override def asNullable = this

}
