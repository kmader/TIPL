import org.apache.spark.util.Vector
val allEdges=sc.textFile("/gpfs/home/mader/Data10/*/*/lacun_edge_ptxt.csv")

val edgeFile=sc.textFile("/gpfs/home/mader/Data10/B910/UJAX_F2_1218/lacun_edge.csv")
val edgeAsCols=edgeFile.map(_.split(','))
val edgeNoHeader=edgeAsCols.filter(! _(0).startsWith("//"))
val edgeAsVec=edgeNoHeader.map(p => new Vector(p.map(_.toDouble)))
val edgeHeader=edgeAsCols.filter(_(0).startsWith("//"))

val headers=edgeHeader.take(2)
val txtHeader=headers(1).map(_.replaceAll("//","").toUpperCase.trim())

val data=edgeNoHeader.map(_.zip(txtHeader))

val valData=data.map(lineObj => lineObj.map(cLine => (cLine._2,cLine._1.toDouble)))

