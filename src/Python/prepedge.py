import sys
from numpy import array,max,min
class Memorize:
	def __init__ (self, f):
		self.f = f
		self.mem = {}
		self.empty=True
		self.min=None
		self.max=None
	def __call__ (self, *args, **kwargs):
		if (str(args)+'K'+str(kwargs)) in self.mem:
			return self.mem[str(args)+'K'+str(kwargs)]
		else:
			tmp = self.f(*args, **kwargs)
			self.mem[str(args)+'K'+str(kwargs)] = tmp
			if self.empty:
				self.min=array(tmp)
				self.max=array(tmp)
				self.empty=False
			else:
				self.min=min([tmp,self.min],0)
				self.max=max([tmp,self.max],0)
			return tmp

def prepareEdge(edgeFile,lacFile):

	outFile='_ptxt.csv'.join(edgeFile.split('.csv'))
	
	lacs=[(lacVal[:lacVal.find(',')],lacVal) for lacVal in open(lacFile,'r').readlines()]
	ldata=dict([(int(cRow),cData[:-1].split(',')) for (cRow,cData) in lacs[2:] if len(cRow)>0])
	lheader=lacs[1][1].split(',')
	lheader=dict([(b,a) for (a,b) in enumerate([''.join(cline.upper().split('//')).strip() for cline in lheader])])
	
	px=lheader['POS_X']
	py=lheader['POS_Y']
	pz=lheader['POS_Z']
	
	exPos=lambda x: array([float(x[px]),float(x[py]),float(x[pz])])
	getPos=Memorize(lambda nPos: exPos(ldata[nPos]))
	# precache values
	for cKey in ldata.keys(): 
		try:
			ttd=getPos(cKey)
		except:
			print (cKey,ldata[cKey])
	
	edges=open(edgeFile,'r')
	out=open(outFile,'w')
	inLine=edges.readline()[:-1]
	inLine+='; LAC_MIN='+str(tuple(getPos.min))+'; LAC_MAX='+str(tuple(getPos.max))+'\n'
	out.write(inLine)
	inLine=edges.readline()[:-1]
	inLine+=',POS_X,POS_Y,POS_Z,DIR_X,DIR_Y,DIR_Z\n'
	out.write(inLine) # First two lines are garbage
	
	for cLine in edges:
		(aLac,bLac,area)=cLine.split(',')
		try:
			aLac=int(aLac)
			bLac=int(bLac)
			area=int(area)
		except:
			print str((aLac,bLac,area))+' is invalido'
			break
		if (ldata.has_key(aLac) & ldata.has_key(bLac)):
			aPos=getPos(aLac)
			bPos=getPos(bLac)
			avgPos=0.5*(aPos+bPos)
			vecDir=0.5*(aPos-bPos)
			out.write(cLine[:-1]+',%f,%f,%f,%f,%f,%f\n' % (avgPos[0],avgPos[1],avgPos[2],vecDir[0],vecDir[1],vecDir[2]))
	
	out.close()	
	return outFile

if __name__ == "__main__":
	edgeFile=sys.argv[1]
	lacFile=sys.argv[2]
	prepareEdge(edgeFile,lacFile)
		
