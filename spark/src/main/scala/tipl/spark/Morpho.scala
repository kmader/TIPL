/**
 *
 */
package tipl.spark

import tipl.util.ArgumentParser
import tipl.formats.TImgRO
import tipl.tools.BaseTIPLPluginIO

/**
 * @author mader
 *
 */
abstract class Morpho extends BaseTIPLPluginIO {

  var labeledImage: KVImg[Boolean] = null

  override def setParameter(p: ArgumentParser, prefix: String): ArgumentParser = {
    p
  }

  override def LoadImages(inImages: Array[TImgRO]) = {

  }

}