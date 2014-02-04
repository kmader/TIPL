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
#' @param supress.warnings supresses warnings since there are hundreds which come from the TIPL specific tags " TIFFReadDirectory: Unknown field with tag 994 (0x3e2) encountered"
read.rdf<-function (filename,cut.edges=T,vox.size=1,supress.warnings=T) {
  if (supress.warnings) im.read.fcn<-function(...) suppressWarnings(readTIFF(...))
  else im.read.fcn<-readTIFF
  cur.imglist<-im.read.fcn(as.character(filename),info=T,all=T)
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
  if(is.null(attr(xd,"vox.size"))) {
    print("vox.size is empty, replacing with 1")
    attr(xd,"vox.size")<-1
  }
  cvoxsize<-attr(xd,"vox.size")
  subset(xd,(round(x/cvoxsize)%%n==0) & (round(y/cvoxsize)%%n==0) & (round(z/cvoxsize)%%n==0))
}


#' A wrapper for the cut functions which makes them better behaved when n==1
#' This returns the average value whenever n==1
#'
#' @param cut_fcn is the function to wrap
cut_wrapper<-function(cut_fcn) {
  function(x, n = NULL,...) {
    if(n==1) rep(mean(x),length(x))
    else cut_fcn(x,n=n,...)
  }
}
icut_interval<-cut_wrapper(cut_interval)
icut_number<-cut_wrapper(cut_number)
#' Adds the appropriate angles to the rdf data.frame
#'
#' Adds R, theta (th) and phi to the data.frame
#'
#' @param in.data the name of the input data
rdf.with.ang<-function(in.data) cbind(in.data,
                                      r=with(in.data,sqrt(x^2+y^2+z^2)),
                                      th=with(in.data,180/pi*acos(z/sqrt(x^2+y^2+z^2))),
                                      phi=with(in.data,180/pi*atan2(y,x)))
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
rdf.rad.slices<-function(in.data,r.step=5,th.step=12,phi.step=18,r.even=T,ang.even=F) {
  good.cols<-names(in.data)
  good.cols<-good.cols[!(good.cols %in% "val")]
  
  if(r.even) rcut.fun<-icut_number
  else rcut.fun<-icut_interval
  if(ang.even) acut.fun<-icut_number
  else acut.fun<-icut_interval
  out.df<-ddply.cutcols(rdf.with.ang(in.data),
                        .(rcut.fun(r,r.step),acut.fun(th,th.step),
                          acut.fun(phi,phi.step),filename),
                        cols=3, function(c.shell) {
                          # include the first row without the value field, and the average for the value field
                          data.frame(c.shell[1,good.cols],
                                     val=mean(c.shell[,"val"]),
                                     val.std=sd(c.shell[,"val"]),
                                     val.min=min(c.shell[,"val"]),
                                     val.max=max(c.shell[,"val"]),
                                     cnt=nrow(c.shell))
                        }
  )
  out.df$x<-with(out.df,r*sin(th*pi/180)*cos(phi*pi/180))
  out.df$y<-with(out.df,r*sin(th*pi/180)*sin(phi*pi/180))
  out.df$z<-with(out.df,r*cos(th*pi/180))
  out.df
}


#' Calculates the angular deviation between the a list of points in x,y,z
#' and a given unit vector direction given in theta and phi
#' ang.dist is the distance away from the given vector the line is
#' vec.dist is the distance along the line
#' 
#' @param th theta angle for unit vector (zr) in radians
#' @param phi phi angle for the vector (xy) in radians
#' @param in.df the data.frame containing x,y,z to be compared
#' @param include.origin include the origin in the final results (replace na with 0)
along.line<-function(th,phi,in.df,include.origin=T) {
  # unit vector (x,y,z) with r=1
  nacos<-function(...) suppressWarnings(acos(...)) # don't make a bunch of stupid warnings about nan
  c.pos<-c(sin(th)*cos(phi),
           sin(th)*sin(phi),
           cos(th))
  r.val<-with(in.df,sqrt(x^2+y^2+z^2))
  vec.dist<-with(in.df,c.pos[1]*x+c.pos[2]*y+c.pos[3]*z)
  ang.dist<-nacos(abs(vec.dist/r.val))
  out.df<-cbind(in.df,ang.dist=ang.dist,vec.dist=vec.dist,
                uv.x=c.pos[1],uv.y=c.pos[2],uv.z=c.pos[3])
  if (include.origin) out.df[which(is.na(out.df$ang.dist)),"ang.dist"]<-0
  out.df
}

