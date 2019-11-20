package org.apache.spark.ui.tipl

import org.apache.spark.rdd.RDD
import org.apache.spark.ui.UIUtils
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, _}
import tipl.blocks.BaseTIPLBlock
import tipl.util.ArgumentList.{MultipleChoiceArgument, RangedArgument}
import tipl.util.{ArgumentList, ArgumentParser, TIPLPluginManager}

import scala.collection.JavaConversions._
import scala.xml.Node


/**
 * Module for handling JSON data to make the results
 * easy to plot in D3 or Bokeh
 */
object JSONTools {
  // json portions of code
  private implicit val format = DefaultFormats


  val blankHist: JValue = histToJSON(Seq[(Double,Long)]((0,0)))

  /**
   * generate a histogram from a array of doubles
   * @param sdata the double array
   * @param histogramBuckets number of buckets in the histogram
   * @return json histogram ready for D3
   */
  def genHist(sdata: Array[Double],histogramBuckets: Int) = {
    val (mn, mx) = (sdata.min, sdata.max)
    val idata = sdata.map(x => Math.round((x - mn) / (mx - mn) * histogramBuckets)).groupBy(_
      .toInt)
      .mapValues(_.length).map(x => (x._1 * (mx - mn) + mn, x._2.toLong))
    JSONTools.histToJSON(idata.toSeq)
  }

  /**
   * Create a histogram from an RDD with the proper conversion
   * @param rddObj the input untyped (or anytyped) RDD
   * @param histogramBuckets the number of buckets in the histogram
   * @param num the implicit conversion class
   * @tparam A the type of the
   * @return
   */
  def genHist[A](rddObj: RDD[_],histogramBuckets: Int)(
    implicit num: Numeric[A]) = {
    import org.apache.spark.SparkContext._
    (Option(num),rddObj) match {
      case (Some(num),aRdd: RDD[A]) =>
        val hist = rddObj.asInstanceOf[RDD[A]].map(num.toDouble).
          histogram(histogramBuckets)
        JSONTools.histToJSON(hist._1.zip(hist._2))
      case (Some(jum),_) =>
        System.err.println("Type converter could not be found!")
        JSONTools.blankHist
      case (None,_) =>
        JSONTools.blankHist
    }
  }

  /**
   * Create JSON output from a histogram
   * @param inHist
   * @return
   */
  implicit def histToJSON(inHist: Seq[(Double,Long)]): JValue = {
    val asMap = inHist.sortBy(_._1).map(a => Map(("bin" -> a._1.toDouble),
      ("count" -> a._2.toDouble)))
    asMap
  }

  /**
   * List of plugins as json object
   * @return
   */
  def pluginsAsJSON: JValue = {
    TIPLPluginManager.getAllPlugins().map(cPlug => Map[String,JValue](
      ("name" -> cPlug.pluginType),
      ("desc" -> cPlug.desc),
      ("slice" -> cPlug.sliceBased),
      ("spark" -> cPlug.sparkBased())
    ))
  }

  /**
   * List of blocks as a JSON object
   * @return
   */
  def blocksAsJSON: JValue = {
    val blockList = BaseTIPLBlock.getAllBlockFactories().toMap.map(_._1)

    blockList.map(inF => Map[String,JValue](
      ("name" -> inF.blockName()),
      ("desc" -> inF.desc()),
      ("input" -> inF.inputNames().toList),
      ("output" -> inF.outputNames().toList)
      )
    )
  }

