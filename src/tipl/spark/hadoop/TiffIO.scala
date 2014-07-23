package tipl.spark.hadoop 
import scala.collection.JavaConversions._
import com.google.common.io.{ByteStreams, Closeables}
import tipl.formats.TiffFolder
import tipl.formats.TReader.TSliceReader
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapred.FileInputFormat
import org.apache.hadoop.mapred.RecordReader
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapred.InputSplit
import org.apache.hadoop.mapred.FileSplit
import org.apache.hadoop.mapred.Reporter


/**
 *  The old (Hadoop 1.0) InputFormat for tiff files (not be to be confused with the recordreader itself)
 */
@serializable class WholeTiffFileInputFormat
 extends FileInputFormat[String,TSliceReader]  {


  def getRecordReader(genericSplit: InputSplit, job: JobConf,reporter: Reporter) =
  {
    reporter.setStatus(genericSplit.toString());
    // Check if we should throw away this split
    val start = genericSplit.asInstanceOf[FileSplit].getStart();
    val file = genericSplit.asInstanceOf[FileSplit].getPath();

    new WholeTiffSliceRecordReader(job,genericSplit,0)
  }
}

/**
 * A [[org.apache.hadoop.mapreduce.RecordReader RecordReader]] for reading a single whole tiff file
 * out in a key-value pair, where the key is the file path and the value is the entire content of
 * the file as a TSliceReader (to keep the size information
 */
@serializable class WholeTiffSliceRecordReader(
    job: JobConf,
    genericSplit : InputSplit ,
    index: Integer)
     extends RecordReader[String, TSliceReader] {

  private val path = genericSplit.asInstanceOf[FileSplit].getPath()
  private val fs = path.getFileSystem(job)
  
  
  // True means the current file has been processed, then skip it.
  private var processed = false

  private val key = path.toString
  private var value: TSliceReader = null
  
  override def close() = {}
  override def getPos() = 0
  
  override def getProgress = if (processed) 1.0f else 0.0f
  //override def getCurrentKey = key

  //override def getCurrentValue = value
  override def next(ikey: String, ival: TSliceReader) = nextKeyValue
  /**
   * The method from the new hadoop code (hide it for now since it doesnt override anything
   */
  private def nextKeyValue = {
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
  // since there is only one read step just do everything at the beginning
  override def createKey() = key
  override def createValue() = {
    nextKeyValue
    value
  }
}