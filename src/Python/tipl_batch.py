#!/usr/bin/python

#-------------------------Import libraries---------------------------------------------------
import sys
import os.path
import string
import commands
import os
import time,datetime,random

import thread
from subprocess import Popen
import subprocess
from glob import glob
from optparse import OptionParser,OptionGroup


def spy(cmd, shell=False,env=os.environ,write=False,cwd=None,hack=False,logName='hack_log.txt'):
    import pty, subprocess
    if hack:
    	os.environ=env
    	os.chdir(cwd)
    	execStr=' '.join(cmd)+'>'+logName
    	print execStr
    	#os.system(execStr)
    	subprocess.check_call(cmd)
    	yield '0'
    else:
		master, slave = pty.openpty()
		p = subprocess.Popen(cmd, shell=shell, stdin=None,env=env, stdout=slave, close_fds=True,cwd=cwd)
		os.close(slave)
		line = ""
		while True:
			try:
				ch = os.read(master, 1)
			except OSError:
				# We get this exception when the spawn process closes all references to the
				# pty descriptor which we passed him to use for stdout
				# (typically when it and its childs exit)
				break
			line += ch
			if write: sys.stdout.write(ch)
			if ch == '\n':
				yield line
				line = ""
		if line:
			yield line
	
		ret = p.wait()
		if ret:
			raise subprocess.CalledProcessError(ret, cmd)
        
# To use scripts and executables on /work/sls/bin
def getFreeMemory():
	ks=spy(['free','-m','-t','-o'])
	header=ks.next()
	freeS=header.find('free')
	junk=ks.next()
	junk=ks.next()
	totmem=int(ks.next()[freeS-2:freeS+4].strip())
	return totmem
	
# TIPL Home
tiplHome=os.path.expandvars('$SLSBASE/sls/bin/TOMCAT/')
tiplHome='/work/sls/bin/TOMCAT/'
# Setup correct environment
tiplJars=[cJar for cJar in glob(tiplHome+'*.jar')]+[tiplHome+'Fiji.app/jars/ij.jar']
print tiplJars
tiplEnv=os.environ
# Setup Java VM
# defaults
javaver='linux'
javaArgs='-Xmx2G'

# 64 bit mode
linuxBitQuery=spy(['uname','-a'])
if linuxBitQuery.next().find('x86_64')>=0: 
	javaver='linux-amd64'
	javaArgs='-Xmx32G'
	
if getFreeMemory()<32000: javaArgs='-Xmx'+str(getFreeMemory())+'M'	

javaRuntime=tiplHome+'Fiji.app/java/'+javaver+'/jdk1.6.0_24/jre/bin/java'

if (not tiplEnv.has_key('CLASSPATH')): tiplEnv['CLASSPATH']=''
tiplEnv['CLASSPATH']=':'.join(tiplEnv['CLASSPATH'].split(':')+tiplJars)
print tiplEnv['CLASSPATH']

pathVariables={}
def runScriptFromFile(option, opt_str, value, parser):
	logFile=parser.rargs[0]
	import shlex
	tFile=open(logFile)
	javaExecStr=tFile.readline()
	tFile.close()
	execCmd=javaRuntime+' '+javaArgs+' '+javaExecStr
	print shlex.split(execCmd)
	bestBasePath='/'.join(logFile.split('/')[:-1])
	tFile=open(logFile,'w+')
	tFile.write(execCmd+'\n Starting with free--:'+str(getFreeMemory())+'\nWorking Directory:'+bestBasePath+'\n')
	tFile.close()    
	for cLine in spy(shlex.split(execCmd),env=tiplEnv,shell=False,cwd=bestBasePath,hack=False,logName=logFile+'.txt'):
		tFile=open(logFile,'a+')
		tFile.write(cLine)
		tFile.close()
	sys.exit(1)
def fetchTiplCommandOptions(option, opt_str, value, parser):
	#print (option, opt_str, value, parser)
	#print parser.rargs
	value=parser.rargs[0]
	
	p=[cLine for cLine in spy([javaRuntime,value,'-?'],env=tiplEnv)]
	def tiplParseArguments(cParser,funcOutput):
		argLine=map(lambda x: x.find('Arguments::')>=0,funcOutput).index(True)
		def _parseOption(cOpt):
			cOpt=cOpt
			argSplit=cOpt[1:].split('=')
			cArg=argSplit[0][1:].strip()
			cBody=('='.join(argSplit[1:])).strip()
			cDef=None
			outType=None
			if cBody.find('Default')>=0:
				cDefault=cBody[cBody.find('Default'):]
				cBody=cBody[0:cBody.find('Default')]+'[default: %default]'
				cDef=cDefault[cDefault.rfind(':')+1:cDefault.rfind(')')]
				cType=cDefault[cDefault.rfind('(')+1:cDefault.rfind(':')]
				if cType=='Bool':
					if cDef=='False': cDef=bool(0)
					else: cDef=bool(1)
					outType='bool'
				if cType=='Int': 
					cDef=int(cDef)
					outType='int'
				if cType=='Double': 
					cDef=float(cDef)
					outType='float'
				if cType=='Path': 
					pathVariables[cArg]=1
					outType='str'
				if cType.find('D3')>=0:
					outType='str'
			return (cArg,cBody,cDef,outType)
		usedLetters={'c':1,'X':1,'S':1,'h':1}
		for cOpt in filter(lambda x: len(x.strip())>0,funcOutput[argLine+2:]):
			#print cOpt[0],usedLetters
			(optName,optHelp,defValue,outType)=_parseOption(cOpt)
			cmdLetter=optName[0]
			while usedLetters.has_key(cmdLetter): cmdLetter=random.choice(string.letters)
			usedLetters[cmdLetter]=1
			if outType=='bool': cParser.add_option('--'+optName,action='store_true',dest=optName,help=optHelp,default=defValue)
			else: cParser.add_option('--'+optName,dest=optName,help=optHelp,default=defValue,type=outType)
	cOGroup = OptionGroup(parser, value,'Specific options for the given command which will be submitted with the program')
	tiplArgs=tiplParseArguments(cOGroup,p)
	parser.add_option_group(cOGroup)
	setattr(parser.values, option.dest, value)



