import os, sys
from numpy import array, max, min, mean, single, sqrt
from math import atan2 as radAtan2
import numpy
import numpy.linalg

atan2 = lambda x, y: 180 / 3.1415 * radAtan2(x, y)  # define in terms of degrees


class mergefiles(file):
    def __init__(self, filelist, headerlen=2):
        file.__init__(self, filelist[0])
        print filelist
        self.files = [open(cfile, "r") for cfile in filelist[:]]
        self.curfile = 0
        self.nexts = 0
        self.headerlen = 2

    def next(self):
        try:
            self.nexts += 1
            return self.files[self.curfile].next()
        except StopIteration:
            print "File " + str(self.files[self.curfile]) + " is empty after " + str(
                self.nexts
            ) + ", moving along " + str(self.curfile + 1)
            self.curfile += 1
            self.nexts = 0
            if self.curfile < len(self.files):
                self.files[self.curfile].seek(0)  # rewind
                self.files[self.curfile].readline()
                self.files[self.curfile].readline()
                return self.files[self.curfile].next()
            else:
                raise StopIteration

    def seek(self, pos):
        self.curfile = 0
        return self.files[self.curfile].seek(pos)


class posvec:
    def __init__(self):
        self.sumpos = numpy.zeros((3, 1))
        self.pts = 0

    def add(self, cVec):
        for i in range(3):
            self.sumpos[i] += cVec[i]
        self.pts += 1

    def pos(self):
        if self.pts > 0:
            return self.sumpos / self.pts
        else:
            return self.sumpos


class texttens:
    def __init__(self, icov=None, ipts=0, iminpts=1):
        if icov is None:
            icov = numpy.zeros((3, 3))
        self.icov = icov
        self.cov = icov
        self.pts = ipts
        self.ptlist = []
        self.tenspts = 0
        self.aiso = 0
        self.obl = 0
        self._w = [0, 0, 0]
        self._v = self.icov
        self.minpts = iminpts

    def tens(self):
        if self.pts < -1:  # self.minpts:
            self._v = numpy.zeros((3, 3))
            self.cov = self._v
        elif self.pts > self.tenspts:
            self.cov = self.icov / self.pts
            try:
                (self._w, self._v) = numpy.linalg.eigh(self.cov)
            except:
                print "Failed EIGH!!!" + str(self.cov)
                self._v = numpy.zeros((3, 3))
                self.cov = self._v
                self.aiso = 0
                self.tenspts = self.pts
                return self.cov
            self.aiso = 3.0 / 2.0 * (self._w[2] / sum(self._w) - 1.0 / 3.0) * 100
            self.aiso = abs(self._w[0] - self._w[2]) / self._w[2]  # New definition
            self.obl = 2 * (self._w[1] - self._w[2]) / (self._w[0] - self._w[2]) - 1.0
            self.tenspts = self.pts  # serves as a cache
        return self.cov

    def logtens(self):
        self.tens()
        tw = numpy.log(self._w)
        lw = numpy.eye(3)
        for i in range(3):
            lw[i, i] = tw[i]
        try:
            return numpy.dot(numpy.dot(self._v, lw), numpy.linalg.inv(self._v))
        except:
            print ("LogTextFail!", self._v, self.cov)

            return numpy.zeros((3, 3))

    def mean(self):
        return numpy.sqrt(self.trace())

    def v1(self):
        self.tens()
        oV = self._v[:, 2]
        if oV[0] < 0:
            oV *= -1  # force all vectors to have a positive x1
        return oV

    def trace(self):
        return self.tens().trace()

    def devamp(self):  # deviatoric amplitude
        tcov = self.tens() - numpy.eye(3) * (self.trace() / 3)
        # (jw,jv)=numpy.linalg.eig(tcov)
        jw = self._w - self.trace() / 3
        return numpy.sqrt(jw[0] ** 2 + jw[1] ** 2 + jw[2] ** 2)

    def add(self, cVec, weight=1.0):
        for i in range(3):
            for j in range(3):
                self.icov[i, j] += weight * cVec[i] * cVec[j]
        # dMag=sqrt(cVec[0]**2+cVec[1]**2+cVec[2]**2)
        # if (dMag>0): print (cVec,sqrt(cVec[0]**2+cVec[1]**2+cVec[2]**2))
        # self.ptlist+=[list(cVec)]
        self.pts += weight


