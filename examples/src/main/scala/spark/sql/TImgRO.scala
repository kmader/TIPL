package spark.sql

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericMutableRow
import org.apache.spark.sql.types._

import tipl.formats.TImgRO
import tipl.formats.TImg.ArrayBackedTImg
import tipl.util.{ID2int, ID3int, D3float, D3int}


/**
 * A package to bring the TIPL tools into Spark SQL to make some of the analyses much easier to
 * run and analyze inside of spark without complex messy scala jobs
 * Created by mader on 4/23/15.
 */
@SQLUserDefinedType(udt = classOf[TImgROUDT])
trait SQLTImgRO extends Serializable {
  def getImg: TImgRO
}

@SQLUserDefinedType(udt = classOf[D3intUDT])
trait SQLD3int extends ID3int {

}
case class SQLD3I(x: Int, y: Int, z: Int) extends SQLD3int {
  override def gz(): Int = z

  override def setPos(ix: Int, iy: Int, iz: Int): ID3int = SQLD3I(ix,iy,iz)

  override def gx(): Int = x

  override def setPos(ix: Int, iy: Int): ID2int = SQLD3I(ix,iy,z)

  override def gy(): Int = y
}

object CoreOps {
  def makeImg(img: TImgRO) = new SQLTImgRO {
    override def getImg: TImgRO = img
  }
  def makeD3int(pos: ID3int) = SQLD3I(pos.gx,pos.gy,pos.gz)
}

class TImgROUDT extends UserDefinedType[SQLTImgRO] {
  val d3iType = new D3intUDT().sqlType
  val d3fType = new D3floatUDT().sqlType

  override def sqlType: DataType = StructType(
    Seq(
      StructField("pos",d3iType,nullable=false),
      StructField("dim",d3iType,nullable=false),
      StructField("elsize",d3fType,nullable=false),
      StructField("image",BinaryType,nullable=false)
    )
  )

  override def serialize(obj: Any): Any = {
    val row = new GenericMutableRow(4)
    obj match {
      case wrapData: SQLTImgRO =>
        val pData = wrapData.getImg
        val pos = pData.getPos
        row.update(0,pData.getDim)
        row.update(1,pData.getPos)
        row.update(2,pData.getElSize)
        row.update(3,ArrayBackedTImg.CreateFromTImg(pData,pData.getImageType))
      case _ =>
        throw new RuntimeException("The given object:"+obj+" cannot be serialized by "+this)
    }
    row
  }

  override def userClass: Class[SQLTImgRO] = classOf[SQLTImgRO]

  override def deserialize(datum: Any): SQLTImgRO =  datum match {
    case v: SQLTImgRO =>
      System.err.println("Something strange happened, or was never serialized")
      v
    case r: Row =>
      require(r.length==4,"Wrong row-length given "+r.length+" instead of 4")
      val dim = r.getAs[D3int](0)
      val pos = r.getAs[D3int](1)
      val elSize = r.getAs[D3float](2)
      CoreOps.makeImg(
        //ArrayBackedTImg.CreateFromSerializedData(dim,pos,elSize,r.getAs[Array[Byte]](3))
        r.getAs[TImgRO](3)
      )
  }
}


class D3intUDT extends UserDefinedType[SQLD3int]  {

  override def sqlType: DataType = StructType(
    Seq(
      StructField("x",IntegerType,nullable=false),
      StructField("y",IntegerType,nullable=false),
      StructField("z",IntegerType,nullable=false)
    )
  )

  override def serialize(obj: Any): Row = {
    val row = new GenericMutableRow(3)
    obj match {
      case pData: ID3int =>
        row.update(0,pData.gx)
        row.update(1,pData.gy)
        row.update(2,pData.gz)
      case _ =>
        throw new RuntimeException("The given object:"+obj+" cannot be serialized by "+this)
    }
    row
  }

  override def userClass: Class[SQLD3int] = classOf[SQLD3int]

  override def deserialize(datum: Any): SQLD3int = datum match {
    case v: ID3int =>
      System.err.println("Something strange happened, or was never serialized")
      CoreOps.makeD3int(v)
    case r: Row =>
      require(r.length==3,"Wrong row-length given "+r.length+" instead of 3")
      SQLD3I(
        r.getInt(0),
        r.getInt(1),
        r.getInt(2)
      )
  }
}

class D3floatUDT extends UserDefinedType[D3float]  {
  override def sqlType: DataType = StructType(
    Seq(
      StructField("x",FloatType,nullable=false),
      StructField("y",FloatType,nullable=false),
      StructField("z",FloatType,nullable=false)
    )
  )

  override def serialize(obj: Any): Row = {
    val row = new GenericMutableRow(3)
    obj match {
      case pData: D3float =>
        row.update(0,pData.x)
        row.update(1,pData.y)
        row.update(2,pData.z)
      case _ =>
        throw new RuntimeException("The given object:"+obj+" cannot be serialized by "+this)
    }
    row
  }

  override def userClass: Class[D3float] = classOf[D3float]

  override def deserialize(datum: Any): D3float =  datum match {
    case v: D3float =>
      System.err.println("Something strange happened, or was never serialized")
      v
    case r: Row =>
      require(r.length==3,"Wrong row-length given "+r.length+" instead of 3")
      new D3float(
        r.getFloat(0),
        r.getFloat(1),
        r.getFloat(2)
      )
  }
}



