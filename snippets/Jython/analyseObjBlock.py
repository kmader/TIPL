"""
The analyze object code makes use of the existing tools and blocks to threshold and analyze an image
"""
# All the java libraries we might need
import tipl.formats.VirtualAim as VA  # image IO
import tipl.tools.ComponentLabel as CL  # component labeling
import tipl.tools.EasyContour as EasyContour  # component labeling
import tipl.tools.GrayAnalysis as GrayAnalysis  # shape analysis
import tipl.tools.kVoronoiShrink as KV  # voronoi transform
import tipl.tools.Neighbors as Neighbors  # neighbors
import tipl.util.D3int as D3int  # 3d points
import tipl.util.D3float as D3float  # 3d float points
import tipl.util.TIPLGlobal as TG
import tipl.util.TImgTools as TIT
import tipl.util.TImgTools.WriteBackground as WriteBackground
import tipl.util.SGEJob as SGEJob
import tipl.formats.MappedImage as MappedImage

# import needed blocks
import tipl.blocks.FilterBlock as FilterB
import tipl.blocks.FoamThresholdBlock as Threshold

import tipl.util.TIPLGlobal as TIPLGlobal  # for changing the number of available cores

# jython libraries
import os, sys, inspect
from jarray import array, zeros  # matrix library


p = TG.activeParser(sys.argv[1:])
p.blockOverwrite()
TIPLGlobal.availableCores = p.getOptionInt(
    "maxcores",
    TIPLGlobal.availableCores,
    "Number of cores/threads to use for processing",
)
VA.scratchLoading = p.getOptionBoolean(
    "local", "Load image data from local filesystems"
)
VA.scratchDirectory = p.getOptionString(
    "localdir", "/home/scratch/", "Directory to save local data to"
)

blocklist = [FilterB("filt:"), Threshold("thresh:")]
# set defaults
p.getOptionD3int(
    "filt:downfactor",
    D3int(1, 1, 1),
    "Default should be 1 since this script isnt about scaling",
)

p = blocklist[0].setParameter(p)
# link blocks
p.getOptionPath(
    "thresh:gfilt", p.getOptionAsString("filt:gfilt"), "Automatically Fed Forward"
)
p.getOptionString("thresh:mask", "mask.tif", "The mask output file should be saved")
p = blocklist[1].setParameter(p)
useInverse = p.getOptionBoolean(
    "useinverse", "Use inverse threshold image instead of threshold"
)
minVoxCount = p.getOptionInt("minvoxcount", 1, "Minimum voxel count")
sphKernelRadius = p.getOptionDouble(
    "sphkernelradius",
    1.74,
    "Radius of spherical kernel to use for component labeling: vertex sharing is sqrt(3)*r, edge sharing is sqrt(2)*r,face sharing is 1*r ",
)
writeShapeTensor = p.getOptionBoolean("shapetensor", "Include Shape Tensor")
phaseName = p.getOptionString("phase", "pores", "Phase name")


startSlice = p.getOptionInt("startslice", -1, "Starting Z Slice")
endSlice = p.getOptionInt("endslice", -2, "Ending Z Slice")
runAsJob = p.getOptionBoolean("sge:runasjob", "Run Program as a Job")
if runAsJob:
    scriptName = p.getOptionPath(
        "sge:scriptname",
        os.path.abspath(inspect.getfile(inspect.currentframe())),
        "Path to Script File",
    )
    job = SGEJob.runScriptAsJob(scriptName, p, "sge:")

if p.hasOption("?"):
    print p.getHelp()
    exit()

# check for invalid parameters before starting
p.checkForInvalid()

if runAsJob:
    job.submit()
    exit()

map(
    lambda x: x.setSliceRange(startSlice, endSlice), blocklist
)  # set the slice range to read
map(lambda x: x.execute(), blocklist)  # execute the block code

if useInverse:
    threshImage = TIT.ReadTImg(p.getOptionAsString("thresh:notthreshold"))
else:
    threshImage = TIT.ReadTImg(p.getOptionAsString("thresh:threshold"))

maskFile = p.getOptionAsString("thresh:mask")
if len(maskFile) < 1:
    maskImage = TIT.WrapTImgRO(MappedImage.FixedImage(threshImage, 10, 1))
else:
    maskImage = TIT.ReadTImg(maskFile)


def analyzePhase(inImg, maskImg, phName):
    # volume filter
    myCL = CL(inImg)
    # Neighborhood definition for component labeling
    # --vertex sharing- is sqrt(3)*r
    # --edge sharing is sqrt(2)*r
    # --face sharing is 1*r
    myCL.useSphKernel(sphKernelRadius)
    myCL.runVoxels(minVoxCount)
    # count only objects with more than 5 voxels
    labImg = myCL.ExportLabelsAim(inImg)
    WriteBackground(labImg, phName + ".tif")
    GrayAnalysis.StartLacunaAnalysis(
        labImg, phName + "_1.csv", "Mask", writeShapeTensor
    )
    # now make the dilation
    vorn = KV(labImg, maskImg)
    vorn.useSphKernel(1.0)
    vorn.run()
    lout = vorn.ExportVolumesAim(labImg)
    WriteBackground(lout, phName + "_dens.tif")
    GrayAnalysis.AddDensityColumn(lout, phName + "_1.csv", phName + "_2.csv", "Density")
    myNH = Neighbors(lout)
    myNH.run()
    myNH.WriteNeighborList(phName + "_edge.csv")
    NHimg = myNH.ExportCountImageAim(lout)
    WriteBackground(NHimg, phName + "_nh.tif")
    GrayAnalysis.AddRegionColumn(
        lout, NHimg, phName + "_2.csv", phName + "_3.csv", "Neighbors"
    )


analyzePhase(threshImage, maskImage, phaseName)
