package tipl.graph




/**
 * Created by mader on 2/3/15.
 */
class CellularPotts {

}

object CellularPotts extends Serializable {
  import org.apache.spark.SparkContext
  import scala.util.control.Exception.allCatch
  private def isLongNumber(s: String): Boolean = (allCatch opt s.toLong).isDefined
  private def isDoubleNumber(s: String): Boolean = (allCatch opt s.toDouble).isDefined
  import tipl.util.D3int

  private def allDbl(s: String*) = s.map(isDoubleNumber(_)).reduce(_ & _)
  private def allLong(s: String*) = s.map(isLongNumber(_)).reduce(_ & _)
  val littleFile =
    "/Users/mader/OneDrive/WorkData/Foam/3DFoam/Gilberto/free-fluid-gilberto/" +
      "thickfoam_100_100_100.piff"
  val bigFile = "/Users/mader/OneDrive/WorkData/Foam/3DFoam/Gilberto/mono_test/overseg/" +
    "thickfoam_504_504_80.piff"
  def setup(sc: SparkContext) = {
    val rawData = sc.textFile(littleFile)
    rawData.flatMap(_.split(" ") match {
      case Array(label,phaseName,x,jx,y,jy,z,jz) if allLong(label,x,y,z) =>
        Seq(
          (new D3int(x.toInt,y.toInt,z.toInt),(label.toLong,phaseName))
        )
      case _ =>
        Seq()
    })
  }

  import org.apache.spark.rdd.RDD
  import org.apache.spark.graphx.VertexId
  import spark.images.ImageTools
  import org.apache.spark.SparkContext._
  import org.apache.spark.graphx.Edge
  import org.apache.spark.graphx.Graph

  case class VertexPoint(pos: D3int, label: Long, phase: String, id: VertexId)

  def createGraph(ptList: RDD[(D3int,(Long,String))]) = {
    val vertices = ptList.zipWithIndex.map{
      case ((pos,(label,phase)),id) => VertexPoint(pos,label,phase,id)
    }

    val fvertices: RDD[(VertexId,VertexPoint)] = vertices.map(vp => (vp.id,vp))
    /**
     * The edges consist of a link (vertex->vertex) and a boolean indicating if it is a border
     */
    val grpVert =  vertices.
      flatMap(vp => ImageTools.spread_voxels_bin((vp.pos,vp),D3int.one, true)).
      groupByKey. // get rid of the extra position information
      filter(_._2.filter(_._2).size>0) // keep points with an original in them
    val graphTopology = grpVert.flatMap {
      case (cPos,pointList) =>
        val centralPoint = pointList.filter(_._2).head._1
        val neighborPoints = pointList.filter(!_._2).map(_._1)
        // only keep edges that connect different phases
        for(cNeighbor<-neighborPoints)
          yield Edge[Boolean](centralPoint.id,cNeighbor.id,cNeighbor.label != centralPoint.label)
    }
    Graph[VertexPoint,Boolean](fvertices,graphTopology)
  }

  def runStep(g: Graph[VertexPoint,Boolean]) = {
  }
}
