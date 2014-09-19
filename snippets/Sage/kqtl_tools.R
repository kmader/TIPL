library("qtl")
library("snow")
library('qtlbim')
library("BayesQTLBIC")
skipSamples<-function(cVar,sFactor) {
  cStats<-summary(cVar)
  midPt<-cStats[4]
  lBound<-midPt-sFactor*(cStats[4]-cStats[2])
  uBound<-midPt+sFactor*(cStats[5]-cStats[4])
  which((cVar<lBound) | (cVar>uBound))
}

testfun<-function (x, marker, pheno.col = 1, jitter = 1, infer = TRUE, 
                    pch, ylab, main, col, ...) 
{
  cross <- x
  if (!any(class(cross) == "cross")) 
    stop("Input should have class \"cross\".")
  type <- class(cross)[1]
  
  if (length(pheno.col) > 1) {
    pheno.col <- pheno.col[1]
    warning("plot.pxg can take just one phenotype; only the first will be used")
  }
  if (is.character(pheno.col)) {
    num <- find.pheno(cross, pheno.col)
    if (is.na(num)) 
      stop("Couldn't identify phenotype \"", pheno.col, 
           "\"")
    pheno.col <- num
  }
  if (pheno.col < 1 | pheno.col > nphe(cross)) 
    stop("pheno.col values should be between 1 and the no. phenotypes")
  if (missing(pch)) 
    pch <- par("pch")
  if (missing(ylab)) 
    ylab <- colnames(cross$pheno)[pheno.col]
  oldlas <- par("las")
  on.exit(par(las = oldlas))
  par(las = 1)
  o <- sapply(cross$geno, function(a, b) b %in% colnames(a$data), 
              marker)
  if (length(marker) == 1) 
    o <- matrix(o, nrow = 1)
  if (!all(apply(o, 1, any))) {
    oo <- apply(o, 1, any)
    stop("Marker ", marker[!oo], " not found")
  }
  n.mark <- length(marker)
  o <- apply(o, 1, which)
  chr <- names(cross$geno)[o]
  uchr <- unique(chr)
  cross <- subset(cross, chr = uchr)
  map <- pull.map(cross)
  pos <- NULL
  for (i in seq(length(chr))) pos[i] <- map[[chr[i]]][marker[i]]
  chrtype <- sapply(cross$geno, class)
  names(chrtype) <- names(cross$geno)
  chrtype <- chrtype[chr]
  if (any(chrtype == "X") && (type == "bc" || type == "f2")) 
    sexpgm <- getsex(cross)
  else sexpgm <- NULL
  gen.names <- list()
  for (i in seq(length(chr))) gen.names[[i]] <- getgenonames(type, 
                                                             chrtype[i], "full", sexpgm, attributes(cross))
  n.gen <- sapply(gen.names, length)
  jitter <- jitter/10
  if (any(n.gen == 2)) 
    jitter <- jitter * 0.75
  tempf <- function(x, type) {
    tmp <- is.na(x)
    if (type == "f2") 
      tmp[!is.na(x) & x > 3] <- TRUE
    if (type == "4way") 
      tmp[!is.na(x) & x > 4] <- TRUE
    tmp
  }
  if (infer) {
    which.missing <- tempf(cross$geno[[chr[1]]]$data[, marker[1]], 
                           type)
    if (n.mark > 1) 
      for (i in 2:n.mark) which.missing <- which.missing | 
        tempf(cross$geno[[chr[i]]]$data[, marker[i]], 
              type)
    which.missing <- as.numeric(which.missing)
    cross <- fill.geno(cross, method = "imp")
  }
  else which.missing <- rep(1, nind(cross))
  x <- cross$geno[[chr[1]]]$data[, marker[1]]
  if (n.mark > 1) 
    for (i in 2:n.mark) x <- cbind(x, cross$geno[[chr[i]]]$data[, 
                                                                marker[i]])
  else x <- as.matrix(x)
  y <- cross$pheno[, pheno.col]
  if (!infer) {
    if (type == "f2") 
      x[x > 3] <- NA
    if (type == "4way") 
      x[x > 4] <- NA
  }
  if (any(chrtype == "X") && (type == "bc" || type == "f2")) {
    ix = seq(n.mark)[chrtype == "X"]
    for (i in ix) x[, i] <- as.numeric(reviseXdata(type, 
                                                   "full", sexpgm, geno = as.matrix(x[, i]), cross.attr = attributes(cross)))
  }
  data <- as.data.frame(x, stringsAsFactors = TRUE)
  names(data) <- marker
  for (i in marker) data[[i]] <- ordered(data[[i]])
  data$pheno <- y
  data$inferred <- which.missing
  if (n.mark > 1) {
    for (i in 2:n.mark) x[, 1] <- n.gen[i] * (x[, 1] - 1) + 
      x[, i]
  }
  x <- x[, 1]
  observed <- sort(unique(x))
  x <- match(x, observed)
  sux = sort(unique(x))
  me <- se <- array(NA, length(observed))
  me[sux] <- tapply(y, x, mean, na.rm = TRUE)
  se[sux] <- tapply(y, x, function(a) sd(a, na.rm = TRUE)/sqrt(sum(!is.na(a))))
  
  me
  x
}
spxgPlot<-function (x, marker, pheno.col = 1, jitter = 1, infer = TRUE, 
          pch, ylab, main, col, ...) 
{
  cross <- x
  if (!any(class(cross) == "cross")) 
    stop("Input should have class \"cross\".")
  type <- class(cross)[1]
  #if (LikePheVector(pheno.col, nind(cross), nphe(cross))) {
  #  cross$pheno <- cbind(pheno.col, cross$pheno)
  #  pheno.col <- 1
  #}
  if (length(pheno.col) > 1) {
    pheno.col <- pheno.col[1]
    warning("plot.pxg can take just one phenotype; only the first will be used")
  }
  if (is.character(pheno.col)) {
    num <- find.pheno(cross, pheno.col)
    if (is.na(num)) 
      stop("Couldn't identify phenotype \"", pheno.col, 
           "\"")
    pheno.col <- num
  }
  if (pheno.col < 1 | pheno.col > nphe(cross)) 
    stop("pheno.col values should be between 1 and the no. phenotypes")
  if (missing(pch)) 
    pch <- par("pch")
  if (missing(ylab)) 
    ylab <- colnames(cross$pheno)[pheno.col]
  oldlas <- par("las")
  on.exit(par(las = oldlas))
  par(las = 1)
  o <- sapply(cross$geno, function(a, b) b %in% colnames(a$data), 
              marker)
  if (length(marker) == 1) 
    o <- matrix(o, nrow = 1)
  if (!all(apply(o, 1, any))) {
    oo <- apply(o, 1, any)
    stop("Marker ", marker[!oo], " not found")
  }
  n.mark <- length(marker)
  o <- apply(o, 1, which)
  chr <- names(cross$geno)[o]
  uchr <- unique(chr)
  cross <- subset(cross, chr = uchr)
  map <- pull.map(cross)
  pos <- NULL
  for (i in seq(length(chr))) pos[i] <- map[[chr[i]]][marker[i]]
  chrtype <- sapply(cross$geno, class)
  names(chrtype) <- names(cross$geno)
  chrtype <- chrtype[chr]
  if (any(chrtype == "X") && (type == "bc" || type == "f2")) 
    sexpgm <- getsex(cross)
  else sexpgm <- NULL
  gen.names <- list()
  for (i in seq(length(chr))) gen.names[[i]] <- getgenonames(type, 
                                                             chrtype[i], "full", sexpgm, attributes(cross))
  n.gen <- sapply(gen.names, length)
  jitter <- jitter/10
  if (any(n.gen == 2)) 
    jitter <- jitter * 0.75
  tempf <- function(x, type) {
    tmp <- is.na(x)
    if (type == "f2") 
      tmp[!is.na(x) & x > 3] <- TRUE
    if (type == "4way") 
      tmp[!is.na(x) & x > 4] <- TRUE
    tmp
  }
  if (infer) {
    which.missing <- tempf(cross$geno[[chr[1]]]$data[, marker[1]], 
                           type)
    if (n.mark > 1) 
      for (i in 2:n.mark) which.missing <- which.missing | 
        tempf(cross$geno[[chr[i]]]$data[, marker[i]], 
              type)
    which.missing <- as.numeric(which.missing)
    cross <- fill.geno(cross, method = "imp")
  }
  else which.missing <- rep(1, nind(cross))
  x <- cross$geno[[chr[1]]]$data[, marker[1]]
  if (n.mark > 1) 
    for (i in 2:n.mark) x <- cbind(x, cross$geno[[chr[i]]]$data[, 
                                                                marker[i]])
  else x <- as.matrix(x)
  y <- cross$pheno[, pheno.col]
  if (!infer) {
    if (type == "f2") 
      x[x > 3] <- NA
    if (type == "4way") 
      x[x > 4] <- NA
  }
  if (any(chrtype == "X") && (type == "bc" || type == "f2")) {
    ix = seq(n.mark)[chrtype == "X"]
    for (i in ix) x[, i] <- as.numeric(reviseXdata(type, 
                                                   "full", sexpgm, geno = as.matrix(x[, i]), cross.attr = attributes(cross)))
  }
  data <- as.data.frame(x, stringsAsFactors = TRUE)
  names(data) <- marker
  for (i in marker) data[[i]] <- ordered(data[[i]])
  data$pheno <- y
  data$geno <- x
  data$inferred <- which.missing
  if (n.mark > 1) {
    for (i in 2:n.mark) x[, 1] <- n.gen[i] * (x[, 1] - 1) + 
      x[, i]
  }
  x <- x[, 1]
  observed <- sort(unique(x))
  x <- match(x, observed)
  u <- runif(nind(cross), -jitter, jitter)
  r <- (1 - 2 * jitter)/2
  # Sort all values so mean phenotype value increases
  sux = sort(unique(x))
  me <- se <- array(NA, length(observed))
  me[sux] <- tapply(y, x, mean, na.rm = TRUE)
  se[sux] <- tapply(y, x, function(a) sd(a, na.rm = TRUE)/sqrt(sum(!is.na(a))))
  kvRE<-sort(me,index.return=T)
  ox<-x
  for (kit in 1:length(kvRE$ix)) {
    ox[which(x==kit)]=kvRE$ix[kit]
  }
  x<-ox
  
  plot(x + u, y, xlab = "Genotype", ylab = ylab, type = "n", 
       main = "", xlim = c(1 - r + jitter, length(observed) + 
         r + jitter), xaxt = "n",...)
  if (missing(main)) 
    mtext(paste(marker, collapse = "\n"), line = 0.5, cex = ifelse(n.mark == 
      1, 1.2, 0.8))
  else title(main = main)
  abline(v = 1:prod(n.gen), col = "gray", lty = 3)
  if (length(pch) == 1) 
    pch = rep(pch, length(x))
  print(n.mark)
  if (n.mark<2) {
  if (infer) {
    points((x + u)[which.missing == 1], y[which.missing == 
      1], col = "red", pch = pch[which.missing == 1])
    points((x + u)[which.missing == 0], y[which.missing == 
      0], pch = pch[which.missing == 0])
  }
  else points(x + u, y, pch = pch)
  }
  
  #sux = sort(unique(x))
  sux<-kvRE$ix
  me<-kvRE$x
  se<-se[kvRE$ix]
  #me <- se <- array(NA, length(observed))
  #me[sux] <- tapply(y, x, mean, na.rm = TRUE)
  #se[sux] <- tapply(y, x, function(a) sd(a, na.rm = TRUE)/sqrt(sum(!is.na(a))))
  thecolors <- c("black", "blue", "red", "purple", "green", 
                 "orange")
  if (missing(col)) {
    col <- thecolors[1:n.gen[n.mark]]
    if (n.gen[n.mark] == 3) 
      col <- c("blue", "purple", "red")
    else if (n.gen[n.mark] == 2) 
      col <- c("blue", "red")
  }
  segments(seq(length(observed)) + jitter * 2, me, seq(length(observed)) + 
    jitter * 4, me, lwd = 2, col = col)
  segments(seq(length(observed)) + jitter * 3, me - se, seq(length(observed)) + 
    jitter * 3, me + se, lwd = 2, col = col)
  segments(seq(length(observed)) + jitter * 2.5, me - se, seq(length(observed)) + 
    jitter * 3.5, me - se, lwd = 2, col = col)
  segments(seq(length(observed)) + jitter * 2.5, me + se, seq(length(observed)) + 
    jitter * 3.5, me + se, lwd = 2, col = col)
  u <- par("usr")
  segments(1:length(observed), u[3], 1:length(observed), u[3] - 
    diff(u[3:4]) * 0.015, xpd = TRUE)
  if (n.mark == 1){
    tmp <- gen.names[[1]]
    tmp<- tmp[kvRE$ix]
  } else {
    tmp <- array(gen.names[[n.mark]], c(prod(n.gen), n.mark))
    for (i in (n.mark - 1):1) {
      tmpi <- rep(gen.names[[i]], rep(prod(n.gen[(i + 1):n.mark]), 
                                      n.gen[i]))
      if (i > 1) 
        tmpi <- rep(tmpi, prod(n.gen[1:(i - 1)]))
      tmp[, i] <- tmpi
    }
    tmp <- apply(tmp, 1, function(x) paste(x, collapse = "\n"))
    tmp<- tmp[kvRE$ix]
    tmp <- tmp[!is.na(match(1:prod(n.gen), observed))]
  }
  
  cxaxis <- par("cex.axis")
  axis(side = 1, at = 1:length(observed), labels = tmp, cex = ifelse(n.mark == 
    1, cxaxis, cxaxis * 0.8), tick = FALSE, line = (length(marker) - 
    1)/2)
  invisible(data)
  
}

