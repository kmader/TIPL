package spark.images

import scala.language.experimental.macros
import scala.reflect.macros.Context
import tipl.util.TImgTools._


object MacroTests {
	object fromName {
  def apply(imageType: Int): Any = macro fromNameImpl
  
  def fromNameImpl(c: Context)(imageType: c.Expr[Int]): c.Expr[Any] = {
    	import c.universe._
    	c.Expr(imageType.tree match {
      case Literal(Constant(IMAGETYPE_BOOL)) => Literal(Constant(false))
      case Literal(Constant(IMAGETYPE_DOUBLE)) => Literal(Constant(0.0))
      case _ => Literal(Constant(()))
    })
  }
}


//val fv = fromName(IMAGETYPE_BOOL)
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
  
}