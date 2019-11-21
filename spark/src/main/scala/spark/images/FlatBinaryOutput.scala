package spark.images

import java.io.{ObjectOutputStream, DataOutputStream}

import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.mapred.{Reporter, RecordWriter, JobConf, FileOutputFormat}
import org.apache.hadoop.util.Progressable
import scala.{specialized => spec}

/**
 * An easy way for storing key value pairs of array data
 * Created by mader on 3/27/15.
 */
class FixedBinaryOutput[@spec(Boolean, Byte, Short, Int, Long, Float, Double) K,
  @spec(Boolean, Byte, Short, Int, Long, Float, Double) V]
  extends FileOutputFormat[Array[K], Array[V]] {
  override def getRecordWriter(fileSystem: FileSystem, job: JobConf, name: String, progress:
  Progressable): RecordWriter[Array[K], Array[V]] = {
    val outPath = FileOutputFormat.getTaskOutputPath(job, name)
    val fs = outPath.getFileSystem(job)
    val outFile = fs.create(outPath, progress)
    new BinaryArrayOutputWriter[K, V](outFile)
  }

}

class BinaryArrayOutputWriter[@spec(Boolean, Byte, Short, Int, Long, Float, Double) K,
  @spec(Boolean, Byte, Short, Int, Long, Float, Double) V](out: DataOutputStream)
  extends RecordWriter[Array[K], Array[V]] {
  override def write(k: Array[K], v: Array[V]): Unit = {
    val oos = new ObjectOutputStream(out)
    writeArray(oos, k)
    writeArray(oos, v)
  }

  private def writeArray[T](oos: ObjectOutputStream, iArr: Array[T]) = {
    oos.writeObject(iArr)
  }

  override def close(reporter: Reporter): Unit = {
    out.close()
  }
}

trait FixedBinaryArrayWriter {
  def isMatch(inObj: AnyRef): Boolean

  def writeArray(os: DataOutputStream, inArr: AnyRef): Boolean
}

class FixedBinaryArrayOutputWriter[@spec(Boolean, Byte, Short, Int, Long, Float, Double) K,
  @spec(Boolean, Byte, Short, Int, Long, Float, Double) V](out: DataOutputStream,
                                                           writers: Array[FixedBinaryArrayWriter])
  extends RecordWriter[Array[K], Array[V]] {
  override def write(k: Array[K], v: Array[V]): Unit = {
    (writers.filter(_.isMatch(k)).headOption.map(_.writeArray(out, k)),
      writers.filter(_.isMatch(v)).headOption.map(_.writeArray(out, v))) match {
      case (Some(true), Some(true)) => true
      case (Some(false), Some(true)) =>
        throw new IllegalArgumentException("Keys could not be written")
      case (Some(true), Some(false)) =>
        throw new IllegalArgumentException("Values could not be written")
      case (None, _) => throw new IllegalArgumentException("Key " + k + " has no supported reader")
      case (_, None) => throw new IllegalArgumentException("Value " + v + " has no supported reader")
      case _ => throw new IllegalArgumentException("No supported readers:" + k + " or " + v)
    }


  }

  override def close(reporter: Reporter): Unit = {
    out.close()
  }

  private def writeArray[T](oos: ObjectOutputStream, iArr: Array[T]) = {
    oos.writeObject(iArr)
  }
}
