import os, sys
import UFOAM  # sometimes needed to get libraries recognized on some machines
import ch.psi.tomcat.tipl.VirtualAim as VA  # image IO
import ch.psi.tomcat.tipl.XDF as XDF  # radial distribution function
import ch.psi.tomcat.tipl.Resize as Resize  # Resize
import ch.psi.tomcat.tipl.TIPLPlugin as TPlug  # Resize
import ch.psi.tomcat.tipl.VFilterScale as VFilterScale  # Resize
import ch.psi.tomcat.tipl.Morpho as Morpho  # morphological operations
import ch.psi.tomcat.tipl.ComponentLabel as CL  # component labeling
import ch.psi.tomcat.tipl.EasyContour as EasyContour  # component labeling
import ch.psi.tomcat.tipl.Peel as Peel  # peeling
import ch.psi.tomcat.tipl.GrayAnalysis as GrayAnalysis  # make histograms and such
import ch.psi.tomcat.tipl.D3int as D3int  # 3d points

from jarray import array, zeros  # matrix library

# VA.scratchLoading=True
comboImg = VA("combo.tif")
maskImg = VA("mask.tif")
xdfSize = D3int(300, 300, 1)
phase = {}
phase["rod"] = 10
phase["inner"] = 20
phase["outer"] = 30
phase["void"] = 0


def makeTwoPhaseCorrelation(ph1, ph2):
    curXDF = XDF(comboImg, 2, maskImg, xdfSize)
    curXDF.fullScan = True
    curXDF.useSurface = True
    curXDF.inPhase = phase[ph1]
    curXDF.outPhase = phase[ph2]
    for (curXDF.useSurface, curName) in zip([True, False], ["sdf_", "rdf_"])[0:1]:
        curXDF.run(100000)
        curXDF.skipFactor = D3int(4)  # every other voxel
        outAim = XDF.WriteHistograms(curXDF, comboImg, curName + ph1 + "_" + ph2)
        outAim.WriteAim(curName + ph1 + "_" + ph2 + ".tif")


def selectSegment(
    aimImg, filt
):  # select segment applies a threshold (filt as lambda function) to the data
    img = aimImg.getIntAim()  # read in the values as an integer array
    outdata = zeros(len(img), "z")  # empty boolean image
    for (i, v) in enumerate(img):
        outdata[i] = filt(v)  # apply filter to each voxel (slow)
    outAim = aimImg.inheritedAim(outdata)  # return array wrapped in an aim
    outAim.appendProcLog("Select Segment Applied:" + str(filt))
    return outAim


def singlePhase(ph1):
    cImg = selectSegment(wholeData, lambda x: (x == phase[ph1]))
    cImg.WriteAim(ph1 + ".tif")
    GrayAnalysis.StartZProfile(cImg, maskImg, ph1 + "_z.txt", 0.1)
    GrayAnalysis.StartRProfile(cImg, maskImg, ph1 + "_r.txt", 0.1)
    GrayAnalysis.StartRCylProfile(cImg, maskImg, ph1 + "_rcyl.txt", 0.1)


# makeTwoPhaseCorrelation('rod','inner')
# makeTwoPhaseCorrelation('rod','outer')
# makeTwoPhaseCorrelation('rod','void')
makeTwoPhaseCorrelation("rod", "rod")
