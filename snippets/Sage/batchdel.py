import sys,os
from numpy import *
from subprocess import *
from glob import glob
doResume=1
showisq=1
showlen=0
rdelete=1
fixmasks=1
vmsFix=lambda wholeFile: '\\;'.join(wholeFile.split(';'))
megsize=lambda fileName: os.path.getsize(fileName)/1e6	
if len(sys.argv)<2:
	for rt,drs,files in os.walk(os.getcwd(),topdown=False):
		ffiles=filter(lambda x: x.find('.csv')>=0,files)
		
		for cFile in ffiles:
			#
			if cFile.lower().find('lacun')>=0: cPre='lacun'
			if cFile.lower().find('canal')>=0: cPre='canal'
			if cFile.lower().find('edge')<0:
				wholeFile=(rt+'/'+cFile)
				try:
					curDir='/'.join((rt+'/'+cFile).split('/')[:-2])
					curSample='_'.join((rt+'/'+cFile).split('/')[-3].split('_')[1:])
					os.chdir(curDir)
				
					if showisq: os.system('ls -lh '+vmsFix(wholeFile))
					if megsize(wholeFile)<0.1:
						csvfiles=glob(rt+'/'+cPre+'_*.csv')
						if showlen: 
							for acsvFile in csvfiles: os.system('wc -l '+acsvFile)
						for ccsv in csvfiles:
							execCmd='rm '+ccsv
							print (ccsv,megsize(ccsv))
							if rdelete: os.system(execCmd)
				except:
					print rt+'/'+cFile+' already gone'
