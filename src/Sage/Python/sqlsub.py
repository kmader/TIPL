import sys,os
from numpy import *
from subprocess import *
from glob import glob
from dbImport import *
vmsFix=lambda wholeFile: '\\;'.join(wholeFile.split(';'))
showlen=0
loadlac=1
loadcan=0
loadlacedge=0
loadcanedge=0	
if len(sys.argv)<2:
	for rt,drs,files in os.walk(os.getcwd(),topdown=False):
		ffiles=filter(lambda x: x.find('isq;1')>=0,files)
		
		for cFile in ffiles:
			if True:
				wholeFile=(rt+'/'+cFile)
				curDir='/'.join((rt+'/'+cFile).split('/')[:-2])
				curSample='_'.join((rt+'/'+cFile).split('/')[-3].split('_')[1:])
				os.chdir(curDir)
				#os.system('pwd')
				
				

				lacFile=glob(rt+'/../lacun_3.csv')
				edgeFile=glob(rt+'/../lacun_edge.csv')
				canFile=glob(rt+'/../canal_1.csv')
				
				
				if showlen: 
					for acsvFile in csvfiles: os.system('wc -l '+acsvFile)
				if loadlac: 
					if len(lacFile)>0: InsertCSV(lacFile[0])
					else: print rt+' is missing lacuna csv file!!'
			else:
				print rt+'/'+cFile+' aint valid'
