package spark.images

import tipl.tools.BaseTIPLPluginIn
import tipl.util.D3int

/**
 * Created by mader on 12/29/14.
 */
object FilterImpl {


  /**
   * A simple gaussian filter
   */
  trait GaussianFilter extends FilterImpl {
    val radius: Double
    override def neighborSize: D3int = new D3int(math.ceil(radius+1).toInt)
    override def isInside(a: D3int,b: D3int) = true
    override def kernelFactory() = BaseTIPLPluginIn.gaussFilter(radius)
  }

  /**
   * A simple median filter
   */
  trait MedianFilter extends FilterImpl {
    override def neighborSize: D3int
    override def isInside(a: D3int,b: D3int) = true
    override def kernelFactory() = BaseTIPLPluginIn.medianFilter(
      neighborSize.gx,
      neighborSize.gy,
      neighborSize.gz)
  }
}

trait FilterImpl extends Serializable {
  def neighborSize: D3int
  def isInside(a: D3int,b: D3int): Boolean
  def kernelFactory(): BaseTIPLPluginIn.filterKernel
}
