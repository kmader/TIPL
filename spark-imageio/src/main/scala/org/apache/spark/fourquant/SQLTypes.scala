package org.apache.spark.fourquant

import org.apache.spark.fourquant.SQLTypes.ArrayTile
import org.apache.spark.fourquant.SQLTypes.udt.{BooleanArrayTileUDT, ByteArrayTileUDT, DoubleArrayTileUDT}
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.GenericMutableRow
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Row, SQLContext}

import scala.reflect.ClassTag

/**
 * Created by mader on 4/21/15.
 */
object SQLTypes {
  abstract class ArrayTileUDT[@specialized(Byte, Char, Short, Int, Long, Float, Double)T]
    extends UserDefinedType[ArrayTile[T]] {
    def getElementType: DataType
    val ct : ClassTag[T]
    override def sqlType: StructType = {
      StructType(
        Seq(
          StructField("rows",IntegerType, nullable=false),
          StructField("cols",IntegerType, nullable=false),
          StructField("array",ArrayType(getElementType,containsNull=false),nullable=false)
        )
      )
    }

    override def serialize(obj: ArrayTile[T]): InternalRow = {
      val row = new GenericMutableRow(3)
      obj match {
        case pData: ArrayTile[T] =>
          row.setInt(0,pData.getRows)
          row.setInt(1,pData.getCols)
          row.update(2,pData.flatten.toSeq)
        case _ =>
          throw new RuntimeException("The given object:"+obj+" cannot be serialized by "+this)
      }
      row
    }

    override def deserialize(datum: Any): ArrayTile[T] = {
      datum match {
        case v: ArrayTile[T] =>
          System.err.println("Something strange happened, or was never serialized")
          v
        case r: Row =>
          require(r.length==3,"Wrong row-length given "+r.length+" instead of 3")
          val rows = r.getInt(0)
          val cols = r.getInt(1)
          val inArr = r.getAs[Iterable[T]](2).toArray(ct)
          ArrayTile[T](rows,cols,inArr)(ct)
      }
    }

    override def userClass: Class[ArrayTile[T]] = classOf[ArrayTile[T]]

    override def equals(o: Any) = o match {
      case v: ArrayTile[T] => true
      case _ => false
    }

    override def hashCode = 5577269
    override def typeName = "ArrayTile["+getElementType+"]"
    override def asNullable = this
  }

  trait ArrayTile[@specialized(Byte, Char, Short, Int, Long, Float, Double) T] extends
  Serializable {
    def getArray(): Array[Array[T]]
    def flatten(): Array[T]
    def getRows = getArray.length
    def getCols = getArray()(0).length
    def getRow(i: Int) = getArray()(i)
    def getCol(j: Int) = getArray().map(_(j))
    override def toString() = "Tile["+getArray()(0)(0).getClass.getSimpleName+
      "]("+getRows+","+getCols+")"
  }

  object ArrayTile extends Serializable {
    def apply[T : ClassTag](rows: Int, cols: Int, inArr: Array[T]) = {
      new ArrayTile[T]{
        private lazy val rwArray = rewrapArray(rows,cols,inArr)
        override def getArray(): Array[Array[T]] = rwArray
        override def flatten(): Array[T] = inArr
      }
    }

    //TODO check this function
    @deprecated("1.0","should not use in production until tested or replaced with matrix " +
      "implementation")
    def rewrapArray[T: ClassTag](rows: Int, cols: Int, inArr: Array[T]): Array[Array[T]] = {
      val outArray = new Array[Array[T]](rows)
      var i = 0
      var ind = 0
      while(i<rows) {
        outArray(i) = new Array[T](cols)
        var j=0
        while(j<cols) {
          outArray(i)(j)=inArr(ind)
          ind+=1
          j+=1
        }
        i+=1
      }
      outArray
    }
  }
  object udf extends Serializable {
    def threshold[T](inTile: ArrayTile[T],func: (T) => Boolean): BooleanArrayTile =
      BooleanArrayTile(inTile.getRows,inTile.getCols,inTile.flatten().map(func))
    def pixelCount(inTile: ArrayTile[Boolean]): Int = inTile.flatten().
      filter(_.booleanValue).length.toInt
    def getPixelVectors[T](offsetX: Int, offsetY: Int, inTile: ArrayTile[T],func: (T) => Boolean):
      Array[(Int,Int)] = {
        {
          val inArr = inTile.getArray()
          for (x <- 0 until inArr.length;
             cArr = inArr(x);
             y <- 0 until cArr.length;
             v = cArr(y);
             if func(v))
          yield (offsetX+x, offsetY+y)
      }.toArray
    }

