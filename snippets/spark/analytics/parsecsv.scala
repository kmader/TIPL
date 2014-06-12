val allLacun=sc.textFile("/gpfs/home/mader/Data*/*/*/lacun_4.csv")
val lacFile=sc.textFile("/gpfs/home/mader/Data10/B910/UJAX_F2_1218/lacun_4.csv")
val lacAsCols=lacFile.map(_.split(','))
val lacNoHeader=lacAsCols.filter(! _(0).startsWith("//"))
val lacHeader=lacAsCols.filter(_(0).startsWith("//"))

val headers=lacHeader.take(2)
val txtHeader=headers(1).map(_.toUpperCase.trim().replaceAll("//",""))

val data=lacNoHeader.map(_.zip(txtHeader))

val valData=data.map(lineObj => lineObj.map(cLine => (cLine._2,cLine._1.toDouble)))

