package org.apache.spark.ui.tipl

import java.awt.image.BufferedImage
import java.awt.{Graphics2D, RenderingHints}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import javax.imageio.ImageIO
import javax.servlet.http.HttpServletRequest

import _root_.tipl.blocks.BaseTIPLBlock
import _root_.tipl.deepzoom.JDZ.MosaicTImg
import _root_.tipl.formats.TImgRO
import _root_.tipl.spark.DSImg.IntSlicePartitioner
import _root_.tipl.util.{TIPLPluginManager, TImgSlice, TImgTools}
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.ui._
import org.apache.spark.ui.tipl.HTMLUtils.{CodeWithHeader, _}
import org.apache.spark.util.Utils
import org.apache.spark.{Dependency, SparkContext}
import org.json4s._

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.{Map => MuMap}
import scala.reflect.ClassTag
import scala.xml.Node

/**
 * Created by mader on 12/9/14.
 */
object TPages {
  val statsPageName = ""

  import org.apache.spark.ui.tipl.TTabs.{CachedImageObject, cachedRDD}

  object RDDSlicePage extends Serializable {

    val maxSliceDimension = 256
    val stdFormat = "png"

    /**
     * Get the history as a Mermaid style graph
     * @param srdd the RDD to trace
     * @return a tree (if it has any dependencies) otherwise nothing
     */
    def getHistoryAsGraph(srdd: RDD[_]): Seq[Node] = {
      JSONTools.makeGraphFromSeq(recurseDep(srdd))
    }

    /**
     * recurve the dependency tree to reconstruct the RDD
     * @param srdd current RDD
     * @return list of links as start,end node strings
     */
    def recurseDep(srdd: RDD[_]): Seq[((Int,String),(Int,String))] = {
      val cKey = rddName(srdd)
      srdd.dependencies match {
        case depList: Seq[Dependency[_]] if depList.length>0 =>
          val oList = for(i <- depList)
          yield (recurseDep(i.rdd) :+ ((srdd.id,cKey), (i.rdd.id,rddName(i.rdd))))
          oList.flatten
        case _ => Seq[((Int,String),(Int,String))]()
      }

    }


    def getRddName(srdd: RDD[_]): Option[String] = srdd.name match {
      case null => None
      case s: String if s.length()>0 => Some(s)
      case _ => None
    }

    def rddName(srdd: RDD[_]): String = getRddName(srdd).getOrElse(srdd
      .getClass.getSimpleName)

    def getSamples[T](srdd: RDD[T], samples: Int,withRep: Boolean = false) = {
      samples match {
        case s if s>0 =>
          srdd.takeSample(withRep,samples)
        case -1 =>
          srdd.collect()
        case _ =>
          throw new IllegalArgumentException(samples+" is not a valid sample count")
      }
    }

    def indexableRDD[A: ClassTag, B: ClassTag](srdd: RDD[(A, B)]): RDD[(Long, (A, B))] = {
      srdd.zipWithIndex.
        map(ikv => (ikv._2, (ikv._1._1, ikv._1._2))).
        partitionBy(new IntSlicePartitioner(0, srdd.getCount().toInt))
    }

    def KVRDDToSliceList[A, B](srdd: RDD[(A, B)], slicePath: String, tlevel: Int):
    RDD[(String, String)] = {

      var stringRdd = srdd.map(_._1.toString)
      if (tlevel>1) stringRdd=stringRdd.flatMap(iname =>
        for(x<-0 until tlevel; y<-0 until tlevel) yield iname+"_"+x+"_"+y)
        stringRdd.zipWithIndex.mapValues(
        index =>
          slicePath + "/" + index+"?tlevel="+tlevel)
    }

