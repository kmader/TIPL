package org.apache.spark.input

import com.google.common.io.{ByteStreams, Closeables}
import org.apache.hadoop.mapreduce.InputSplit
import org.apache.hadoop.mapreduce.lib.input.CombineFileSplit
import org.apache.hadoop.mapreduce.RecordReader
import org.apache.hadoop.mapreduce.TaskAttemptContext
import tipl.formats.TiffFolder
import tipl.formats.TReader.TSliceReader
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.fs.Path


/**
 * The InputFormat for tiff files (not be to be confused with the recordreader itself)
 */
@serializable class WholeTiffFileInputFormat(
    split: CombineFileSplit,
    context: TaskAttemptContext,
    index: Integer)
 extends FileInputFormat[String,TSliceReader]  {
  
  def getRecordReader(split: InputSplit, taContext: TaskAttemptContext) = new WholeTiffSliceRecordReader(split,taContext,0)
  def createRecordReader(split: InputSplit, taContext: TaskAttemptContext) = getRecordReader(split,taContext)
}

/**
 * A [[org.apache.hadoop.mapreduce.RecordReader RecordReader]] for reading a single whole tiff file
 * out in a key-value pair, where the key is the file path and the value is the entire content of
 * the file as a TSliceReader (to keep the size information
 */
@serializable class WholeTiffSliceRecordReader(
    split: InputSplit,
    context: TaskAttemptContext,
    index: Integer)
     extends RecordReader[String, TSliceReader] {

  private val path = new Path(split.getLocations()(index))//.getPath(index)
  private val fs = path.getFileSystem(context.getConfiguration)

  // True means the current file has been processed, then skip it.
  private var processed = false

  private val key = path.toString
  private var value: TSliceReader = null
  override def initialize(split: InputSplit, context: TaskAttemptContext) = {}
  override def close() = {}

  override def getProgress = if (processed) 1.0f else 0.0f

  override def getCurrentKey = key

  override def getCurrentValue = value

  override def nextKeyValue = {
    if (!processed) {
      val fileIn = fs.open(path)
      val innerBuffer = ByteStreams.toByteArray(fileIn)
      value = new TiffFolder.TIFSliceReader(innerBuffer)
      Closeables.close(fileIn, false)

      processed = true
      true
    } else {
      false
    }
  }
}