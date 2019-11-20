package tipl.formats

import java.io.IOException

import tipl.formats.TImgRO.ATImgRO
import tipl.formats.TReader.{TSliceFactory, TSliceReader}
import tipl.util.{TImgTools, D3float, D3int, TypedPath}

object AVIDirectoryReader {
  case class AviSliceReader(idim: D3int, ipos: D3int, ielSize: D3float, iimageType: Int) extends
  ATImgRO(idim,ipos,ielSize, iimageType)
  with TSliceReader {

    def getPolyImage(sliceNumber: Int, asType: Int): AnyRef = {
      return null
    }

    def CheckSizes(otherPath: String): Boolean = {
      return false
    }

    @throws(classOf[IOException])
    def checkSliceDim(globalDim: D3int) {
    }

    def getSampleName: String = {
      return null
    }

    @throws(classOf[IOException])
    def polyReadImage(asType: Int): AnyRef = {
      return null
    }

    def setShortScaleFactor(ssf: Float) {
    }
  }

  class AviSliceFactory extends TSliceFactory {
    @throws(classOf[IOException])
    def ReadFile(curfile: TypedPath): TReader.TSliceReader = {
      //TODO Make this correct
      return AVIDirectoryReader.AviSliceReader(D3int.zero,D3int.zero,D3float.one,
        TImgTools.IMAGETYPE_FLOAT)
    }
  }
}
class AVIDirectoryReader(path: TypedPath, filter: TypedPath.PathFilter)
  extends DirectoryReader(path,filter, new AVIDirectoryReader.AviSliceFactory()) {

  def ParseFirstHeader {
  }

  def readerName: String = {
    return null
  }

  def SetupReader(inPath: TypedPath) {
  }
}
