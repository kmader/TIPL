library(plyr)
compare.foam<-function(cDir,goldFile='glpor_1.csv',kevinFile='clpor_2.csv') {
  kbubs<-read.csv(paste(cDir,kevinFile,sep='/'),skip=1)
  gbubs<-read.csv(paste(cDir,goldFile,sep='/'),skip=1)
  
  gbubs<-compare.foam.clean(gbubs)
  kbubs<-compare.foam.clean(kbubs)
  compare.frames(gbubs,kbubs)
}
# calculate the bubble to bubble spacing
calc.track.statistics<-function(in.roi) ddply(in.roi,.(sample),function(c.sample) data.frame(mean_velocity=mean(c.sample$DIR_Z),
                                                                                        mean_obj_spacing=(with(c.sample,rng(POS_X)*rng(POS_Y)*rng(POS_Z))/nrow(c.sample))^(0.33),
                                                                                        sd_vel_x=sd(c.sample$DIR_X),
                                                                                        sd_vel_y=sd(c.sample$DIR_Y),
                                                                                        sd_vel_z=sd(c.sample$DIR_Z)
                                                                                        ))
# fancy edge data reader
read.edge<-function(x) {
  edge.data<-read.csv(x,skip=1)
  names(edge.data)[1]<-"Component.1"
  edge.data
}
# Add MChain to edges
edges.append.mchain<-function(in.edges,in.bubbles) {
  sub.bubbles<-in.bubbles[,names(in.bubbles) %in% c("sample","LACUNA_NUMBER","MChain")]
  colnames(sub.bubbles)[colnames(sub.bubbles)=="MChain"]<-"MChain.1"
  o.merge1<-merge(in.edges,sub.bubbles,
                  by.x=c("sample","Component.1"),by.y=c("sample","LACUNA_NUMBER"))
  sub.bubbles<-in.bubbles[,names(in.bubbles) %in% c("sample","LACUNA_NUMBER","MChain")]
  colnames(sub.bubbles)[colnames(sub.bubbles)=="MChain"]<-"MChain.2"
  out.df<-merge(o.merge1,sub.bubbles,
                by.x=c("sample","Component.2"),by.y=c("sample","LACUNA_NUMBER"))
  mc1a<-out.df$MChain.1
  mc2a<-out.df$MChain.2
  switch.els<-which(mc1a>mc2a)
  out.df$MChain.1[switch.els]<-mc2a[switch.els]
  out.df$MChain.2[switch.els]<-mc1a[switch.els]
  out.df
}
# calculate statistics
chain.life.stats.fn<-function(in.data,include.orig=F) ddply(in.data,.(MChain),function(c.chain) {
  disp.val<-sqrt(with(c.chain,sum(DIR_X)^2+sum(DIR_Y)^2+sum(DIR_Z)^2))
  leng.val<-with(c.chain,sum(sqrt(DIR_X^2+DIR_Y^2+DIR_Z^2)))
  new.cols<-data.frame(sample=c.chain$sample,
                       min.sample=min(c.chain$sample),
                       max.sample=max(c.chain$sample),
                       cnt.sample=length(unique(c.chain$sample)),
                       cnt.chain=nrow(c.chain),
                       mean.dist=mean(c.chain$M_MATCH_DIST),
                       max.dist=max(c.chain$M_MATCH_DIST),
                       dir.disp=disp.val,
                       dir.length=leng.val, 
                       disp.to.leng=disp.val/leng.val)
  if(include.orig) {
    cbind(c.chain,new.cols)
  } else {
    new.cols
  }
}) 
# calculate the bubble life stats from the chains
bubble.life.stats.fn<-function(in.chains,chain.life.stats,sample.vec) { 
  out.val<-ddply(in.chains,.(MChain.1,MChain.2),function(c.edge) {
    a.chain<-c.edge$MChain.1[1]
    b.chain<-c.edge$MChain.2[1]
    sample.range<-subset(chain.life.stats,MChain %in% c(a.chain,b.chain))
    sample.cnt<-ddply(sample.range,.(sample),function(c.sample) data.frame(cnt=nrow(c.sample)))
    both.present<-intersect(subset(sample.cnt,cnt>1)$sample,sample.vec)
    # max of the min and the min of the max make the smallest range
    data.frame(c.edge,min.sample=min(both.present),max.sample=max(both.present),cnt.sample=length(both.present))
  })
  out.val$range.sample<-out.val$max.sample-out.val$min.sample
  out.val
}
bubble.samples.exists.fn<-function(edge.chains,chain.life.stats,sample.vec) {
  # calculate the full lifetime information
  bubble.life.full<-bubble.life.stats.fn(edge.chains,chain.life.stats,sample.vec)
  # only the possible topological events
  # the bubbles must have been mutually alive more than 2 frames 
  # the number of number of frames they are connected much be less than the mutual lifetime
  bubble.life.good<-subset(bubble.life.full,range.sample>=2 & Connections<=(range.sample+1))
  # give the bubbles an id
  bubble.life.good$id<-as.factor(paste(bubble.life.good$MChain.1,bubble.life.good$MChain.2))
  
  bubble.samples.exists<-ddply(bubble.life.good,.(MChain.1,MChain.2,id),function(c.edge) {
    a.chain<-c.edge$MChain.1[1]
    b.chain<-c.edge$MChain.2[1]
    sample.range<-subset(chain.life.stats,MChain %in% c(a.chain,b.chain))
    sample.cnt<-ddply(sample.range,.(sample),function(c.sample) data.frame(cnt=nrow(c.sample)))
    both.present<-intersect(subset(sample.cnt,cnt>1)$sample,sample.vec)
    data.frame(sample=both.present)
  })
  all.bubbles.topo<-rbind(cbind(bubble.life.good[,names(bubble.life.good) %in% c("MChain.1","MChain.2","id","sample","Voxels")],connection="Touching"),cbind(bubble.samples.exists,Voxels=0,connection="Separated"))
  # remove all the extra empties
  ddply(all.bubbles.topo,.(id,sample),function(c.edge) {
    o.val<-subset(c.edge,connection=="Touching")
    if (nrow(o.val)<1) o.val<-c.edge
    o.val
  })
}

