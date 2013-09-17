library("flexclust")
dm.as.df<-function(distDF) {
  # turns a distance matrix (with column showing cluster assignment)
  # into a data.frame with 3 columns showing the distance from each cluster
  distMat<-distDF[,!(names(ldist)=="cluster")]
  for(i in 1:ncol(distMat)) {                             
    cDF<-cbind(cluster=i,rcluster=distDF$cluster,dist=distMat[,i])
    if(i==1) {dfNM<-cDF}
    else {dfNM<-rbind(dfNM,cDF)}
  }
  dfNM<-data.frame(dfNM)
  names(dfNM)<-c("fromcluster","cluster","dist")
  dfNM
}
distSpatAndAngle<-function(xdf,centerdf,angleWeight=3.30826e-05/3,pos=c(1:3),dir=c(4:6),euDistFun=distEuclidean) {
  # angle weight controls how many voxels a orthogonal alignment costs
  # drop=F is crazy important otherwise R converts matrices into vectors and numerics which breaks everything
  xd<-as.matrix(xdf)
  centers<-as.matrix(centerdf)
  centLen<-nrow(centers)
  if (length(pos)>0) {
    distMat<-euDistFun(as.matrix(xd[,pos,drop=F]),centers[,pos,drop=F])
  } else {
    distMat<-matrix(0,nrow(xd),centLen)
  }
  if (angleWeight>0) {
    matchDir<-function(curCent) {
      curCentVec<-centers[curCent,dir,drop=F]
      curCentVec<-curCentVec/sqrt(sum(curCentVec**2))
      apply(xd[,dir,drop=F],1,function(x) {abs(sum(x*curCentVec))})
    }
    for (k in 1:centLen) {
      distMat[,k]<-distMat[,k]+angleWeight*(1-matchDir(k))
    }
  }
  #print(paste("distSpat out",nrow(distMat),ncol(distMat)))
  distMat
}

soDistSpatAndAngle<-function(angleWeight,pos=c(1:3),dir=c(4:6)) {
  function(xdf,centerdf) {distSpatAndAngle(xdf,centerdf,angleWeight=angleWeight,pos=pos,dir=dir)}
}
centSpatAndAngle<-function(xdf,pos=c(1:3),dir=c(4:6),posMeanFcn=colMeans) {
  #print(paste("spatAngle:",nrow(xdf),ncol(xdf)))
  allMeans<-t(as.matrix(colMeans(xdf)))
  if (length(pos)>0) {
    meanCent<-posMeanFcn(xdf[,pos,drop=F])
    allMeans[,pos]<-meanCent
  }
  meanDir<-alignMatrix(xdf[,dir,drop=F])$vectors[1,]
  allMeans[,dir]<-meanDir
  
  allMeans
}
soCentSpatAndAngle<-function(pos=c(1:3),dir=c(4:6)) {
  function(xdf) {centSpatAndAngle(xdf,pos=pos,dir=dir)}
}

