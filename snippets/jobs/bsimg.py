import ch.psi.tomcat.tipl.VirtualAim as VA
import ch.psi.tomcat.tipl.FNImage as FNI
import ch.psi.tomcat.tipl.GrayAnalysis as GA
from jarray import array,zeros  
bs=VA('bubbleseeds.tif')
dm=VA('../distmap.tif')
# Combine the bubble seeds with the distance map
class MaskValueFunction(FNI.VoxelFunctionN):
	def __init__(self):
		self.keep=0
		self.val=0
		self.cnt=0
	def name(self):
		return "BubbleSeed-Add"
	def add(self,ipos,v):
		if self.cnt==0: self.keep=v # should the voxel be kept (mask)
		if self.cnt==1: self.val=v
		self.cnt+=1
	def get(self):
		if self.keep>0: return self.val
		return 0
	def getRange(self):
		return array([0,32768],'d')
class MVImage(FNI.VFNGenerator):
	def get(self):
		return MaskValueFunction()
IVA=array([bs,dm],VA)
FNOut=FNI(IVA,2,MVImage())
GA.StartHistogram(FNOut,'bsdist.csv')	      		 
