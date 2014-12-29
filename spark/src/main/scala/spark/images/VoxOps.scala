package spark.images

import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import tipl.tools.BaseTIPLPluginIn
import tipl.util.D3int
import tipl.util.TIPLOps._

import scala.reflect.ClassTag


object VoxOps {


  /**
   * A very generic voxel operation taking a list of input voxels and producing an output voxel
   * @tparam A the type of the input image
   * @tparam B the type of the output image
   */
  trait VoxOp[A,B] extends Serializable {
    def neededVoxels(pos: D3int): Seq[D3int]
    def process(curvox: (D3int,A),voxs: Seq[(D3int,A)]): Seq[(D3int,B)]
  }
  trait stationaryVoxOp[A,B] extends VoxOp[A,B] {
    val relVoxels: Seq[D3int]
    override def neededVoxels(pos: D3int): Seq[D3int] = {
      relVoxels.map(_+pos)
    }
  }

  trait neighborhoodVoxOp[A,B] extends stationaryVoxOp[A,B] {
    def neighborSize: D3int
    def neighborKernel: BaseTIPLPluginIn.morphKernel
    val relVoxels: Seq[D3int] = {
      for(x<- 0 to neighborSize.gx();
          y<- 0 to neighborSize.gy();
          z<- 0 to neighborSize.gz();
          if neighborKernel.inside(0,0,0,x,0,y,0,z))
        yield new D3int(x,y,z)
    }
  }


  trait canApplyVoxOp[A] extends Serializable {
    def apply[B](vo: VoxOp[A,B]): canApplyVoxOp[B]
  }

  class listVoxOpImp[A](inList: Seq[(D3int,A)]) extends canApplyVoxOp[A] {
    override def apply[B](vo: VoxOp[A,B]): canApplyVoxOp[B] = {
      val oList = for(cVox <- inList;
          nPos = vo.neededVoxels(cVox._1);
          nVox = inList.filter(p => nPos.contains(p._1))
          )
        yield vo.process(cVox,nVox)
      new listVoxOpImp[B](oList.flatten)
    }
  }

  class rddVoxOpImp[A](inRdd: RDD[(D3int,A)])(implicit am: ClassTag[A]) extends canApplyVoxOp[A] {
    override def apply[B](vo: VoxOp[A, B]): canApplyVoxOp[B] = vo match {
      case nvo: neighborhoodVoxOp[A,B] => nvapply(nvo)
      case _ => sapply(vo)
    }
    def nvapply[B](nvo: neighborhoodVoxOp[A, B]): canApplyVoxOp[B] = {
      val bImg = inRdd.flatMap(
        curVox =>
          nvo.relVoxels.map(offset => (curVox._1-offset,curVox))
      ).groupByKey
      val cImg = bImg.map {
        inPts =>
          val (cPos, allPts) = inPts
          allPts.filter(_._1.isEqual(cPos)).headOption match {
            case Some(cPt) => Some((cPt,allPts))
            case None => None
          }
      }.filter(_.isDefined).map(_.get)
      val dImg = cImg.flatMap{
        cVals =>
          val (cVox,nVox) = cVals
          nvo.process(cVox,nVox.toSeq)
      }
      new rddVoxOpImp(dImg)
    }
    def sapply[B](vo: VoxOp[A, B]): canApplyVoxOp[B] = {
      val fImg = inRdd.flatMap(
        cVox => vo.neededVoxels(cVox._1).map((_,cVox))
      )
      // rearrange pixels
      val cImg = fImg.leftOuterJoin(inRdd).
        filter(_._2._2.isDefined).
        map {
        cPt =>
          val (nPos, (cVox, cVal)) = cPt
          (cVox, (nPos,cVal.get))
      }.groupByKey
      val dImg = cImg.flatMap{
        cVals =>
          val (cVox,nVox) = cVals
          vo.process(cVox,nVox.toSeq)
      }
      new rddVoxOpImp(dImg)
    }
  }




}
