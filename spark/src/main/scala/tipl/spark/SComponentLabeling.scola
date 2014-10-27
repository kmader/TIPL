package tipl.spark


import java.io.Serializable
import org.apache.spark.SparkContext._
import org.apache.commons.collections.IteratorUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, Accumulator}
import org.apache.spark.broadcast.Broadcast
import tipl.formats.TImgRO
import tipl.tests.TestPosFunctions
import tipl.util._
import tipl.tools.{BaseTIPLPluginIO,BaseTIPLPluginIn}

import scala.collection.mutable


/**
 * CL performs iterative component labeling using a very simple label and merge algorithm
 *
 * @author mader
 */
@SuppressWarnings(Array("serial"))
object SComponentLabeling {

  def main(args: Array[String]): Unit = {
    println("Testing Component Label Code")
    val p: ArgumentParser = SparkGlobal.activeParser(args)
    testFunc(p)
  }

  private def testFunc(p: ArgumentParser) {
    val boxSize: Int = p.getOptionInt("boxsize", 8, "The dimension of the image used for the analysis")
    val layerWidth: Int = p.getOptionInt("width", boxSize / 4, "The width of the layer used for the analysis")
    val writeIt: TypedPath = p.getOptionPath("out", "", "write image as output file")
    val testImg: TImgRO = TestPosFunctions.wrapIt(boxSize, new TestPosFunctions.SphericalLayeredImage(boxSize / 2, boxSize / 2, boxSize / 2, 0, 1, layerWidth))
    if (writeIt.length > 0) TImgTools.WriteTImg(testImg, writeIt)
    val curPlugin: CL = new CL
    curPlugin.setParameter(p, "")
    p.checkForInvalid
    curPlugin.LoadImages(Array[TImgRO](testImg))
    curPlugin.execute
  }

  /**
   * Get a summary of the groups and the count in each
   *
   * @param inLabelImg
   * @return
   */
  private def groupCount(inLabelImg: DSImg[Long]) = {
    inLabelImg.getBaseImg.values.flatMap{
      inSlice =>
        for(ival <- inSlice.get() if(ival>0) ) yield ival
    }.countByValue()
  }

  /**
   * A utility function for joining together two maps where values from the same key are added
   *
   * @param mapA
   * @param mapB
   * @return a joined map where the overlapping elements have been joined using the canJoin interface
   */
  protected def joinMap[Sf,Sc](mapA: Map[Sf, Sc], mapB: Map[Sf, Sc], joinTool: canJoin[Sc]) = {
    val joinMap: mutable.HashMap[Sf, Sc] = new mutable.HashMap[Sf, Sc]()
    for((key,value) <- mapA) joinMap.put(key,value)
    for ((cKey,value) <- mapB) {
      if (joinMap.contains(cKey)) joinMap.put(cKey, joinTool.join(value, joinMap(cKey)))
      else joinMap.put(cKey, value)
    }
    joinMap
  }

  /**
   * Generates a label image using the index of each voxel (easy to keep concurrent)
   *
   * @param inMaskImg the binary input image
   * @return labeled image as long[]
   */
  private def makeLabelImage(inMaskImg: DSImg[Boolean], ns: D3int, mKernel: BaseTIPLPluginIn.morphKernel): DSImg[Long] = {
    val wholeSize: D3int = inMaskImg.getDim
    import scala.collection.JavaConversions._
    val slicesList = inMaskImg.getBaseImg().map
    {
      arg0 =>
        val inBlock: TImgBlock[Array[Boolean]] = arg0._2
        val cSlice: Array[Boolean] = inBlock.get
        val spos: D3int = inBlock.getPos
        val sdim: D3int = inBlock.getDim
        val gOffset: D3int = new D3int(0, 0, 0)
        val oSlice: Array[Long] = new Array[Long](cSlice.length)
        var z: Int = 0
        while (z < sdim.z) {
          var y: Int = 0
          while (y < sdim.y) {
            var x: Int = 0
            while (x < sdim.x) {
              {
                val off: Int = (z * sdim.y + y) * sdim.x + x
                if (cSlice(off)) {
                  var label: Long = 0L
                  if (oSlice(off) == 0) label = ((z + spos.z) * wholeSize.y + (y + spos.y)) * wholeSize.x + x + spos.x
                  else label = oSlice(off)
                  oSlice(off) = label
                  for (scanPos <- BaseTIPLPluginIn.getScanPositions(mKernel, new D3int(x, y, z), gOffset, off, sdim, ns)) {
                    if (cSlice(scanPos.offset)) oSlice(scanPos.offset) = label
                  }
                }
              }
              x += 1;
            }
            y += 1;
          }
          z += 1;
        }
        (arg0._1, new TImgBlock[Array[Long]](oSlice, inBlock.getPos, inBlock.getDim))
    }
    new DSImg[Long](wholeSize,inMaskImg.getPos(),inMaskImg.getElSize(),TImgTools.IMAGETYPE_LONG,slicesList,
      inMaskImg.getPath().append("::aslabels"))
  }

