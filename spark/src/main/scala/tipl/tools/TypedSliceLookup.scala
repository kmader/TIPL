package tipl.tools

import tipl.formats.TImgRO
import tipl.util.TImgTools

/**
 * A strongly typed slice lookup function and the assosciated getPolyImage function built around it
 * Created by mader on 1/27/15.
 */
trait TypedSliceLookup[T] extends TImgRO {
  def getSlice(sliceNum: Int): Option[Array[T]]

  /**
   * A fairly simple operation of filtering the RDD for the correct slice and returning that slice
   */
  override def getPolyImage(sliceNumber: Int, asType: Int): AnyRef = {
    if ((sliceNumber < 0) || (sliceNumber >= getDim.z))
      throw new IllegalArgumentException(getSampleName + ": Slice requested (" + sliceNumber +
        ") exceeds image dimensions " + getDim)

    val zPos: Int = getPos.z + sliceNumber

    val curSlice = getSlice(sliceNumber) match {
      case Some(slice) => slice
      case None =>
        throw new IllegalArgumentException(getSampleName + ", lookup failed (0 found):" +
          sliceNumber + " (z:" + zPos + "), of " + getDim + " of #" + getDim().z + " blocks")
    }

    return TImgTools.convertArrayType(curSlice, getImageType, asType, getSigned,
      getShortScaleFactor)
  }
}