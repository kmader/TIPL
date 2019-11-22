import sys, os
import tracktools as tt
from glob import glob
import numpy
from numpy import array, mean, min, max, sqrt
from math import atan2 as radAtan2

# Parameters
# number of steps to take
steps = (8, 8, 8)
# steps=(10,36,2)
# lstep=array([  42,   42,  42*1.5])
# lstep=array([80,80,80]) # bernd's data
# lstep=array([8000,8000,8000]) # bernd's data
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


def atan2(x, y):
    return 180 / 3.1415 * radAtan2(x, y)


wFun = lambda x: 1 / (1 + x)  # Weighting Function
prescreenFun = lambda x, pos,: True
# wFun=lambda x: x

# edgePathFunction=lambda lacPath: '/'.join(lacPath[:-3]+['dk31m17_edge_csv',lacPath[-2],'']) # for dk31
edgePathFunction = lambda lacPath: "/".join(lacPath[:-1] + [""])  # for dkg21 and mi7

seedShift = lambda lacA, lacB: lambda nPos: [0, 0, 30]  # dk31
# seedShift=lambda lacA,lacB: lambda nPos: [0,0,10] # dkg21
# seedShift=lambda lacA,lacB: lambda nPos: [0,0,0] # lip4 or coarsening foam
# Maximum bubble distance per frame (ignoring
gMaxDist = 90e9
# Maximum bubble volume difference (%, 0.05 = 5%)
tt.maxVolDifference = 200
# gMaxDist=12 # lip4
# seedShift=lambda x: [0,0,20*(abs(x[0]-250)<80)*(abs(x[1]-250)<80)*(abs(x[2]-250)<80)+0*(abs(x[2]-250)<80)] # higher velocity through constriction
# seedShift=lambda lacA,lacB: lambda x: [0,0,30*(abs(x[0]-250)<80)*(abs(x[1]-250)<80)*(abs(x[2]-250)<80)] # higher velocity through constriction
# tt.matchVars+=['SHAPET_'+cAx+cAy for cAx in 'XYZ' for cAy in 'XYZ'] # only if the shape tensor has been made

# which bubbles to keep for tracking (volume, position, etc)
# tt.retainBubbleFunction=lambda bubPosVec: (bubPosVec[3]>150000) # only bubbles with volume (column 3 in matchvars)
# tt.retainBubbleFunction=lambda bubPosVec: (bubPosVec[3]>20000) # only bubbles with volume (column 3 in matchvars)
# retainBubbleFunction=lambda bubPosVec: (bubPosVec[3]>100000) & (bubPosVec[3]<500000) # only bubbles with volume (column 3 in matchvars)
# retainBubbleFunction=lambda bubPosVec: (bubPosVec[4]**3>20*bubPosVec[3]) # keep all bubbles]
# Samples are rotated by 180 we need a pre-processing function for the files
# prescreenFun=lambda x,pos,: sqrt((pos[0]-250)**2+(pos[1]-250)**2+(pos[2]-250)**2)<110

# tt.tmatRead=lambda a: 'hello!' # disable rigid correction (this function will result in an error if it is called)

## built in rotation matrix
import math


def makeRM(th):
    tRM = numpy.eye(3)
    tRM[0, 0] = math.cos(th)
    tRM[0, 1] = -math.sin(th)
    tRM[1, 0] = math.sin(th)
    tRM[1, 1] = math.cos(th)
    return tRM


# Special Function for dkg since the data is rotated so we can fix it again
# tt.groupMatchFun=lambda bubPosVec,bubPosMat: tt.rCylPosDiff(bubPosVec,bubPosMat)+0.5*tt.ZDiff(bubPosVec,bubPosMat)+100*(tt.volCompare(bubPosVec,bubPosMat)>0.5)+10*(tt.nhCompare(bubPosVec,bubPosMat)) # artifical weight if it is greater than 0.5
# prescreenFun=lambda x,pos : sqrt((pos[0]-250)**2+(pos[1]-250)**2+(pos[2]-250)**2)>100

getDataName = lambda fname: fname.split("/")[-2]
getTimeStep = lambda x: int(x.split("/")[-2].split("_")[-2])


centOfRot = [252, 252, 0]
# centOfRot=[0,0,0]

if correctRot:  # images are rotated

    def useRotMat(cFile):
        cNum = int(cFile.split("_")[2])
        print cNum
        print makeRM(numpy.pi)
        if cNum % 2 == 0:
            return makeRM(numpy.pi)
        else:
            return None


else:
    useRotMat = lambda x: None  # disable rotation matrices
if disableRot:
    useRotMat = lambda x: -1
# Shift for dkg31 (10 pixels per step)
# seedShift=lambda lacA,lacB: lambda x: [0,0,10*(abs(x[0]-250)<80)*(abs(x[1]-250)<80)*(abs(x[2]-250)<80)] # higher velocity through constriction

