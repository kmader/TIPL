require(tiff)
require(lattice)
require(plyr)
slice.to.df<-function(cur.slice) {
  xv<-c((-(dim(cur.slice)[1]-1)/2):((dim(cur.slice)[1]-1)/2))
  yv<-c((-(dim(cur.slice)[2]-1)/2):((dim(cur.slice)[2]-1)/2))
  xx<-as.vector(xv%*%t(rep(1,length(yv))))
  yy<-as.vector(t(yv%*%t(rep(1,length(xv)))))
  data.frame(x=yy,y=xx,val=as.vector(cur.slice)) # read in transposed
}
read.rdf<-function (filename,cut.edges=T,vox.size=1) {
  cur.imglist<-readTIFF(as.character(filename),info=T,all=T)
  z.dim<-length(cur.imglist)
  zv<-c((-(z.dim-1)/2):((z.dim-1)/2))
  staggered.data<-mapply(list, cur.imglist,zv, SIMPLIFY=F)
  cur.img<-ldply(staggered.data,function (x) cbind(slice.to.df(x[[1]]),z=x[[2]]),.parallel=T)
  if (cut.edges) { # removes the edges which makes the r and other values more meaningful
    max.rad.vals<-colwise(function(x) {if (max(x)>0) max(x) else 1})(cur.img[,c("x","y","z")])
    cur.img<-subset(cur.img, ((x/max.rad.vals$x)^2+(y/max.rad.vals$y)^2+(z/max.rad.vals$z)^2)<1)
  } 
  cur.img$x<-vox.size*cur.img$x
  cur.img$y<-vox.size*cur.img$y
  cur.img$z<-vox.size*cur.img$z
  attr(cur.img,"vox.size")<-vox.size
  cur.img
}
rdf.sub<-function(xd,n=2) {
  cvoxsize<-attr(xd,"vox.size")
  subset(xd,(round(x/cvoxsize)%%n==0) & (round(y/cvoxsize)%%n==0) & (round(z/cvoxsize)%%n==0))
}
