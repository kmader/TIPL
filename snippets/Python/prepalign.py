import os,sys
from numpy import array,max,min,mean,single,sqrt
from math import atan2 as radAtan2
import gridavg as ga
import numpy
import numpy.linalg

if __name__ == "__main__":
	# number of steps to take
	steps=(8,8,40)
	#steps=(10,36,2)
	lstep=array([  42,   42,  42*1.5])
	#lstep=array([80,80,80]) # bernd's data
	#lstep=array([8000,8000,8000]) # bernd's data
	# should the cube have an isotropic shape
	isoSteps=False
	useLstep=False
	useBubbles=False
	useRadPos=False
	doStrain=numpy.log10(540) # zero is off, -1 is average, positive is U0=doStrain*eye(3)
	doStrain=numpy.log10(2.25e2)
	doStrain=-1
	#doStrain=numpy.array([[numpy.log10(370),0,0],[0,numpy.log10(340),0],[0,0,numpy.log10(125)]])
	# minimum number of objects (edges, lacunae, ...) to be averaged in each box
	minObj=2
	# the name of the direction vectors (edges are dir_..., alignment would be pca1_...)
	#dirV=['DIR_X','DIR_Y','DIR_Z']
	dirV=['PCA1_X','PCA1_Y','PCA1_Z']
	
	#dirV=['PCA2_X','PCA2_Y','PCA2_Z']
	dirV=['PCA3_X','PCA3_Y','PCA3_Z']
	ptxtFile=os.path.abspath(sys.argv[1])
	if len(sys.argv)>2: outFile=os.path.abspath(sys.argv[2])
	else: outFile=os.path.abspath('texture.csv')
	from glob import glob
	# To open all prepared edge files in the current path
	#edges=mergefiles(glob('/'.join(ptxtFile.split('/')[:-1]+['*ptxt.csv'])))
	# To open all prepared edge files in the subdirectories of the current path
	
	#elist=glob('/'.join(ptxtFile.split('/')[:-1]+['*/*ptxt.csv']))
	elist=[]
	if ptxtFile.find('%')>=0: 
		elist=glob('*'.join(ptxtFile.split('%')))
		ptxtFile=elist[0]
	
	print elist
	if len(elist)>0: edges=ga.mergefiles(elist)
	else: edges=None
	if len(sys.argv)>3:
		sruns=int(sys.argv[2])
		fruns=int(sys.argv[3])
		resMat=[]
		for i in range(sruns,fruns,3):
			steps=(i,i,i)
			steps=(0,i,0)
			resMat+=[ga.runTexture(ptxtFile,steps,lstep,dirV,isoSteps,useLstep,useBubbles,useRadPos,doStrain,trimEdge=1,minObj=minObj)]
		print 'Final Results'
		for (cVal,cLine) in zip(range(sruns,fruns),resMat): print str(cVal)+':'+str(cLine)
	else:
		resMat=ga.runTexture(ptxtFile,outFile,steps,lstep,dirV,isoSteps,useLstep,useBubbles,useRadPos,doStrain,trimEdge=0,edges=edges)
		print resMat
		
