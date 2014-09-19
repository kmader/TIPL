import sys,os
from numpy import *
from subprocess import *
from glob import glob
doResume=1
mjobs=1
rsubmit=1
showlen=0
showisq=0
fixmasks=1
vmsFix=lambda wholeFile: '\\;'.join(wholeFile.split(';'))	
if len(sys.argv)<2:
	for rt,drs,files in os.walk(os.getcwd(),topdown=False):
		ffiles=filter(lambda x: x.find('isq;1')>=0,files)
		
		for cFile in ffiles:
			try:
				wholeFile=(rt+'/'+cFile)
				curDir='/'.join((rt+'/'+cFile).split('/')[:-2])
				curSample='_'.join((rt+'/'+cFile).split('/')[-3].split('_')[1:])
				os.chdir(curDir)
				#os.system('pwd')
				execCmd='qsub -N UFEM_'+curSample+' ~/jobs/UFEMbatch.sge '+rt+'/'+vmsFix(cFile)
				if doResume: execCmd+=' -resume'
				if fixmasks: execCmd+=' -fixmasks'
				if mjobs: execCmd+=' -multiJobs'
				
				if showisq: os.system('ls -lh '+vmsFix(wholeFile))
				
				csvfiles=glob(rt+'/../lacun_3.csv')+glob(rt+'/../canal_1.csv')
				distfiles=glob(rt+'/../maskdist.tif')+glob(rt+'/../canalvols.tif')+glob(rt+'/../lacunvols.tif')
				if showlen: 
					for acsvFile in csvfiles: os.system('wc -l '+acsvFile)
				if len(distfiles)==3: # only finished data
					print execCmd
					if rsubmit: 
						os.system(execCmd)
			except:
				print rt+'/'+cFile+' aint valid'
