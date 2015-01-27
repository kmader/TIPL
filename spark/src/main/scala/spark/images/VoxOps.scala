package spark.images

import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import tipl.spark.{DSImg, KVImg}
import tipl.tools.BaseTIPLPluginIn
import tipl.util.{TImgSlice, TImgTools, D3int}
import tipl.util.TIPLOps._

import scala.reflect.ClassTag


object VoxOps {


  /**
   * A very generic voxel operation taking a list of input voxels and producing an output voxel
   * @tparam A the type of the input image
   * @tparam B the type of the output image
   */
  trait VoxOp[A,B] extends Serializable {
    val atag: ClassTag[A]
    val btag: ClassTag[B]
    def neededVoxels(pos: D3int): Seq[D3int]
    def process(curvox: (D3int,A),voxs: Seq[(D3int,A)]): Seq[(D3int,B)]
  }

  trait ArrayVoxOp[A,B] extends Serializable {
    val atag: ClassTag[A]
    val btag: ClassTag[B]
    def neededVoxels(): Array[D3int]
    def process(curvox: (D3int,A),voxs: Array[A]): Seq[(D3int,B)]
  }

  /**
   * Converts an array operation to a standard operation
   * @param avo the array operation to replace
   * @param defVal the value to use around the border (if a value is missing)
   * @tparam A the type of the input
   * @tparam B the type of the output
   */
  class ArrayVoxOpToVoxOp[A,B](avo: ArrayVoxOp[A,B], defVal: A)(
                                       implicit val atag: ClassTag[A],
                                                val btag: ClassTag[B]) extends stationaryVoxOp[A,B]
  {

    override def relVoxels: Seq[D3int] = avo.neededVoxels()
    override def process(curvox: (D3int, A), voxs: Seq[(D3int, A)]): Seq[(D3int, B)] = {
      val vmap = voxs.toMap
      avo.process(curvox,relVoxels.map(vmap.getOrElse(_,defVal)).toArray)
    }
  }

  trait stationaryVoxOp[A,B] extends VoxOp[A,B] {
    def relVoxels: Seq[D3int]
    override def neededVoxels(pos: D3int): Seq[D3int] = {
      relVoxels.map(_+pos)
    }
  }


  /**
   * Single input single output array voxel operation
   * This makes computing outputs on slice based data significantly easier
   * @tparam A the type of the input
   * @tparam B the type of the output
   */
  trait sisoArrayVoxOp[A,B] extends ArrayVoxOp[A,B] {
    def sprocess(curvox: (D3int,A),voxs: Array[A]): B
    override def process(curvox: (D3int,A),voxs: Array[A]): Seq[(D3int,B)] =
      Seq((curvox._1,sprocess(curvox,voxs)))
  }
  trait neighborhoodArrayVoxOp[A,B] extends ArrayVoxOp[A,B] {
    def neighborSize: D3int

    def isInside(a: D3int,b: D3int): Boolean
    lazy val nvList = {
      val lseq = for(z<- -neighborSize.gz to neighborSize.gz;
                     y<- -neighborSize.gy to neighborSize.gy;
                     x<- -neighborSize.gx  to neighborSize.gx;
                     if isInside(D3int.zero, new D3int(x,y,z)))
      yield new D3int(x,y,z)
      lseq.toArray
    }

    override def neededVoxels() = nvList

  }



  trait neighborhoodVoxOp[A,B] extends stationaryVoxOp[A,B] {
    def neighborSize: D3int

    def isInside(a: D3int,b: D3int): Boolean
    lazy val nvList = {
      for(z<- -neighborSize.gz to neighborSize.gz;
                     y<- -neighborSize.gy to neighborSize.gy;
                     x<- -neighborSize.gx  to neighborSize.gx;
                     if isInside(new D3int(0), new D3int(x,y,z)))
      yield new D3int(x,y,z)
    }
    def relVoxels: Seq[D3int] = nvList
  }

  trait voxelNeighborFilter[A] extends neighborhoodVoxOp[A,Double] {
    implicit val tm: Numeric[A]