  val useSpreadSlicesPartitioner = false
  /**
   * Looks for all the connections in an image by scanning neighbors
   *
   * @param labeledImage
   * @param neighborSize
   * @param mKernel
   * @return
   */
  private def slicesToConnections(labeledImage: DSImg[Long], neighborSize: D3int, mKernel: BaseTIPLPluginIn.morphKernel, inTO: TimingObject): RDD[(D3int, OmnidirectionalMap)] = {
    var fannedImage: RDD[(D3int, Iterable[TImgBlock[Array[Long]]])] =
    if (useSpreadSlicesPartitioner) labeledImage.spreadSlices(neighborSize.z).groupByKey.partitionBy(SparkGlobal.getPartitioner(labeledImage.getDim))
    else labeledImage.getBaseImg.groupByKey
    val gccObj: GetConnectedComponents = new GetConnectedComponents(inTO, mKernel, neighborSize)
    val outComponents: RDD[(D3int, OmnidirectionalMap)] = fannedImage.map(ikey => gccObj.call(ikey))
    return outComponents
  }

  /**
   * Searches for new groups in the images by scanning all neighbors
   *
   * @param labeledImage
   * @param neighborSize
   * @param mKernel
   * @return
   */
  private def scanForNewGroups(labeledImage: DSImg[Long], neighborSize: D3int, mKernel: BaseTIPLPluginIn.morphKernel, inTO: TimingObject): Map[Long, Long] = {
    val connectedGroups: RDD[(D3int, SComponentLabeling.OmnidirectionalMap)] = slicesToConnections(labeledImage, neighborSize, mKernel, inTO)

    val groupList: SComponentLabeling.OmnidirectionalMap = connectedGroups.values.reduce(
      (a: OmnidirectionalMap, b: OmnidirectionalMap) => a.coalesce(b)
    )
    return groupListToMerges(groupList)
  }

  /**
   * A simple command to run the slice based component labeling and connecting on each slice first
   *
   * @param labeledImage
   * @param neighborSize
   * @param mKernel
   * @return
   */
  private def scanAndMerge(labeledImage: DSImg[Long], neighborSize: D3int, mKernel: BaseTIPLPluginIn.morphKernel, inTO: TimingObject): (DSImg[Long], Long) = {
    val connectedGroups: RDD[(D3int, OmnidirectionalMap)] = slicesToConnections(labeledImage, neighborSize, mKernel, inTO)
    val mergeCmds: RDD[(D3int, Map[Long, Long])] = connectedGroups.mapValues{
      arg0 =>
        val start: Long = System.currentTimeMillis
        val outList: Map[Long, Long] = groupListToMerges(arg0)
        inTO.timeElapsed+=(System.currentTimeMillis - start)*1.0
        inTO.mapOperations+=1
        outList
    }
    System.out.println("Merges per slice")
    var totalMerges: Long = 0
    for (cVal <- mergeCmds.values.map{arg0 =>arg0.size}.collect) {
      System.out.println("\t" + cVal)
      totalMerges += cVal
    }
    val newlabeledImage: RDD[(D3int, TImgBlock[Array[Long]])] =
      labeledImage.getBaseImg.join(mergeCmds, SparkGlobal.getPartitioner(labeledImage.getDim)).mapValues {
        inTuple =>
          val start: Long = System.currentTimeMillis
          val cBlock: TImgBlock[Array[Long]] = inTuple._1
          val curSlice: Array[Long] = cBlock.get
          val outSlice: Array[Long] = new Array[Long](curSlice.length)
          val mergeCommands: Map[Long, Long] = inTuple._2
            var i: Int = 0
            while (i < curSlice.length) {
                if (curSlice(i) > 0) outSlice(i) = mergeCommands(curSlice(i))
                i += 1;
            }

          inTO.mapOperations+=(1)
          inTO.timeElapsed+=((System.currentTimeMillis - start)*1.0)
          new TImgBlock[Array[Long]](outSlice, cBlock)
      }
    return (new DSImg[Long](labeledImage, newlabeledImage, TImgTools.IMAGETYPE_LONG), totalMerges)
  }

