package org.apache.spark.ui.tipl

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.ui.JettyUtils._
import org.apache.spark.ui.tipl.HTMLUtils.CodeWithHeader
import org.apache.spark.ui.{SparkUI, SparkUITab}
import org.apache.spark.util.StatCounter
import org.json4s._
import tipl.formats.TImgRO
import tipl.util.{TImgSlice, TImgTools}

import scala.collection.mutable.{Map => MuMap}
import scala.reflect.ClassTag
import scala.xml.Node


/**
 * TiplUI is a modification fo the SparkUI for TIPL commands and code to avoid rewriting too much
 * of the basic webserver and gui tools
 * Created by mader on 11/21/14.
 */

object TTabs {

  private val rddCountCache = collection.mutable.Map[Int, Long]()
  private val rddFirstCache = collection.mutable.Map[Int, Any]()


  /**
   * Handle caching of results since first and count can take awhile to compute for some datasets
   * @param in
   */
  implicit class cachedRDD(in: RDD[_]) {
    def getCount(): Long =
      rddCountCache.getOrElseUpdate(in.id, in.count)

    def getFirst(): Any =
      rddFirstCache.getOrElseUpdate(in.id, in.first)
  }

  /**
   * a cache for a single image object which is held in memory and unpersisted when changed
   * @param slice
   * @param subslice
   * @param rdd
   */
  case class CachedImageObject(slice: Int, subslice: Int, rdd: RDD[(Long,(String,
    Array[Byte]))]
  = null) {
    def isEmpty(): Boolean = (rdd==null)
    def update(newImage: CachedImageObject) = {
      if (!isEmpty) {
        println("Unpersisting old RDD object:" + rdd)
        rdd.unpersist()
      }
      newImage
    }
  }


  /**
   * A statically typed tab so the contents of the RDD are known
   * @param sc
   * @param parent
   * @param tabName
   * @tparam T the type of the RDDs accepted
   */
  abstract class TypedTab[T](sc: SparkContext, parent: SparkUI, tabName: String
                             )(implicit nt: ClassTag[T]) extends SparkUITab(parent, tabName) with
  Serializable {
    /**
     * This allows the RDD list to be actively updated or polled from elsewhere
     * @return the map of RDDs
     */
    def getSavedRDDs(): MuMap[String,RDD[T]]

    def addRDDToTab(rddName: String, curRDD: RDD[T]): Unit =
      getSavedRDDs += (rddName -> curRDD)

    def getSavedRDD(id: String) = getSavedRDDs.filter(_._1.trim().equalsIgnoreCase(id.trim())).
        map(_._2).headOption

    def eleToHTML(ele: T) = <p>{ele.toString}</p>

    def getAvailableRDDs(): CodeWithHeader = {
      CodeWithHeader(<div class="row-fluid">
        <li>
          <strong>
            {"Available RDD Names:"}
          </strong>
        </li>{getSavedRDDs().map(_._1).map(ele => <li>
          <a href={"?id=" + ele}>
            {ele}
          </a> <br/>
        </li>).toSeq}
      </div>)
    }
  }