line.scan<-function(in.df,th.steps=5,phi.steps=NULL,ang.thresh=NULL,.parallel=T,fit.thick=F,flat.fact=2) {
  if(is.null(phi.steps)) phi.steps<-th.steps*4
  th.list<-seq(0,pi/2,length.out=th.steps)
  phi.list<-seq(-pi,pi,length.out=phi.steps)
  if(is.null(ang.thresh)) ang.thresh<-mean(c(diff(phi.list),diff(th.list)))/5
  good.cols<-names(in.df)
  good.cols<-good.cols[!(good.cols %in% "val")]
  ddply(in.df,.(filename),.parallel=.parallel,function(c.file) {
    o.linevals<-ddply(expand.grid(phi=phi.list,th=th.list),.(phi,th),function(c.start) {
      c.inline<-subset(along.line(c.start[1,"th"],c.start[1,"phi"],c.file),ang.dist<ang.thresh)
      fit.res<-list(w.fit=0,R2=0)
      if(fit.thick) {
        tryCatch({
          fit.res<-estimate.period(function(...) flat.fit.fun(...,flat.fact=flat.fact),c.inline)
        },
                 error=function(e) print(paste("Can't fit well",e)))
      } 
      data.frame(uv.x=c.inline[1,"uv.x"],
                 uv.y=c.inline[1,"uv.y"],
                 uv.z=c.inline[1,"uv.z"],
                 val=mean(c.inline[,"val"]),
                 val.std=sd(c.inline[,"val"]),
                 val.min=min(c.inline[,"val"]),
                 val.max=max(c.inline[,"val"]),
                 cnt=nrow(c.inline),
                 w.fit=fit.res$w.fit,
                 R2=fit.res$R2
                 )
    })
    cbind(c.file[1,],o.linevals)
  })
}


# a few simple tools for estimating thickness
sin.fit.fun<-function(x,w,offset,min.val,max.val) {
  (max.val-min.val)*(sin((x+offset)/w*pi)+1)/2+min.val
}
clip.v<-function(x,minv,maxv) {
  x[x>maxv]<-maxv
  x[x<minv]<-minv
  x
}
flat.fit.fun<-function(x,w,offset,min.val,max.val,flat.fact=1) {
  (max.val-min.val)*(clip.v(flat.fact*sin((x+offset)/w*pi),-1,1)/2+0.5)+min.val
}
estimate.period<-function(fun,in.line,max.val=1,min.val=0) {
  fit.vals<-nls(
    val~fun(vec.dist,w.fit,offset.fit,min.val,max.val),
    data=in.line,
    start=list(w.fit=diff(range(in.line$vec.dist))/2,offset.fit=0),
    trace=F)
  out.list<-as.list(fit.vals$m$getPars())
  out.list$R2<-fit.vals$m$Rmat()[2,2]^2
  out.list
}

library(testthat)
# some simple tests to ensure the along line code works correctly
expect_true( { # check that you get the correct number of points
  c.test<-subset(along.line(0,0,expand.grid(x=-2:2,y=-2:2,z=-2:2)),ang.dist<pi/8)
  nrow(c.test)==5 # vertical line
})
expect_true( { # check that the z axis is ok
  c.test<-subset(along.line(0,0,expand.grid(x=-2:2,y=-2:2,z=-2:2)),ang.dist<pi/8 )
  ((max(c.test$z)-min(c.test$z))==4) & 
    ((max(c.test$x)-min(c.test$x))==0) &
    ((max(c.test$y)-min(c.test$y))==0) 
})
expect_true( { # check that the x axis is ok
  c.test<-subset(along.line(pi/2,0,expand.grid(x=-2:2,y=-2:2,z=-2:2)),ang.dist<pi/8)
  ((max(c.test$x)-min(c.test$x))==4) & 
    ((max(c.test$z)-min(c.test$z))==0) &
    ((max(c.test$y)-min(c.test$y))==0) 
})
expect_true( { # check that the y axis is ok
  c.test<-subset(along.line(pi/2,pi/2,expand.grid(x=-2:2,y=-2:2,z=-2:2)),ang.dist<pi/8)
  ((max(c.test$x)-min(c.test$x))==0) & 
    ((max(c.test$z)-min(c.test$z))==0) &
    ((max(c.test$y)-min(c.test$y))==4) 
})
