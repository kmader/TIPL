package org.apache.spark.ui.tipl

import javax.servlet.http.HttpServletRequest

import org.apache.spark.ui.tipl.HTMLUtils.CodeWithHeader
import org.apache.spark.ui.tipl.WebViz.{RDDInfo, ExtInfo, VizTool}
import org.json4s.JValue
import tipl.util.{TImgTools, TImgSlice}
import WebViz._

import scala.reflect.ClassTag

/**
 * Created by mader on 1/22/15.
 */
class TImgSliceViz(parentTabPrefix: String, slicePageName: String, sliceCount: Int,
                    trimLevel: Int = 0)(implicit val et: ClassTag[TImgSlice[_]])
  extends VizTool {

  override type elementType = TImgSlice[_]

  val thumbPath = "/" + parentTabPrefix + "/" + slicePageName
  val slicePath = thumbPath + "/" + WebViz.RAWPREFIX

  val format = "png"
  val imgSize = 250

  implicit def curTImgSliceVizReq(req: HttpServletRequest) =
  new HTTPWebVizRequest(req,trimLevel)


  /**
   * Support the conversion of elements which have elementType in their key (less common)
   * @return
   */
  override def supportKeys(): Boolean = false

  override def typedDetailRender(slice: elementType,info: ExtInfo, ri: RDDInfo,
                                 req: HttpServletRequest): CodeWithHeader =
  {

    val slicePreviewName = info.getOrElse("key","UnkSlice").toString

    val id = req.getRddId().getOrElse("Unknown_RDD")
    val idStr = req.getRddId().map("id="+_).getOrElse("")


    val summaryInfo = <div class="panel panel-default">
      <div class="panel-heading">
        <span class="label label-default">Slice:
          {info.toString}
        </span> <span class="label label-primary">
        {slice.getClass().getSimpleName}
      </span> <span class="label label-info">
        {TImgTools.getImageTypeName(TImgTools.identifySliceType(slice.get()))}
      </span>
      </div>
      <div class="panel-body">
        <a href={thumbPath + "/?id=" + id} class="Thumbnail">
          <img id={slicePreviewName} src="/tstatic/assets/loading.png"
               data-src={slicePath + "/" + id}></img>
        </a>
      </div>
      <ul class="list-group">
        <li class="list-group-item">
          {info.toString()}
        </li>
        <li class="list-group-item">
          {"Size:" + slice.getDim()}
        </li>
        <li class="list-group-item">
          {"Position:" + slice.getPos()}
        </li>
        <li class="list-group-item">
          {"Type:" + TImgTools.getImageTypeName(TImgTools.identifySliceType(slice.get()))}
        </li>
      </ul>
    </div>
    val cHist = HTMLUtils.makeHistogram(req.makePathJSON(idStr))
    CodeWithHeader(summaryInfo) + HTMLUtils.sliceSlider("slice", sliceCount,
      Some(HTMLUtils.sliderCode(slicePreviewName,false))) + cHist
  }

  override def typedJsonRender(ele: elementType, info: ExtInfo, ri: RDDInfo, request:
  HttpServletRequest): JValue = {
    JSONTools.genHist(ele.getAsDouble(),15)
  }

  override def typedSingleRender(slice: elementType, info: ExtInfo, ri: RDDInfo, req:
  HttpServletRequest): CodeWithHeader = {
    val id = req.getRddId().getOrElse("Unknown_RDD")
    val idStr = req.getRddId().map("id="+_).getOrElse("")

    CodeWithHeader(<div class="panel panel-default">
      <div class="panel-heading">
        <span class="label label-default">Slice:
          {info.toString}
        </span> <span class="label label-primary">
        {slice.getClass().getSimpleName}
      </span> <span class="label label-info">
        {TImgTools.getImageTypeName(TImgTools.identifySliceType(slice.get()))}
      </span>
      </div>
      <div class="panel-body">
        <a href={req.makePathSample(idStr)} class="Thumbnail">
          <img id={slicePageName} src="/tstatic/assets/loading.png"
               data-src={req.makePathRaw(idStr)}></img>
        </a>
      </div>
      <ul class="list-group">
        <li class="list-group-item">
          {info.toString}
        </li>
        <li class="list-group-item">
          {"Size:" + slice.getDim()}
        </li>
        <li class="list-group-item">
          {"Position:" + slice.getPos()}
        </li>
        <li class="list-group-item">
          {"Type:" + TImgTools.getImageTypeName(TImgTools.identifySliceType(slice.get()))}
        </li>
      </ul>
    </div>)
  }

  override def typedSampleRender(ele: elementType, info: ExtInfo, ri: RDDInfo, request:
  HttpServletRequest): CodeWithHeader = ???
}