    def kernelFactory(): BaseTIPLPluginIn.filterKernel

    override def process(curvox: (D3int, A), voxs: Seq[(D3int, A)]): Seq[(D3int, Double)] = {
      val kernel = kernelFactory()
      for(cPt <- voxs) kernel.addpt(
        curvox._1.gx,cPt._1.gx,
        curvox._1.gy,cPt._1.gy,
        curvox._1.gz,cPt._1.gz,tm.toDouble(cPt._2))
      Seq((curvox._1,kernel.value()))
    }
  }

  abstract class VoxelFilter[A](implicit val tm: Numeric[A],
                          val atag: ClassTag[A],
                          val btag: ClassTag[Double]) extends voxelNeighborFilter[A]


  // voxel implementations

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


  implicit class rddVoxOpImp[A](inRdd: RDD[(D3int,A)])(implicit val atag: ClassTag[A]) extends
  canApplyVoxOp[A] {
    def getBaseImg() = inRdd
    override def apply[B](vo: VoxOp[A, B]): rddVoxOpImp[B] =
      vo match {
      case nvo: neighborhoodVoxOp[A,B] => nvapply(nvo)
      case _ => sapply(vo)
      }
    def nvapply[B](nvo: neighborhoodVoxOp[A, B]): rddVoxOpImp[B] = {
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
      new rddVoxOpImp(dImg)(nvo.btag)
    }
    def sapply[B](vo: VoxOp[A, B]): rddVoxOpImp[B] = {
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
      new rddVoxOpImp(dImg)(vo.btag)
    }
  }

  implicit class kvImgVoxOpImp[A](ikv: KVImg[A])(implicit am: ClassTag[A]) extends
  rddVoxOpImp[A](ikv.getBaseImg()) {

    def apply[B](vo: VoxOp[A, B],paddingVal: B,outType: Int = TImgTools.IMAGETYPE_UNKNOWN):
    kvImgVoxOpImp[B] = {
      val newRdd = super.apply(vo).getBaseImg()
      new kvImgVoxOpImp[B](KVImg.fromRDD(ikv,outType,newRdd,paddingVal)(vo.btag))(vo.btag)
    }

    def apply[B](vo: VoxOp[A, B],outType: Int = TImgTools.IMAGETYPE_UNKNOWN)(implicit nm:
    Numeric[B]): kvImgVoxOpImp[B] = apply(vo,nm.zero,outType)
  }

  implicit class dsImgVoxOpImp[A](ids: DSImg[A])(implicit am: ClassTag[A]) extends Serializable {

    def apply[B](vo: VoxOp[A, B],paddingValA: A, paddingValB: B,
                 outType: Int = TImgTools.IMAGETYPE_UNKNOWN) =
      vo match {
      case nvo: neighborhoodVoxOp[A,B] => nvapply(nvo)
      case _ => kvapply(DSImg.toKVImg(ids,paddingValA),vo,paddingValB,ids.getImageType)
    }

    def apply[B](vo: VoxOp[A, B],outType: Int = TImgTools.IMAGETYPE_UNKNOWN)(implicit nma:
    Numeric[A], nmb: Numeric[B]) =
      vo match {
        case nvo: neighborhoodVoxOp[A,B] => nvapply(nvo)
        case _ => kvapply(DSImg.toKVImg(ids,nma.zero),vo,nmb.zero,ids.getImageType)
      }

    def kvapply[B](ikv: KVImg[A],vo: VoxOp[A,B], paddingVal: B, imgType: Int) =
      ikv(vo,paddingVal,imgType)

    def nvapply[B](nvo: neighborhoodVoxOp[A,B]): dsImgVoxOpImp[B] = ??? //TODO implement slice
    // based method
  }

  trait canApplyArrayVoxOp[A] extends Serializable {
    def aapply[B](vo: ArrayVoxOp[A,B], paddingValue: A): canApplyArrayVoxOp[B]
  }

