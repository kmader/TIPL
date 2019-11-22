import os, sys
from paraview.simple import *
from paraview import vtk
from paraview import servermanager as sm

doCenter = True
filename = sys.argv[1]

headername = filename + "-raw.dat"
header = open(headername).readlines()

dim = [int(cEle) for cEle in header[0].split(",")]
imageType = int(header[1])
print " Loading " + filename + " size:" + str(dim)
imgIn = ImageReader()
print imgIn.ListProperties()
if doCenter:
    dx = int(dim[0] / 2)
    dy = int(dim[1] / 2)
    dz = int(dim[2] / 2)
    # imgIn.DataExtent=[1-dx,dim[0]-dx,1-dy,dim[1]-dy,1-dz,dim[2]-dz]
    # print imgIn.DataExtent
    imgIn.DataOrigin = [-dx, -dy, -dz]


imgIn.DataExtent = [1, dim[0], 1, dim[1], 1, dim[2]]

minVal = 0
maxVal = 127
if imageType == 0:
    imgIn.DataScalarType = "unsigned char"
if imageType == 1:
    imgIn.DataScalarType = "short"
    maxVal = 1000
if imageType == 2:
    imgIn.DataScalarType = "int"
    maxVal = 1000
if imageType == 3:
    imgIn.DataScalarType = "float"
    minVal = 0
    maxVal = 1


imgIn.FilePrefix = filename
if filename.upper().find("THICK") >= 0:
    minVal = 0
    maxVal = 65
if len(sys.argv) > 2:
    minVal = float(sys.argv[2])
    maxVal = float(sys.argv[3])


def VolumeColoring(objRep, UseLogScale=False):
    sof = CreatePiecewiseFunction()
    sof.Points = [minVal, 0, 0.5, 0, maxVal, 1, 0.5, 0]

    lut = sm.rendering.PVLookupTable()
    # lut.RGBPoints=[0,1,1,1,127,0,0,0]
    rVal = (maxVal - minVal) / 3
    lut.RGBPoints = (
        [minVal, 0, 0, 0]
        + [minVal + rVal, 0.90, 0, 0]
        + [minVal + 2 * rVal, 0.9, 0.9, 0]
        + [maxVal, 1, 1, 1]
    )
    lut.VectorMode = 0
    lut.ScalarRangeInitialized = 1
    if filename.upper().find("CURV") >= 0:
        sof.Points = [-1, 1, 0.5, 0, 0, 0, 0.5, 0, 1, 1, 0.5, 0]
        rVal = 1 / 3
        lut.RGBPoints = (
            [-1, 0, 1, 1]
            + [-2 * rVal, 0, 0.9, 0.9]
            + [-rVal, 0, 0, 0.9]
            + [0, 0, 0, 0]
            + [rVal, 0.90, 0, 0]
            + [2 * rVal, 0.9, 0.9, 0]
            + [1, 1, 1, 1]
        )
    lut.UseLogScale = UseLogScale
    objRep.ScalarOpacityFunction = sof

    objRep.LookupTable = lut
    objRep.ColorArrayName = imgIn.ScalarArrayName
    objRep.Representation = "Volume"
    ScalarBarWidgetRepresentation1 = CreateScalarBar(
        Title="SAF",
        Enabled=1,
        LabelFontSize=12,
        LabelColor=[0.0, 0.0, 1.0],
        TitleFontSize=12,
        TitleColor=[0.0, 0.0, 1.0],
    )
    ScalarBarWidgetRepresentation1.LookupTable = lut
    return ScalarBarWidgetRepresentation1


# nClip=Clip(imgIn)
# nSlice=nClip.ClipType
# nSlice.Normal=[0,0,1]

doVol = True

if doVol:

    objRep = Show()
    scaleBar = VolumeColoring(objRep)


else:
    newThresh = Threshold(imgIn)
    newThresh.ThresholdRange = [126, 127]

    objRep = Show()
    objRep.DiffuseColor = [0.9, 1, 0.16]  # yellow
    objRep.DiffuseColor = [1, 0.05, 0.05]  # red


# Clip2 = Clip( ClipType="Plane" )

# Clip2.Scalars = ['POINTS', '']
# Clip2.ClipType = "Plane"
# Clip2.ClipType.Normal = [0,-0.77, 0]

# Clip2.ClipType = "Box"
# Clip2.ClipType.Bounds = [0, 45, 0, 45, -45, 45]


# DataRepresentation2 = Show()
# DataRepresentation2.Visibility = 1

# DataRepresentation1 = GetDisplayProperties(imgIn)
# DataRepresentation1.Visibility = 0

# VolumeColoring(DataRepresentation2)
# DataRepresentation2.CubeAxesVisibility = 0
if 0:
    SQ = Superquadric()
    SQ.Toroidal = 0
    SQ.Size = 1
    SQ.Scale = [20, 20, 20]

    DataRepresentation1 = GetDisplayProperties(SQ)
    DataRepresentation1.Opacity = 0.5
    DataRepresentation1.EdgeColor = [0.0, 0.0, 0.5000076295109483]
    DataRepresentation1.SelectionPointFieldDataArrayName = "Normals"
    DataRepresentation1.DiffuseColor = [0.0, 0.0, 0.0]
    DataRepresentation1.AmbientColor = [0.0, 0.0, 0.0]
    DataRepresentation1.BackfaceDiffuseColor = [0.0, 0.0, 0.0]
    DataRepresentation1.CubeAxesColor = [0.0, 0.0, 0.0]
    DataRepresentation1.ScaleFactor = 0.1
    DataRepresentation1.DiffuseColor = [0.1411764705882353, 1.0, 0.0]


RenderView1 = GetRenderView()


RenderView = GetRenderView()  # set camera properties for current view
RenderView.CameraViewUp = [-0.25, 0.82, -0.51]
RenderView.CameraViewUp = [-0.59, 0.08, 0.79]
RenderView.CameraViewUp = [-0.2, -0.2, 0.99]  # (Birds Eye Over Z)
RenderView.CameraViewUp = [0, 0.0, 1]
RenderView.CameraFocalPoint = [0.5, 0.5, 0.5]
RenderView.CameraClippingRange = [2.91, 9.55]
RenderView.CameraPosition = [1.85, 3.79, 4.40]
RenderView.ViewSize = [1024, 1024]

GetRenderView().Representations.append(scaleBar)


Render()
print RenderView.CameraPosition
if filename.upper().find("ROIRDF") >= 0:
    RenderView.CameraPosition = [575, 880, 1100]
    RenderView.CameraPosition = [60.02764585610261, 60.7192258270945, 75.52431025096308]
    Render()
print RenderView.CameraPosition
WriteImage(filename + ".png")
sm.SaveState(filename + ".pvsm")
print (RenderView.CameraPosition)
# RenderView.WriteImage('/Users/maderk/test3.png',"vtkPNGWriter",1)
