# simple R functions
plist<-function(...) as.pairlist(list(...))
plist.to.cmd<-function(in.args,block.prefix="") paste(mapply(function(...) paste("-",block.prefix,...,sep=""),names(in.args),"=",in.args),collapse=" ")

# java and tipl functions
library("rJava")
.jinit(classpath=c("/Users/mader/Dropbox/tipl/build/TIPL_core.jar","/Users/mader/Dropbox/Informatics/spark/lib/spark-assembly-1.0.1-hadoop2.2.0.jar"), parameters=paste("-Xmx4G","-Djava.awt.headless=true", sep=" "))
# Get a Spark Context
getContext<-function() .jcall("tipl/spark/SparkGlobal",returnSig="Lorg/apache/spark/api/java/JavaSparkContext;","getContext","RTIPLContext")

# Parse Arguments
parseArguments<-function(inArgs) .jcast(J("tipl/util/TIPLGlobal")$activeParser(.jcast(.jarray(strsplit(inArgs," ")[[1]]))),"tipl/util/ArgumentParser")
showArguments<-function(inObj) {
  p<-inObj$setParameter(parseArguments(""))
  xs<-strsplit(p$toString()," -")[[1]]
  print(xs)
  p
}
getHelp<-function(inObj) {
  fullArgs<-inObj$setParameter(parseArguments(""))
  strsplit(fullArgs$getHelp(),"\n")[[1]]
}
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

makeBlock<-function(block.name,inst.name) .jnew(paste("tipl/blocks",block.name,sep="/"),inst.name)

makeBlockWithArgs<-function(block.name,inst.name,input.args) {
  cmdline<-plist.to.cmd(input.args,block.prefix=inst.name)
  print(cmdline)
  p<-parseArguments(cmdline)
  cBlock<-makeBlock(block.name,inst.name)
  np<-cBlock$setParameter(p)
  print(showArguments(cBlock))
  cBlock
}