""" A script which implements the : try to segment the pores using a region growing 
/  k-means clustering - like algorithm where neighboring pores which have similar 
enough (a threshold value) orientations are grouped together. 
That might make the data easier to visualize and ideally the layers we sometimes see will become even clearer"""
import tracktools as tt
import os, sys
import numpy as np

# class
keys2dict = lambda keys, defaultVal=[]: dict(map(lambda x: (x, defaultVal), keys))
fullset = lambda inlist: np.unique(
    map(lambda x: x[0], inlist) + map(lambda x: x[1], inlist)
)  # all elements in a dictionary / list


def run(
    objFile,
    edgeFile,
    objVars=["PCA3_X", "PCA3_Y", "PCA3_Z"],
    distEq=lambda x, y: abs(x[0] * y[0] + x[1] * y[1] + x[2] * y[2]),
    distThresh=5e-2,
):
    """The main function in the script, lacFile and edgeFile are the input data files for the lacuna and the edge list respectively
	lacVars is the list of variables used for the distance metric that are to be saved in that order with the data
	distEq is the equation based on lacVars for computing the distance from lacuna x to lacuna y"""
    objData = tt.lacunData(objFile, rotMat=-1)
    edgeData = tt.edgeData(edgeFile)

    cxnDict = cxnListToDict(edgeData.lkeys)
    # grpLabels is a dictionary for objects where each object has a label and a score
    # the score is not used now but could be used to allow for collapsing groups into the simplist catagories
    keepVars = map(lambda x: objData.lheader[x], objVars)  # translate names to indices
    extVars = lambda inRow: map(lambda y: float(inRow[y]), keepVars)
    grpLabels = dict(
        map(lambda x: (x, [x, extVars(objData.ldata[x]), -1]), objData.lkeys)
    )
    joinList = [1]
    while len(joinList) > 0:
        joinList = connectGroups(grpLabels, cxnDict, distEq, distThresh)
        print("Connecting", len(grpLabels), "with ", len(joinList), " joins")
        grpLabels = processJoin(grpLabels, joinList)
        uniqueGrps = np.unique(map(lambda x: x[0], grpLabels.values()))
        print(len(grpLabels), " in ", len(uniqueGrps), " unique groups")
    return (objData, grpLabels)


def runEdges(
    objFile,
    edgeFile,
    objVars=["PCA3_X", "PCA3_Y", "PCA3_Z"],
    distEq=lambda x, y: abs(x[0] * y[0] + x[1] * y[1] + x[2] * y[2]),
    distThresh=5e-2,
):
    """The main function in the script, lacFile and edgeFile are the input data files for the lacuna and the edge list respectively
	lacVars is the list of variables used for the distance metric that are to be saved in that order with the data
	distEq is the equation based on lacVars for computing the distance from lacuna x to lacuna y"""
    objData = tt.lacunData(objFile, rotMat=-1)
    edgeList = tt.edgeData(edgeFile).lkeys  # never load the whole data

    # grpLabels is a dictionary for objects where each object has a label and a score
    # the score is not used now but could be used to allow for collapsing groups into the simplist catagories
    keepVars = map(lambda x: objData.lheader[x], objVars)  # translate names to indices
    extVars = lambda inRow: map(lambda y: float(inRow[y]), keepVars)
    grpLabels = dict(
        map(lambda x: (x, [x, extVars(objData.ldata[x]), -1]), objData.lkeys)
    )
    joinList = [1]
    while len(joinList) > 0:
        joinList = connectGroupsEdges(grpLabels, edgeList, distEq, distThresh)
        print("Connecting", len(grpLabels), "with ", len(joinList), " joins")
        grpLabels = processJoin(grpLabels, joinList)
        uniqueGrps = np.unique(map(lambda x: x[0], grpLabels.values()))
        print(len(grpLabels), " in ", len(uniqueGrps), " unique groups")
    grpInfo = groupInfo(grpList, objData)
    nGrpLabels = swapGroups(grpLabels, edgeList, grpInfo, distEq, distThresh)
    return (objData, jGrpLabels)


def connectImproveGroups(grpLabels, edgeList, grpMetric, distFunc, distThresh):
    """ iterate over the edges instead of the objects """

    joinList = {}
    minmax = lambda a, b: (min(a, b), max(a, b))
    for (i, j) in edgeList:
        (iGrp, iGrpVec, iScore) = grpLabels[i]
        (jGrp, jGrpVec, jScore) = grpLabels[j]
        if iGrp != jGrp:  # not already in the same group
            gKey = minmax(iGrp, jGrp)  # order correctly
            if not (joinList.has_key(gKey)):  # not already a pending join
                cDist = distFunc(iGrpVec, jGrpVec)
                if cDist < distThresh:
                    joinList[gKey] = 1

    return joinList.keys()


