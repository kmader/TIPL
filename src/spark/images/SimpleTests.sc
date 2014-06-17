package spark.images
import tipl.tests._
import tipl.util.TImgTools
import tipl.spark._
import tipl.formats.TImgRO

import java.io.Serializable
import tipl.util.D3int
import tipl.util.TImgTools.HasDimensions
import tipl.util.D3float

trait TImgROLike extends TImgRO {
  val dim: D3int
  val pos: D3int
  val offset: D3int
  val elSize: D3float
  override def getDim = {dim}
  override def getPos = {pos}
  override def getOffset = {offset}
  override def getElSize = {elSize}
}


object SimpleTests {
	
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
  val x: Long = java.lang.Long.valueOf(1)         //> x  : Long = 1
  val testImg = TestPosFunctions.wrapItAs(10,
				new TestPosFunctions.DiagonalPlaneAndDotsFunction(),TImgTools.IMAGETYPE_INT);
                                                  //> testImg  : tipl.formats.TImgRO = tipl.formats.PureFImage@3630f72
	val testF = Float.MaxValue                //> testF  : Float = 3.4028235E38
	var testR = testF+1                       //> testR  : Float = 3.4028235E38
	
  //val jsc = SparkGlobal.getContext("FunnyTests")
  
	//val dtestImg = DTImg.ConvertTImg(jsc,testImg,TImgTools.IMAGETYPE_INT)
  
		
		
		
		
	//val kvCount = kvImg.getBaseImg().count

	//val kvOut = kvImg.getBaseImg().first
}