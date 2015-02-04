package tipl.spark

import org.scalatest.FunSuite
import org.scalatest._
import Matchers._
import spark.images.{ImageTools2D, ImageTools}
import tipl.tests.TestPosFunctions
import tipl.tests.TestPosFunctions.{ProgYImage, ProgZImage, ProgXImage}
import tipl.util.{TIPLGlobal, TImgSlice, TImgTools, D3int}
import tipl.util.TIPLOps._
import tipl.spark.DSImg.FancyTImgSlice
/**
 * Created by mader on 12/19/14.
 */
class VoxelOpsTest extends FunSuite {
  val (xs,ys,zs) = (3,3,3)
  val ptList = for(x<-1 to xs; y<-1 to ys; z<-1 to zs) yield new D3int(x,y,z)
  test("Point list has correct size") {
    assert(ptList.length == (xs)*(ys)*(zs))
  }
  for(wind<-1 to 3) {
    test("Single Point Spread: "+wind) {
      val spd = ImageTools.spread_voxels((new D3int(1,1,1),"test"),new D3int(wind,wind,wind),true)
      assert(spd.length == (wind+1)*(wind+1)*(wind+1),"Forward length")

      assert(spd.filter(_._2._2==0).length == 1,"Forward direction")

      val spd2 = ImageTools.spread_voxels((new D3int(1,1,1),"test"),new D3int(wind,wind,wind),false)
      assert(spd2.length == (2*wind+1)*(2*wind+1)*(2*wind+1),"Both length")

      assert(spd2.filter(_._2._2==0).length == 1,"Both direction")
    }
  }

  test("Check that pixel spreading occurs correctly") {
    val wind = 1
    val valList = ptList.map(inpt => (inpt,Some(inpt)))
    val spreadList = valList.flatMap(ImageTools.spread_voxels(_,new D3int(wind,wind,wind),false))
    val spreadZero = ImageTools.spread_zero(new D3int(wind,wind,wind),false)
    assert(spreadList.length==(ptList.length*(2*wind+1)*(2*wind+1)*(2*wind+1)),"Check Length")
    val gList = spreadList.groupBy(_._1)
    assert(gList.size==(xs+2)*(ys+2)*(zs+2),"Grouped length")
    val noEdge = gList.filter(
      kv =>
        kv._2.filter(_._2._2==spreadZero).length==1 // exactly one original point
    )
    assert(noEdge.size==valList.length,"No Edge length")

    val feat_vec_len = (2*wind+1)*(2*wind+1)*(2*wind+1)
    val feat_vec = noEdge.map(
      kv => {
        val outArr = Array.fill[Option[D3int]](feat_vec_len)(None)
        val iter = kv._2.toIterator
        while (iter.hasNext) {
          val outVal = iter.next
          outArr(outVal._2._2.index) = outVal._2._1
        }
        (kv._1,
          outArr
          )
      }
    )

    if (TIPLGlobal.getDebug) {
      println(feat_vec.map(kv => kv._1.toString + " : " +
        (kv._2.map(iv => iv.map(i => (i - kv._1).toString).getOrElse("_")).mkString("\t"))).
        mkString("\n"))
    }
    val fvec = feat_vec.
      map(kv => kv._2.map(_.map(_-kv._1))). // remove the base position
      reduce{ // ensure all of the elements that are present are equal
      (a,b) =>
        a.zip(b).map(i =>
          i match {
            case (Some(lef),Some(rig)) =>
              assert(lef.x == rig.x,"x pos")
              assert(lef.y == rig.y,"y pos")
              assert(lef.z == rig.z,"z pos")
              Some(lef)
            case (Some(lef),None) => Some(lef)
            case (None,Some(rig)) => Some(rig)
            case (None,None) => None
          }
        )
    }
    assert(fvec.map(_.isDefined).reduce(_ && _),"All elements present")
    println("Combined feature vector:"+fvec.map(_.get.toString).mkString(", "))


  }
  val imgSize = 5
  for((cfunc,caxes) <-
      Array((new ProgXImage,(a: D3int) => a.x),
        (new ProgYImage,(a: D3int) => a.y),
        (new ProgZImage,(a: D3int) => a.z)
      )) {
    test("Array to KVPoints "+cfunc.name()) {
      val testImg = TestPosFunctions.wrapItAs(imgSize, cfunc, TImgTools.IMAGETYPE_INT)
      val cSlice = TImgSlice.fromTImg[Array[Int]](testImg,3,TImgTools.IMAGETYPE_INT)
      val slicePts = ImageTools2D.SliceToKVList(cSlice)
      slicePts.map(_._2.toInt).toArray should equal (slicePts.map(i => caxes(i._1)).toArray)
      if (TIPLGlobal.getDebug) {
        println(slicePts.map(_._2.toInt).zip(slicePts.map(i => caxes(i._1))).map(j
        => j._1+", "+j._2)
          .mkString("\n"))
      }

    }
  }

  test("Adding Slices") {
    val testImg = TestPosFunctions.wrapItAs(imgSize, new ProgZImage, TImgTools.IMAGETYPE_INT)
    val sliceA = TImgSlice.fromTImg[Array[Int]](testImg,3,TImgTools.IMAGETYPE_INT)
    val sliceB = TImgSlice.fromTImg[Array[Int]](testImg,5,TImgTools.IMAGETYPE_INT)
    val addSlice = sliceA+sliceB
    assert(TImgSlice.slicesSizeMatch(addSlice,sliceA),"Size is correct")
    addSlice.get() should be (Array.fill(imgSize*imgSize)(8.0))
  }

  test("Concatenating Slices") {
    val testImg = TestPosFunctions.wrapItAs(imgSize, new ProgZImage, TImgTools.IMAGETYPE_INT)
    val sliceA = TImgSlice.fromTImg[Array[Int]](testImg,3,TImgTools.IMAGETYPE_INT)
    val sliceB = TImgSlice.fromTImg[Array[Int]](testImg,5,TImgTools.IMAGETYPE_INT)
    val addSlice = sliceA++sliceB
    assert(TImgSlice.slicesSizeMatch(addSlice,sliceA),"Size is correct")
    addSlice.get() should be (Array.fill(imgSize*imgSize)((3,5)))
  }

}
