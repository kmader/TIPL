/**
 *
 */
package tipl.util
import tipl.tools.BaseTIPLPluginIO
import tipl.spark.DTImg
import tipl.formats.{TImg,TImgRO}

/**
 * A trait intended for usage with DTImages 
 * @author mader
 *
 */
trait TTIPLPluginIO extends BaseTIPLPluginIO with ITIPLPluginIO {
	def LoadDTImages(inImages: Array[DTImg[_]]): Unit
	def ExportDTImages(template: TImgRO): Array[DTImg[_]]
	
	override def LoadImages(inImages: Array[TImgRO]): Unit
	override def ExportImages(template: TImgRO): Array[TImg] 
	
}