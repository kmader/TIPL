package spark.images

import spark.images.VoxOps.{neighborhoodArrayVoxOp, sisoArrayVoxOp}
import tipl.util.D3int

import scala.reflect.ClassTag

/**
 * A collection of operations for voxel processing of images
 * Created by mader on 12/29/14.
 */
object ExampleVoxOps {

  class DistMapOp(val ns: D3int)(implicit val atag: ClassTag[(Float, Boolean)],
                                 val btag: ClassTag[(Float, Boolean)]) extends
    neighborhoodArrayVoxOp[(Float, Boolean), (Float, Boolean)]
    with sisoArrayVoxOp[(Float, Boolean), (Float, Boolean)] {

    lazy val voxDist = neededVoxels().zipWithIndex.map(ioff => (ioff._2,
      Math.sqrt(Math.pow(ioff._1.gx, 2) + Math.pow(ioff._1.gy, 2) + Math.pow(ioff._1.gz, 2)))).toMap

    override def neighborSize: D3int = ns

    override def isInside(a: D3int, b: D3int): Boolean = true

    override def sprocess(curvox: (D3int, (Float, Boolean)),
                          voxs: Array[(Float, Boolean)]): (Float, Boolean) = {

      val nvox = voxs.zipWithIndex.filter(_._1._2)
      if (nvox.length > 0) {
        (nvox.map(invox => invox._1._1 + voxDist(invox._2)).min.toFloat, true)
      } else {
        curvox._2
      }

    }

  }

}
