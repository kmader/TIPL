package tipl.util

import java.io.File

import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import tipl.formats.{FImage, TImg, TImgRO}
import tipl.settings.FilterSettings
import tipl.spark.DTImgOps._
import tipl.spark.{DTImg, KVImg, SKVoronoi, ShapeAnalysis}
import tipl.tools.{BaseTIPLPluginIn, GrayAnalysis, HildThickness}

import scala.math.{pow, sqrt}
import scala.reflect.ClassTag

/**
 * An extension of TImgRO to make the available filters show up
 */

object TIPLOps {


  trait NeighborhoodOperation[T, U] {
    def blockOperation(windSize: D3int, kernel: Option[BaseTIPLPluginIn.morphKernel],
                       mapFun: (Iterable[T] => U)): RDD[(D3int, U)]
  }


  implicit def TypedPathFromFileObject(localFile: File): TypedPath = TypedPath.localFile(localFile)


  /**
   * A version of D3float which can perform simple math operations
   */
  implicit class RichD3float(ip: D3float) {
    def -(ip2: D3float) = {
      new D3float(ip.x - ip2.x, ip.y - ip2.y, ip.z - ip2.z)
    }

    def +(ip2: D3float) = {
      new D3float(ip.x + ip2.x, ip.y + ip2.y, ip.z + ip2.z)
    }

    def *(iv: Double) = {
      new D3float(ip.x * iv, ip.y * iv, ip.z * iv)
    }

    def *(iv: D3float) = {
      new D3float(ip.x * iv.x, ip.y * iv.y, ip.z * iv.z)
    }

    def /(iv: Double) = {
      new D3float(ip.x / iv, ip.y / iv, ip.z / iv)
    }

    def /(iv: D3float) = {
      new D3float(ip.x / iv.x, ip.y / iv.y, ip.z / iv.z)
    }

    def mag() = {
      sqrt(pow(ip.x, 2) + pow(ip.y, 2) + pow(ip.z, 2))
    }

    def <(iv: Double) = mag() < iv

    def >(iv: Double) = mag() > iv

    def ==(iv: Double) = mag() == iv

  }


  /**
   * A version of D3int which can perform simple arithmetic
   */
  implicit class RichD3int(ip: D3int) {
    def -(ip2: D3int) = {
      new D3int(ip.x - ip2.x, ip.y - ip2.y, ip.z - ip2.z)
    }

    def +(ip2: D3int) = {
      new D3int(ip.x + ip2.x, ip.y + ip2.y, ip.z + ip2.z)
    }

    def *(iv: Int) = {
      new D3int(ip.x * iv, ip.y * iv, ip.z * iv)
    }

    def *(iv: Float) = {
      new D3float(ip.x * iv, ip.y * iv, ip.z * iv)
    }
  }


  implicit class RichTImgList[T <: TImgRO](val inputImageList: Array[T]) {
    def pluginIO(name: String): ITIPLPluginIO = {
      TIPLPluginManager.createBestPluginIO[T](name, inputImageList)
    }

    def plugin(name: String): ITIPLPlugin = {
      TIPLPluginManager.createBestPlugin[T](name, inputImageList)
    }

    def run(name: String, parameters: String): Array[TImg] = {
      val plug = pluginIO(name)
      plug.LoadImages(inputImageList.asInstanceOf[Array[TImgRO]])
      plug.setParameter(parameters)
      plug.execute()
      plug.ExportImages(inputImageList(0))
    }
  }


