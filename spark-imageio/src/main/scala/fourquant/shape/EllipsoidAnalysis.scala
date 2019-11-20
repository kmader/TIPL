package fourquant.shape

import fourquant.arrays.ArrayPosition
import org.apache.spark.annotation.Experimental
import org.apache.spark.rdd.RDD

/**
 * Created by mader on 4/14/15.
 */
object EllipsoidAnalysis extends Serializable {


  /**
   * Generic representation of an N-dimensional ellipse
   * @param label the label for this point
   * @param dim the number of dimension
   * @param sum the sum of all the values in each dimension
   * @param sum2 the sum of the squares of all the values
   * @param min the minimum value along each dimension
   * @param max the maximum value along each dimension
   * @param cov the covariance matrix
   * @param count the number of pixels
   * @tparam A the type of the label
   */
  case class EllipseND[A](label: A, val dim: Int, private[shape] val sum: Array[Double],
                          private[shape] val sum2: Array[Double],
                          private[shape] val min: Array[Double],
                          private[shape] val max: Array[Double],
                          private[shape] val cov: Array[Array[Double]],
                          private[shape] var count: Long = 0L) {

    def this(label: A, dim: Int) = this(label,dim,
      sum = new Array[Double](dim),
      sum2 = new Array[Double](dim),
      min = Array.fill[Double](dim)(Double.MaxValue),
      max = Array.fill[Double](dim)(Double.MinValue),
      cov = Array.fill[Double](dim,dim)(0.0), count = 0L
    )

    def +=(el: EllipseND[_]): EllipseND[A] = {
      assert(dim==el.dim,"Dimensions must be the same")
      var i = 0
      while(i<dim) {
        sum(i)+=el.sum(i)
        sum2(i)+=el.sum2(i)
        min(i)=Math.min(min(i),el.min(i))
        max(i)=Math.max(max(i),el.max(i))
        i+=1
      }
      count+=el.count
      this
    }

    def +(el: EllipseND[_]): EllipseND[A] = {
      assert(dim==el.dim,"Dimensions must be the same")
      val out = new EllipseND(label,dim)
      var i = 0
      while(i<dim) {
        out.sum(i) = sum(i)+el.sum(i)
        out.sum2(i)= sum2(i)+el.sum2(i)
        out.min(i)=Math.min(min(i),el.min(i))
        out.max(i)=Math.max(max(i),el.max(i))
        i+=1
      }
      out.count=count+el.count
      out
    }

    def addPoint(pt: Array[Double]): EllipseND[A] = {
      var i = 0
      while(i<dim) {
        sum(i)+=pt(i)
        sum2(i)+=pt(i)*pt(i)
        min(i)=Math.min(min(i),pt(i))
        max(i)=Math.max(max(i),pt(i))
        var j = 0
        while(j<dim) {
          cov(i)(j) += pt(i)*pt(j)
          j+=1
        }

        i+=1
      }
      count+=1
      this
    }
    def addPoint[B: ArrayPosition](pt: B): EllipseND[A] =
      addPoint(implicitly[ArrayPosition[B]].getPos(pt).map(_.toDouble))
  }


  /**
   * Not sure if this class is even useful for anything
   * @param label
   * @param dim
   * @param sumw
   * @tparam A
   */
  @Experimental
  class WeightedEllipseND[A](override val label: A, override val dim: Int,
                                  private var sumw: Double = 0.0)
    extends EllipseND[A](label,dim) {

    def this(label: A, dim: Int) = this(label,dim,0.0)
    override def +=(el: EllipseND[_]): EllipseND[A] = {
      val out = super.+=(el)
      out match {
        case wel: WeightedEllipseND[_] =>
          sumw+=wel.sumw
          out
        case _ =>
          out
      }
    }

  }

  class Ellipse2D[A](override val label: A) extends EllipseND[A](label,2) {
    def addPoint(x: Double, y: Double): EllipseND[A] = addPoint(Array(x,y))

  }


  implicit class EllipseFunctions(el: EllipseND[_]) extends Serializable {
    def centerOfVolume(): Array[Double] = el.sum.map(_/el.count)
    def extents(): Array[Double] = el.max.zip(el.min).map(kv => kv._1-kv._2)
  }


  /** for exporting and using in sparkSQL
    *
    * @param comX
    * @param comY
    */
  case class ShapeInformation2D[A](label: A, comX: Double, comY: Double, extentsX: Double,
                                   extentsY: Double, area: Long)

  object ShapeInformation2D extends Serializable {
    def apply[A](el: EllipseND[A]): ShapeInformation2D[A] = {
      val cov = el.centerOfVolume()
      val ext = el.extents()
      ShapeInformation2D(el.label,cov(0),cov(1),ext(0),ext(1),el.count)
    }
  }

  /** for exporting and using in sparkSQL
    *
    * @param comX
    * @param comY
    */
  case class IntensityShape2D(label: Long, Intensity: Double ,
                                comX: Double, comY: Double,
                                extentsX: Double,
                                extentsY: Double, area: Long)

  object IntensityShape2D extends Serializable {
    def apply(el: EllipseND[(Long,Double)]): IntensityShape2D = {
      val cov = el.centerOfVolume()
      val ext = el.extents()
      IntensityShape2D(el.label._1,el.label._2,cov(0),cov(1),ext(0),ext(1),el.count)
    }
  }


  trait Indexable[A] extends Serializable {
    def toIndex(a: A): Long
  }
  object implicits extends Serializable {
    implicit val longIndexable = new Indexable[Long] {
      override def toIndex(a: Long): Long = a
    }
    implicit val longDoubleIndexable = new Indexable[(Long,Double)] {
      override def toIndex(a: (Long, Double)): Long = a._1
    }


  }

  def runShapeAnalysis[A: ArrayPosition, B: Indexable](img: RDD[(A,B)]) = {
    img.mapPartitions{
      inPointList =>
        val shapeMap = collection.mutable.Map[Long,EllipseND[B]]()
        for((ckey,cvalue)<-inPointList;
            idx = implicitly[Indexable[B]].toIndex(cvalue);
            pos = implicitly[ArrayPosition[A]].getPos(ckey)) {
          shapeMap.getOrElseUpdate(idx,
            new EllipseND(cvalue,pos.length)).addPoint(pos.map(_.toDouble))
        }
        shapeMap.toIterator
    }.reduceByKey{
      case (a,b) => a+=b
    }
  }

  def runShapeAnalysis2D[A: ArrayPosition, B: Indexable](img: RDD[(A,B)]) = {
    runShapeAnalysis(img).mapValues(
      iv => ShapeInformation2D(iv)
    ).map(_._2).sortBy(_.area,false)
  }

  def runIntensityShapeAnalysis[A: ArrayPosition](img: RDD[(A,(Long,Double))]) = {
    import implicits._
    runShapeAnalysis(img).mapValues(
      iv => IntensityShape2D(iv)
    ).map(_._2).sortBy(_.area,false)
  }


}
