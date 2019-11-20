package fourquant.io

import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io._
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.stream.ImageInputStream
import javax.imageio.{ImageIO, ImageReader}

import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi
import fourquant.io.BufferedImageOps._
import fourquant.tiles.TilingStrategy2D
import io.scif.filters.ReaderFilter
import org.apache.spark.SparkContext
import org.apache.spark.input.PortableDataStream

import scala.collection.JavaConversions.asScalaIterator
import scala.collection.mutable

/**
 * A general set of opertions for importing images
 * Created by mader on 2/27/15.
 */
object ImageIOOps extends Serializable {
  val verbose = false
  val loadTiffies=false
  /**
   * object to limit access to imageio
   */
  val imIOAccess = new AtomicInteger(0)


  case class ImageInfo(count: Int, height: Int, width: Int, info: String)


  /**
   * For creating an input stream from the PortableDataStream
   * @param input
   * @return
   */
  def createStream(input: InputStream) = ImageIO.createImageInputStream(input)

  /**
   * Create an input stream from a bytearray (byteswritable)
   * @param bArray
   * @return
   */
  def createStream(bArray: Array[Byte]) = ImageIO.createImageInputStream(
    new ByteArrayInputStream(bArray)
  )

  private def getAllReaders(stream: ImageInputStream,suffix: Option[String] = None) = {
    ImageIO.scanForPlugins()

    val sufReader = (for(sf<-suffix.toList;
                         foundReader <- ImageIO.getImageReadersBySuffix(sf).toList)
      yield foundReader)
    val streamReader = ImageIO.getImageReaders(stream).toList
    (sufReader,streamReader)
  }

  def getReaderList(stream: ImageInputStream,suffix: Option[String] = None) = {
    val (a,b) = getAllReaders(stream,suffix)
    a ++ b
  }

  /**
  def getGeoTifReader(stream: ImageInputStream): Option[ImageReader] = {
    val reader = new RawTiffImageReader.Spi().createReaderInstance()
    try {
      reader.setInput(stream)
      Some(reader)
    } catch {
      case e: Throwable =>
        val stmMsg = "Reader is: "+reader+
          "and stream Input was:"+stream+" and had class:"+stream.getClass()+
          " and was IIS "+ stream.isInstanceOf[ImageInputStream]
        val outMsg = "Stream cannot be read :"+stmMsg+", "+e.getMessage
        System.err.println(outMsg)
        None
    }
  }
    **/

  def getTifReader(stream: ImageInputStream): Option[ImageReader] = {
    val reader = new TIFFImageReaderSpi().createReaderInstance()
    try {
      reader.setInput(stream)
      Some(reader)
    } catch {
      case e: Throwable =>
        val stmMsg = "Reader is: "+reader+
          "and stream Input was:"+stream+" and had class:"+stream.getClass()+
          " and was IIS "+ stream.isInstanceOf[ImageInputStream]
        val outMsg = "Stream cannot be read :"+stmMsg+", "+e.getMessage
        System.err.println(outMsg)
        None
    }
  }


  def getReader(stream: ImageInputStream, suffix: Option[String] = None,
                tempDir: Option[String] = None) = {
    // reset the stream to beginning (if possible)
    stream.seek(0)
    // set the cache if needed
    val tempOrIoDir = (tempDir,System.getProperty("java.io.tmpdir")) match {
        case(Some(prefDir),_) => Some(prefDir)
        case (_,tmpDir)=> Some(tmpDir)
        case _ => None
    }
    tempOrIoDir match {
      case Some(validDirName) if validDirName.length()>0  =>
        ImageIO.setCacheDirectory(new File(validDirName))
    }

    suffix match {
        // hard-code some cases since this can be very time consuming and has concurrency issues
      case Some(tifEnding) if tifEnding.toUpperCase().contains("TIF") =>
        getTifReader(stream)
      case _ =>

        val (sufReader, streamReader) = imIOAccess.synchronized {
          getAllReaders (stream, suffix)
        }

        if (verbose) println ("\tTotal Readers found:" + sufReader.length + ", " + streamReader.length)
        val bestReader = (sufReader.headOption, streamReader.headOption) match {
          case (Some (reader), _) => Some (reader) // prefer the suffix based reader
          case (None, Some (reader) ) => Some (reader)
          case (None, None) => None
        }
        bestReader.map (reader => {
          try {
            reader.setInput (stream)
          } catch {
            case e: Throwable =>
              val stmMsg = "Reader is: " + reader +
                "and stream Input was:" + stream + " and had class:" + stream.getClass () +
                " and was IIS " + stream.isInstanceOf[ImageInputStream]
              println (stmMsg)

              throw new RuntimeException ("Stream cannot be read :" + stmMsg + ", " + e.getMessage)
          }

          reader
        })
    }
  }

