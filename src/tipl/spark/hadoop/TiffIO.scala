package tipl.spark.hadoop

import scala.collection.JavaConversions._
import org.apache.hadoop.mapreduce.InputSplit
import org.apache.hadoop.mapreduce.lib.input.CombineFileSplit
import org.apache.hadoop.mapreduce.TaskAttemptContext
import tipl.formats.TiffFolder
import org.apache.hadoop.mapreduce.lib.input.CombineFileRecordReader
import tipl.formats.TiffFolder.TIFSliceReader
import org.apache.spark.input.BinaryRecordReader
import org.apache.spark.input.BinaryFileInputFormat


/**
 *  The new (Hadoop 2.0) InputFormat for tiff files (not be to be confused with the recordreader itself)
 */
@serializable class TiffFileInputFormat extends BinaryFileInputFormat[TIFSliceReader] {
 override def createRecordReader(split: InputSplit, taContext: TaskAttemptContext)= 
  {
    new CombineFileRecordReader[String,TIFSliceReader](split.asInstanceOf[CombineFileSplit],taContext,classOf[TiffSliceRecordReader])
  }
}
/**
 * A [[org.apache.hadoop.mapreduce.RecordReader RecordReader]] for reading a single whole tiff file
 * out in a key-value pair, where the key is the file path and the value is the entire content of
 * the file as a TSliceReader (to keep the size information
 */
@serializable class TiffSliceRecordReader(
    split: CombineFileSplit,
    context: TaskAttemptContext,
    index: Integer)
     extends BinaryRecordReader[TIFSliceReader](split,context,index) {
  
    def parseByteArray(inArray: Array[Byte]) = new TiffFolder.TIFSliceReader(inArray)
}