  /**
   * A rather ugly function to turn the neighbor lists into specific succinct merge commands
   *
   * @param groupList
   * @return
   */
  private def groupListToMerges(groupList: OmnidirectionalMap): Map[Long, Long] = {
    val groups: Set[Long] = groupList.getKeys
    val groupGroups: Set[Set[Long]] = new HashSet[Set[Long]](groups.size)
    
    for (curKey <- groups) {
      
      for (oldSet <- groupGroups) if (oldSet.contains(curKey)) break //todo: break is not supported
      val cList: Set[Long] = groupList.rget(curKey)
      groupGroups.add(cList)
    }
    val mergeCommands: Map[Long, Long] = new PassthroughHashMap(groupGroups.size * 2)
    
    for (curSet <- groupGroups) {
      val mapToVal: Long = Collections.min(curSet)
      
      for (cKey <- curSet) {
        mergeCommands.put(cKey, mapToVal)
      }
    }
    return mergeCommands
  }

  private def mergeGroups(labeledImage: DSImg[Long], mergeCommands: Map[Long, Long], inTO: TimingObject): DSImg[Long] = {
    val broadcastMergeCommands: Broadcast[Map[Long, Long]] = SparkGlobal.getContext.broadcast(mergeCommands)
    val newlabeledImage: DSImg[Long] = labeledImage.map(new PairFunction[(D3int, TImgBlock[Array[Long]]), D3int, TImgBlock[Array[Long]]] {
      def call(arg0: (D3int, TImgBlock[Array[Long]])): (D3int, TImgBlock[Array[Long]]) = {
        val start: Long = System.currentTimeMillis
        val curSlice: Array[Long] = arg0._2.get
        val outSlice: Array[Long] = new Array[Long](curSlice.length)
        {
          var i: Int = 0
          while (i < curSlice.length) {
            {
              outSlice(i) = cMergeCommands.get(curSlice(i))
            }
            ({
              i += 1; i - 1
            })
          }
        }
        inTO.timeElapsed.$plus$eq((System.currentTimeMillis - start).asInstanceOf[Double])
        inTO.mapOperations.$plus$eq(1)
        return new (D3int, TImgBlock[Array[Long]])(arg0._1, new TImgBlock[Array[Long]](outSlice, arg0._2))
      }

      private[spark] final val cMergeCommands: Map[Long, Long] = broadcastMergeCommands.value
    }, TImgTools.IMAGETYPE_LONG)
    return newlabeledImage
  }

  protected var LongAdder: canJoin[Long] = new canJoin[Long] {
    def join(a: Long, b: Long): Long = {
      return a + b
    }
  }

  /**
   * A simple interface for joining two elements together
   *
   * @param <Si>
   * @author mader
   */
  abstract trait canJoin[Si] extends Serializable {
    def join(a: Si, b: Si): Si
  }