  //TODO implement this class and replace the tempDir option with a real class so it can be made
  // more complicated in the future
  case class ImageIOSetup(tempDir: Option[String]) {
    def setupPartition() = {
      ImageIO.scanForPlugins()
      val tempOrIoDir = (tempDir,System.getProperty("java.io.tmpdir")) match {
        case(Some(prefDir),_) => Some(prefDir)
        case (_,tmpDir)=> Some(tmpDir)
        case _ => None
      }
      tempOrIoDir match {
        case Some(validDirName) if validDirName.length()>0  =>
          ImageIO.setCacheDirectory(new File(validDirName))
      }

    }
  }

  def getImageInfo(stream: ImageInputStream, suffix: Option[String], tempDir: Option[String] = None)
  = {
    getReader(stream,suffix,tempDir) match {
      case Some(reader) =>
        ImageInfo(reader.getNumImages(true),reader.getHeight(0),reader.getWidth(0),
          reader.getFormatName)
      case None =>
        System.err.println("Empty Image Info, could not be read:"+stream)
        ImageInfo(0,-1,-1,"")
    }
  }

  def readTile(infile: File, x: Int, y: Int, w: Int, h: Int, tempDir: Option[String]):
  Option[BufferedImage] =
    readTile(createStream(new FileInputStream(infile)),
      infile.getName().split("[.]").reverse.headOption,
      x,y,w,h,tempDir)

  /**
   * THe base function for reading images in as tiles
   * @param stream
   * @param suffix
   * @param x
   * @param y
   * @param w
   * @param h
   * @param tempDir
   * @return
   */
  private[io] def readTile(stream: ImageInputStream, suffix: Option[String],
                           x: Int, y: Int, w: Int, h: Int, tempDir: Option[String]):
  Option[BufferedImage] = {
    val sourceRegion = new Rectangle(x, y, w, h) // The region you want to extract

    getReader(stream,suffix) match {
      case Some(reader) =>
        val param = reader.getDefaultReadParam()
        param.setSourceRegion(sourceRegion); // Set region
        try {
          Some(reader.read(0, param))
        } catch {
          case  e : Throwable =>
            e.printStackTrace()
            println("Image cannot be loaded, or likely is out of bounds, returning none")
            None
        }
      case None => None
    }
  }

  def readWholeImage(infile: File, tempDir: Option[String]):
  Option[BufferedImage] =
    readWholeImage(createStream(new FileInputStream(infile)),
      infile.getName().split("[.]").reverse.headOption,
      tempDir)

  /**
   * Read the image at once
   * @param stream
   * @param suffix
   * @param tempDir
   * @return
   */
  private[io] def readWholeImage(stream: ImageInputStream, suffix: Option[String],
                                 tempDir: Option[String]):
  Option[BufferedImage] = {

    getReader(stream,suffix) match {
      case Some(reader) =>
        try {
          Some(reader.read(0))
        } catch {
          case  e : Throwable =>
            e.printStackTrace()
            println("Image cannot be loaded, or likely is out of bounds, returning none")
            None
        }
      case None => None
    }
  }



  def readTileArray[T: ArrayImageMapping](stream: ImageInputStream,suffix: Option[String],
                                          x: Int, y: Int, w: Int, h: Int,
                                           tempDir: Option[String]):
  Option[Array[Array[T]]] = {
    readTile(stream,suffix,x,y,w,h,tempDir).map(_.as2DArray[T])
  }

  private[io] def readTileDouble(stream: ImageInputStream,suffix: Option[String],
                                 x: Int, y: Int, w: Int, h: Int, tempDir: Option[String]):
  Option[Array[Array[Double]]] = {
    import fourquant.io.BufferedImageOps.implicits.directDoubleImageSupport
    readTile(stream,suffix,x,y,w,h,tempDir).map(_.as2DArray[Double])
  }



