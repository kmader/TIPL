package tipl.util

import scala.{specialized => spec}

/**
  * The representation of a single block in an image (typically a slice) containing the original
  * position and any offset from this position which is useful for filtering
  *
  * @param <V> The class of the data inside (typically int[] or boolean[])
  * @author mader
  */

class TBlockSlice[V](sdim: ID2int,
  pos: D3int,
  offset: D3int,
  var sliceData: Array[V])
  extends TImgBlock[V](new D3int(sdim, 1), pos, offset, sliceData) {
  val zero = new D3int(0)
  def this(sdim: ID2int,
    pos: D3int,
    offset: D3int) = this(new D3int(sdim, 1), pos, offset, null)

  def this(isliceData: Array[V],pos: D3int,dim: D3int) = this(dim,pos,D3int.zero,isliceData)


}