  /**
   * Get the arguments from a function and return it as a json object
   * @param hasArguments any object that has a setParameters method
   * @return
   */
  def getParametersAsJSON(hasArguments: { def setParameter(p: ArgumentParser): ArgumentParser }):
  JValue = {
    val emptyAP = ArgumentParser.CreateArgumentParser(Array[String](),false)
    val filledAP = hasArguments.setParameter(emptyAP)
    val allItems = filledAP.sneakyGetOptions().getAllItems()
    allItems.flatMap{
      cKey =>
        for(nKey <- cKey._2) yield (cKey._1,nKey._1,nKey._2)
    }.map{
      cItem =>
        val oVals = cItem._3 match {
          case mca: MultipleChoiceArgument => mca.acceptableAnswers
          case _ => Array[String]()
        }
        val (minVal,maxVal) = cItem._3 match {
          case rga: RangedArgument[_] => (Some(rga.minVal.toString),Some(rga.maxVal.toString))
          case _ => (None,None)
        }
        Map[String,JValue](
          ("layer" -> cItem._1),
          ("key" -> cItem._2),
          ("arg_name" -> cItem._3.getName()),
          ("arg_value" -> cItem._3.getValueAsString()),
          ("arg_help" -> cItem._3.getHelpText()),
          ("class" -> cItem._3.getValue.getClass().getSimpleName),
          ("values" -> oVals.toList),
          ("min_val" -> minVal),
          ("max_val" -> maxVal)
        )
    }
  }
  def getParametersAsJSON(hasArguments: { def setParameter(p: ArgumentParser, prefix: String):
  ArgumentParser }, prefix: String): JValue = {
    val nha = new { def setParameter(p: ArgumentParser) = hasArguments.setParameter(p,prefix)}
    getParametersAsJSON(nha)
  }

  case class ALElem(layer: String, key: String, arg: ArgumentList.Argument) extends Serializable


  /**
   * Convert a layeredmap to a useful scala object (based on the ALElem case class)
   * @param alist
   * @return
   */
  def AListToScala(alist: ArgumentList) = {
    val allItems = alist.sneakyGetOptions().getAllItems().toSeq
    allItems.sortBy(_._1).flatMap(ikv => for (kvarg <- ikv._2.toSeq) yield ALElem(ikv._1,
      kvarg._1,kvarg._2))
  }
  /** argument list to json
    *
    * @param alist
    * @return
    */
  implicit def AListToJSON(alist: ArgumentList): JValue = {
    AListToScala(alist).map(kv => Map(("layer" -> kv.layer),("key" -> kv.key),
      ("name"-> kv.arg.getName),
      ( "help" -> kv.arg.getHelpText),
      ("value" -> kv.arg.getValueAsString),
      ("type" -> kv.arg.getType.toString))
    )

  }

  /**
   * produce json text from the jvalue object
   * @param in
   * @return
   */
  def toJS(in: JValue): String =
    pretty(render(in))

  /**
   * Makes a mermaid.js style graph from a given list of nodes
   * @param nodeList the list of nodes formatted as id,name edges (pair to pair)
   * @param includejs if the javascript code should be included as well
   * @param formatting use special formatting in the output
   * @return html and javascript code for the mermaid plot
   */
  def makeGraphFromSeq(nodeList: Seq[((Int,String),(Int,String))],
                       includejs: Boolean=false, formatting: Boolean = false):
  Seq[Node] = {
    def cleanName(name: String) = name.replaceAll("[^a-zA-Z0-9\\s]", "")
    val gnodes = nodeList.map(kv => "id"+kv._1._1+"("+cleanName(kv._1._2)+")"+
      "-->"+"id"+kv._2._1+"("+cleanName(kv._2._2)+")")
    match {
      case fullHist if fullHist.length>0 => Some((Seq("graph TD")++fullHist).mkString("; ")+"; ")
      case _ => None
    }
    gnodes match {
      case Some(graphStr) =>
        var formatStr = "\n classDef default fill:#f9f,stroke:#333,stroke-width:4px;\n"
        if(formatting) {
          formatStr+="classDef fancy fill:#f9f,stroke:#333,stroke-width:4px;" +
            " class "+
            nodeList.flatMap(kv => Seq("id"+kv._1._1,"id"+kv._2._1)).distinct.mkString(",")+
            " fancy;\n"
        }
        <div class="well mermaid">
          {graphStr+formatStr}
        </div> ++ (if (includejs) {
          <script src={UIUtils.prependBaseUri("/tstatic/mermaid.min.js")}></script>
        } else {
          Seq[Node]()
        })
      case None =>
        Seq[Node]()
    }

  }



}

