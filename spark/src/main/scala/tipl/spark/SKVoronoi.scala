/**
 *
 */
package tipl.spark


import org.apache.spark.Partitioner
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import tipl.util.ArgumentParser
import tipl.tools.BaseTIPLPluginIn
import scala.math.sqrt
import tipl.formats.TImgRO.CanExport
import tipl.util.TIPLGlobal
import tipl.formats.TImg
import tipl.util.TImgTools
import tipl.formats.TImgRO
import tipl.tools.BaseTIPLPluginIO
import tipl.tools.IVoronoiTransform
import tipl.util.D3int
import tipl.formats.PureFImage
import tipl.tests.TestPosFunctions
import tipl.spark.TypeMacros._
import tipl.util.TypedPath
import tipl.util.TIPLOps._

/**
 * A spark based code to perform shape analysis similarly to the code provided GrayAnalysis
 *
 * @author mader
 *
 */
class SKVoronoi extends BaseTIPLPluginIO with IVoronoiTransform {

  val stepSize = 0.5f
  /**
   * (avec: (D3int,((Long,Float),Boolean,Boolean))
   * position, (label, distance), alive, is_moved
   */
  val spread_voxels = (pvec: (D3int, ((Long, Float), Boolean)), windSize: D3int,
                       kernel: Option[BaseTIPLPluginIn.morphKernel], min_distance: Float) => {
    val pos = pvec._1
    val alive = pvec._2._2 & (pvec._2._1._1 > 0) & (pvec._2._1._2 >= min_distance)
    if (alive) {
      val windx = -windSize.x to windSize.x
      val windy = -windSize.y to windSize.y
      val windz = -windSize.z to windSize.z
      for (x <- windx; y <- windy; z <- windz
           if kernel.map(_.inside(0, 0, pos.x, pos.x + x, pos.y, pos.y + y, pos.z,
             pos.z + z)).getOrElse(true))
        yield (new D3int(pos.x + x, pos.y + y, pos.z + z),
          (
            (pvec._2._1._1, pvec._2._1._2 + sqrt(1.0 * x * x + y * y + z * z).floatValue()),
            true, x == 0 & y == 0 & z == 0))
    } else {
      Seq((pos,
        (
          (pvec._2._1._1, pvec._2._1._2),
          false, true)))
    }

  }
  /**
   * only keep points do not originate from a real point
   */
  val remove_edges = (pvec: (D3int, Iterable[((Long, Float), Boolean, Boolean)])) => {
    pvec._2.map(_._3).reduce(_ || _)
  }
  val compare_point_distance = (a: ((Long, Float), Boolean), b: ((Long, Float), Boolean, Boolean))
  => a._1._2 compare b._1._2
  val collect_voxels =
    (pvec: Iterable[((Long, Float), Boolean, Boolean)]) => {
      val tval = pvec.minBy(_._1._2)
      (tval._1, !tval._3)
    }
  val get_change = (avec: (D3int, ((Long, Float), Boolean))) => avec._2._2
  val get_empty = (avec: (D3int, ((Long, Float), Boolean))) => avec._2._1._1 < 1L
  val get_distance = (avec: (D3int, ((Long, Float), Boolean))) => avec._2._1._2.doubleValue
  val get_label = (avec: (D3int, ((Long, Float), Boolean))) => avec._2._1._1.doubleValue
  val fillImage = (tempObj: TImgTools.HasDimensions, defValue: Double) => {
    val defFloat = new PureFImage.ConstantValue(defValue)
    KVImgOps.createFromPureFun(SparkGlobal.getContext(getPluginName()), tempObj, defFloat).toKVFloat
  }
  var preserveLabels = false
  var alreadyCopied = false
  var includeEdges = false
  var maxUsuableDistance = -1.0
  var labeledDistanceMap: Option[RDD[(D3int, ((Long, Float), Boolean))]] = None
  var objDim: Option[D3int] = None
  var partitioner: Option[Partitioner] = None

  override def setParameter(p: ArgumentParser, prefix: String): ArgumentParser = {
    val args: ArgumentParser = super.setParameter(p, prefix)
    preserveLabels = args.getOptionBoolean(prefix + "preservelabels", preserveLabels,
      "Preserve the labels in old image")
    alreadyCopied = args.getOptionBoolean(prefix + "alreadycopied", alreadyCopied,
      "Has the image already been copied")
    includeEdges = args.getOptionBoolean(prefix + "includeedges", includeEdges,
      "Include the edges")
    maxUsuableDistance = args.getOptionDouble(prefix + "maxdistance", maxUsuableDistance,
      "The maximum distance to run the voronoi tesselation until")
    args
  }

