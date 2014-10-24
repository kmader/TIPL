package tipl.ij.scripting

import ij.{ImageStack, IJ}
import org.apache.spark.rdd.RDD
import tipl.formats.TImgRO
import tipl.util.TImgSlice.TImgSliceAsTImg
import tipl.util.{D3int, TImgTools, TImgSlice}


class rddImage extends ImageStack {

}

/**
  * Tools for making previewing and exploring data in FIJI from Spark easy
  * @author mader
  *
  */
        object scOps {
             implicit class ijConvertableBlock[T](inBlock: TImgSlice[T]) {
               def asTImg() = {
                 new TImgSliceAsTImg(inBlock)
               }
             }
             implicit class ijConvertibleImage(inImg: TImgRO) {
               lazy val is = tipl.ij.TImgToImageStack.MakeImageStack(inImg)

               def show() = {
                 val ip = tipl.ij.TImgToImagePlus.MakeImagePlus(inImg)
                 ip.show(inImg.getPath().getPath())
               }

               def show3D() = {
                 val vvPlug = new tipl.ij.volviewer.Volume_Viewer
                 vvPlug.LoadImages(Array[TImgRO](inImg))
                 vvPlug.run("")
                 vvPlug.waitForClose()
               }

             }

             implicit class rddImage[V](inImg: RDD[(D3int,TImgSlice[V])]) {
               def show() = {

               }
             }


             /**
              * Read the image as a TImg file
              */
             def OpenImg(path: String): TImgRO = TImgTools.ReadTImg(path)
             def GetCurrentImage() = {
               val curImage = IJ.getImage()
               tipl.ij.ImageStackToTImg.FromImagePlus(curImage)
             }

         }
