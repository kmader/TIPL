library("testthat")
source("trackingCode.R")
context("preliminary matching code")
# A simple test data set to play with


test.bubbles<-data.frame(       sample=c(1,1,2,2,2,3,3,3,4,4,4),
                                MChain=c(2,3,2,1,3,1,2,3,2,1,3),
                         LACUNA_NUMBER=c(1,2,1,2,3,1,2,3,1,2,3)
                         )
sample.vec<-unique(test.bubbles$sample)

cls<-chain.life.stats.fn(test.bubbles)
test_that("chain.life.stats", {
  expect_equal(nrow(cls),nrow(test.bubbles))
  expect_equal(subset(cls,MChain==1)$cnt.sample[1],3)
  expect_equal(subset(cls,MChain==1)$max.sample[1],4)
  expect_equal(subset(cls,MChain==1)$min.sample[1],2)
  expect_equal(subset(cls,MChain==2)$cnt.sample[1],4)
})

test.edges<-data.frame(Component.1=c(1,1,1,2,1,1),
                       Component.2=c(2,2,2,3,3,2),
                            sample=c(1,2,3,3,3,4),
                       Voxels=1)

edge.plus<-edges.append.mchain(test.edges,test.bubbles)
test_that("edges.append.mchain",{
  expect_equal(nrow(edge.plus),nrow(test.edges)) # same length
  expect_equal(nrow(subset(edge.plus,sample==1)),nrow(subset(test.edges,sample==1))) # same length for sample 1
  expect_equal(nrow(subset(edge.plus,sample==1 & MChain.1==1)),0)
  expect_equal(nrow(subset(edge.plus,MChain.1==1)),4)
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
  # No tests yet
})