def connectGroupsEdges(grpLabels, edgeList, distFunc, distThresh):
    """ iterate over the edges instead of the objects """
    joinList = {}
    minmax = lambda a, b: (min(a, b), max(a, b))
    for (i, j) in edgeList:
        (iGrp, iGrpVec, iScore) = grpLabels[i]
        (jGrp, jGrpVec, jScore) = grpLabels[j]
        if iGrp != jGrp:  # not already in the same group
            gKey = minmax(iGrp, jGrp)  # order correctly
            if not (joinList.has_key(gKey)):  # not already a pending join
                cDist = distFunc(iGrpVec, jGrpVec)
                if cDist < distThresh:
                    joinList[gKey] = 1

    return joinList.keys()


def swapGroups(grpLabels, edgeList, grpInfo, distFunc, distThresh):
    """ iterate over the edges instead of the objects """
    newGrpLabels = {}
    for (i, j) in edgeList:
        (iGrp, iGrpVec, iScore) = grpLabels[i]
        (jGrp, jGrpVec, jScore) = grpLabels[j]
        if (iGrp != jGrp) & (iGrp > 0) & (jGrp > 0):  # not already in the same group
            newIgrp = iGrp
            newJgrp = jGrp
            iGrpMeanVec = grpInfo[iGrp][2].v0()
            jGrpMeanVec = grpInfo[jGrp][2].v0()
            distItoImean = distFunc(iGrpVec, iGrpMeanVec)
            distItoJmean = distFunc(iGrpVec, jGrpMeanVec)
            distJtoImean = distFunc(jGrpVec, iGrpMeanVec)
            distJtoJmean = distFunc(jGrpVec, jGrpMeanVec)
            if distItoImean > distThresh:
                distItoImean = distThresh
                newIgrp = 0
            if distJtoJmean > distThresh:
                newJgrp = 0
                distJtoJmean = distThresh
            if distItoJmean < distItoImean:  # swap i
                newIgrp = jGrp
                distItoImean = distItoJmean
            if distJtoImean < distJtoJmean:  # swap j
                newJgrp = iGrp
                distJtoJmean = distJtoImean
            iVals = newGrpLabels.get(i, (iGrp, iGrpVec, distThresh * 20))
            if distItoImean < iVals[2]:
                newGrpLabels[i] = (newIgrp, iGrpVec, distItoImean)
            jVals = newGrpLabels.get(j, (jGrp, jGrpVec, distThresh * 20))
            if distJtoJmean < jVals[2]:
                newGrpLabels[j] = (newJgrp, jGrpVec, distJtoJmean)

    return newGrpLabels


def connectGroups(grpLabels, cxnDict, distFunc, distThresh):
    joinList = []
    for (i, cVal) in grpLabels.items():
        (grpId, objVec, score) = cVal
        for cCxn in cxnDict[i]:  # all neighbors
            (otGrpId, otObjVec, otScore) = grpLabels[cCxn]
            if otGrpId != grpId:
                if distFunc(objVec, otObjVec) < distThresh:
                    joinList += [(grpId, otGrpId)]

    return joinList


def dictlistContains(dictDict, cKey):
    for (iKey, iList) in dictDict.items():
        if cKey in iList:
            return iKey
    return None


def joinToMap(joinList):
    """ converts a list of join operations into a mapping from the original values to the new values """
    newLabels = {}
    for (x, y) in joinList:
        xdd = dictlistContains(newLabels, x)
        ydd = dictlistContains(newLabels, y)
        if (xdd is None) & (ydd is None):
            newLabels[min(x, y)] = [x, y]
        elif xdd is not None:
            newLabels[xdd] += [y]
        elif ydd is not None:
            newLabels[ydd] += [x]
    selfMap = {}
    for (jnk, cVals) in newLabels.items():
        cMin = min(cVals)
        for cVal in cVals:
            selfMap[cVal] = cMin
    return selfMap


def processJoin(grpLabels, joinList):
    selfMap = joinToMap(joinList)
    print("Map Down", len(selfMap.keys()), " to ", len(np.unique(selfMap.values())))
    # Applys the mapping to the group if it's not there identify map
    applyMap = lambda x: (x[0], [selfMap.get(x[1][0], x[1][0])] + x[1][1:])
    return dict(map(applyMap, grpLabels.items()))


