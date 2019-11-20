package org.apache.spark.fourquant

import ch.fourquant.images.types.SerDeserHelper
import fourquant.imagej.ImagePlusIO.ImageLog
import fourquant.imagej.PortableImagePlus
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.{GenericMutableRow, MutableRow}
import org.apache.spark.sql.types
import org.apache.spark.sql.types.{UserDefinedType, StringType, StructField, StructType}

/**
 * the Sparksql user-defined type for the the PortableImagePlus
 * stores the pip as the log (as plain text json) and the array
 * the array is just an object which will then be recognized by the array to imageplus conversion
 */
class PipUDT extends UserDefinedType[PortableImagePlus] {
  @deprecated("this never worked but appeared elegant for keeping more complicated data-structures","1.0")
  private[PipUDT] case class CustomType(cls: Class[_]) extends types.DataType {
    override def defaultSize: Int =
      throw new UnsupportedOperationException("No size estimation available for objects.")

    def asNullable: types.DataType = this
  }

  private[PipUDT] val PipType = CustomType(classOf[PortableImagePlus])

  /** some serious cargo-cult, no idea hwo this is actually used **/
  override def sqlType: StructType = {
    StructType(
      Seq(
        StructField("jsonlog",StringType, nullable=false),
        StructField("fullimage",types.BinaryType,nullable=false)
      )
    )
  }

  override def serialize(obj: PortableImagePlus): InternalRow = {

    val row = new GenericMutableRow(2)
    obj match {
      case pData: PortableImagePlus =>
        import ch.fourquant.images.types.implicits._
        row.setString(0,pData.imgLog.toJSONString)
        row.update(1,SerDeserHelper.bytify(pData).get)
        row
      case cRow: MutableRow =>
        System.err.println(s"Something strange happened, or was already serialized: ${cRow}")
        cRow
      case _ =>
        throw new RuntimeException(s"The given class ${obj.getClass.getCanonicalName} containing " +
          s"object: ${obj.toString()} cannot be serialized by ${this.toString}")
    }
  }



  override def deserialize(datum: Any): PortableImagePlus = {
    datum match {
      case v: PortableImagePlus =>
        System.err.println("Something strange happened, or was never serialized")
        v
      case r: InternalRow =>
        require(r.numFields==2,"Wrong row-length given "+r.numFields+" instead of 2")
        val ilog = ImageLog.fromJSONString( r.getUTF8String(0).toString) //TODO fix conversion error
        //TODO this is really inefficient, but the code generation aspect of catalyst wont take anything else
        SerDeserHelper.debytify(r.getBinary(1)).get.asInstanceOf[PortableImagePlus]
      //new PortableImagePlus(Right(inArr),ilog)
    }
  }

  override def equals(o: Any) = o match {
    case v: PortableImagePlus => true
    case _ => false
  }

  override def hashCode = 9571330
  override def typeName = "PortableImagePlusSQL"
  override def asNullable = this

  override def userClass: Class[PortableImagePlus] = classOf[PortableImagePlus]
}
