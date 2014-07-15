package tipl.util

import tipl.formats.TImgRO
import tipl.formats.TImg
import tipl.tools.BaseTIPLPluginIn._
import tipl.tools.VFilterScale
import tipl.util.D3int
import tipl.util.D3float
import tipl.tools.FilterScale.filterGenerator
import tipl.spark.SKVoronoi
import tipl.spark.ShapeAnalysis
/**
 * An extension of TImgRO to make the available filters show up
*/
object TIPLOps {
    /**
   * A subtractable version of D3float
   */
  @serializable implicit class RichD3float(ip: D3float) {
	 def -(ip2: D3float) = {
	   new D3float(ip.x-ip2.x,ip.y-ip2.y,ip.z-ip2.z)
	 }
	 def +(ip2: D3float) = {
	   new D3float(ip.x+ip2.x,ip.y+ip2.y,ip.z+ip2.z)
	 }
	 def *(iv: Double) = {
	   new D3float(ip.x*iv,ip.y*iv,ip.z*iv)
	 }
  }
      /**
   * A subtractable version of D3int
   */
  @serializable implicit class RichD3int(ip: D3int) {
	 def -(ip2: D3int) = {
	   new D3int(ip.x-ip2.x,ip.y-ip2.y,ip.z-ip2.z)
	 }
	 def +(ip2: D3int) = {
	   new D3int(ip.x+ip2.x,ip.y+ip2.y,ip.z+ip2.z)
	 }
	 def *(iv: Int) = {
	   new D3int(ip.x*iv,ip.y*iv,ip.z*iv)
	 }
	 def *(iv: Float) = {
	   new D3float(ip.x*iv,ip.y*iv,ip.z*iv)
	 }
  }
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

