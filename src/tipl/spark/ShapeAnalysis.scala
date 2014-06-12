/**
 *
 */
package tipl.spark

import tipl.formats.TImgRO
import tipl.formats.TImg
import tipl.util.TImgTools
import tipl.tools.BaseTIPLPluginIn

/**
 * A scala class to perform shape analysis similarly to the code provided GrayAnalysis
 * @author mader
 *
 */
object ShapeAnalysis extends BaseTIPLPluginIn {
	override def getPluginName() = {  "ShapeAnalysis:Spark" }
	
	override def execute():Boolean = { 
	  print("Running...");
	  true
	 }
	
	override def LoadImages(inImages: Array[TImgRO]) = {
	  
	}
	
	
	def testFun(): Int = {
	  return 5;
	}
	
	 def main(args: Array[String]): Unit = {
        print("Hello"+testFun+" num of args:"+args.length)
        execute
    }
}