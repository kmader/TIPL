package tipl.spark

import tipl.formats.TImgRO
import tipl.formats.TImg
import tipl.tools.BaseTIPLPluginIn
import tipl.tools.BaseTIPLPluginIn._
import tipl.tools.VFilterScale
import tipl.util.D3int
import tipl.tools.FilterScale.filterGenerator
/**
 * An extension of TImgRO to make the available filters show up
*/
object ETImg {
  /**
   * The extended implicit class
   */
  implicit class RichTImg(val inputImage: TImgRO) {
    /**
     * The kVoronoi operation
     */
    def kvoronoi(mask: TImgRO): Array[TImg] = {
      val plugObj = new SKVoronoi
      plugObj.LoadImages(Array(inputImage,mask))
      plugObj.execute
      plugObj.ExportImages(mask)
    }
    def shapeAnalysis(outfile: String): Unit = {
      val plugObj = new ShapeAnalysis
      plugObj.LoadImages(Array(inputImage))
      plugObj.setParameter("-csvname="+outfile)
      plugObj.execute
    }
    def filter(size: D3int = new D3int(1,1,1), shape: morphKernel = fullKernel, filter: filterGenerator = null): TImgRO = {
      val plugObj = new VFilterScale
      plugObj.LoadImages(Array(inputImage))
      plugObj.neighborSize=size
      plugObj.neighborKernel=shape
      plugObj.scalingFilterGenerator = filter
      plugObj.execute
      plugObj.ExportImages(inputImage)(0)
    }
  }
}