    def SliceRDDToByte[A, B <: TImgSlice[_]](srdd: RDD[(A, B)], format: String, tlevel: Int) = {
      require(tlevel > 0,"Level must be a positive integer")
      val outSize =  maxSliceDimension*tlevel
      val baseSlices = srdd.map(ikv => (ikv._1.toString(),
        RDDSlicePage.sliceToByteArray(ikv._2, format,maxSize=outSize)))

      if(tlevel>1) {
        baseSlices.flatMap(ikv => {
          val (name,inSliceData) = ikv
          // tlevel x tlevel tiles of maxSliceDimension size
          val slice = ImageIO.read(new ByteArrayInputStream(inSliceData))
          val tiles = sliceToTiles(slice,format,
            maxSliceDimension,maxSliceDimension,tlevel,tlevel)
          for(tile <- tiles) yield (name+"_"+tile._1+"_"+tile._2,tile._3)
        }).zipWithIndex.map(_.swap)
      } else {
        baseSlices.zipWithIndex.map(_.swap)
      }
  }

    def sliceToTiles(slice: BufferedImage, format: String, xTileSize: Int, yTileSize: Int,
                     xTiles: Int, yTiles: Int) = {
      val tOut = mutable.Buffer[(Int,Int,Array[Byte])]()
      for(x<-0 until xTiles; y<- 0 until yTiles) {
        val xPos=x*xTileSize
        val yPos=y*yTileSize
        val curTile: BufferedImage = new BufferedImage(xTileSize, yTileSize, slice.getType)
        val g: Graphics2D = curTile.createGraphics
        // try interpolation
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.drawImage(slice,
          0, 0, xTileSize, yTileSize, // destination coordinates to tile
          xPos, yPos, xPos+xTileSize,yPos+yTileSize, // input coordinates from slice
          null)

        val os = new ByteArrayOutputStream()
        ImageIO.write(curTile, format, os)
        tOut.append((x,y,os.toByteArray()))
      }
      tOut
    }

    /**
     * turn a single slice into a byte array
     *
     * @param tB
     * @return
     */
    def sliceToByteArray(tB: TImgSlice[_], format: String,
                         maxSize: Int = maxSliceDimension): Array[Byte] = {
      println(tB.toString() + " of dimensions:" + tB.getDim.toString)
      val dbArr = tB.getAsDouble()
      val minVal = dbArr.min
      val maxVal = dbArr.max

      val bImg: BufferedImage = MosaicTImg.byteSliceDataToBImg(dbArr.map(dbVal => Math.round(
        (dbVal - minVal) /
          (maxVal - minVal) * 255).toInt).toArray,
        tB.getDim().gx,
        tB.getDim.gy)
      bufToByte(bImg, format,maxSize)
    }

    val forceRescale = true

    /**
     * make the buffered image into an output image
     * @param bImg
     * @param format
     * @return
     */
    def bufToByte(bImg: BufferedImage, format: String, maxSize: Int = maxSliceDimension):
    Array[Byte] = {
      val (wid, hgt) = (bImg.getWidth, bImg.getHeight)
      val rImg = if ((wid > maxSize) || (hgt > maxSize) || forceRescale) {
        val (sWid, sHgt) = if (wid > hgt) (maxSize, (maxSize * hgt) / wid) else ((maxSize * wid)
          / hgt, maxSize)
        val outImg: BufferedImage = new BufferedImage(sWid, sHgt, bImg.getType)
        val g: Graphics2D = outImg.createGraphics
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.drawImage(bImg, 0, 0, sWid, sHgt, 0, 0, wid, hgt, null)
        outImg
      }
      else bImg
      val os = new ByteArrayOutputStream()
      ImageIO.write(rImg, format, os)
      os.toByteArray()
    }

    def TImgRDDToByte[A, B <: TImgRO](srdd: RDD[(A, B)], subslice: Int,
                                      format: String) =
      srdd.zipWithIndex.map(ikv => (ikv._2,(ikv._1._1.toString, RDDSlicePage.TImgToByteArray(ikv._1
        ._2, subslice, format))))

