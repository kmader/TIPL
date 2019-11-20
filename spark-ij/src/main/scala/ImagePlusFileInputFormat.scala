package org.apache.spark.input {


import fourquant.imagej.{PortableImagePlus, Spiji}
import org.apache.hadoop.mapreduce.lib.input.{CombineFileRecordReader, CombineFileSplit}
import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}
;


/**
 * Created by mader on 3/3/15.
 */
/**
 * The new (Hadoop 2.0) InputFormat for imagej files (not be to be confused with the
 * recordreader
 * itself)
 */
class ImagePlusFileInputFormat extends StreamFileInputFormat[PortableImagePlus] {
  override def createRecordReader(split: InputSplit, taContext: TaskAttemptContext) = {
    new CombineFileRecordReader[String, PortableImagePlus](split.asInstanceOf[CombineFileSplit],
      taContext, classOf[ImagePlusRecordReader])
  }
}


class ImagePlusRecordReader(
                             split: CombineFileSplit,
                             context: TaskAttemptContext,
                             index: Integer)
  extends StreamBasedRecordReader[PortableImagePlus](split, context, index) {

  override def parseStream(inStream: PortableDataStream): PortableImagePlus =
    new PortableImagePlus(Spiji.loadImageFromByteArray(inStream.toArray(),
      inStream.getPath().toString().split("[.]").last))
}


}