# merge two data files after tracking into the same file
mergedata<-function(goldData,matchData,prefix="M_",as.diff=F) {
  outData<-data.frame(goldData)
  for (cCol in names(matchData)) {
    outData[[paste(prefix,cCol,sep="")]]=matchData[[cCol]]
  }
  as.diff.data(outData,m.prefix=prefix)
}
as.diff.data<-function(mergedData,m.prefix="M_",d.prefix="D_",sample.col.name="sample") {
  all.cols<-names(mergedData)
  keep.original<-which(laply(all.cols,
                         function(x) {
                           length(grep(m.prefix,x))<1
                           }
                         )
                   )
  original.cols<-all.cols[keep.original]
  keep.numeric<-which(laply(original.cols,
                         function(x) {
                          is.numeric(mergedData[,x])
                         }
  )
  )
  numeric.cols<-all.cols[keep.numeric]
  out.data<-mergedData[,(names(mergedData) %in% original.cols)]
  # normalize the fields by their velocity (D_sample)
  if (sample.col.name %in% original.cols) {
    dsample.vec<-mergedData[[paste(m.prefix,sample.col.name,sep="")]]-mergedData[[sample.col.name]]
  } else {
    dsample.vec<-1
  }
  for(c.col in numeric.cols) {
    # add differential column
    new.name<-switch(c.col,
                     POS_X={"DIR_X"},
                     POS_Y={"DIR_Y"},
                     POS_Z={"DIR_Z"},
                     paste(d.prefix,c.col,sep="")
    )
    old.name<-paste(m.prefix,c.col,sep="")
    cur.out.col<-mergedData[[old.name]]-mergedData[[c.col]]
    if (c.col!=sample.col.name) cur.out.col=cur.out.col/dsample.vec
    out.data[[new.name]]<-cur.out.col
  }
  cbind(out.data,M_MATCH_DIST=mergedData$M_MATCH_DIST,BIJ_MATCH=mergedData$M_BIJ_MATCHED)
}


edge.status.change<-function(ic.edge) {
  # sort the list appropriately
  c.edge<-ic.edge[order(ic.edge$sample),]
  c.info<-c.edge[,c("sample","connection")]
  # true if the bubbles are touching
  c.info$connection<-c.info$connection=="Touching"
  # shift the list by one forwards to have the connection before
  # in each column
  c.info$cxn.before<-c(NA,c.info$connection[-nrow(c.info)])
  # shift the list by one backwards
  c.info$cxn.after<-c(c.info$connection[-1],NA)
  # there are actually 4 possibilities
  # was.created means it was created between t-1 and t
  # will.created means it will be created between t and t+1
  # was.destroyed means it was destroyed between t-1 and t
  # will.destroyed means it will be destoryed between t and t+1
  c.info$was.created<-(!c.info$cxn.before 
                       & c.info$connection)
  c.info$will.created<-(!c.info$connection & 
                          c.info$cxn.after)
  c.info$was.destroyed<-(c.info$cxn.before
                         & !c.info$connection)
  c.info$will.destroyed<-(c.info$connection &
                            !c.info$cxn.after)
  out.cols<-c.info[,c("was.created","will.created","was.destroyed","will.destroyed")]
  cbind(c.edge,out.cols)
}
# Converts a topology into a list of status changes for each eedge
topo2status.change<-function(in.topo,parallel=T) ddply(in.topo,.(id),edge.status.change,.parallel=parallel)



