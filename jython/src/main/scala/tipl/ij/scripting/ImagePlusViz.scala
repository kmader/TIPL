package tipl.ij.scripting

import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.servlet.http.HttpServletRequest

import fourquant.imagej.PortableImagePlus
import org.apache.spark.ui.tipl.WebViz
import org.apache.spark.ui.tipl.WebViz.{RDDInfo, ExtInfo}

import scala.reflect.ClassTag

/**
 * Created by mader on 3/9/15.
 */
/**
 * Support for ImagePlus
 * case ((_, firstImage: TImgRO), tRdd: RDD[(_, TImgRO)]) =>
 */
abstract class ImagePlusViz(parentTabPrefix: String, slicePageName: String)(
  implicit val et: ClassTag[PortableImagePlus]) extends WebViz.VizTool {
  val thumbPath = "/" + parentTabPrefix + "/" + slicePageName
  val slicePath = thumbPath + "/png"

  val format = "png"
  val imgSize = 250

  override type elementType = PortableImagePlus

  /**
   * Support the conversion of elements which have elementType in their key (less common)
   * @return
   */
  override def supportKeys(): Boolean = false

  override def typedRawRender(ele: elementType, info: ExtInfo, ri: RDDInfo, request:
  HttpServletRequest) = {
    val baos = new ByteArrayOutputStream()

    ImageIO.write(
      ele.getImg().getProcessor().resize(250).getBufferedImage,
      format, baos
    )
    baos.toByteArray
  }

}

