package tipl.spark

import tipl.util.TImgTools._
import scala.reflect.ClassTag
import tipl.formats.TImgRO

object TypeMacros {

  def exCT[V](inObj: V)(implicit lm: ClassTag[V]) = lm

  /**
   * Converts an imagetype integer into the appropriate classtag for DTImg objects
   */
  def asClassTag(imageType: Int) = imageType match {

    case IMAGETYPE_BOOL => exCT(new Array[Boolean](0))
    case IMAGETYPE_CHAR => exCT(new Array[Char](0))
    case IMAGETYPE_SHORT => exCT(new Array[Short](0))
    case IMAGETYPE_INT => exCT(new Array[Int](0))
    case IMAGETYPE_LONG => exCT(new Array[Long](0))
    case IMAGETYPE_FLOAT => exCT(new Array[Float](0))
    case IMAGETYPE_DOUBLE => exCT(new Array[Double](0))
    case _ => throw new IllegalArgumentException("Type Not Found:" + imageType + " " +
      getImageTypeName(imageType))
  }

  def castArr(obj: Any, imageType: Int) = imageType match {
    case IMAGETYPE_BOOL => obj.asInstanceOf[Array[Boolean]]
    case IMAGETYPE_CHAR => obj.asInstanceOf[Array[Char]]
    case IMAGETYPE_SHORT => obj.asInstanceOf[Array[Short]]
    case IMAGETYPE_INT => obj.asInstanceOf[Array[Int]]
    case IMAGETYPE_LONG => obj.asInstanceOf[Array[Long]]
    case IMAGETYPE_FLOAT => obj.asInstanceOf[Array[Float]]
    case IMAGETYPE_DOUBLE => obj.asInstanceOf[Array[Double]]
    case _ => throw new IllegalArgumentException("Type Not Found:" + imageType + " " +
      getImageTypeName(imageType))
  }

  /**
   * converts everything we know about to a double
   */
  def castEleToDouble(inVal: Any, imType: Int): Double = imType match {
    case IMAGETYPE_BOOL => if (inVal.asInstanceOf[Boolean]) 1.0 else 0.0
    case IMAGETYPE_CHAR => inVal.asInstanceOf[Byte].doubleValue()
    case IMAGETYPE_SHORT => inVal.asInstanceOf[Short].doubleValue()
    case IMAGETYPE_INT => inVal.asInstanceOf[Int].doubleValue()
    case IMAGETYPE_LONG => inVal.asInstanceOf[Long].doubleValue()
    case IMAGETYPE_FLOAT => inVal.asInstanceOf[Float].doubleValue()
    case IMAGETYPE_DOUBLE => inVal.asInstanceOf[Double]
    case _ => throw new IllegalArgumentException(imType + " is not a known image type")
  }

  def castArrToDouble(inVal: Any, imType: Int): Array[Double] = imType match {
    case IMAGETYPE_BOOL => inVal.asInstanceOf[Array[Boolean]].map { bval => if (bval) 1.0 else 0.0}
    case IMAGETYPE_CHAR => inVal.asInstanceOf[Array[Byte]].map {
      _.doubleValue()
    }
    case IMAGETYPE_SHORT => inVal.asInstanceOf[Array[Short]].map {
      _.doubleValue()
    }
    case IMAGETYPE_INT => inVal.asInstanceOf[Array[Int]].map {
      _.doubleValue()
    }
    case IMAGETYPE_LONG => inVal.asInstanceOf[Array[Long]].map {
      _.doubleValue()
    }
    case IMAGETYPE_FLOAT => inVal.asInstanceOf[Array[Float]].map {
      _.doubleValue()
    }
    case IMAGETYPE_DOUBLE => inVal.asInstanceOf[Array[Double]]
    case _ => throw new IllegalArgumentException(imType + " is not a known image type")
  }

  def fromDouble(inVal: Double, outType: Int) = outType match {
    case IMAGETYPE_BOOL => inVal > 0
    case IMAGETYPE_CHAR => inVal.byteValue
    case IMAGETYPE_SHORT => inVal.shortValue
    case IMAGETYPE_INT => inVal.intValue
    case IMAGETYPE_LONG => inVal.longValue
    case IMAGETYPE_FLOAT => inVal.floatValue
    case IMAGETYPE_DOUBLE => inVal
    case _ => throw new IllegalArgumentException(outType + " is not a known image type")
  }

  def fromDouble(inVal: Array[Double], outType: Int): Array[_] = outType match {
    case IMAGETYPE_BOOL => inVal.map {
      _ > 0
    }
    case IMAGETYPE_CHAR => inVal.map {
      _.byteValue
    }
    case IMAGETYPE_SHORT => inVal.map {
      _.shortValue
    }
    case IMAGETYPE_INT => inVal.map {
      _.intValue
    }
    case IMAGETYPE_LONG => inVal.map {
      _.longValue
    }
    case IMAGETYPE_FLOAT => inVal.map {
      _.floatValue
    }
    case IMAGETYPE_DOUBLE => inVal
    case _ => throw new IllegalArgumentException(outType + " is not a known image type")
  }


