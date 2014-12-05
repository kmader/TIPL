package tipl.spark

import org.scalatest.FunSuite
import tipl.formats.TImgRO
import tipl.tests.{LocalSparkContext, TestPosFunctions, TIPLTestingLibrary}
import tipl.util.TImgTools

/**
 * Created by mader on 10/13/14.
 */
class ImageTypesTests extends FunSuite with LocalSparkContext {
  val lineImage: TImgRO = TestPosFunctions.wrapItAs(10, new TestPosFunctions.LinesFunction,
    TImgTools.IMAGETYPE_BOOL)

  def checkImage(imgA: TImgRO, imgB: TImgRO): Unit = {
    assert(imgA.getImageType() == imgB.getImageType())
    assert(imgA.getDim().z == imgB.getDim().z)
    TIPLTestingLibrary.doSlicesMatchB(imgA, 5, imgB, 5)
  }

  test("Creation of DSImg") {
    sc = getSpark("Create DSImg")
    val testDSImg = new DSImg[Boolean](sc, lineImage, TImgTools.IMAGETYPE_BOOL)
    checkImage(lineImage, testDSImg)
  }
  test("Creation of KVImg") {
    sc = getSpark("Create KVImg")
    val testKVImg = new KVImg[Boolean](sc, lineImage, TImgTools.IMAGETYPE_BOOL)
    checkImage(lineImage, testKVImg)
  }
}


object ImageTypesStatic {

}
