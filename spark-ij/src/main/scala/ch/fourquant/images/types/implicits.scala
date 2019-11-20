package ch.fourquant.images.types

import org.apache.spark.sql.catalyst.expressions.{MutableRow, GenericMutableRow}
import org.apache.spark.unsafe.types.UTF8String

/**
  * To make working with rows safer and easier to globally fix
  * Created by mader on 1/28/16.
  */
object implicits {


  /**
    * Created by mader on 1/28/16.
    */
  implicit class StringContainingRow(ir: MutableRow) {
    /**
      * Set the string as a UTF8String to keep SparkSQL happy
      * @param pos
      * @param rawString
      */
    def setString(pos: Int, rawString: String): Unit = {
      ir.update(pos,UTF8String.fromString(rawString))
    }
  }

}
