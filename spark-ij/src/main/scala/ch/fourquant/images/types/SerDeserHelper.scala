package ch.fourquant.images.types

import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}

import scala.util.Try

/**
  * Classes for simply serializing objects
  * @note stolen from (http://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array)
  * Created by mader on 1/28/16.
  */
object SerDeserHelper extends Serializable {

  def bytify(obj: Any): Try[Array[Byte]] = {
    Try {
      val bos = new ByteArrayOutputStream();
      val out = new ObjectOutputStream(bos)
      out.writeObject(obj)
      out.close()
      val oval = bos.toByteArray()
      bos.close()
      oval
    }
  }

  def debytify(bArr: Array[Byte]): Try[Any] = {
    Try {
      val bis = new ByteArrayInputStream(bArr)
      val in = new ObjectInputStream(bis)
      val o = in.readObject()
      in.close()
      bis.close()
      o
    }
  }

  def typedDebytify[T](bArr: Array[Byte]) =
    Try {
      debytify(bArr).get.asInstanceOf[T]
    }

  def typedSerialize[T](inObj: T) = Try {
    typedDebytify[T](bytify(inObj).get).get
  }


}