testPlaneData<-function(centFun=centPlane,distFun=distPlane,cfun=function(x){x}) {
  tx<-c(1:10)
  ty<-c(c(1:5),c(10:6))
  plane3<-cfun(data.frame(x=tx,y=ty,z=tx-3*ty+15))
  plane2<-cfun(data.frame(x=ty,y=tx,z=-2*tx+3*ty-40))
  plane1<-cfun(data.frame(x=ty,y=tx,z=0.001*tx-0.002*ty+20))
  eqp1<-centFun(plane1)
  funInfo<-function(name,indat,outdat) {print(paste(name,": in-",class(indat),"(",nrow(indat),ncol(indat),") out-",class(outdat),"(",nrow(outdat),ncol(outdat),")=",paste(outdat,collapse=",")))}
  funInfo("centFune",plane1,eqp1)
  eqp2<-centFun(plane2)
  eqp3<-centFun(plane3)
  allPos<-rbind(plane1,plane2,plane3)
  eqpA<-centFun(allPos)
  eqp<-rbind(eqp1,eqp2,eqp3,eqpA)
  oVal<-distFun(allPos,eqp1)
  funInfo("distFun",allPos,oVal)
  oVal
}
centPlane<-function(xdf) {
  # converts a list a points into a best fitting plane (x,y,z) -> (a,b,c,d) -> (th,phi,d)
  # test positions allpos<-cbind(c(1:10),c(10:1),c(1:10))
  
  if (length(as.matrix(xdf))<4) {
    cOut<-c(0,0,xdf[3])
  } else {
    xd<-as.matrix(xdf)
   asdf<-data.frame(x=xd[,1],y=xd[,2],z=xd[,3])
    coefs<-lm(z~x+y,data=asdf)$coeff
    
    a<--1*coefs[["x"]]
    b<--1*coefs[["y"]]
    c<-1
    d<--1*coefs[1]
    if (is.na(a)) {a<-0}
    if (is.na(b)) {b<-0}
    if (is.na(d)) {d<-0}
    d<-d/sqrt(a^2+b^2+c^2)
    
    th<-asin(a/sqrt(a^2+b^2))
    phi<-atan2(c,sqrt(a^2+b^2))
    cOut<-c(th,phi,d)
  }
  #print(paste("cplane:",nrow(xdf),ncol(xdf),paste(cOut,collapse=",")))
  t(as.matrix(cOut))
}
distPlane<-function(xdf,centerdf) {
  # a plane is defined by a*x+b*y+c*z+d=0
  # we assume a,b,c are normalized and thus c=sqrt(1-a^2-b^2)
  # to ensure the right parameters we use a=sin(th)*cos(phi), b=cos(th)*cos(phi)
  # thus the center of a plane can be represented by (a,b,d)
  # the points are x,y,z and the distance metric is a*x+b*y+c*z+d
  # drop=F is crazy important otherwise R converts matrices into vectors and numerics which breaks everything
  # test code
  # two horizontal p;lanes one at z=0 the other at z=20
  # mycenters<-rbind(c(0,3.14/2,0),c(0,3.14/2,-20))
  # test ositions mybos<-cbind(c(1:10),c(1:10),c(1:10))

  #print(paste("distPlane:x",nrow(xdf),ncol(xdf),"cents:",nrow(centerdf),ncol(centerdf)))
  xd<-as.matrix(xdf)
  centers<-as.matrix(centerdf)
  
  needsFix<-F
  if (prod(dim(centers))<4) {
    #print(paste("needs-fixin:",nrow(centerdf),ncol(centerdf)))
    centers<-as.matrix(rbind(centerdf,centerdf))
    needsFix<-T
  } 
  
  centLen<-nrow(centers)
  # transform centers from (th,phi,d) -> (a,b,c,d)
  tcenters<-cbind(sin(centers[,1])*cos(centers[,2]),cos(centers[,1])*cos(centers[,2]))
  tcenters<-cbind(tcenters,sqrt(1-colSums(t(tcenters^2))),centers[,3])
  distMat<-matrix(0,nrow(xd),centLen)
    matchPlane<-function(curCent) {
      cFun<-function(x) {
        abs(x[1]*tcenters[curCent,1,drop=F]+x[2]*tcenters[curCent,2,drop=F]+
                                               x[3]*tcenters[curCent,3,drop=F]+tcenters[curCent,4,drop=F])
      }
      apply(xd,1,cFun)
    }
    for (k in 1:centLen) {
      distMat[,k]<-matchPlane(k)
    }
  if (needsFix) {
    
    outVal<-distMat[,1,drop=F]
    # absolutely no idea why i need this command, eek!
    #outVal<-cbind(outVal,outVal)
  } else {
    outVal<-distMat
  }
  #print(paste("distPlane:out",nrow(outVal),ncol(outVal)))
  outVal
}


