import os,sys
import UFOAM # sometimes needed to get libraries recognized on some machines
import ch.psi.tomcat.tipl.VirtualAim as VA # image IO
import ch.psi.tomcat.tipl.XDF as XDF # radial distribution function
import ch.psi.tomcat.tipl.Resize as Resize # Resize
import ch.psi.tomcat.tipl.VFilterScale as VFilterScale # Resize
import ch.psi.tomcat.tipl.Morpho as Morpho # morphological operations
import ch.psi.tomcat.tipl.ComponentLabel as CL # component labeling
import ch.psi.tomcat.tipl.EasyContour as EasyContour # component labeling
import ch.psi.tomcat.tipl.Peel as Peel # peeling
import ch.psi.tomcat.tipl.GrayAnalysis as GrayAnalysis # peeling
import ch.psi.tomcat.tipl.D3int as D3int # 3d points
import ch.psi.tomcat.tipl.D3float as D3float # 3d float points
from jarray import array,zeros # matrix library
#VA.scratchLoading=True

imgFile='ufilt.tif'
voxSize=D3float(0.0066,0.0066,0.0066)
# Prereqs for multiphase processing
rodSelect=lambda x: (x>24000) & (x<30070)
maskSelect=lambda x: True
InnerCoatSelect=lambda x: (x>30840) & (x<50372)
OuterCoatSelect=lambda x: (x>51400)
doCrop=False

def cleanMasks(threshImg,closeIter=2,openIter=2): # denoise images
	cMorph=Morpho(threshImg)
	cMorph.erode(D3int(2,2,0),0.2) # remove single voxels in 2d slices
	cMorph.closeMany(closeIter,1,1.0) # simple close operation
	cMorph.erode(2,0.08) # remove small groups of voxels
	boolImg=cMorph.ExportAim(threshImg)
	cMorph=None
	if openIter<1: return boolImg
	boolMat=boolImg.getBoolAim()
	for (i,v) in enumerate(boolMat): boolMat[i]=not v # invert image
	boolImg=boolImg.inheritedAim(boolMat)
	boolImg.appendProcLog("CMD:Invert")
	cMorph=Morpho(boolImg)
	cMorph.openMany(openIter,1,1.0) # simple open operation
	boolImg=cMorph.ExportAim(boolImg)
	cMorph=None
	boolMat=boolImg.getBoolAim()
	for (i,v) in enumerate(boolMat): boolMat[i]=not v # invert image
	boolImg=boolImg.inheritedAim(boolMat)
	boolImg.appendProcLog("CMD:Invert")
	return boolImg


def peelAim(inImg,maskImg,iters=2): # Remove outer layer of image
	boundbox=Resize(inImg) # first match image size to mask
	boundbox.cutROI(maskImg)
	boundbox.run()
	cImg=boundbox.ExportAim(inImg)
	cPeel=Peel(cImg,maskImg,D3int(iters)) # actual peeling command
	cPeel.run()
	return cPeel.ExportAim(cImg)

wholeData=VA(imgFile) # load image
wholeData.setElSize(voxSize) # set voxel size
# crop image for testing
if doCrop:
	myResize=Resize(wholeData)
	myResize.cutROI(D3int(200,200,100),D3int(350,350,50))
	myResize.run()
	wholeData=myResize.ExportAim(wholeData)

# Threshold Data
img=wholeData.getIntAim() # read in the values as an integer array

rodImg=zeros(len(img),'z') # empty boolean image
maskImg=zeros(len(img),'z') # empty boolean image
icImg=zeros(len(img),'z') # empty boolean image
ocImg=zeros(len(img),'z') # empty boolean image

for (i,v) in enumerate(img): # Apply thresholds to every voxel in the image
	rodImg[i]=rodSelect(v) # apply filter to each voxel (slow)
	maskImg[i]=maskSelect(v) # apply filter to each voxel (slow)
	icImg[i]=InnerCoatSelect(v) # apply filter to each voxel (slow)
	ocImg[i]=OuterCoatSelect(v) # apply filter to each voxel (slow)
	
# Make all of the arrays into image elements	
roda=wholeData.inheritedAim(rodImg) # return array wrapped in an aim
roda.appendProcLog("Select Segment Applied:Rod")
# volume filter
fixrod=CL(roda)
fixrod.run()
newrod=fixrod.ExportMaskAimVoxels(roda,250000)
roda=newrod
roda.WriteAim('rod.tif')

maska=wholeData.inheritedAim(maskImg) # return array wrapped in an aim
maska.appendProcLog("Select Segment Applied:Mask")
maska.WriteAim('mask.tif')

ica=wholeData.inheritedAim(icImg) # return array wrapped in an aim
ica.appendProcLog("Select Segment Applied: Inner Coating")
ica.WriteAim('inner.tif')

oca=wholeData.inheritedAim(ocImg) # return array wrapped in an aim
oca.appendProcLog("Select Segment Applied: Outer Coating")
oca.WriteAim('outer.tif')

wholeData=None


# Make background image (called mask, indicates where the sample is, helpful for identifying holes in the sample)
roda=cleanMasks(roda)
ica=cleanMasks(ica)
oca=cleanMasks(oca)

imgr=roda.getBoolAim()
imgi=ica.getBoolAim()
imgo=oca.getBoolAim()
combImg=zeros(len(img),'i') # empty int img
for (i,v) in enumerate(imgr):
	if v: combImg[i]=10
	elif imgi[i]: combImg[i]=20
	elif imgo[i]: combImg[i]=30

comb=roda.inheritedAim(combImg) # return array wrapped in an aim
comb.appendProcLog("Combined Filtered Segments Applied")
comb.WriteAim('combo.tif')	


