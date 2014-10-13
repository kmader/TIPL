package tipl.ij.scripting

import ij.IJ
import tipl.formats.TImgRO
import tipl.util.{TImgTools, TImgBlock}



/**
  * Tools for making previewing and exploring data in FIJI from Spark easy
  * @author mader
  *
  */
        object scOps {
             implicit class ijConvertableBlock[T](inBlock: TImgBlock[T]) {
               def asTImg() = {
                 new TImgBlock.TImgBlockAsTImg(inBlock)
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
             /**
              * Read the image as a TImg file
              */
             def OpenImg(path: String): TImgRO = TImgTools.ReadTImg(path)
             def GetCurrentImage() = {
               val curImage = IJ.getImage()
               tipl.ij.ImageStackToTImg.FromImagePlus(curImage)
             }

         }
