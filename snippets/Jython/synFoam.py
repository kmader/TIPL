import os, sys
import UFOAM  # sometimes needed to get libraries recognized on some machines
import tipl.tools.VirtualAim as VA  # image IO
import tipl.tools.XDF as XDF  # radial distribution function
import tipl.tools.Resize as Resize  # Resize
import tipl.tools.VFilterScale as VFilterScale  # Resize
import tipl.tools.Morpho as Morpho  # morphological operations
import tipl.tools.TIPLPlugin as TIPLPlugin  # generic plug-in interface
import tipl.tools.ComponentLabel as CL  # component labeling
import tipl.tools.EasyContour as EasyContour  # component labeling
import tipl.tools.Peel as Peel  # peeling
import tipl.tools.GrayAnalysis as GrayAnalysis  # peeling
import tipl.tools.D3int as D3int  # 3d points
import tipl.tools.D3float as D3float  # 3d float points
from jarray import array, zeros  # matrix library

# Initialize output image size
imageSize = D3int(200, 200, 75)

# A function to generate ellipsoid functions
elipFunc = (
    lambda x0, y0, z0, a, b, c: lambda x, y, z: (
        ((x - x0) / a) ** 2 + ((y - y0) / b) ** 2 + ((z - z0) / c) ** 2
    )
    < 1
)

# Define the function for the bubbles
# Single bubble example (at 25,25,25)
# bubbleFunc=lambda x,y,z: ((x-25)**2+(y-25)**2+(z-25)**2)<10**2
# bubbleFuncList=[bubbleFunc]
# Periodic Bubbles
# Bubble Dimensions
a = 20.0
b = 20.0
c = 20.0
# Bubble Spacing
xSp = 20
ySp = xSp
zSp = ySp
bubbleFuncList = []
bubbleList = open("bubbles_seed.csv", "w")
bubbleList.write("POS_X,POS_Y,POS_Z,PROJ_X,PROJ_Y,PROJ_Z,VOLUME\n")
for z in range(0, imageSize.z, zSp)[1:-1]:
    for y in range(0, imageSize.y, ySp)[1:-1]:
        for x in range(0, imageSize.x, xSp)[1:-1]:
            bubbleFuncList += [elipFunc(x, y, z, a, b, c)]
            bubbleList.write(
                "%f,%f,%f" % (x, y, z)
                + ",%f,%f,%f" % (2 * a, 2 * b, 2 * c)
                + ",%f" % (4 * 3.14 / 3 * a * b * c)
                + "\n"
            )
bubbleList.close()
# Create the bubbles and plateau borders for every voxel in the image from the function
bubbleImg = zeros(
    int(imageSize.prod()), "i"
)  # since it is number of bubbles each voxel belongs too (>0 means it is in a bubble)
platImg = zeros(int(imageSize.prod()), "z")
maskImg = zeros(int(imageSize.prod()), "z")
for z in range(imageSize.z):
    for y in range(imageSize.y):
        off = (z * imageSize.y + y) * imageSize.x
        for x in range(imageSize.x):
            # the value is the number of bubbles each point belongs too
            # calculated by evaluating each of the bubble functions at the current position (x,y,z)
            # and counting the number of bubble functions which are true
            bCount = len(filter(lambda tFunc: tFunc(x, y, z), bubbleFuncList))
            bubbleImg[off] = bCount
            platImg[off] = bCount < 1  # only if the value belongs to no bubbles
            maskImg[off] = True  # All voxels in mask
            off += 1
# Wrap them in VirtualAim classes and them save them as images
platAim = VA(platImg, imageSize, D3int(0, 0, 0), D3int(0, 0, 0), D3float(1, 1, 1))
bubbleAim = VA(bubbleImg, imageSize, D3int(0, 0, 0), D3int(0, 0, 0), D3float(1, 1, 1))
maskAim = VA(maskImg, imageSize, D3int(0, 0, 0), D3int(0, 0, 0), D3float(1, 1, 1))
platAim.WriteAim("plat.tif")
maskAim.WriteAim("mask.tif")
bubbleAim.WriteAim("bubbles.tif")


from time import sleep

sleep(4)  # Make Sure the files are really written
# Run the labeling and Shape Analysis
UFOAM.main(
    ["UFOAM", "-stagelist=3,4,5,8", "-outdir=.", "-thickmap=distmap.tif"]
)  # fake thickness map for now
