package fourquant.tiles

import org.apache.spark.annotation.Experimental

/**
 * A generic tiling strategy for ND images (turning into ImgLib2)
 * Created by mader on 4/11/15.
 */
trait TilingStrategy extends Serializable {
  /**
   * Create a list of tile positions given the image dimensions and the tile size
   * @param imgDim dimensions of the original image a 100x400 image would be {100,400}
   * @param tileDim dimensions of the tiles to use to cover the image
   * @note if imgDim has more dimensions than tileDim the other dimensions will be
   *       assumed to be 1
   * @return a list of tuples with the starting locations with the same size as the original
   *         image and the dimension which should in normal cases match the size of the tile
   */
  def createTiles(imgDim: Array[Long], tileDim: Array[Long]): Array[(Array[Long], Array[Long])]

}


/**
 * A strategy exclusively for 2D images, easier to implement than the generic approach, it works
 * in both directions
 */
trait TilingStrategy2D extends Serializable {
  def createTiles2D(fullWidth: Int, fullHeight: Int, tileWidth: Int, tileHeight: Int):
  Array[(Int, Int, Int, Int)]
}


object TilingStrategies extends Serializable {


  /**
   * Import Grid._ to make the default tiling a standard grid
   */
  object Grid extends Serializable {
    implicit val GridTiling = new SimpleGridStrategy()
    implicit val GridTiling2D = new Tiling2DFromTiling(new SimpleGridStrategy())
  }


  @Experimental
  object OverlappingGrid extends Serializable {
    //TODO implement this
  }


  implicit class Tiling2DFromTiling(ts: TilingStrategy) extends TilingStrategy2D {
    override def createTiles2D(fullWidth: Int, fullHeight: Int, tileWidth: Int, tileHeight: Int):
    Array[(Int, Int, Int, Int)] = ts.createTiles(Array[Long](fullWidth, fullHeight),
      Array[Long](tileWidth, tileHeight)).map {
      case (sPos, tSize) => (sPos(0).toInt, sPos(1).toInt, tSize(0).toInt, tSize(1).toInt)
    }
  }


  implicit class TilingFromTiling2D(ts: TilingStrategy2D) extends TilingStrategy {
    override def createTiles(imgDim: Array[Long], tileDim: Array[Long]): Array[(Array[Long],
      Array[Long])] = ts.createTiles2D(imgDim(0).toInt, imgDim(1).toInt,
      tileDim(0).toInt, tileDim(1).toInt).map {
      case (x, y, w, h) => (Array[Long](x, y), Array[Long](w, h))
    }
  }


  def cartesianProduct[T](xss: List[List[T]]): List[List[T]] = xss match {
    case Nil => List(Nil)
    case h :: t => for (xh <- h; xt <- cartesianProduct(t)) yield xh :: xt
  }

  private def roundDown(a: Long, b: Long): Long = {
    a % b match {
      case 0 => Math.floor(a / b).toLong - 1
      case _ => Math.floor(a * 1.0 / b).toLong
    }
  }


  class SimpleGridStrategy extends TilingStrategy {
    override def createTiles(imgDim: Array[Long], itileDim: Array[Long]): Array[(Array[Long],
      Array[Long])] = {
      assert(imgDim.length >= itileDim.length, "The image dimension count need to be larger than " +
        "the" +
        " " +
        "tiles")
      val tileDim = imgDim.zipWithIndex.map {
        case (iDim, ind) => if (ind < itileDim.length) itileDim(ind) else 1
      }
      val stopTiles = imgDim.zip(tileDim).map {
        case (fullDim: Long, tileDim: Long) => roundDown(fullDim, tileDim)
      }
      val tileVals = stopTiles.toList.map(eVal => (0 to eVal.toInt).map(_.toLong).toList)
      cartesianProduct(tileVals).map(
        sPos => (sPos.zip(tileDim).map(iv => iv._1 * iv._2).toArray, tileDim.toArray)).toArray
    }
  }


  class Simple2DGrid extends TilingStrategy2D {
    override def createTiles2D(fullWidth: Int, fullHeight: Int, tileWidth: Int, tileHeight: Int):
    Array[(Int, Int, Int, Int)] = {
      val endXtile = roundDown(fullWidth, tileWidth)
      val endYtile = roundDown(fullHeight, tileHeight)
      (for (stx <- 0 to endXtile.toInt; sty <- 0 to endYtile.toInt)
        yield (stx * tileWidth, sty * tileHeight, tileWidth, tileHeight)).toArray
    }
  }


}
