package tipl.spark.hadoop 
import scala.collection.JavaConversions._
import com.google.common.io.{ByteStreams, Closeables}
import org.apache.hadoop.mapreduce.InputSplit
import org.apache.hadoop.mapreduce.lib.input.CombineFileSplit
import org.apache.hadoop.mapreduce.RecordReader
import org.apache.hadoop.mapreduce.TaskAttemptContext
import tipl.formats.TiffFolder
import tipl.formats.TReader.TSliceReader
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.lib.input.CombineFileInputFormat
import org.apache.hadoop.mapreduce.JobContext
import org.apache.hadoop.mapreduce.lib.input.CombineFileRecordReader
import org.apache.spark.rdd.RDD
import tipl.formats.TiffFolder.TIFSliceReader



/**
 *  The new (Hadoop 2.0) InputFormat for tiff files (not be to be confused with the recordreader itself)
 */
@serializable class WholeTiffFileInputFormat extends WholeFileInputFormat[TSliceReader] {
 override def createRecordReader(split: InputSplit, taContext: TaskAttemptContext)= 
  {
    new CombineFileRecordReader[String,TSliceReader](split.asInstanceOf[CombineFileSplit],taContext,classOf[WholeTiffSliceRecordReader])
  }
}
/**
 * A [[org.apache.hadoop.mapreduce.RecordReader RecordReader]] for reading a single whole tiff file
 * out in a key-value pair, where the key is the file path and the value is the entire content of
 * the file as a TSliceReader (to keep the size information
 */
@serializable class WholeTiffSliceRecordReader(
    split: CombineFileSplit,
    context: TaskAttemptContext,
    index: Integer)
     extends WholeRecordReader[TSliceReader](split,context,index) {
  
    def parseByteArray(inArray: Array[Byte]) = new TiffFolder.TIFSliceReader(inArray)
}

/** 
 *  Now the spark heavy classes linking Byte readers to Tiff Files
 */

import org.apache.spark.SparkContext._
import org.apache.spark.rdd.PairRDDFunctions._


class UnreadTiffRDD(srd: RDD[(String,Array[Byte])]) {
  def toTiffSlices() = {
    val tSlice = srd.first
    val decoders = TIFSliceReader.IdentifyDecoderNames(tSlice._2)
    srd.mapValues{new TIFSliceReader(_,decoders(0))}
  }
}
