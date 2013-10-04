require("rJava")
.jinit(classpath="/Users/mader/Dropbox/TIPL/build/TIPL.jar",parameters="-Xmx4g -Djava.awt.headless=true")
.jaddClassPath("/Users/mader/Dropbox/TIPL/build/TIPL.jar")
# Test Functions
as.tipl.pf<-function(obj) .jcast(obj,new.class="tipl/formats/PureFImage$PositionFunction",check=T)
tiplPlane<-as.tipl.pf(.jnew("tipl/tests/TestFImages$DiagonalPlaneFunction"))
tiplLine<-as.tipl.pf(.jnew("tipl/tests/TestFImages$LinesFunction"))
tiplDots<-as.tipl.pf(.jnew("tipl/tests/TestFImages$DotsFunction"))
# this doesnt work :: not sure why .jcall("tipl/tests/TestFImages","tipl/formats/TImgRO","wrapIt",as.integer(10),tiplPlane)
tiplWrap<-function(c.fun,size=10) J("tipl.tests.TestFImages")$wrapIt(as.integer(size),c.fun)
planeImg<-tiplWrap(tiplPlane)
lineImg<-tiplWrap(tiplLine)
dotsImg<-tiplWrap(tiplDots)
# this gets very unhappy with AWT ReadTImg("/Users/mader/Dropbox/TIPL/test/foam/ufilt.tif")
ReadTImg<-function(filename) J("tipl.util.TImgTools")$ReadTImg(filename)
# read a slice from an image
getSlice<-function(c.img,c.slice,slice.type=3) {
  size<-c.img$getDim()
  cur.slice<-c.img$getPolyImage(as.integer(c.slice),as.integer(slice.type))
  matrix(cur.slice,nrow=size$x,byrow=T)
}
# show slice from an image
showSlice<-function(...) image(getSlice(...))