  /**
   * Effectively a map but it uses only primitives instead of objects so it doesn't really implement the interface
   *
   * @author mader
   */
  protected class OmnidirectionalMap extends Serializable {
    /**
     * The maximum number of iterations for the recursive get command (-1 is no-limit)
     */
    val RGET_MAX_ITERS: Int = 2
    val emptyList: List[Long] = new ArrayList[Long](0)
    def this(guessLength: Int) {
      this()
      mapElements = new LinkedHashSet[Array[Long]](guessLength)
    }

    /**
     * for presorted elements
     *
     * @param ele
     */
    private[spark] def add(ele: Array[Long]) {
      if (!mapElements.contains(ele)) mapElements.add(ele)
    }

    def put(valA: Long, valB: Long) {
      var curEle: Array[Long] = null
      if (valA > valB) curEle = Array[Long](valB, valA)
      else curEle = Array[Long](valA, valB)
      add(curEle)
    }

    def size: Int = {
      return mapElements.size
    }

    def isEmpty: Boolean = {
      return (this.size == 0)
    }

    def containsKey(key: Long): Boolean = {
      
      for (curKey <- mapElements) {
        if (key == curKey(0)) return true
        else if (key == curKey(1)) return true
      }
      return false
    }

    /**
     * get all of the keys in the list
     *
     * @return
     */
    def getKeys: Set[Long] = {
      val outList: Set[Long] = new HashSet[Long]
      
      for (curKey <- mapElements) {
        outList.add(curKey(0))
        outList.add(curKey(1))
      }
      return outList
    }

    def get(key: Long): Set[Long] = {
      val outList: Set[Long] = new HashSet[Long]
      
      for (curKey <- mapElements) {
        if (key == curKey(0)) outList.add(curKey(1))
        else if (key == curKey(1)) outList.add(curKey(0))
      }
      return outList
    }

    /**
     * A recursive get command which gets all the neighbors of the neighbors ... until the list stops growing
     *
     * @param key
     * @return
     */
    def rget(key: Long): Set[Long] = {
      val firstSet: Set[Long] = get(key)
      var outSet: Set[Long] = new HashSet[Long](firstSet.size)
      outSet.addAll(get(key))
      var lastlen: Int = 0
      var rgetCount: Int = 0
      while (outSet.size > lastlen) {
        lastlen = outSet.size
        val outSetTemp: Set[Long] = new HashSet[Long](outSet.size)
        outSetTemp.addAll(outSet)
        
        for (e <- outSet) outSetTemp.addAll(get(e))
        outSet = outSetTemp
        rgetCount += 1
        if ((RGET_MAX_ITERS > 0) & (rgetCount > RGET_MAX_ITERS)) break //todo: break is not supported
      }
      outSet.add(key)
      return outSet
    }

    private[spark] def getAsSet: Set[Array[Long]] = {
      return mapElements
    }

    /**
     * merge two sets together
     *
     * @param map2
     */
    def coalesce(map2: OmnidirectionalMap): OmnidirectionalMap = {
      for (curEle <- map2.getAsSet) this.add(curEle)
      this
    }

    private[spark] final val mapElements: Set[Array[Long]] = null
  }

  /**
   * a hashmap which returns the input value when it is missing from the map and 0 when 0 is given as an input
   *
   * @author mader
   */
  private[spark] class PassthroughHashMap(guessSize: Int) extends HashMap[Long, Long](guessSize) with Serializable {
    val zero: Long = 0.asInstanceOf[Long]
    override def get(objVal: AnyRef): Long = {
      val eVal: Long = objVal.asInstanceOf[Long]
      if (eVal == zero) return zero
      if (this.containsKey(eVal)) return super.get(objVal)
      else return eVal
    }
  }

  private[spark] class TimingObject(sc: SparkContext) extends Serializable {
    val timeElapsed: Accumulator[Double] = sc.accumulator(0.0)
    val mapOperations: Accumulator[Int] = sc.accumulator(0)
  }

