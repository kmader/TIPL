source('~/Dropbox/TIPL/src/R/commonReportFunctions.R')
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
#' Read in a tif stack as a radial distribution function
#'
#' Imports the file as an rdf which means the values are associated with x,y,z positions from -dim to dim
#' It also scales based on the voxel size if needed
#' 
#'
#' @param filename name of the tif file to open
#' @param cut.edges whether or not to remove the corners (default is true since they cause distortions when looking at single components)
#' @param vox.size for the voxel size to a specific value
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


#' Generate radial slices of an RDF function
#'
#' Takes the rdf (loaded as a data.frame) and splits it into radial slices with coordinates theta and phi
#' The n
#' 
#'
#' @param in.data the name of the input data
#' @param r.step number of steps in the r direction (equally counts in each group)
#' @param th.step number of steps in the theta direction (Z-xy plane)
#' @param phi.step number of steps in the phi direction (XY angle)
#' @param r.even r has evenly distributed counts (cut_number)
#' @param ang.even r has evenly distributed counts (cut_number)
rdf.rad.slices<-function(in.data,r.step=5,th.step=18,phi.step=18,r.even=T,ang.even=F) {
  good.cols<-names(in.data)
  good.cols<-good.cols[!(good.cols %in% "val")]
  if(r.even) rcut.fun<-cut_number
  else rcut.fun<-cut_interval
  if(ang.even) acut.fun<-cut_number
  else acut.fun<-cut_interval
  ddply.cutcols(cbind(in.data,
                      r=with(in.data,sqrt(x^2+y^2+z^2)),
                      th=with(in.data,180/pi*atan2(z,sqrt(x^2+y^2))),
                      phi=with(in.data,180/pi*atan2(y,x))),
                .(rcut.fun(r,r.step),acut.fun(th,th.step),
                  acut.fun(phi,phi.step),filename),
                cols=3, function(c.shell) {
                  # include the first row without the value field, and the average for the value field
                  data.frame(c.shell[1,good.cols],val=mean(c.shell[,"val"]))
                }
  )
}
