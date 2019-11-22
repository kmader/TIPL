import os, sys
from paraview.simple import *
from paraview import vtk
from paraview import servermanager as sm

imgIn = ImageReader()
imgIn.DataExtent = [0, 431, 0, 431, 0, 431]
imgIn.DataScalarType = "unsigned char"
imgIn.FilePrefix = "/Users/maderk/Documents/MATLAB/TextureTensor/Foam/raufaste/dk31-01/plat432x432x508.raw"

objRep = Show()

doVol = True
if doVol:
    sof = CreatePiecewiseFunction()
    sof.Points = [0, 0, 0.5, 0, 1, 1, 0.5, 0]
    objRep.ScalarOpacityFunction = sof
    lut = sm.rendering.PVLookupTable()
    lut.RGBPoints = [0, 1, 1, 1, 127, 0, 0, 0]
    lut.VectorMode = 0
    lut.ScalarRangeInitialized = 1
    objRep.LookupTable = lut
    objRep.ColorArrayName = imgIn.ScalarArrayName
    objRep.Representation = "Volume"
else:
    objRep.DiffuseColor = [0.9, 1, 0.16]  # yellow
    objRep.DiffuseColor = [1, 0.05, 0.05]  # red

RenderView = GetRenderView()  # set camera properties for current view
RenderView.CameraViewUp = [-0.25, 0.82, -0.51]
RenderView.CameraViewUp = [-0.59, 0.08, 0.79]
RenderView.CameraViewUp = [-0.2, -0.2, 0.99]
RenderView.CameraFocalPoint = [0.0, 0.5, 0.0]
RenderView.CameraClippingRange = [2.91, 9.55]
RenderView.CameraPosition = [1.85, 3.79, 4.40]
RenderView.ViewSize = [1024, 1024]


Render()
WriteImage("/Users/maderk/tester.png")
