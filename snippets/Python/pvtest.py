import os, sys
from paraview.simple import *
from paraview import vtk
from paraview import servermanager as sm

# sm.Connect()
spath = os.path.dirname(sys.argv[0])
opath = os.getcwd()
print "script running from", spath, "current path", opath
if len(sys.argv) > 1:
    sqlText = sys.argv[1]
else:
    sqlText = "SAMPLE_AIM_NUMBER=2"
tscript = ["import os,sys"]
tscript += ['aPath="' + spath + '"']
tscript += ["if aPath not in sys.path: sys.path.append(aPath)"]
tscript += ["import pvtools as pvtools"]
tscript += [
    'pvtools.ImportDB(self,"' + sqlText + '",filterList=[],synList=[],outObj=0)'
]

# aPath='/Users/maderk/Documents/SchoolProjects/PhD/Java/src/Python/'
# cDir='/Users/maderk/Documents/MATLAB/TextureTensor/Foam/ujax/Bernd/'
# cFile=cDir+'ujax_463_edge_text.csv'


# if aPath not in sys.path: sys.path.append(aPath)

# import pvtools as pvtools

# filterList=[]
# filterList+=[('MASK_DISTANCE_MEAN',lambda x: x>-200)]
# filterList+=[('VOLUME',lambda x: x<4000)]
# filterList+=[('NEIGHBORS',lambda x: x>0)]
# synList=[]
# synList+=[('Alignment',lambda x: 180/numpy.pi*pvtools.calcAlignment(x,'STRAIN_X','STRAIN_Y','STRAIN_Z','NEIGHBORS'))]
# synList+=[('Anisotropy',lambda x: pvtools.calcAnisotropy(x,'PCA1_S','PCA2_S','PCA3_S'))]
# cTable=vtk.vtkTable()
# oTable=pvtools.ImportCSV_(cTable,cFile,filterList,synList)

ps = ProgrammableSource()
ps.OutputDataSetType = "vtkTable"
ps.Script = "\n".join(tscript)
ttp = TableToPoints(ps)

# obj=ttp.GetClientSideObject()
# obj.SetInput(oTable)
ttp.XColumn = "POS_X"
ttp.YColumn = "POS_Y"
ttp.ZColumn = "POS_Z"
ttp.UpdatePipeline()

glp = Glyph(ttp)
glp.Vectors = "Sphere"
glp.MaximumNumberofPoints = 100000
glp.Orient = 1
glp.GlyphType = "Sphere"
glp.SetScaleFactor = 0.01
Show()
RenderView = GetRenderView()  # set camera properties for current view
RenderView.CameraViewUp = [-0.25, 0.82, -0.51]
RenderView.CameraFocalPoint = [0.0, 0.5, 0.0]
RenderView.CameraClippingRange = [2.91, 9.55]
RenderView.CameraPosition = [1.85, 3.79, 4.40]

ResetCamera()
Render()
sm.SaveState(opath + "/testImage.pvsm")
WriteImage(opath + "/testImage.png")
# view = CreateRenderView()
# view.ResetCamera()
# view.StillRender()
# view.WriteImage("/Users/maderk/test.png","vtkPNGWriter",1)
