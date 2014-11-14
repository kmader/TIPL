package spark



/**
 * Created by mader on 10/16/14.
 */
object RaberAnalysis {
  def main(args: Array[String]): Unit = {

    import org.apache.spark.SparkContext._
    import tipl.formats.TImgRO
    import tipl.spark.IOOps._
    import tipl.spark.SparkGlobal
    import tipl.util.TIPLOps._
    import tipl.util.{D3int, TImgSlice, TImgTools, TypedPath}
    val p = SparkGlobal.activeParser(args)

    val sc = SparkGlobal.getContext("RaberAnalysis").sc
    val useAll = false
    val imgPath = if (useAll) {
      "/Volumes/WORKDISK/WorkData/Raber/bci102014/brain*/*.tif"
    } else {
      "/Volumes/WORKDISK/WorkData/Raber/bci102014/brain*23/a*.tif"
    }
    val savePath = "/Volumes/WORKDISK/WorkData/Raber/bci102014/results/"
    val PERSIST_LEVEL = org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK

    val basePath = TypedPath.localFile(new java.io.File(savePath))

    // read in all the tiff images and make groups later

    val tiffSlices = sc.tiffFolder(imgPath)
    val sliceCnt = tiffSlices.count
    // parse folder and path
    val gtifSlices = tiffSlices.map {
      inSlice =>
        val imgName = inSlice._1.split("/").toList.reverse
        val foldName = imgName(1)
        val fileName = imgName(0).substring(0, imgName(0).lastIndexOf("."))
        val sampName = fileName.substring(0, fileName.find("0"))
        val sliceNum = fileName.substring(fileName.find("0"), fileName.length).toInt
        ((foldName, sampName), (new D3int(0, 0, sliceNum), inSlice._2))
    }
    // get image count
    val imgCount = gtifSlices.map(_._1).distinct.count
    // convert to timgblock
    val gBlocks = gtifSlices.mapValues {
      inSlice =>
        // focus on the red channel
        TImgTools.rgbConversionMethod = TImgTools.RGBConversion.RED
        (inSlice._1,
          new TImgSlice[Array[Float]](inSlice._2.polyReadImage(TImgTools.IMAGETYPE_FLOAT)
            .asInstanceOf[Array[Float]], inSlice._1, inSlice._2.getDim))
    }

    val gImgs = gBlocks.groupByKey.repartition(imgCount.toInt).persist(PERSIST_LEVEL)

    val slicePerImg = gImgs.mapValues(_.size).collect

    implicit val elSize = new tipl.util.D3float(1.0)

    val tImgs = gImgs.map {
      inKV =>
        (basePath.appendDir(inKV._1._1).appendDir(inKV._1._2),
          inKV._2.toTImg(basePath.appendDir(inKV._1._1).appendDir(inKV._1._2).append("ufilt.tif")))
    }

    def saveFiles(fileName: String, inVal: (TypedPath, TImgRO)): Unit =
      inVal._2.write(inVal._1.append(fileName + ".tif"))

    tImgs.foreach(saveFiles("ufilt", _))

    // make a rendering
    tImgs.foreach{
      inObj =>
        val (path,img) = inObj
        img.show3D()
    }


    import tipl.settings.FilterSettings

    val filtImgs = tImgs.mapValues(_.filter(1, 1, filterType = FilterSettings.MEDIAN))
    filtImgs.foreach(saveFiles("gfilt", _))
    val threshImgs = filtImgs.mapValues(_(_ > 2))
    threshImgs.foreach(saveFiles("thresh", _))

    val clImgs = filtImgs.map {
      inVal =>
        val curImg = inVal._2
        val clImg = curImg.componentLabel()
        clImg.shapeAnalysis(inVal._1.append("clpor_0.csv"))
        (inVal._1, clImg)
    }
    clImgs.foreach(saveFiles("cl", _))

    threshImgs.
      map {
      inVal =>
        val thImgs = inVal._2.thickness(inVal._1.append("dto.csv"))
        (inVal._1,
          thImgs(0))
    }.foreach(saveFiles("dto", _))

  }
}
