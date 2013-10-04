require("rJava")
.jinit(classpath="/Users/mader/Dropbox/TIPL/build/TIPL.jar",parameters="-Xmx4g -Djava.awt.headless=true")
.jaddClassPath("/Users/mader/Dropbox/TIPL/build/TIPL.jar")
# Test Functions
as.tipl.pf<-function(obj) .jcast(obj,new.class="tipl/formats/PureFImage$PositionFunction",check=T)
tiplPlane<-as.tipl.pf(.jnew("tipl/tests/TestFImages$DiagonalPlaneFunction"))
tiplLines<-as.tipl.pf(.jnew("tipl/tests/TestFImages$LinesFunction"))
# this doesnt work :: not sure why .jcall("tipl/tests/TestFImages","tipl/formats/TImgRO","wrapIt",as.integer(10),tiplPlane)
testWrap<-function(c.fun,size=10) J("tipl.tests.TestFImages")$wrapIt(as.integer(size),c.fun)
planeImg<-testWrap(tiplPlane)
lineImg<-testWrap(tiplLine)
# this gets very unhappy with AWT ReadTImg("/Users/mader/Dropbox/TIPL/test/foam/ufilt.tif")
ReadTImg<-function(filename) J("tipl.util.TImgTools")$ReadTImg(filename)



