package tipl.spark

import scala.language.experimental.macros
import scala.reflect.macros.Context
import tipl.util.TImgTools

object TypeMacros {
  	object correctlyTypeElement {
  		def apply(objToType: Any, imageType: Int): Any = macro correctlyTypeElementImpl
  		def correctlyTypeElementImpl(c: Context)(objToType: c.Expr[Any] ,imageType: c.Expr[Int]): c.Expr[Any] = {
  			import c.universe._
  			    imageType.tree match {
  				case Literal(Constant(TImgTools.IMAGETYPE_BOOL)) => reify{objToType.splice.asInstanceOf[Boolean]}
  				case Literal(Constant(TImgTools.IMAGETYPE_CHAR)) => reify{objToType.splice.asInstanceOf[Char]}
  				case Literal(Constant(TImgTools.IMAGETYPE_SHORT)) => reify{objToType.splice.asInstanceOf[Short]}
  				case Literal(Constant(TImgTools.IMAGETYPE_INT)) => reify{objToType.splice.asInstanceOf[Int]}
  				case Literal(Constant(TImgTools.IMAGETYPE_LONG)) => reify{objToType.splice.asInstanceOf[Long]}
  				case Literal(Constant(TImgTools.IMAGETYPE_FLOAT)) => reify{objToType.splice.asInstanceOf[Float]}
  				case Literal(Constant(TImgTools.IMAGETYPE_DOUBLE)) => reify{objToType.splice.asInstanceOf[Double]}
  				case _ => reify{null}
  			}
  		}
  	}
  	object correctlyTypeArray {
  	  
  		def apply(objToType: Any, imageType: Int): Any = macro correctlyTypeArrayImpl
  		def correctlyTypeArrayImpl(c: Context)(objToType: c.Expr[Any] ,imageType: c.Expr[Int]): c.Expr[Any] = {
  			import c.universe._
  			    imageType.tree match {
  				case Literal(Constant(TImgTools.IMAGETYPE_BOOL)) => reify{objToType.splice.asInstanceOf[Array[Boolean]]}
  				case Literal(Constant(TImgTools.IMAGETYPE_CHAR)) => reify{objToType.splice.asInstanceOf[Array[Char]]}
  				case Literal(Constant(TImgTools.IMAGETYPE_SHORT)) => reify{objToType.splice.asInstanceOf[Array[Short]]}
  				case Literal(Constant(TImgTools.IMAGETYPE_INT)) => reify{objToType.splice.asInstanceOf[Array[Int]]}
  				case Literal(Constant(TImgTools.IMAGETYPE_LONG)) => reify{objToType.splice.asInstanceOf[Array[Long]]}
  				case Literal(Constant(TImgTools.IMAGETYPE_FLOAT)) => reify{objToType.splice.asInstanceOf[Array[Float]]}
  				case Literal(Constant(TImgTools.IMAGETYPE_DOUBLE)) => reify{objToType.splice.asInstanceOf[Array[Double]]}
  				case _ => reify{null}
  			}
  		}
  	}
}

