package tipl.spark.ij2

import net.imglib2.`type`.numeric.RealType
import net.imglib2.`type`.numeric.real.{DoubleType, FloatType}
import net.imglib2.img.Img
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.lib.input.{FileInputFormat => NewFileInputFormat}
import org.apache.hadoop.mapreduce.{InputFormat => NewInputFormat, Job => NewHadoopJob}
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.{OldBinaryFileRDD, RDD}
import tipl.spark.ij2.hadoop.{DoubleImgOpenerInputFormat, FloatImgOpenerInputFormat}

import scala.reflect.ClassTag

/**
 * Created by mader on 12/15/14.
 */
object IJ2Ops {


  implicit class imglibRDD[A , B<: RealType[B],C<: Img[B]](rd: RDD[(A,C)])(
  implicit ca: ClassTag[A], cc: ClassTag[C]
    ) {
    def gaussian() = {
      rd.mapValues{
        iImg =>
          net.imglib2.algorithm.gauss.Gauss.toFloat(Array(3.0,3.0),iImg)
      }
    }
  }

  /**
   * Add the byte reading to the SparkContext
   */
  implicit class imglibSparkContext(sc: SparkContext) {
    val defMinPart = sc.defaultMinPartitions

    def floatImages(path: String, minPartitions: Int = sc.defaultMinPartitions): RDD[(String,
      Img[FloatType])] = {
      val job = new NewHadoopJob(sc.hadoopConfiguration)
      NewFileInputFormat.addInputPath(job, new Path(path))
      val updateConf = job.getConfiguration
      new OldBinaryFileRDD(
        sc,
        classOf[FloatImgOpenerInputFormat],
        classOf[String],
        classOf[Img[FloatType]],
        updateConf,
        minPartitions).setName(path)
    }

    def doubleImages(path: String, minPartitions: Int = sc.defaultMinPartitions) = {
      val job = new NewHadoopJob(sc.hadoopConfiguration)
      NewFileInputFormat.addInputPath(job, new Path(path))
      val updateConf = job.getConfiguration
      new OldBinaryFileRDD(
        sc,
        classOf[DoubleImgOpenerInputFormat],
        classOf[String],
        classOf[Img[DoubleType]],
        updateConf,
        minPartitions).setName(path)
    }
  }

}
