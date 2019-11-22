import sys, os
from glob import glob
import numpy
from numpy import array, mean, min, max, sqrt
from math import atan2 as radAtan2


def atan2(x, y):
    return 180 / 3.1415 * radAtan2(x, y)


## built in rotation matrix
import math


def makeRM(th):
    tRM = numpy.eye(3)
    tRM[0, 0] = math.cos(th)
    tRM[0, 1] = -math.sin(th)
    tRM[1, 0] = math.sin(th)
    tRM[1, 1] = math.cos(th)
    return tRM


class dispVec:
    def __init__(self):
        self.nsum = array([0.0, 0.0, 0.0])
        self.wsum = array([0.0, 0.0, 0.0])
        self.wsum2 = array([0.0, 0.0, 0.0])
        self.nsum2 = array([0.0, 0.0, 0.0])
        self.count = 0
        self.weight = 0

    def add(self, vec, weight=1, vec2=[0.0] * 3):
        # print (vec,weight)
        self.nsum += array(vec)
        self.nsum2 += array(vec)
        self.wsum += weight * array(vec)
        self.wsum2 += weight * array(vec2)
        self.weight += weight
        self.count += 1

    def val(self, weighted=True):
        if self.count > 0:
            if weighted:
                return self.wsum / self.weight
            else:
                return self.nsum / self.count
        else:
            return array([0, 0, 0])

    def val2(self, weighted=True):
        if self.count > 0:
            if weighted:
                return self.wsum2 / self.weight
            else:
                return self.nsum / self.count
        else:
            return array([0, 0, 0])


