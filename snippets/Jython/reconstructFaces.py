 import tipl.formats.VirtualAim as VA
import tipl.formats.FNImage as FNI
import tipl.tools.GrayAnalysis as GA
import tipl.tools.kVoronoiShrink as kVorObj
import tipl.tools.cVoronoi as cVorObj
import tipl.tools.Neighbors as NH
from jarray import array,zeros 

for (cPre,VorObj) in zip(["cVor","kVor"],[lambda x,y: cVorObj(x,y,True) ,kVorObj]):
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