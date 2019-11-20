package org.apache.spark.ui.tipl

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import org.apache.spark.{SparkConf, SecurityManager}
import org.apache.spark.ui.{JettyUtils, UIUtils}
import org.eclipse.jetty.servlet.ServletContextHandler

import scala.xml.Node
/**
 * Created by mader on 11/22/14.
 */
object HTMLUtils {
  case class CodeWithHeader(code: Seq[Node], header: Headers = emptyHeader) {
    def +(n: CodeWithHeader) = CodeWithHeader(code ++ n.code,header + n.header)
  }
  /**
   * A class to store header components separately
   * @param css css and components that belong in HEAD
   *            going first
   * @param js the javascript files which are but at the end of the body to make loading pages
   *           faster
   */
  case class Headers(css: Seq[Node], js: Seq[Node]) {
    def +(n: Headers) = Headers(css ++ n.css,js ++ n.js)
  }

  def generateTempSecMan(sconf: SparkConf) = new SecurityManager(sconf)


  /**
   * Create a servlet for handling binary files
   * @param path
   * @param contentType
   * @param responder
   * @param securityMgr
   * @param basePath
   * @return
   */
  def createBinaryServletHandler(path: String,
                                 contentType: String, responder: (HttpServletRequest =>
    Array[Byte]), securityMgr: SecurityManager, basePath: String = ""): ServletContextHandler = {
    val core = new HttpServlet {
      override def doGet(request: HttpServletRequest, response: HttpServletResponse) {
        if (securityMgr.checkUIViewPermissions(request.getRemoteUser)) {
          response.setContentType("%s;charset=utf-8".format(contentType))
          response.setStatus(HttpServletResponse.SC_OK)
          val result = responder(request)
          // binary data can be cached
          response.setHeader("Cache-Control", "public")
          response.getOutputStream().write(result)

        }
        else {
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
          response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate")
          response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
            "User is not authorized to access this page.")
        }
      }
    }
    JettyUtils.createServletHandler(path, core, basePath)
  }

  /**
   * getting null values is really damn annoying
   * common path and argument parsing code so I do not have to rewrite it a million times
   * @param req
   */
  implicit class SmartHSRequest(req: HttpServletRequest)  {

    private lazy val lParsePath = req.getServletPath().split("/").map(_.trim().toLowerCase)
    def getParsedPath(): Seq[String] = lParsePath

    private lazy val lPathMap = lParsePath.zipWithIndex.map(_.swap).toMap
    def getPathMap(): Map[Int,String] = lPathMap
    /**
     * get a nicely trimmed, lowercase version or return a default value
     * @param key
     * @param orElse
     * @return
     */
    def getOrElse(key: String, orElse: String = ""): String = getOption(key) match {
      case Some(s) => s.trim().toLowerCase
      case None => orElse
    }

    /**
     * get the parameter as an option
     * @param key key to read
     * @return the optional return if it is there and has  length greater than 0
     */
    def getOption(key: String): Option[String] = req.getParameter(key) match {
      case s: String if s.length()>0 => Some(s)
      case _ => None
    }

    /**
     * A function to build others on top of
     * @param f the conversion function
     * @tparam T the output type
     * @return option with the type T
     */
    private def getFOption[T](key: String,f: String => T): Option[T] = {
      try {
        Some(f(req.getParameter(key)))
      } catch {
        case _ => None
      }
    }

    /**
     * get the parameter as an integer or nothing
     * @param key parameter to read
     * @return integer or none
     */

    def getIntOption(key: String) = getFOption(key, (n: String)=> n.toInt )
    /**
     * get the parameter as a double or nothing
     * @param key parameter to read
     * @return double or none
     */
    def getDoubleOption(key: String) = getFOption(key, (n: String)=> n.toDouble )


  }

  /**
   * The call-back code for the sliders on the image slices
   * @param subslice
   * @return
   */
  def sliderCode(slicePreviewName: String, subslice: Boolean = false) = {
    val insIndex = if (subslice) 6 else 5
    "var curSlice = $('#" + slicePreviewName + "')" +
      ".attr('src').split('/');\n" +
      "if (!curSlice[5]) curSlice[5]=0;\n" + // add a slice even if it's not needed
      "curSlice[" + insIndex + "]=slideEvent.value;\n" +
      "console.log('loading slice '+curSlice.join('/'));\n" +
      "$('#" + slicePreviewName + "').attr('src',curSlice.join('/'));\n"
  }



  val emptyHeader = new Headers(Seq[Node](),Seq[Node]())
  def JSHeader(js: Seq[Node]) = Headers(Seq[Node](),js)
  val baseHeader =
    Headers(<link href={UIUtils.prependBaseUri("/tstatic/bootstrap/css/bootstrap.min.css")}
                  rel="stylesheet" /><style>
      .chart div {{
      font: 10px sans-serif;
      background-color: steelblue;
      text-align: right;
      padding: 3px;
      margin: 1px;
      color: white;
      }}
      /* line style */
      path {{
      stroke: red;
      stroke-width: 2;
      fill: none; /* stop it being solid area */
      }}
      /* x axis tick */
      .x.axis line {{
      stroke: #000;
      }}
      /* x axis line */
      .x.axis path {{
      fill: none;
      stroke: #000;
      }}
      /* Y tick */
      .y.axis line, .y.axis path {{
      fill: none;
      stroke: #000;
      }}
    </style><link rel="stylesheet" href={UIUtils.prependBaseUri("/tstatic/webui.css")}
                  type="text/css"/>,
      <script src={UIUtils.prependBaseUri("/tstatic/jquery-2.1" +
        ".1.min.js")}></script>
      <script type="text/javascript"
              src={UIUtils.prependBaseUri("/tstatic/bootstrap/js/bootstrap.min.js")}></script> ++
        <script src={UIUtils.prependBaseUri("/tstatic/d3.min.js")}></script> ++
        <script src={UIUtils.prependBaseUri("/tstatic/jquery.unveil.js")}></script> ++
        <script src={UIUtils.prependBaseUri("/tstatic/mermaid.min.js")}></script> ++
          <script src={UIUtils.prependBaseUri("/static/sorttable.js")}></script> ++
          <script src={UIUtils.prependBaseUri("/static/bootstrap-tooltip.js")}></script> ++
          <script src={UIUtils.prependBaseUri("/static/initialize-tooltips.js")}></script>
    )

  private val histHeader =
      Headers(
      <style>
        .axis path,
        .axis line {{
        fill: none;
        stroke: #eee;
        shape-rendering: crispEdges;
        }}
        .axis text {{
        font-size: 11px;
        }}
        .bar {{
        fill: steelblue;
        }}
      </style>,
      <script src={UIUtils.prependBaseUri("/tstatic/histogram.js")}></script>)

  def makeHistBody(histName: String = "graph") =
    <div class="container">
      <div class="row">
        <div id={histName} class="col-md-12">
        </div>
      </div>
    </div>

  def makeHistogram(inJsonDataPath: String,targetObj: String = "graph",width: Int = 900,
                    height: Int = 450) = {
    CodeWithHeader(makeHistBody(targetObj),
      JSHeader(
        <script>d3.json('{inJsonDataPath}', makeHistogramFromJSON('{targetObj}',{width},
        {height}));</script>))
  }

  val sliderHeader = Headers(
      <link href={UIUtils.prependBaseUri("/tstatic/bootstrap/css/bootstrap-slider" +
    ".css")} rel="stylesheet" />,
    <script src={UIUtils.prependBaseUri("/tstatic/bootstrap/js/bootstrap-slider.js")}></script>)

  /**
   * get all the header code needed for TIPL operation (jquery should always come first)
   * @return
   */
  def getHeader: Headers = baseHeader + histHeader + sliderHeader

  /**
   * Creates a slider object using bootstrap-slider
   * @param sliderName the javascript name of the object
   * @param itemCount the maximum number
   * @param slideCode what happens on a slide event (the event data is called slideEvent and the
   *                  current value is stored in slideEvent.value
   * @return
   */
  def sliceSlider(sliderName: String, itemCount: Long, slideCode: Option[String] = None) =
    CodeWithHeader(<div class="well"><h4>Select:<span class="label
                label-primary">{sliderName}</span></h4><input id={sliderName}
                                                  data-slider-id={sliderName+"Slider"}
                                                  type="text" data-slider-min="0"
                                                  data-slider-max={itemCount.toString}
                                                  data-slider-step="1"
                                                  data-slider-value="0"/></div>,
      JSHeader(<script>
        $('{"#"+sliderName}').slider({{
        formatter: function(value) {{
        return 'Value of {sliderName} is ' + value + ' of {itemCount.toString}';
        }}
        }});
        {
        slideCode match {
          case Some(scode) => "$('#"+sliderName+"').on('slideStop'," +
            "function(slideEvent) { "+scode+"});"
          case None => ""
        }
        }
      </script>))


  def thumbnailImgFunc(caption: String, path :String ) = CodeWithHeader(<img
    src="/tstatic/assets/loading.png"
                                                              data-src={path} alt={caption}
                                                              height="256"
                                                                width="256"/>)
  var randHistTag = 0
  def thumbHistFunc(caption: String, path: String) = {
    randHistTag+=1
    makeHistogram(path,
      "hist_"+randHistTag,300,256)
  }
  /**
   *
   * @param imgPaths array of (alt text, path of the imaage)
   * @return
   */
  def makeThumbnailPage(imgPaths: Array[(String,String)],tmbFunc: ((String,
    String) => CodeWithHeader) = thumbnailImgFunc) = {
      val newCode = { for(cImg <- imgPaths; curThumb = tmbFunc(cImg._1,cImg._2)  )
      yield
        CodeWithHeader(<div class="col-xs-6 col-md-3">
          <div class="thumbnail">
            {curThumb.code}
            <div class="caption">
              <h3>{cImg._1}</h3>
            </div>
          </div>
        </div>,curThumb.header)
        }.reduce(_ + _)

    CodeWithHeader(<div class="row">{newCode.code}</div>,newCode.header)
  }





}