plotWithMarginals <- function(x, y){
  
  # find min / max on each dimension
  # then set up breaks so that even if x, y are on very different ranges things work
  mm <- max(abs(range(x, y)))
  breaks <- seq(-mm, mm, by=(2*mm)/1000)
  
  hist0 <- hist(x, breaks=breaks, plot=F)
  hist1 <- hist(y, breaks=breaks, plot=F)
  
  # create a grid and check it out to make sure that it's what we want
  pp <- layout(matrix(c(2,0,1,3), 2, 2, byrow=T), c(3,1), c(1,3), T)
  layout.show(pp)
  
  rang <- c(-mm, mm)
  
  par(mar=c(3,3,1,1))
  plot(x, y, xlim=rang, ylim=rang, xlab='', ylab='')
  
  # now plot marginals
  top <- max(hist0$counts, hist1$counts)
  par(mar=c(0,3,1,1))
  barplot(hist0$counts, axes=F, ylim=c(0, top), space=0)
  
  par(mar=c(3,0,1,1))
  barplot(hist1$counts, axes=F, xlim=c(0,top), space=0, horiz=T)
}
kbicreq<-function(fData,n.pheno=1,...){
  #genos<-fData$geno[[1]]$data
  genos<-pull.geno(fData)
  phenos<-pull.pheno(fData,pheno.col=n.pheno)
  
  oVal<-array(0,dim=c(sum(!is.na(phenos)),dim(genos)[2]))
  for(i in 1:dim(genos)[2]) {
    tempVal<-genos[,i]
    oVal[,i]<-tempVal[!is.na(phenos)]
    }
  phenos<-phenos[!is.na(phenos)]
  genos<-oVal
  print(length(phenos))
  print(dim(genos))
  phenos[!is.na(phenos)]
  
  outData<-bicreg.qtl(genos,phenos,...)
  #outData<-bicreg2(genos,phenos)
  plot(outData$size,outData$r2,main=names(fData$pheno)[n.pheno])
  outData
}
# do a permuation analysis
soperm<- function(fData,cPheno,n.perm=100,savePlot=T,n.cluster=2,...) {
  v1<-scanone(fData,pheno.col=cPheno)
  sp1<-scanone(fData,pheno.col=cPheno,n.perm=n.perm,n.cluster=n.cluster,...)
  thresh1<-summary(sp1,alpha=c(0.63,0.1,0.05),main=fData)
  cName=names(fData$pheno)[cPheno]
  if (savePlot) {
    pdf(paste("thresh_",cName,".pdf",sep=""))
  }
  plot(v1,main=cName)
  ktlin<-function(val,col) {abline(h=val,lty="dotted",lwd=3,col=col)} 
  ktlin(thresh1[1],col="blue")
  ktlin(thresh1[2],col="green")
  ktlin(thresh1[3],col="red")
  dev.off()
  
  summary(v1,perm=sp1,lodcol=1,alpha=0.1) # gTags
}