tt.matchVars += ["THICKNESS", "NEIGHBORS"]
# tt.retainBubbleFunction=lambda bubPosVec: (bubPosVec[3]>2000) # only bubbles with volume (column 3 in matchvars)
# prescreenFun=lambda x,pos,: pos[len(tt.matchVars)-1]>0
subVec = lambda cInd: array([0.0] * 3)  # no subtraction
# subVec=lambda cInd: array([0.0,0.0,-20.0]) # constant subtraction


# List track tracks bubbles through a list of files (within directories)
lacFileRoot = sys.argv[1]
print (lacFileRoot, lacFileRoot.find("%"))
if lacFileRoot.find("%") >= 0:
    qPath = "*".join(lacFileRoot.split("%"))
else:
    lacFileRoot = os.path.abspath(lacFileRoot)
    qPath = "/".join(lacFileRoot.split("/")[:-2] + ["*/" + lacFileRoot.split("/")[-1]])

    # qPath='/Users/maderk/Documents/MATLAB/TextureTensor/Foam/ujax/dk31m00/dk31m17_clpor2_csv/*_0*/clpor_2.csv'
    print qPath
tlist = sorted(glob(qPath))
# tlist=filter(lambda x: (int(x.split('/')[-2].split('_')[-2])>=18),tlist)
# Remove Specific Images
BadList = [0, 2, 17]
BadList = [0, 8, 15, 18, 27, 28, 31, 32, 35]
# tlist=filter(lambda x: (int(x.split('/')[-2].split('_')[-2]) not in BadList),tlist)
# Keep only a range
checkFilenumber = lambda x: (x >= 10) & (x < 37)
# tlist=filter(lambda x: checkFilenumber(int(x.split('/')[-2].split('_')[-2])),tlist)
print tlist
outFile = sys.argv[2]
outFileRaw = outFile + "_raw.txt"
outFileEdges = outFile + "_edges.txt"
outFileNames = outFile + "_names.txt"
lacList = []
print (correctRot, useRotMat("test"))
for curLacFile in tlist:
    lacList += [
        tt.lacunData(curLacFile, rotMat=useRotMat(curLacFile), centRot=centOfRot)
    ]
    try:
        print "A"
        # lacList+=[tt.lacunData(curLacFile,rotMat=useRotMat(curLacFile),centRot=centOfRot)]
    except:
        print ("Error", curLacFile)
lacList = filter(lambda x: len(x.lkeys) > 1, lacList)
out = open(outFile, "w")
outRaw = open(outFileRaw, "w")
outEdges = open(outFileEdges, "w")
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
inLine += "," + ",".join(tt.matchVars[3:])  # add standard columns
if diffMatchVars:
    inLine += ",D_" + ",D_".join(tt.matchVars[3:])  # add standard columns
inLine += "\n"
outRaw.write(inLine)  # First two lines are garbage
inLine = "//Count,Match_Quality,I,J,K,POS_X,POS_Y,POS_Z,POS_R,POS_THETA,DIR_X,DIR_Y,DIR_Z,NDIR_X,NDIR_Y,NDIR_Z\n"
out.write(inLine)  # First two lines are garbage
inLine = "//Frame,Chain,Component 1,Component 2,Match_Quality,EDGE_INFO,POS_X,POS_Y,POS_Z,POS_R,POS_THETA,EDGE_X,EDGE_Y,EDGE_Z"
inLine += "," + ",".join(tt.edgeVars[3:])  # add standard columns
inLine += "\n"
outEdges.write(inLine)  # First two lines are garbage
# Calculate Values on a Grid
print ("lmin", lmin, "lmax", lmax)
if useLstep:
    steps = tuple(numpy.int_(numpy.round((lmax - lmin) / lstep)))
else:
    lstep = (array(lmax) - array(lmin)) / steps

if isoSteps:
    lstep = array([mean(lstep)] * 3)
    steps = tuple(numpy.int_(numpy.round((lmax - lmin) / lstep)))
if trimEdge >= 1:
    lmin += lstep * (0.5 * trimEdge)
    lmax -= lstep * (0.5 * trimEdge)
    steps = (steps[0] - trimEdge, steps[1] - trimEdge, steps[2] - trimEdge)
print (lmin, lmax, lstep, steps)
# Init Radial Coordinates
radCent = tuple(
    array(lacList[0].lmin) + (array(lacList[0].lmax) - array(lacList[0].lmin)) / 2
)  # immutable


def transPosRad(cPos):
    nx = cPos[0] - radCent[0]
    ny = cPos[1] - radCent[1]
    return (sqrt(ny ** 2 + nx ** 2), atan2(ny, nx), cPos[2])


getPosRad = lambda x: transPosRad(getPosXYZ(x))
# Initialize Array
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


otext = {}
for i in range(steps[0]):
    otext[i] = {}
    for j in range(steps[1]):
        otext[i][j] = {}
        for k in range(steps[2]):
            otext[i][j][k] = dispVec()

# Prescreen for long chains
allChains = tt.chain()