def cxnListToDict(cxnlist):
    keys = fullset(cxnlist)
    sDict = keys2dict(keys)
    for (id1, id2) in cxnlist:
        sDict[id1] += [id2]
        sDict[id2] += [id1]
    return sDict


class AlignTensor:
    """ Include the logic for reading in the alignment tensor so it can be easily swapped out for other things
	>>> tempTensor=AlignTensor(lacData=None)
	>>> for i in range(100): tempTensor.addpt([0,0,(-1)**i])
	>>> int(tempTensor.align()*100)
	100
	>>> tempTensor=AlignTensor(lacData=None)
	>>> for i in range(100): tempTensor.addpt([0,sqrt(2)/2*(-1)**i,sqrt(2)/2*(-1)**i])
	>>> for i in range(100): tempTensor.addpt([sqrt(2)/2*(-1)**i,0,sqrt(2)/2*(-1)**(i+1)])
	>>> int(tempTensor.align()*100)
	100
	>>> tempTensor=AlignTensor(lacData=None)
	>>> for i in range(100): tempTensor.addpt([0,sqrt(2)/2*(-1)**i,sqrt(2)/2*(-1)**i])
	>>> for i in range(100): tempTensor.addpt([sqrt(2)/2*(-1)**i,0,sqrt(2)/2*(-1)**(i+1)])
	>>> for i in range(100): tempTensor.addpt([0,(-1)**i,0])
	>>> print int(tempTensor.align()*100)
	>>> int(tempTensor.align()*100)
	92
	"""

    import numpy as np

    def __init__(self, lacData, startList=None, objVars=["PCA3_X", "PCA3_Y", "PCA3_Z"]):
        self.vars = objVars
        if lacData is not None:
            self.objCols = map(lambda x: lacData.lheader[x], objVars)
        if startList is None:
            self.list = []
        else:
            self.list = startList
        self._cov = None

    def addpt(self, pt):
        self.list += [pt]
        return self

    def add(self, lacData, row):
        self.addpt(map(lambda cCol: float(lacData.ldata[row][cCol]), self.objCols))
        return self

    def cov(self):
        if self._cov is None:
            tu = np.matrix(self.list)
            self._cov = (np.dot(tu.T, tu.conj()) * 1.0 / tu.shape[0]).squeeze()
            (self.evals, self.evecs) = np.linalg.eigh(self._cov)
        return self._cov

    def v0(self):
        self.cov()
        mDex = list(self.evals).index(max(self.evals))
        return self.evecs[mDex]

    def align(self):
        self.cov()
        return (max(self.evals) - min(self.evals)) / max(self.evals)


def groupInfo(grpList, lacData, objVars=["PCA3_X", "PCA3_Y", "PCA3_Z"]):
    grpInfo = {}
    addObjects = lambda x, a: (x[0] + a[0], x[1] + a[1], x[2].add(lacData, a[2]))
    volCol = lacData.lheader["VOLUME"]
    objCols = map(lambda x: lacData.lheader[x], objVars)
    for (i, grpVals) in grpList.items():
        gV = grpVals[0]
        colMapper = lambda cCol: float(lacData.ldata[i][cCol])
        grpInfo[gV] = addObjects(
            grpInfo.get(gV, (0, 0, AlignTensor(lacData, objVars=objVars))),
            (1, colMapper(volCol), i),
        )  #

    return grpInfo


def writeOutput(outFile, lacData, grpList, colName="Group"):
    fOut = open(outFile, "w")
    fOut.write(lacData.preamble + "\n")
    ordCols = map(
        lambda y: dict(map(lambda x: (x[1], x[0]), lacData.lheader.items()))[y],
        sorted(lacData.lheader.values()),
    )
    grpInfo = groupInfo(grpList, lacData)
    fOut.write(",".join(ordCols + [colName, colName + "_CNT", colName + "_VOL"]) + "\n")
    for (i, grpVals) in grpList.items():
        fOut.write(
            ",".join(
                lacData.ldata[i]
                + [
                    str(grpVals[0]),
                    str(grpInfo[grpVals[0]][0]),
                    str(grpInfo[grpVals[0]][1]),
                ]
            )
            + "\n"
        )
    fOut.close()


if __name__ == "__main__":
    lacFile = sys.argv[1]
    edgeFile = sys.argv[2]
    outFile = lacFile + "_rg.csv"
    (a, b) = runEdges(lacFile, edgeFile, distThresh=0.1)
    writeOutput(outFile, a, b)
