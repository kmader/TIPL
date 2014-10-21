package org.apache.spark.rdd

import org.apache.hadoop.mapreduce.lib.input.{FileInputFormat => NewFileInputFormat}
import org.apache.hadoop.mapreduce.{InputFormat => NewInputFormat, Job => NewHadoopJob}
import org.apache.spark.input.BinaryFileInputFormat
import org.apache.spark.{Partition, SparkContext}

/**
 * Allows better control of the partitioning
 *
 */

import org.apache.hadoop.conf.{Configurable, Configuration}
import org.apache.hadoop.io.Writable
import org.apache.hadoop.mapreduce._

class BinaryFileRDD[T](
                        sc: SparkContext,
                        inputFormatClass: Class[_ <: BinaryFileInputFormat[T]],
                        keyClass: Class[String],
                        valueClass: Class[T],
                        @transient conf: Configuration,
                        minPartitions: Int)
  extends NewHadoopRDD[String, T](sc, inputFormatClass, keyClass, valueClass, conf) {

  override def getPartitions: Array[Partition] = {
    val inputFormat = inputFormatClass.newInstance
    inputFormat match {
      case configurable: Configurable =>
        configurable.setConf(conf)
      case _ =>
    }
    val jobContext = newJobContext(conf, jobId)
    inputFormat.setMaxSplitSize(jobContext, minPartitions)
    val rawSplits = inputFormat.getSplits(jobContext).toArray
    val result = new Array[Partition](rawSplits.size)
    for (i <- 0 until rawSplits.size) {
      result(i) = new NewHadoopPartition(id, i, rawSplits(i).asInstanceOf[InputSplit with Writable])
    }
    result
  }
}

