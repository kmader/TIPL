package tipl.spark.ij2.hadoop

/**
 * Created by mader on 12/15/14.
 */

import loci.common.{ByteArrayHandle, Location}
import net.imglib2.`type`.NativeType
import net.imglib2.`type`.numeric.RealType
import net.imglib2.`type`.numeric.real.{DoubleType, FloatType}
import net.imglib2.img.Img
import net.imglib2.img.array.ArrayImgFactory
import net.imglib2.io.ImgOpener
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}
import org.apache.hadoop.mapreduce.lib.input.{CombineFileRecordReader, CombineFileSplit}
import org.apache.spark.input.{BinaryFileInputFormat, BinaryRecordReader}


/**
 * whole tiff file
 * out in a key-value pair, where the key is the file path and the value is the entire content of
 * the file as a TSliceReader (to keep the size information
 */
abstract class ImgOpenerRecordReader[T <: RealType[T] with NativeType[T]](
                             split: CombineFileSplit,
                             context: TaskAttemptContext,
                             index: Integer)
  extends BinaryRecordReader[Img[T]](split, context, index) {

  def parseByteArray(path: Path,inArray: Array[Byte]): Img[T] = {
  /**
    val ir = new ImageReader
    val cR = ir.getReader(path.toString)
    **/
    val rawData = new ByteArrayHandle(inArray)
    // store the file temporarily
    Location.mapFile(path.toString,rawData)

    //val ir = ImgOpener.createReader(path.toString)
    //ir.ge
    val input: Img[T] = new ImgOpener().openImg(path.toString
    , getFactory,getType)
    Location.reset()

    input
  }

  def getType: T
  def getFactory: ArrayImgFactory[T]

}


/**
 * The new (Hadoop 2.0) InputFormat for tiff files (not be to be confused with the recordreader
 * itself)
 */
class FloatImgOpenerInputFormat extends BinaryFileInputFormat[Img[FloatType]] {
  override def createRecordReader(split: InputSplit, taContext: TaskAttemptContext) = {
    new CombineFileRecordReader[String, Img[FloatType]](split.asInstanceOf[CombineFileSplit],
      taContext, classOf[FloatRR])
  }
}

class FloatRR(split: CombineFileSplit,
               context: TaskAttemptContext,
               index: Integer) extends ImgOpenerRecordReader[FloatType](split,context,index) {

  override def getType = new FloatType

  override def getFactory = new ArrayImgFactory[FloatType]
}

/**
 * The new (Hadoop 2.0) InputFormat for tiff files (not be to be confused with the recordreader
 * itself)
 */
class DoubleImgOpenerInputFormat extends BinaryFileInputFormat[Img[DoubleType]] {
  override def createRecordReader(split: InputSplit, taContext: TaskAttemptContext) = {
    new CombineFileRecordReader[String, Img[DoubleType]](split.asInstanceOf[CombineFileSplit],
      taContext, classOf[DoubleRR])
  }
}

class DoubleRR(split: CombineFileSplit,
              context: TaskAttemptContext,
              index: Integer) extends ImgOpenerRecordReader[DoubleType](split,context,index) {

  override def getType= new DoubleType
  override def getFactory = new ArrayImgFactory[DoubleType]
}