def runTexture(
    ptxtFile,
    outFile,
    steps,
    lstep,
    dirV,
    isoSteps,
    useLstep,
    useBubbles,
    useRadPos,
    doStrain,
    keepExtremes=False,
    trimEdge=1,
    edges=None,
    minObj=2,
):
    # outFile=sys.argv[2]
    global allAlign, allAiso, allMean, allSum, allPts, allCov

    outFile = "_".join(outFile.split(";"))
    if edges is None:
        edges = open(ptxtFile, "r")
    out = open(outFile, "w")

    def getfield(fd, txt):
        spos = txt.find(fd)
        slen = txt[spos:].find(";")
        if slen > 0:
            slen += spos
        cLine = inLine[spos:slen]
        return single(cLine[cLine.find("(") + 1 : cLine.find(")")].split(","))

    inLine = edges.readline()[:-1]
    # Read and parse header
    eheader = edges.readline()[:-1].split(",")
    eheader = dict(
        [
            (b, a)
            for (a, b) in enumerate(
                ["".join(cline.upper().split("//")).strip() for cline in eheader]
            )
        ]
    )
    posC = (eheader["POS_X"], eheader["POS_Y"], eheader["POS_Z"])
    dirC = (eheader[dirV[0]], eheader[dirV[1]], eheader[dirV[2]])
    getPosXYZ = lambda x: single([x[posC[0]], x[posC[1]], x[posC[2]]])

    getPos = getPosXYZ
    getDir = lambda x: single([x[dirC[0]], x[dirC[1]], x[dirC[2]]])
    getBubble = lambda x: numpy.int_([x[0], x[1]])

    print eheader

    preScan = False
    otext = {}

    if useBubbles:
        bubpos = {}

        for cLine in edges:
            try:
                cBub = getBubble(cLine.split(","))
                if not otext.has_key(cBub[0]):
                    otext[cBub[0]] = texttens()
                if not otext.has_key(cBub[1]):
                    otext[cBub[1]] = texttens()
                if not bubpos.has_key(cBub[0]):
                    bubpos[cBub[0]] = posvec()
                if not bubpos.has_key(cBub[1]):
                    bubpos[cBub[1]] = posvec()
            except:
                print "Invalid line @ " + cLine
        edges = open(ptxtFile, "r")
        edges.readline()
        edges.readline()

        def transPosRad(cPos):
            nx = cPos[0]
            ny = cPos[1]
            return (sqrt(ny ** 2 + nx ** 2), atan2(ny, nx), cPos[2])

    else:
        recalcLimits = True
        if useRadPos:
            isoSteps = 0
        try:
            lmin = getfield("LAC_MIN", inLine)
            lmax = getfield("LAC_MAX", inLine)
            print "Read LMIN/LMAX Recal:" + str(recalcLimits)
        except:
            print "GetField failed on " + inLine + " running prescan"
            recalcLimits = True
        while recalcLimits:
            recalcLimits = False
            isEmpty = True
            twoStrikes = 0
            for cLine in edges:
                cRows = cLine.split(",")
                try:
                    cPos = getPos(cRows)
                    if isEmpty:
                        lmin = cPos
                        lmax = cPos
                        lmean = array(cPos)
                        lcount = 0
                        isEmpty = False
                    else:
                        lmin = min([cPos, lmin], 0)
                        lmax = max([cPos, lmax], 0)
                        lmean += array(cPos)
                        lcount += 1
                except:
                    print cLine + " is not valid"
                    twoStrikes += 1
                    if twoStrikes > 2:
                        break

            edges.seek(0)
            edges.readline()
            edges.readline()

            print (lmin, lmax)
            radCent = tuple(lmean / lcount)  # immutable

            def transPosRad(cPos):
                nx = cPos[0] - radCent[0]
                ny = cPos[1] - radCent[1]
                return (sqrt(ny ** 2 + nx ** 2), atan2(ny, nx), cPos[2])

            getPosRad = lambda x: transPosRad(getPosXYZ(x))
            if useRadPos:
                getPos = getPosRad
                useRadPos = False
                recalcLimits = True
        print ("min", lmin, "max", lmax)

        if useLstep:
            steps = tuple(numpy.int_(numpy.round((lmax - lmin) / lstep)))
        else:
            lstep = (lmax - lmin) / steps

        if isoSteps:
            lstep = array([mean(lstep)] * 3)
            steps = tuple(numpy.int_(numpy.round((lmax - lmin) / lstep)))
        if trimEdge >= 1:
            lmin += lstep * (0.5 * trimEdge)
            lmax -= lstep * (0.5 * trimEdge)
            steps = (steps[0] - trimEdge, steps[1] - trimEdge, steps[2] - trimEdge)
        print (lmin, lmax, lstep, steps)

    out.write(inLine[: inLine.find(";")] + "\n")

    outCols = [
        "// POS_X",
        "POS_Y",
        "POS_Z",
        "POS_R",
        "POS_TH",
        "COUNT",
        "AISO",
        "PCA1_X",
        "PCA1_Y",
        "PCA1_Z",
    ]
    for xc in "XYZ":
        for yc in "XYZ":
            outCols += ["TEXT_" + xc + yc]
    for xc in "XYZ":
        for yc in "XYZ":
            outCols += ["STRAINT_" + xc + yc]
    outCols += ["TR_STRAIN"]
    outCols += ["AMP_DSTRAIN"]
    out.write(",".join(outCols) + "\n")  # First two lines are garbage

    if not useBubbles:
        for i in range(steps[0]):
            otext[i] = {}
            for j in range(steps[1]):
                otext[i][j] = {}
                for k in range(steps[2]):
                    otext[i][j][k] = texttens(iminpts=minObj)
    avgTexture = texttens()
    for cLine in edges:
        cRows = cLine.split(",")
        try:
            cPos = getPos(cRows)
            cDir = getDir(cRows)
            avgTexture.add(cDir)
            if useBubbles:
                cBub = getBubble(cRows)
                otext[cBub[0]].add(cDir)
                otext[cBub[1]].add(cDir)
                bubpos[cBub[0]].add(cPos)
                bubpos[cBub[1]].add(cPos)
            else:
                cDex = numpy.int_(numpy.round((cPos - lmin) / lstep))

                ncDex = max([cDex, [0, 0, 0]], 0)
                ncDex = min([ncDex, [steps[0] - 1, steps[1] - 1, steps[2] - 1]], 0)
                if not keepExtremes:
                    if (
                        (ncDex[0] == cDex[0])
                        and (ncDex[1] == cDex[1])
                        and (ncDex[2] == cDex[2])
                    ):
                        otext[cDex[0]][cDex[1]][cDex[2]].add(cDir)
                else:
                    otext[ncDex[0]][ncDex[1]][ncDex[2]].add(cDir)
        except:
            print cLine + " is invalido :"

    # Calculate the Average Texture If needed
    if hasattr(doStrain, "__iter__"):
        strainCov = doStrain
        strainPts = 1
    elif doStrain < 0:
        strainPts = 0
        strainCov = numpy.zeros((3, 3))
        if useBubbles:
            for cKey in otext.keys():
                strainCov += otext[cKey].icov
                strainPts += otext[cKey].pts
        else:
            for i in range(steps[0]):
                for j in range(steps[1]):
                    for k in range(steps[2]):
                        strainCov += otext[i][j][k].icov
                        strainPts += otext[i][j][k].pts
    else:
        strainCov = doStrain * numpy.eye(3)
        strainPts = 1
    # Calculate the log of the average texture etc
    if doStrain is not 0:
        strainTens = texttens(strainCov, strainPts)

        print ("Undeformed Reference Texture : ", strainTens.tens())
        strainTens = strainTens.logtens()
    else:
        strainTens = numpy.zeros((3, 3))

    allCov = numpy.zeros((3, 3))
    allSum = 0
    allPts = 0
    allAlign = texttens()
    allAiso = 0
    allMean = 0

    # Prepare to write the output files

    def procText(ctextcla, posStr, doWrite=True):
        global allAlign, allAiso, allMean, allSum, allPts, allCov
        if ctextcla.pts >= minObj:
            ostr = posStr
            ctext = ctextcla.tens()
            # print (ctext,ctextcla.pts)
            if doWrite:
                ostr += "%i,%f" % (ctextcla.pts, ctextcla.aiso)
                ostr += ",%f,%f,%f" % tuple(ctextcla.v1())
                for ii in range(3):
                    for jj in range(3):
                        ostr += ",%f" % (ctext[ii, jj],)
                cstrain = texttens(ctextcla.logtens() - strainTens, 1)
                cstrtext = cstrain.tens()
                for ii in range(3):
                    for jj in range(3):
                        ostr += ",%f" % (cstrtext[ii, jj],)
                ostr += ",%f,%f" % (cstrain.trace(), cstrain.devamp())
                out.write(ostr + "\n")

            allAiso += ctextcla.pts * ctextcla.aiso
            allMean += ctextcla.pts * ctextcla.mean()
            allSum += ctextcla.pts
            allCov += ctextcla.icov
            allPts += 1
            allAlign.add(ctextcla.v1(), ctextcla.pts)
            # print (ctextcla.pts,ctextcla.aiso,ctextcla.v1(),ctextcla.mean())

    if useBubbles:
        for cKey in otext.keys():
            iPos = bubpos[cKey].pos()
            rPos = transPosRad(iPos)
            procText(
                otext[cKey], "%f,%f,%f,%f,%f," % tuple(list(iPos) + list(rPos[0:2]))
            )
        print ("Steps", len(otext.keys()))
    else:
        for i in range(steps[0]):
            for j in range(steps[1]):
                for k in range(steps[2]):
                    iPos = lmin + lstep * [i + 0.5, j + 0.5, k + 0.5]
                    rPos = transPosRad(iPos)
                    try:
                        procText(
                            otext[i][j][k],
                            "%f,%f,%f,%f,%f," % tuple(list(iPos) + list(rPos[0:2])),
                        )
                    except:
                        print str(otext[i][j][k]) + " failed"

        print ("Steps", steps)
    ctext = allAlign.tens()
    if allPts == 0:
        allPts = 1
    if allSum == 0:
        allSum = 0.1
    print ("Mean Tensor", texttens(allCov, allSum).mean(), allSum)
    try:
        if useBubbles:
            print ("Rand Tensor", otext.values()[1].mean(), otext.values()[1].pts)
            print ("Rand Tensor", otext.values()[-5].mean(), otext.values()[-5].pts)
        else:
            print ("Rand Tensor", otext[0][0][0].mean(), otext[0][0][0].pts)
            print ("Rand Tensor", otext[2][2][1].mean(), otext[0][0][0].pts)
    except:
        print "Sparse data eh? even random tensor failed!"
    print allCov / allSum
    print numpy.sqrt(allCov / allSum)

    return {
        "AvgTexture": avgTexture,
        "Groups": allPts,
        "PPG": allSum / allPts,
        "MeanAiso": allAiso / allSum,
        "GrMeanTr": allMean / allSum,
        "GrMeanAiso": allAlign.aiso,
        "GrMeanDir": allAlign.v1(),
    }
    out.close()


