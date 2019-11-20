package ch.fourquant.images

import ch.fourquant.images.types.{NamedSQLImage, FullSQLImage}
import fourquant.imagej.{PortableImagePlus => PIP}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.{InternalRow, ScalaReflection}
import org.apache.spark.sql.execution.RDDConversions
import org.apache.spark.sql.sources._
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Row, SQLContext, SaveMode}
import org.apache.spark.{SparkContext, SparkException}

import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

/**
 * Provides access to Image data from pure SQL statements (i.e. for users of the
 * JDBC server).
 */
abstract class AbstractImageSource
  extends RelationProvider with CreatableRelationProvider {

  var tableType = "simple" //todo only works now as simple

  override def createRelation(sq: SQLContext, parameters: Map[String, String]):
  BaseRelation = {
    val pm = parseArguments(parameters)
    pm.getOrElse("debug","false") match {
      case "true" =>
        SimpleImageRelation(
          createTestImages(sq.sparkContext, pm)
        )(sq)
      case _ =>
        pm.get("path") match {
          case Some(path) => makeRelation(sq,path,pm)
          case None => throw new SparkException("Path is needed for creation")
        }
    }

  }

  def parseArguments(pm: Map[String,String]): Map[String,String] = {
    val fpm = pm.map(f => (f._1.toLowerCase(),f._2.toLowerCase()))
    tableType=fpm.getOrElse("table",tableType)
    fpm
  }
  override def createRelation(sq: SQLContext, mode: SaveMode, parameters: Map[String,
    String], data: DataFrame): BaseRelation = {

    parameters.get("path") match {
      case Some(path) => makeRelation(sq,path,parameters)
      case None => throw new SparkException("Path is needed for creation")
    }
  }



  def makeRelation(sq: SQLContext, path: String, pm: Map[String,String]) = {
    tableType match {
      case "simple" =>
        SimpleImageRelation(
          readImagesFromPath(sq.sparkContext, path, pm)
        )(sq)
      case "abstract" =>
        println("Using Abstract Backend")
        AbstractImageRelation[FullSQLImage](
          readImagesFromPath(sq.sparkContext, path, pm),
          AbstractImageSource.SimpleImage
        )(sq)
    }
  }

  def createTestImages(sc: SparkContext, parameters: Map[String,String]) = {
    val path = parameters.getOrElse("path","temp")
    val count = parameters.getOrElse("count","100").toInt

    import fourquant.imagej.scOps._
    () => sc.createEmptyImages(path, count, 100, 101,
      (i: Int) => (i - 1) * 1000 + 1000, None)
  }

  def readImagesFromPath(sc: SparkContext, path: String, parameters: Map[String,String]):
  () => RDD[(String,PIP)]
}


object AbstractImageSource extends Serializable {
  def SimpleImage(path: String,img: PIP) = new FullSQLImage(path,img)

}


/**
 * This is a very basic implementation of the BaseRelation for Images.
  *
  * @param baseRDD
 * @param pipToProd
 * @param sqlContext
 * @tparam A The type parameter allows for the exact specification of the table to be flexibly
 *           controlled based on what is needed and what other metadata is available
 */
case class AbstractImageRelation[A <: Product : TypeTag : ClassTag] protected[fourquant](
                                             baseRDD: () => RDD[(String,PIP)],
                                             pipToProd: (String,PIP) => A
                                                                                        )
                                              (@transient val sqlContext: SQLContext)
  extends BaseRelation with TableScan with Serializable {
  //TODO the typetag creates serialization problems in execution, fix this
  override def schema = ScalaReflection.schemaFor[A].dataType.asInstanceOf[StructType]

  override def buildScan(): RDD[Row] = {

    baseRDD().map(iv => pipToProd(iv._1, iv._2))
      .map(Row.fromTuple(_))
    // schema.map(_.dataType)

  }
}

case class SimpleImageRelation protected[fourquant](
                                                     baseRDD: () => RDD[(String,PIP)]
                                                     )(@transient val sqlContext: SQLContext)
  extends BaseRelation with TableScan with Serializable {
  override def schema = ScalaReflection.schemaFor[FullSQLImage].dataType.asInstanceOf[StructType]

  override def buildScan(): RDD[Row] =
      baseRDD().map(iv => new FullSQLImage(iv._1,iv._2)).map(Row.fromTuple(_))

}