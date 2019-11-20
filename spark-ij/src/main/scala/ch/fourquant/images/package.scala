package ch.fourquant

import org.apache.spark.sql.SQLContext

/**
 * Add the implicit classes for automatically creating new tables based on images
 * Created by mader on 10/8/15.
 */
package object images {
  implicit class ImageContext(sq: SQLContext) extends Serializable {
    //TODO add new functions here
  }
}
