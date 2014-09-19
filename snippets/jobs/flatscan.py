import ch.psi.tomcat.tipl.VirtualAim as VA
import ch.psi.tomcat.tipl.FNImage as FNI
import ch.psi.tomcat.tipl.GrayAnalysis as GA
import ch.psi.tomcat.tipl.DistLabel as DistLabel
from jarray import array,zeros  
bubs=VA('bubbles.tif')
for DistLabel.FLATCRIT in [0.01,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0,1.1,2,5]:
	dm=VA('distmap.tif')
	DL=DistLabel(dm,bubs)
	bs=DL.ExportBubbleseedsAim(dm)
	bsArr=bs.getBoolAim()
	dmArr=dm.getIntAim()
	for (n,cvals) in enumerate(bsArr): 
		if not cvals: dmArr[n]=0
	GA.StartHistogram(dm.inheritedAim(dmArr),'seedhist_FT%1.2f.csv' % (DistLabel.FLATCRIT))     		 
