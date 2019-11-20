package fourquant.tiles

import java.awt.{RenderingHints, Graphics2D}
import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.imageio.ImageIO

import fourquant.arrays.ArrayPosition
import fourquant.io.{ImageIOOps, BufferedImageOps}
import fourquant.io.BufferedImageOps.ArrayImageMapping
import org.apache.spark.rdd.RDD
import scala.collection.JavaConversions._

import scala.reflect.ClassTag

/**
 * Created by mader on 4/16/15.
 */
object Previews extends Serializable {
    object implicits extends Serializable {
      implicit class previewTiles[A: ArrayPosition,
      @specialized(Double, Char, Boolean) B : ArrayImageMapping](
                                                                  rdd: RDD[(A,Array[Array[B]])])(
                                                                  implicit ct: ClassTag[A],
                                                                  btt: ClassTag[Array[Array[B]]]
                                                                  ) extends Serializable {
        def tilePreview(scale: Double, outWidth: Int, outHeight: Int) = {
          val pngReader = ImageIO.getImageReadersByFormatName("png").toList.head

          val outImg: BufferedImage = new BufferedImage(outWidth, outHeight,
            BufferedImage.TYPE_BYTE_INDEXED)
          val g: Graphics2D = outImg.createGraphics
          g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC)

          val sp = rdd.bytePreview(scale).map{
            case (pos,data) =>
              val newPos = implicitly[ArrayPosition[A]].getPos(pos).map(i =>
                (i.toDouble*scale).toInt)
              (newPos,data)
          }.collect().foreach{
            case (pos,data) =>
              pngReader.setInput(ImageIOOps.createStream(new ByteArrayInputStream(data)))
              val bImg = pngReader.read(0)
              g.synchronized { // only write one at a time
                g.drawImage(bImg, pos(0), pos(1),
                  pos(0) + bImg.getWidth,
                  bImg.getHeight,
                  0, 0, bImg.getWidth, bImg.getHeight, null)
              }

          }
          val os = new ByteArrayOutputStream()
          ImageIO.write(outImg, "png", Base64.getEncoder().wrap(os))
          os.toString(StandardCharsets.ISO_8859_1.name())
        }

        def zeppPreview(scale: Double, count: Int) = {
          "%table x\ty\tpreview\n"+rdd.base64Preview(scale).map{
            case (pos,bimg) =>
              implicitly[ArrayPosition[A]].getX(pos)+"\t"+implicitly[ArrayPosition[A]].getY(pos)+
                "\t%html <img alt='"+pos.toString()+"' src='data:image/png;base64,"+bimg+"'/>"

          }.take(count).
            mkString("\n")
        }
      }

      implicit class previewImage[A,
      @specialized(Double, Char, Boolean) B : ArrayImageMapping](
                                                       rdd: RDD[(A,Array[Array[B]])])(
                                                                implicit ct: ClassTag[A],
      btt: ClassTag[Array[Array[B]]]
        ) extends Serializable {
        def makeBufferedImage(cArr: Array[Array[B]],scale: Double) = {
          val bImg = BufferedImageOps.fromArrayToImage(cArr)
          val sWid = (bImg.getWidth()*scale).toInt
          val sHgt = (bImg.getHeight()*scale).toInt
          val outImg: BufferedImage = new BufferedImage(sWid, sHgt, bImg.getType)
          val g: Graphics2D = outImg.createGraphics
          g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC)
          g.drawImage(bImg, 0, 0, sWid, sHgt, 0, 0, bImg.getWidth, bImg.getHeight, null)
          outImg
        }

        def bytePreview(scale: Double) = {
          rdd.mapValues{
            cArr =>
              val outImg = makeBufferedImage(cArr,scale)
              val os = new ByteArrayOutputStream()
              //val b64 = new Base64.OutputStream(os);
              ImageIO.write(outImg, "png", os)
              os.toByteArray
          }
        }

        def base64Preview(scale: Double) = {
          rdd.mapValues{
            cArr =>
              val outImg = makeBufferedImage(cArr,scale)
              val os = new ByteArrayOutputStream()
              //val b64 = new Base64.OutputStream(os);
              ImageIO.write(outImg, "png", Base64.getEncoder().wrap(os))
              os.toString(StandardCharsets.ISO_8859_1.name())
          }
        }
      }
    }
}
