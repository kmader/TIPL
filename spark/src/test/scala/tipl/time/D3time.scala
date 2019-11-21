package tipl.time

import tipl.util.{D3int, ID2int, ID3int}

/**
 * A coordinate that contains time (usually extends a spatial coordinate, but needn't
 */
trait TimeCoordinate extends Serializable {
  def getTime(): Double
}

trait ID3Time extends ID2int with TimeCoordinate

trait ID4Time extends ID3int with TimeCoordinate


/**
 * For a series of 2D images, D3time replaces D3int to show the position of each slice in space
 * and time
 *
 * @param x the position of the bottom left corner of the slice
 * @param y the position of the bottom left corner of the slice
 * @param t the time point of the slice
 */
case class D3time(x: Int, y: Int, t: Double) extends ID2int with ID3Time {
  override def getTime(): Double = t

  override def gx(): Int = x

  override def setPos(ix: Int, iy: Int): ID2int = D3time(ix, iy, t)

  override def gy(): Int = y
}


/**
 * For a series of 3D images, D4time extends D3int
 *
 * @param tx
 * @param ty
 * @param tz
 * @param t time
 */
case class D4time(tx: Int, ty: Int, tz: Int, t: Double) extends D3int(tx, ty, tz) with
  ID4Time {
  override def getTime(): Double = t

  override def setPos(ix: Int, iy: Int) = D3time(ix, iy, t)

  override def setPos(ix: Int, iy: Int, iz: Int) = D4time(ix, iy, iz, t)
}