orientMatch<-function(poros.table,labeled.table,posV=c("POS_X","POS_Y","POS_Z"),dirV=c("PCA3_X","PCA3_Y","PCA3_Z"),fixOrientation=T) {
  require("tree")
  kmeansData<-data.frame(x=poros.table[[posV[1]]],y=poros.table[[posV[2]]],z=poros.table[[posV[3]]],ax=poros.table[[dirV[1]]],ay=poros.table[[dirV[2]]],az=poros.table[[dirV[3]]])
  if (fixOrientation) {kmeansData<-koMeans.FixOrientation(kmeansData)}
  llTree<-tree(cluster~ax+ay+az,labeled.table)
  kmeansData$cluster<-as.factor(round(predict(llTree,kmeansData)))
  kmeansData
}
kccaDirPlane<-function(inData,nGroups,angleWeight,inTh=0,inPhi=pi/2,inThV=pi/2,inPhiV=pi/2,iters=3,sDir=c(0,0,1)) {
  # make the startup plans automatically rather than using random input points
  # also use the angle information as well
  saDistPlane<-function(xdf,centerdf) {
    distSpatAndAngle(xdf,centerdf,angleWeight,euDistFun=distPlane)
  }
  saCentPlane<-function(xdf) {
    centSpatAndAngle(xdf,posMeanFcn=centPlane)
  }
  saKccaFam<-kccaFamily(dist=saDistPlane,cent=saCentPlane) # special orientation sensitive kmeans
  zrange<-range(inData[,3])
  inVecs<-do.call(rbind,lapply(seq(zrange[1],zrange[2],length.out=nGroups),function(x) {c(runif(1,inTh-inThV,inTh+inThV),runif(1, inPhi-inPhiV,inPhi+inPhiV),-1*x,sDir)}))
  kCtrl<-as(list(iter=iters), "flexclustControl")
  kmeansResult<-kcca(inData,inVecs,family=saKccaFam,control=kCtrl)
}