  def readImageAsTiles[T: ArrayImageMapping](stream: ImageInputStream,suffix: Option[String],
                                             tileWidth: Int, tileHeight: Int)(
                                              implicit ts: TilingStrategy2D) = {

    val info = getImageInfo(stream,suffix)
    ts.createTiles2D(info.width,info.height,tileWidth,tileHeight).flatMap {
      case (x, y, width, height) =>
        for(cTile<-readTileArray[T](stream,suffix,x,y,width,height,None))
          yield ((x,y),cTile)
    }
  }


  def readWholeImageArray[T: ArrayImageMapping](stream: ImageInputStream,suffix: Option[String],
                                          tempDir: Option[String]):
  Option[Array[Array[T]]] = {
    readWholeImage(stream,suffix,tempDir).map(_.as2DArray[T])
  }

  import fourquant.io.BufferedImageOps.implicits.directDoubleImageSupport
  def readWholeImageArrayDouble(stream: ImageInputStream, suffix: Option[String],
                                tempDir: Option[String]) =
    readWholeImageArray[Double](stream, suffix, tempDir)


  /**
   * Keep all the scifio related tools together
   */
  object ScifioUtilFun extends Serializable{
    def calculateTiles(path: String, tileWidth: Int, tileHeight: Int )(
      implicit ts: TilingStrategy2D
      ) = {
      val (creader,meta) = ScifioOps.readPath(path)
      val imgMeta = meta.get(0)
      val axLen = imgMeta.getAxesLengths()
      val width = axLen(0).toInt
      val height = axLen(1).toInt
      ts.createTiles2D(width,height,tileWidth,tileHeight)
    }
    def readImageAsTiles(path: String, tileWidth: Int, tileHeight: Int)(
      implicit ts: TilingStrategy2D) = {
      val (creader,meta) = ScifioOps.readPath(path)
      calculateTiles(path,tileWidth,tileHeight).map {
        case (x, y, width, height) =>
          ((x,y),creader.openPlane(0,0,Array[Long](x,y),Array[Long](width,height)))
      }
    }
  }


  implicit class scifioSC(sc: SparkContext) extends Serializable {
    import fourquant.utils.IOUtils.LocalPortableDataStream
    def scifioTileRead(path: String, tileWidth: Int, tileHeight: Int, partCount: Int = 100)(
      implicit ts: TilingStrategy2D) = {
      sc.binaryFiles(path).flatMapValues{
        inPDS =>
          val cPath = inPDS.makeLocal(inPDS.getPath().split("[.]").reverse.head)
          for(cTile<-ScifioUtilFun.calculateTiles(cPath,tileWidth,tileHeight)) yield (inPDS,cTile)
      }.repartition(partCount).mapPartitions {
        cPart =>
          // in case there are multiple paths in the given path
          val localPaths = collection.mutable.Map[String,String]()
          val readers = collection.mutable.Map[String,ReaderFilter]()
          for ((cKey, (inPDS, (x, y, width, height)))<-cPart;
               remotePath = inPDS.getPath;
               cPath = localPaths.getOrElseUpdate(remotePath,
                 inPDS.makeLocal(remotePath.split("[.]").reverse.head));
               creader = readers.getOrElseUpdate(remotePath,ScifioOps.readPath(cPath)._1);
               curPlane = creader.openPlane(0, 0, Array[Long](x, y), Array[Long](width, height))
          )
            yield ((cKey, x, y), (curPlane.getLengths, curPlane.getBytes))

      }

    }

  }