    def TImgToByteArray(tB: TImgRO, subslice: Int, format: String): Array[Byte] = {
      println(tB.toString() + " of dimensions:" + tB.getDim.toString)
      val dbArr = tB.getPolyImage(subslice,
        TImgTools.IMAGETYPE_FLOAT)
        .asInstanceOf[Array[Float]]
      val minVal = dbArr.min
      val maxVal = dbArr.max
      val bImg = MosaicTImg.byteSliceDataToBImg(dbArr.map(dbVal => Math.round(
        (dbVal - minVal) /
          (maxVal - minVal) * 255).toInt).toArray,
        tB.getDim().gx,
        tB.getDim.gy)
      bufToByte(bImg, format)
    }
  }

  /**
   * A simple 'page' for getting jpeg images of the slice data out
   * @param parentGUI
   * @param parentTab
   * @param basePath
   */
  class RDDSlicePage(parentGUI: SparkUI, parentTab: TTabs.TIPLTab,
                     basePath: String, slicePageName: String = "slice") extends
  WebUIPage(slicePageName) {
    val thumbPath = "/" + parentTab.prefix + "/" + slicePageName
    val slicePath = thumbPath + "/" + WebViz.RAWPREFIX

    val format = "png"



    /**
     * the function to produce a from a given RDD
     *
     * @param request
     * @return
     */
    def sliceServlet(request: HttpServletRequest): Array[Byte] = {
      val curRequest = request.getServletPath().split("/")
      // the first is empty

      val id = curRequest(1)
      val slice = if (curRequest.length > 2) Some(curRequest(2).toInt) else None
      val subslice = if (curRequest.length > 3) Some(curRequest(3).toInt) else None
      val tlevel = request.getIntOption("tlevel").getOrElse(1)

      val countScale = tlevel*tlevel

      parentTab.getSavedRDD(id) match {
        case Some(sRdd) =>
          (sRdd.getFirst(), sRdd) match {
            case ((_, firstSlice: TImgSlice[_]), tRdd: RDD[(_, TImgSlice[_])]) =>
              // for TImgSlice images the subslice is used to store the zoom level
              slice match {
                case Some(sliceNo) if sliceNo < (tRdd.getCount()*countScale) =>
                  lazy val newResult = CachedImageObject(tRdd.id, tlevel, RDDSlicePage
                    .SliceRDDToByte(tRdd, format,tlevel).persist)
                  parentTab.currentImage = parentTab.synchronized {
                    parentTab.currentImage match {
                      case emptyImage if emptyImage.isEmpty() => newResult
                      case CachedImageObject(id, lastlevel, outRdd) if ((sRdd.id == id) &&
                        (lastlevel==tlevel)) =>
                        CachedImageObject(id, tlevel, outRdd) // if it is the last
                      // image used
                      case oldImage => oldImage.update(newResult)
                    }
                  }
                  parentTab.currentImage.rdd.lookup(sliceNo).map(_._2).head
                case Some(sliceNo) =>
                  throw new IllegalArgumentException("Slice " + sliceNo + " of "+
                    tRdd.getCount()+"*"+countScale+" for " + id + " is out of bounds")

                case None => RDDSlicePage.sliceToByteArray(firstSlice, format)
              }
            case ((_, firstImage: TImgRO), tRdd: RDD[(_, TImgRO)]) =>
              val curss = subslice match {
                case Some(a) => a;
                case None => 0
              }
              lazy val newResult = CachedImageObject(tRdd.id, curss, RDDSlicePage
                .TImgRDDToByte(tRdd, curss,
                  format).persist)
              slice match {
                case Some(sliceNo) if sliceNo < tRdd.getCount() =>
                  parentTab.currentImage =
                    parentTab.synchronized {
                      parentTab.currentImage match {
                        case oldImage if oldImage.isEmpty => newResult
                        case CachedImageObject(id, sslice, outRdd) if (sRdd.id == id) &
                          (sslice == curss) =>
                          parentTab.currentImage // if it is the last
                        // image used
                        case oldImage => oldImage.update(newResult)
                      }
                    }
                  parentTab.currentImage.rdd.lookup(sliceNo).map(_._2).head
                case Some(sliceNo) =>
                  throw new IllegalArgumentException("Slice " + sliceNo + " for " + id + " is out" +
                    " of " +
                    "bounds!")
                case None => RDDSlicePage.TImgToByteArray(firstImage, curss, format)
              }

            case fEle: Any =>
              println("RDD is not an image:" + fEle.toString())
              WebViz.backupImage
          }
        case None =>
          println("Image not located in RDDList!")
          WebViz.backupImage
      }
    }

    /**
     * The code to handle slices
     */
    def handleSlices(): Unit = {
      parentGUI.attachHandler(
        HTMLUtils.createBinaryServletHandler(slicePath, "image/" + format, sliceServlet,
          parentGUI.getSecurityManager,
          basePath))
    }

    def generateThumbnails(request: HttpServletRequest, rddName: String,
                           curRdd: RDD[_]): Option[CodeWithHeader] = {
      val tlevel = request.getIntOption("tlevel").getOrElse(1)
      val sampleCount = request.getIntOption("samples").getOrElse(16)

      (curRdd.getFirst(), curRdd) match {
        case ((_, slice: TImgSlice[_]), sliceRdd: RDD[(_, TImgSlice[_])]) =>
          val namePathR = RDDSlicePage.KVRDDToSliceList(sliceRdd, slicePath + "/" + rddName,tlevel)

          Some(HTMLUtils.makeThumbnailPage(RDDSlicePage.getSamples(namePathR,sampleCount)))
        case ((_, dbArr: Array[Double]), sliceRdd: RDD[(_, _)]) =>
          val namePathR = RDDSlicePage.KVRDDToSliceList(sliceRdd,
            "/" + parentTab.prefix + "/" + parentTab.detailsPage.detailPageName + "/json/" +
              rddName,tlevel)
          Some(HTMLUtils.makeThumbnailPage(RDDSlicePage.getSamples(namePathR,sampleCount),
            tmbFunc = HTMLUtils.thumbHistFunc))
        case _ => None

      }
    }

    /**
     *  make a thumbnail preview page
     * @param request contains the RDD id as parameter
     * @return html for the given page
     */
    override def render(request: HttpServletRequest): Seq[Node] = {
      request.getParameter("id") match {
        case rddName: String if rddName.length() > 0 =>
          parentTab.getSavedRDD(rddName) match {
            case Some(curRDD) =>
              return generateThumbnails(request,
                rddName, curRDD) match {
                case Some(content) => TiplUI.makeHeaderPage("Thumbnails for " + rddName, content,
                  parentTab)
                case None => TiplUI.makeErrorPage("Thumbnails not supported", "The RDD:" + rddName +
                  " does not have the correct type :" + curRDD.elementClassTag.getClass(),
                  CodeWithHeader(Seq[Node]()), parentTab)
              }
            case None =>
              return TiplUI.makeErrorPage("No Image", "Image:" + rddName + " could not be located!",
                parentTab.getAvailableRDDs(), parentTab)
          }
        case _ =>
          return TiplUI.makeErrorPage("Bad Query", "Query has no id argument:" + request
            .getParameterMap().map(kv => "Variable: " + kv._1 + " = " + kv._2).mkString(" "),
            parentTab.getAvailableRDDs(),
            parentTab)
      }
    }

    // automatically attach
    handleSlices()
  }


