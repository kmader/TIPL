package tests

import javax.servlet.http.HttpServletRequest

import org.apache.spark.ui.tipl.HTMLUtils.CodeWithHeader
import org.apache.spark.ui.tipl.WebViz.{RDDInfo, ExtInfo, VizTool}
import org.json4s.JValue
import org.scalatest.FunSuite

import scala.reflect.ClassTag


/**
 * See how well the VizTool model of WebViz can handle identifying generic types in scala
 * Created by mader on 1/23/15.
 */
class ScalaGenericTests extends FunSuite {
  val it = new IntIntTool()

    test(it.toString+" Support") {
      assert(!it.supports(5),"Doesnt Supports int")
      assert(it.supports((5,5)),"Supports (int,int)")
      assert(!it.supports(6.0),"Doesnt support double")
      assert(!it.supports("7_"),"Doesnt support string")
    }

    test(it.toString+" RealSupport") {
      assert(!it.reallySupports(5),"Doesnt supports int")
      assert(it.reallySupports((5,5)),"Supports int,int")
      assert(!it.reallySupports(6.0),"Doesnt support double")
      assert(!it.reallySupports("7_"),"Doesnt support string")
    }

  val st = new StringTool()
  test(st.toString+" Support") {
    assert(!st.supports(5.asInstanceOf[Int]),"Doesnt support int")
    assert(!st.supports(6.0),"Doesnt support double")
    assert(st.supports("7_"),"Supports string")
  }

  test(st.toString+" RealSupport") {
    assert(!st.reallySupports(5.asInstanceOf[Int]),"Doesnt support int")
    assert(!st.reallySupports(6.0),"Doesnt support double")
    assert(st.reallySupports("7_"),"Supports string")
  }
}
class IntIntTool(implicit val et: ClassTag[(Int,Int)]) extends VizTool {
  override type elementType = (Int,Int)
  override def supportKeys(): Boolean = true


  override def typedJsonRender(ele: elementType, info: ExtInfo, ri: RDDInfo, request:
  HttpServletRequest): JValue = ???

  override def typedSampleRender(ele: elementType, info: ExtInfo, ri: RDDInfo, request:
  HttpServletRequest): CodeWithHeader = ???

  override def typedDetailRender(ele: elementType, info: ExtInfo, ri: RDDInfo, request:
  HttpServletRequest): CodeWithHeader = ???

  override def typedSingleRender(ele: elementType, info: ExtInfo, ri: RDDInfo, request:
  HttpServletRequest): CodeWithHeader = ???
}

class StringTool(implicit val et: ClassTag[String]) extends VizTool {
  override type elementType = String

  override def supportKeys(): Boolean = true

  override def typedJsonRender(ele: elementType, info: ExtInfo, ri: RDDInfo, request:
  HttpServletRequest): JValue = ???

  override def typedSampleRender(ele: elementType, info: ExtInfo, ri: RDDInfo, request:
  HttpServletRequest): CodeWithHeader = ???

  override def typedDetailRender(ele: elementType, info: ExtInfo, ri: RDDInfo, request:
  HttpServletRequest): CodeWithHeader = ???

  override def typedSingleRender(ele: elementType, info: ExtInfo, ri: RDDInfo, request:
  HttpServletRequest): CodeWithHeader = ???
}

