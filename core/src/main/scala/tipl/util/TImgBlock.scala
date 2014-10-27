package tipl.util

import tipl.formats.TImgRO

import scala.{specialized => spec}

/**
 * The TImgBlock class for storing blocks based on a position
 * Created by mader on 10/22/14.
 */
class TImgBlock[@spec(Boolean, Byte, Short, Int, Long, Float, Double) V](dim: D3int,
                                                                         pos: D3int,
                                                                         offset: D3int,
                                                                         var blockData: Array[V])
  extends Serializable {

  // var blockData: Array[V] = null

  def this(dim: D3int,
           pos: D3int,
           offset: D3int) = this(dim, pos, offset, null)

  def get(): Array[V] = blockData

  def getClone: Array[V] = blockData.clone()

  def getAsDouble: Array[Double] = TImgTools.convertArrayDouble(blockData)

  def getPos: D3int = pos

  def getDim: D3int = dim

  def getOffset: D3int = offset

}


object TImgBlock {

  import tipl.formats.TImg

  /**
   * Convert a TImgSlice (a single slice) into a TImg Object since it is easier like that
   * @author mader
   *
   */
  class TImgBlockAsTImg[V](baseBlock: TImgBlock[V], elSize: D3float = new D3float(1)) extends
  TImg.ATImg(baseBlock.getDim, baseBlock.getPos, elSize,
    TImgTools.identifySliceType(baseBlock.get)) {

    def getPolyImage(sliceNumber: Int, asType: Int): AnyRef = {
      assert((sliceNumber == 0))
      return TImgTools.convertArrayType(baseBlock.get, getImageType, asType, getSigned,
        getShortScaleFactor)
    }

    def getSampleName: String = baseBlock.toString

  }


  /**
   * A class for storing a TImgSlice that reads the file
   *
   * V
   * @author mader
   */
  abstract class TImgBlockFuture[V](dim: D3int,
                                    pos: D3int,
                                    offset: D3int) extends TImgBlock[V](pos, dim, offset) {

    lazy val debug: Boolean = TIPLGlobal.getDebug

    var cacheResult: Boolean = true
    var readTimes: Int = 0
    @transient
    protected var sliceData: Array[V] = null
    @transient
    protected var isRead: Boolean = false

    override def get: Array[V] = {
      readTimes += 1
      if (debug) System.out.println("Calling read function:(#" + readTimes + "):" + this)
      if (isRead) {
        sliceData
      }
      else {
        val cSlice = getSliceData
        if (cacheResult) {
          sliceData = cSlice
          isRead = true
        }
        cSlice
      }
    }

    protected def getSliceData: Array[V]

  }


  /**
   * Read from the given slice in the future (send the object across the wire and read it on the
   * other side, good for virtual objects)
   *
   * @param <V>
   * @author mader
   */

  class TImgBlockFromImage[V](inImObj: TImgRO,
                              sliceNumber: Int, imageType: Int)
    extends TImgBlockFuture[V](inImObj.getPos(),
    inImObj.getDim(),
    inImObj.getOffset()) {

    override protected def getSliceData: Array[V] =
      inImObj.getPolyImage(sliceNumber, imageType).asInstanceOf[Array[V]]

    override def toString: String =
      "TBF:sl=" + sliceNumber + ",obj=" + inImObj
  }


  /**
   * Create a new block with an offset given a chunk of data and position, dimensions
   *
   * @param fileName
   * @param sliceNumber
   * @param imgType
   * @param pos         position of the upper left corner of the block
   * @param dim         the dimension of the block
   * @param offset      the offset of the block
   */
  class TImgBlockFile[V](fileName: TypedPath, sliceNumber: Int, imgType: Int, pos: D3int,
                         dim: D3int,
                         offset: D3int) extends TImgBlockFuture[V](pos, dim, offset) {

    protected def getSliceData: Array[V] = {
      readTimes += 100
      if (debug) System.out.println("Reading (#" + readTimes + ") slice:" + this)
      if (!TIPLGlobal.waitForReader) throw new IllegalArgumentException("Process was interupted " +
        "while waiting for reader")
      val outSlice = TImgTools.ReadTImgSlice(fileName, sliceNumber,
        imgType).asInstanceOf[Array[V]]
      TIPLGlobal.returnReader
      return outSlice
    }

    override def toString: String =
      "SLR:sl=" + sliceNumber + ",path=" + fileName
  }


}
