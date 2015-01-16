package tipl.ij.scripting

import java.io._

import ij.plugin.PlugIn
import ij.plugin.filter.PlugInFilter
import ij.{WindowManager, ImagePlus}
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.lib.input.{CombineFileSplit, CombineFileRecordReader}
import org.apache.hadoop.mapreduce.{TaskAttemptContext, InputSplit}
import org.apache.spark.input.{BinaryRecordReader, BinaryFileInputFormat}
import tipl.formats.TImgRO
import tipl.ij.{ImageStackToTImg, Spiji}

object ImagePlusIO {
    /**
     * The new (Hadoop 2.0) InputFormat for imagej files (not be to be confused with the
     * recordreader
     * itself)
     */
    class ImagePlusFileInputFormat extends BinaryFileInputFormat[PortableImagePlus] {
      override def createRecordReader(split: InputSplit, taContext: TaskAttemptContext) = {
        new CombineFileRecordReader[String, PortableImagePlus](split.asInstanceOf[CombineFileSplit],
          taContext, classOf[ImagePlusRecordReader])
      }
    }


    class ImagePlusRecordReader(
                                 split: CombineFileSplit,
                                 context: TaskAttemptContext,
                                 index: Integer)
      extends BinaryRecordReader[PortableImagePlus](split, context, index) {

      def parseByteArray(path: Path,inArray: Array[Byte]) =
        new PortableImagePlus(Spiji.loadImageFromByteArray(inArray,path.toString().
          split("[.]").last))
    }

  /**
   * Since ImagePlus is not serializable this class allows for it to be serialized and thus used
   * in operations besides map in Spark.
   * @param baseData either an array of the correct type or an imageplus object
   */
  class PortableImagePlus(var baseData: Either[ImagePlus,AnyRef]) extends Serializable {
    def this(inImage: ImagePlus) = this(Left(inImage))
    def this(inArray: AnyRef) = this(Right(inArray))

    private def calcImg: ImagePlus =
      baseData match {
        case Left(tImg) => tImg
        case Right(tArr) => Spiji.createImage(File.createTempFile("img","").getName,tArr,false)
      }

    private def calcArray: AnyRef =
      baseData match {
        case Left(tImg) =>
          WindowManager.setTempCurrentImage(tImg)
          Spiji.getCurrentImage
        case Right(tArr) => tArr
      }

    lazy val curImg = calcImg
    lazy val curArr = calcArray
    def getImg() = curImg
    def getArray() = curArr

    override def toString(): String = {
      val nameFcn = (inCls: String) => this.getClass().getSimpleName()+"["+inCls+"]"
      baseData.fold(
        img => nameFcn(img.toString),
        arr => nameFcn(scala.runtime.ScalaRunTime.stringOf(arr))
      )
    }
    // useful commands in imagej
    def run(cmd: String, args: String = ""): PortableImagePlus = {
      WindowManager.setTempCurrentImage(curImg)
      Spiji.run(cmd,args)
      new PortableImagePlus(WindowManager.getCurrentImage())
    }

    def runAsPlugin(cmd: String, args: String = ""): Either[PlugIn,PlugInFilter] = {
      WindowManager.setTempCurrentImage(curImg)
      Spiji.runCommandAsPlugin(cmd,args) match {
        case plug: PlugIn =>
          Left(plug)
        case plugfilt: PlugInFilter =>
          Right(plugfilt)
      }
    }

    def getTImgRO(): TImgRO =
      ImageStackToTImg.FromImagePlus(curImg)

    // custom serialization
    @throws[IOException]("if the file doesn't exist")
    private def writeObject(oos: ObjectOutputStream): Unit = {
      oos.writeObject(curArr)
    }
    @throws[IOException]("if the file doesn't exist")
    @throws[ClassNotFoundException]("if the class cannot be found")
    private def readObject(in: ObjectInputStream): Unit =  {
      baseData = Right(in.readObject())
    }
    @throws(classOf[ObjectStreamException])
    private def readObjectNoData: Unit = {
      throw new IllegalArgumentException("Cannot have a dataless protableimageplus");
    }
  }
}
/**
 * Created by mader on 1/16/15.
 */
class ImagePlusIO {

}
