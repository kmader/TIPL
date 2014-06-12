import org.apache.spark.util.Vector
val allLacun=sc.textFile("/gpfs/home/mader/Data*/*/*/lacun_4.csv")
val lacAsCols=allLacun.map(_.split(','))
val lacNoHeader=lacAsCols.filter(! _(0).startsWith("//"))
val lacHeader=lacAsCols.filter(_(0).startsWith("//"))

val headers=lacHeader.take(2)
val txtHeader=headers(1).map(_.replaceAll("//","").toUpperCase.trim())


def asDouble(chunk: String): Double = try { chunk.toDouble } catch { case _ => -1}
def parseVector(line: Array[String]): Vector = { try { new Vector(line.map(_.toDouble)) } catch { case _ => Vector() }}


val lacVec=lacNoHeader.map(parseVector(_))
val goodCount=lacVec.take(1)(0).length
val goodLac=lacVec.filter(_.length==goodCount)

val goodCols=txtHeader.zipWithIndex.filter(p => Array("VOLUME","PROJ_PCA1","PROJ_PCA2","PROJ_PCA3") contains p._1).map(_._2)


val intCols=goodLac.map(row => new Vector(goodCols.map(colIndex => row(colIndex)))).cache()



def closestPoint(p: Vector, centers: Array[Vector]): Int = {
    var index = 0
    var bestIndex = 0
    var closest = Double.PositiveInfinity
  
    for (i <- 0 until centers.length) {
      val tempDist = p.squaredDist(centers(i))
      if (tempDist < closest) {
        closest = tempDist
        bestIndex = i
      }
    }
  
    bestIndex
  }
val K = 10
// just use the first 10 points
val kPoints=intCols.take(K)

val closest = intCols.map(p => (closestPoint(p,kPoints),(p,1)))
val pointStats = closest.reduceByKey{case ((x1, y1), (x2, y2)) => (x1 + x2, y1 + y2)}
val newPoints = pointStats.map {pair => (pair._1, pair._2._1 / pair._2._2)}.collectAsMap()
      
      tempDist = 0.0
      for (i <- 0 until K) {
        tempDist += kPoints(i).squaredDist(newPoints(i))
      }
      
      for (newP <- newPoints) {
        kPoints(newP._1) = newP._2
      }