kccaPlane<-function(inData,nGroups,inTh=0,inPhi=pi/2,inThV=pi/2,inPhiV=pi/2,iters=3,angleWeight=0) {
  # make the startup plans automatically rather than using random input points
  saKccaFam<-kccaFamily(dist=distPlane,cent=centPlane) # special orientation sensitive kmeans
  zrange<-range(inData[,3])
  inVecs<-do.call(rbind,lapply(seq(zrange[1],zrange[2],length.out=nGroups),function(x) {c(runif(1,inTh-inThV,inTh+inThV),runif(1, inPhi-inPhiV,inPhi+inPhiV),-1*x)}))
  kCtrl<-as(list(iter=iters), "flexclustControl")
  kmeansResult<-kcca(inData,inVecs,family=saKccaFam,control=kCtrl)
}
planeKmeans<-function(poros.table,k,angleWeight=3.30826e-05,posV=c("POS_X","POS_Y","POS_Z"),fixOrientation=T,kccaFcn=kccaDirPlane,iters=100,dirV=c("PCA3_X","PCA3_Y","PCA3_Z")) {
  kmeansData<-data.frame(x=poros.table[[posV[1]]],y=poros.table[[posV[2]]],z=poros.table[[posV[3]]],ax=poros.table[[dirV[1]]],ay=poros.table[[dirV[2]]],az=poros.table[[dirV[3]]])
  kmeansMatrix<-as.matrix(kmeansData)
  kmeansResult<-kccaFcn(kmeansMatrix,k,angleWeight,iters=iters)
  outData<-cbind(kmeansData,as.factor(attr(kmeansResult,"cluster")))
  names(outData)[ncol(outData)]<-"cluster"
  attr(outData,"kmeans")<-kmeansResult
  outData
}
orientKmeans<-function(poros.table,k,angleWeight=3.30826e-05,posV=c("POS_X","POS_Y","POS_Z"),dirV=c("PCA3_X","PCA3_Y","PCA3_Z"),fixOrientation=T) {
  kmeansData<-data.frame(x=poros.table[[posV[1]]],y=poros.table[[posV[2]]],z=poros.table[[posV[3]]],ax=poros.table[[dirV[1]]],ay=poros.table[[dirV[2]]],az=poros.table[[dirV[3]]])
  if (fixOrientation) {kmeansData<-koMeans.FixOrientation(kmeansData)}
  kmeansMatrix<-as.matrix(kmeansData)
  saKccaFam<-kccaFamily(dist=soDistSpatAndAngle(angleWeight),cent=centSpatAndAngle) # special orientation sensitive kmeans
  kmeansResult<-kcca(kmeansMatrix,k,family=saKccaFam)
  outData<-cbind(kmeansData,as.factor(attr(kmeansResult,"cluster")))
  names(outData)[ncol(outData)]<-"cluster"
  attr(outData,"kmeans")<-kmeansResult
  outData
}
rawToFrame<-function(poros.table,k,angleWeight=1,posV=c("POS_X","POS_Y","POS_Z"),otherV=c("AISO"),dirV=c("PCA3_X","PCA3_Y","PCA3_Z"),fixOrientation=T,otherTransform=function(x) {x}) {
  # Normalize the position variables before comparison (minus mean over standard deviation) useful for adding density, volume, etc to mix
  buildTable<-function(inTable) {cbind(data.frame(x=poros.table[[posV[1]]],y=poros.table[[posV[2]]],z=poros.table[[posV[3]]]),inTable,data.frame(ax=poros.table[[dirV[1]]],ay=poros.table[[dirV[2]]],az=poros.table[[dirV[3]]]))}
  kmeansData<-poros.table[,names(poros.table) %in% otherV,drop=F]
  for (i in 1:ncol(kmeansData)) {
    tempCol<-otherTransform(kmeansData[,i])
    kmeansData[,i]<-(tempCol-mean(tempCol))/sd(tempCol)
  }
  kmeansData<-buildTable(kmeansData)
  iPos<-c((length(posV)+1):(ncol(kmeansData)-length(dirV)))
  iDir<-c((ncol(kmeansData)-length(dirV)+1):ncol(kmeansData))
  kmeansData
}
orientNormKmeans<-function(poros.table,k,angleWeight=1,posV=c("POS_X","POS_Y","POS_Z"),otherV=c("AISO"),dirV=c("PCA3_X","PCA3_Y","PCA3_Z"),fixOrientation=T,otherTransform=function(x) {x}) {
  # Normalize the position variables before comparison (minus mean over standard deviation) useful for adding density, volume, etc to mix
  buildTable<-function(inTable) {cbind(data.frame(x=poros.table[[posV[1]]],y=poros.table[[posV[2]]],z=poros.table[[posV[3]]]),inTable,data.frame(ax=poros.table[[dirV[1]]],ay=poros.table[[dirV[2]]],az=poros.table[[dirV[3]]]))}
  trivialTable<-function() {cbind(data.frame(x=poros.table[[posV[1]]],y=poros.table[[posV[2]]],z=poros.table[[posV[3]]],ax=poros.table[[dirV[1]]],ay=poros.table[[dirV[2]]],az=poros.table[[dirV[3]]]))}
  
  if (length(otherV)>0) {
    kmeansData<-poros.table[,names(poros.table) %in% otherV,drop=F]
    for (i in 1:ncol(kmeansData)) {
      tempCol<-otherTransform(kmeansData[,i])
      kmeansData[,i]<-(tempCol-mean(tempCol))/sd(tempCol)
    }
    kmeansData<-buildTable(kmeansData)
  } else { # with no other metrics
    kmeansData<-trivialTable()
  }
  
  iPos<-c((length(posV)+1):(ncol(kmeansData)-length(dirV)))
  iDir<-c((ncol(kmeansData)-length(dirV)+1):ncol(kmeansData))
  kmeansMatrix<-as.matrix(kmeansData)
  #kmeansMatrix
  saKccaFam<-kccaFamily(dist=soDistSpatAndAngle(angleWeight,pos=iPos,dir=iDir),cent=soCentSpatAndAngle(pos=iPos,dir=iDir)) # special orientation sensitive kmeans
  kmeansResult<-kcca(kmeansMatrix,k,family=saKccaFam)
  # Recreate kmeansdata with the raw value columns (not normalized)
  kmeansData<-poros.table[,names(poros.table) %in% otherV,drop=F]
  kmeansData<-buildTable(kmeansData)
  outData<-cbind(kmeansData,as.factor(attr(kmeansResult,"cluster")))
  names(outData)[ncol(outData)]<-"cluster"
  attr(outData,"kmeans")<-kmeansResult
  outData
}
okMeans.Test<-function(roiData,grpList,wgtList,cores=2,pathPrefix="") {
  library("foreach")
  library(doMC)
  registerDoMC(cores)
  foreach(dWeight=wgtList) %dopar% { # double parallel is of course bad form, but in most cases one of the lists is singular
    foreach(seedGroups=grpList) %dopar% {
      system.time(newSubSet<-orientKmeans(roiData,seedGroups,angleWeight=dWeight))
      cWeight<-sprintf("%2.0f",dWeight*1e6)
      okMeans.SaveGraphs(newSubSet,pathPrefix,paste(cWeight,"G",seedGroups,sep="_"))
     }
  }
}