  /**
   * The function which actually finds connected components in each block and returns
   * them as a list
   *
   * @author mader
   */
  def GetConnectedComponents(inTO: TimingObject, mKernel: BaseTIPLPluginIn.morphKernel, ns: D3int) = {
    (inVal: (D3int, Iterable[TImgBlock[Array[Long]]])) =>
      val start: Long = System.currentTimeMillis
      val inBlocks = inVal._2.toList
      val templateBlock = inBlocks(0)
      val blockSize: D3int = templateBlock.getDim
      val eleCount: Int = templateBlock.getDim.prod.asInstanceOf[Int]
      val neighborList = new Array[mutable.HashSet[Long]](eleCount)

    {
      var i: Int = 0
      while (i < eleCount) {
        neighborList(i) = new mutable.HashSet[Long]
        i += 1
      }
    }

      for (cBlock <- inBlocks) {
        val curBlock: Array[Long] = cBlock.get()

        {
          var zp: Int = 0
          while (zp < templateBlock.getDim.z) {
            {
              {
                var yp: Int = 0
                while (yp < templateBlock.getDim.y) {
                  {
                    {
                      var xp: Int = 0
                      while (xp < templateBlock.getDim.x) {
                        {
                          val off: Int = ((zp) * blockSize.y + (yp)) * blockSize.x + (xp)
                          for (cPos <- BaseTIPLPluginIn.getScanPositions(mKernel, new D3int(xp, yp, zp), cBlock.getOffset, off, blockSize, ns)) {
                            val cval: Long = curBlock(cPos.offset)
                            if (cval > 0) neighborList(off).add(cval)
                          }
                        }
                        xp += 1
                      }
                    }
                  }
                  yp += 1
                }
              }
            }
            zp += 1
          }
        }
      }
      val pairs: OmnidirectionalMap = new OmnidirectionalMap(2 * eleCount) {
        var i: Int = 0
        while (i < eleCount) {
          {
            if (neighborList(i).size > 2) {
              val iterArray: Array[Long] = neighborList(i).toArray(new Array[Long](neighborList(i).size)) {
                var ax: Int = 0
                while (ax < (iterArray.length - 1)) {
                  {
                    {
                      var bx: Int = ax + 1
                      while (bx < iterArray.length) {
                        {
                          pairs.put(iterArray(ax), iterArray(bx))
                        }
                        ({
                          bx += 1;
                          bx - 1
                        })
                      }
                    }
                  }
                  ({
                    ax += 1;
                    ax - 1
                  })
                }
              }
            }
          }
          i += 1
        }
      }
      inTO.timeElapsed += (System.currentTimeMillis - start) * 1.0
      inTO.mapOperations += 1
      new (D3int, OmnidirectionalMap)(inTuple._1, pairs)


  }

}

@SuppressWarnings(Array("serial"))
class SComponentLabeling extends BaseTIPLPluginIO {
  private def this() {
    this()
  }

  override def setParameter(p: ArgumentParser, prefix: String): ArgumentParser = {
    runSliceMergesFirst = p.getOptionBoolean("slicemerging", runSliceMergesFirst, "Run slice merges before entire image merges, faster for very large data sets")
    return super.setParameter(p, prefix)
  }

  def getPluginName: String = {
    return "Spark-ComponentLabel"
  }

  def LoadImages(inImages: Array[TImgRO]) {
    assert((inImages.length > 0))
    val inImage: TImgRO = inImages(0)
    if (inImage.isInstanceOf[DTImg[_]] & inImage.getImageType == TImgTools.IMAGETYPE_BOOL) maskImg = inImage.asInstanceOf[DTImg[Array[Boolean]]]
    else maskImg = DTImg.ConvertTImg(SparkGlobal.getContext(getPluginName + "Context"), inImage, TImgTools.IMAGETYPE_BOOL)
  }

  /**
   * run and return only the largest component
   */
  def runFirstComponent {
    objFilter = new ComponentLabel.CLFilter {
      def accept(labelNumber: Int, voxCount: Int): Boolean = {
        return (labelNumber == maxComp)
      }

      def getProcLog: String = {
        return "Using largest component filter\n"
      }

      def prescan(labelNumber: Int, voxCount: Int) {
        if ((voxCount > maxVol) | (maxComp == -1)) {
          maxComp = labelNumber
          maxVol = voxCount
        }
      }

      private[spark] var maxComp: Int = -1
      private[spark] var maxVol: Int = -1
    }
    execute
  }

