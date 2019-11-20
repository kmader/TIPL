/**
 *
 */
package tipl.util

import org.apache.spark.SparkContext
import tipl.formats.{TImg, TImgRO}
import tipl.spark._
import tipl.tools.BaseTIPLPluginIn

import scala.reflect.ClassTag

/**
 * A collection of traits intended to be used to make TIPL plugins in spark easier and less
 * repetitive
 *
 * @author mader
 *
 */
trait SPlugin extends ITIPLPlugin {
  implicit val sc: SparkContext
}


trait SPluginIn extends SPlugin with ITIPLPluginIn {
  var images: Option[Array[DSImg[_]]] = None

  def getDesiredImageType(int: Int): Int

}


trait SPluginOut extends SPlugin with ITIPLPluginOut {

  override def ExportImages(template: TImgRO): Array[TImg]
}


trait SPluginIO extends SPluginIn with SPluginOut {
  def LoadDTImages(inImages: Array[DTImg[_]]): Unit

  def ExportDTImages(template: TImgRO): Array[DTImg[_]]

  override def LoadImages(inImages: Array[TImgRO]): Unit

  override def ExportImages(template: TImgRO): Array[TImg]

}

trait SInFixed extends SPluginIn {
  type inType
  val inTypeVal: Int

  implicit val num: Numeric[inType]
  implicit val ctIn: ClassTag[inType]

  override def LoadImages(inImages: Array[TImgRO]): Unit = {
    images = Some({
      for (iImg <- inImages.zipWithIndex)
        yield iImg match {
          case (sImg: DSImg[inType], _) => sImg
          case (osImg: DSImg[_], _) => osImg.changeTypes[inType](inTypeVal)
          //case (kvImg: KVImg[inType],_) => DSImg.fromKVImg(kvImg)
          //case (kvImg: KVImg[_],_) => DSImg.fromKVImg(kvImg).changeTypes[inType](inTypeVal)
          case (_, index) => new DSImg[inType](sc, iImg._1, inTypeVal)
          case _ => throw new IllegalArgumentException(iImg + " is not a supported image type")
        }
    })
  }
}

trait MapPlugin extends SPluginIO with SInFixed {

  type outType

}

abstract class SimpleIn extends BaseTIPLPluginIn with SInFixed {
  override type inType = Double
  override val inTypeVal: Int = TImgTools.IMAGETYPE_DOUBLE

  override def getDesiredImageType(int: Int): Int = inTypeVal


  override def getPluginName: String = "Simple"

  override def execute(): Boolean = true
}