# Add position (or other columns to the edge file)
# it can be used like this edge.w.pos<-edges.append.pos(bubbles.join,mini.edges)
edges.append.pos<-function(in.bubbles,in.edges,time.col="sample",
                           bubble.col="MChain",bubble.col1="MChain.1",bubble.col2="MChain.2") {
  join.cols<-c(time.col,bubble.col)
  link.left<-merge(in.edges,in.bubbles,all.x=T,all.y=F,by.x=c(time.col,bubble.col1),by.y=join.cols,sort=F,suffixes=c(".edge",""))
  merge(link.left,in.bubbles,all.x=T,all.y=F,by.x=c(time.col,bubble.col2),by.y=join.cols,sort=F,suffixes=c(".start",".end"))
}
# add chain (time-independent bubble identifier)
tracking.add.chains<-function(in.data,check.bij=F) {
  if (check.bij) sub.bubbles<-subset(in.data,BIJ_MATCH)
  else sub.bubbles<-in.data
  if(nrow(sub.bubbles)>0) {
  sub.bubbles$Chain<-c(1:nrow(sub.bubbles)) # Unique Bubble ID
  bubbles.forward<-data.frame(sample=sub.bubbles$sample+sub.bubbles$D_sample,
                              LACUNA_NUMBER=sub.bubbles$LACUNA_NUMBER+sub.bubbles$D_LACUNA_NUMBER,
                              Chain=sub.bubbles$Chain)
  bubbles.mapping<-merge(sub.bubbles[,names(sub.bubbles) %in% c("sample","Chain","LACUNA_NUMBER")],
                         bubbles.forward,by=c("sample","LACUNA_NUMBER"))
  bubble.mapping.proper<-mapply(list, bubbles.mapping$Chain.x, bubbles.mapping$Chain.y, SIMPLIFY=F)
  bubbles.mapping.full<-1:max(sub.bubbles$Chain)
  for(c in bubble.mapping.proper) {
    cx<-c[[1]]
    cy<-c[[2]]
    min.ch<-c(cx,cy,bubbles.mapping.full[cx],bubbles.mapping.full[cy])
    min.val<-min(min.ch[!is.na(min.ch)])
    bubbles.mapping.full[cx]<-min.val
    bubbles.mapping.full[cy]<-min.val
  }
  cbind(sub.bubbles,MChain=bubbles.mapping.full[sub.bubbles$Chain])
  } else {
    cbind(sub.bubbles,MChain=c(),Chain=c())
  }
}
# combine the edges with the bubble file to have chains id's instead of components and unique names
process.edges<-function(in.edges,in.bubbles) {
  edges.join<-edges.append.mchain(in.edges,in.bubbles)
  rows.to.swap<-which(edges.join$MChain.2>edges.join$MChain.1)
  edges.join2<-edges.join[rows.to.swap,]
  edges.join[rows.to.swap,]$MChain.1<-edges.join2$MChain.2
  edges.join[rows.to.swap,]$MChain.2<-edges.join2$MChain.1
  # Edge lifetime information
  edges.join.stats<-ddply(edges.join,.(MChain.1,MChain.2),function(x) {cbind(x,
                                                                             Range=max(x$sample)-min(x$sample),
                                                                             Start.Frame=min(x$sample),
                                                                             Final.Frame=max(x$sample),
                                                                             Connections=nrow(x)
  )})
  edges.join.stats$name<-paste(edges.join.stats$MChain.1,edges.join.stats$MChain.2)
  edges.join.stats$id<-as.numeric(as.factor(edges.join.stats$name))
  edges.join.stats2<-ddply(edges.join.stats,.(MChain.1,MChain.2),function(x) {
    cbind(x,n.sample=x$sample-min(x$sample),x.sample=(x$sample-min(x$sample))/(max(x$sample)-min(x$sample)))
  })
  
  edges.join.stats2
}