optParse=OptionParser()
optParse.add_option('-X','--execute',action="callback",help='Execute command locally',callback=runScriptFromFile)
optParse.add_option('-S','--submit',action='store_true',dest='submit',help='Submit command to cluster ',default=False)
optParse.add_option('-c','--cmd',dest='cmd',help='Name of TIPL Command to Run [default: %default]',action="callback",default='UFOAM',callback=fetchTiplCommandOptions)
(opt,args)=optParse.parse_args()
#optParse.print_help()
# Find more out about the command

doSubmit=opt.submit
doCmd=opt.cmd








curPath=os.getcwd()
userID=''
bestLogPath='/sls/X02DA/data/e12050/Data20/'
def getGPFSPath(cPath):
	rPath=os.path.abspath(cPath)
	if rPath.lower().find('slsbl')>=0: # coming from an NT mount somewhere
		sPath=rPath[rPath.lower().find('slsbl')+6:]
		bLine=sPath[0:sPath.find('/')]
		tPath=sPath[sPath.find('/')+1:]
		eAccount=tPath[0:tPath.find('/')]
		globals()['userID']=eAccount
		subDir=tPath[tPath.find('/')+1:]
		dataDrive=subDir[:subDir.find('/')]
		if dataDrive=='Data10': rPath='/sls/'+bLine+'/Data10/'+eAccount+'/'+subDir[subDir.find('/')+1:]
		else: rPath='/sls/'+bLine+'/data/'+eAccount+'/'+subDir
	rPathList=rPath.split('/')
	if rPath.find('Data10'): globals()['userID']=rPathList[rPathList.index('Data10')+1]
	else: globals()['userID']=rPathList[rPathList.index('data')+1]
	if rPathList[-1].lower().find('.')>=0: rPathList=rPathList[0:-1]
	globals()['bestLogPath']='/'.join(rPathList)
	print (rPath,globals()['bestLogPath'])
	return rPath	
def checkForPath(cVar,cVal):
	cPath=cVal
	if type(cVal) is type('bob'):
		if pathVariables.has_key(cVar):
			cPath=getGPFSPath(cVal)# fix path to reflect current path (if relative)
		else:
			if cVal.find('/')>=0: cPath=getGPFSPath(cVal)
			if cVal.lower().find('.tif')>=0: cPath=getGPFSPath(cVal)
			if cVal.lower().find('.isq')>=0: cPath=getGPFSPath(cVal)
	return cPath
		

# Generate executable string
cmdVars=vars(opt)
del cmdVars['submit']
del cmdVars['cmd']
javaExecStr=doCmd
for (cVar,cVal) in cmdVars.items():
	if type(cVal)==type(True):
		if cVal: javaExecStr+=' -'+cVar
	else:
		cVal=checkForPath(cVar,cVal)
		javaExecStr+=' -'+cVar+'='+str(cVal)
		
print bestLogPath

logFile=bestLogPath+"/"+doCmd+"."+str(datetime.date.today())+".log"


# Generate Submission Text
if doSubmit:
	javaExecStr=doCmd
	for (cVar,cVal) in cmdVars.items():
		if type(cVal)==type(True):
			if cVal: javaExecStr+=' -'+cVar
		else:
			cVal=checkForPath(cVar,cVal)
			javaExecStr+=' -'+cVar+'='+str(cVal)
	print bestLogPath
	import shlex
	tFile=open(logFile,'w+')
	tFile.write(javaExecStr)
	tFile.close()
	# Start DiCoClient
	command = 'java -jar DiCoClient.jar sEtUsErNaMe ' + userID + '\; simu 1 all\; submitjob /work/sls/bin/tipl_batch.py -X '+logFile+' \;'	
	print command
	# To use x02da-cn-??
	os.chdir("/usr/local/cluster/DiCoClient")
	
	os.system(command)