  /**
   * A generic tab to serve as a visual for all types of data
   * @param sc
   * @param parent name of the GUI to attach to
   * @param tabName name of the tab
   * @param rddList an optional rddList for list-driven (rather than resident) RDD lists
   */
  class TIPLTab(sc: SparkContext, parent: SparkUI, tabName: String = "TIPL",
                rddList: Option[MuMap[String, RDD[_]]] = None) extends
  SparkUITab(parent,tabName) {
    private val timeout: Long = 5

    /**
     * A function to get all the saved RDDs, currently uses a fairly simple basis of the
     * persistent rdd objects but may eventually look to another cache elsewhere (sparkglobal for
     * example)
     * @return map or rdd-names and rdd objects
     */
    def getSavedRDDs(): MuMap[String, RDD[_]] = {
      val curRdds = rddList.getOrElse(sc.getPersistentRDDs)
      val outRdds = curRdds.map {
        inObj =>
          (inObj._1.toString, inObj._2)
      }
      MuMap(outRdds.toSeq: _*)
    }

    /**
     * A function to add an new RDD to the given tab, if the tab is not list backed,
     * the RDD is persisted so it shows up in the general query
     * @param rddName name of the RDD to add
     * @param curRDD the rdd itself
     */
    def addRDDToTab(rddName: String, curRDD: RDD[_]): Unit = {
      rddList match {
        case Some(aList) => aList += (rddName -> curRDD)
        case None => curRDD.persist()
      }
    }

    def getSavedRDD(id: String) = getSavedRDDs.filter(_._1.trim().equalsIgnoreCase(id.trim())).
      map(_._2).headOption

    def getAvailableRDDs(): CodeWithHeader = {
      CodeWithHeader(<div class="row-fluid">
        <li>
          <strong>
            {"Available RDD Names:"}
          </strong>
        </li>{getSavedRDDs().map(_._1).map(l => <li>
          <a href={"?id=" + l}>
            {l}
          </a> <br/>
        </li>).toSeq}
      </div>)
    }

    implicit def statCounterToHTML(st: StatCounter): Seq[Node] = {
      <div class="well">
        <li>
          {"Min:" + st.min}
        </li>
        <li>
          {"Mean:" + st.mean}
        </li>
        <li>
          {"Sample Std.:" + st.sampleStdev}
        </li>
        <li>
          {"Sample Var.:" + st.sampleVariance}
        </li>
        <li>
          {"Max:" + st.max}
        </li>
        <li>
          {"Sum:" + st.sum}
        </li>
      </div>
    }

    val slicePreviewName = "slicePreview"
    val runStats = false

    /**
     * create a brief summary of object
     * @param id
     * @param firstEl
     * @param rddObj
     * @return
     */
    def getSummary(id: String, firstEl: Any, rddObj: RDD[_]): Seq[Node] = {
      (firstEl, rddObj) match {
        case (fVal: Number, tRdd: RDD[Number]) if (runStats) =>
          tRdd.map(_.doubleValue).stats()
        case (fVal: Number, nRdd: RDD[Number]) =>
          val tRdd = nRdd.map(_.doubleValue)
          <span>
            {"Min:" + tRdd.min}<br/>
          </span>
            <span>
              {"Max:" + tRdd.max}<br/>
            </span>
            <span>
              {"Mean:~" + tRdd.meanApprox(timeout)}<br/>
            </span>
        case (fVal: String, tRDD: RDD[String]) =>
          <span>
            {"Min:" + tRDD.min}<br/>
          </span>
            <span>
              {"Max:" + tRDD.max}<br/>
            </span>
            <span>
              {"Unique Values:~" + tRDD.countApproxDistinct(0.25)}<br/>
            </span>
        case (kvPair: (_, _), _) =>
          val (key, value) = kvPair
          value match {
            case slice: TImgSlice[_] =>
              <div class="panel panel-default">
                <div class="panel-heading">
                  <span class="label label-default">Slice:
                    {key.toString}
                  </span> <span class="label label-primary">
                  {slice.getClass().getSimpleName}
                </span> <span class="label label-info">
                  {TImgTools.getImageTypeName(TImgTools.identifySliceType(slice.get()))}
                </span>
                </div>
                <div class="panel-body">
                  <a href={slicePage.thumbPath + "/?id=" + id} class="Thumbnail">
                    <img id={slicePreviewName} src="/tstatic/assets/loading.png"
                         data-src={slicePage.slicePath + "/" + id}></img>
                  </a>
                </div>
                <ul class="list-group">
                  <li class="list-group-item">
                    {"Key:" + key.toString}
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
            case slice: TImgRO =>
              <div class="panel panel-default">
                <div class="panel-heading">
                  <span class="label label-default">
                    {slice.getSampleName()}
                  </span> <span class="label label-primary">
                  {slice.getClass().getSimpleName}
                </span> <span class="label label-info">
                  {TImgTools.getImageTypeName(slice.getImageType)}
                </span>
                </div>
                <div class="panel-body">
                  <ul class="nav nav-tabs" id={"tab_nav_" + id}>
                    <li>
                      <a href={"#tab_image_" + id}
                         data-toggle="tab">Preview
                        Image</a>
                    </li>
                    <li class="active">
                      <a href={"#tab_overview_" + id}
                         data-toggle="tab">Overview</a>
                    </li>
                    <li>
                      <a href={"#tab_log_" + id} data-toggle="tab">Processing Log</a>
                    </li>
                  </ul>
                  <div class="tab-content" id={"tab_content_" + id}>
                    <div class="tab-pane" id={"tab_image_" + id}>
                      <img id={slicePreviewName} src="/tstatic/assets/loading.png"
                           data-src={slicePage.slicePath + "/" + id}></img>
                    </div>
                    <div class="tab-pane active" id={"tab_overview_" + id}>
                      <ul class="list-group">
                        <li class="list-group-item">
                          {"Key:" + key.toString}
                        </li>
                        <li class="list-group-item">
                          {"Size:" + slice.getDim()}
                        </li>
                        <li class="list-group-item">
                          {"Position:" + slice.getPos()}
                        </li>
                        <li class="list-group-item">
                          {"Type:" + TImgTools.getImageTypeName(slice.getImageType)}
                        </li>
                      </ul>
                    </div>
                    <div class="tab-pane" id={"tab_log_" + id}>
                      <h4>Image Processing Log</h4>
                      <p>
                        {slice.getProcLog()}
                      </p>
                    </div>
                  </div>
                </div>
              </div>

            case _ =>
              <span>
                {"First 3 values"}<br/>
              </span>
              rddObj.map(l => <span>
                {l}<br/>
              </span>).take(3)
          }
        case _ =>
          <span>
            {"First 3 values"}<br/>
          </span>
          rddObj.map(l => <span>
            {l}<br/>
          </span>).take(3)
      }

    }

    var histogramBuckets = 15

    /**
     * Create a JSON from the object
     * @param firstEl
     * @param rddObj
     * @param sliceNum slice number (from keys)
     * @param subSliceNum slice number (for the slice within the TImg value)
     * @return a JSON containing the plot data to be displayed in D3,
     *         usually histogram but can also be line and point plots
     */
    def getDetailsJSON(firstEl: Any, rddObj: RDD[_], sliceNum: Option[Int] = None,
                       subSliceNum: Option[Int] = None): JValue = {
      val nsubslice = subSliceNum match { case Some(someNum) => someNum; case None => 0 }

      (firstEl, rddObj) match {
        case (fVal: Double, dRdd: RDD[Double]) =>
          JSONTools.genHist[Double](dRdd,histogramBuckets)
        case (fVal: Number, dRdd: RDD[Number]) =>
          val hist = dRdd.map(_.doubleValue()).histogram(histogramBuckets)
          JSONTools.histToJSON(hist._1.zip(hist._2))
        case ((_, slice: TImgSlice[_]), iRdd: RDD[(_, TImgSlice[_])]) =>
          JSONTools.genHist(slice.getAsDouble(),histogramBuckets)
        case ((_, slice: TImgRO), iRdd: RDD[(_, TImgSlice[_])]) =>
          JSONTools.genHist(slice.getPolyImage(nsubslice, TImgTools.IMAGETYPE_DOUBLE)
            .asInstanceOf[Array[Double]],histogramBuckets)
        case ((_, fVal: Array[Double]), dRdd: RDD[(_, Array[Double])]) =>
          val sRdd = {
            sliceNum match {
              case Some(slice) => dRdd.zipWithIndex.filter(_._2 == slice).map(_._1)
              case None => dRdd
            }
          }
          val hist = sRdd.flatMap(_._2).histogram(histogramBuckets)
          JSONTools.histToJSON(hist._1.zip(hist._2))
        case _ => JSONTools.blankHist
      }
    }



    @Deprecated
    def sliderCode(subslice: Boolean = false) = HTMLUtils.sliderCode(slicePreviewName,subslice)

    var currentImage: CachedImageObject = CachedImageObject(-1,-1)

    attachPage(new TPages.RDDStatisticsPage(sc, parent, this))
    val detailsPage = new TPages.RDDDetailPage(sc, parent, this)
    attachPage(detailsPage)

    /** so other elements can read it */
    val slicePage = new TPages.RDDSlicePage(parent, this, basePath)
    attachPage(slicePage)
    parent.attachHandler(createStaticHandler(TiplUI.STATIC_RESOURCE_DIR, "/tstatic"))

    parent.attachPage(new TPages.RDDProcessingPage(sc, parent, this, processingName = "processing"))

  }


}
