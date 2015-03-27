package tests

import org.apache.spark.ui.tipl.TiplUI
import tipl.spark.SparkGlobal
import tipl.util._

/**
 * Created by mader on 3/27/15.
 */
class UFEMCloudTest {

}

object UFEMCloudTest {
  def main(args: Array[String]): Unit = {
    val p = SparkGlobal.activeParser(args) // for gui code
    p.checkForInvalid()
    // setup spark context
    val sc = SparkGlobal.getContext("RaberAnalysis").sc

    // the real code
    import tipl.spark.IOOps._ // for use pieces of IO code
    val PERSIST_LEVEL = org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK
    // get the gui running
    TiplUI.attachUI(sc)
    // setup s3 access
    sc.hadoopConfiguration.set("fs.s3n.awsAccessKeyId","AKIAJM4PPKISBYXFZGKA")
    sc.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey","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")
    // load the data
    val basePath = TIPLStorageManager.openPath("s3n://ksmbench3/")

    val tiffSlices = sc.tiffFolder(basePath.append("*/*.tif").toString)
    // parse the filename
    def fileNameToImageName(filename: String): (String,String,Int) = {
      val nPath = filename.split("/").reverse
      val fileName = nPath(0).split(".rec")(0)
      val sliceNumber = fileName.substring(fileName.length-4).toInt
      val sampleName = nPath(1)
      val folderName = nPath(2)
      (folderName,sampleName,sliceNumber)
    }
    // parse folder and path
    val gtifSlices = tiffSlices.map {
      inSlice =>
        val (foldName,sampName,sliceNum) = fileNameToImageName(inSlice._1)
        ((foldName, sampName), (new D3int(0, 0, sliceNum), inSlice._2))
    }
    // get image count
    val imgCount = gtifSlices.map(_._1).distinct.count

    // convert to image block
    val gBlocks = gtifSlices.mapValues {
      inSlice =>
         (inSlice._1,
          new TImgSlice[Array[Short]](inSlice._2.polyReadImage(TImgTools.IMAGETYPE_SHORT)
            .asInstanceOf[Array[Short]], inSlice._1, inSlice._2.getDim))
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


  }
}
