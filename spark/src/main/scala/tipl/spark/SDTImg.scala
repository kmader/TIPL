package tipl.spark

import scala.reflect.ClassTag
import tipl.util.D3int
import tipl.util.D3float
import tipl.formats.TImg

/**
 * Scala Version of KVImg
 * @author mader
 *
 */
abstract class SDTImg[T](dim: D3int, pos: D3int, elSize: D3float, imageType: Int)(implicit lm:
ClassTag[T])
  extends TImg.ATImg(dim, pos, elSize, imageType) with TImg {
}
