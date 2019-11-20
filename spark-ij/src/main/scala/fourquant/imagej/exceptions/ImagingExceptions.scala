package fourquant.imagej.exceptions


trait GeneralImagingException extends Serializable {
  val image_information: String
  val plugin_information: String
  val error_information: String
}
/**
  * The basic superclass of all imaging related exceptions
  * Created by mader on 5/9/16.
  */
abstract class BaseImagingException
  extends Exception with GeneralImagingException {

}



class SegmentationException(val image_information: String, val plugin_information: String,
                            val error_information: String) extends GeneralImagingException {

}




