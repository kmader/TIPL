library("testthat")
source("trackingCode.R")
context("preliminary matching code")
# A simple test data set to play with
# there are bubbles 1,2,3
# bubbles 2,3 are live from 1-4
# bubble 1 lives from 2-4
#                               
test.bubbles.noch<-data.frame(  sample=c(1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4),
                         LACUNA_NUMBER=c(1, 2, 1, 2, 3, 1, 2, 3, 1, 2, 3),
                       D_LACUNA_NUMBER=c(1,-1, 1,-1, 0, 0, 0, 0, 0, 0, 0),
                                B.VAL=c(2, 3, 2, 1, 3, 1, 2, 3, 2, 1, 3),
                                VOLUME=1,
                                AISO=0,
                                D_sample=1,
                                POS_X=0,
                                POS_Y=0,
                                POS_Z=0
)

test.bubbles<-tracking.add.chains(test.bubbles.noch,check.bij=F)
test_that("tracking.add.chains", {
  expect_equal(length(unique(test.bubbles$MChain)),3) # make sure we end up with only 3 unique chains
  expect_equal(max(summary(as.factor(test.bubbles$MChain))),4) # longest chain is 4
  expect_equal(min(summary(as.factor(test.bubbles$MChain))),3) # shortest chain is 3
})
# Make positions more discernable
test.bubbles$POS_X<-with(test.bubbles,(sample+MChain/10))
test.bubbles$POS_Y<-with(test.bubbles,10*(sample+MChain/10))
test.bubbles$POS_Z<-with(test.bubbles,100*(sample+MChain/10))

sample.vec<-unique(test.bubbles$sample)

cls<-chain.life.stats.fn(test.bubbles)
test_that("chain.life.stats", {
  expect_equal(nrow(cls),nrow(test.bubbles))
  expect_equal(subset(cls,MChain==1)$cnt.sample[1],3)
  expect_equal(subset(cls,MChain==1)$max.sample[1],4)
  expect_equal(subset(cls,MChain==1)$min.sample[1],2)
  expect_equal(subset(cls,MChain==2)$cnt.sample[1],4)
})


# at time 1, bubbles 2, 3 are connected
# at time 2, bubble 1 is created and bubbles 1,2 are connected 
# at time 3, all bubbles but 2,1 are connected
# at time 4, only bubbles 2 and 1 are connected

# t1 event is bubbles 1,2 disconnecting from 2->3 and reconnecting from 3->4

test.edges<-data.frame(Component.1=c(1,1,2,1,1),
                       Component.2=c(2,2,3,3,2),
                            sample=c(1,2,3,3,4),
                       Voxels=1)

edge.plus<-edges.append.mchain(test.edges,test.bubbles)
test_that("edges.append.mchain",{
  expect_equal(nrow(edge.plus),nrow(test.edges)) # same length
  expect_equal(nrow(subset(edge.plus,sample==1)),nrow(subset(test.edges,sample==1))) # same length for sample 1
  expect_equal(nrow(subset(edge.plus,sample==1 & MChain.1==1)),0)
  expect_equal(nrow(subset(edge.plus,MChain.1==1)),3)
})

edge.app<-edges.append.pos(test.bubbles,edge.plus)
test_that("edges.append.pos",{

})

edges.processed<-process.edges(test.edges,test.bubbles)
test_that("process.edges", {
  expect_equal(nrow(edges.processed),nrow(test.edges)) # same length
  expect_equal(min(edges.processed$Range),0)
  expect_equal(max(edges.processed$Range),2)
  expect_equal(subset(edges.processed,sample==1 & Component.1==1)$Connections,2)
})

bubble.life.stats<-bubble.life.stats.fn(edges.processed,cls,sample.vec)
test_that("bubble.life.stats", {
  # No tests yet
})

all.topo<-bubble.samples.exists.fn(edges.processed,cls,sample.vec)

test_that("all.topo/bubble.samples.exists", {
  expect_equal(nrow(subset(all.topo,id=="3 1")),3)
  expect_equal(nrow(subset(all.topo,id=="3 1" & connection=="Touching")),1)
  expect_equal(nrow(subset(all.topo,id=="3 2")),4)
  expect_equal(nrow(subset(all.topo,id=="3 2" & connection=="Touching")),2)
  expect_equal(nrow(subset(all.topo,id=="2 1")),3)
  expect_equal(nrow(subset(all.topo,id=="2 1" & connection=="Touching")),2)
  # No tests yet
})


event.list<-topo2status.change(all.topo)
event.list.interesting<-subset(event.list,was.created | will.created | was.destroyed | will.destroyed)

# combine the list together as chain1 and chain2
singlechain.event.list<-rbind(cbind(event.list.interesting,MChain=event.list.interesting$MChain.1),cbind(event.list.interesting,MChain=event.list.interesting$MChain.2))

bubble.events<-ddply(singlechain.event.list,.(MChain),function(c.bubble) {
  ocols<-colSums(c.bubble[,c("was.created","will.created","was.destroyed","will.destroyed")],na.rm=T)
  data.frame(t(ocols),event.count=sum(ocols))
})

bubble.events<-bubble.events[order(-bubble.events$event.count),]
test_that("bubble.events", {
  cSums<-colSums(bubble.events)
  expect_equal(cSums["was.created"],cSums["will.created"])
  expect_equal(cSums["was.destroyed"],cSums["will.destroyed"])
}

important.edges<-ddply(singlechain.event.list,.(sample,MChain),function(c.bubble.frame) {
  sum.stats<-colSums(c.bubble.frame[,c("was.created","will.created","was.destroyed","will.destroyed")],na.rm=T)
  event.count<-sum(sum.stats)
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
  if (nrow(out.mat)>0) out.mat<-cbind(out.mat,event.count=event.count)
  out.mat
})
important.edges<-important.edges[order(-important.edges$event.count),]

          
          ssbc<-function(...,cn) { # subset both chains
            cval<-subset(...)
            if (nrow(cval)>0) data.frame(MChain=c(cval$MChain.1,cval$MChain.2),id=cval$id,MC.Number=c(rep(1,nrow(cval)),rep(2,nrow(cval))),type=cn) # preserve order with MC.Number
            else data.frame(MChain=c(),type=c())
          }
          t1.events<-ddply(important.edges,.(sample,MChain),function(c.bf) {
            rbind(ssbc(c.bf,was.created,cn="was.cr"),
                  ssbc(c.bf,was.destroyed,cn="was.dy"),
                  ssbc(c.bf,will.created,cn="will.cr"),
                  ssbc(c.bf,will.destroyed,cn="will.dy"))
          })

          
          links.start<-unique(merge(subset(t1.events,MC.Number==1),test.bubbles,by=c("sample","MChain")))
          links.end<-unique(merge(subset(t1.events,MC.Number==2),test.bubbles,by=c("sample","MChain")))
          full.links<-merge(links.start,links.end,by=c("sample","id","type"),suffixes=c(".start",".end"))