sum2mark<-function(fData,summaryData) {
  marks<-c()
  for (i in 1:length(summaryData[[1]])) {
    marks[[i]]<-find.marker(fData,chr=summaryData[i,1],pos=summaryData[i,2])
  }
  marks
}
extEffect2<- function(fData,cPheno,mark1) {
  cStats<-summary(fData$pheno[[cPheno]])
  ylim<-c(cStats[2],cStats[5])
  spxgPlot(fData,pheno.col=cPheno,marker=mark1,ylim=ylim,jitter=0.15,col=c("black"))
  par(new=T)
  spxgPlot(extSubset(fData,cPheno,0.75),pheno.col=cPheno,ylim=ylim,jitter=0.075,marker=mark1,col=c("blue"))
  par(new=T)
  spxgPlot(extSubset(fData,cPheno,2.0),pheno.col=cPheno,ylim=ylim,jitter=0,marker=mark1,col=c("red"))
}
extEffect<- function(fData,cPheno,mark1) {
  cStats<-summary(fData$pheno[[cPheno]])
  ylim<-c(cStats[2],cStats[5])
  plot.pxg(fData,pheno.col=cPheno,marker=mark1,ylim=ylim,jitter=1,col=c("black"))
  par(new=T)
  plot.pxg(extSubset(fData,cPheno,0.5),pheno.col=cPheno,ylim=ylim,jitter=0.5,marker=mark1,col=c("blue"))
  par(new=T)
  plot.pxg(extSubset(fData,cPheno,1.5),pheno.col=cPheno,ylim=ylim,jitter=0,marker=mark1,col=c("red"))
}
extMcMc<- function(fData,stepSize,cPheno,sFact) {
qbf2<-qb.genoprob(extSubset(fData,cPheno,sFact),step=stepSize)
qb.mcmc(qbf2, pheno.col = cPheno)
}

