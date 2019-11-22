import os, sys, inspect  # standard python libraries

# All the TIPL libraries we might need
import tipl.formats.VirtualAim as VA  # image IO
import tipl.tools.XDF as XDF  # radial distribution function
import tipl.tools.Resize as Resize  # Resize
import tipl.tools.VFilterScale as VFilterScale  # Resize
import tipl.tools.Morpho as Morpho  # morphological operations
import tipl.tools.ComponentLabel as CL  # component labeling
import tipl.tools.EasyContour as EasyContour  # component labeling
import tipl.tools.Peel as Peel  # peeling
import tipl.tools.GrayAnalysis as GrayAnalysis  # shape analysis
import tipl.tools.kVoronoiShrink as KV  # voronoi transform
import tipl.tools.Neighbors as Neighbors  # neighbors
import tipl.util.D3int as D3int  # 3d points
import tipl.util.D3float as D3float  # 3d float points
import tipl.util.ArgumentParser as AP
import tipl.formats.MappedImage as MappedImage
import tipl.util.TImgTools as TIT
import tipl.util.TImgTools.ReadTImg as ReadTImg

import tipl.util.TImgTools.WriteTImg as SaveImage


options = AP(sys.argv[1:])
thickFile = options.getOptionPath("thickmap", "", "Input thickness map")
smallThickMin = options.getOptionDouble(
    "smallradiusmin", 1.0, "Smallest thickness for small objects"
)
smallThickMax = options.getOptionDouble(
    "smallradiusmax", 2.0, "Largest thickness for small objects"
)
mediumThickMin = options.getOptionDouble(
    "mediumthickmin", smallThickMax, "Smallest thickness for medium objects"
)
mediumThickMax = options.getOptionDouble(
    "mediumthickmax", 10.0, "Largest thickness for medium objects"
)
largeThickMin = options.getOptionDouble(
    "largethickmin", mediumThickMax, "Smallest thickness for large objects"
)
largeThickMax = options.getOptionDouble(
    "largethickmax", 20.0, "Largest thickness for large objects"
)
smallFile = options.getOptionPath("small", "small.tif", "Small object output image")
mediumFile = options.getOptionPath("medium", "medium.tif", "Medium object output image")
largeFile = options.getOptionPath("large", "large.tif", "Large object output image")
doCache = options.getOptionBoolean(
    "cache", "Cache mapped values (faster but more memory intensive"
)
doBG = options.getOptionBoolean("bg", "Perform Writes in Background")
doPreload = options.getOptionBoolean("preload", "Preload images")
runAsJob = options.getOptionBoolean("sge:runasjob", "Run Program as a Job")

if runAsJob:
    scriptName = options.getOptionPath(
        "sge:scriptname",
        os.path.abspath(inspect.getfile(inspect.currentframe())),
        "Path to Script File",
    )
    job = SGEJob.runScriptAsJob(scriptName, options, "sge:")

if options.hasOption("?"):
    print options.getHelp()
    exit()

# check for invalid parameters before starting
options.checkForInvalid()
if runAsJob:
    job.submit()
    exit()
if doBG:
    import tipl.util.TImgTools.WriteBackground as SaveImage  # allows background processing
inImg = ReadTImg(thickFile)
if doPreload:
    inImg.getShortAim()  # preload the image


class keepSizedStructures(MappedImage.StationaryVoxelFunction):
    def __init__(self, minVal, maxVal, name):
        self.minVal = minVal
        self.maxVal = maxVal
        self.cname = name

    def name(self):
        return self.cname + ": keep structures size:" + str((self.minVal, self.maxVal))

    def getRange(self):
        return MappedImage.typeRange(10)  # output type is boolean

    def get(self, inval):
        return (inval < self.maxVal) & (inval > self.minVal)


keepSmall = keepSizedStructures(smallThickMin, smallThickMax, "Large")
keepMedium = keepSizedStructures(mediumThickMin, mediumThickMax, "Medium")
keepLarge = keepSizedStructures(largeThickMin, largeThickMax, "Large")

smallImage = MappedImage(inImg, 2, keepSmall)
mediumImage = MappedImage(inImg, 2, keepMedium)
largeImage = MappedImage(inImg, 2, keepLarge)

if doCache:
    smallImage = smallImage.cache(1, 0.0)
    mediumImage = mediumImage.cache(1, 0.0)
    largeImage = largeImage.cache(1, 0.0)

SaveImage(smallImage, smallFile)
SaveImage(mediumImage, mediumFile)
SaveImage(largeImage, largeFile)