  implicit class RichTImgRO[T <: TImgRO](cImg: T)(implicit lm: ClassTag[T]) {
    def toKV(): KVImg[_] = {
      val ity = cImg.getImageType
      cImg match {
        case m: KVImg[_]  => m
        case m: DTImg[_] if ity == IMAGETYPE_BOOL =>
          KVImg.fromDTImg[Array[Boolean], Boolean](m.asInstanceOf[DTImg[Array[Boolean]]])
        case m: DTImg[_] if ity == IMAGETYPE_CHAR =>
          KVImg.fromDTImg[Array[Char], Char](m.asInstanceOf[DTImg[Array[Char]]])
        case m: DTImg[_] if ity == IMAGETYPE_SHORT =>
          KVImg.fromDTImg[Array[Short], Short](m.asInstanceOf[DTImg[Array[Short]]])
        case m: DTImg[_] if ity == IMAGETYPE_INT =>
          KVImg.fromDTImg[Array[Int], Int](m.asInstanceOf[DTImg[Array[Int]]])
        case m: DTImg[_] if ity == IMAGETYPE_LONG =>
          KVImg.fromDTImg[Array[Long], Long](m.asInstanceOf[DTImg[Array[Long]]])
        case m: DTImg[_] if ity == IMAGETYPE_FLOAT =>
          KVImg.fromDTImg[Array[Float], Float](m.asInstanceOf[DTImg[Array[Float]]])
        case m: DTImg[_] if ity == IMAGETYPE_DOUBLE =>
          KVImg.fromDTImg[Array[Double], Double](m.asInstanceOf[DTImg[Array[Double]]])
        case m: TImgRO => KVImg.ConvertTImg(SparkGlobal.getContext(), m, IMAGETYPE_INT)
      }
    }

    def toDT(): DTImg[_] = {
      val ity = cImg.getImageType
      cImg match {
        case m: DTImg[_] => m
        case m: KVImg[_] if ity == IMAGETYPE_BOOL =>
          DTImgOps.fromKVImg[Boolean](m.asInstanceOf[KVImg[Boolean]])
        case m: KVImg[_] if ity == IMAGETYPE_CHAR =>
          DTImgOps.fromKVImg[Char](m.asInstanceOf[KVImg[Char]])
        case m: KVImg[_] if ity == IMAGETYPE_SHORT =>
          DTImgOps.fromKVImg[Short](m.asInstanceOf[KVImg[Short]])
        case m: KVImg[_] if ity == IMAGETYPE_INT =>
          DTImgOps.fromKVImg[Int](m.asInstanceOf[KVImg[Int]])
        case m: KVImg[_] if ity == IMAGETYPE_LONG =>
          DTImgOps.fromKVImg[Long](m.asInstanceOf[KVImg[Long]])
        case m: KVImg[_] if ity == IMAGETYPE_FLOAT =>
          DTImgOps.fromKVImg[Float](m.asInstanceOf[KVImg[Float]])
        case m: KVImg[_] if ity == IMAGETYPE_DOUBLE =>
          DTImgOps.fromKVImg[Double](m.asInstanceOf[KVImg[Double]])
        case m: TImgRO => DTImg.ConvertTImg(SparkGlobal.getContext(), m, IMAGETYPE_INT)
      }
    }

    def toDTLabels(): DTImg[Array[Long]] =
      DTImg.ConvertTImg(SparkGlobal.getContext, cImg, IMAGETYPE_LONG)

    def toDTValues(): DTImg[Array[Double]] =
      DTImg.ConvertTImg(SparkGlobal.getContext, cImg, IMAGETYPE_DOUBLE)

    def toDTBinary(): DTImg[Array[Boolean]] =
      DTImg.ConvertTImg(SparkGlobal.getContext, cImg, IMAGETYPE_BOOL)
  }


  /**
   * makeImgBlock
   * @param size is the length of the array
   * @param outType is the type of the output image
   */
  def makeImgBlock(size: Int, outType: Int) = {
    assert(isValidType(outType))
    outType match {
      case IMAGETYPE_BOOL => new Array[Boolean](size)
      case IMAGETYPE_CHAR => new Array[Char](size)
      case IMAGETYPE_SHORT => new Array[Short](size)
      case IMAGETYPE_INT => new Array[Int](size)
      case IMAGETYPE_LONG => new Array[Long](size)
      case IMAGETYPE_FLOAT => new Array[Float](size)
      case IMAGETYPE_DOUBLE => new Array[Double](size)
      case _ => throw new IllegalArgumentException(outType + " is not a known image type")
    }
  }

  /**
   * A setter method for arrays of the imagetype
   */
  def arraySetter(arr1: Any, arr1idx: Int, arr2: Any, arr2idx: Int, arrType: Int): Unit = {
    assert(isValidType(arrType))
    arrType match {
      case IMAGETYPE_BOOL =>
        arr1.asInstanceOf[Array[Boolean]](arr1idx) = arr2.asInstanceOf[Array[Boolean]](arr2idx)
      case IMAGETYPE_CHAR =>
        arr1.asInstanceOf[Array[Char]](arr1idx) = arr2.asInstanceOf[Array[Char]](arr2idx)
      case IMAGETYPE_SHORT =>
        arr1.asInstanceOf[Array[Short]](arr1idx) = arr2.asInstanceOf[Array[Short]](arr2idx)
      case IMAGETYPE_INT =>
        arr1.asInstanceOf[Array[Int]](arr1idx) = arr2.asInstanceOf[Array[Int]](arr2idx)
      case IMAGETYPE_LONG =>
        arr1.asInstanceOf[Array[Long]](arr1idx) = arr2.asInstanceOf[Array[Long]](arr2idx)
      case IMAGETYPE_FLOAT =>
        arr1.asInstanceOf[Array[Float]](arr1idx) = arr2.asInstanceOf[Array[Float]](arr2idx)
      case IMAGETYPE_DOUBLE =>
        arr1.asInstanceOf[Array[Double]](arr1idx) = arr2.asInstanceOf[Array[Double]](arr2idx)
      case _ => throw new IllegalArgumentException(arrType + " is not a known image type")
    }
  }

}

