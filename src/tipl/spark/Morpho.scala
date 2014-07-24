/**
 *
 */
package tipl.spark

import tipl.tools.BaseTIPLPluginIO
import tipl.util.ArgumentParser
import tipl.formats.TImgRO
import 

/**
 * @author mader
 *
 */
class Morpho extends BaseTIPLPluginIO {
  
  override def setParameter(p: ArgumentParser, prefix: String): ArgumentParser = {
    return p
  }
    var labeledImage: KVImg[Boolean] = null

  override def LoadImages(inImages: Array[TImgRO]) = {
    labeledImage = inImages(0) match {
      case m: KVImg[_] => m.toKVLong
      case m: DTImg[_] => m.asKV().toKVLong()
      case m: TImgRO => KVImg.ConvertTImg(SparkGlobal.getContext(getPluginName()), m, TImgTools.IMAGETYPE_INT).toKVLong()
    }
  }

}