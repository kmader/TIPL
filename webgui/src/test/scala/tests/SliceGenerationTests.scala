package tests

import org.apache.spark.ui.tipl.TPages
import org.scalatest.FunSuite
import tipl.formats.TImgRO
import tipl.spark.DSImg
import tipl.tests.{LocalSparkContext, TIPLTestingLibrary, TestPosFunctions}
import tipl.util.TImgTools

/**
 * Created by mader on 12/5/14.
 */

class SliceGenerationTests extends FunSuite with LocalSparkContext {
  val startSlices = 10
  val lineImage: TImgRO = TestPosFunctions.wrapItAs(startSlices, new TestPosFunctions.LinesFunction,
    TImgTools.IMAGETYPE_FLOAT)

  val boolImage = new DSImg[Boolean](sc, lineImage, TImgTools.IMAGETYPE_BOOL)
  val floatImage = new DSImg[Float](sc, lineImage, TImgTools.IMAGETYPE_FLOAT)
  val dblImage = new DSImg[Double](sc, lineImage, TImgTools.IMAGETYPE_DOUBLE)
  val intImage = new DSImg[Int](sc, lineImage, TImgTools.IMAGETYPE_INT)

  def checkImage(imgA: TImgRO, imgB: TImgRO): Unit = {
    assert(imgA.getImageType() == imgB.getImageType())
    assert(imgA.getDim().z == imgB.getDim().z)
    TIPLTestingLibrary.doSlicesMatchB(imgA, 5, imgB, 5)
  }

  test("DSImg to Byte") {

    val byteImage = TPages.RDDSlicePage.SliceRDDToByte(floatImage.getBaseImg(),"png",1)
    val nSlices = byteImage.count.toInt
    assert(nSlices == startSlices,"Output image has :"+nSlices+" instead of "+startSlices )

  }

  for (cImg <- Seq(boolImage,intImage,floatImage,dblImage);
       tlevel<- Range(2,7,2)
       ) {
    val typeName = TImgTools.getImageTypeName(cImg.getImageType)
    test("DSImg["+typeName+"] to Tiles:"+tlevel) {
      val byteImage = TPages.RDDSlicePage.SliceRDDToByte(cImg.getBaseImg(),"png",tlevel)
      val nSlices = byteImage.count.toInt
      assert(nSlices ==  (startSlices * tlevel * tlevel),
        "Tiled image has :"+nSlices+" instead " +
          "of "+(startSlices * tlevel * tlevel) )
    }
  }



}