frameAvgDir = {}
for (cInd, cLacs) in enumerate(zip(lacList, lacList[1:] + [lacList[-1]])):
    (lacA, lacB) = cLacs
    try:
        frameCount = getTimeStep(lacB.fname) - getTimeStep(lacA.fname)
    except:
        print "Auto-frame count detection failed, default is : 1"
        frameCount = 1
    cShift = lambda pos: frameCount * numpy.array(seedShift(lacA, lacB)(pos))
    matchDict = lacA.match(
        lacB,
        prescreenFun=prescreenFun,
        maxDist=frameCount * gMaxDist,
        bijTrim=isBijective,
        seedShift=cShift,
    )
    scoreMap = map(lambda a: a[1][1], matchDict.items())
    try:
        print (
            "Pretracking",
            getDataName(lacA.fname),
            getDataName(lacB.fname),
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

print "Average Frame:" + str([frameAvgDir[aVal] for aVal in sorted(frameAvgDir.keys())])
subVec = lambda cInd: frameAvgDir[cInd]  # use the frame average
tChains = allChains.get()
nChains = dict(
    filter(lambda a: a[1] > minChainLength, tChains.items())
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


def readedges(lacIn):  # read in the edge file
    lacPath = lacIn.fname.split("/")
    cPath = edgePathFunction(lacPath)
    # numA=int(lacPath[-2].split('_')[1])
    # print (cPath,numA,numA)
    cFile = cPath + "clpor_edge_ptxt.csv"
    return tt.edgeData(cFile)


edgeA = None
matchDict = None
for (cInd, cLacs) in enumerate(
    zip(lacList[0:-1], lacList[1:])
):  # frame number, (bubble, next bubble)
    (lacA, lacB) = cLacs
    try:
        frameCount = getTimeStep(lacB.fname) - getTimeStep(lacA.fname)
    except:
        print "Auto-frame count detection failed, default is : 1"
        frameCount = 1
    print ("Tracking", lacA, lacB, "Cnt-", frameCount)

    matchDictPA = matchDict
    if useMatchFile:
        cPath = "/".join(lacA.fname.split("/")[:-2] + ["bubcor", ""])
        numA = int(lacA.fname.split("/")[-2].split("_")[1])
        numB = int(lacB.fname.split("/")[-2].split("_")[1])
        print (cPath, numA, numB)
        cFile = cPath + "nfill12_%02d_%02d.txt" % (numA, numB)
        matchDict = lacA.fmatch(open(cFile), lacB)
    else:
        prescreenFun = lambda x, pos: nChains.has_key(allChains.getChain((cInd, x)))
        cShift = lambda pos: frameCount * numpy.array(seedShift(lacA, lacB)(pos))
        matchDict = lacA.match(
            lacB,
            prescreenFun=prescreenFun,
            maxDist=frameCount * gMaxDist,
            seedShift=cShift,
        )
    lastKeys = set([a[0] for a in matchDict.values()])
    # prescreenFun=lambda x,pos: (x in lastKeys)
    print ("Bubbles:", len(lacA.posMat), "Matches", len(matchDict.keys()))
    # Write Edges
    edgeB = readedges(lacB)  # read the next edge list
    tt.retainEdgeFunction = lambda a, b, c: matchDict.has_key(a) & matchDict.has_key(
        b
    )  # both bubbles are matched
    edgePA = edgeA
    edgeA = readedges(lacA)
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
        toStr = "," + ",".join(["%f"] * (len(tt.edgeVars) - 3)) + "\n"
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
            toStr = "," + ",".join(["%f"] * (len(tt.matchVars) - 3))
            outRaw.write(toStr % tuple(lacA.getPos(aLac)[3:]))
            if diffMatchVars:
                outRaw.write(toStr % tuple(vecDir[3:]))
            outRaw.write("\n")
            # Do the lattice boxing / discretization
            avgPos = (aPos + bPos) / 2
            cDex = numpy.int_(numpy.round((avgPos[0:3] - lmin) / lstep))
            ncDex = max([cDex, [0, 0, 0]], 0)
            ncDex = min([ncDex, [steps[0] - 1, steps[1] - 1, steps[2] - 1]], 0)
            if not keepExtremes:
                if (
                    (ncDex[0] == cDex[0])
                    and (ncDex[1] == cDex[1])
                    and (ncDex[2] == cDex[2])
                ):
                    otext[cDex[0]][cDex[1]][cDex[2]].add(
                        vecDir[0:3], wFun(mQual), vec2=nsVec
                    )
            else:
                otext[ncDex[0]][ncDex[1]][ncDex[2]].add(
                    vecDir[0:3], wFun(mQual), vec2=nsVec
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


for i in range(steps[0]):
    for j in range(steps[1]):
        for k in range(steps[2]):
            iPos = lmin + lstep * [i + 0.5, j + 0.5, k + 0.5]
            rPos = transPosRad(iPos)
            if 1:
                printVecLine(
                    otext[i][j][k],
                    "%i,%i,%i,%f,%f,%f,%f,%f,"
                    % tuple([i, j, k] + list(iPos) + list(rPos[0:2])),
                )
            # except:
            # 	print str(otext[i][j][k])+' failed'

        print ("Steps", steps)
out.close()
outnames = open(outFileNames, "w")
for (cInd, cLacs) in enumerate(lacList):
    outnames.write("%i,%s\n" % (cInd, cLacs.fname))
outnames.close()
