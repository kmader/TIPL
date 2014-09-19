import tipl.formats.VirtualAim as VA
import tipl.formats.FNImage as FNI
import tipl.tools.Resize as RS
import tipl.tools.GrayAnalysis as GA
import tipl.tools.kVoronoiShrink as kVorObj
import tipl.tools.cVoronoi as cVorObj
import tipl.tools.Neighbors as NH
from jarray import array,zeros 
blName='bubblelabels.tif'
platName='plat.tif'
outNameLabel='keptlabels.tif'
outNamePlat='keptplat.tif'
removeEdges=True
# Thermal 1 = purple, 2 = green, 3= white, 4=teal, 5 = orange, 6=red

#keepBubbles=[101,197,194,128,11,309] # _36_
#keepBubbles=[128,171,160,127,12,211] # _37_
#keepBubbles=[200,234,151,-1,18,116] # _21_
keepBubbles=[284,240,290,-1,269,382] # _22_
mapBubbles=dict([(j,i+2) for (i,j) in enumerate(keepBubbles)]) # 1 is plateau
blImg=VA(blName)
blArr=blImg.getIntAim()
plArr=VA(platName).getBoolAim()
blKeepArr=zeros(len(blArr),'i') # empty integer image
ptKeepArr=zeros(len(blArr),'z') # empty integer image
for (i,v) in enumerate(blArr): # Apply thresholds to every voxel in the image
	
	if plArr[i]: 
		blKeepArr[i]=1
		ptKeepArr[i]=1
	elif (v in keepBubbles): blKeepArr[i]=mapBubbles[v] # apply check to each voxel (slow)
keepImgLabels=blImg.inheritedAim(blKeepArr) # return array wrapped in an aim
keepImgPlat=blImg.inheritedAim(ptKeepArr) # return array wrapped in an aim

keepImgLabels.appendProcLog("Filtered Bubbles: "+str(keepBubbles)+" and plateau borders as 1")

if removeEdges:
	removeEdges=RS(keepImgLabels)
	removeEdges.find_edges(1) # find edges except for plateau
	removeEdges.run()
	keepImgLabelsBB=removeEdges.ExportAim(keepImgLabels)
	cutToMatch=RS(keepImgPlat)
	cutToMatch.cutROI(keepImgLabelsBB)
	cutToMatch.run()
	keepImgPlatBB=cutToMatch.ExportAim(keepImgPlat)
else:
	keepImgLabelsBB=keepImgLabels
	keepImgPlatBB=keepImgPlat
keepImgLabelsBB.WriteAim(outNameLabel)
keepImgPlatBB.WriteAim(outNamePlat)



"""for (cPre,VorObj) in zip(["cVor","kVor"],[lambda x,y: cVorObj(x,y,True) ,kVorObj]):
	cFaceTestName=cPre+"Mask1.0"
	outCSVA='clpor_'+cFaceTestName+'_1.csv'
	outCSVB='clpor_'+cFaceTestName+'_2.csv'
	bubs=VA('mask.tif')
	labBubs=VA('labels.tif')
	curVorObj=VorObj(labBubs,bubs)
	curVorObj.maxUsuableDistance=1
	#cVorObj.scaleddist=True
	curVorObj.run()
	oBubs=curVorObj.ExportVolumesAim(labBubs)
	oBubs.WriteAim('bubblelabels_'+cFaceTestName+'.tif')
	GA.StartLacunaAnalysis(oBubs,outCSVA,"")
	import tipl.tools.Neighbors as NH
	cNH=NH(oBubs)
	cNH.run()
	oNH=cNH.ExportCountImageAim(oBubs)
	GA.AddRegionColumn(oBubs,oNH,outCSVA,outCSVB,"Neighbors"); # add the faces column to the data
	oNH.WriteAim("nh_"+cFaceTestName+".tif")
	cNH.ExportVoxCountImageAim(oBubs).WriteAim("voxnh_"+cFaceTestName+".tif")
	"""