edges.missing<-function(in.edges,in.bubbles) {
  sub.bubbles<-ddply(in.bubbles[,names(in.bubbles) %in% c("sample","MChain")],.(MChain),function(c.chain) {
    data.frame(start.sample=min(c.chain$sample),final.sample=max(c.chain$sample))
  })
  
  ddply(in.edges,.(id),function(c.edge) {
    c.row<-c.edge[1,!(names(c.edge) %in% c("sample"))]
    c1<-c.row$MChain.1
    c2<-c.row$MChain.2
    rel.samples<-subset(sub.bubbles,MChain==c1 | MChain==c2)
    sample.vals<-c(max(rel.samples$start.sample):min(rel.samples$final.sample)) # from the highest starting frame to the lowest ending frame
    cbind(c.row,sample=sample.vals,connected=(sample.vals %in% c.edge$sample))
  })
}
#' Match objects 
#' @author Kevin Mader (kevin.mader@gmail.com)
#' 
#'
#' @param groundTruth is the frame to compare to
#' @param susData is the current frame
#' @param maxVolDifference is the largest allowable difference in volume before maxVolPenalty is added
#' @param in.offset the offset to apply to groundTruth before comparing to susData
#' @param do.bij run the bijective comparison as well
#' @param x.weight weight to scale the x distance with
#' @param dist.fun a custom distance metric to use
matchObjects<-function(groundTruth,susData,maxVolDifference=0.5,
                        maxVolPenalty=5000^2,in.offset=c(0,0,0),
                        do.bij=T,x.weight=1,y.weight=1,z.weight=1,
                        dist.fun=NA) {
  gmatch<-c()
  gdist<-c()
  gincl<-c()
  if(is.na(dist.fun)) { # if it is not present
    if (!is.na(maxVolPenalty)) { # use maxVolPenalty
        dist.fun<-function(bubMat,cPos,offset) { (maxVolPenalty*((abs(bubMat$VOLUME-cPos$VOLUME)/cPos$VOLUME)>maxVolDifference)+x.weight*(bubMat$POS_X-offset[1]-cPos$POS_X)**2+y.weight*(bubMat$POS_Y-offset[2]-cPos$POS_Y)**2+z.weight*(bubMat$POS_Z-offset[3]-cPos$POS_Z)**2) }
    } else { # skip it
        # leave volume out
        dist.fun<-function(bubMat,cPos,offset) { x.weight*(bubMat$POS_X-offset[1]-cPos$POS_X)**2+y.weight*(bubMat$POS_Y-offset[2]-cPos$POS_Y)**2+z.weight*(bubMat$POS_Z-offset[3]-cPos$POS_Z)**2 }
    }
  }
  for (i in 1:dim(groundTruth)[1]) {
    cVec<-dist.fun(susData,groundTruth[i,],in.offset)
    cDist<-min(cVec)
    gdist[i]<-sqrt(cDist) # perform square root operation before saving and only on one value
    gmatch[i]<-which(cVec==cDist)[1]
  }
  mData<-susData[gmatch,]
  mData$MATCH_DIST<-gdist
  if (do.bij) {
    # Check the reverse
    for (i in 1:length(gmatch)) {
      c.susbubble<-gmatch[i]
      # distance from matched bubble to all bubbles in ground truth
      cVec<-dist.fun(groundTruth,susData[c.susbubble,],-1*in.offset)
      cDist<-min(cVec)
      gincl[i]<-(i==which(cVec==cDist)[1])
    }
    mData$BIJ_MATCHED<-gincl
  }
  
  
  mData
}
# allow old function to continue working
compare.foam.frames<-function(...) compare.frames(...)
# compare two frames and forward parameters to the matchObjects function
compare.frames<-function(gbubs,kbubs,as.diff=F,...) {
  kmatch<-matchObjects(gbubs,kbubs,...)
  fData<-mergedata(gbubs,kmatch,as.diff=as.diff)
  fData
}
# takes a tracked data experiment with sample columns and calculates the birth and death
bubble.life.check<-function(in.data) {
  ddply(in.data,.(sample),function(x) {
    c.sample<-x$sample[1]
    n.sample<-x$sample[1]+x$D_sample[1]
    n.bubbles<-unique(subset(in.data,sample==n.sample)$LACUNA_NUMBER)
    dies=!((x$LACUNA_NUMBER+x$D_LACUNA_NUMBER) %in% n.bubbles)
    l.bubbles.list<-subset(in.data,sample+D_sample==c.sample)
    l.bubbles<-unique(l.bubbles.list$LACUNA_NUMBER+l.bubbles.list$D_LACUNA_NUMBER)
    born=!(x$LACUNA_NUMBER %in% l.bubbles)
    cbind(x,dies=dies,born=born)
  })
}


