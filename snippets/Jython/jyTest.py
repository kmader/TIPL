import os, sys
from jarray import array, zeros
import UFOAM  # for some reason something needs to be imported to get everything correct
from tipl.tools import *
import tipl.tools.VirtualAim as VA
import tipl.tools.Morpho as Morpho
import tipl.tools.kVoronoiShrink as DMap
import tipl.tools.Thickness as TMap
import tipl.tools.GrayAnalysis as GA

print sys.argv
ipath = sys.argv[1]
w = VA(ipath)
mydata = w.getIntAim()
# Threshold
outdata = zeros(len(mydata), "z")
for (i, v) in enumerate(mydata):
    outdata[i] = v > 110
outAim = w.inheritedAim(outdata)
outAim.WriteAim(ipath + "/thresh.raw")

# One morphology operation
t = Morpho(outAim)
t.openMany(2)
outAim = t.ExportAim(outAim)
outAim.WriteAim(ipath + "/clean/")

# Inversion
outdata = zeros(len(mydata), "z")
mydata = outAim.getBoolAim()
for (i, v) in enumerate(mydata):
    outdata[i] = not v
outAim = outAim.inheritedAim(outdata)
outAim.WriteAim(ipath + "/bubbles/")

# Distances
dplug = DMap(outAim, False)
dplug.run()
outAim = dplug.ExportDistanceAim(outAim)
outAim.WriteAim(ipath + "/dist/")

# Thickness
tplug = TMap(outAim)
tplug.run()
outAim = tplug.ExportAim(outAim)
outAim.WriteAim(ipath + "/thick/")
