package fourquant.arrays

/**
 * Basic tools for keeping track of positions and locations in 2D and beyond
 * Created by mader on 4/13/15.
 */
object Positions {

  implicit val labeledSliceToArrayPosition = new ArrayPosition[(String, Int,Int)] {
    override def getPos(a: (String, Int,Int)): Array[Int] = Array(a._2,a._3)

    override def getY(a: (String, Int,Int)): Int = a._3

    override def getSize(a: (String, Int,Int)): Int = 2

    override def getX(a: (String, Int,Int)): Int = a._2

    override def setPos(a: (String,Int,Int), b: Array[Int]): (String, Int, Int) = (a._1,b(0),b(1))

    /** metadata **/
    override def getMetadata(a: (String,Int,Int)): String = a._1

    override def setMetadata(a: (String, Int, Int), meta: String): (String, Int, Int) =
      (meta,a._2,a._3)
  }
  implicit val normalSliceToArrayPosition = new ArrayPosition[(Int,Int)] {
    override def getPos(a: (Int,Int)): Array[Int] = Array(a._1,a._2)

    override def getY(a: (Int,Int)): Int = a._2

    override def getSize(a: (Int,Int)): Int = 2

    override def getX(a: (Int,Int)): Int = a._1

    override def setPos(a: (Int,Int), b: Array[Int]): (Int, Int) = (b(0),b(1))

    /** metadata **/
    override def getMetadata(a: (Int, Int) ): String = ""
    override def setMetadata(a: (Int, Int), meta: String): (Int, Int) = {
      System.err.println("This type cannot store metadata")
      a
    }
  }

  implicit class SimplePosition[T: ArrayPosition](cp: T) extends Serializable {
    def getPos() = implicitly[ArrayPosition[T]].getPos(cp)
    def scalePos(scale: Double) = implicitly[ArrayPosition[T]].scalePos(cp,scale)
    def getX() = implicitly[ArrayPosition[T]].getX(cp)
    def getY() = implicitly[ArrayPosition[T]].getY(cp)
    def getZ() = implicitly[ArrayPosition[T]].getZ(cp)
    def getTime() = implicitly[ArrayPosition[T]].getTime(cp)

    def getMetadata() = implicitly[ArrayPosition[T]].getMetadata(cp)
    def setMetadata(meta: String) = implicitly[ArrayPosition[T]].setMetadata(cp,meta)

    def +[S: ArrayPosition](offset: S) = implicitly[ArrayPosition[T]].add(cp,offset)
  }

}

trait GlobalPosition[T] extends Serializable {

  def getPos(a: T): Array[Double]
  def setPos(a: T, b: Array[Double]): T
  def getSize(a: T): Int = getPos(a).length
  def getX(a: T): Double
  def getY(a: T): Double
  def getZ(a: T): Double

}


/**
 * A design choice was made to keep the positions as a helper trait so more types could be
 * supported particularly from other packages (GIS, etc) where the existing position classes are
 * likely similar, but non-identical to what we come up with here. Additionally for tile based
 * operations the size and cost of reading the position with such a function is minimal
 * @tparam T the type of the object to extract the position from
 */
trait ArrayPosition[T] extends Serializable {
  def getPos(a: T): Array[Int]
  /**
   * Allows the arrayposition to be updated without changing the other data in the structure
   * @param a old position
   * @param b the new position
   * @return the new object (since they might be immutable
   */
  def setPos(a: T, b: Array[Int]): T
  def setPos[S: ArrayPosition](a: T, b: S): T = setPos(a,implicitly[ArrayPosition[S]].getPos(b))

  /**
   * Scale the position by a factor
   * @param a the old position
   * @param scale the factor to scale by (Math.round)
   * @return the new position
   */
  def scalePos(a: T, scale: Double): T =
    setPos(a,getPos(a).map(v=> Math.round(v.toDouble*scale).toInt))

  def getSize(a: T): Int = getPos(a).length

  def add(a: T, offset: Array[Int]): T = {
    val offsetMap = offset.zipWithIndex.map(_.swap).toMap
    val newPos = getPos(a).zipWithIndex.map{
      case (iPos,iIdx) => iPos+offsetMap.getOrElse(iIdx,0)
    }
    setPos(a,newPos)
  }

  def add[S: ArrayPosition](a: T,offset: S): T =
    add(a,implicitly[ArrayPosition[S]].getPos(offset))

  /** metadata **/
  def getMetadata(a: T): String

  /**
   * Creates a new element with the given metadata
   * @param a the old variable
   * @param meta the new meta-data contents
   * @return
   */
  def setMetadata(a: T, meta: String): T

  def appendMetadata(a: T, meta: String): T = setMetadata(a,getMetadata(a)+meta)

  /** basic 2D functions **/
  def getX(a: T): Int
  def getY(a: T): Int
  /** 3d function **/
  def getZ(a: T): Int = 0

  def getTime(a: T): Int = 0
}

