package org.apache.spark.ui.tipl

import javax.servlet.http.HttpServletRequest

import org.apache.commons.io.IOUtils
import org.apache.spark.ui.tipl.HTMLUtils.CodeWithHeader
import org.apache.spark.util.Utils
import org.json4s._
import tipl.formats.TImgRO

import scala.reflect.ClassTag

/**
 * The separated components for visualizing various types of datasets inside of TPages (TImgRO,
 * ImagePlus, Arrays, Point lists etc)
 * Created by mader on 1/18/15.
 */
object WebViz {
  val runStats = true
  val timeout = 5

  val RAWPREFIX = "raw"
  val JSONPREFIX = "jaon"
  val DETAILPREFIX = "detail"
  val SAMPLEPREFIX = "sample"


  /**
   * Store the extra-information in an easily accessible manner
   */
  case class ExtInfo(infoMap: Map[String,_] = Map.empty[String,Any]) {
    def this(place: String, inval: Any) = this(Map(place -> inval))
    def get(key: String) = infoMap.get(key)

    def getOrElse(key: String, orElse: Any) = get(key) match {
      case Some(outvalue) => outvalue
      case None => orElse
    }
    /**
     * getOrElse with strong-typing
     * @param key the value to get (iff it is of type T)
     * @param orElse the value to return if it is not present or not of the correct type
     * @tparam T
     * @return the value or orElse of type T
     */
    def getOrElseT[T](key: String, orElse: T) = {
      get(key) match {
        case Some(mvalue: T) => mvalue
        case _ => orElse
      }
    }
    def key() = infoMap.get("key")
    def value() = infoMap.get("value")

    override def toString(): String = {
      infoMap.mkString("(",", ",")")
    }
  }

  trait RDDInfo {
    def getCount(): Int
    def getElements(): Array[_]
  }

  /**
   * A trait which encapsulates the important aspect of creating an RDD visualization within
   * pages listing many different types of RDDs. The current organization is as such
   * - singleRender is for a line within a table summarizing that RDD, but listing many others
   * - detailRender is for a dedicated page/panel on just this specific RDD
   * - rawRender is for producing output as a byte array (images, movies, etc..)
   * - jsonRender is for producing json data (histograms, plots, panels, etc)
   * Typing is done in a static manner with the convertEle function being overloadable to support
   * more flexible conversions, per default val: T and (_,val: T) are supported
   *
   * As a matter of style other information known about the RDD / current setup should be passed
   * into the VizTool class using the constructor (if possible)
   */
  trait VizTool extends Serializable {
    /**
     * @param ele untyped element
     * @return Is it possible for the given element to be rendered using this tool
     */
    type elementType

    implicit val et: ClassTag[elementType]

    /**
     * Support the conversion of elements which have elementType in their key (less common)
     * @return
     */
    def supportKeys(): Boolean

    /**
     * Support the conversion of elements which have elementType in their value (more common)
     * @return
     */
    def supportValues(): Boolean = true

    /**
     * Support the conversion of an element-type itself
     * @return
     */
    def supportRaw(): Boolean = true

    /**
     * Can handle means the class can handle the type (ie using _.toString to convert to a string)
     * @param ele the element
     * @return if it can be accepted by the tool
     */
    def canHandle(ele: Any): Boolean = supports(ele)

    /**
     * Supports means the class is made for handling this type
     * @param ele the element
     * @return is it accepted
     */
    def supports(ele: Any): Boolean = convertEle(ele)._1.isDefined

    def reallySupports(ele: Any): Boolean = {
      (try {
        val darray = new Array[elementType](1)
        darray(0) = ele.asInstanceOf[elementType]
        Some(darray(0))
      } catch {
        case _ => None
      }).isDefined
    }

    /**
     * convert the element to the proper static type (supports values and keys by default)
     * @param ele
     * @return
     */
    def convertEle(ele: Any): (Option[elementType],ExtInfo) = {
      ele match {
        case e: elementType if supportRaw() => (Some(e),ExtInfo())
        case (key: Any,e: elementType) if supportValues() => (Some(e),new ExtInfo("key",key))
        case (e: elementType,value: Any) if supportKeys() => (Some(e),new ExtInfo("value",value))
        case f: Any => (None,new ExtInfo("other",f))
      }
    }

    def detailRender(ele: Any, ri: RDDInfo, request: HttpServletRequest): CodeWithHeader = {
      convertEle(ele) match {
        case (Some(e),info) => typedDetailRender(e,info,ri,request)
        case (None,_) => CodeWithHeader(
          TiplUI.makeErrorMessage("Element could not be converted","The element: "+ele.toString()+
            " could not be converted by class "+this.toString)
        )
      }
    }

