from numpy import array, max, min, zeros, sum, sqrt, argmin, matrix
import numpy

## Here are the important defintions for matching
## Variables needed
matchVars = [
    "POS_X",
    "POS_Y",
    "POS_Z",
    "VOLUME",
    "MASK_DISTANCE_MEAN",
    "PROJ_X",
    "PROJ_Y",
    "PROJ_Z",
    "NEIGHBORS",
    "PCA1_S",
    "PCA2_S",
    "PCA3_S",
]
matchVars = [
    "POS_X",
    "POS_Y",
    "POS_Z",
    "VOLUME",
    "MASK_DISTANCE_MEAN",
    "PROJ_X",
    "PROJ_Y",
    "PROJ_Z",
    "PROJ_Z",
    "PCA1_S",
    "PCA2_S",
    "PCA3_S",
]
edgeVars = ["POS_X", "POS_Y", "POS_Z", "DIR_X", "DIR_Y", "DIR_Z"]
## Which bubbles and edges should be kept
retainBubbleFunction = lambda bubPosVec: True  # keep all bubbles
retainEdgeFunction = lambda eLac1, eLac2, edgePosVec: True
## General Methods
matchFun = lambda bubPosVecA, bubPosVecB: sqrt(
    sum([(bubPosVecB[i] - bubPosVecA[i]) ** 2 for i in (0, 1, 2)])
)  # simple euclidean distance funcion
# groupMatchFun=lambda bubPosVec,bubPosMat: [matchFun(bubPosVec,cRow) for cRow in bubPosMat]
## Faster but less flexible vector based approach
volCompare = lambda bubPosVec, bubPosMat: (
    abs((bubPosMat[:, 3] - bubPosVec[3]) / (bubPosMat[:, 3] + bubPosVec[3]))
)  # compare volume between bubbles
maxVolDifference = 0.5
volDifferencePenalty = 0
groupMatchFun = lambda bubPosVec, bubPosMat: sqrt(
    sum((bubPosMat[:, 0:3] - bubPosVec[0:3]) * (bubPosMat[:, 0:3] - bubPosVec[0:3]), 1)
) + (
    volDifferencePenalty * (volCompare(bubPosVec, bubPosMat) > maxVolDifference)
    if volDifferencePenalty > 0
    else 0
)

nhCompare = lambda bubPosVec, bubPosMat: abs(bubPosMat[:, 8] - bubPosVec[8])
tRXPos = 250
tRYPos = 250
rCylPosDiff = lambda bubPosVec, bubPosMat: abs(
    sqrt(
        (bubPosMat[:, 0] - tRXPos) * (bubPosMat[:, 0] - tRXPos)
        + (bubPosMat[:, 1] - tRYPos) * (bubPosMat[:, 1] - tRYPos)
    )
    - sqrt(
        (bubPosVec[0] - tRXPos) * (bubPosVec[0] - tRXPos)
        + (bubPosVec[1] - tRYPos) * (bubPosVec[1] - tRYPos)
    )
)
ZDiff = lambda bubPosVec, bubPosMat: abs(bubPosMat[:, 2] - bubPosVec[2])

groupMatchFun = lambda bubPosVec, bubPosMat: sqrt(
    sum((bubPosMat[:, 0:3] - bubPosVec[0:3]) * (bubPosMat[:, 0:3] - bubPosVec[0:3]), 1)
) + 1000 * (
    volCompare(bubPosVec, bubPosMat) > maxVolDifference
)  # artifical weight if it is greater than 0.5

# groupMatchFun=lambda bubPosVec,bubPosMat: 5000*(volCompare(bubPosVec,bubPosMat)>0.5)