okMeans.SaveGraphs<-function(inSubSet,pathPrefix="",baseName="okMeans",fixOrientation=T,minCnt=1000) {
  jDots<-simpleTheme(pch='.')
  if (nrow(inSubSet)<minCnt) jDots<-simpleTheme()
  
  newSubSet<-inSubSet
  if (fixOrientation) {newSubSet<-koMeans.FixOrientation(inSubSet)}
  aspdf(cloud(az ~ ax * ay | cluster, data = newSubSet,groups=cluster,par.settings=jDots),paste(pathPrefix,"orient_",baseName,sep=""))
  aspdf(cloud(z ~ x * y | cluster, data = newSubSet,groups=cluster,par.settings=jDots),paste(pathPrefix,"pos_",baseName,sep=""))
  aspdf(cloud(az ~ ax * ay , data = newSubSet,groups=cluster,par.settings=jDots),paste(pathPrefix,"corient_",baseName,sep=""))
  aspdf(cloud(z ~ x * y, data = newSubSet,groups=cluster,par.settings=jDots),paste(pathPrefix,"cpos_",baseName,sep=""))
  aspdf(xyplot(z ~ x | cluster, data = newSubSet,groups=cluster,par.settings=jDots),paste(pathPrefix,"posxz_",baseName,sep=""))
  aspdf(xyplot(y ~ x | cluster, data = newSubSet,groups=cluster,par.settings=jDots),paste(pathPrefix,"posxy_",baseName,sep=""))
  aspdf(barchart(attr(newSubSet,"kmeans")),paste(pathPrefix,"stats_",baseName,sep=""))
  aspdf(ggplot(newSubSet,aes(x=ax,y=ay,color=cluster))+geom_jitter(alpha=0.5)+theme_bw(),paste(pathPrefix,"gorient_xy_",baseName,sep=""))
  aspdf(ggplot(newSubSet,aes(x=ax,y=az,color=cluster))+geom_jitter(alpha=0.5)+theme_bw(),paste(pathPrefix,"gorient_xz_",baseName,sep=""))
  aspdf(ggplot(newSubSet,aes(x=ax,y=ay,color=as.factor(cluster)))+geom_density2d()+theme_bw(),paste(pathPrefix,"gorient_densxy_",baseName,sep=""))
  aspdf(ggplot(newSubSet,aes(x=ax,y=az,color=as.factor(cluster)))+geom_density2d()+theme_bw(),paste(pathPrefix,"gorient_densxz_",baseName,sep=""))
  aspdf(ggplot(newSubSet,aes(x=x,y=y,color=cluster))+geom_jitter(alpha=0.5)+theme_bw(),paste(pathPrefix,"gpos_xy_",baseName,sep=""))
  aspdf(ggplot(newSubSet,aes(x=x,y=z,color=cluster))+geom_jitter(alpha=0.5)+theme_bw(),paste(pathPrefix,"gpos_xz_",baseName,sep=""))
  # cluster colored
  aspdf(ggplot(newSubSet,aes(x=x,y=y,xend=x+ax/cf,yend=y+ay/cf,color=z))+geom_segment()+facet_wrap(~cluster)+theme_bw(),paste(pathPrefix,"seg_xy_",baseName,sep=""))
  aspdf(ggplot(newSubSet,aes(x=x,y=z,xend=x+ax/cf,yend=z+az/cf,color=y))+geom_segment()+facet_wrap(~cluster)+theme_bw(),paste(pathPrefix,"seg_xz_",baseName,sep=""))
  aspdf(ggplot(newSubSet,aes(x=y,y=z,xend=y+ay/cf,yend=z+az/cf,color=x))+geom_segment()+facet_wrap(~cluster)+theme_bw(),paste(pathPrefix,"seg_yz_",baseName,sep=""))
  
  if ("Density"%in%colnames(newSubSet)) {
    aspdf(ggplot(newSubSet,aes(x=180/pi*atan2(ay,ax),y=Density,color=as.factor(cluster)))+geom_jitter(alpha=0.3)+geom_density2d()+scale_y_log10(),paste(pathPrefix,"dens_angle_",baseName,sep=""))
  }
  
  write.csv(newSubSet,paste(pathPrefix,"stats_",baseName,".csv",sep=""))
}
# Utility Functions