if __name__ == "__main__":
    # number of steps to take
    steps = (11, 11, 11)
    # steps=(10,36,2)
    lstep = array([42, 42, 42 * 1.5])
    # lstep=array([80,80,80]) # bernd's data
    # lstep=array([8000,8000,8000]) # bernd's data
    # should the cube have an isotropic shape
    isoSteps = True
    useLstep = False
    useBubbles = False
    useRadPos = False
    doStrain = numpy.log10(
        540
    )  # zero is off, -1 is average, positive is U0=doStrain*eye(3)
    doStrain = numpy.log10(2.25e2)
    doStrain = -1
    # doStrain=numpy.array([[numpy.log10(370),0,0],[0,numpy.log10(340),0],[0,0,numpy.log10(125)]])
    # minimum number of objects (edges, lacunae, ...) to be averaged in each box
    minObj = 2
    # the name of the direction vectors (edges are dir_..., alignment would be pca1_...)
    dirV = ["DIR_X", "DIR_Y", "DIR_Z"]
    # dirV=['PCA1_X','PCA1_Y','PCA1_Z']
    # dirV=['PCA2_X','PCA2_Y','PCA2_Z']
    ptxtFile = os.path.abspath(sys.argv[1])
    if len(sys.argv) > 2:
        outFile = os.path.abspath(sys.argv[2])
    else:
        outFile = os.path.abspath("texture.csv")
    from glob import glob

    # To open all prepared edge files in the current path
    # edges=mergefiles(glob('/'.join(ptxtFile.split('/')[:-1]+['*ptxt.csv'])))
    # To open all prepared edge files in the subdirectories of the current path

    # elist=glob('/'.join(ptxtFile.split('/')[:-1]+['*/*ptxt.csv']))
    elist = []
    if ptxtFile.find("%") >= 0:
        elist = glob("*".join(ptxtFile.split("%")))
        ptxtFile = elist[0]

    print elist
    if len(elist) > 0:
        edges = mergefiles(elist)
    else:
        edges = None
    if len(sys.argv) > 3:
        sruns = int(sys.argv[2])
        fruns = int(sys.argv[3])
        resMat = []
        for i in range(sruns, fruns, 3):
            steps = (i, i, i)
            steps = (0, i, 0)
            resMat += [
                runTexture(
                    ptxtFile,
                    steps,
                    lstep,
                    dirV,
                    isoSteps,
                    useLstep,
                    useBubbles,
                    useRadPos,
                    doStrain,
                    trimEdge=1,
                    minObj=minObj,
                )
            ]
        print "Final Results"
        for (cVal, cLine) in zip(range(sruns, fruns), resMat):
            print str(cVal) + ":" + str(cLine)
    else:
        resMat = runTexture(
            ptxtFile,
            outFile,
            steps,
            lstep,
            dirV,
            isoSteps,
            useLstep,
            useBubbles,
            useRadPos,
            doStrain,
            trimEdge=0,
            edges=edges,
        )
        print resMat