  case class RDDDetailPage(sc: SparkContext, parentGUI: WebUI, parentTab: TTabs.TIPLTab,
                           detailPageName: String = "details") extends
  WebUIPage(detailPageName) {
    override def render(request: HttpServletRequest): Seq[Node] = {
      request.getParameter("id") match {
        case rddName: String if rddName.length() > 0 =>
          parentTab.getSavedRDD(rddName) match {
            case Some(curRDD) =>
              return TiplUI.makeHeaderPage("Summary for " + rddName, generateDetailPage(request,
                rddName, curRDD),
                parentTab)
            case None =>
              return TiplUI.makeErrorPage("Missing RDD", "RDD:" + rddName + " could not be " +
                "located!",
                parentTab.getAvailableRDDs(), parentTab)
          }
        case _ =>
          return TiplUI.makeErrorPage("Bad Query", "Query does not have id argument: " + request
            .getParameterMap().map(kv => "Variable: " + kv._1 + " = " + kv._2).mkString(" "),
            parentTab.getAvailableRDDs(),
            parentTab)
      }

    }

    def generateDetailPage(request: HttpServletRequest,
                           rddName: String, curRDD: RDD[_]): CodeWithHeader = {
      val summaryInfo =
        CodeWithHeader(parentTab.getSummary(rddName, curRDD.getFirst(), curRDD))

      val sliceCount = curRDD.getCount
      lazy val cHist = HTMLUtils.makeHistogram(request.getRequestURL() + "json/" + rddName)

      (curRDD.getFirst, curRDD) match {
        case ((_, firstSlice: TImgSlice[_]), tRdd: RDD[(_, TImgSlice[_])]) =>
          summaryInfo + HTMLUtils.sliceSlider("slice", sliceCount,
            Some(parentTab.sliderCode(false))) + cHist
        case ((_, firstImage: TImgRO), tRdd: RDD[(_, TImgRO)]) =>
          summaryInfo + HTMLUtils.sliceSlider("slice", sliceCount,
            Some(parentTab.sliderCode(false))) + HTMLUtils.sliceSlider("subslice",
            firstImage.getDim().z, Some(parentTab.sliderCode(true))) + cHist
        case _ =>
          summaryInfo + cHist
      }

    }

    /**
     * code to generate the JSON data needed for plotting histograms and other plots with D3
     * @param request the query
     * @return the json data stored as ("bin", "count") for histograms and ("x","y") for plots
     */
    override def renderJson(request: HttpServletRequest): JValue = {
      val curRequest = request.getPathMap
      // the first is empty
      val id = curRequest(1)
      val slice = curRequest.get(2).map(_.toInt)
      val subslice = curRequest.get(3).map(_.toInt)

      curRequest.get(1) match {
        case Some(id) =>
          val rdds = parentTab.getSavedRDD(id)
          rdds match {
            case  Some(curRDD: RDD[_]) =>
              return parentTab.getDetailsJSON(curRDD.getFirst(), curRDD, slice, subslice)
            case None =>
              return JSONTools.blankHist
          }
        case _ => JSONTools.blankHist

      }
    }
  }