extSubset<- function(fData,cPheno,sFact) {
  cVar<-fData$pheno[[cPheno]]
  subset(fData,ind=skipSamples(cVar,sFact))
}
# look at the extreme cases
extCases<- function(fData,cPheno) {
  cVar<-fData$pheno[[cPheno]]
  em<-c()
  aData<-subset(fData,ind=skipSamples(cVar,0.5))
  summary(aData$pheno)
  bData<-subset(fData,ind=skipSamples(cVar,1))
  summary(bData$pheno)
  cData<-subset(fData,ind=skipSamples(cVar,1.5))
  summary(cData$pheno)
  dData<-subset(fData,ind=skipSamples(cVar,2))
  if (FALSE) {
    em[[1]]<-scanone(aData,pheno.col=cPheno)
    em[[2]]<-scanone(bData,pheno.col=cPheno)
    em[[3]]<-scanone(cData,pheno.col=cPheno)
    #em[[4]]<-scanone(fData,pheno.col=cPheno,ind.noqtl=skipSamples(cVar,1.5))
    #em[[5]]<-scanone(fData,pheno.col=cPheno,ind.noqtl=skipSamples(cVar,3))
    tLimit<-c(0,10)
    #plot(em[[1]],col="black",incl.markers=TRUE,asp=50,ylim=tLimit,lw=1)
    plot(em[[1]],em[[2]],em[[3]],incl.markers=TRUE)
  }
  if (TRUE) {
    par(new=F)
    effectscan(aData,pheno.col=cPheno,get.se=T)
    par(new=T)
    effectscan(cData,pheno.col=cPheno,get.se=T)
  }
  
  em
}
smartMapJoin<- function(amap,bmap) {
newCols=unique(c(names(amap),names(bmap)))
outMap<-amap[newCols]
isMissing<-is.na(outMap)
outMap[isMissing]<-bmap[newCols[isMissing]]
names(outMap)<-newCols
sort(outMap)
}


calc.genoprob_temp<-function (cross,crossT, step = 0, off.end = 0, error.prob = 1e-04, map.function = c("haldane", 
    "kosambi", "c-f", "morgan"), stepwidth = c("fixed", "variable", 
    "max"),smartJoin=TRUE,flipJoin=FALSE) 
{
    if (!any(class(cross) == "cross")) 
        stop("Input should have class \"cross\".")
    if (!any(class(crossT) == "cross")) 
        stop("Input should have class \"cross\".")
    map.function <- match.arg(map.function)
    if (map.function == "kosambi") 
        mf <- mf.k
    else if (map.function == "c-f") 
        mf <- mf.cf
    else if (map.function == "morgan") 
        mf <- mf.m
    else mf <- mf.h
    stepwidth <- match.arg(stepwidth)
    if (error.prob < 1e-50) 
        error.prob <- 1e-50
    if (error.prob > 1) {
        error.prob <- 1 - 1e-50
        warning("error.prob shouldn't be > 1!")
    }
    n.ind <- nind(cross)
    n.chr <- nchr(cross)
    n.mar <- nmar(cross)
    type <- class(cross)[1]
    for (i in 1:n.chr) {
        if (n.mar[i] == 1) 
            temp.offend <- max(c(off.end, 5))
        else temp.offend <- off.end
        chrtype <- class(cross$geno[[i]])
        if (chrtype == "X") 
            xchr <- TRUE
        else xchr <- FALSE
        if (type == "f2") {
            one.map <- TRUE
            if (!xchr) {
                cfunc <- "calc_genoprob_f2"
                n.gen <- 3
                gen.names <- getgenonames("f2", "A", cross.attr = attributes(cross))
            }
            else {
                cfunc <- "calc_genoprob_bc"
                n.gen <- 2
                gen.names <- c("g1", "g2")
            }
        }
        else if (type == "bc") {
            cfunc <- "calc_genoprob_bc"
            n.gen <- 2
            if (!xchr) 
                gen.names <- getgenonames("bc", "A", cross.attr = attributes(cross))
            else gen.names <- c("g1", "g2")
            one.map <- TRUE
        }
        else if (type == "riself" || type == "risib" || type == 
            "dh") {
            cfunc <- "calc_genoprob_bc"
            n.gen <- 2
            gen.names <- getgenonames(type, "A", cross.attr = attributes(cross))
            one.map <- TRUE
        }
        else if (type == "4way") {
            cfunc <- "calc_genoprob_4way"
            n.gen <- 4
            one.map <- FALSE
            gen.names <- getgenonames(type, "A", cross.attr = attributes(cross))
        }
        else if (type == "ri8sib" || type == "ri4sib" || type == 
            "ri8self" || type == "ri4self") {
            cfunc <- paste("calc_genoprob_", type, sep = "")
            n.gen <- as.numeric(substr(type, 3, 3))
            one.map <- TRUE
            gen.names <- LETTERS[1:n.gen]
            if (xchr) 
                warning("calc.genoprob not working properly for the X chromosome for 4- or 8-way RIL.")
        }
        else stop("calc.genoprob not available for cross type ", 
            type, ".")
        gen <- cross$geno[[i]]$data
        gen[is.na(gen)] <- 0
        if (smartJoin) {
        	if (flipJoin) combMap<-smartMapJoin(crossT$geno[[i]]$map,cross$geno[[i]]$map)
        	else combMap<-smartMapJoin(cross$geno[[i]]$map,crossT$geno[[i]]$map)
        } else {
        	combMap<-crossT$geno[[i]]$map
        }
        if (one.map) {
            map <- create.map(combMap, step, temp.offend, 
                stepwidth)
            rf <- mf(diff(map))
            if (type == "risib" || type == "riself") 
                rf <- adjust.rf.ri(rf, substr(type, 3, nchar(type)), 
                  chrtype)
            rf[rf < 1e-14] <- 1e-14
            newgen <- matrix(ncol = length(map), nrow = nrow(gen))
            dimnames(newgen) <- list(NULL, names(map))
            newgen[, colnames(gen)] <- gen
            newgen[is.na(newgen)] <- 0
            n.pos <- ncol(newgen)
            marnames <- names(map)
        }
        else {
            map <- create.map(combMap, step, temp.offend, 
                stepwidth)
            rf <- mf(diff(map[1, ]))
            rf[rf < 1e-14] <- 1e-14
            rf2 <- mf(diff(map[2, ]))
            rf2[rf2 < 1e-14] <- 1e-14
            newgen <- matrix(ncol = ncol(map), nrow = nrow(gen))
            dimnames(newgen) <- list(NULL, dimnames(map)[[2]])
            newgen[, colnames(gen)] <- gen
            newgen[is.na(newgen)] <- 0
            n.pos <- ncol(newgen)
            marnames <- colnames(map)
        }
        if (one.map) {
            z <- .C(cfunc, as.integer(n.ind), as.integer(n.pos), 
                as.integer(newgen), as.double(rf), as.double(error.prob), 
                genoprob = as.double(rep(0, n.gen * n.ind * n.pos)), 
                PACKAGE = "qtl")
        }
        else {
            z <- .C(cfunc, as.integer(n.ind), as.integer(n.pos), 
                as.integer(newgen), as.double(rf), as.double(rf2), 
                as.double(error.prob), genoprob = as.double(rep(0, 
                  n.gen * n.ind * n.pos)), PACKAGE = "qtl")
        }
        cross$geno[[i]]$prob <- array(z$genoprob, dim = c(n.ind, 
            n.pos, n.gen))
        dimnames(cross$geno[[i]]$prob) <- list(NULL, marnames, 
            gen.names)
        attr(cross$geno[[i]]$prob, "map") <- map
        attr(cross$geno[[i]]$prob, "error.prob") <- error.prob
        attr(cross$geno[[i]]$prob, "step") <- step
        attr(cross$geno[[i]]$prob, "off.end") <- temp.offend
        attr(cross$geno[[i]]$prob, "map.function") <- map.function
        attr(cross$geno[[i]]$prob, "stepwidth") <- stepwidth
    }
    if (type == "ri4self" || type == "ri4sib" || type == "ri8self" || 
        type == "ri8sib") 
        cross <- reorgRIgenoprob(cross)
    cross
}
kscanmany<-function (cross, pheno.cols, ...) {
	result<-c()
	
	n.ph=length(pheno.cols)
	for (i in 1:n.ph) {
		result[[i]]<-scanone(cross,pheno.col=pheno.cols[i],...)
		names(result[[i]])[3]<-paste("LOD",names(cross$pheno)[pheno.cols[i]])
	}
	class(result) <- c(class(cross), "mqmmulti")
	result
}