    def registerAll(sq: SQLContext) = {
      sq.udf.register("threshold",(i: ArrayTile[Double], minVal: Double) => threshold(i,
        (v: Double) => v>minVal))
      sq.udf.register("pixelCount",(it: ArrayTile[Boolean]) => pixelCount(it))
      sq.udf.register("sparseThreshold",(x: Int, y: Int, i: ArrayTile[Double], minVal: Double) =>
        getPixelVectors[Double](x,y,i,(v: Double) => v>minVal))
    }
  }

  /**
   * Contains the user-defined types so they are easily distinguished from the standard code
   */
  object udt extends Serializable {
    class DoubleArrayTileUDT extends ArrayTileUDT[Double] {
      override def getElementType: DataType = DoubleType

      override val ct: ClassTag[Double] = implicitly[ClassTag[Double]]
    }
    class ByteArrayTileUDT extends ArrayTileUDT[Byte] {
      override def getElementType: DataType = ByteType

      override val ct: ClassTag[Byte] = implicitly[ClassTag[Byte]]
    }
    class BooleanArrayTileUDT extends ArrayTileUDT[Boolean] {
      override def getElementType: DataType = BooleanType
      override val ct: ClassTag[Boolean] = implicitly[ClassTag[Boolean]]
    }
  }


}


/**
 * NOTE: These should not be used, since there is no contract that they will come out like this
 * from other functions, use the ArrayTile construct
 */
@SQLUserDefinedType(udt = classOf[DoubleArrayTileUDT])
trait DoubleArrayTile extends ArrayTile[Double]

object DoubleArrayTile extends Serializable {
  def apply(rows: Int,cols: Int, inArr: Array[Double]) = new DoubleArrayTile {
    lazy val rwArray = ArrayTile.rewrapArray(rows,cols,inArr)
    override def getArray(): Array[Array[Double]] = rwArray

    override def flatten(): Array[Double] = inArr
  }

  def apply(inArr: Array[Array[Double]]) = new DoubleArrayTile {
    override def getArray(): Array[Array[Double]] = inArr
    override def flatten(): Array[Double] = getArray().flatten
  }
}


@SQLUserDefinedType(udt = classOf[ByteArrayTileUDT])
trait ByteArrayTile extends ArrayTile[Byte]

object ByteArrayTile extends Serializable {
  def apply(rows: Int,cols: Int, inArr: Array[Byte]) = new ByteArrayTile {
    lazy val rwArray = ArrayTile.rewrapArray(rows,cols,inArr)
    override def getArray(): Array[Array[Byte]] = rwArray
    override def flatten(): Array[Byte] = getArray().flatten
  }

  def apply(inArr: Array[Array[Byte]]) = new ByteArrayTile {
    override def getArray(): Array[Array[Byte]] = inArr
    override def flatten(): Array[Byte] = getArray().flatten
  }
}

@SQLUserDefinedType(udt = classOf[BooleanArrayTileUDT])
trait BooleanArrayTile extends ArrayTile[Boolean]

object BooleanArrayTile extends Serializable {
  def apply(rows: Int,cols: Int, inArr: Array[Boolean]) = new BooleanArrayTile {
    lazy val rwArray = ArrayTile.rewrapArray(rows,cols,inArr)
    override def getArray() = rwArray
    override def flatten() = getArray().flatten
  }

  def apply(inArr: Array[Array[Boolean]]) = new BooleanArrayTile {
    override def getArray() = inArr
    override def flatten() = getArray().flatten
  }
}