  trait canApplySISOArrayVoxOp[A] extends Serializable {
    def aapply[B](vo: sisoArrayVoxOp[A,B], paddingValue: A): canApplySISOArrayVoxOp[B]
  }

  class listArrVoxOpImp[A](inList: Seq[(D3int,A)]) extends canApplyArrayVoxOp[A] {
    override def aapply[B](avo: ArrayVoxOp[A,B], paddingValue: A): listArrVoxOpImp[B] = {
      val outVox = avo.neededVoxels()
      val pointMap = inList.toMap
      val oList = for(cVox <- inList;
                      voxs = outVox.map(pointMap.getOrElse(_,paddingValue)).toArray(avo.atag))
        yield avo.process(cVox,voxs)
      new listArrVoxOpImp[B](oList.flatten)
    }
  }

  implicit class rddArrVoxOpImp[A](inRdd: RDD[(D3int,A)]) extends canApplyArrayVoxOp[A] {
    override def aapply[B](avo: ArrayVoxOp[A,B], paddingValue: A): rddArrVoxOpImp[B] = {
      val outVox = avo.neededVoxels()
      val bImg = inRdd.flatMap(
        curVox =>
          outVox.map(offset => (curVox._1-offset,curVox))
      ).groupByKey

      val cImg = bImg.map {
        inPts =>
          val (cPos, allPts) = inPts
          allPts.filter(_._1.isEqual(cPos)).headOption match {
            case Some(cPt) => Some((cPt,allPts))
            case None => None
          }
      }.filter(_.isDefined).map(_.get)
      val fImg = cImg.mapValues{
        inVals =>
          val curMap = inVals.toMap
          outVox.map(curMap.getOrElse(_,paddingValue)).toArray(avo.atag)
      }.flatMap{
        inKV =>
          avo.process(inKV._1,inKV._2)
      }

      new rddArrVoxOpImp[B](fImg)
    }

    implicit class rddArrSliceOpImp[A](inRdd: RDD[(D3int,TImgSlice[Array[A]])])
      extends canApplySISOArrayVoxOp[A] {
      override def aapply[B](vo: sisoArrayVoxOp[A, B], paddingValue: A): rddArrSliceOpImp[B] = {
        val nv = vo.neededVoxels()
        implicit val atag: ClassTag[A] = vo.atag
        implicit val btag: ClassTag[B] = vo.btag
        val nslices = nv.map(_.gz).distinct
        val gSlices = inRdd.flatMap {
          inKV =>
            val cpt = inKV._1
            for(z <- nslices) yield (
              new D3int(cpt.gx,cpt.gy,cpt.gz+z),
              (cpt,inKV._2)
              )
        }.groupByKey

        val outImage = gSlices.map{
          inKV =>
            val (pos,pts) = inKV
            val coreSlice = pts.filter(_._1.isEqual(pos)).head._2
            val coreSliceData = coreSlice.get
            val sliceMap = pts.map(cslice => (cslice._1.gz-pos.gz,cslice._2.get())).toMap
            val dim = coreSlice.getDim()

            val outSlice = new Array[B](dim.gx*dim.gy)
            var i=0
            // for every voxel in the output slice
            for(
              iy<- pos.gy to pos.gy+dim.gy;
              ix<- pos.gx to pos.gx+dim.gx) {
              val cdex = (iy - pos.gy) * dim.gx + (ix - pos.gx)
              val arrVals = nv.map {
                relpos =>
                  val slicedex = (iy + relpos.gy - pos.gy) * dim.gx + (ix + relpos.gx - pos.gx)
                  sliceMap.get(relpos.gz).
                    map(_.get(slicedex)).
                    getOrElse(Some(paddingValue)).
                    getOrElse(paddingValue)
              }

              outSlice(i)=vo.sprocess((pos,coreSliceData(cdex)),arrVals)

              i+=1
            }
            (pos,new TImgSlice(outSlice,coreSlice))
        }
        new rddArrSliceOpImp[B](outImage)
      }
    }


  }



}
