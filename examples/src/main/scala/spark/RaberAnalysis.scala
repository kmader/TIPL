package spark

import org.apache.spark.SparkContext._
import org.apache.spark.ui.tipl.TiplUI
import tipl.blocks.BaseBlockRunner
import tipl.formats.TImgRO
import tipl.spark.IOOps._
import tipl.spark.SparkGlobal
import tipl.util.TIPLOps._
import tipl.util._
/**
 * Created by mader on 10/16/14.
 */
object RaberAnalysis {

  /**
   * The parameters needed to run the analysis
   * @param runLocal run using local drive
   * @param doRender created 3D renderings of objects
   * @param threshVals list of thresholds (just one for single threshold)
   * @param imgPath
   * @param savePath
   * @param checkpointDir directory for checkpointing files
   */
  case class RAArgs(runLocal: Boolean,doRender: Boolean, threshVals: Array[Int],
                    imgPath: String,
                    savePath: String,
                    checkpointDir: String) extends Serializable
  /**
   * Handle all of the parsing arguments
   * @param args
   * @return case class containing all the arguments bundled up
   */
  def parseArguments(args: Array[String]) = {
    val p = SparkGlobal.activeParser(args)

    val runLocal = true
    val defPath = if (runLocal) {
      "/Users/mader/Dropbox/WorkRelated/Raber/bci102014"
    } else {
      "/Volumes/WORKDISK/WorkData/Raber/bci102014_2"
    }
    val rootPath = p.getOptionPath("root",defPath,"The base path containing all of the images")
      .getPath()

    val useAll = p.getOptionBoolean("useall",true,"Use all of the images or just a few to test")
    val doRender = p.getOptionBoolean("render",true,"Make 3D renderings of the various steps")
    val thresh = p.getOptionInt("thresh",40,"Single threshold value for the images")

    val multiThresh = p.getOptionBoolean("multithresh",true,"Use multiple threshold values")
    val minThresh = p.getOptionInt("minthresh",35,"Starting threshold")
    val maxThresh = p.getOptionInt("maxthresh",71,"Final threshold")
    val ssThresh = p.getOptionInt("step",7,"Step between threshold values")
    val savePath = rootPath+"/results/"
    val checkpointDir = rootPath+"/checkpoint/"
    val imgPath = if (useAll) {
      rootPath+"/brain*/*.tif"
    } else {
      rootPath+"/brain*3/a*.tif"
    }
    p.checkForInvalid()
    val threshVals = if (multiThresh) {
      Range(minThresh,maxThresh,ssThresh).toArray
    } else {
      Array(thresh)
    }
    RAArgs(runLocal,doRender,threshVals,imgPath,
      savePath,
      checkpointDir)
  }
  def runAnalysis(inArgs: RAArgs): Unit = {
    // setup spark context
    val sc = SparkGlobal.getContext("RaberAnalysis").sc
    // get the gui running
    TiplUI.attachUI(sc)
    val doCheckpoint = (inArgs.checkpointDir.length()>0)
    if (doCheckpoint) sc.setCheckpointDir(inArgs.checkpointDir)
    val PERSIST_LEVEL = org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK

    // make basepath into a typedpath object
    val basePath = TIPLStorageManager.openPath(inArgs.savePath)

    // read in all the tiff images and make groups later

    val tiffSlices = sc.tiffFolder(inArgs.imgPath)
    val sliceCnt = tiffSlices.count

    // parse folder and path
    val gtifSlices = tiffSlices.map {
      inSlice =>
        val (foldName,sampName,sliceNum) = fileNameToImageName(inSlice._1)
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
    // make the required directories and the a TImg from the slices
    val tImgs = gImgs.map {
      inKV =>
        (basePath.appendDir(inKV._1._1).appendDir(inKV._1._2),
          inKV._2.toTImg(basePath.appendDir(inKV._1._1).appendDir(inKV._1._2).append("ufilt.tif")))
    }

    def saveFiles(fileName: String, inVal: (TypedPath, TImgRO)): Unit =
      inVal._2.write(inVal._1.append(fileName + ".tif"))

    tImgs.foreach(saveFiles("ufilt", _))

    // make a rendering

    if (inArgs.doRender) {
      tImgs.foreach {
        inObj =>
          val (path, img) = inObj
          img.render3D(path.append("render_ufilt.tif"), extargs = "-crmin=0 -crmax=100 -usecr " +
            "-lutnr=4")
      }
    }

    import tipl.settings.FilterSettings

    val filtImgs = tImgs.mapValues(_.filter(1, 1, filterType = FilterSettings.MEDIAN)).
      persist(PERSIST_LEVEL)
    if (doCheckpoint) filtImgs.checkpoint()

    filtImgs.foreach(saveFiles("gfilt", _))
    // apply multiple threshold values if needed
    val threshImgs = {
      val threshVals = sc.parallelize(inArgs.threshVals)
      filtImgs.cartesian(threshVals).map{
        inObj =>
          val ((path,img),cthresh) = inObj
          (path.appendDir("thresh_"+cthresh.toString),
            img(_>cthresh,0))
      }.repartition((imgCount*threshVals.count).asInstanceOf[Int])
    }

    if (doCheckpoint) threshImgs.checkpoint()
    threshImgs.foreach(saveFiles("threshold", _))

    if (inArgs.doRender) {
      threshImgs.foreach {
        inObj =>
          val (path, img) = inObj
          img.render3D(path.append("render_thresh.tif"))
      }
    }

    case class DTOutput(thickness: TImgRO, distance: TImgRO, ridge: TImgRO) extends Serializable
    // generate the distance map and the ridge file
    val dtImgs = threshImgs.
      map {
      inVal =>
        val thImgs = inVal._2.thickness(inVal._1.append("dto.csv"))
        (inVal._1,
          DTOutput(thImgs(0),thImgs(1),thImgs(2)))
    }
    dtImgs.foreach(inObj => {
      val (path, imgs) = inObj
      saveFiles("dto", (path,imgs.thickness))
      saveFiles("dist", (path,imgs.distance))
      saveFiles("ridge", (path,imgs.ridge))
      if (inArgs.doRender) {
        imgs.thickness.render3D(path.append("render_dto.tif"), extargs = "-crmin=0 -crmax=120 " +
          "-usecr " +
          "-lutnr=4")
        imgs.ridge.render3D(path.append("render_ridge.tif"), extargs = "-crmin=0 -crmax=120 " +
          "-usecr " +
          "-lutnr=4")
      }

    }
    )

    val clImgs = threshImgs.map {
      inVal =>
        val curImg = inVal._2
        val clImg = curImg.componentLabel()
        clImg.shapeAnalysis(inVal._1.append("pores_0.csv"))
        (inVal._1, clImg)
    }
    clImgs.foreach(saveFiles("cl", _))

    if (inArgs.doRender) {
      clImgs.foreach {
        inObj =>
          val (path, img) = inObj
          img.render3D(path.append("render_cl.tif"), extargs = "-crmin=0 -crmax=100 -usecr " +
            "-lutnr=4")
      }
    }


    // run Append Analyze Phase Block
    val baseStr = "-blocknames=AnalyzePhase,AppendAnalyzePhase -simplenames"
    clImgs.foreach(
      inObj => {
        val (path,_) = inObj

        val apStr = "-apb1:segmented=ridge.tif -apb1:labeled=subvessels.tif -apb1:minvoxcount=30 " +
          "-apb1:neighborhood=1,1,1 -apb1:phase=subvessels -apb1:mask=threshold.tif " +
          "-apb1:density=sv_filled.tif -apb1:neighbors=sv_connections.tif"
        val aapStr = "-aapb2:labeled=sv_filled.tif -aapb2:density=sv_density.tif " +
          "-aapb2:neighbors=sv_neighbors.tif -aapb2:seedfile=subvessels_3.csv " +
          "-aapb2:phase=full -aapb2:mask=mask.tif"


        val runStr = baseStr +" "+ apStr +" "+ aapStr
        val p = TIPLGlobal.activeParser(runStr.split(" "))
        p.setRootDirectory(path.getPath())
        BaseBlockRunner.main(p)
      }
    )


  }

  /**
   * convert the path into a folder name, sample name, and slice number
   * @param filePath
   * @return tuple with foldername, samplename, and slicenumber
   */
  def fileNameToImageName(filePath: String): (String,String,Int) = {
    val imgName = filePath.split("/").toList.reverse
    val foldName = imgName(1)
    val fileName = imgName(0).substring(0, imgName(0).lastIndexOf("."))
    val sampName = fileName.substring(0, fileName.find("0"))
    val sliceNum = fileName.substring(fileName.find("0"), fileName.length).toInt
    (foldName,sampName,sliceNum)
  }

  def main(args: Array[String]): Unit = {
    val inArgs = parseArguments(args)
    runAnalysis(inArgs)
  }
}
