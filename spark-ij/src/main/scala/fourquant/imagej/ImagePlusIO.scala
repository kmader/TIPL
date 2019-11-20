package fourquant.imagej

import java.io._

import fourquant.imagej.Spiji.{PIPOps, PIPTools}
import ij.ImagePlus
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.collection.mutable.ArrayBuffer

object ImagePlusIO extends Serializable {

  case class LogEntry(opType: PIPOps,opTool: PIPTools,
                      opVal: String, opArgs: Array[String] = Array.empty[String],
                       children: IndexedSeq[LogEntry] = Array.empty[LogEntry]) {
    def toJSON(): JValue = {
      Map[String,JValue](
        ("optype" -> opType.toString),
        ("optool" -> opTool.toString),
        ("opval" -> opVal),
        ("opargs" -> opArgs.toSeq),
        ("children" -> children.map(_.toJSON))
      )
    }

    def toJSONString() = compact(toJSON())

    def le_eq(le: LogEntry) = {
      (
        (le.opType.toString.equalsIgnoreCase(opType.toString)) &
          (le.opTool.toString.equalsIgnoreCase(opTool.toString)) &
          (le.opVal.equalsIgnoreCase(opVal)) &
          (le.opArgs.mkString(", ").equalsIgnoreCase(opArgs.mkString(", ")))
        )
    }
  }
  import scala.collection.JavaConversions._

  object LogEntry {
    implicit val formats = DefaultFormats

    def apply(opType: PIPOps,opTool: PIPTools,
              opVal: String, opArgs: String): LogEntry = apply(opType,opTool,opVal,Array(opArgs))

    def apply(opType: PIPOps,opTool: PIPTools,
              opVal: String, opArgs: String, oldLog: Array[LogEntry]): LogEntry =
      apply(opType,
      opTool,
      opVal,
      Array(opArgs),
      oldLog)
    def getInfoFromImagePlus(ip: ImagePlus) = {
      val prop = ip.getProperties() match {
        case p if (p!=null) => Some(p)
        case _ => None
      }
      val tprop = prop match {
        case Some(pr) =>
          pr.stringPropertyNames().map(pname => (pname, pr.getProperty(pname))).
            mkString("[", ", ", "]")
        case None => "[]"
      }
      Array("InfoProperty: "+ip.getInfoProperty,
        "Properties: "+tprop,
        "Calibration: "+ip.getCalibration().toString
      )
    }

    def create(fromSrc: String, srcInfo: String) =
      LogEntry(PIPOps.CREATE,PIPTools.OTHER,fromSrc,Array(srcInfo))
    def create(srcImg: ImagePlus) =
      LogEntry(PIPOps.CREATE,PIPTools.IMAGEJ,srcImg.getTitle,getInfoFromImagePlus(srcImg))
    def createFromArray(srcName: String, srcArray: AnyRef) =
      LogEntry(PIPOps.CREATE,PIPTools.OTHER,srcName,Array(srcArray.toString))

    /**
     * record the loading of the images
     * @param loadCmd the command used to load (this is usually the name of the sparkcontext method)
     * @param loadPath the path being loaded (currently as a string)
     * @return a log entry of the loading event
     */
    def loadImages(loadCmd: String, loadPath: String) =
      LogEntry(PIPOps.LOAD,PIPTools.SPARK,loadCmd,loadPath)

    def ijRun(cmd: String, args: String) =
      LogEntry(PIPOps.RUN,PIPTools.IMAGEJ,cmd,Array(args))

    def fromJSON(inJSON: JValue): LogEntry = {

      val optype = PIPOps.valueOf((inJSON \ "optype").extract[String])
      val optool = PIPTools.valueOf((inJSON \ "optool").extract[String])
      val opval = (inJSON \ "opval").extract[String]
      val opargs = (inJSON \ "opargs" \\ classOf[JString]).map(_.toString).toArray
      val children: IndexedSeq[LogEntry] = (inJSON \ "children").
        children.map(fromJSON(_)).toIndexedSeq
      LogEntry(
        optype,optool,opval,opargs,children
      )
    }

    def mergeEntry(mergeName: String, mergeArgs: String, mergeLog: ImageLog) = LogEntry(
      PIPOps.MERGE_STORE,
      PIPTools.SPARK,mergeName,mergeArgs,
      mergeLog.ilog.toArray
    )
  }

  /**
   * A class for keeping track of the transformations applied to an image
   * @param ilog
   */
  case class ImageLog(ilog: ArrayBuffer[LogEntry]) {
    def this(le: LogEntry) = this(ArrayBuffer(le))

    def this(opType: PIPOps, opTool: PIPTools, opVal: String,
             opArgs: Array[String] = Array.empty[String]) =
      this(LogEntry(opType,opTool,opVal,opArgs))

    def this(oldlog: Iterable[LogEntry]) = this(ArrayBuffer.empty[LogEntry] ++ oldlog)
    //TODO this might eventually be a good place to show what has been "queried" from the image
    // for making more traceable 'filter' and 'reduce' commands
    def addComment(comment: String) =
      ilog.append(LogEntry(PIPOps.COMMENT,PIPTools.OTHER, comment))

    /**
     * Add a new entry and create a new imagelog
     * @param le the entry to be added
     * @return a new list
     */
    def appendAndCopy(le: LogEntry) = ImageLog(ilog ++ Array(le) )

    def copy() = ImageLog (ilog.clone() )

    def toJSON(): JValue =
      ilog.map(_.toJSON).toSeq

    /**
     * The entire list as a JSON string
     * @return
     */
    def toJSONString() = compact(toJSON)

    /**
     * Each entry as a json string
     * @return
     */
    @deprecated("With toJSONString this shouldn't be needed","1.0")
    def toJsStrArray(): Array[String] =
      ilog.map(i => compact(i.toJSON)).toArray

    def apply(index: Int) = ilog(index)
    def headOption = ilog.headOption

    /**
     * two logs are equal iff they have the same length and are elements are equal
     * @param log2
     * @return
     */
    def log_eq(log2: ImageLog): Boolean = log_eq(log2.ilog)

    def log_eq(log2: Iterable[LogEntry]): Boolean = {
      (
        (ilog.size == log2.size) &
          ilog.zip(log2).map{ case(le1,le2) => le1 le_eq le2 }.reduce(_ & _)
        )
    }

  }


  object ImageLog {
    def merge(logA: ImageLog, logB: ImageLog, opVal: String, opArgs: String): ImageLog = {
      ImageLog(
        ArrayBuffer(
          LogEntry.mergeEntry("A",opArgs,logA),
          LogEntry.mergeEntry("B",opArgs,logB),
          LogEntry(PIPOps.MERGE,PIPTools.SPARK,opVal+"(A+B)",Array(opArgs))
        )
      )
    }

    def fromJSON(logJV: JValue) = new ImageLog(logJV.children.map(LogEntry.fromJSON(_)))

    def fromJSONString(logJV: String) = fromJSON(parse(logJV))

  }



}
/**
 * Created by mader on 1/16/15.
 */
class ImagePlusIO {

}
