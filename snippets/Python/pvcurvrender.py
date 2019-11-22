import os, sys
from paraview.simple import *
from paraview import vtk
from paraview import servermanager as sm

doCenter = True
gCurv = True
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

threshVal = 100
if imageType == 0:
    imgIn.DataScalarType = "unsigned char"
if imageType == 1:
    imgIn.DataScalarType = "short"

if imageType == 2:
    imgIn.DataScalarType = "int"
if imageType == 3:
    imgIn.DataScalarType = "float"
    threshVal = 1.0
if gCurv:
    minVal = 0
    maxVal = 10
else:
    minVal = -2.5
    maxVal = 2.5


imgIn.FilePrefix = filename

if len(sys.argv) > 2:
    minVal = float(sys.argv[2])
    maxVal = float(sys.argv[3])


Threshold1 = Threshold(imgIn)

Threshold1.Scalars = ["POINTS", "ImageFile"]
Threshold1.ThresholdRange = [threshVal, 255.0]
ExtractSurface1 = ExtractSurface(Threshold1)
ExtractSurface1.NonlinearSubdivisionLevel = 1
Smooth1 = Smooth(ExtractSurface1)
Smooth1.NumberofIterations = 120

Curvature1 = Curvature(Smooth1)
if gCurv:
    Curvature1.CurvatureType = "Gaussian"
else:
    Curvature1.CurvatureType = "Mean"


objRep = Show()


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
    lut.UseLogScale = UseLogScale
    # objRep.ScalarOpacityFunction=sof

    if gCurv:
        objRep.ColorArrayName = "Gauss_Curvature"
        meTitle = "Gauss Curv"
    else:
        meTitle = "Mean Curv"
        objRep.ColorArrayName = "Mean_Curvature"
        rVal = (maxVal - minVal) / 6
        lut.RGBPoints = (
            [minVal, 0, 1, 1]
            + [minVal + rVal, 0, 0.9, 0.9]
            + [minVal + 2 * rVal, 0, 0, 0.9]
            + [minVal + 3 * rVal, 0, 0, 0]
            + [minVal + 4 * rVal, 0.90, 0, 0]
            + [minVal + 5 * rVal, 0.9, 0.9, 0]
            + [maxVal, 1, 1, 1]
        )
    objRep.LookupTable = lut
    objRep.Representation = "Surface"

    ScalarBarWidgetRepresentation1 = CreateScalarBar(
        Title=meTitle,
        Enabled=1,
        LabelFontSize=12,
        LabelColor=[0.0, 0.0, 1.0],
        TitleFontSize=12,
        TitleColor=[0.0, 0.0, 1.0],
    )
    ScalarBarWidgetRepresentation1.LookupTable = lut
    return ScalarBarWidgetRepresentation1


if 0:
    Clip2 = Clip(ClipType="Plane")

    Clip2.Scalars = ["POINTS", ""]
    Clip2.ClipType = "Plane"
    Clip2.ClipType.Normal = [0, -0.77, 0]

    Clip2.ClipType = "Box"
    Clip2.ClipType.Bounds = [0, 45, 0, 45, -45, 45]

    DataRepresentation2 = Show()
    DataRepresentation2.Visibility = 1
    scaleBar = VolumeColoring(DataRepresentation2)
    DataRepresentation2.CubeAxesVisibility = 0
    DataRepresentation1 = GetDisplayProperties(Curvature1)
    DataRepresentation1.Visibility = 0
else:
    scaleBar = VolumeColoring(objRep)

DataRepresentation1 = GetDisplayProperties(imgIn)
DataRepresentation1.Visibility = 0
DataRepresentation1 = GetDisplayProperties(Threshold1)
DataRepresentation1.Visibility = 0
DataRepresentation1 = GetDisplayProperties(ExtractSurface1)
DataRepresentation1.Visibility = 0


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
WriteImage(filename + "_curv.png")
sm.SaveState(filename + "_curv.pvsm")
print (RenderView.CameraPosition)
# RenderView.WriteImage('/Users/maderk/test3.png',"vtkPNGWriter",1)