  /**
   * A class for executing commands and creating new images on TIPL
   * @param sc
   * @param parentGUI
   * @param parentTab
   * @param processingName
   */
  class RDDProcessingPage(sc: SparkContext, parentGUI: WebUI, parentTab: TTabs.TIPLTab,
                          processingName: String) extends WebUIPage(processingName) {
    def process(request: HttpServletRequest) = {
      (request.getParameter("cmd"), request.getParameter("op")) match {
        case ("exec", pluginName: String) => TIPLPluginManager.createBestPluginIO(pluginName)
        case ("thresh", valToThresh: String) => "heelo"
      }
    }

    override def render(request: HttpServletRequest): Seq[Node] = <div class="alert alert-danger"
                                                                       role="alert">This
      hasn't been implemented yet</div>

    override def renderJson(request: HttpServletRequest): JValue = {
      (request.getOrElse("op"),
        request.getOrElse("type"),
        request.getOrElse("value"))
      match {
        case ("list", "plugin",_) => JSONTools.pluginsAsJSON
        case ("list", "block",_) => JSONTools.blocksAsJSON
        case ("getarg","plugin",pluginName: String) =>
          val plug = TIPLPluginManager.createBestPlugin(pluginName)
          JSONTools.getParametersAsJSON(plug,"")
        case ("getarg","block",blockName: String) =>
          val block = BaseTIPLBlock.getBlockList().get(blockName)
          JSONTools.getParametersAsJSON(block.get())
        case _ => JSONTools.blankHist
      }

    }
  }