# Old Version -- tmatRead=lambda tmatFile: numpy.reshape([float(arv) for (arv,junk) in zip(open(tmatFile,'r').readlines()[0].split(','),range(16))],(4,4)).transpose(1,0)
# New Version
tmatRead = lambda tmatFile: numpy.array(
    map(
        lambda x: map(lambda y: float(y), x.strip().split(",")),
        open(tmatFile).readlines(),
    )
)
# tmatRead=lambda a: 'hello!'
class chain:
    def __init__(self):
        self.mChain = {}
        self.mLinks = {}
        self.cChain = 0

    def add(self, linA, linB):
        if self.mLinks.has_key(linA):
            self.mLinks[linB] = self.mLinks[linA]
            return
        if self.mLinks.has_key(linB):
            self.mLinks[linA] = self.mLinks[linB]
            return
        self.cChain += 1
        self.mLinks[linA] = self.cChain
        self.mLinks[linB] = self.cChain

    def getChain(self, linkC):
        if self.mLinks.has_key(linkC):
            return self.mLinks[linkC]
        return -1

    def get(self):
        cntChain = dict([(a, 0) for a in self.mLinks.values()])
        for a in self.mLinks.values():
            cntChain[a] += 1
        return cntChain


class Memorize:
    def __init__(self, f):
        self.f = f
        self.mem = {}
        self.empty = True
        self.min = None
        self.max = None

    def __call__(self, *args, **kwargs):
        if (str(args) + "K" + str(kwargs)) in self.mem:
            return self.mem[str(args) + "K" + str(kwargs)]
        else:
            tmp = self.f(*args, **kwargs)
            self.mem[str(args) + "K" + str(kwargs)] = tmp
            if self.empty:
                self.min = array(tmp)
                self.max = array(tmp)
                self.empty = False
            else:
                self.min = min([tmp, self.min], 0)
                self.max = max([tmp, self.max], 0)
            return tmp


class edgeData:
    def __init__(self, inFile):
        lacs = [
            (lacVal.split(",")[0:2], lacVal) for lacVal in open(inFile, "r").readlines()
        ]  # read file and format as lacun#,data
        self.correctPos = False
        self.fname = inFile
        self.preamble = lacs[0][1][:-1]
        self.lraw = [
            ((int(cRow[0]), int(cRow[1])), cData[:-1].split(","))
            for (cRow, cData) in lacs[2:]
            if len(cRow) > 0
        ]
        self.ldata = dict(self.lraw)
        lheader = lacs[1][1].split(",")
        self.lheader = dict(
            [
                (b, a)
                for (a, b) in enumerate(
                    ["".join(cline.upper().split("//")).strip() for cline in lheader]
                )
            ]
        )
        self.matchCols = [self.lheader[cVar] for cVar in edgeVars]
        self.exPos = lambda x: ([float(x[pcol]) for pcol in self.matchCols])
        self.getPosj = lambda nPos: self.exPos(self.ldata[nPos])  # uncached access
        if self.correctPos:
            ptTransform = (
                lambda pt: (self.rotMat * matrix(pt).transpose(1, 0))
                .transpose(1, 0)
                .tolist()[0][0:3]
            )
            self.getPosi = lambda nPos: array(
                ptTransform(self.getPosj(nPos)[0:3]) + self.getPosj(nPos)[3:]
            )
        else:
            self.getPosi = lambda nPos: array(self.getPosj(nPos))
        self.lkeys = sorted(
            filter(
                lambda x: retainEdgeFunction(x[0], x[1], self.getPosi(x)),
                self.ldata.keys(),
            )
        )  # only keep valid bubbles
        print (
            "Edges Filtered, Remaining",
            len(self.lkeys),
            "/",
            len(self.ldata.keys()),
            retainBubbleFunction,
            "Correction:",
            self.correctPos,
        )
        self.getPos = Memorize(lambda nPos: self.getPosi(nPos))  # cache bubble access
        self.posMat = zeros((len(self.lkeys), len(self.matchCols)))
        # precache values
        for (cRow, cKey) in enumerate(self.lkeys):
            # self.posMat[cRow,:]=self.getPos(cKey)
            try:
                self.posMat[cRow, :] = self.getPos(cKey)
            except:
                print (cKey, ldata[cKey])
        self.lmin = self.getPos.min
        self.lmax = self.getPos.max

    def getPoses(self):
        return [self.getPos(x) for x in self.lkeys]

    def getTuplePoses(self):
        return [(x, self.getPos(x)) for x in self.lkeys]

    def forwardMap(self, matchDict, cEdge):
        # lacA -> lacB as matchDict
        # cEdge is edge in edgeA, edgeB.forwardMap answers if it appears in edgeB
        return self.has_key((matchDict[cEdge[0]][0], matchDict[cEdge[1]][0]))

    def backMap(self, matchDict, cEdge):
        # lacA -> lacB as matchDict
        # cEdge is edge in edgeB, edgeA.backMap answers if it appears in edgeA
        # -1 means the bubbles do not even appear in lacA
        backMatch = dict([(b[0], a) for (a, b) in matchDict.items()])
        if len(filter(backMatch.has_key, cEdge)) == 2:
            return self.has_key((backMatch[cEdge[0]], backMatch[cEdge[1]]))
        return -1

    def has_key(self, key):
        return self.ldata.has_key(key) | self.ldata.has_key((key[1], key[0]))

    def get(self, key):
        if self.ldata.has_key(key):
            return self.ldata[key]
        elif self.ldata.has_key((key[1], key[0])):
            return self.ldata[(key[1], key[0])]
        return None


