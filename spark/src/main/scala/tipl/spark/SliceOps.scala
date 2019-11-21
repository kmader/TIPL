package tipl.spark

import tipl.util.{TImgSlice, TImgTools}

import scala.reflect.ClassTag

object SliceOps {
  implicit class FancyTImgSlice[T: ClassTag](ts: TImgSlice[Array[T]]) {
    /**
     * Concatenate two images together for multivalued data
     *
     * @param otherSlice the other slice (must be the same size)
     * @tparam V the type
     * @return a new slice with a tuple2 type
     */
    def ++[V: ClassTag](otherSlice: TImgSlice[Array[V]]):
    TImgSlice[Array[(T, V)]] = {
      TImgSlice.doSlicesSizeMatch(ts, otherSlice)
      val combArr = ts.get.zip(otherSlice.get)
      new TImgSlice[Array[(T, V)]](
        combArr.toArray,
        ts
      )
    }

    /**
     * Add two numeric supported slices together
     *
     * @param otherSlice
     * @param numt
     * @param numv
     * @tparam V
     * @return double image
     */
    def +[V: ClassTag](otherSlice: TImgSlice[Array[V]])(
      implicit numt: Numeric[T], numv: Numeric[V]) = {
      TImgSlice.doSlicesSizeMatch(ts, otherSlice)
      val combArr = ts.get.zip(otherSlice.get).
        map(i =>
          numt.toDouble(i._1) + numv.toDouble(i._2)
        )
      new TImgSlice[Array[Double]](
        combArr.toArray,
        ts
      )
    }

    def changeType[V](outType: Int)(implicit ctv: ClassTag[V],
                                    numv: Numeric[V]): TImgSlice[Array[V]]
    = {
      TImgTools.isValidType(outType)
      new TImgSlice[Array[V]](
        TypeMacros.castArr(ts.getAsDouble, outType).asInstanceOf[Array[V]]
        , ts)
    }

  }
}
