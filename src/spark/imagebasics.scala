val textImg=sc.textFile("2011text/block*.csv")
val rImg=textImg.map(_.split(",")).map(cLine => (cLine(0).toInt,cLine(1).toInt,cLine(2).toInt,cLine(3).toDouble))