  class RDDStatisticsPage(sc: SparkContext, parentGUI: WebUI, parentTab: TTabs.TIPLTab) extends
  WebUIPage(statsPageName) {

    /** Header fields for the block table */
    private def rddOverviewHeader = Seq(
      "ID",
      "Name/Creation Site",
      "Storage Level",
      "First Element",
      "Count",
      "Summary")


    /** Render an HTML row representing a block */
    private def rddRow(rddRow: (String, RDD[_])): Seq[Node] = {
      val (id, rddObj) = rddRow
      val firstEl = rddObj.getFirst()
      val count = rddObj.getCount()
      <tr>
        <td sorttable_customkey={id}>
          <strong>
            <a href={parentTab.detailsPage.detailPageName + "/?id=" + id}>
              {id}
            </a>
          </strong>
        </td>
        <td>
         Name: {rddObj.name}<br/>
          Creation: { RDDSlicePage.getHistoryAsGraph(rddObj)}
        </td>
        <td>
          <strong>
            {rddObj.getStorageLevel match {
            case a: StorageLevel if a.useDisk & a.useMemory => "Disk and Memory"
            case a: StorageLevel if a.useDisk => "Disk-only"
            case a: StorageLevel if a.useMemory => "Memory-only"
            case a: StorageLevel => a.toString()
          }}
          </strong>
          <br/>{rddObj.getStorageLevel.description}
        </td>
        <td sorttable_customkey={firstEl.toString}>
          {firstEl.toString}
        </td>
        <td sorttable_customkey={count.toString}>
          {count}
        </td>
        <td>
          {parentTab.getSummary(id, firstEl, rddObj)}
        </td>
      </tr>
    }

    def makeTable(inRDDs: Seq[(String, RDD[_])]): Seq[Node] = {
      val rddTable = UIUtils.listingTable(rddOverviewHeader, rddRow, inRDDs)

      val content =
        <div class="row-fluid">
          <div class="col-md">
            <ul class="unstyled">
              <li>
                <strong>Executor Memory Status:</strong>{sc.getExecutorMemoryStatus.toSeq.map(l
              => <span>
                  {l._1 + " " + Utils.bytesToString(l._2._1) + " of " + Utils.bytesToString(l._2
                    ._2)}<br/>
                </span>)}
              </li>
            </ul>
          </div>
        </div>
          <div class="row-fluid">
            <div class="col-md">
              <h4>
                {inRDDs.size}
                Number of RDD Objects</h4>{rddTable}
            </div>
          </div>;
      TiplUI.makeHeaderPage("RDD Summary Info", CodeWithHeader(content), parentTab)
    }

    def render(request: HttpServletRequest): Seq[Node] = {
      (request.getParameter("id"),request.getParameter("idlike")) match {
        case (_,a: String) if a.length() > 0 =>
          val keepRDDs = parentTab.getSavedRDDs.filter(_._1.trim.toLowerCase.contains(a.trim
            .toLowerCase))
          makeTable(keepRDDs.toSeq)
        case ("",_) => // empty string
          return TiplUI.makeErrorPage("Bad Argument", "Missing ID argument:" + request
            .getParameterMap().map(kv => "Variable: " + kv._1 + " = " + kv._2).mkString(" "),
            CodeWithHeader(Seq[Node]()), parentTab)
        case (_,_) =>
          // show all the persistent RDD objects
          makeTable(parentTab.getSavedRDDs().toSeq)
      }

    }



  }


}
