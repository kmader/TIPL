import os
import sys
import numpy
def readHist(filename): # 
	histFile=open(filename)
	txtdata=map(lambda x: x.strip().split('\t'),histFile)
	histFile.close()
	headerRead=False
	colsRead=False
	colHeader=[]
	header=[]
	histMat=[]
	for cLine in txtdata:
		if headerRead:
			# Process Data
			if colsRead:
				#Import Data
				histMat+=[[float(cVal) for cVal in cLine]]
			else:
				colHeader=cLine
				colsRead=True
		else:
			if cLine[0].find('BinSize')>=0: headerRead=True
			header+=[cLine]
	histMat=numpy.array(histMat)
	for (cCol,cTxt) in enumerate(colHeader):
		print str(cCol)+'\t'+cTxt+'\tMin:'+str(min(histMat[:,cCol]))+'\tMax:'+str(max(histMat[:,cCol]))
	return (colHeader,histMat)

def meanStd(histMat,valCol=0,weightCol=1):
	if weightCol>=0:
		wVal=histMat[:,weightCol]
	else:
		wVal=numpy.ones(histMat[:,0].shape)
	meanval=sum(histMat[:,valCol]*wVal)/sum(wVal)
	stdval=numpy.sqrt(sum((histMat[:,valCol]-meanval)**2*histMat[:,weightCol])/sum(histMat[:,weightCol]))
	return (meanval,stdval)
if __name__ == "__main__":
	(a,b)=readHist(sys.argv[1])
	print meanStd(b)		
				
				
