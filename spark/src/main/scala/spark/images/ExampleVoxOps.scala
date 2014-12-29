package spark.images

import spark.images.VoxOps.neighborhoodVoxOp
import tipl.tools.BaseTIPLPluginIn
import tipl.tools.BaseTIPLPluginIn.morphKernel
import tipl.util.D3int

/**
 * A collection of operations for voxel processing of images
 * Created by mader on 12/29/14.
 */
object ExampleVoxOps {
  class imgFilter[A,Double](kernelFactory: Unit => BaseTIPLPluginIn.filterKernel,nsize: D3int,
                             mk: BaseTIPLPluginIn.morphKernel)(implicit tm: Numeric[A])
    extends neighborhoodVoxOp[A,Double] {
    override def neighborSize: D3int = nsize
    override def neighborKernel: morphKernel = mk

    override def process(curvox: (D3int, A), voxs: Seq[(D3int, A)]): Seq[(D3int, Double)] = {
      val kernel = kernelFactory()
      for(cPt <- voxs) kernel.addpt(curvox._1.gx,cPt._1.gx,
        curvox._1.gy,cPt._1.gy,
        curvox._1.gz,cPt._1.gz,tm.toDouble(cPt._2))
      Seq((curvox._1,kernel.value()))
    }
  }
}