    def sampleRender(ele: Any, ri: RDDInfo, request: HttpServletRequest): CodeWithHeader = {
      convertEle(ele) match {
        case (Some(e),info) => typedSampleRender(e,info,ri,request)
        case (None,_) => CodeWithHeader(
          TiplUI.makeErrorMessage("Element could not be converted","The element: "+ele.toString()+
            " could not be converted by class "+this.toString)
        )
      }
    }

    def singleRender(ele: Any, ri: RDDInfo, request: HttpServletRequest): CodeWithHeader = {
      convertEle(ele) match {
        case (Some(e),info) => typedSingleRender(e,info,ri,request)
        case (None,_) => CodeWithHeader(
          TiplUI.makeErrorMessage("Element could not be converted","The element: "+ele.toString()+
            " could not be converted by class "+this.toString)
        )
      }
    }

    def jsonRender(ele: Any,ri: RDDInfo, request: HttpServletRequest): JValue = {
      convertEle(ele) match {
        case (Some(e),info) => typedJsonRender(e,info,ri,request)
        case (None,_) => JSONTools.blankHist
      }
    }


    def rawRender(ele: Any,ri: RDDInfo, request: HttpServletRequest) = {
      convertEle(ele) match {
        case (Some(e),info) => typedRawRender(e,info,ri,request)
        case (None,_) =>
          System.err.println("Element "+ele+" could not be generated by "+this)
          TiplUI.getMissingImageData()
      }
    }
    /**
     * Render an element as a byte array (useful for images, but can be ignored for many other
     * types, for this reason the default implementation exists
     * @param ele
     * @param request
     * @return
     */
    def typedRawRender(ele: elementType, info: ExtInfo,ri: RDDInfo,
                       request: HttpServletRequest): Array[Byte] = {
      System.err.println("Object "+ele+" could not be generated by "+this)
      TiplUI.getMissingImageData()
    }

    def typedJsonRender(ele: elementType,info: ExtInfo, ri: RDDInfo,
                        request: HttpServletRequest): JValue
    def typedDetailRender(ele: elementType,info: ExtInfo, ri: RDDInfo,
                          request: HttpServletRequest): CodeWithHeader
    def typedSampleRender(ele: elementType,info: ExtInfo, ri: RDDInfo,
                          request: HttpServletRequest): CodeWithHeader
    def typedSingleRender(ele: elementType,info: ExtInfo, ri: RDDInfo,
                          request: HttpServletRequest): CodeWithHeader
  }
  //TODO make a better servletrequest type that handles the path mangling automatically
  //TODO
  trait WebVizRequest {
    def getPath(): String
    def makePathDetail(args: String): String

    /**
     * @note thumbnail was renaimed to sample, a page which shows a sampling of the contents
     *       inside the RDD
     */
    def makePathSample(args: String): String
    def makePathRaw(args: String): String
    def makePathJSON(args: String): String
  }

  class HTTPWebVizRequest(req: HttpServletRequest, trimLevel: Int) extends WebVizRequest {
    override def getPath(): String = req.getServletPath

    def getBasePath() = req.getServletPath.split("/")
    def appendArgs(args: String) = {
      val qs = req.getQueryString.trim
      if (qs.length>0) qs+"&"+args
      else {
        if(args.trim.length>0) "?"+args.trim
        else ""
      }
    }

    override def makePathJSON(args: String): String =
      getBasePath+"/"+JSONPREFIX+appendArgs(args)

    override def makePathRaw(args: String): String =
      getBasePath+"/"+RAWPREFIX+appendArgs(args)

    override def makePathDetail(args: String): String =
      getBasePath+"/"+DETAILPREFIX+appendArgs(args)

    override def makePathSample(args: String): String =
      getBasePath+"/"+SAMPLEPREFIX+appendArgs(args)
  }



  implicit class RDDHttpServletRequest(req: HttpServletRequest) {
    import org.apache.spark.ui.tipl.HTMLUtils._
    def getRddId(): Option[String] = {
      req.getOption("id") match {
        case Some(rddName: String) if rddName.length() > 0 => Some(rddName)
        case _ => None
      }
    }
  }


  abstract class TImgROViz extends VizTool {
    override type elementType = TImgRO

    /**
     * Support the conversion of elements which have elementType in their key (less common)
     * @return
     */
    override def supportKeys(): Boolean = false

    /** requires more complicated interactions
      lazy val cHist = HTMLUtils.makeHistogram(request.getRequestURL() + "json/" + rddName)
      summaryInfo + HTMLUtils.sliceSlider("slice", sliceCount,
        Some(parentTab.sliderCode(false))) + HTMLUtils.sliceSlider("subslice",
        firstImage.getDim().z, Some(parentTab.sliderCode(true))) + cHist
      **/
  }


  lazy val backupImage = {
    val outFile = Utils.getSparkClassLoader.
      getResource(TiplUI.STATIC_RESOURCE_DIR+"/spark_logo.png")
    val inStream = outFile.openStream()
    IOUtils.toByteArray(inStream)
  }


}
