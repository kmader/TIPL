package tipl.util

import java.io.File

import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import tipl.formats.{FImage, TImg, TImgRO}
import tipl.settings.FilterSettings
import tipl.spark.DTImgOps._
import tipl.spark.{DTImg, KVImg}
import tipl.tools.{BaseTIPLPluginIn, GrayAnalysis, HildThickness}

import scala.math.{pow, sqrt}
import scala.reflect.ClassTag

/**
 * An extension of TImgRO to make the available filters show up
 */

object TIPLOps {


  /**
   * Allow the nice get (option) and getorelse to be used on array objects
   *
   * @param inArr
   * @tparam T
   */
  implicit class optionArray[T](inArr: Array[T]) {
    def get(ind: Int): Option[T] = {
      if (ind >= 0 & ind < inArr.length) Some(inArr(ind))
      else None
    }

    def getOrElse(ind: Int, elseVal: T) = {
      if (ind >= 0 & ind < inArr.length) inArr(ind)
      else elseVal
    }
  }

  trait NeighborhoodOperation[T, U] {
    def blockOperation(windSize: D3int, kernel: Option[BaseTIPLPluginIn.morphKernel],
                       mapFun: (Iterable[T] => U)): RDD[(D3int, U)]
  }


  implicit def TypedPathFromFileObject(localFile: File): TypedPath = new LocalTypedPath(localFile)


  /**
   * A version of D3float which can perform simple math operations
   */
  implicit class RichD3float(ip: D3float) extends Serializable {
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

    def <(iv: Double) = mag() < iv

    def mag() = {
      sqrt(pow(ip.x, 2) + pow(ip.y, 2) + pow(ip.z, 2))
    }

    def >(iv: Double) = mag() > iv

    def ==(iv: Double) = mag() == iv

  }


  /**
   * A version of D3int which can perform simple arithmetic
   */
  implicit class RichD3int(ip: D3int) extends Serializable {
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


  /**
   * valid for a list of TImgRO objects
   *
   * @param inputImageList list of images
   * @tparam T any image type
   * @return an object with many more operators than TImgRO or array
   */
  implicit def TImgListToRichTImgList[T <: TImgRO](inputImageList: Array[T]) = new
      RichTImgList[T](inputImageList)

  /**
   * valid for a single TImgRO objects
   *
   * @param inputImage single object (to be converted to a one element list)
   * @tparam T any image type
   * @return an object with many more operators than TImgRO or array
   */
  implicit def TImgToRichTImgList[T <: TImgRO](inputImage: T) =
    new RichTImgList[TImgRO](Array[TImgRO](inputImage))


  class RichTImgList[T <: TImgRO](val inputImageList: Array[T]) extends Serializable {

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


    def pluginIO(name: String): ITIPLPluginIO = {
      TIPLPluginManager.createBestPluginIO[T](name, inputImageList)
    }


    def show(index: Int = 0) = {
      val ip = tipl.ij.TImgToImagePlus.MakeImagePlus(inputImageList(index))
      ip.show(inputImageList(index).getPath().getPath())
    }


    /**
     * Render a 3d image and save it to disk
     *
     * @param output the path to save the image to
     * @param index
     */
    def render3D(output: TypedPath, index: Int = 0, extargs: String = ""): Unit = {
      val mode = 4;
      // volume
      val argStr = "-batch -snapshot -rendermode=" + mode + (
        if (extargs.length > 0) {
          " " + extargs
        } else {
          ""
        }
        )

      val p = TIPLGlobal.activeParser(argStr.split(" "))
      p.getOptionPath("output", output, "")
      show3D(index, Some(p))
    }


    /**
     * Show a 3d rendering view of the object
     *
     * @param index
     * @param p
     * @return
     */
    def show3D(index: Int = 0, p: Option[ArgumentParser] = None) = {
      val vvPlug = pluginIn("VolumeViewer")
      vvPlug.LoadImages(Array[TImgRO](inputImageList(index)))
      p.foreach(apval => vvPlug.setParameter(apval, ""))
      vvPlug.execute("waitForClose")
    }


    def pluginIn(name: String): ITIPLPluginIn = {
      TIPLPluginManager.createBestPluginIn[T](name, inputImageList)
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
      write(TIPLStorageManager.openPath(path))
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
      val imgList = Array(inputImage, mask)
      val plugObj = TIPLPluginManager.createBestPluginIO("KVoronoi", imgList)
      plugObj.LoadImages(imgList)
      plugObj.execute
      plugObj.ExportImages(mask)
    }

    def shapeAnalysis(outfile: String): Unit = {
      val plugObj = TIPLPluginManager.createBestPluginIO("ShapeAnalysis", Array(inputImage))
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
      plugObj.setParameter("-kernel=" + kernel + " -neighborhood=" + neighborhood)
      plugObj.execute
      plugObj.ExportImages(inputImage)(0)
    }

    //TODO modernize this function
    def shapeAnalysis(outPath: TypedPath, analysisName: String = "Shape",
                      includeShapeTensor: Boolean = true) =
      GrayAnalysis.StartLacunaAnalysis(inputImage, outPath, analysisName, includeShapeTensor)

    //TODO modernize this function as well
    /**
     *
     * @param csvFile
     * @return thicknessmap, distance map, ridge map
     */
    def thickness(csvFile: TypedPath): Array[TImgRO] = {
      HildThickness.DTO(inputImage, csvFile)
    }

    //TODO Implement advanced filter  plugObj.neighborKernel=shape
    //TODO plugObj.asInstanceOf[].scalingFilterGenerator = filter

    def apply[B](mapFun: (Double => B))(implicit bm: ClassTag[B], nm: Numeric[B]): TImgRO =
      apply(mapFun, nm.zero)

    /**
     * simple apply a function to each point in the image
     */
    def apply[B](mapFun: (Double => B), paddingVal: B)(implicit bm: ClassTag[B]): TImgRO = {
      val outputType = TImgTools.identifySliceType(new Array[B](1))
      inputImage match {
        case a: DTImg[_] =>
          a.getBaseImg().rdd.apply[B](mapFun).wrap(a.getElSize)
        case a: KVImg[_] =>
          new KVImg(a, outputType, a.toKVDouble.getBaseImg().mapValues(mapFun), paddingVal)
        case a: TImgRO =>
          new FImage(a, outputType, mapFun.asVF, true)
      }
    }

  }

  import scala.{specialized => spec}

  implicit class RichFunction[@spec(Boolean, Byte, Short, Int, Long, Float,
    Double) B](val mapFun: (Double => B))(implicit bm: ClassTag[B]) extends Serializable {

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
    val outputType = TImgTools.identifySliceType(new Array[B](1))

    def asVF() = vf
  }


  /**
   * allow an RDD to be treated as factor (as in R) where distinct values are used
   *
   * @param baseRDD
   * @tparam T the type of the factors (strings, ints, tuples make sense, double probably does not)
   */
  implicit class factorRDD[T](baseRDD: RDD[T]) {
    /**
     * Return a histogram (map) for the rdd
     *
     * @param approxTime allows an approximation time to be given in seconds without changing the
     *                   result type
     * @return map of keys T and values
     */
    def factorHistogram(approxTime: Option[Long] = None) = {
      approxTime match {
        case Some(timeout) =>
          baseRDD.countByValueApprox(timeout, 0.95).
            map(_.mapValues(_.mean.toLong)).getFinalValue()
        case None =>
          baseRDD.countByValue()
      }

    }
  }


}