# List track tracks bubbles through a list of files (within directories)
class groupTrack:
    # Parameters

    steps = (8, 8, 8)
    isoSteps = False
    useLstep = False
    isBijective = True
    useMatchFile = False
    trimEdge = 1
    keepExtremes = False
    minChainLength = -1
    diffMatchVars = True

    correctRot = False
    disableRot = False
    trackingFuncs = (
        {}
    )  # need to do these functions as a dictionary because python binds them all if you decalre them like parameters
    trackingFuncs["seedShift"] = lambda lacA, lacB: lambda nPos: [
        0,
        0,
        0,
    ]  # lip4 or coarsening foam
    trackingFuncs["wFun"] = lambda x: 1 / (1 + x)  # Weighting Function
    trackingFuncs["prescreenFun"] = lambda x, pos,: True
    trackingFuncs["getDataName"] = lambda fname: fname.split("/")[-2]
    trackingFuncs["getTimeStep"] = lambda x: int(x.split("/")[-2].split("_")[-2])
    trackingFuncs["edgePathFunction"] = lambda lacPath: "/".join(
        lacPath[:-1] + [""]
    )  # for dkg21 and mi7
    centOfRot = [0, 0, 0]
    addMatchVars = ["THICKNESS", "NEIGHBORS"]
    gMaxDist = 9e99
    maxVolDifference = 0  # maximum volume difference
    volDifferencePenalty = -1  # -1 means off

    def __init__(self, lacFileRoot, outFile=None, tlistFilterFunc=lambda x: True):
        import tracktools as tracktoolbox

        self.tracktoolbox = tracktoolbox
        print (lacFileRoot, lacFileRoot.find("%"))
        if lacFileRoot.find("%") >= 0:
            qPath = "*".join(lacFileRoot.split("%"))
        else:
            lacFileRoot = os.path.abspath(lacFileRoot)
            qPath = "/".join(
                lacFileRoot.split("/")[:-2] + ["*/" + lacFileRoot.split("/")[-1]]
            )

            # qPath='/Users/maderk/Documents/MATLAB/TextureTensor/Foam/ujax/dk31m00/dk31m17_clpor2_csv/*_0*/clpor_2.csv'
            print qPath
        tlist = sorted(glob(qPath))
        self.tlist = filter(tlistFilterFunc, tlist)
        if outFile is None:
            outFile = lacFileRoot
        print ("Initialized ", lacFileRoot, outFile[1] + "-tracked.csv")
        self.outFile = outFile
        self.outFileRaw = outFile + "_raw.txt"
        self.outFileEdges = outFile + "_edges.txt"
        self.outFileNames = outFile + "_names.txt"

    def readedges(self, lacIn):  # read in the edge file
        lacPath = lacIn.fname.split("/")
        cPath = self.trackingFuncs["edgePathFunction"](lacPath)
        # numA=int(lacPath[-2].split('_')[1])
        # print (cPath,numA,numA)
        cFile = cPath + "clpor_edge_ptxt.csv"
        return self.tracktoolbox.edgeData(cFile)

    def run(self):
        self.tracktoolbox.maxVolDifference = self.maxVolDifference
        self.tracktoolbox.volDifferencePenalty = self.volDifferencePenalty
        self.tracktoolbox.matchVars += self.addMatchVars
        if self.correctRot:  # images are rotated

            def useRotMat(cFile):
                cNum = int(cFile.split("_")[2])
                print cNum
                print makeRM(numpy.pi)
                if cNum % 2 == 0:
                    return makeRM(numpy.pi)
                else:
                    return None

        else:
            useRotMat = (
                lambda x: None
            )  # uses the matrices if they are present as clpor_2.csv_tf.dat files
        if self.disableRot:
            useRotMat = lambda x: -1  # disable rotation matrices

        lacList = []
        print (self.correctRot, useRotMat("test"))
        for curLacFile in self.tlist:
            lacList += [
                self.tracktoolbox.lacunData(
                    curLacFile, rotMat=useRotMat(curLacFile), inCentRot=self.centOfRot
                )
            ]
            try:
                print "A"
                # lacList+=[self.tracktoolbox.lacunData(curLacFile,rotMat=useRotMat(curLacFile),inCentRot=self.centOfRot)]
            except:
                print ("Error", curLacFile)
        lacList = filter(lambda x: len(x.lkeys) > 1, lacList)
        out = open(self.outFile, "w")
        outRaw = open(self.outFileRaw, "w")
        outEdges = open(self.outFileEdges, "w")
        inLine = lacList[0].preamble
        lmin = array(lacList[0].lmin[0:3])
        lmax = array(lacList[0].lmax[0:3])

        outRaw.write(
            inLine + "\n"
        )  # - LAC_MIN='+str(tuple(lmin))+'- LAC_MAX='+str(tuple(lmin))+
        outEdges.write(inLine + "\n")
        out.write(inLine + "\n")
        inLine = "//Frame,Chain,Chain_Length,Component 1,Component 2,Match_Quality,POS_X,POS_Y,POS_Z,POS_R,POS_THETA,DIR_X,DIR_Y,DIR_Z"
        # if subVec is not None:
        inLine += ",SFADIR_X,SFADIR_Y,SFADIR_Z,NEIGHBORS"
        inLine += "," + ",".join(
            self.tracktoolbox.matchVars[3:]
        )  # add standard columns
        if self.diffMatchVars:
            inLine += ",D_" + ",D_".join(
                self.tracktoolbox.matchVars[3:]
            )  # add standard columns
        inLine += "\n"
        outRaw.write(inLine)  # First two lines are garbage
        inLine = "//Count,Match_Quality,I,J,K,POS_X,POS_Y,POS_Z,POS_R,POS_THETA,DIR_X,DIR_Y,DIR_Z,NDIR_X,NDIR_Y,NDIR_Z\n"
        out.write(inLine)  # First two lines are garbage
        inLine = "//Frame,Chain,Component 1,Component 2,Match_Quality,EDGE_INFO,POS_X,POS_Y,POS_Z,POS_R,POS_THETA,EDGE_X,EDGE_Y,EDGE_Z"
        inLine += "," + ",".join(self.tracktoolbox.edgeVars[3:])  # add standard columns
        inLine += "\n"
        outEdges.write(inLine)  # First two lines are garbage
        # Calculate Values on a Grid
        print ("lmin", lmin, "lmax", lmax)
        if self.useLstep:
            self.steps = tuple(numpy.int_(numpy.round((lmax - lmin) / lstep)))
        else:
            self.lstep = (array(lmax) - array(lmin)) / self.steps

        if self.isoSteps:
            self.lstep = array([mean(lstep)] * 3)
            self.steps = tuple(numpy.int_(numpy.round((lmax - lmin) / self.lstep)))
        if self.trimEdge >= 1:
            lmin += self.lstep * (0.5 * self.trimEdge)
            lmax -= self.lstep * (0.5 * self.trimEdge)
            self.steps = (
                self.steps[0] - self.trimEdge,
                self.steps[1] - self.trimEdge,
                self.steps[2] - self.trimEdge,
            )
        print (lmin, lmax, self.lstep, self.steps)
        # Init Radial Coordinates
        radCent = tuple(
            array(lacList[0].lmin)
            + (array(lacList[0].lmax) - array(lacList[0].lmin)) / 2
        )  # immutable

        def transPosRad(cPos):
            nx = cPos[0] - radCent[0]
            ny = cPos[1] - radCent[1]
            return (sqrt(ny ** 2 + nx ** 2), atan2(ny, nx), cPos[2])

        getPosRad = lambda x: transPosRad(getPosXYZ(x))
        # Initialize Array

        otext = {}
        for i in range(self.steps[0]):
            otext[i] = {}
            for j in range(self.steps[1]):
                otext[i][j] = {}
                for k in range(self.steps[2]):
                    otext[i][j][k] = dispVec()

        # Prescreen for long chains
        allChains = self.tracktoolbox.chain()

        frameAvgDir = {}
        for (cInd, cLacs) in enumerate(zip(lacList, lacList[1:] + [lacList[-1]])):
            (lacA, lacB) = cLacs
            try:
                frameCount = trackingFuncs["getTimeStep"](lacB.fname) - trackingFuncs[
                    "getTimeStep"
                ](lacA.fname)
            except:
                print "Auto-frame count detection failed, default is : 1"
                frameCount = 1
            cShift = lambda pos: frameCount * numpy.array(
                self.trackingFuncs["seedShift"](lacA, lacB)(pos)
            )
            matchDict = lacA.match(
                lacB,
                prescreenFun=self.trackingFuncs["prescreenFun"],
                maxDist=frameCount * self.gMaxDist,
                bijTrim=self.isBijective,
                seedShift=cShift,
            )
            scoreMap = map(lambda a: a[1][1], matchDict.items())
            # print ('Pretracking',trackingFuncs['getDataName'](lacA.fname),trackingFuncs['getDataName'](lacB.fname),'Seed-Dist',cShift([250,250,250]),'Mean-Dist:',mean(scoreMap),'Max-Dist:',max(scoreMap))

            try:
                print (
                    "Pretracking",
                    self.trackingFuncs["getDataName"](lacA.fname),
                    self.trackingFuncs["getDataName"](lacB.fname),
                    "Seed-Dist",
                    cShift([250, 250, 250]),
                    "Mean-Dist:",
                    mean(scoreMap),
                    "Max-Dist:",
                    max(scoreMap),
                )
            except:
                print "Tracking Failed!"
                continue

            for (a, b) in matchDict.items():
                allChains.add((cInd, a), (cInd + 1, b[0]))
            fdirSum = array([0.0, 0.0, 0.0])
            fdirCnt = 0
            for aLac in sorted(matchDict.keys()):
                (bLac, mQual) = matchDict[aLac]
                if lacA.has_key(aLac) & lacB.has_key(bLac):
                    aPos = lacA.getPos(aLac)[0:3]
                    bPos = lacB.getPos(bLac)[0:3]
                    vecDir = bPos - aPos
                    fdirSum += vecDir
                    fdirCnt += 1
            frameAvgDir[cInd] = fdirSum / fdirCnt

        print "Average Frame:" + str(
            [frameAvgDir[aVal] for aVal in sorted(frameAvgDir.keys())]
        )
        subVec = lambda cInd: frameAvgDir[cInd]  # use the frame average
        tChains = allChains.get()
        nChains = dict(
            filter(lambda a: a[1] > self.minChainLength, tChains.items())
        )  # only keep chains of a certain length
        print (
            "Chains",
            len(tChains),
            "/",
            len(nChains),
            "Mean Length",
            mean(tChains.values()),
            "/",
            mean(nChains.values()),
            "Max",
            max(tChains.values()),
        )
        # Reset Average Vector
        dirSum = array([0.0, 0.0, 0.0])
        dirCnt = 0

        edgeA = None
        matchDict = None
        for (cInd, cLacs) in enumerate(
            zip(lacList[0:-1], lacList[1:])
        ):  # frame number, (bubble, next bubble)
            (lacA, lacB) = cLacs
            try:
                frameCount = trackingFuncs["getTimeStep"](lacB.fname) - trackingFuncs[
                    "getTimeStep"
                ](lacA.fname)
            except:
                print "Auto-frame count detection failed, default is : 1"
                frameCount = 1
            print ("Tracking", lacA, lacB, "Cnt-", frameCount)

            matchDictPA = matchDict
            if self.useMatchFile:
                cPath = "/".join(lacA.fname.split("/")[:-2] + ["bubcor", ""])
                numA = int(lacA.fname.split("/")[-2].split("_")[1])
                numB = int(lacB.fname.split("/")[-2].split("_")[1])
                print (cPath, numA, numB)
                cFile = cPath + "nfill12_%02d_%02d.txt" % (numA, numB)
                matchDict = lacA.fmatch(open(cFile), lacB)
            else:
                prescreenFunInChains = lambda x, pos: nChains.has_key(
                    allChains.getChain((cInd, x))
                )
                cShift = lambda pos: frameCount * numpy.array(
                    self.trackingFuncs["seedShift"](lacA, lacB)(pos)
                )
                matchDict = lacA.match(
                    lacB,
                    prescreenFun=prescreenFunInChains,
                    maxDist=frameCount * self.gMaxDist,
                    seedShift=cShift,
                )
            lastKeys = set([a[0] for a in matchDict.values()])
            # prescreenFun=lambda x,pos: (x in lastKeys)
            print ("Bubbles:", len(lacA.posMat), "Matches", len(matchDict.keys()))
            # Write Edges
            edgeB = self.readedges(lacB)  # read the next edge list
            self.tracktoolbox.retainEdgeFunction = lambda a, b, c: matchDict.has_key(
                a
            ) & matchDict.has_key(
                b
            )  # both bubbles are matched
            edgePA = edgeA
            edgeA = self.readedges(lacA)
            nEdges = edgeA.getTuplePoses()
            for (cEdge, edgePos) in nEdges:
                # do not update cInd, aLac, bLac, mQuql
                edgeSPos = lacA.getPos(cEdge[0])[0:3]
                edgeFPos = lacA.getPos(cEdge[1])[0:3]
                # edgeInfo
                eInfo = 0  # 0 means edge is preserved
                if edgePA is not None:
                    if edgePA.backMap(matchDictPA, cEdge) == 0:
                        eInfo = 1  # if it is created (not backmappable) make it 1
                if eInfo == 0:
                    if not edgeB.forwardMap(matchDict, cEdge):
                        eInfo = -1  # if it is deleted make it -1

                edgeAvPos = 0.0 * numpy.array(edgeFPos) + 1.0 * numpy.array(edgeSPos)
                erPos = transPosRad(edgeAvPos)
                edgeVec = numpy.array(edgeFPos) - numpy.array(edgeSPos)
                outEdges.write(
                    "%i,%i,%i,%i,%f,%i,%f,%f,%f,%f,%f,%f,%f,%f"
                    % (
                        cInd,
                        0,
                        cEdge[0],
                        cEdge[1],
                        0,
                        eInfo,
                        edgeAvPos[0],
                        edgeAvPos[1],
                        edgeAvPos[2],
                        erPos[0],
                        erPos[1],
                        edgeVec[0],
                        edgeVec[1],
                        edgeVec[2],
                    )
                )
                toStr = (
                    ","
                    + ",".join(["%f"] * (len(self.tracktoolbox.edgeVars) - 3))
                    + "\n"
                )
                outEdges.write(toStr % tuple(edgePos[3:]))
            for aLac in sorted(matchDict.keys()):
                (bLac, mQual) = matchDict[aLac]

                if lacA.has_key(aLac) & lacB.has_key(bLac):
                    # Write Raw File
                    aPos = lacA.getPos(aLac)
                    bPos = lacB.getPos(bLac)
                    avgPos = aPos  # always use avgPos
                    rPos = transPosRad(avgPos)
                    vecDir = bPos - aPos
                    nsVec = vecDir[0:3] - subVec(cInd)
                    dirSum += vecDir[0:3]
                    dirCnt += 1
                    curChain = allChains.getChain((cInd, aLac))
                    curChainLength = nChains[curChain]
                    outRaw.write(
                        "%i,%i,%i,%i,%i,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%i"
                        % (
                            cInd,
                            curChain,
                            curChainLength,
                            aLac,
                            bLac,
                            mQual,
                            avgPos[0],
                            avgPos[1],
                            avgPos[2],
                            rPos[0],
                            rPos[1],
                            vecDir[0],
                            vecDir[1],
                            vecDir[2],
                            nsVec[0],
                            nsVec[1],
                            nsVec[2],
                            len(nEdges),
                        )
                    )
                    toStr = "," + ",".join(
                        ["%f"] * (len(self.tracktoolbox.matchVars) - 3)
                    )
                    outRaw.write(toStr % tuple(lacA.getPos(aLac)[3:]))
                    if self.diffMatchVars:
                        outRaw.write(toStr % tuple(vecDir[3:]))
                    outRaw.write("\n")
                    # Do the lattice boxing / discretization
                    avgPos = (aPos + bPos) / 2
                    cDex = numpy.int_(numpy.round((avgPos[0:3] - lmin) / self.lstep))
                    ncDex = max([cDex, [0, 0, 0]], 0)
                    ncDex = min(
                        [
                            ncDex,
                            [self.steps[0] - 1, self.steps[1] - 1, self.steps[2] - 1],
                        ],
                        0,
                    )
                    if not self.keepExtremes:
                        if (
                            (ncDex[0] == cDex[0])
                            and (ncDex[1] == cDex[1])
                            and (ncDex[2] == cDex[2])
                        ):
                            otext[cDex[0]][cDex[1]][cDex[2]].add(
                                vecDir[0:3],
                                self.trackingFuncs["wFun"](mQual),
                                vec2=nsVec,
                            )
                    else:
                        otext[ncDex[0]][ncDex[1]][ncDex[2]].add(
                            vecDir[0:3], self.trackingFuncs["wFun"](mQual), vec2=nsVec
                        )

        # Write the result to the file
        print "Mean Velocity Vector :" + str(dirSum / dirCnt)
        meanSubVec = lambda dvecObj: dvecObj.val() - (
            dirSum / dirCnt
        )  # subtract the mean value
        meanSubVec = lambda dvecObj: dvecObj.val2()  # print the mean subtracted value

        def printVecLine(cveccla, posStr, doWrite=True):
            global allAlign, allAiso, allMean, allSum, allPts, allCov
            ostr = "%i,%f," % (cveccla.count, cveccla.weight) + posStr
            if doWrite:
                ostr += "%f,%f,%f" % tuple(cveccla.val())
                ostr += ",%f,%f,%f" % tuple(meanSubVec(cveccla))
                out.write(ostr + "\n")

        for i in range(self.steps[0]):
            for j in range(self.steps[1]):
                for k in range(self.steps[2]):
                    iPos = lmin + self.lstep * [i + 0.5, j + 0.5, k + 0.5]
                    rPos = transPosRad(iPos)
                    if 1:
                        printVecLine(
                            otext[i][j][k],
                            "%i,%i,%i,%f,%f,%f,%f,%f,"
                            % tuple([i, j, k] + list(iPos) + list(rPos[0:2])),
                        )
                    # except:
                    # 	print str(otext[i][j][k])+' failed'

                print ("Steps", self.steps)
        out.close()
        outnames = open(self.outFileNames, "w")
        for (cInd, cLacs) in enumerate(lacList):
            outnames.write("%i,%s\n" % (cInd, cLacs.fname))
        outnames.close()
        # Continue RUnning


#
if __name__ == "__main__":
    lacFileRoot = sys.argv[1]
    outFile = sys.argv[2]
    cTracker = groupTrack(lacFileRoot, outFile)
    cTracker.run()
