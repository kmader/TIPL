package tipl.tests

import org.apache.spark.ui.tipl.HTMLUtils
import tipl.spark.SparkGlobal



/**
 * Created by mader on 3/27/15.
 */
object UFEMCloudTest {

  def main(args: Array[String]): Unit = {
    val p = SparkGlobal.activeParser(args) // for gui code
    //p.checkForInvalid()
    // setup spark context
    val sc = SparkGlobal.getContext("RaberAnalysis").sc

    // start notebook code
    import org.apache.spark.rdd.RDD
    import tipl.formats.TiffFolder
    import tipl.spark.IOOps._
    import tipl.util._

    import scala.reflect.ClassTag // for use pieces of IO code

    val PERSIST_LEVEL = org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK

    implicit class stackOTiff(ird: RDD[(String,TiffFolder.TIFSliceReader)])
      extends Serializable {
      def readAndRename(nameFun: (String => (String,String,Int))) = {
        ird.map {
          inSlice =>
            val (foldName,sampName,sliceNum) = nameFun(inSlice._1)
            val slicePos = new D3int(0, 0, sliceNum)
            ((foldName, sampName), (slicePos,
              new TImgSlice[Array[Short]](inSlice._2.polyReadImage(TImgTools.IMAGETYPE_SHORT)
                .asInstanceOf[Array[Short]], slicePos, inSlice._2.getDim)))
        }.persist(PERSIST_LEVEL)
      }
    }

    implicit class bunchOfPosImages[K : ClassTag,V : ClassTag](
                                     ird: RDD[(K,(D3int, TImgSlice[Array[V]]))])
      extends Serializable {
      def groupToBlocks()(implicit elSize: D3float) = {
        val imgCount = ird.keys.distinct.count
        ird.groupByKey.
          repartition(imgCount.toInt).
          map(
            inKV => (inKV._1,inKV._2.toTImg(TIPLStorageManager.openPath(inKV._1.toString)))
          )
      }
    }

    // the real code

    // get the gui running

    import org.apache.spark.ui.tipl.TiplUI
    HTMLUtils.createBinaryServletHandler("Test","Test/Test",(u: Any) => new Array[Byte](0),
      HTMLUtils.generateTempSecMan(sc.getConf),"")
    TiplUI.attachUI(sc)

    // setup s3 access
    sc.hadoopConfiguration.set("fs.s3n.awsAccessKeyId","")
    sc.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey",
      "")
    // load the data
    val baseString = "s3n://ksmbench3/"
    //val basePath = TIPLStorageManager.openPath()

    val tiffSlices = sc.tiffFolder(baseString+"*/*_015*.tif")
    // parse the filename
    def fileNameToImageName(filename: String): (String,String,Int) = {
      val nPath = filename.split("/").reverse
      val fileName = nPath(0).split(".rec")(0)
      val sliceNumber = fileName.substring(fileName.length-4).toInt
      val sampleName = nPath(1)
      val folderName = nPath(2)
      (folderName,sampleName,sliceNumber)
    }

    // set the voxel size to 1.4um
    implicit val elSize = new tipl.util.D3float(1.4)

    val gBlocks = tiffSlices.//sample(false,0.1).
      readAndRename(fileNameToImageName).
      groupToBlocks()


    val slicePerImg = gBlocks.mapValues(_.getDim()).collect

    println(slicePerImg.mkString("\n"))

  }
}
