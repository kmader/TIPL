library(plyr)
compare.foam<-function(cDir,goldFile='glpor_1.csv',kevinFile='clpor_2.csv') {
  kbubs<-read.csv(paste(cDir,kevinFile,sep='/'),skip=1)
  gbubs<-read.csv(paste(cDir,goldFile,sep='/'),skip=1)
  
  gbubs<-compare.foam.clean(gbubs)
  kbubs<-compare.foam.clean(kbubs)
  compare.foam.frames(gbubs,kbubs)
}
# Match the bubbles in image A to image B
matchBubbles<-function (groundTruth,susData,maxVolDifference=0.5,maxVolPenalty=5000^2,in.offset=c(0,0,0),do.bij=T,x.weight=1,y.weight=1,z.weight=1) {
  gmatch<-c()
  gdist<-c()
  gincl<-c()
  distVec<-function (bubMat,cPos) { (maxVolPenalty*((abs(bubMat$VOLUME-cPos$VOLUME)/cPos$VOLUME)>maxVolDifference)+x.weight*(bubMat$POS_X-in.offset[1]-cPos$POS_X)**2+y.weight*(bubMat$POS_Y-in.offset[2]-cPos$POS_Y)**2+z.weight*(bubMat$POS_Z-in.offset[3]-cPos$POS_Z)**2) }
  rdistVec<-function (bubMat,cPos) { (maxVolPenalty*((abs(bubMat$VOLUME-cPos$VOLUME)/cPos$VOLUME)>maxVolDifference)+x.weight*(bubMat$POS_X+in.offset[1]-cPos$POS_X)**2+y.weight*(bubMat$POS_Y+in.offset[2]-cPos$POS_Y)**2+z.weight*(bubMat$POS_Z+in.offset[3]-cPos$POS_Z)**2) }
  
  for (i in 1:dim(groundTruth)[1]) {
    cVec<-distVec(susData,groundTruth[i,])
    cDist<-min(cVec)
    gdist[i]<-sqrt(cDist) # perform square root operation before saving and only on one value
    gmatch[i]<-which(cVec==cDist)
  }
  mData<-susData[gmatch,]
  mData$MATCH_DIST<-gdist
  if (do.bij) {
    # Check the reverse
    for (i in 1:length(gmatch)) {
      c.susbubble<-gmatch[i]
      # distance from matched bubble to all bubbles in ground truth
      cVec<-rdistVec(groundTruth,susData[c.susbubble,])
      cDist<-min(cVec)
      gincl[i]<-(i==which(cVec==cDist))
    }
    mData$BIJ_MATCHED<-gincl
  }
  
  
  mData
}
# sorted by samples
sort.by.sample<-function(in.bubbles) {in.bubbles[with(in.bubbles, order(sample)), ]}
# no additional first column in ldply
ldply.delfirstcol<-function(...) {
  o.data<-ldply(...)
  o.data[,-1]
}
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
  merge(o.merge1,sub.bubbles,
        by.x=c("sample","Component.2"),by.y=c("sample","LACUNA_NUMBER"))
}
# calculate statistics
chain.life.stats.fn<-function(in.data) ddply(in.data,.(MChain),function(c.chain) {
  data.frame(MChain=c.chain$MChain[1],min.sample=min(c.chain$sample),max.sample=max(c.chain$sample),cnt.sample=length(unique(c.chain$sample)),sample=c.chain$sample)
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
    data.frame(c.edge[1,],min.sample=min(both.present),max.sample=max(both.present),cnt.sample=length(both.present))
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
  bubble.life.good<-subset(bubble.life.full,range.sample>=2 & Connections<(range.sample))
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
  rbind(cbind(bubble.life.good[,names(bubble.life.good) %in% c("MChain.1","MChain.2","id","sample","Voxels")],connection="Touching"),cbind(bubble.samples.exists,Voxels=0,connection="Separated"))
}

# Add position (or other columns to the edge file)
# it can be used like this edge.w.pos<-edges.append.pos(bubbles.join,mini.edges)
edges.append.pos<-function(in.bubbles,in.edges,pos.cols=c("POS_X","POS_Y","POS_Z"),add.cols=c()) {
  s.edges<-in.edges[,names(in.edges) %in% c("sample","MChain.1","MChain.2")]
  keep.cols<-c(pos.cols,add.cols)
  s.bubbles<-in.bubbles[,names(in.bubbles) %in% c("sample","MChain",keep.cols)]
  out.table.1<-merge(s.edges,s.bubbles,
                     by.x=c("sample","MChain.1"),by.y=c("sample","MChain"))
  append.cols<-which(names(out.table.1) %in% keep.cols)
  names(out.table.1)[append.cols]<-sapply(names(out.table.1)[append.cols],function(x) paste(x,"1",sep="_"))
  out.table.2<- merge(s.edges,s.bubbles,
                      by.x=c("sample","MChain.2"),by.y=c("sample","MChain"))
  #out.table.2<-out.table.2[,!(names(out.table.2) %in% c("sample","MChain","MChain.2","MChain.1"))]
  names(out.table.2)<- sapply(names(out.table.2),function(x) paste(x,"2",sep="_"))
  #print(cbind(summary(out.table.1),summary(out.table.2)))
  cbind(out.table.1,out.table.2)
}
# add chain (time-independent bubble identifier)
tracking.add.chains<-function(in.data,check.bij=F) {
  if (check.bij) sub.bubbles<-subset(in.data,BIJ_MATCH)
  else sub.bubbles<-in.data
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


compare.foam.frames<-function(gbubs,kbubs,as.diff=F,...) {
  kmatch<-matchBubbles(gbubs,kbubs,...)
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
