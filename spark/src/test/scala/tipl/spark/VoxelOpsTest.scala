package tipl.spark

import org.scalatest.FunSuite
import spark.images.ImageTools
import tipl.util.D3int
import tipl.util.TIPLOps._

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
    val valList = ptList.map(inpt => (inpt,inpt))
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
        val outArr = Array.fill[D3int](feat_vec_len)(new D3int(-100,-100,-100))
        val iter = kv._2.toIterator
        while (iter.hasNext) {
          val outVal = iter.next
          outArr(outVal._2._2) = outVal._2._1
        }
        (kv._1,
          outArr
          )
      }
    )
    println(feat_vec.map(kv => kv._1.toString+" : "+
      (kv._2.map(iv => if(iv.prod()>0) (iv-kv._1).toString else "_").mkString("\t"))).
      mkString("\n"))

  }

}
