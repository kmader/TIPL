package tipl.spark

import scala.reflect.ClassTag

/**
 * Scala Version of KVImg
 * @author mader
 *
 */
@serializable abstract class SDTImg[T](dim: D3int, pos: D3int, elSize: D3float, imageType: Int)(implicit lm: ClassTag[T])
  extends TImg.ATImg(dim, pos, elSize, imageType) with TImg {
}
