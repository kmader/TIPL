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
  trait GaussianFilter extends Serializable {
    val radius: Double
    def neighborSize: D3int = new D3int(math.ceil(radius+1).toInt)
    def isInside(a: D3int,b: D3int) = true
    def kernelFactory() = BaseTIPLPluginIn.gaussFilter(radius)
  }

  /**
   * A simple median filter
   */
  trait MedianFilter extends Serializable {
    def neighborSize: D3int
    def isInside(a: D3int,b: D3int) = true
    def kernelFactory() = BaseTIPLPluginIn.medianFilter(neighborSize.gx,neighborSize.gy, neighborSize.gz)
  }
}
