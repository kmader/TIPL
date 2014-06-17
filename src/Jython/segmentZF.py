"""
The analyze object code makes use of the existing blocks to filter and segment the image
The Zebrafish has several phases :
first is yolk and eye and is between 0 - 26 (closing, and remove objects below 1000vx)
second is lining cells between 21-100 (opening to separate cells and other objects)
third is other lining cells between 100-169

lapfilt >25 for cells

whole fish is 0 - 242
"""
# All the java libraries we might need
import tipl.formats.VirtualAim as VA # image IO
import tipl.tools.Neighbors as Neighbors # neighbors
import tipl.util.D3int as D3int # 3d points
import tipl.util.D3float as D3float # 3d float points
import tipl.util.TImgTools as TIT
import tipl.util.TImgTools.WriteBackground as WriteBackground
import tipl.util.SGEJob as SGEJob
import tipl.formats.MappedImage as MappedImage
# import needed blocks
import tipl.blocks.BaseBlockRunner as BaseBlockRunner
import tipl.blocks.FilterBlock as FilterB
import tipl.blocks.ThresholdBlock as StdThreshold
import tipl.blocks.FoamThresholdBlock as FancyThreshold

import tipl.blocks.AnalyzePhase as AnalyzePhase

import tipl.util.TIPLGlobal as TIPLGlobal 
# jython libraries
import os,sys,inspect
from jarray import array,zeros # matrix library

p=TIPLGlobal.activeParser(sys.argv[1:])
p.blockOverwrite()  
bbr=BaseBlockRunner()
bbr.add(FilterB("filt:"))
useFancy=not p.getOptionBoolean("usestandardthreshold",False,"Use standard threshold instead of the threshold with morphological operations")
if useFancy: Threshold=FancyThreshold
else: Threshold=StdThreshold

phaseBlocks=["bodyt","yolk","lining","other"]
maskPhase="bodyt"
# Make the threshold blocks
map(lambda phName: bbr.add(Threshold(phName+":")),phaseBlocks)


# Make the analysis blocks
noMaskPhases=[cItem for cItem in phaseBlocks if cItem!=maskPhase]

map(lambda phName: bbr.add(AnalyzePhase(phName+"_ap:")),noMaskPhases)

# set defaults
p.getOptionD3int("filt:downfactor",D3int(2,2,2),"Scale the data down a bit first")
p.getOptionD3float("filt:elsize",D3float(0.743,0.743,0.743),"The default voxel size of the high resolution measurements")
p.getOptionBoolean("filt:changeelsize",True,"The default element size is certainly wrong")
p.getOptionInt("filt:filter",1,"1 is gaussian and 4 is median") # for second filter 3 is laplace

# for yolk phase
p.getOptionInt("yolk:threshvalue",0,"Minimum value for yolk")
p.getOptionInt("yolk:maxthreshvalue",26,"Maximum value for yolk")
# other settings are fine since yolk is very similar to plateau borders

# for the whole body (mask) phae
p.getOptionInt("bodyt:threshvalue",0,"Minimum value for body")
p.getOptionInt("bodyt:maxthreshvalue",242,"Maximum value for body")
if (useFancy): p.getOptionInt("bodyt:closeiter",3,"Body should be a mask like object close several times")
if (useFancy): p.getOptionBoolean("bodyt:clthresh",True,"Remove small components around body")

# for the lining phase
p.getOptionInt("lining:threshvalue",20,"Minimum value for lining")
p.getOptionInt("lining:maxthreshvalue",100,"Maximum value for lining")
if (useFancy): p.getOptionInt("lining:closeiter",1,"Small objects which shouldnt be connected")
if (useFancy): p.getOptionBoolean("lining:clthresh",True,"Remove small specs")
if (useFancy): p.getOptionInt("lining:clminpct",5,"Remove everything small than 10%")

# for the other phase
p.getOptionInt("other:threshvalue",100,"Minimum value for other lining")
p.getOptionInt("other:maxthreshvalue",180,"Maximum value for other lining")
if (useFancy): p.getOptionInt("other:closeiter",0,"Small objects which shouldnt be connected")
if (useFancy): p.getOptionBoolean("other:clthresh",True,"Remove small specs")
if (useFancy): p.getOptionInt("other:clminpct",10,"Remove everything small than 10%")

# set default output names for threshold block
map(lambda blockName: p.getOptionPath(blockName+":threshold",blockName+"_bw.tif","Output file for "+blockName), phaseBlocks)

gfiltName=p.getOptionPath("filt:gfilt","gfilt.tif","Name for smooth-filtered output file")
# link filter to threshold
linkBlock=lambda blockName: p.getOptionPath(blockName+":gfilt",gfiltName,"Automatically Fed Forward")
map(linkBlock,phaseBlocks)

# link threshold to analysis
linkBlockAP=lambda blockName: p.getOptionPath(blockName+"_ap:segmented",p.getOptionAsString(blockName+":threshold"),"Automatically Fed Forward")
map(linkBlockAP,noMaskPhases)
# fill the mask in
linkBlockMask=lambda blockName: p.getOptionPath(blockName+"_ap:mask",p.getOptionAsString(maskPhase+":threshold"),"Automatically Fed Forward")
map(linkBlockMask,noMaskPhases)

# set the labeled output files
linkBlockLabels=lambda blockName: p.getOptionPath(blockName+"_ap:labeled",blockName+"_lab.tif","Automatically Fed Forward")
map(linkBlockLabels,noMaskPhases)
# set name of the phases
linkBlockLabels=lambda blockName: p.getOptionPath(blockName+"_ap:phase",blockName,"Automatically Fed Forward")
map(linkBlockLabels,noMaskPhases)

# forward the minimum voxel count to all phases
minVoxCount=p.getOptionInt("minvoxcount",20,"Minimum voxel count");
linkBlockMVC=lambda blockName: p.getOptionInt(blockName+"_ap:minvoxcount",minVoxCount,"Automatically Fed Forward")
map(linkBlockMVC,noMaskPhases)


# set the rest of the parameters for each block
p=bbr.setParameter(p)

startSlice=p.getOptionInt("startslice",-1,"Starting Z Slice")
endSlice=p.getOptionInt("endslice",-2,"Ending Z Slice")

runAsJob=p.getOptionBoolean("sge:runasjob","Run Program as a Job")
if runAsJob: 
	scriptName=p.getOptionPath("sge:scriptname",os.path.abspath(inspect.getfile(inspect.currentframe())),"Path to Script File")
	job=SGEJob.runScriptAsJob(scriptName,p,"sge:")
	
if p.hasOption("?"):
	print p.getHelp()
	exit()
	
# check for invalid parameters before starting
p.checkForInvalid()

if runAsJob:
	job.submit()
	exit()

# set the slice range to read (if it is only a sub region
bbr.setSliceRange(startSlice,endSlice) 
bbr.execute()