class lacunData:
    def __init__(self, inFile, rotMat=None, inCentRot=array([0, 0, 0])):
        """ rotMat has several options if it is None a file in the same directory as the image is looked for, if it is -1 rotation is disabled
    	if it is a value than it is used for correction """
        lacs = [
            (lacVal[: lacVal.find(",")], lacVal)
            for lacVal in open(inFile, "r").readlines()
        ]  # read file and format as lacun#,data
        self.rotMat = matrix(numpy.eye(3))
        self.centRot = inCentRot
        self.correctPos = False

        if rotMat is -1:
            print "Rotation has been force disabled!"
        elif rotMat is None:
            try:
                corData = tmatRead(inFile + "_tf.dat")
                self.centRot = corData[0, :]
                self.rotMat = matrix(corData[1:, :])
                ptTransform = lambda pt: list(
                    array(
                        (
                            self.rotMat
                            * matrix(list(array(pt) - self.centRot)).transpose(1, 0)
                        )
                        .transpose(1, 0)
                        .tolist()[0][0:3]
                    )
                    + self.centRot
                )
                print (
                    "Around:",
                    self.centRot,
                    "Origin Goes to:",
                    ptTransform([0, 0, 0]),
                )
                self.rotMat[2, 2] = 1  # forbid z-flipping!
                self.correctPos = True
            except:
                print "Could not be loaded"

        else:
            self.rotMat = rotMat
            self.centRot = centRot
            ptTransform = lambda pt: list(
                array(
                    (
                        self.rotMat
                        * matrix(list(array(pt) - self.centRot)).transpose(1, 0)
                    )
                    .transpose(1, 0)
                    .tolist()[0][0:3]
                )
                + self.centRot
            )
            print ("Around:", self.centRot, "Origin Goes to:", ptTransform([0, 0, 0]))
            self.correctPos = True
        self.fname = inFile
        self.preamble = lacs[0][1][:-1]
        self.lraw = [
            (int(cRow), cData[:-1].split(","))
            for (cRow, cData) in lacs[2:]
            if len(cRow) > 0
        ]
        self.ldata = dict(self.lraw)
        lheader = lacs[1][1].split(",")
        self.lheader = dict(
            [
                (b, a)
                for (a, b) in enumerate(
                    ["".join(cline.upper().split("//")).strip() for cline in lheader]
                )
            ]
        )
        self.matchCols = [self.lheader[cVar] for cVar in matchVars]
        self.exPos = lambda x: ([float(x[pcol]) for pcol in self.matchCols])
        self.getPosj = lambda nPos: self.exPos(self.ldata[nPos])  # uncached access
        if self.correctPos:
            ptTransform = lambda pt: list(
                array(
                    (
                        self.rotMat
                        * matrix(list(array(pt) - self.centRot)).transpose(1, 0)
                    )
                    .transpose(1, 0)
                    .tolist()[0][0:3]
                )
                + self.centRot
            )
            self.getPosi = lambda nPos: array(
                ptTransform(self.getPosj(nPos)[0:3]) + self.getPosj(nPos)[3:]
            )
        else:
            self.getPosi = lambda nPos: array(self.getPosj(nPos))
        self.lkeys = sorted(
            filter(lambda x: retainBubbleFunction(self.getPosi(x)), self.ldata.keys())
        )  # only keep valid bubbles
        print (
            "Bubbles Filtered, Remaining",
            len(self.lkeys),
            "/",
            len(self.ldata.keys()),
            retainBubbleFunction,
            "Correction:",
            self.correctPos,
        )
        self.getPos = Memorize(lambda nPos: self.getPosi(nPos))  # cache bubble access
        self.posMat = zeros((len(self.lkeys), len(self.matchCols)))
        # precache values
        for (cRow, cKey) in enumerate(self.lkeys):
            # self.posMat[cRow,:]=self.getPos(cKey)
            try:
                self.posMat[cRow, :] = self.getPos(cKey)
            except:
                print (cKey, ldata[cKey])
        self.lmin = self.getPos.min
        self.lmax = self.getPos.max

    def has_key(self, key):
        return self.ldata.has_key(key)

    def fmatch(self, inFile, toLacun):  # match using a file
        matchDict = {}
        for cLineText in inFile.readlines():
            cMatch = [int(cBub) - 1 for cBub in cLineText.strip().split(" ")]
            if cMatch[0] >= len(self.lraw):
                print ("Error", cMatch[0], " is too big for :", len(self.lraw))
            elif cMatch[1] >= len(toLacun.lraw):
                print ("Error", cMatch[1], " is too big for :", len(toLacun.lraw))
            else:
                matchDict[self.lraw[cMatch[0]][0]] = (toLacun.lraw[cMatch[1]][0], 0.0)
        print matchDict
        return matchDict

    def match(
        self,
        toLacun,
        bijTrim=True,
        maxDist=None,
        prescreenFun=lambda a, b: True,
        seedShift=lambda nPos: [0, 0, 0],
    ):  # bijTrim trims matches which are not bijective, max dist is the maximum distance
        matchDict = {}
        for (cInd, cRow) in enumerate(self.posMat):
            if prescreenFun(self.lkeys[cInd], cRow):
                gRow = cRow
                seedShiftV = seedShift(gRow)
                for ssi in range(len(seedShiftV)):
                    gRow[ssi] += seedShiftV[ssi]
                matchScore = groupMatchFun(gRow, toLacun.posMat)
                # print matchScore
                msMin = min(matchScore)
                matchDict[self.lkeys[cInd]] = (toLacun.lkeys[argmin(matchScore)], msMin)
        if bijTrim:
            mdict2 = toLacun.match(
                self,
                bijTrim=False,
                maxDist=maxDist,
                seedShift=lambda oPos: [-1 * ssd for ssd in seedShift(oPos)],
            )

            def bijMatch(cInd, mInd, fDict):  # cur index, matching index foreign dict
                if fDict.has_key(mInd):
                    return fDict[mInd][0] == cInd
                else:
                    return False

            matchDict = dict(
                [(a, b) for (a, b) in matchDict.items() if bijMatch(a, b[0], mdict2)]
            )
        if maxDist is not None:
            matchDict = dict(filter(lambda a: a[1][1] < maxDist, matchDict.items()))
        return matchDict