qb.genoprob_temp<-function (cross,crossT, map.function = map.functions, step = 2, tolerance =
1e-06, 
    stepwidth = "variable", ...) 
{
    map.functions <-
unlist(as.list(formals(calc.genoprob_temp)$map.function)[-1])
    map.function <- match.arg(tolower(map.function[1]), map.function)
    if (any(sapply(pull.map(cross), function(x) ifelse(length(x) > 
        1, max(diff(x)) < tolerance, FALSE)))) 
        cross <- jittermap(cross, tolerance)
        crossT <- jittermap(crossT, tolerance)
    cross <- calc.genoprob_temp(cross,crossT, step, map.function = map.function, 
        stepwidth = stepwidth, ...)
    attr(cross$geno[[1]]$prob, "tolerance") <- tolerance
    return(cross)
}

kmscanmany<-function (cross, pheno.cols, multicore = TRUE, n.clusters = 1, batchsize = 10, 
    cofactors = NULL, ...) 
{
    crossSub<-cross
    crossSub$pheno<-cross$pheno[pheno.cols]
    scanall(cross = crossSub, multicore = multicore, n.clusters =
n.clusters, 
        batchsize = batchsize, cofactors = cofactors, ..., scanfunction
= mqmscan)
}

kmtrait<-function (result, type = c("lines","chr","chre", "image", "contour", "3Dplot"), 
    group = NULL, meanprofile = c("none", "mean", "median","pheno"),meancol=0, 
    theta = 30, phi = 15,ylim=NULL,show.legend=TRUE, ...) 
{
    type <- match.arg(type)
    meanprofile <- match.arg(meanprofile)
    if (class(result)[2] != "mqmmulti") {
        warning("Wrong type of result file, please supply a valid mqmmulti
object.")
    }
    n.pheno <- length(result)
    temp <- lapply(result, getThird)
    chrs <- unique(lapply(result, getChr))
    qtldata <- do.call("rbind", temp)
    cScanName<-function(cScan) { paste("D",names(cScan)[3],sep="") }
    
    if (meanprofile == "pheno") {
    	meandata<-qtldata[meancol, ]
    	meanname<-names(result[[meancol]])[3]
    	
    	if (is.null(group)) {
        	group <- 1:n.pheno
        	group <- group[1:n.pheno != meancol]
        	
    	}
    	inscolors <- rainbow(length(group))
    	colors <- rep("white",n.pheno)
    	colors[group]<-inscolors
    	qtldata<-qtldata[group, ]
    	
    } else {	
    	if (!is.null(group)) {
        	qtldata <- qtldata[group, ]
        	colors <- rep("blue", n.pheno)
    	} else {
        	group <- 1:n.pheno
        	colors <- rainbow(n.pheno)
    	}
    }
    plotNames<-do.call("rbind",lapply(result,cScanName))[group]
    print(plotNames)
    qtldata <- t(qtldata)
    if (type == "contour") {
        contour(x = seq(1, dim(qtldata)[1]), y = seq(1,
dim(qtldata)[2]), 
            qtldata, xlab = "Markers", ylab = "Trait", ...)
        for (x in unique(chrs[[1]])) {
            abline(v = sum(as.numeric(chrs[[1]]) <= x))
        }
    }
    if (type == "image") {
        image(x = 1:dim(qtldata)[1], y = 1:dim(qtldata)[2], qtldata, 
            xlab = "Markers", ylab = "Trait", ...)
        for (x in unique(chrs[[1]])) {
            abline(v = sum(as.numeric(chrs[[1]]) <= x))
        }
    }
    if (type == "3Dplot") {
        persp(x = 1:dim(qtldata)[1], y = 1:dim(qtldata)[2], qtldata, 
            theta = theta, phi = phi, expand = 1, col = "gray", 
            xlab = "Markers", ylab = "Traits", zlab = "LOD score")
    }
    if (type == "chr") {
   		first <- TRUE
        if (is.null(ylim)) ylim = c(-10, max(qtldata))
        if (meanprofile == "pheno") qtldata<-qtldata-t(sapply(meandata, function (x) rep(x,length(group))))
        print(group)
        outLab<-as.factor(as.vector(sapply(group, function (x) rep(x,length(meandata)))))
        
        boxplot(as.vector(qtldata)~outLab)
        #return(outLab)
        invisible()
        
    }
    if (type == "chre") {
   		first <- TRUE
        if (is.null(ylim)) ylim = c(-10, max(qtldata))
        meanmat<-t(sapply(meandata, function (x) rep(x,length(group))))
        if (meanprofile == "pheno") qtldata<-qtldata-meanmat
        print(group)
        outLab<-as.factor(as.integer(meanmat))
        
        boxplot(as.vector(qtldata)~outLab)
        #return(outLab)
        invisible()
        
    }
    if (type == "lines") {
        first <- TRUE
        if (is.null(ylim)) ylim = c(-10, max(qtldata))
        for (i in group) {
            cResult<-result[[i]]
            if (meanprofile == "pheno") cResult[,3]<-cResult[,3]-meandata
            
            if (first) {
                plot(cResult, ylim = ylim, 
                  col = colors[i], lwd = 2, ylab = "LOD score", 
                  xlab = "Markers", main = "Multiple profiles", 
                  ...)
                first <- FALSE
            }
            else {
                plot(cResult, add = TRUE, col = colors[i], 
                  lwd = 2, ...)
            }
        }
        if (meanprofile != "none") {
            temp <- result[[1]]
            if (meanprofile == "median") {
                temp[, 3] <- apply(qtldata, 1, median)
                if (show.legend) legend("topright", c("QTL profiles", "Median profile"), 
                  col = c("blue", "black"), lwd = c(2, 4))
            }
            if (meanprofile == "mean") {
                temp[, 3] <- rowMeans(qtldata)
                if (show.legend) legend("topright", c("QTL profiles", "Mean profile"), 
                  col = c("blue", "black"), lwd = c(2, 4))
            }
            if (meanprofile == "pheno") {
                temp[, 3] <- meandata
                if (show.legend) legend("topright", c(plotNames, meanname), 
                  col = c(colors[group], "black"),lwd=c(rep(2,length(plotNames)),4))
            }
            plot(temp, add = TRUE, col = "black", lwd = 4, ...)
        }
    }
}



kmsoplot<-function (x, x2, x3, chr, lodcolumn = 1, incl.markers = TRUE, 
    xlim, ylim, lty = 1, col = c("black", "blue", "red"), lwd = 2, 
    add = FALSE, gap = 25, mtick = c("line", "triangle"), show.marker.names = FALSE, 
    alternate.chrid = FALSE, bandcol = NULL, ...) 
{
    if (!any(class(x) == "scanone") || (!missing(x2) && !any(class(x2) == 
        "scanone")) || (!missing(x3) && !any(class(x3) == "scanone"))) 
        stop("Input should have class \"scanone\".")
    if (!is.factor(x$chr)) 
        x$chr <- factor(x$chr, levels = unique(x$chr))
    dots <- list(...)
    mtick <- match.arg(mtick)
    if (length(dim(x)) != 2) 
        stop("Argument x must be a matrix or data.frame.")
    if (!missing(x2) && length(dim(x2)) != 2) 
        stop("Argument x2 must be a matrix or data.frame.")
    if (!missing(x3) && length(dim(x3)) != 2) 
        stop("Argument x3 must be a matrix or data.frame.")
    if (length(lodcolumn) == 1) 
        lodcolumn <- rep(lodcolumn, 3)[1:3]
    else if (length(lodcolumn) == 2) {
        if (missing(x2)) 
            x2 <- x
        lodcolumn <- lodcolumn[c(1, 2, 3)]
    }
    else {
        if (missing(x2)) 
            x2 <- x
        if (missing(x3)) 
            x3 <- x
    }
    lodcolumn <- lodcolumn + 2
    second <- third <- TRUE
    if (missing(x2) && missing(x3)) 
        second <- third <- FALSE
    if (missing(x3)) 
        third <- FALSE
    if (missing(x2)) 
        second <- FALSE
    if (lodcolumn[1] > ncol(x) || (second && lodcolumn[2] > ncol(x2)) || 
        (third && lodcolumn[3] > ncol(x3))) 
        stop("Argument lodcolumn misspecified.")
    out <- x[, c(1:2, lodcolumn[1])]
    if (second) 
        out2 <- x2[, c(1:2, lodcolumn[2])]
    if (third) 
        out3 <- x3[, c(1:2, lodcolumn[3])]
    if (length(lty) == 1) 
        lty <- rep(lty, 3)
    if (length(lwd) == 1) 
        lwd <- rep(lwd, 3)
    if (length(col) == 1) 
        col <- rep(col, 3)
    if (missing(chr) || length(chr) == 0) 
        chr <- unique(as.character(out[, 1]))
    else chr <- matchchr(chr, unique(out[, 1]))
    out <- out[!is.na(match(out[, 1], chr)), ]
    if (second) 
        out2 <- out2[!is.na(match(out2[, 1], chr)), ]
    if (third) 
        out3 <- out3[!is.na(match(out3[, 1], chr)), ]
    onechr <- FALSE
    if (length(chr) == 1) {
        gap <- 0
        onechr <- TRUE
    }
    temp <- out
    begend <- matrix(unlist(tapply(temp[, 2], temp[, 1], range)), 
        ncol = 2, byrow = TRUE)
    rownames(begend) <- unique(out[, 1])
    begend <- begend[as.character(chr), , drop = FALSE]
    len <- begend[, 2] - begend[, 1]
    if (!onechr) 
        start <- c(0, cumsum(len + gap)) - c(begend[, 1], 0)
    else start <- 0
    maxx <- sum(len + gap) - gap
    if (all(is.na(out[, 3]))) 
        maxy <- 1
    else maxy <- max(out[, 3], na.rm = TRUE)
    if (second) 
        maxy <- max(c(maxy, out2[, 3]), na.rm = TRUE)
    if (third) 
        maxy <- max(c(maxy, out3[, 3]), na.rm = TRUE)
    old.xpd <- par("xpd")
    old.las <- par("las")
    par(xpd = FALSE, las = 1)
    on.exit(par(xpd = old.xpd, las = old.las))
    if (missing(ylim)) 
        ylim <- c(0, maxy)
    if (missing(xlim)) {
        if (onechr) 
            xlim <- c(0, max(out[, 2]))
        else xlim <- c(-gap/2, maxx + gap/2)
    }
    if (!add) {
        if (onechr) {
            if ("ylab" %in% names(dots)) {
                if ("xlab" %in% names(dots)) {
                  plot(0, 0, ylim = ylim, xlim = xlim, type = "n", 
                    ...)
                }
                else {
                  plot(0, 0, ylim = ylim, xlim = xlim, type = "n", 
                    xlab = "Map position (cM)", ...)
                }
            }
            else {
                if ("xlab" %in% names(dots)) {
                  plot(0, 0, ylim = ylim, xlim = xlim, type = "n", 
                    ylab = dimnames(out)[[2]][3], ...)
                }
                else {
                  plot(0, 0, ylim = ylim, xlim = xlim, type = "n", 
                    xlab = "Map position (cM)", ylab = dimnames(out)[[2]][3], 
                    ...)
                }
            }
        }
        else {
            if ("ylab" %in% names(dots)) {
                if ("xlab" %in% names(dots)) {
                  plot(0, 0, ylim = ylim, xlim = xlim, type = "n", 
                    xaxt = "n", xaxs = "i", ...)
                }
                else {
                  plot(0, 0, ylim = ylim, xlim = xlim, type = "n", 
                    xaxt = "n", xlab = "Chromosome", xaxs = "i", 
                    ...)
                }
            }
            else {
                if ("xlab" %in% names(dots)) {
                  plot(0, 0, ylim = ylim, xlim = xlim, type = "n", 
                    xaxt = "n", ylab = dimnames(out)[[2]][3], 
                    xaxs = "i", ...)
                }
                else {
                  plot(0, 0, ylim = ylim, xlim = xlim, type = "n", 
                    xaxt = "n", xlab = "Chromosome", ylab = dimnames(out)[[2]][3], 
                    xaxs = "i", ...)
                }
            }
        }
    }
    if (!add && !onechr && !is.null(bandcol)) {
        u <- par("usr")
        for (i in seq(2, by = 2, length(chr))) {
            rect(min(out[out[, 1] == chr[i], 2]) + start[i] - 
                gap/2, u[3], max(out[out[, 1] == chr[i], 2]) + 
                start[i] + gap/2, u[4], border = bandcol, col = bandcol)
        }
        abline(h = u[3:4])
    }
    xtick <- NULL
    xticklabel <- NULL
    for (i in 1:length(chr)) {
        x <- out[out[, 1] == chr[i], 2] + start[i]
        y <- out[out[, 1] == chr[i], 3]
        if (length(x) == 1) {
            g <- max(gap/10, 2)
            x <- c(x - g, x, x + g)
            y <- rep(y, 3)
        }
        lines(x, y, lwd = lwd[1], lty = lty[1], col = col[1])
        if (!add && !onechr) {
            tloc <- mean(c(min(x), max(x)))
            xtick <- c(xtick, tloc)
            xticklabel <- c(xticklabel, as.character(chr[i]))
        }
        if (second) {
            x <- out2[out2[, 1] == chr[i], 2] + start[i]
            y <- out2[out2[, 1] == chr[i], 3]
            if (length(x) == 1) {
                g <- max(gap/10, 2)
                x <- c(x - g, x, x + g)
                y <- rep(y, 3)
            }
            lines(x, y, lty = lty[2], col = col[2], lwd = lwd[2])
        }
        if (third) {
            x <- out3[out3[, 1] == chr[i], 2] + start[i]
            y <- out3[out3[, 1] == chr[i], 3]
            if (length(x) == 1) {
                g <- max(gap/10, 2)
                x <- c(x - g, x, x + g)
                y <- rep(y, 3)
            }
            lines(x, y, lty = lty[3], col = col[3], lwd = lwd[3])
        }
        if (!add) {
            nam <- dimnames(out)[[1]][out[, 1] == chr[i]]
            wh.genoprob <- grep("^c.+\\.loc-*[0-9]+", nam)
            if (length(wh.genoprob) == 0) 
                wh.genoprob <- seq(along = nam)
            else wh.genoprob <- (seq(along = nam))[-wh.genoprob]
            pos <- out[out[, 1] == chr[i], 2][wh.genoprob] + 
                start[i]
            if (incl.markers) {
                if (mtick == "line") 
                  rug(pos, 0.02, quiet = TRUE)
                else {
                  a <- par("usr")
                  points(pos, rep(a[3] + diff(a[3:4]) * 0.01, 
                    length(pos)), pch = 17, cex = 1.5)
                }
            }
            if (show.marker.names) {
                a <- par("usr")
                text(pos, rep(a[3] + diff(a[3:4]) * 0.03, length(pos)), 
                  nam[wh.genoprob], srt = 90, adj = c(0, 0.5))
            }
        }
    }
    if (!add && !onechr) {
        if (!alternate.chrid || length(xtick) < 2) {
            for (i in seq(along = xtick)) axis(side = 1, at = xtick[i], 
                labels = xticklabel[i])
        }
        else {
            odd <- seq(1, length(xtick), by = 2)
            even <- seq(2, length(xtick), by = 2)
            for (i in odd) {
                axis(side = 1, at = xtick[i], labels = "")
                axis(side = 1, at = xtick[i], labels = xticklabel[i], 
                  line = -0.4, tick = FALSE)
            }
            for (i in even) {
                axis(side = 1, at = xtick[i], labels = "")
                axis(side = 1, at = xtick[i], labels = xticklabel[i], 
                  line = +0.4, tick = FALSE)
            }
        }
    }
    invisible()
}


kscanplot<-function (chrVal,posVal,plotVal,chr, incl.markers = TRUE, 
    xlim, ylim, lty = 1, col = c("black", "blue", "red"), lwd = 2, 
    add = FALSE, gap = 25, mtick = c("line", "triangle"), show.marker.names = FALSE, 
    alternate.chrid = FALSE, bandcol = NULL, ...) 
{
    if (!is.factor(chrVal)) 
        chrVal <- factor(chrVal, levels = unique(chrVal))
    dots <- list(...)
    mtick <- match.arg(mtick)
    
   
    
    if (length(lty) == 1) 
        lty <- rep(lty, 3)
    if (length(lwd) == 1) 
        lwd <- rep(lwd, 3)
    if (length(col) == 1) 
        col <- rep(col, 3)
    if (missing(chr) || length(chr) == 0) 
        chr <- unique(as.character(chrVal))
    else chr <- matchchr(chr, unique(chrVal))
    
    posVal <- posVal[!is.na(match(chrVal, chr))]
    plotVal <- plotVal[!is.na(match(chrVal, chr))]
    chrVal <- chrVal[!is.na(match(chrVal, chr))]

    onechr <- FALSE
    if (length(chr) == 1) {
        gap <- 0
        onechr <- TRUE
    }
    begend <- matrix(unlist(tapply(posVal, chrVal, range)), 
        ncol = 2, byrow = TRUE)
    rownames(begend) <- unique(chrVal)
    begend <- begend[as.character(chr), , drop = FALSE]
    len <- begend[, 2] - begend[, 1]
    if (!onechr) 
        start <- c(0, cumsum(len + gap)) - c(begend[, 1], 0)
    else start <- 0
    maxx <- sum(len + gap) - gap
    if (all(is.na(plotVal))) 
        maxy <- 1
    else maxy <- max(plotVal, na.rm = TRUE)
    
    old.xpd <- par("xpd")
    old.las <- par("las")
    par(xpd = FALSE, las = 1)
    on.exit(par(xpd = old.xpd, las = old.las))
    if (missing(ylim)) 
        ylim <- c(0, maxy)
    if (missing(xlim)) {
        if (onechr) 
            xlim <- c(0, max(posVal))
        else xlim <- c(-gap/2, maxx + gap/2)
    }
    if (!add) {
        if (onechr) {
            if ("ylab" %in% names(dots)) {
                if ("xlab" %in% names(dots)) {
                  plot(0, 0, ylim = ylim, xlim = xlim, type = "n", 
                    ...)
                }
                else {
                  plot(0, 0, ylim = ylim, xlim = xlim, type = "n", 
                    xlab = "Map position (cM)", ...)
                }
            }
            else {
                if ("xlab" %in% names(dots)) {
                  plot(0, 0, ylim = ylim, xlim = xlim, type = "n", 
                    ylab ="Markers", ...)
                }
                else {
                  plot(0, 0, ylim = ylim, xlim = xlim, type = "n", 
                    xlab = "Map position (cM)", ylab = posVal, 
                    ...)
                }
            }
        }
        else {
            if ("ylab" %in% names(dots)) {
                if ("xlab" %in% names(dots)) {
                  plot(0, 0, ylim = ylim, xlim = xlim, type = "n", 
                    xaxt = "n", xaxs = "i", ...)
                }
                else {
                  plot(0, 0, ylim = ylim, xlim = xlim, type = "n", 
                    xaxt = "n", xlab = "Chromosome", xaxs = "i", 
                    ...)
                }
            }
            else {
                if ("xlab" %in% names(dots)) {
                  plot(0, 0, ylim = ylim, xlim = xlim, type = "n", 
                    xaxt = "n", ylab = "Markers", 
                    xaxs = "i", ...)
                }
                else {
                  plot(0, 0, ylim = ylim, xlim = xlim, type = "n", 
                    xaxt = "n", xlab = "Chromosome", ylab = posVal, 
                    xaxs = "i", ...)
                }
            }
        }
    }
    if (!add && !onechr && !is.null(bandcol)) {
        u <- par("usr")
        for (i in seq(2, by = 2, length(chr))) {
            rect(min(posVal[chrVal == chr[i]]) + start[i] - 
                gap/2, u[3], max(posVal[chrVal[, 1] == chr[i]]) + 
                start[i] + gap/2, u[4], border = bandcol, col = bandcol)
        }
        abline(h = u[3:4])
    }
    xtick <- NULL
    xticklabel <- NULL
    for (i in 1:length(chr)) {
        x <- posVal[chrVal == chr[i]] + start[i]
        y <- plotVal[chrVal == chr[i]]
        if (length(x) == 1) {
            g <- max(gap/10, 2)
            x <- c(x - g, x, x + g)
            y <- rep(y, 3)
        }
        lines(x, y, lwd = lwd[1], lty = lty[1], col = col[1])
        if (!add && !onechr) {
            tloc <- mean(c(min(x), max(x)))
            xtick <- c(xtick, tloc)
            xticklabel <- c(xticklabel, as.character(chr[i]))
        }
        if (!add) {
            nam <- chrVal[chrVal == chr[i]]
            wh.genoprob <- grep("^c.+\\.loc-*[0-9]+", nam)
            if (length(wh.genoprob) == 0) 
                wh.genoprob <- seq(along = nam)
            else wh.genoprob <- (seq(along = nam))[-wh.genoprob]
            pos <- posVal[chrVal == chr[i]][wh.genoprob] + 
                start[i]
            if (incl.markers) {
                if (mtick == "line") 
                  rug(pos, 0.02, quiet = TRUE)
                else {
                  a <- par("usr")
                  points(pos, rep(a[3] + diff(a[3:4]) * 0.01, 
                    length(pos)), pch = 17, cex = 1.5)
                }
            }
            if (show.marker.names) {
                a <- par("usr")
                text(pos, rep(a[3] + diff(a[3:4]) * 0.03, length(pos)), 
                  nam[wh.genoprob], srt = 90, adj = c(0, 0.5))
            }
        }
    }
    if (!add && !onechr) {
        if (!alternate.chrid || length(xtick) < 2) {
            for (i in seq(along = xtick)) axis(side = 1, at = xtick[i], 
                labels = xticklabel[i])
        }
        else {
            odd <- seq(1, length(xtick), by = 2)
            even <- seq(2, length(xtick), by = 2)
            for (i in odd) {
                axis(side = 1, at = xtick[i], labels = "")
                axis(side = 1, at = xtick[i], labels = xticklabel[i], 
                  line = -0.4, tick = FALSE)
            }
            for (i in even) {
                axis(side = 1, at = xtick[i], labels = "")
                axis(side = 1, at = xtick[i], labels = xticklabel[i], 
                  line = +0.4, tick = FALSE)
            }
        }
    }
    invisible()
}

