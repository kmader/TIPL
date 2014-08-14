package tipl.spark.hadoop

import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}
import org.apache.hadoop.mapreduce.lib.input.{CombineFileRecordReader, CombineFileSplit}
import org.apache.spark.input.BinaryRecordReader
import org.apache.spark.input.BinaryFileInputFormat
import tipl.formats.TiffFolder.TIFSliceReader
import tipl.formats.TiffFolder


/**
 * The new (Hadoop 2.0) InputFormat for tiff files (not be to be confused with the recordreader itself)
 */
@serializable class TiffFileInputFormat extends BinaryFileInputFormat[TIFSliceReader] {
  override def createRecordReader(split: InputSplit, taContext: TaskAttemptContext) = {
    new CombineFileRecordReader[String, TIFSliceReader](split.asInstanceOf[CombineFileSplit], taContext, classOf[TiffSliceRecordReader])
  }
}

/**
 * A [[org.apache.hadoop.mapreduce.RecordReader R e c o r d R e a d e r]] for reading a single whole tiff file
 * out in a key-value pair, where the key is the file path and the value is the entire content of
 * the file as a TSliceReader (to keep the size information
 */
@serializable class TiffSliceRecordReader(
                                           split: CombineFileSplit,
                                           context: TaskAttemptContext,
                                           index: Integer)
  extends BinaryRecordReader[TIFSliceReader](split, context, index) {

  def parseByteArray(inArray: Array[Byte]) = new TiffFolder.TIFSliceReader(inArray)
}


