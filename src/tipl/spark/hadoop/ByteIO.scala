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
import scala.reflect.ClassTag


/**
 *  The new (Hadoop 2.0) InputFormat for while binary files (not be to be confused with the recordreader itself)
 */
@serializable abstract class WholeFileInputFormat[T]
 extends CombineFileInputFormat[String,T]  {
  override protected def isSplitable(context: JobContext, file: Path): Boolean = false
  /**
   * Allow minPartitions set by end-user in order to keep compatibility with old Hadoop API.
   */
  def setMaxSplitSize(context: JobContext, minPartitions: Int) {
    val files = listStatus(context)
    val totalLen = files.map { file =>
      if (file.isDir) 0L else file.getLen
    }.sum
    val maxSplitSize = Math.ceil(totalLen * 1.0 /
      (if (minPartitions == 0) 1 else minPartitions)).toLong
    super.setMaxSplitSize(maxSplitSize)
  }

  def createRecordReader(split: InputSplit, taContext: TaskAttemptContext): RecordReader[String,T]
  
}

  /**
 * A [[org.apache.hadoop.mapreduce.RecordReader RecordReader]] for reading a single whole tiff file
 * out in a key-value pair, where the key is the file path and the value is the entire content of
 * the file as a TSliceReader (to keep the size information
 */
@serializable abstract class WholeRecordReader[T](
    split: CombineFileSplit,
    context: TaskAttemptContext,
    index: Integer)
     extends RecordReader[String, T] {

  private val path = split.getPath(index)
  private val fs = path.getFileSystem(context.getConfiguration)

  // True means the current file has been processed, then skip it.
  private var processed = false

  private val key = path.toString
  private var value: T = null.asInstanceOf[T]
  override def initialize(split: InputSplit, context: TaskAttemptContext) = {}
  override def close() = {}

  override def getProgress = if (processed) 1.0f else 0.0f

  override def getCurrentKey = key

  override def getCurrentValue = value

  override def nextKeyValue = {
    if (!processed) {
      val fileIn = fs.open(path)
      val innerBuffer = ByteStreams.toByteArray(fileIn)
      value = parseByteArray(innerBuffer)
      Closeables.close(fileIn, false)

      processed = true
      true
    } else {
      false
    }
  }
  def parseByteArray(inArray: Array[Byte]): T
}

/**
 * This is not very efficient, but I am not comfortable enough with types and classtags to make in better
 */

@serializable class WholeByteInputFormat extends WholeFileInputFormat[Array[Byte]] {
 override def createRecordReader(split: InputSplit, taContext: TaskAttemptContext)= 
  {
    new CombineFileRecordReader[String,Array[Byte]](split.asInstanceOf[CombineFileSplit],taContext,classOf[WholeByteRecordReader])
  }
}

@serializable class WholeByteRecordReader(
    split: CombineFileSplit,
    context: TaskAttemptContext,
    index: Integer)
     extends WholeRecordReader[Array[Byte]](split,context,index) {
  
    def parseByteArray(inArray: Array[Byte]) = inArray
}