plot.t1.event<-function(edges.tracked,keep.event,with.frames=F,all.frames=F,
                        x.name="POS_X",y.name="POS_Z",x.label=NA,y.label=NA) {
  important.edges<-edges.tracked$important.edges
  edge.info<-edges.tracked$edge.info
  good.roi.data<-edges.tracked$obj.list
  cur.event<-subset(important.edges,event.name==keep.event)
  keep.chains<-unique(c(cur.event$MChain.1,cur.event$MChain.2))
  keep.frames<-c((min(cur.event$sample)-1):(max(cur.event$sample)+1))
  sub.edges<-subset(edge.info,(MChain.1 %in% keep.chains) | (MChain.2 %in% keep.chains))
  sub.edges<-subset(sub.edges,((MChain.1 %in% keep.chains) & (MChain.2 %in% keep.chains)) | (was.created | was.destroyed))
  
  selected.links<-edges.append.pos(good.roi.data,sub.edges)
  selected.chains<-subset(good.roi.data[with(good.roi.data, order(sample)), ],MChain %in% keep.chains)
  if (!all.frames) {
    print(keep.frames)
    selected.links<-subset(selected.links,sample %in% keep.frames)
    if (with.frames) selected.chains<-subset(selected.chains,sample %in% keep.frames)
  }
  selected.links$involved<-with(selected.links,(MChain.1 %in% keep.chains) & (MChain.2 %in% keep.chains))                                 
  selected.links$edge.length<-with(selected.links,sqrt((POS_X.start-POS_X.end)^2+(POS_Y.start-POS_Y.end)^2+(POS_Z.start-POS_Z.end)^2))
  selected.links$type="No Event"
  selected.links[which(selected.links$was.created),]$type<-"Was Created"
  selected.links[which(selected.links$will.created),]$type<-"Will Created"
  selected.links[which(selected.links$was.destroyed),]$type<-"Was Destroyed"
  selected.links[which(selected.links$will.destroyed),]$type<-"Will Destroyed"
  ss<-function(var) paste(var,".start",sep="")
  se<-function(var) paste(var,".end",sep="")
  if (with.frames) {                            
    o.plot<-ggplot(selected.links)+
      geom_segment(aes_string(x=ss(x.name),y=ss(y.name),xend=se(x.name),yend=se(y.name),
                       linetype="connection",alpha="involved",color="type"))+
      geom_point(data=selected.chains,aes_string(x=x.name,y=y.name),alpha=1,color="red")+   
      labs(color="Edge Event")+facet_wrap(~sample)
  } else {
    o.plot<-ggplot(selected.links)+
      geom_segment(aes_string(x=ss(x.name),y=ss(y.name),xend=se(x.name),yend=se(y.name),
                       linetype="connection",alpha="involved"))+
      geom_point(data=selected.chains,aes_string(x=x.name,y=y.name),alpha=1,color="red")+
      geom_path(data=selected.chains,aes_string(x=x.name,y=y.name,group="MChain",color="as.factor(MChain)"),alpha=1)+
      labs(color="Chain")
  }
  if(is.na(x.label)) x.label<-x.name
  if(is.na(y.label)) y.label<-y.name
  o.plot+theme_bw(20)+labs(x=x.label,y=y.label,alpha="Involved",linetype="Connected")
}


