package org.apache.spark.ui.tipl


import _root_.tipl.formats.TImgRO
import _root_.tipl.spark.{DSImg, SparkGlobal}
import _root_.tipl.tests.TestPosFunctions
import _root_.tipl.tests.TestPosFunctions.{BoxDistances, DiagonalPlaneAndDotsFunction}
import _root_.tipl.util.{D3int, TImgSlice, TImgTools}
import org.apache.commons.io.IOUtils
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.ui._
import org.apache.spark.ui.tipl.HTMLUtils.CodeWithHeader
import org.apache.spark.util.{Utils, AkkaUtils}

import scala.collection.mutable.{Map => MuMap}
import scala.xml.Node


object TiplUI {
  val STATIC_RESOURCE_DIR = "tipl/static"

  lazy val tiplDefaultImage = {
    val outFile = Utils.getSparkClassLoader.
      getResource(TiplUI.STATIC_RESOURCE_DIR+"/spark_logo.png")
    val inStream = outFile.openStream()
    IOUtils.toByteArray(inStream)
  }

  def getMissingImageData() = tiplDefaultImage
  def getDefaultImageData() = tiplDefaultImage
  def getLoadingImageData() = tiplDefaultImage

  def makeErrorMessage(error: String, errorMessage: String) = {
    <div class="panel panel-warning">
      <div class="panel-heading">
        <h3 class="panel-title">Error:
          {error}
        </h3>
      </div>
      <div class="panel-body">The request cannot be fulfilled because
        {errorMessage}
      </div>
    </div>
  }
  def makeErrorPage(error: String, errorMessage: String, content: CodeWithHeader,
                    parentTab: TTabs.TIPLTab): Seq[Node] = {
    val warning = CodeWithHeader(makeErrorMessage(error,errorMessage))
    makeHeaderPage(error, warning + content, parentTab)
  }

  /**
   * a function to wrap around the header spark page to include the libraries needed for TIPLs gui
   * @param title
   * @param content
   * @param parentTab
   * @return
   */
  def makeHeaderPage(title: String, content: CodeWithHeader, parentTab: TTabs.TIPLTab):
  Seq[Node] = {
    val appName = parentTab.appName
    val allHeaders = HTMLUtils.getHeader+content.header
    val header = parentTab.headerTabs.map { tab =>
      <li class={if (tab == parentTab) "active" else ""}>
        <a href={UIUtils.prependBaseUri(parentTab.basePath, "/" + tab.prefix)}>
          {tab.name.replaceAll("_", " ")}
        </a>
      </li>
    }
    <html>
      <head>
        <meta http-equiv="Content-type" content="text/html;
     charset=utf-8"/>
        {allHeaders.css}
        <title>
          {appName}
          -
          {title}
        </title>
      </head>
      <body>
        <nav class="navbar navbar-static-top navbar-default" role="navigation">
          <div class="container-fluid">
            <!-- Brand and toggle get grouped for better mobile display -->
            <div class="navbar-header">
              <a href={UIUtils.prependBaseUri("/")} class="navbar-brand">
                <img src={UIUtils.prependBaseUri("/tstatic/assets/4Quant_Button.png")}/>
              </a>
            </div>
            <ul class="nav navbar-nav">
              {header}
            </ul>
            <ul class="nav navbar-nav navbar-right">

              <li class="dropdown">
                <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button"
                   aria-expanded="false">Run... <span class="caret"></span></a>
                <ul class="dropdown-menu" role="menu">
                  <li><a href="#">Load Data</a></li>
                  <li><a href="#">Run Plugin</a></li>
                  <li><a href="#">Export Data</a></li>
                  <li class="divider"></li>
                  <li><a href="#">Stop Spark</a></li>
                </ul>
              </li>
              <p class="navbar-text">
                <strong>
                  {appName}
                </strong>
                UI</p>
            </ul>
          </div>
        </nav>
        <div class="container-fluid">
          <div class="row">
            <div class="col-sm-12 col-md-12">
              <h3 style="vertical-align: bottom; display: inline-block;">
                {title}
              </h3>
            </div>
          </div>{content.code}
        </div>
        {allHeaders.js}
      </body>
    </html>
  }



  /**
   * Function for creating a new panel in the UI
   * @param sc sparkcontext
   * @param tabName name of the tab
   * @param rddList optional list of RDDs to include
   */
  def attachUI(sc: SparkContext, tabName: String = "TIPL", rddList: Option[MuMap[String,
    RDD[_]]] = None): Unit = {
    sc.ui match {
      case Some(wui) => wui.attachTab(new TTabs.TIPLTab(sc, wui, tabName, rddList))
      case None => System.err.println("No webgui present to attach to")
    }
  }