  override def execute(): Boolean = {
    print("Starting Plugin..." + getPluginName)
    labeledDistanceMap match {
      case Some(ldm) =>
        var varLDM = ldm

        val curKernel: Option[BaseTIPLPluginIn.morphKernel] =
          if (neighborKernel == null) {
            None
          } else {
            Some(neighborKernel)
          }
        var changes = 1L
        var curDist = 0f
        var iter = 0
        var empty_vx = varLDM.filter(get_empty).count
        var mean_dist = varLDM.map(get_distance).mean
        var mean_label = varLDM.map(get_label).mean
        println("KSM - MD:" + mean_dist + "\tML" + mean_label)
        println("KSM - Dist:" + curDist + ", Iter:" + iter + ", Ops:" + 0 + ", " +
          "Changes:" + changes + ", Empty:" + empty_vx)
        while ((
          (curDist <= maxUsuableDistance) |
            (maxUsuableDistance < 0)) & (changes > 0)) {
          val spreadObj = varLDM.
            flatMap(spread_voxels(_, neighborSize, curKernel, curDist))
          val vox_involved = spreadObj.count

          varLDM = {
            val tvLDM = spreadObj.groupByKey.
              filter(remove_edges).mapValues(collect_voxels)
            partitioner match {
              case Some(cpt) => tvLDM.partitionBy(cpt)
              case None => tvLDM
            }
          }
          changes = ldm.filter(get_change).count
          empty_vx = ldm.filter(get_empty).count
          mean_dist = ldm.map(get_distance).mean
          mean_label = ldm.map(get_label).mean
          println("KSM - MD:" + mean_dist + "\tML" + mean_label)
          println("KSM -Dist:" + curDist + ", Iter:" + iter + ", Ops:" + vox_involved + ", " +
            "Changes:" + changes + ", Empty:" + empty_vx)
          println("KSM - " + ldm.takeSample(true, 10, 50).mkString(", "))

          iter += 1
          curDist += stepSize

        }
        labeledDistanceMap = Some(varLDM)
        true
      case None => throw new IllegalArgumentException(this + " has not been loaded yet")
    }

  }

  override def getPluginName() = {
    "kVoronoi:Spark"
  }

  /**
   * The first image is the
   */
  override def LoadImages(inImages: Array[TImgRO]) = {
    var labelImage = inImages(0)
    objDim = Some(labelImage.getDim)
    val liPart = SparkGlobal.getPartitioner(labelImage)
    partitioner = Some(liPart)
    val labeledImage = labelImage.toKV.toKVLong.getBaseImg.partitionBy(liPart)

    // any image filled with all negative distances
    val initialDistances = fillImage(labelImage, -1).getBaseImg
    val inMask = (ival: (D3int, Float)) => ival._2 > 0
    val toBoolean = (ival: Float) => true
    val maskedImage = (inImages.get(1) match {
      case Some(m) => m.toKV().toKV.toKVFloat
      case None => fillImage(labelImage, 1)
    }).getBaseImg.filter(inMask).mapValues(toBoolean)
    if (TIPLGlobal.getDebug()) println("KSM-MaskedImage:\t" + maskedImage + "\t" + maskedImage
      .first)
    // combine the images to create the starting distance map
    val maskPlusDist = (inVal: (Float, Option[Boolean])) => {
      inVal._2.map(inBool => -1f).getOrElse(inVal._1.floatValue)
    }
    val maskDistance = initialDistances.
      leftOuterJoin(maskedImage).mapValues(maskPlusDist)
    val fix_dist = (avec: (Long, Float)) => {
      if (avec._1 > 0) ((avec._1, 0f), true)
      else ((avec._1, java.lang.Float.MAX_VALUE.floatValue), false)
    }

    labeledDistanceMap = Some(labeledImage.join(maskDistance).
      mapValues(fix_dist).partitionBy(liPart))
    if (TIPLGlobal.getDebug()) println("KSM-LDMap:\t" + labeledDistanceMap + "\t" +
      labeledDistanceMap.get.first)
    if (TIPLGlobal.getDebug()) println("KSM-" + labeledDistanceMap.get.takeSample(true, 10,
      50).mkString(", "))
  }

  override def ExportImages(templateImage: TImgRO): Array[TImg] = {
    val tiEx = TImgTools.makeTImgExportable(templateImage)
    Array(ExportVolumesAim(tiEx),
      ExportDistanceAim(tiEx))
  }

  override def ExportDistanceAim(inObj: CanExport): TImg = {
    labeledDistanceMap match {
      case Some(ldm) => new KVImg[Float](inObj, TImgTools.IMAGETYPE_FLOAT, ldm.mapValues(_._1._2), 0)
      case None => throw new IllegalArgumentException(this + " has not yet been run!")
    }
  }

  override def ExportVolumesAim(inObj: CanExport): TImg = {
    labeledDistanceMap match {
      case Some(ldm) => new KVImg[Long](inObj, TImgTools.IMAGETYPE_LONG, ldm.mapValues(_._1._1), 0L)
      case None => throw new IllegalArgumentException(this + " has not yet been run!")
    }
  }

  override def WriteVolumesAim(exObj: TImgRO.CanExport, filename: TypedPath) = {
    TImgTools.WriteTImg(ExportVolumesAim(exObj), filename)
  }

  override def WriteDistanceAim(exObj: TImgRO.CanExport, filename: TypedPath) = {
    TImgTools.WriteTImg(ExportDistanceAim(exObj), filename)
  }

}


object SKTest extends SKVoronoi {
  def main(args: Array[String]): Unit = {
    val p = SparkGlobal.activeParser(args)
    val imSize = p.getOptionInt("size", 50,
      "Size of the image to run the test with")
    val testImg = TestPosFunctions.wrapItAs(imSize,
      new TestPosFunctions.DotsFunction(), TImgTools.IMAGETYPE_INT)


    LoadImages(Array(testImg))

    setParameter(p, "")

    p.checkForInvalid()
    execute()

  }
}