aspdf<-function(model,name="test") {
  pdf(paste(name,".pdf",sep=""))
  print(model)
  dev.off()
}
okMeans.FindSlices<-function(newSubSet,pathPrefix="",inBase="okMeans",pr=T,inModel=lm) {
  fitPlane<-function(inSubSet,model=inModel) {
    cFit<-lm(z~x+y,data=inSubSet)
    cbind(inSubSet,fitZ=cFit$fitted.values,r2=rep(summary(cFit)$r.squared,nrow(inSubSet)))
  }
  bvr<-ddply(newSubSet,.(cluster),fitPlane,.parallel=pr)
  outPlot<-ggplot(cbind(bvr,fitLabel=as.factor(paste(bvr$cluster,round(bvr$r2*100),sep=":"))),aes(x=x))
  outPlotY<-ggplot(cbind(bvr,fitLabel=as.factor(paste(bvr$cluster,round(bvr$r2*100),sep=":"))),aes(x=y))
  tAdd<-function(inPlot,baseName) {aspdf(inPlot+geom_jitter(aes(y=z),color="red",size=0.4)+geom_jitter(aes(y=fitZ),color="black",size=0.3)+facet_wrap(~fitLabel,as.table=T),paste(pathPrefix,"fits_",inBase,baseName,sep=""))}
  tAdd(outPlot,"xz")
  tAdd(outPlotY,"yz")
}
# Perform alignment analysis on subtable
alignFunc<-function(subDF,key=0) {
  alignMatrix(subDF[,(names(subDF) %in% c("PCA1_X","PCA1_Y","PCA1_Z"))],key=key)
}

alignMatrix<-function(subMatrix,key=0) {
  dfCOV<-cov.wt(subMatrix,center=c(0,0,0))$cov
  vals=c(0,0,0)
  eTrans<-list(values=vals)
  eTrans$vectors<-diag(3)
  try(eTrans<-eigen(dfCOV))
  eTrans$length<-nrow(subMatrix)
  eTrans$align<-(max(eTrans$values)-min(eTrans$values))/max(eTrans$values)
  eTrans$key<-key
  eTrans
}
koMeans.FixOrientation<-function(inSubSet) {
  newSubSet<-inSubSet
  newSubSet$ax<-inSubSet$ax*sign(inSubSet$az)
  newSubSet$ay<-inSubSet$ay*sign(inSubSet$az)
  newSubSet$az<-inSubSet$az*sign(inSubSet$az)
  newSubSet
}