  /**
   * Since it is easier to just add a new gui using the standard method
   * @param sc
   */
  implicit class tiplSC(sc: SparkContext) {
    def attachUI(tabName: String = "TIPL", rddList: Option[MuMap[String, RDD[_]]] = None): Unit = {
      TiplUI.attachUI(sc, tabName, rddList)
    }



    def addRDD(panelName: String, rddName: String, data: RDD[_]): Boolean = {
      sc.ui match {
        case Some(webgui) => webgui.
          getTabs.filter(_.prefix.trim().equalsIgnoreCase(panelName.trim())).map(
          cTab =>
            cTab match {
              case atab: TTabs.TIPLTab => {
                atab.addRDDToTab(rddName, data)
                true
              }
              case _ => false
            }).reduce(_ & _)
        case None =>
          System.err.println("No web gui is currently runnings")
          false
      }

    }
  }

  /**
   * Create a webgui and then hang
   * @param sc
   * @param guiName
   */
  def createUIAndWait(sc: SparkContext,guiName: String = "Multiple_RDDs"): Unit = {

    if(guiName.length()>0) sc.attachUI(guiName)

    val (as, newPort) = AkkaUtils.createActorSystem("TIPLGUI", "localhost", 1234,
      sc.getConf,
      sc.env.securityManager)


    as.awaitTermination()
  }

  def main(args: Array[String]): Unit = {
    val p = SparkGlobal.activeParser(args)
    val ldd = p.getOptionBoolean("demo",true,"Load demo data")
    val sc = SparkGlobal.getContext("SimpleUI Demo").sc

    if(ldd) loadDemoData(sc)
    // code to make my own tabs
    //sc.addRDD("Images", "Brains", brainImage)
    // make it hang for awhile
    createUIAndWait(sc)

  }

  def loadDemoData(sc: SparkContext): Unit = {
    val cData = sc.parallelize(0 to 100).cache()
    val rData = cData.cartesian(cData).cache()
    val dData = sc.parallelize(101 to 200).map(_ / 150.0 + 0.01).map(Math.pow(_, 3)).cache()

    val arrayData = sc.parallelize(1 to 10).map(i => (i, (for (j <- 0 to 100) yield Math.pow(j,
      i)).toArray)).persist

    val fData = cData.map("Je m'appelle " + _.toString).cache()
    val lineImage: TImgRO = TestPosFunctions.wrapItAs(10, new DiagonalPlaneAndDotsFunction,
      TImgTools.IMAGETYPE_FLOAT)

    val bigLineImage: TImgRO = TestPosFunctions.wrapItAs(256, new BoxDistances(255, 255, 150),
      TImgTools.IMAGETYPE_FLOAT)
    val testDSImg = new DSImg[Float](sc, lineImage, TImgTools.IMAGETYPE_FLOAT)
    testDSImg.getBaseImg().persist()

    val bigTestDSImg = new DSImg[Float](sc, bigLineImage, TImgTools.IMAGETYPE_FLOAT)
    val bbimg = bigTestDSImg.getBaseImg().persist()
    bbimg.setName("PT:"+bbimg.partitioner.map(_.toString()))

    val finalTest = sc.parallelize(Array(("Big", bigLineImage), ("Little", lineImage), ("Big",
      bigLineImage))).persist()

    sc.attachUI("Images", Some(MuMap(("Little" -> testDSImg.getBaseImg()), ("Big" ->
      bbimg), ("Final" -> finalTest))))


    sc.attachUI("Numbers", Some(MuMap(("Integer" -> cData), ("Double", dData))))

    sc.addRDD("Numbers", "MessyCart", rData.flatMap(ival => Seq(ival._1, ival._2)))
    sc.addRDD("Numbers", "DoubleVec", arrayData)
    import _root_.tipl.spark.IOOps._
    val local=true
    val brainPath = if(local) {
      "/Users/mader/Dropbox/WorkRelated/Raber/bci102014"
    } else {
      "/Volumes/WORKDISK/WorkData/Raber/bci102014_2"
    }

    val brainImage = sc.tiffFolder(brainPath+
      "/brain*3/*.tif")
      .map {
      inSlice =>
        // focus on the red channel
        TImgTools.rgbConversionMethod = TImgTools.RGBConversion.RED
        val imgPath = inSlice._1.split("/").reverse
        (imgPath(1) + "/" + imgPath(0),
          new TImgSlice[Array[Float]](inSlice._2.polyReadImage(TImgTools.IMAGETYPE_FLOAT)
            .asInstanceOf[Array[Float]], D3int.zero, inSlice._2.getDim))
    }

  }
}

