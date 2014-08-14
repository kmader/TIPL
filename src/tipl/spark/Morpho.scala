/**
 *
 */
package tipl.spark


/**
 * @author mader
 *
 */
abstract class Morpho extends BaseTIPLPluginIO {

  override def setParameter(p: ArgumentParser, prefix: String): ArgumentParser = {
    return p
  }

  var labeledImage: KVImg[Boolean] = null

  override def LoadImages(inImages: Array[TImgRO]) = {

  }

}