  implicit class iioSC(sc: SparkContext) extends Serializable {
    import fourquant.io.BufferedImageOps.implicits.directDoubleImageSupport
    import fourquant.utils.IOUtils.LocalPortableDataStream

    /**
     * Load the images as a series of 2D arrays
     * @param path hadoop-style path to the image files (can contain wildcards)
     * @param partitionCount number of partitions (cores * 2-4)
     * @tparam T (the type of the output image)
     * @return an RDD with a key of the image names, and tile coordinates, and a value of the data
     *         as a 2D array typed T
     */
    def readWholeImages[T : ArrayImageMapping](path: String, partitionCount: Int) = {
      val tempDir = sc.getConf.getOption("spark.local.dir")
      sc.binaryFiles(path).mapValues{
        case pds: PortableDataStream =>
          val cachedPDS = pds.cache()
          val imInfo = getImageInfo(createStream(cachedPDS.getUseful()),pds.getSuffix(),tempDir)
          (cachedPDS,imInfo)
      }.repartition(partitionCount).mapPartitions{
        inPart =>
          for (cChunk <- inPart;
               curPath = cChunk._1;
               suffix =  curPath.split("[.]").reverse.headOption;
               curInput = cChunk._2._1.getUseful();
               emptyVal = curInput.reset();
               curStream = createStream(curInput);
               curImageData <- readWholeImageArray[T](curStream,suffix,tempDir)
          )
            yield (curPath,curImageData)
      }
    }

    def readWholeImagesDouble(path: String,pc: Int) = readWholeImages[Double](path,pc)


    /** hadoop things **/
    import org.apache.hadoop.io._;
    /**
     * Read an sequence file as a series of image
     * @param path
     * @param partitionCount
     * @tparam T
     * @return
     */
    def readImageSequence[T : ArrayImageMapping](path: String, partitionCount: Int) = {
      val tempDir = sc.getConf.getOption("spark.local.dir")
      sc.sequenceFile(path,classOf[Text],classOf[BytesWritable]).map{
        case (pth: Text,bw: BytesWritable) =>
          val byteData = bw.copyBytes()
          val imInfo = getImageInfo(createStream(byteData),
            pth.toString.split(".").reverse.headOption,tempDir)
          (pth.toString(),(byteData,imInfo))
      }.repartition(partitionCount).mapPartitions{
        inPart =>
          for (cChunk <- inPart;
               curPath = cChunk._1;
               suffix =  curPath.split("[.]").reverse.headOption;
               curStream = createStream(cChunk._2._1);
               curImageData <- readWholeImageArray[T](curStream,suffix,tempDir)
          )
            yield (curPath,curImageData)
      }
    }

    def readImageSequenceDouble(path: String,pc: Int) = readImageSequence[Double](path,pc)

    /**
     * Load the image(s) as a series of 2D tiles
     * @param path hadoop-style path to the image files (can contain wildcards)
     * @param tileWidth
     * @param tileHeight
     * @param partitionCount number of partitions (cores * 2-4)
     * @tparam T (the type of the output image)
     * @return an RDD with a key of the image names, and tile coordinates, and a value of the data
     *         as a 2D array typed T
     */
    def readTiledImage[T : ArrayImageMapping](path: String, tileWidth: Int, tileHeight: Int,
                                              partitionCount: Int
                                               )(implicit ts: TilingStrategy2D) = {
      val tempDir = sc.getConf.getOption("spark.local.dir")
      sc.binaryFiles(path).mapValues{
        case pds: PortableDataStream =>
          val cachedPDS = pds.cache()
          val imInfo = getImageInfo(createStream(cachedPDS.getUseful()),pds.getSuffix(),tempDir)
          (cachedPDS,imInfo)
      }.flatMapValues{
        case (pds, info) =>
          for(cTile <- ts.createTiles2D(info.width,info.height,tileWidth,tileHeight))
            yield (pds,cTile)
      }.repartition(partitionCount).mapPartitions{
        inPart =>
          // reuise the open portabledatastreams to avoid reopening and copying the file
          var streamLog = new mutable.HashMap[String,InputStream]()

          for (cTileChunk <- inPart;
               curPath = cTileChunk._1;
               suffix =  curPath.split("[.]").reverse.headOption;
               curInput = streamLog.getOrElseUpdate(curPath,cTileChunk._2._1.getUseful());
               /** for now read the tile every time
                curStream = createStream(cTileChunk._2._1.open());
                 **/
               emptyVal = curInput.reset();
               curStream = createStream(curInput);
               sx = cTileChunk._2._2._1;
               sy = cTileChunk._2._2._2;
               curTile <- readTileArray[T](curStream,suffix,sx,sy, tileWidth,tileHeight,tempDir)
          )
            yield ((curPath,sx,sy),curTile)
      }
    }

    def readTiledDoubleImage(path: String, tileWidth: Int, tileHeight: Int,
                             partitionCount: Int)(implicit ts: TilingStrategy2D) =
      readTiledImage[Double](path,tileWidth,tileHeight,partitionCount)
  }

}