  def execute: Boolean = {
    val start: Long = System.currentTimeMillis
    val to: TimingObject = new TimingObject(this.maskImg.getContext)
    labelImg = makeLabelImage(this.maskImg, getNeighborSize, getKernel)
    var stillMerges: Boolean = runSliceMergesFirst
    var i: Int = 0
    while (stillMerges) {
      val smOutput: (DSImg[Long], Long) = scanAndMerge(labelImg, getNeighborSize, getKernel, to)
      labelImg = smOutput._1
      System.out.println("Iter: " + i + ", Full:" + (false) + "\n\tMerges " + smOutput._2)
      stillMerges = (smOutput._2 > 0)
      i += 1
    }
    stillMerges = true
    while (stillMerges) {
      var curGrpSummary: String = ""
      val cGrp: Map[Long, Long] = groupCount(labelImg)
      
      for (cEntry <- cGrp.entrySet) {
        curGrpSummary += cEntry.getKey + "\t" + cEntry.getValue + "\n"
        if (curGrpSummary.length > 50) break //todo: break is not supported
      }
      val curMap: Map[Long, Long] = scanForNewGroups(labelImg, getNeighborSize, getKernel, to)
      var curMapSummary: String = ""
      
      for (cEntry <- curMap.entrySet) {
        curMapSummary += cEntry.getKey + "=>" + cEntry.getValue + ","
        if (curMapSummary.length > 50) break //todo: break is not supported
      }
      System.out.println("Iter: " + i + ", Full:" + (true) + "\n" + curGrpSummary + "\tMerges " + curMap.size + ": Groups:" + cGrp.size + "\n\t" + curMapSummary)
      if (curMap.size > 0) {
        labelImg = mergeGroups(labelImg, curMap, to)
      }
      else {
        stillMerges = false
      }
      i += 1
    }
    val cGrp: List[Map.Entry[Long, Long]] = new ArrayList[Map.Entry[Long, Long]](groupCount(labelImg).entrySet)
    Collections.sort(cGrp, new Comparator[Map.Entry[Long, Long]] {
      def compare(o1: Map.Entry[Long, Long], o2: Map.Entry[Long, Long]): Int = {
        return o1.getValue.compareTo(o2.getValue)
      }
    })
    var curGrpSummary: String = ""
    val reArrangement: SortedMap[Long, Long] = new TreeMap[Long, Long]
    var outDex: Int = 1
    
    for (cEntry <- cGrp) {
      if (curGrpSummary.length < 100) curGrpSummary += cEntry.getKey + "=>" + outDex + "\t" + cEntry.getValue + " voxels\n"
      reArrangement.put(cEntry.getKey, outDex.asInstanceOf[Long])
      outDex += 1
    }
    System.out.println("Final List:\n" + curGrpSummary + "\n Total Elements:\t" + cGrp.size)
    labelImg = mergeGroups(labelImg, reArrangement, to)
    val runTime: Long = System.currentTimeMillis - start
    var mapTime: Double = .0
    var mapOps: Long = 0L
    mapTime = to.timeElapsed.value
    mapOps = to.mapOperations.value
    System.out.println("CSV_OUT," + SparkGlobal.getMasterName + "," + reArrangement.size + "," + labelImg.getDim.x + "," + labelImg.getDim.y + "," + labelImg.getDim.z + "," + mapTime + "," + runTime + "," + mapOps + "," + SparkGlobal.maxCores + "," + SparkGlobal.getSparkPersistenceValue + "," + SparkGlobal.useCompression)
    return true
  }

  def ExportImages(templateImage: TImgRO): Array[TImg] = {
    return Array[TImg](labelImg, maskImg)
  }

  private var runSliceMergesFirst: Boolean = true
  private var maskImg: DTImg[Array[Boolean]] = null
  private var labelImg: DSImg[Long] = null
  private var objFilter: ComponentLabel.CLFilter = null
}