  /**
   * A TImg class supporting both filters and IO
   */
  implicit class RichTImg(val inputImage: TImgRO) extends TImgRO.TImgOld {
    /**
     * The old reading functions
     */
    lazy val fullTImg = new TImgRO.TImgFull(inputImage)

    override def getBoolArray(sliceNumber: Int) = {
      fullTImg.getBoolArray(sliceNumber)
    }

    override def getByteArray(sliceNumber: Int) = {
      fullTImg.getByteArray(sliceNumber)
    }

    override def getShortArray(sliceNumber: Int) = {
      fullTImg.getShortArray(sliceNumber)
    }

    override def getIntArray(sliceNumber: Int) = {
      fullTImg.getIntArray(sliceNumber)
    }

    override def getFloatArray(sliceNumber: Int) = {
      fullTImg.getFloatArray(sliceNumber)
    }

    /**
     * Basic IO
     *
     */
    def write(path: String) {
      write(new TypedPath(path))
    }

    /**
     * Basic IO
     *
     */
    def write(path: TypedPath) {
      TImgTools.WriteTImg(inputImage, path)
    }

    /**
     * The kVoronoi operation
     */
    def kvoronoi(mask: TImgRO): Array[TImg] = {
      val plugObj = new SKVoronoi
      plugObj.LoadImages(Array(inputImage, mask))
      plugObj.execute
      plugObj.ExportImages(mask)
    }

    def shapeAnalysis(outfile: String): Unit = {
      val plugObj = new ShapeAnalysis
      plugObj.LoadImages(Array(inputImage))
      plugObj.setParameter("-csvname=" + outfile)
      plugObj.execute
    }

    def filter(downfactor: Int, upfactor: Int, filterType: Int): TImgRO = filter(new D3int
    (downfactor), new D3int(upfactor), filterType)

    def filter(downfactor: D3int = new D3int(1, 1, 1), upfactor: D3int = new D3int(1, 1, 1),
               filterType: Int = FilterSettings.GAUSSIAN): TImgRO = {
      //, shape: morphKernel = fullKernel, filter: filterGenerator = null): TImgRO = {
      FilterSettings.checkFilterType(filterType, true)
      val plugObj = TIPLPluginManager.createBestPluginIO("Filter", Array(inputImage))
      plugObj.LoadImages(Array(inputImage))
      plugObj.setParameter("-upfactor=" + upfactor + " -downfactor=" + downfactor + " -filter=" +
        filterType)
      plugObj.execute
      plugObj.ExportImages(inputImage)(0)
    }

    def componentLabel(kernel: Int = 0, neighborhood: D3int = new D3int(1, 1, 1)) = {
      val plugObj = TIPLPluginManager.createBestPluginIO("ComponentLabel", Array(inputImage))
      plugObj.LoadImages(Array(inputImage))
      plugObj.setParameter("-kernel=" + kernel + " -neighborhood" + neighborhood)
      plugObj.execute
      plugObj.ExportImages(inputImage)(0)
    }

    //TODO modernize this function
    def shapeAnalysis(outPath: TypedPath, analysisName: String = "Shape",
                      includeShapeTensor: Boolean = true) =
      GrayAnalysis.StartLacunaAnalysis(inputImage, outPath, analysisName, includeShapeTensor)

    //TODO modernize this function as well
    def thickness(csvFile: TypedPath): Array[TImgRO] = {
      HildThickness.DTO(inputImage, csvFile)
    }

    //TODO Implement advanced filter  plugObj.neighborKernel=shape
    //TODO plugObj.asInstanceOf[].scalingFilterGenerator = filter

    /**
     * simple apply a function to each point in the image
     */
    def apply[B](mapFun: (Double => B))(implicit bm: ClassTag[B]) = {
      val outputType = TImgTools.identifySliceType(new Array[B](1))
      inputImage match {
        case a: DTImg[_] => a.getBaseImg().rdd.apply[B](mapFun).wrap(a.getElSize)
        case a: KVImg[_] => new KVImg(a, outputType, a.toKVDouble.getBaseImg().mapValues(mapFun))
        case a: TImgRO => new FImage(a, outputType, mapFun.asVF, true)
      }
    }

  }

  import scala.{specialized => spec}

  implicit class RichFunction[@spec(Boolean, Byte, Short, Int, Long, Float,
    Double) B](val mapFun: (Double => B))(implicit bm: ClassTag[B]) {

    val outputType = TImgTools.identifySliceType(new Array[B](1))
    lazy val vf = new FImage.VoxelFunction() {
      override def get(ipos: Array[java.lang.Double], voxval: Double): Double = {
        mapFun(voxval) match {
          case a: Boolean => if (a) 127.0 else 0.0
          case a: Byte => a.toDouble
          case a: Char => a.toDouble
          case a: Short => a.toDouble
          case a: Int => a.toDouble
          case a: Long => a.toDouble
          case a: Float => a.toDouble
          case a: Double => a
          case a: B => a.toString.toDouble
        }
      }

      override def getRange() = TImgTools.identifyTypeRange(outputType)

      override def name() = "ScalaFunction:" + mapFun

      override def toString() = name()
    }

    def asVF() = vf
  }


}