#' Edge Tracking Function
#' @author Kevin Mader (kevin.mader@gmail.com)
#' Tracks a list of data.frames using the compare.frames function
#' and standard tracking, offset tracking, and adaptive offset tracking
#' Tracking Function
track.edges<-function(in.objs,in.edges,keep.all.events=F,parallel=F) {
  edge.chain<-process.edges(in.edges,in.objs)
  chain.life.stats<-chain.life.stats.fn(in.objs)
  sample.vec<-unique(in.objs$sample)
  # just get a summary (we can more carefully analyze later)
  obj.life.stats<-bubble.life.stats.fn(edge.chain,chain.life.stats,sample.vec)
  all.bubbles.topo<-bubble.samples.exists.fn(edge.chain,chain.life.stats,sample.vec) 
  edge.info<-topo2status.change(all.bubbles.topo,parallel=parallel)
  # keep only the interesting events
  edge.info.interesting<-subset(edge.info,was.created | will.created | was.destroyed | will.destroyed)
  # combine the list together as chain1 and chain2
  singlechain.edge.info<-rbind(cbind(edge.info.interesting,MChain=edge.info.interesting$MChain.1),
                               cbind(edge.info.interesting,MChain=edge.info.interesting$MChain.2))
  important.edges<-ddply(singlechain.edge.info,.(sample,MChain),function(c.bubble.frame) {
    sum.stats<-colSums(c.bubble.frame[,c("was.created","will.created","was.destroyed","will.destroyed")],na.rm=T)
    event.count<-sum(sum.stats)
    event.name<-paste("S",c.bubble.frame$sample[1],"_",paste(unique(c.bubble.frame$id),collapse=",",sep=""),sep="")
    if ((sum.stats["was.created"]>0) & (sum.stats["was.destroyed"]>0)) {
      was.events<-subset(c.bubble.frame,was.created | was.destroyed)
    } else {
      was.events<-c.bubble.frame[0,]
    }
    if ((sum.stats["will.created"]>0) & (sum.stats["will.destroyed"]>0)) {
      will.events<-subset(c.bubble.frame,will.created | will.destroyed)
    } else {
      will.events<-c.bubble.frame[0,]
    }
    out.mat<-rbind(was.events,will.events)
    if (nrow(out.mat)>0) out.mat<-cbind(out.mat,event.count=event.count,event.name=event.name)
    out.mat
  })
  important.edges<-important.edges[order(-important.edges$event.count),]
  
  list(important.edges=important.edges,edge.info=edge.info,obj.list=in.objs,obj.life.stats=obj.life.stats)
}

#' @author Kevin Mader (kevin.mader@gmail.com)
#' Tracks a list of data.frames using the compare.frames function
#' and standard tracking, offset tracking, and adaptive offset tracking
#' 
#'
#' @param inData the list of data.frames containing the samples
#' @param offset is the offset to use for the offset run
#' @param run.offset if the offset analysis should be run
#' @param run.adaptive if the adaptive analysis should be run
#' @param ... parameters to be passed onto the compare.frames function
track.frames<-function(inData,offset,run.offset=T,run.adaptive=T,parallel=F,...) {
  track.fcn<-function(x,in.offset=c(0,0,0)) {
    cbind(compare.frames(x[[1]],x[[2]],as.diff=T,in.offset=in.offset,...),Frame=x[[3]])
  }
  # Track function adaptive
  track.fcn.adp<-function(x,in.offset=c(0,0,0)) {
    pre.match<-compare.frames(x[[1]],x[[2]],in.offset=in.offset,...)
    pre.offset<-colMeans(pre.match)[c("DIR_X","DIR_Y","DIR_Z")]
    cbind(compare.frames(x[[1]],x[[2]],as.diff=T,in.offset=pre.offset,...),Frame=x[[3]])
  }
  staggered.data<-mapply(list, inData[-length(inData)], inData[-1],1:(length(inData)-1), SIMPLIFY=F)
  
  track.data<-ldply(staggered.data,
                    track.fcn,.parallel=parallel)
  if(run.offset) {
    track.data.fix<-ldply(staggered.data,
                        function(x) track.fcn(x,offset),.parallel=parallel)
    if(nrow(track.data.fix)<1) run.offset<-F
  }
  if(run.adaptive) {
    track.data.adp<-ldply(staggered.data,
                        function(x) track.fcn.adp(x,offset),.parallel=parallel)
    if(nrow(track.data.adp)<1) run.adaptive<-F
  }
  # functions to apply before combinining
  # 1) life check
  # 2) under quantile for match distance
  # 2) add chain numbers based on remaining bubbles
  preproc.fcn<-function(...) {
    alive.bubbles<-bubble.life.check(...)
    track.one<-tracking.add.chains(alive.bubbles)
    chain.life.stats.fn(track.one,include.orig=T)
  }
  
  all.tracks<-cbind(preproc.fcn(track.data),Matching="No Offset")
  if(run.offset) all.tracks<-rbind(all.tracks,cbind(preproc.fcn(track.data.fix),Matching="Fix Offset"))
  if(run.adaptive) all.tracks<-rbind(all.tracks,cbind(preproc.fcn(track.data.adp),Matching="Adaptive Offset"))
  all.tracks
}


