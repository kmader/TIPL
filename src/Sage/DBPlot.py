from pylab import *
import pylab
import numpy
import matplotlib.pyplot as plt
import matplotlib
class pylabGraphics(Graphics):
    def __init__(self,savefigfun,closefun,*args,**kwargs):
        self.savefigfun=savefigfun
	self.closefun=closefun
        self._extra_kwds=kwargs
    def save(self,filename=None, dpi=100, savenow=True,axes_labels=None,xmin=0,xmax=0,ymin=0,ymax=0,**kwargs):
        self.savefigfun(filename,dpi=dpi)
	self.closefun()
def SummaryPlot(cPlotVar,pName='',pXlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,**kwargs):
    if pName=='': pName=ptName(cPlotVar)
    if pXlabel=='': pXlabel=ptName(cPlotVar)
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    results=(cur.execute('select '+cPlotVar+' from Lacuna WHERE Project_Number = "'+str(projectTitle)+'" '+sqlAdd)).fetchall()
    results=[obj[0] for obj in results if type(obj[0]) is type(pi)]
    print kwargs
    pylab.hist(numpy.array(results),bins,label=pName,histtype='bar',log=False,alpha=myalph,**kwargs)
    pylab.title(pName)
    pylab.xlabel(pXlabel)    
    if drawGraph:
        pylab.savefig('histogram-'+strcsv(pName),dpi=72)
        pylab.close()
	return None
    return pylabGraphics(pylab.savefig,pylab.close)
def SummaryPlotC(cPlotVar,pName='',pXlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,cOp='AVG',norm=True):
    if pName=='': pName=ptName(cPlotVar)
    if pXlabel=='': pXlabel=ptName(cPlotVar)
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    curSQLcmd='select '+cOp+'('+cPlotVar+') as cVal from Lacuna WHERE Project_Number = "'+str(projectTitle)+'" '+sqlAdd+' GROUP BY Canal_Number'

    results=(cur.execute(curSQLcmd)).fetchall()
    results=[obj[0] for obj in results if type(obj[0]) is type(pi)]
    pylab.hist(numpy.array(results),bins,label=pName,normed=True,histtype='bar',log=False,alpha=myalph)
    if drawGraph:
        pylab.title(pName)
        pylab.xlabel(pXlabel)
        pylab.savefig('histogram-'+strcsv(pName),dpi=72)
        pylab.close()
	return None
    return pylabGraphics(pylab.savefig,pylab.close)
def SummaryPlotGrp(gGroups,cPlotVar,pName='',pXlabel='',bins=20,sqlAdd='',normed=True,cOp='AVG',drawGraph=True,showLegend=True,imgDPI=72,pFunc=None):
    """SummaryPlotC(gGroupscPlotVar,pName='',pXlabel='',bins=20,sqlAdd='',myalph=1,cOp='AVG'):
    Runs and stacks SQLHistPlot over a group of SQL where compatible statements stored in a dictionary
    """
    globals()['persistentColoringCount']=0
    globals()['persistentStylingCount']=0
    keyList=gGroups.keys()
    keyList.sort()
    keyList.reverse()
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd+' '
    cAlph=1
    pylab.close()
    cWid=1.0/len(keyList)
    bins=np.array(bins)
    binStep=(bins[1]-bins[0])/len(keyList)
    minbins=min(bins)-binStep
    maxbins=max(bins)+binStep
    if pFunc is None: pFunc=consistentColoringFunction
    for cGroup in keyList:
        # New Function
        
        SummaryPlot(cPlotVar,pName=cGroup,pXlabel=pXlabel,bins=bins,sqlAdd=gGroups[cGroup]+' '+sqlAdd,drawGraph=False,myalph=cAlph,normed=normed,color=pFunc(cGroup),rwidth=cWid,align='left')
        bins+=binStep
    
    if pXlabel=='': pXlabel=ptName(cPlotVar)
    
    if drawGraph:
        title(pName)
        xlabel(pXlabel)
        ylabel('Count')
	cAx=pylab.axis()
	cAx=(minbins,maxbins,cAx[2],cAx[3])
	print cAx
	pylab.axis(cAx)
	if showLegend: pylab.legend(loc=0)
        pylab.savefig('histogram-'+strcsv(pName)+'.pdf',dpi=imgDPI*2)
        pylab.savefig('histogram-'+strcsv(pName),dpi=imgDPI)
        pylab.close()	
def SummaryPlotCGrp(gGroups,cPlotVar,pName='',pXlabel='',bins=20,sqlAdd='',myalph=0.9,cOp='AVG',drawGraph=True,showLegend=True,imgDPI=72):
    """SummaryPlotC(gGroupscPlotVar,pName='',pXlabel='',bins=20,sqlAdd='',myalph=1,cOp='AVG'):
    Runs and stacks SQLHistPlot over a group of SQL where compatible statements stored in a dictionary
    """
    keyList=gGroups.keys()
    keyList.sort()
    keyList.reverse()
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd+' '
    cAlph=1
    pylab.close()
    for cGroup in keyList:
        # New Function
        
        SummaryPlotC(cPlotVar,pName=cGroup,pXlabel=pXlabel,bins=bins,sqlAdd=gGroups[cGroup]+' '+sqlAdd,drawGraph=False,myalph=cAlph,cOp=cOp)
        cAlph*=myalph
    
    if pXlabel=='': pXlabel=ptName(cPlotVar)
    
    if drawGraph:
        title(pName)
        xlabel(pXlabel)
        ylabel('Count')
	if showLegend: pylab.legend()
        pylab.savefig('histogram-'+strcsv(pName)+'.pdf',dpi=imgDPI*2)
        pylab.savefig('histogram-'+strcsv(pName),dpi=imgDPI)
        pylab.close()	
def _fixVar(cVar):
    
    if cVar[0]=='&': return cVar[1:]
    if cVar[1]=='$': return cVar[1:]
    return 'AVG('+cVar+')'
def SummaryPlot2(cPlotVar1,cPlotVar2,pXlabel='',pYlabel='',pName='',bins=20,stdVal='',sqlAdd='',drawGraph=True,myalph=1,logX=False,logY=False,logC=False,fancy=True,axisIn=None,dpiIn=72,groupByi=''):
    """ SummaryPlot2(cPlotVar1,cPlotVar2,pXlabel='',pYlabel='',pName='',bins=20,stdVal='',sqlAdd='',drawGraph=True,myalph=1,logX=False,logY=False,logC=False)
    For Example
    SummaryPlot2('VOLUME*1000*1000*1000','PROJ_PCA1/(PROJ_PCA2+PROJ_PCA3)*2',bins=200,sqlAdd=inRoiSuf+femSuf,stdVal=2,logX=True)
    """
    if pXlabel=='': pXlabel=ptName(cPlotVar1)
    if pYlabel=='': pYlabel=ptName(cPlotVar2)
    if pName=='': pName=pYlabel+' vs '+pXlabel
    origSql=sqlAdd
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    
    if groupByi!='': groupBy=' Group By '+groupByi
    else: groupBy=''
    xscale='linear'
    yscale='linear'
    cLog=None
    
    if logX: xscale='log'
    if logY: yscale='log'
    if logC: cLog='log'
    
    (cmin1,cmax1,cmean1,cstd1)=_GetVarStats(cPlotVar1,sqlAdd,groupBy=groupByi)
    (cmin2,cmax2,cmean2,cstd2)=_GetVarStats(cPlotVar2,sqlAdd,groupBy=groupByi)
    if stdVal!='':  
        cmin1=max(cmin1,cmean1-cstd1*stdVal)
        cmax1=min(cmax1,cmean1+cstd1*stdVal)
        cmin2=max(cmin2,cmean2-cstd2*stdVal)
        cmax2=min(cmax2,cmean2+cstd2*stdVal)
    bndSql=' AND '+cPlotVar1+' BETWEEN '+str(cmin1)+' AND '+str(cmax1)+' '
    bndSql+='AND '+cPlotVar2+' BETWEEN '+str(cmin2)+' AND '+str(cmax2)+' '
    if groupBy=='': corrTable([cPlotVar1,cPlotVar2],sqlAdd=origSql+bndSql)
    if groupBy!='':
    	cPlotVar1=_fixVar(cPlotVar1)
    	cPlotVar2=_fixVar(cPlotVar2)
    
    totSqlStr='select '+cPlotVar1+','+cPlotVar2+' from Lacuna WHERE Project_Number = "'+str(projectTitle)+'"'+bndSql+sqlAdd+groupBy
    
    #print totSqlStr
    results=numpy.array((cur.execute(totSqlStr)).fetchall())
    #print results
    #xedges=numpy.linspace(cmin1,cmax1,bins)
    #yedges=numpy.linspace(cmin2,cmax2,bins)
    #X, Y = np.meshgrid(xedges, yedges)
    #H, a, b=histogram2d(results[:,0],results[:,1],bins=[xedges,yedges])
    #return (H,X,Y)
    plt.hexbin(results[:,0],results[:,1],gridsize=bins,bins=cLog,xscale=xscale,yscale=yscale,marginals=False,extent=axisIn)
    plt.colorbar()
    pylab.title(pName)
    pylab.xlabel(pXlabel)
    pylab.ylabel(pYlabel)
    if axisIn!=None: pylab.axis(axisIn)
    if drawGraph:
    	#plt.imshow(H)
	#plt.contour(X,Y,H)
    	#matrix_plot(H)
        pylab.savefig('histogram-'+strcsv(pName),dpi=dpiIn)
        pylab.close()
	return None
    return pylabGraphics(pylab.savefig,pylab.close)	
    
def CompareStrTable(gGroups,pVars=['PCA1_S'],sqlAdd='',grpBy='Project',dbTable='Canal'):


    headerLine=['Group']
    headerLine+=[ptName(cVar) for cVar in pVars]
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    def sqlWrap(cStr):
        # New Function
	if cStr[0]=='$': return cStr[1:]
	return 'AVG('+cStr+')'
    nvVars=[sqlWrap(cVar) for cVar in pVars]
    sqlCmd='select '+','.join(nvVars)+' from '+dbTable+' where Project_Number="'+str(projectTitle)+'" '+sqlAdd
    results=[]
    for cGroup in gGroups.keys():
        # New Function
	cLine=cur.execute(sqlCmd+' AND '+gGroups[cGroup]+' group by '+grpBy).fetchall()
	
	cLine=[[cGroup]+list(ccLine) for ccLine in cLine]
	results+=cLine
    print results    
    htmlTable(headerLine,results)



def CompareStrGraph(gGroups,pVar='PCA1_S',pName='',pXlabel='',bins=40,sqlAdd='',imgDPI=72):
    if pName=='': pName=ptName(pVar)
    if pXlabel=='' : pXlabel=ptName(pVar)
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd    
    #from pylab import *
    myalph=1
    for cGroup in gGroups.keys():
        # New Function
        
        results=dbExecute(pVar,wherei=gGroups[cGroup]+sqlAdd+' AND Project_Number = "'+str(projectTitle)+'"')
        
        #results=(cur.execute('select '+pVar+' from Lacuna where )).fetchall()
        hist(numpy.array(results),bins,normed=True,label=cGroup,histtype='bar',alpha=myalph,log=False)
        myalph*=0.75
    title(pName)
    xlabel(pXlabel)
    legend()
    if drawGraph:
    	pylab.savefig('histogram-'+strcsv(pName),dpi=imgDPI)
    	pylab.close()
	return None
    return pylabGraphics(pylab.savefig,pylab.close)
def GroupCompareGraph(gGroups,pVar='PCA1_S',pName='',pXlabel='',bins=40,sqlAdd='',drawGraph=True,table='Lacuna',alphaDecay=0.8,stdVal=1):
    if pName=='': pName=ptName(pVar)
    if pXlabel=='' : pXlabel=ptName(pVar)
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd    
    #from pylab import *
    # Determine Range Values
    stdStr=''
    if stdVal is not None:
        # New Function
        groupTotal='AND ('+' OR '.join(['('+cVal+')' for cVal in gGroups.values()])+')'
        (cmin,cmax,cmean,cstd)=_GetVarStats(pVar,groupTotal+sqlAdd,table=table)
        cmin=max(cmin,cmean-stdVal*cstd)
        cmax=min(cmax,cmean+stdVal*cstd)
        stdStr=' AND '+pVar+' BETWEEN '+str(cmin)+' AND '+str(cmax)+' '
        
        
    myalph=1
    gKeys=gGroups.keys()
    gKeys.sort()
    for cGroup in gKeys:
        # New Function
        results=dbExecute(pVar,wherei=gGroups[cGroup]+sqlAdd+stdStr,table=table)
        #print results
        #results=(cur.execute('select '+pVar+' from Lacuna where )).fetchall()
        pylab.hist(numpy.array(results),bins,normed=True,label=cGroup,histtype='bar',alpha=myalph,log=False)
        myalph*=alphaDecay
    pylab.title(pName)
    pylab.xlabel(pXlabel)
    pylab.legend()
    if drawGraph:
    	savefig('histogram-'+strcsv(pName)+rndSuf(),dpi=72)
    	close()
    return pylabGraphics(pylab.savefig,pylab.close)        
def CompareGraph(gGroups,pVar='PCA1_S',pName='',pXlabel='',bins=40,sqlAdd='',drawGraph=True):
    if pName=='': pName=ptName(pVar)
    if pXlabel=='' : pXlabel=ptName(pVar)
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd    
    #from pylab import *
    myalph=1
    for cGroup in gGroups.keys():
        # New Function
        
        results=dbExecute(pVar,wherei=gGroups[cGroup][0]+' LIKE '+gGroups[cGroup][1]+sqlAdd+' AND Project_Number = "'+str(projectTitle)+'"')
        
        #results=(cur.execute('select '+pVar+' from Lacuna where )).fetchall()
        pylab.hist(numpy.array(results),bins,normed=True,label=cGroup,histtype='bar',alpha=myalph,log=False)
        myalph*=0.75
    pylab.title(pName)
    pylab.xlabel(pXlabel)
    pylab.legend()
    if drawGraph:
    	savefig('histogram-'+strcsv(pName),dpi=72)
    	close()
    return pylabGraphics(pylab.savefig,pylab.close)    
def ShowRanges(gRanges,sqlAdd='',table='Lacuna'):
    fakeOut={}
    totalSum=0
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd  
    for cRange in gRanges.keys():
        results=(cur.execute('select Count(Lacuna_Number) from Lacuna where '+gRanges[cRange][0]+'<'+str(gRanges[cRange][2])+' AND '+gRanges[cRange][0]+'>'+str(gRanges[cRange][1])+sqlAdd+' AND Project_Number = "'+str(projectTitle)+'"')).fetchall()[0]
        results=results[0]
        fakeOut[cRange]=results
        totalSum+=results
    html('<font size=2><table border="1"><tr><td>'+'</td><td>'.join(['Field','Percentage','Count'])+'</td></tr>')
    html('<tr><td>'+'</td></tr><tr><td>'.join(['</td><td>'.join((strd(obj),strd(fakeOut[obj]/(totalSum+0.00)*100.0)+'%',strd(fakeOut[obj]))) for obj in fakeOut.keys()])+'</td></tr>')
    html('</table></font>')  

def ShowGroups(gGroups,sqlAdd='',table='Lacuna',allProjects=False,countField='COUNT(*)'):
    fakeOut={}
    totalSum=0
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    prjText=''
    if projectTitle is not '': prjText=' AND Project_Number = "'+str(projectTitle)+'"'  
    if allProjects: prjText=''
    for cRange in gGroups.keys():
        results=(cur.execute('select '+countField+' from '+table+' where '+str(gGroups[cRange])+prjText+sqlAdd)).fetchall()[0]
        results=results[0]
	if results is None: results=0
        fakeOut[cRange]=results
        totalSum+=results
    html('<font size=2><table border="1"><tr><td>'+'</td><td>'.join(['Field','Percentage','Count'])+'</td></tr>')
    html('<tr><td>'+'</td></tr><tr><td>'.join(['</td><td>'.join((strd(obj),strd(fakeOut[obj]/(totalSum+0.00)*100.0)+'%',strd(fakeOut[obj]))) for obj in fakeOut.keys()])+'</td></tr>')
    html('</table></font>')  

def RangeGraph(gRanges,pVar='PCA1_S',pName='',pXlabel='',bins=30,sqlAdd='',isNormed=True):
    if pName=='': pName=ptName(pVar)
    if pXlabel=='' : pXlabel=ptName(pVar)
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd     
    #from pylab import *
    myalph=1
    for cRange in gRanges.keys():
        results=(cur.execute('select '+pVar+' from Lacuna where '+gRanges[cRange][0]+'<'+str(gRanges[cRange][2])+' AND '+gRanges[cRange][0]+'>'+str(gRanges[cRange][1])+sqlAdd+' AND Project_Number = "'+str(projectTitle)+'"')).fetchall()
        hist(numpy.array(results),bins,normed=isNormed,label=cRange,histtype='bar',alpha=myalph,log=False)
        myalph*=0.75
    title(pName)
    xlabel(pXlabel)
    legend()
    savefig('histogram-'+strcsv(pName),dpi=72)
    close()
def CompareVar(cVar1,cVar2,sqlAdd=''):
   if sqlAdd!='': sqlAdd=' AND '+sqlAdd
   results1=numpy.array((cur.execute('select '+cVar1+' from Lacuna WHERE Project_Number = "'+str(projectTitle)+'"'+sqlAdd)).fetchall())
   results2=numpy.array((cur.execute('select '+cVar2+' from Lacuna WHERE Project_Number = "'+str(projectTitle)+'"'+sqlAdd)).fetchall())
   print '\t'.join(['Var','Mean','Std'])
   print '\t'.join([cVar1,str(results1.mean()),str(results1.std())])
   print '\t'.join([cVar2,str(results2.mean()),str(results2.std())])
   tCorr=(results1-results1.mean())/results1.std()*(results2-results2.mean())/results2.std()
   print '\t'.join(['Together',str(tCorr.mean()),str(tCorr.std())])
def VarGraph(cVar1,cVar2,sqlAdd=''):
   if sqlAdd!='': sqlAdd=' AND '+sqlAdd
   results=(cur.execute('select '+cVar1+','+cVar2+' from Lacuna WHERE Project_Number = "'+str(projectTitle)+'"'+sqlAdd)).fetchall()
   histogramdd(results)
   title(cVar1+' vs. '+cVar2)
   xlabel(cVar1)
   ylabel(cVar2)
   return results
def GridLabelBoxPlot(boneList,pVars='PCA1_S',pName='',pXlabel='',sqlAdd='',vert=0,dpiIn=50,groupField='Sample_AIM_Number',dbTable='Lacuna'):
    nsqlAdd=sqlAdd
    if sqlAdd!='': nsqlAdd=' AND '+sqlAdd
    curFiga=plt.figure()
    for i in range(0,len(pVars)):
        curFig=plt.subplot(1,len(pVars),i+1)
        pVar=pVars[i]
        rName=pName
        rLabel=pXlabel
        if type(pName)==type([]): rName=pName[i]
        if type(pXlabel)==type([]): rLabel=pXlabel[i]
        LabelBoxPlot(boneList,pVar=pVar,pName=rName,pXlabel=rLabel,sqlAdd=sqlAdd,vert=vert,curFig=curFig,groupField=groupField,dbTable=dbTable)
    plt.setp(curFiga,'size_inches',[7*len(pVars),len(boneList)/2.5+2])
    plt.savefig('gl'+str(md5.md5(str(pName)+str(pVars)+groupField).hexdigest())+'.png',dpi=dpiIn)
    plt.close()
    
def LabelBoxPlot(boneList,pVar='PCA1_S',pName='',pXlabel='',bins=30,sqlAdd='',isNormed=True,vert=0,dpiIn=50,curFig='',groupField='SAMPLE_AIM_NUMBER',dbTable='Lacuna'):
    bMean=[]
    bStd=[]
    fehler=0
    saveImg=False
    if curFig=='': 
        curFig=plt.figure()
        saveImg=True
        
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    StrSearchWrapper=lambda x,y: ' AND '+x+' like "'+x+'" '
    NumSearchWrapper=lambda x,y: ' AND '+x+' = '+str(y)+' '
    BlkSearchWrapper=lambda x,y: ''
    curSearchWrapper=StrSearchWrapper
    if type(boneList)!=type([]):
        if boneList.find('*')>-1: curSearchWrapper=BlkSearchWrapper
	boneList=[obj[0] for obj in (cur.execute('select '+groupField+' from '+dbTable+' WHERE Project_Number = "'+str(projectTitle)+'" '+curSearchWrapper(groupField,boneList)+sqlAdd+' group by '+groupField)).fetchall()]
    if boneList[0]==str(boneList[0]): curSearchWrapper=StrSearchWrapper
    else: curSearchWrapper=NumSearchWrapper
    cDex=0
    plist=[]
    ind=[]
    tickNames=[]
    oTable=[]
    width=1
    for cBone in boneList: 
        results=(cur.execute('select '+pVar+' from '+dbTable+' WHERE Project_Number = "'+str(projectTitle)+'" '+curSearchWrapper(groupField,cBone)+sqlAdd)).fetchall()
        results=numpy.array([cEle[0] for cEle in results])
        #print min(results)
        results.shape=(-1,1)
        
        oTable+=[results]
        ind+=[cDex]
        tickNames+=[strd(cBone)]
        cDex+=width
    plt.boxplot(oTable,vert=vert,positions=ind)
    if vert==0:
        plt.xlabel(pXlabel)
        plt.yticks(ind, tickNames)
    else:
        plt.ylabel(pXlabel)
        plt.xticks(ind, tickNames)
    plt.title(pName)
    #print plt.axis()
    if saveImg:
        plt.setp(curFig,'size_inches',[8,len(boneList)/2.5+2])
        plt.savefig('boxplot'+pName,dpi=dpiIn)
        plt.close() 
    return pylabGraphics(plt.savefig,plt.close)
        
    
def LabelPlot(boneList,pVar='PCA1_S',pName='',pXlabel='',bins=30,sqlAdd='',isNormed=True,dpiIn=50,pVar2='',showError=True,drawGraph=True):
    bMean=[]
    bStd=[]
    fehler=0
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    curFig=plt.figure()
    if type(boneList)!=type([]):
        boneList=[obj[0] for obj in (cur.execute('select SAMPLE_AIM_NUMBER from Lacuna WHERE Project_Number = "'+str(projectTitle)+'" AND SAMPLE_AIM_NUMBER Like "'+boneList+'" '+sqlAdd+' group by SAMPLE_AIM_NUMBER')).fetchall()]
    for cBone in boneList: 
    	if pVar[0]=='&':
            curSQLcmd=pVar[1:]
            curSQLcmd+=',0'
            showError=False
            #if curSQLcmd.find(',')0: 
                
	elif pVar2=='':
	    curSQLcmd='AVG('+pVar+'),STD('+pVar+')'
	else:
	    curSQLcmd='CORR('+pVar+','+pVar2+'),TTEST('+pVar+','+pVar2+')'
        curSQLcmd='select '+curSQLcmd+' from Lacuna WHERE Project_Number = "'+str(projectTitle)+'" AND Sample_AIM_Number = "'+cBone+'" '+sqlAdd
        #print curSQLcmd
        results=(cur.execute(curSQLcmd)).fetchall()[0]
        
        if results[0] is not None:
            bMean+=[results[0]]
            bStd+=[results[1]]
        else:
            fehler+=1
            bMean+=[0]
            bStd+=[0]
        
    ind = numpy.array(numpy.arange(len(bMean)))    # the x locations for the groups
    width = 1       # the width of the bars: can also be len(x) sequence
    
    
    if showError: p1 = plt.barh(ind, bMean,   width, color='r', xerr=bStd)
    else: p1 = plt.barh(ind, bMean,   width, color='r')
    #p2 = plt.bar(ind, womenMeans, width, color='y',
    #             bottom=menMeans, yerr=menStd)
    plt.xlabel(pXlabel)
    plt.title(pName)
    curAxes=plt.axes()
    
    #curAxes.set_xticks(ind+width/2)
    tickNames=tuple(map(strd,boneList))
    #curAxes.set_xticklabels(tickNames,False,{'fontsize':'xx-small','rotation':'vertical'})
    plt.yticks(ind+width/2., tickNames)
    plt.setp(curFig,'size_inches',[8,len(boneList)/3.1+2])
    curAxis=plt.axis()
    curAxis=[curAxis[0],curAxis[1],0,len(bMean)]
    plt.axis(curAxis)
    if drawGraph:
    	plt.savefig('bdplot'+strcsv(pName),dpi=dpiIn)
    	plt.close()
        return None
    return pylabGraphics(pylab.savefig,pylab.close)
def LabelPlotOld(boneList,pVar='PCA1_S',pName='',pXlabel='',bins=30,sqlAdd='',isNormed=True,dpiIn=50,pVar2='',showError=True,drawGraph=True):
    bMean=[]
    bStd=[]
    fehler=0
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    curFig=plt.figure()
    if type(boneList)!=type([]):
        boneList=[obj[0] for obj in (cur.execute('select SAMPLE_AIM_NUMBER from Lacuna WHERE Project_Number = "'+str(projectTitle)+'" AND SAMPLE_AIM_NUMBER Like "'+boneList+'" '+sqlAdd+' group by SAMPLE_AIM_NUMBER')).fetchall()]
    for cBone in boneList: 
    	
	if pVar2=='':
	    curSQLcmd='AVG('+pVar+'),STD('+pVar+')'
	else:
	    curSQLcmd='CORR('+pVar+','+pVar2+'),TTEST('+pVar+','+pVar2+')'
        curSQLcmd='select '+curSQLcmd+' from Lacuna WHERE Project_Number = "'+str(projectTitle)+'" AND Sample_AIM_Number = "'+cBone+'" '+sqlAdd
        results=(cur.execute(curSQLcmd)).fetchall()[0]
        
        if results[0] is not None:
            bMean+=[results[0]]
            bStd+=[results[1]]
        else:
            fehler+=1
            bMean+=[0]
            bStd+=[0]
        
    ind = numpy.array(numpy.arange(len(bMean)))    # the x locations for the groups
    width = 1       # the width of the bars: can also be len(x) sequence
    
    
    if showError: p1 = plt.barh(ind, bMean,   width, color='r', xerr=bStd)
    else: p1 = plt.barh(ind, bMean,   width, color='r')
    #p2 = plt.bar(ind, womenMeans, width, color='y',
    #             bottom=menMeans, yerr=menStd)
    plt.xlabel(pXlabel)
    plt.title(pName)
    curAxes=plt.axes()
    
    #curAxes.set_xticks(ind+width/2)
    tickNames=tuple(map(strd,boneList))
    #curAxes.set_xticklabels(tickNames,False,{'fontsize':'xx-small','rotation':'vertical'})
    plt.yticks(ind+width/2., tickNames)
    plt.setp(curFig,'size_inches',[8,len(boneList)/3.1+2])
    curAxis=plt.axis()
    curAxis=[curAxis[0],curAxis[1],0,len(bMean)]
    plt.axis(curAxis)
    if drawGraph:
    	plt.savefig('bdplot'+pName,dpi=dpiIn)
    	plt.close()  
    return pylabGraphics(pylab.savefig,pylab.close)	 
# Looks between two cutoffs
# when evaluating cutoff-factor, one wants as low of a number as possible
# where the weird data is still removed
# this tool allows you to find the transition much more easily
def EvalCutoffGraph(cVar='Canal_Distance_Mean',startR=1,endR=2,useMask=False):
    generateROI(startR,useMask=useMask)
    curStr=globals()['inRoiSuf']
    generateROI(endR,useMask=useMask)
    globals()['outRoiSuf']=curStr+' AND '+globals()['outRoiSuf']
    SummaryPlot(cVar,'In ROI','Canal Distance (mm)',40,sqlAdd=globals()['inRoiSuf'],drawGraph=False)
    SummaryPlot(cVar,'Between ROI','Canal Distance (mm)',40,sqlAdd=globals()['outRoiSuf'],drawGraph=False,myalph=0.7)
    legend()
    title(cVar+str((startR,endR)))
    xlabel('Distance (mm)')
    savefig('histogram'+cVar+str((startR,endR))+'.png',dpi=72)
    close()
    
import numpy
from pylab import *
def SQLSpec(cPlotVar,bins=256,pts=5000,pName='',pXlabel='',pYlabel='',sqlAdd='',drawGraph=True,imgDPI=72):
    """SQLRawPlot(cPlotVarX,cPlotVarY,pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,norm=True,pts=5000,imgDPI=72):
    """
    if pYlabel=='': pYlabel='Energy Strength'
    if pXlabel=='': pXlabel='$\omega$ :'+ptName(cPlotVar)
    if pName=='': pName=pXlabel

    (bins,vals)=SQLHist2(cPlotVar,bins=bins,sqlAdd=sqlAdd,giveTable=True)
    fig=figure()
    ax = fig.add_subplot(111, frame_on=False)
    psd(vals)
    if drawGraph:
        title(pName,size=20)
        xlab=xlabel(pXlabel,size=20)
        ylab=ylabel(pYlabel,size=20)
	#ax.set_xticklabels([clab.get_text() for clab in ax.get_xticklabels()],'fontsize',24)
	#for tick in ax.xaxis.get_major_ticks(): globals()['tickz']=tick

	#setp(xlab,)
	#setp(ylab,size='large')
        savefig('histogram-'+strcsv(pName),dpi=imgDPI)
        close()
	return vals

def SQLRawPlot(cPlotVarX,cPlotVarY,pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,xnorm=True,ynorm=False,fixVals=None,pStyle='.',pts=5000,imgDPI=72,coherePlot=False,fitFunc=None,logx=False,logy=False,table='Laucuna',doContour=False,**kwargs):
    """SQLRawPlot(cPlotVarX,cPlotVarY,pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,norm=True,pts=5000,imgDPI=72,doContour=False):
    """
    if pXlabel=='': pXlabel=ptName(cPlotVarX)
    if pYlabel=='': pYlabel=ptName(cPlotVarY)
    if pName=='': pName=pYlabel+' vs '+pXlabel

    sqlArgs=[]
    if projectTitle is not None: sqlArgs+=['Project_Number="'+str(projectTitle)+'" ']
    if sqlAdd!='': sqlArgs+=[sqlAdd]
    if doContour: sqlArgs+=['is_numeric('+cPlotVarX+') AND '+cPlotVarX+'>0']
    if doContour: sqlArgs+=['is_numeric('+cPlotVarY+') AND '+cPlotVarY+'>0']
    dbData=[]
    for (cX,cY) in dbExecute(cPlotVarX+','+cPlotVarY,wherei=' AND '.join(sqlArgs),records=pts,table=table):
        try:
            dbData+=[(float(cX),float(cY))]
        except:
            1+1
    dbData=numpy.array(dbData)
    
    #fig=figure()
    #ax = fig.add_subplot(111, frame_on=False)
    xdat=dbData[:,0]
    ydat=dbData[:,1]
    
    if drawGraph: pylab.close()
    if coherePlot: cohere(xdat,ydat)
    else: 
        print (len(xdat),pStyle)
        if doContour:
            xMin=min(xdat)
            xMax=max(xdat)
            yMin=min(ydat)
            yMax=max(ydat)
            if fixVals is not None:
                (xMin,xMax,yMin,yMax)=fixVals
            
            xvals=linspace(xMin,xMax,bins+1)
            yvals=linspace(yMin,yMax,bins+1)
            #[xx,yy]=np.meshgrid(xvals,yvals)
            hv,xe,ye  = np.histogram2d(xdat, ydat, bins=(xvals, yvals))
            xvals=linspace(xMin,xMax,bins)
            yvals=linspace(yMin,yMax,bins)
            contour(xvals,yvals,hv.transpose(),colors=consistentColoringFunction(pName),label=pName)
            cHand=plot(xdat,ydat,consistentColoringFunction(pName)+'.',linewidth=int(0),markersize=int(2),label=pName)
        else:
            

            if (logx & logy): cHand=loglog(xdat,ydat,pStyle,linewidth=int(2),markersize=int(3),label=pName)
            elif logx: cHand=semilogx(xdat,ydat,pStyle,linewidth=int(2),markersize=int(3),label=pName)
            elif logy: cHand=semilogy(xdat,ydat,pStyle,linewidth=int(2),markersize=int(3),label=pName)
            else: cHand=plot(xdat,ydat,pStyle,linewidth=int(2),markersize=int(3),label=pName)
        
    
    if drawGraph:
        title(pName,size=20)
        pylab.xlabel(pXlabel,size=20)
        pylab.ylabel(pYlabel,size=20)
        pylab.savefig('histogram-'+strcsv(pName),dpi=imgDPI)
        pylab.close()
    return (xdat,ydat,cHand)
def SQLHist(cPlotVar,pName='',pXlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,norm=True,imgDPI=72):
    """SQLHist(cPlotVar,pName='',pXlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,norm=True,imgDPI=72):
    """
    if pName=='': pName=ptName(cPlotVar)
    if pXlabel=='': pXlabel=ptName(cPlotVar)
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    #(cmin,cmax)=(cur.execute('select MIN('+cPlotVar+'),MAX('+cPlotVar+') from Lacuna WHERE Project_Number="'+str(projectTitle)+'" '+sqlAdd)).fetchall()[0]
    (cmin,cmax)=dbExecute('MIN('+cPlotVar+'),MAX('+cPlotVar+')',wherei='Project_Number="'+str(projectTitle)+'" '+sqlAdd)[0]
    crange=cmax-cmin
    cStepSize=crange/(bins+1)
    xBins=[]
    xVals=[]
    for cStep in range(1,bins+1):
        cX=cmin+cStepSize*cStep
        xBins+=[cX]
        cStepStr='AND '+cPlotVar+' BETWEEN '+str(cX-cStepSize/2)+' AND '+str(cX+cStepSize/2)
        #xVals+=[(cur.execute('select COUNT(Lacuna_Number) from Lacuna WHERE Project_Number="'+str(projectTitle)+'" '+cStepStr+sqlAdd)).fetchall()[0][0]]
        xVals+=[dbExecute('COUNT(Lacuna_Number)',wherei='Project_Number="'+str(projectTitle)+'" '+cStepStr+sqlAdd)[0][0]]
    xBins=numpy.array(xBins)
    xVals=numpy.array(xVals)
    #if norm: xVals/=(1.0*numpy.sum(xVals))
    outPlot=plot(xBins,xVals)
    
    if drawGraph:
        title(pName)
        xlabel(pXlabel)
        ylabel(cPlotVar)
        savefig('histogram-'+strcsv(pName),dpi=imgDPI)
        close()
    else:	
	return outPlot
def _GetVarStats(icVar,sqlAdd,table='Lacuna',groupBy=''):

    cVar='IFNULL('+icVar+',0)'
    sqlArgs=[]
    if projectTitle is not None: sqlArgs+=['Project_Number="'+str(projectTitle)+'"']
    if sqlAdd!='': sqlArgs+=[sqlAdd]
    print sqlArgs
    if groupBy=='':
    	(cmin,cmax,cmean,cstd)=dbExecute('MIN('+cVar+'),MAX('+cVar+'),AVG('+cVar+'),STD('+cVar+')',wherei=' AND '.join(sqlArgs),table=table)[0]
    else:
    	cVar=_fixVar(cVar)
    	baseSQLcmd='SELECT '+cVar+' as MainVar FROM LACUNA'
    	baseSQLcmd+=' WHERE Project_Number="'+str(projectTitle)+'" '+sqlAdd
    	baseSQLcmd+=' GROUP BY '+groupBy
    	
    	superSQLcmd='SELECT MIN(MainVar),MAX(MainVar),AVG(MainVar),STD(MainVar) FROM ('+baseSQLcmd+') '
    	
    	(cmin,cmax,cmean,cstd)=numpy.array(cur.execute(superSQLcmd).fetchall()[0])
    	print (cmin,cmax,cmean,cstd)
    if cstd is None: cstd=0
    if cmean is None: cmean=0
    if cmin is None: cmin=cmean
    if cmax is None: cmax=cmean
    return (cmin,cmax,cmean,cstd)

def GroupTTestPlot(gGroups,cPlotVar,cPlotVarY,pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,norm=True,stdVal='',imgDPI=72,prefGroup='',countField='',showLegend=True):
    """GroupTTestPlot(cPlotVar,cPlotVarY,pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,norm=True,stdVal='',imgDPI=72)
    GroupTTestPlot is a function to plot the TTest between N-groups defined in the gGroup dictionary with SQL where compatible statements
    The x-axis is cPlotVar and the variable used for the ttest is cPlotVarY
    An example of how this function might be used is to compare two different sets of canals as a function of the distance away from the canal
    
    """
    minProb=1e-8
    if pXlabel=='': pXlabel=ptName(cPlotVar)
    if pYlabel=='': pYlabel=ptName(cPlotVarY)
    
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    keyList=gGroups.keys()
    keyList.sort()
    
    (cmin,cmax,cmean,cstd)=_GetVarStats(cPlotVar,sqlAdd)

    if stdVal!='':  
        cmin=max(cmin,cmean-cstd*stdVal)
        cmax=min(cmax,cmean+cstd*stdVal)
        
    crange=cmax-cmin
    cStepSize=crange/(bins+1)
    xBins=[]
    xVals={}
    
    curSQLcmd='AVG('+cPlotVarY+'),VAR('+cPlotVarY+')'
    if (countField==''): curSQLcmd+=',COUNT('+cPlotVarY+')'
    else: curSQLcmd+=',UNICNT('+countField+')'
    
    # Determine the Preffered Comparison Group and make a list of the other groups
    prefDex=0
    for cDex in range(len(keyList)): 
        if keyList[cDex].upper()==prefGroup.upper(): prefDex=cDex
    dexList=[cDex for cDex in range(len(keyList)) if cDex<>prefDex]
    
    xLabel=[keyList[cDex]+' vs '+keyList[prefDex] for cDex in range(len(keyList))]
    for cName in xLabel:
        xVals[cName]=[]
        

    for cStep in range(1,bins+1):
        cX=cmin+cStepSize*cStep
        cStepStr=' AND '+cPlotVar+' BETWEEN '+str(cX-cStepSize/2)+' AND '+str(cX+cStepSize/2)
        
        fehler=0
        gBins=[]
        for cGroup in keyList:
            woStr=cStepStr+' AND '+gGroups[cGroup]+' '+sqlAdd
            
            results=dbExecute(curSQLcmd,wherei=cStepStr+' AND '+gGroups[cGroup]+' '+sqlAdd)[0]
            #results=(1.1,5,10)
            if results[0] is not None:
                gBins+=[results]
            else:
                fehler+=1
       
        #print gBins
        if fehler==0:
            xBins+=[cX]
            pResult=gBins[prefDex]
            for cDex in dexList:
                cResults=gBins[cDex]
                (t,prob)=kttest(cResults[0],cResults[1],cResults[2],pResult[0],pResult[1],pResult[2])
                if prob<minProb: prob=minProb
                xVals[xLabel[cDex]]+=[prob]
        
        else:
            print str(fehler)+' problems of '+str(bins)
       
            
    
    xBins=numpy.array(xBins)
    for cDex in dexList:
        semilogy(xBins,numpy.array(xVals[xLabel[cDex]]),linewidth=3)
        

    if drawGraph:
        title(pYlabel,size=20)
        xlabel(pXlabel,size=20)
        ylabel('T-Test P-Value',size=20)
	if showLegend: legend([xLabel[cDex] for cDex in dexList])
        savefig('histogram-'+strcsv(pName),dpi=imgDPI)
        close()   
def xmapbin(fx,fy,cmin,cmax,bins):
    fxa=np.array(fx)
    fya=np.array(fy)
    outx=linspace(cmin,cmax,bins)
    outy=zeros(bins)
    cStep=(cmax-cmin)/bins
    for cdx in range(bins):
        nVals=find(abs(fxa-outx[cdx])<(cStep/2))
        if len(nVals)>0: outy[cdx]=fya[nVals[0]]
    return (outx,outy)


def SQLBoxGrp(gGroups,cPlotVar,pName='',pXlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,stdVal='',imgDPI=72,cPlotVarY2='',cPlotW='',useEbar=False,plotEbar=False,showLegend=True,fixVals=None,groupLimit=10000,vert=0):
    """SQLHistPlotGrp(gGroups,cPlotVar,cPlotVarY,pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,norm=True,stdVal=''):
    Runs and stacks SQLHistPlot over a group of SQL where compatible statements stored in a dictionary
    FitFunc is a function to fit the data (x,y) that comes from sqlhistplot.
    """
    pylab.close()
    globals()['persistentColoringCount']=0
    globals()['persistentStylingCount']=0
    if pXlabel=='': pXlabel=ptName(cPlotVar)
    if pName=='': pName=pXlabel
    if type(gGroups) is not type({}):
        groupBy=gGroups
        gGroups={}
        gList=cur.execute('SELECT '+groupBy+' from Lacuna where Project_Number = "'+str(projectTitle)+'" AND '+cPlotVar+' IS NOT NULL group by '+groupBy+' ORDER BY PROJECT,SAMPLE_AIM_NUMBER').fetchmany(groupLimit)
        for cObj in gList: gGroups[cObj[0]]=groupBy+'="'+cObj[0]+'"'
        print gGroups
    keyList=gGroups.keys()
    keyList.sort()
    keyList.reverse()
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd+' '
    cDex=0
    plist=[]
    ind=[]
    tickNames=[]
    oTable=[]
    width=1
    for cGroup in keyList:
        # New Function

        results=numpy.array(dbExecute(cPlotVar,wherei='Project_Number="'+str(projectTitle)+'" AND '+gGroups[cGroup]+' '+sqlAdd))
        results=numpy.array([cEle[0] for cEle in results])
        
        #print min(results)
        results.shape=(-1,1)
        
        oTable+=[results]
        ind+=[cDex]
        tickNames+=[strd(cGroup)]
        cDex+=width

    if drawGraph:
        title(pName)
        plt.boxplot(oTable,vert=vert,positions=ind)
    if vert==0:
        plt.xlabel(pXlabel)
        plt.yticks(ind, tickNames)
    else:
        plt.ylabel(pXlabel)
        plt.xticks(ind, tickNames)
        #if showLegend: legend(keyList,loc=0)
        
    #plt.setp(curFig,'size_inches',[8,len(keyList)/2.5+2])        
    pylab.savefig('histogram-'+strcsv(pName),dpi=int(imgDPI))
    pylab.savefig('histogram-'+strcsv(pName)+'.pdf',dpi=int(imgDPI*2))
    
    #pylab.close()	        

        
        
def SQLHistPlot(cPlotVar,cPlotVarY,pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,ynorm=0,xnorm=0,stdVal='',imgDPI=72,cPlotVarY2='',cPlotW='',useEbar=True,useR2=True,plotEbar=False,table='Lacuna',cGroupVar=None,fixVals=None,logx=False,logy=False,isInt=False,pStyle='-',**kwargs):
    """SQLHistPlot(cPlotVar,cPlotVarY,pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,norm=True,stdVal='',imgDPI=72,cPlotVarY2='',cPlotW='',useEbar=True,useR2=True,plotEbar=False)
    cPlotVarY2 does a correlation between the two variables
    cPlotW uses a weighted mean and std 
    """
    
    if pXlabel=='': 
    	pXlabel=ptName(cPlotVar)
    	if xnorm==1: pXlabel='Norm.'+pXlabel
    	elif xnorm==2: pXlabel='Norm.'+pXlabel
    	elif xnorm==3: pXlabel=pXlabel+'/<'+pXlabel+'>'
    	elif xnorm==4: pXlabel=pXlabel+'/<'+pXlabel+'>'
    	elif xnorm==5: pXlabel='Norm.'+pXlabel
    if pYlabel=='': 
    	pYlabel=ptName(cPlotVarY)
    	if ynorm==1: pYlabel='Probability (norm)'
    	elif ynorm==2: pYlabel='Occurance Normalized to X0 (\%)'
    	elif ynorm==3: pYlabel='Occurance Normalized to Max (\%)'
    	elif ynorm==4: pYlabel='Cumulative Probability (\%)'
    	elif ynorm==5: pYlabel=pYlabel+'/<'+pYlabel+'>'
    	elif ynorm==6: pYlabel=pYlabel+' (Vol.Frac \%)'
    if cGroupVar is None: cGroupVar=cPlotVar
    
    (cmin,cmax,cmean,cstd)=_GetVarStats(cGroupVar,sqlAdd,table=table)
    if fixVals is None:
        if stdVal!='':  
        	cmin=max(cmin,cmean-cstd*stdVal)
        	cmax=min(cmax,cmean+cstd*stdVal)
    else:
        cmin=min(fixVals)
        cmax=max(fixVals)

    
        
    crange=cmax-cmin
    cStepSize=crange/(bins+1)
    histGroupStr='LINSPACE('+cGroupVar+','+str(cmin)+','+str(cmax)+','+str(bins)+')'
    if isInt: histGroupStr=cGroupVar
    print histGroupStr
    xBins=[]
    xVals=[]
    eVals=[]
    fehler=0
    if cPlotVarY2=='':
        if cPlotW=='': curSQLcmd='AVG('+cPlotVarY+'),STD('+cPlotVarY+')'
	else: curSQLcmd='WAVG('+cPlotVarY+','+cPlotW+'),WSTD('+cPlotVarY+','+cPlotW+')'
        if pName=='': pName=pYlabel+' vs '+pXlabel
	if cPlotW=='': pName+=' (w)'
    else:
        if useR2: curSQLcmd='SQ(CORR('+cPlotVarY+','+cPlotVarY2+')),TTEST('+cPlotVarY+','+cPlotVarY2+')'
	else: curSQLcmd='CORR('+cPlotVarY+','+cPlotVarY2+'),TTEST('+cPlotVarY+','+cPlotVarY2+')'
        if pName=='': pName='Correlation ('+pYlabel+'::'+ptName(cPlotVarY2)+')'+' vs '+pXlabel
    if cPlotVarY[0]=='&':
    	cPlotVarY=cPlotVarY[1:]
    	curSQLcmd=cPlotVarY+',0'
    curSQLcmd=histGroupStr+','+curSQLcmd
    # Replace LINSPACE (int) with FLINSPACE (float)
    curSQLcmd='FLINSPACE'.join(curSQLcmd.split('LINSPACE'))
    sqlArgs=[]
    sqlArgs+=[cGroupVar+' BETWEEN '+str(cmin)+' AND '+str(cmax)]
    if sqlAdd!='': sqlArgs+=[sqlAdd]
    print (curSQLcmd,sqlArgs)
    results=numpy.array(dbExecute(curSQLcmd,wherei=' AND '.join(sqlArgs),groupbyi=histGroupStr,sortopt=cPlotVar,table=table))
    try:   
		xBins=results[:,0]
		xVals=results[:,1]
		eVals=results[:,2]
		
    except: return None
    (filledX,filledVal)=xmapbin(xBins,xVals,cmin,cmax,bins)
    (filledX,filledE)=xmapbin(xBins,eVals,cmin,cmax,bins)
    remapVals=True
    if (fixVals is not None) & (remapVals):   
		xBins=filledX
		xVals=filledVal
		eVals=filledE
		
    linfit=clacDB_LINFIT()
    ccorr=clacDB_CORR()
    for ix in range(len(xBins)):   
		linfit.step(xBins[ix],xVals[ix])
		ccorr.step(xBins[ix],xVals[ix])
	
    print ('Linear Fit:',linfit.finalize(),'Correlation:',ccorr.finalize())
    # Averages (assuming count based histogram)
    cntAvg=sum(xBins*xVals)/sum(xVals)
    cntStd=sqrt(sum(xBins*xBins*xVals)/sum(xVals)-cntAvg*cntAvg)
    # Y - Normalization
    print pStyle
    if ynorm==1: xVals=xVals/sum(xVals) # Normalize by total area (probability)
    elif ynorm==2: xVals=xVals/xVals[0]*100 # Normalize by first value then scale to percentage
    elif ynorm==3: xVals=xVals/max(xVals)*100 # Normalize by maximum value
    elif ynorm==4: xVals=cumsum(xVals/sum(xVals)) # Cumulative count
    elif ynorm==5: xVals/=mean(xVals) # Normalized by meane
    elif ynorm==6: xVals=xBins*xVals/sum(xBins*xVals)*100# Assume xPlotVar is a volume, xVal will be percentage of total
    # X - Normalization
    if xnorm==1: xBins=(xBins-cmean)/cstd # Global Std's Graph
    elif xnorm==2: xBins=(xBins-cntAvg)/cntStd # Local Std's Graph 
    elif xnorm==3: xBins/=cmean # Ratio to Global Mean
    elif xnorm==4: xBins/=cntAvg # Ratio to Local Mean
    elif xnorm==5: xBins=(xBins-cmin)/(cmax-cmin) # Normalized Units
    xBins=floatlist(xBins)
    xVals=floatlist(xVals)
    eVals=floatlist(eVals)
    if useEbar: cHand=errorbar(xBins,xVals,yerr=eVals,label=pName)
    elif plotEbar: cHand=plot(xBins,eVals,pStyle,markersize=int(12),label=pName)
    elif (logx & logy): cHand=loglog(xBins,xVals,pStyle,linewidth=int(3),markersize=int(12),label=pName)
    elif logx: cHand=semilogx(xBins,xVals,pStyle,linewidth=int(3),markersize=int(12),label=pName)
    elif logy: cHand=semilogy(xBins,xVals,pStyle,linewidth=int(3),markersize=int(12),label=pName)
    else: cHand=plot(xBins,xVals,pStyle,linewidth=int(3),markersize=int(12),label=pName)
    

    if drawGraph:
        title(pName)
        xlabel(pXlabel)
        ylabel(pYlabel)
        savefig('histogram-'+strcsv(pName),dpi=int(imgDPI))
        savefig('histogram-'+strcsv(pName)+'.pdf',dpi=int(imgDPI))
        
        close()
    return (xBins,xVals,cHand)        

from scipy.optimize import leastsq as lsq
import numpy as np
def meshgridn(*xi,**kwargs):
    """
    Return coordinate matrices from one or more coordinate vectors.

    Make N-D coordinate arrays for vectorized evaluations of
    N-D scalar/vector fields over N-D grids, given
    one-dimensional coordinate arrays x1, x2,..., xn.

    Parameters
    ----------
    x1, x2,..., xn : array_like
        1-D arrays representing the coordinates of a grid.
    indexing : 'xy' or 'ij' (optional)
        cartesian ('xy', default) or matrix ('ij') indexing of output
    sparse : True or False (default) (optional)
         If True a sparse grid is returned in order to conserve memory.
    copy : True (default) or False (optional)
        If False a view into the original arrays are returned in order to
        conserve memory

    Returns
    -------
    X1, X2,..., XN : ndarray
        For vectors `x1`, `x2`,..., 'xn' with lengths ``Ni=len(xi)`` ,
        return ``(N1, N2, N3,...Nn)`` shaped arrays if indexing='ij'
        or ``(N2, N1, N3,...Nn)`` shaped arrays if indexing='xy'
        with the elements of `xi` repeated to fill the matrix along
        the first dimension for `x1`, the second for `x2` and so on.

    See Also
    --------
    index_tricks.mgrid : Construct a multi-dimensional "meshgrid"
                     using indexing notation.
    index_tricks.ogrid : Construct an open multi-dimensional "meshgrid"
                     using indexing notation.

    Examples
    --------
    >>> x = np.linspace(0,1,3)   # coordinates along x axis
    >>> y = np.linspace(0,1,2)   # coordinates along y axis
    >>> xv, yv = meshgrid(x,y)   # extend x and y for a 2D xy grid
    >>> xv
    array([[ 0. ,  0.5,  1. ],
           [ 0. ,  0.5,  1. ]])
    >>> yv
    array([[ 0.,  0.,  0.],
           [ 1.,  1.,  1.]])
    >>> xv, yv = meshgrid(x,y, sparse=True)  # make sparse output arrays
    >>> xv
    array([[ 0. ,  0.5,  1. ]])
    >>> yv
    array([[ 0.],
           [ 1.]])

    >>> meshgrid(x,y,sparse=True,indexing='ij')  # change to matrix indexing
    [array([[ 0. ],
           [ 0.5],
           [ 1. ]]), array([[ 0.,  1.]])]
    >>> meshgrid(x,y,indexing='ij')
    [array([[ 0. ,  0. ],
           [ 0.5,  0.5],
           [ 1. ,  1. ]]),
     array([[ 0.,  1.],
           [ 0.,  1.],
           [ 0.,  1.]])]

    >>> meshgrid(0,1,5)  # just a 3D point
    [array([[[0]]]), array([[[1]]]), array([[[5]]])]
    >>> map(np.squeeze,meshgrid(0,1,5))  # just a 3D point
    [array(0), array(1), array(5)]
    >>> meshgrid(3)
    array([3])
    >>> meshgrid(y)      # 1D grid; y is just returned
    array([ 0.,  1.])

    `meshgrid` is very useful to evaluate functions on a grid.

    >>> x = np.arange(-5, 5, 0.1)
    >>> y = np.arange(-5, 5, 0.1)
    >>> xx, yy = meshgrid(x, y, sparse=True)
    >>> z = np.sin(xx**2+yy**2)/(xx**2+yy**2)
    """
    copy = kwargs.get('copy',True)
    args = np.atleast_1d(*xi)
    if not isinstance(args, list):
        if args.size>0:
            return args.copy() if copy else args
        else:
            raise TypeError('meshgrid() take 1 or more arguments (0 given)')

    sparse = kwargs.get('sparse',False)
    indexing = kwargs.get('indexing','xy') # 'ij'


    ndim = len(args)
    s0 = (1,)*ndim
    output = [x.reshape(s0[:i]+(-1,)+s0[i+1::]) for i, x in enumerate(args)]

    shape = [x.size for x in output]

    if indexing == 'xy':
        # switch first and second axis
        output[0].shape = (1,-1) + (1,)*(ndim-2)
        output[1].shape = (-1, 1) + (1,)*(ndim-2)
        shape[0],shape[1] = shape[1],shape[0]

    if sparse:
        if copy:
            return [x.copy() for x in output]
        else:
            return output
    else:
        # Return the full N-D matrix (not only the 1-D vector)
        if copy:
            mult_fact = np.ones(shape,dtype=int)
            return [x*mult_fact for x in output]
        else:
            return np.broadcast_arrays(*output)


def ndgrid(*args,**kwargs):
    """
    Same as calling meshgrid with indexing='ij' (see meshgrid for
    documentation).
    """
    kwargs['indexing'] = 'ij'
    return meshgridn(*args,**kwargs)

	        

def SQLHistPlotGrp(gGroups,cPlotVar,cPlotVarY,pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,xnorm=0,ynorm=0,stdVal='',imgDPI=72,cPlotVarY2='',cPlotW='',useEbar=False,plotEbar=False,showLegend=True,fixVals=None,logx=False,logy=False,isInt=False,groupLimit=10000,pStyle='-',pFunc=None,fitFunc=None,SPFun=SQLHistPlot,table='Lacuna',doContour=False,**kwargs):
    """SQLHistPlotGrp(gGroups,cPlotVar,cPlotVarY,pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,norm=True,stdVal=''):
    Runs and stacks SQLHistPlot over a group of SQL where compatible statements stored in a dictionary
    FitFunc is a function to fit the data (x,y) that comes from sqlhistplot.
    """
    pylab.close()
    globals()['persistentColoringCount']=0
    globals()['persistentStylingCount']=0
    if type(gGroups) is not type({}):
        groupBy=gGroups
        gGroups={}
        gList=cur.execute('SELECT '+groupBy+' from Lacuna where '+cPlotVar+' IS NOT NULL group by '+groupBy+' ORDER BY PROJECT,SAMPLE_AIM_NUMBER').fetchmany(groupLimit)
        for cObj in gList: gGroups[cObj[0]]=groupBy+'="'+cObj[0]+'"'
        print gGroups
    keyList=gGroups.keys()
    keyList.sort()
    keyList.reverse()
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd+' '
    outData={}
    outLeg={}
    if pFunc is None: 
        if len(pStyle)<2: pFunc=lambda x: consistentColoringFunction(x)+pStyle
        else: pFunc=lambda x: pStyle
    for cGroup in keyList:
        # New Function
        try:
            (colX,colY,cHand)=SPFun(cPlotVar,cPlotVarY,pName=cGroup,pXlabel=pXlabel,pYlabel=pYlabel,bins=bins,sqlAdd=gGroups[cGroup]+' '+sqlAdd,drawGraph=False,myalph=myalph,xnorm=xnorm,ynorm=ynorm,stdVal=stdVal,imgDPI=imgDPI,cPlotVarY2=cPlotVarY2,cPlotW=cPlotW,useEbar=useEbar,plotEbar=plotEbar,fixVals=fixVals,logx=logx,logy=logy,isInt=isInt,pStyle=pFunc(cGroup),table=table,doContour=doContour,**kwargs)
        except:
            #(colX,colY,cHand)=SPFun(cPlotVar,cPlotVarY,pName=cGroup,pXlabel=pXlabel,pYlabel=pYlabel,bins=bins,sqlAdd=gGroups[cGroup]+' '+sqlAdd,drawGraph=False,myalph=myalph,xnorm=xnorm,ynorm=ynorm,stdVal=stdVal,imgDPI=imgDPI,cPlotVarY2=cPlotVarY2,cPlotW=cPlotW,useEbar=useEbar,plotEbar=plotEbar,fixVals=fixVals,logx=logx,logy=logy,isInt=isInt,pStyle=pFunc(cGroup),table=table,doContour=doContour,**kwargs)
            colX=[]
            colY=[]
            cHand=0
            print 'Group Failed:'+cGroup
        outData[cGroup]=zip(colX,colY)
        outLeg[cGroup]=cHand
    
    globals()['SQLHistTemp']=outData
    if pXlabel=='': 
    	pXlabel=ptName(cPlotVar)
    	if xnorm==1: pXlabel='Norm.'+pXlabel
    	elif xnorm==2: pXlabel='Norm.'+pXlabel
    	elif xnorm==3: pXlabel=pXlabel+'/<'+pXlabel+'>'
    	elif xnorm==4: pXlabel=pXlabel+'/<'+pXlabel+'>'
    	elif xnorm==5: pXlabel='Norm.'+pXlabel
    if pYlabel=='': 
    	pYlabel=ptName(cPlotVarY)
    	if ynorm==1: pYlabel='Probability (norm)'
    	elif ynorm==2: pYlabel='Occurance Normalized to X0 (\%)'
    	elif ynorm==3: pYlabel='Occurance Normalized to Max (\%)'
    	elif ynorm==4: pYlabel='Cumulative Probability (\%)'
    	elif ynorm==5: pYlabel=pYlabel+'/<'+pYlabel+'>'
    	elif ynorm==6: pYlabel=pYlabel+' (Vol.Frac \%)'
    f=open(strcsv(pName)+'.csv','w')
    
    oLine=[]
    nLine=[]
    for cKey in keyList:
         oLine+=[str(cKey),str(cKey)]
         nLine+=[str(pXlabel),str(pYlabel)]
    f.write(','.join(oLine)+'\n')
    f.write(','.join(nLine)+'\n')
    cRow=0
    stillRemaining=True
    
    while stillRemaining:
        stillRemaining=False
        oLine=[]
        for cKey in keyList:
            if len(outData[cKey])>cRow: 
                oVals=[str(outData[cKey][cRow][0]),str(outData[cKey][cRow][1])]
                stillRemaining=True
            else: oVals=['','']
            oLine+=oVals
        if stillRemaining: f.write(','.join(oLine)+'\n')
        cRow+=1
    f.close()  
            
    if drawGraph:
        title(pName)
        xlabel(pXlabel)
        ylabel(pYlabel)
        if (fixVals is not None) & (xnorm<1):
            cAxis=pylab.axis()
            pylab.axis((min(fixVals),max(fixVals),cAxis[2],cAxis[3]))
        if showLegend: legend(loc=0)
        if fitFunc is not None:
            for cKey in keyList:
                cData=np.array(outData[cKey])
                xdat=cData[:,0]
                ydat=cData[:,1]
                guessP=fitFunc[0](xdat,ydat)
                (newP,msg)=lsq(lambda p: (ydat-fitFunc[1](xdat,p)),guessP)
                print guessP
                fitdat=fitFunc[1](xdat,newP)
                
                print (cKey,'-> Fit Vals:',newP,'R^2:',np.corrcoef(fitdat,ydat)[0,1]**2)
                
                cStyle=pFunc(cKey)
                if len(cStyle)>1: cStyle=cStyle[0]+'-'
                else: cStyle='-'
                xdat=np.array(floatlist(linspace(min(xdat),max(xdat),500)))
                fitdat=floatlist(fitFunc[1](xdat,newP))
                if (logx & logy): loglog(xdat,fitdat,cStyle,linewidth=int(2),markersize=int(3))
                elif logx: semilogx(xdat,fitdat,cStyle,linewidth=int(2),markersize=int(3))
                elif logy: semilogy(xdat,fitdat,cStyle,linewidth=int(2),markersize=int(3))
                else: plot(xdat,fitdat,cStyle,linewidth=int(2),markersize=int(3))
                
        pylab.savefig('histogram-'+seqName()+strcsv(pName),dpi=int(imgDPI))
        pylab.savefig('histogram-'+seqName()+strcsv(pName)+'.pdf',dpi=int(imgDPI*2))

def SQLHistPlotDiv(cPlotVar,cPlotVarY,divElement='CANAL_NUMBER',pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,xnorm=0,ynorm=0,stdVal='',imgDPI=72,useEbar=True,useR2=True,plotEbar=False,cGroupVar=None,fixVals=None,logx=False,logy=False,isInt=False):
    """SQLHistPlotDiv(cPlotVar,cPlotVarY,pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,norm=True,stdVal='',imgDPI=72,cPlotVarY2='',cPlotW='',useEbar=True,useR2=True,plotEbar=False)
    Plots the graph by grouping the data into chunks based on divElement (eg various canals or geographical regions) and then computing statistics
    """
    
    if pXlabel=='': pXlabel=ptName(cPlotVar)
    if pYlabel=='': pYlabel=ptName(cPlotVarY)
    if cGroupVar is None: cGroupVar=cPlotVar
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    if fixVals is None:
        (cmin,cmax,cmean,cstd)=_GetVarStats(cGroupVar,sqlAdd)
        if stdVal!='':  
        	cmin=max(cmin,cmean-cstd*stdVal)
        	cmax=min(cmax,cmean+cstd*stdVal)
    else:
        cmin=min(fixVals)
        cmax=max(fixVals)

    
        
    crange=cmax-cmin
    cStepSize=crange/(bins+1)
    histGroupStr='LINSPACE('+cGroupVar+','+str(cmin)+','+str(cmax)+','+str(bins)+')'
    if isInt: histGroupStr=cGroupVar
    print histGroupStr
    xBins=[]
    xVals=[]
    eVals=[]
    cVals=[]
    fehler=0
    
    if cPlotVarY[0]=='&':
    	cPlotVarY=cPlotVarY[1:]
    	curSQLcmd=cPlotVarY
    else:
    	curSQLcmd='AVG('+cPlotVar+')'
    	
    curSQLcmd+=' AS MainVar'
    curSQLcmd='FLINSPACE'.join(histGroupStr.split('LINSPACE'))+' AS LINS,'+curSQLcmd+',COUNT(*) AS CNT'
    # Replace LINSPACE (int) with FLINSPACE (float)
    dhistGroupStr='('+histGroupStr+' || '+divElement+')'
    baseSQLcmd='SELECT '+curSQLcmd+' FROM LACUNA'
    baseSQLcmd+=' WHERE Project_Number="'+str(projectTitle)+'" AND '+cGroupVar+' BETWEEN '+str(cmin)+' AND '+str(cmax)+sqlAdd
    baseSQLcmd+='GROUP BY '+dhistGroupStr
    superSQLcmd='SELECT LINS,AVG(MainVar),STD(MainVar),COUNT(MainVar),AVG(CNT) FROM ('+baseSQLcmd+') GROUP BY LINS ORDER BY LINS'
    results=numpy.array(cur.execute(superSQLcmd).fetchall())
    try:   
        xBins=results[:,0]
        xVals=results[:,1]
        eVals=results[:,2]
        cVals=results[:,3]
        scVals=results[:,4]
        print scVals
    except: 
        return None
    if useEbar: errorbar(xBins,xVals,yerr=eVals)
    elif plotEbar: plot(xBins,eVals) 
    elif logx: semilogx(xBins,xVals,linewidth=3)
    elif logy: semilogy(xBins,xVals,linewidth=3)
    else: plot(xBins,xVals,linewidth=3)
	
    if drawGraph:
        title(pName)
        xlabel(pXlabel)
        ylabel(pYlabel)
        savefig('histogram-'+strcsv(pName),dpi=imgDPI)
        savefig('histogram-'+strcsv(pName)+'.pdf',dpi=imgDPI)
        close()	
    return (xBins,xVals)
def SQLHistPlotDivGrp(gGroups,cPlotVar,cPlotVarY,divElement='Canal_Number',pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,xnorm=0,ynorm=0,stdVal='',imgDPI=72,useEbar=False,plotEbar=False,showLegend=True,fixVals=None,logx=False,logy=False,isInt=False):
    """SQLHistPlotGrp(gGroups,cPlotVar,cPlotVarY,pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,norm=True,stdVal=''):
    Runs and stacks SQLHistPlot over a group of SQL where compatible statements stored in a dictionary
    """
    if type(gGroups) is not type({}):
        groupBy=gGroups
        gGroups={}
        gList=cur.execute('SELECT '+groupBy+' from Lacuna where Project_Number = "'+str(projectTitle)+'" AND '+cPlotVar+' IS NOT NULL group by '+groupBy+' ORDER BY PROJECT,SAMPLE_AIM_NUMBER').fetchall()
        for cObj in gList: gGroups[cObj[0]]=groupBy+'="'+cObj[0]+'"'
        print gGroups
    keyList=gGroups.keys()
    keyList.sort()
    keyList.reverse()
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd+' '
    for cGroup in keyList:
        # New Function
        
        SQLHistPlotDiv(cPlotVar,cPlotVarY,divElement=divElement,pName=pName,pXlabel=pXlabel,pYlabel=pYlabel,bins=bins,sqlAdd=gGroups[cGroup]+' '+sqlAdd,drawGraph=False,myalph=myalph,xnorm=xnorm,ynorm=ynorm,stdVal=stdVal,imgDPI=imgDPI,useEbar=useEbar,plotEbar=plotEbar,fixVals=fixVals,logx=logx,logy=logy,isInt=isInt)
        
    
    if pXlabel=='': pXlabel=ptName(cPlotVar)
    if pYlabel=='': pYlabel=ptName(cPlotVarY)
    
    if drawGraph:
        title(pName)
        xlabel(pXlabel)
        ylabel(pYlabel)
	if showLegend: legend(keyList)
        savefig('histogram-'+strcsv(pName)+'.pdf',dpi=imgDPI*2)
        savefig('histogram-'+strcsv(pName),dpi=imgDPI)
        close()	

def SQLHist2(cPlotVar,pName='',pXlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,norm=True,imgDPI=72,giveTable=False):
    """SQLHist2(cPlotVar,pName='',pXlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,norm=True,imgDPI=72):
    Uses extended queries instead of interated (much faster in theory)
    """
    if pName=='': pName=ptName(cPlotVar)
    if pXlabel=='': pXlabel=ptName(cPlotVar)
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    (cmin,cmax)=dbExecute('MIN('+cPlotVar+'),MAX('+cPlotVar+')',wherei='Project_Number="'+str(projectTitle)+'" '+sqlAdd)[0]
    crange=cmax-cmin
    cStepSize=crange/(bins+1)
    xBins=[]
    xVals=[]
    
    for cStep in range(1,bins+1):
        cX=cmin+cStepSize*cStep
        xBins+=[cX]
        cStepStr=cPlotVar+' BETWEEN '+str(n(cX-cStepSize/2))+' AND '+str(n(cX+cStepSize/2))
        xVals+=['SUM('+cStepStr+')'] 
    xVals=numpy.array(dbExecute(','.join(xVals),wherei='Project_Number="'+str(projectTitle)+'" '+sqlAdd)[0])
    xBins=numpy.array(xBins)
    if giveTable: return (xBins,xVals)
    #if norm: xVals/=(1.0*numpy.sum(xVals))
    plot(xBins,xVals)
    if drawGraph:
        title(pName)
        xlabel(pXlabel)
        ylabel(cPlotVar)
        savefig('histogram-'+strcsv(pName),dpi=imgDPI)
        close()
def SQLHistPlot2(cPlotVar,cPlotVarY,pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,norm=True,stdVal='',imgDPI=72,useEbar=True,plotEbar=False):
    """SQLHistPlot(cPlotVar,cPlotVarY,pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,norm=True,stdVal='',imgDPI=72,useEbar=True,useR2=True,plotEbar=False)
    cPlotVarY2 does a correlation between the two variables
    cPlotW uses a weighted mean and std 
    """
    
    if pXlabel=='': pXlabel=ptName(cPlotVar)
    if pYlabel=='': pYlabel=ptName(cPlotVarY)
    
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    #(cmin,cmax)=(cur.execute('select MIN('+cPlotVar+'),MAX('+cPlotVar+') from Lacuna WHERE Project_Number="'+str(projectTitle)+'" '+sqlAdd)).fetchall()[0]
    
    (cmin,cmax,cmean,cstd)=_GetVarStats(cPlotVar,sqlAdd)

    if stdVal!='':  
        cmin=max(cmin,cmean-cstd*stdVal)
        cmax=min(cmax,cmean+cstd*stdVal)
        
    crange=cmax-cmin
    cStepSize=crange/(bins+1)
    
    fehler=0
    curSQLcnt=lambda rng: 'SUM('+str(rng)+')'

    curSQLavg=lambda rng: 'SUM('+cPlotVarY+'*('+str(rng)+'))'
    curSQLstd=lambda rng: 'SUM('+cPlotVarY+'*'+cPlotVarY+'*('+str(rng)+'))'
    
    if pName=='': pName=pYlabel+' vs '+pXlabel
    
    
    xBins=[]
    xVals=[]
    eVals=[]
    cVals=[]
    
    for cStep in range(1,bins+1):
        cX=cmin+cStepSize*cStep
        cStepStr=cPlotVar+' BETWEEN '+str(n(cX-cStepSize/2))+' AND '+str(n(cX+cStepSize/2))
        xBins+=[cX]
        xVals+=[curSQLavg(cStepStr)]
        eVals+=[curSQLstd(cStepStr)]
        cVals+=[curSQLcnt(cStepStr)]
        
    xVals=dbExecute(','.join(xVals),wherei='Project_Number="'+str(projectTitle)+'" '+sqlAdd)[0]
    eVals=dbExecute(','.join(eVals),wherei='Project_Number="'+str(projectTitle)+'" '+sqlAdd)[0]
    cVals=dbExecute(','.join(cVals),wherei='Project_Number="'+str(projectTitle)+'" '+sqlAdd)[0]
    
    xBins=numpy.array(xBins)
    xVals=numpy.array(xVals)
    eVals=numpy.array(eVals)
    cVals=numpy.array(cVals)
    # Remove Empty Points
    xBins=xBins[cVals>0]
    xVals=xVals[cVals>0]
    eVals=eVals[cVals>0]
    cVals=cVals[cVals>0]
    
    xVals/=cVals.astype('double')
    eVals=sqrt(eVals/cVals.astype('double')-xVals**2)
    
    if useEbar: errorbar(xBins,xVals,yerr=eVals)
    elif plotEbar: plot(xBins,eVals) 
    else: plot(xBins,xVals,linewidth=3)

    if drawGraph:
        title(pName)
        xlabel(pXlabel)
        ylabel(pYlabel)
        savefig('histogram-'+strcsv(pName),dpi=imgDPI)
        close()
def corrTable(fields,projectName='',sqlAdd='',mode=0,maxFetch=20,drawTable=True):
    if (sqlAdd!='') & (projectName!='') : sqlAdd+=' AND '
    if projectName!='': sqlAdd+=' Project_Number = "'+projectName+'" '
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd+' '
    if mode==0: groupBy='Project'
    if mode==1: groupBy='SAMPLE_AIM_NUMBER'
    if mode==2: groupBy='Canal_Number'
    genFields=[]
    headFields=['Sample Name','Lacuna Count','Canal Count']	
    for cField in fields:
    	genFields+=['AVG('+cField+')','STD('+cField+')']
        cName=cField
        if fullFieldNames.has_key(cField.upper()):
            cName=fullFieldNames[cField.upper()]
            cName=cField
        headFields+=['Avg.'+cName]
        headFields+=['Std.'+cName]
    genFields+=[groupBy,'COUNT(LACUNA_NUMBER)','COUNT(CANAL_NUMBER)']	
    nTable=dbExecute(','.join(genFields),wherei='Lacuna_Number>0 '+sqlAdd,groupbyi=groupBy,sortopt='order by Count(Lacuna_Number) DESC',records=maxFetch)
    oData=[]
    cData={}
    oDict={}
    for row in nTable:
        
        cRow=list(row)
        canCnt=cRow.pop()
        lacCnt=cRow.pop()
        sampleName=cRow.pop()
        cData[sampleName]=[]
        oDict[sampleName]={}
        oRow=[sampleName,lacCnt,canCnt]
        for cEle in range(0,len(cRow)/2):
            cMean=cRow[2*cEle]
            cStd=cRow[2*cEle+1]
            oRow+=[(cMean)]       
            oRow+=[cStd]
        oData+=[oRow]
    
	    
   
    if drawTable: htmlTable(headFields,oData)
    
    
    def tempColorFunc(cellVal):
	try:
	    if (abs(float(cellVal))>0.2) & (abs(float(cellVal))<.99): return True
	except:
	    return False
	return False
    for sampleName in cData.keys():
        headFields=[sampleName+' Correlation Table']
        for cFieldA in fields:
            cNameA=ptName(cFieldA)
            headFields+=[cNameA]
            genFields=[]
            for cFieldB in fields:
                cNameB=ptName(cFieldB)
        	genFields+=['CORR('+cFieldA+','+cFieldB+')'] # use var - 
            nRow=(cur.execute('select '+','.join(genFields)+' from Lacuna WHERE '+groupBy+'="'+sampleName+'" '+sqlAdd+' group by '+groupBy)).fetchall()[0]
            #for nRow in lRow:
            nRow=list(nRow)
            #cGroup=nRow.pop()
            nRow.insert(0,cNameA)
            cData[sampleName]+=[nRow]
	   
        if drawTable: htmlTable(headFields,cData[sampleName],colorFunc=tempColorFunc)
    if not drawTable: return cData
        
        
def corrTable2(fields,projectName='',sqlAdd='',mode=0,maxFetch=20):
    if (sqlAdd!='') & (projectName!='') : sqlAdd+=' AND '
    if projectName!='': sqlAdd+=' Project_Number = "'+projectName+'" '
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd+' '
    if mode==0: groupBy='Project'
    if mode==1: groupBy='SAMPLE_AIM_NUMBER'
    if mode==2: groupBy='Canal_Number'
    genFields=[]
    cData=[]
    #for sampleName in fields:
    headFields=[' Correlation Table']
    for cFieldA in fields:
        cNameA=cFieldA
        if fullFieldNames.has_key(cFieldA.upper()):
            cNameA=fullFieldNames[cFieldA.upper()]
            #cNameA=cFieldA
        headFields+=[cNameA]
        genFields=[]
        for cFieldB in fields:
            cNameB=cFieldB
            if fullFieldNames.has_key(cFieldB.upper()):
                cNameB=fullFieldNames[cFieldB.upper()]
                #cNameB=cFieldB
    	    genFields+=['CR['+cFieldA+';'+cFieldB+']']
        nRow=dbExecute(','.join(genFields),wherei=sqlAdd,groupbyi=groupBy,records=1)[0]
        
        nRow=list(nRow)
        
        nRow.insert(0,cNameA)
        #if (cData.has_key(sampleName)==False): cData[sampleName]=[]
        #cData[sampleName]+=[nRow]
        cData+=[nRow]
    htmlTable(headFields,cData)

import numpy
# Region of Interest (Exclude Borders)
roiCutFact=0.2
def getBounds(cBone=''):
    if cBone!='': cBone=' AND SAMPLE_AIM_NUMBER="'+cBone+'"'
    return numpy.array((cur.execute('select Min(Pos_X),Max(Pos_X),Min(Pos_Y),Max(Pos_Y),Min(Pos_Z),Max(Pos_Z),AVG(Canal_Distance_Mean) from Lacuna where Project_Number = "'+str(projectTitle)+'"'+cBone)).fetchall()[0])
def generateROI(radCutFact='',roiG='',useMask=False):
    if radCutFact=='': radCutFact=roiCutFact    
    if (roiG==''):
        roiG=getBounds()
        roiG[0]+=radCutFact*roiG[6]
        roiG[1]+=-radCutFact*roiG[6]
        roiG[2]+=radCutFact*roiG[6]
        roiG[3]+=-radCutFact*roiG[6]
        roiG[4]+radCutFact*roiG[6]
        roiG[5]+=-radCutFact*roiG[6]
        endPrint=1 # Print distances and return roiGrid
    else:
        endPrint=2 # return number of lacuna
    if useMask:
        inRoiSuf=      'Mask_Distance_Mean>'+str(radCutFact*roiG[6])
        outRoiSuf=      'Mask_Distance_Mean<'+str(radCutFact*roiG[6])
    else:
        inRoiSuf=''
        inRoiSuf+=     'Pos_X>'+str(roiG[0])
        inRoiSuf+=' AND Pos_X<'+str(roiG[1])
        inRoiSuf+=' AND Pos_Y>'+str(roiG[2])    
        inRoiSuf+=' AND Pos_Y<'+str(roiG[3])    
        inRoiSuf+=' AND Pos_Z>'+str(roiG[4])    
        inRoiSuf+=' AND Pos_Z<'+str(roiG[5])
    
        outRoiSuf=''
        outRoiSuf+=     '(Pos_X<'+str(roiG[0])
        outRoiSuf+=' OR Pos_X>'+str(roiG[1])
        outRoiSuf+=' OR Pos_Y<'+str(roiG[2])
        outRoiSuf+=' OR Pos_Y>'+str(roiG[3])
        outRoiSuf+=' OR Pos_Z<'+str(roiG[4])
        outRoiSuf+=' OR Pos_Z>'+str(roiG[5])+')'
    
    if endPrint==1:
        print 'Cut-off Distance : '+str(radCutFact*roiG[6])+' mm'
        print 'Inside ROI:'+'\n'.join(['\t'.join([strd(sobj) for sobj in obj]) for obj in (cur.execute('select Project,COUNT(SAMPLE_AIM_NUMBER) from Lacuna WHERE '+inRoiSuf+' AND Project_Number = "'+str(projectTitle)+'" group by Project')).fetchall()])
        print 'Outside ROI:'+'\n'.join(['\t'.join([strd(sobj) for sobj in obj]) for obj in (cur.execute('select Project,COUNT(SAMPLE_AIM_NUMBER) from Lacuna WHERE '+outRoiSuf+' AND Project_Number = "'+str(projectTitle)+'" group by Project')).fetchall()])
        globals()['inRoiSuf']=inRoiSuf
        globals()['outRoiSuf']=outRoiSuf
        return roiG
    else:
        return inRoiSuf

## Code from Canal Classification
from pylab import *
import numpy
import matplotlib.pyplot as plt

def randName():
    jadebet='kevinmaderwrotethisshit'
    ostr=''
    cList=random(8)*len(jadebet)
    for i in cList: ostr+=jadebet[int(i)]
    return ostr
def SummaryPlotCan(cPlotVar,pName='',pXlabel='',bins=20,sqlAdd='',numCanals=5,canalNums='',drawGraph=True,myalph=.80,maxRadius='',norm=True,cBone=''):
    if pName=='': pName=cPlotVar
    if pXlabel=='': pXlabel=cPlotVar
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    if cBone!='': cBone=' AND SAMPLE_AIM_NUMBER LIKE "'+cBone+'" '
    if maxRadius=='' : maxRadius=600
    if canalNums=='' : 
        canalNums=[can[0] for can in (cur.execute('select ROUND(CANAL_NUMBER) from Lacuna where Canal_Distance_Mean*1000<5000 '+cBone+' group by Canal_Number order by Count(Lacuna_Number) DESC').fetchmany(numCanals))]
        #print canalNums
    lastalph=1
    
    for curCanal in canalNums:
        results=(cur.execute('select '+cPlotVar+' from Lacuna WHERE Project_Number="'+str(projectTitle)+'"  AND Round(Canal_Number) = '+str(curCanal)+' AND Canal_Distance_Mean*1000<'+str(maxRadius)+sqlAdd+cBone)).fetchall()
        results=[obj[0] for obj in results if type(obj[0]) is type(pi)]
        hist(numpy.array(results),bins,label=pName+'Can-'+str(curCanal)+', T.Lac-'+str(len(results)),histtype='bar',log=False,alpha=lastalph,normed=norm)
        lastalph*=myalph
    if drawGraph:
        title(pName)
        xlabel(pXlabel)
        legend()
        savefig('histogram-'+pName,dpi=72)
        close()
import numpy
from pylab import *
def __histget__(ptMin,cols):
    out=[]
    ptMat={}
    for pt in ptMin: ptMat[pt[0]]=pt[1]
    for col in cols:
        if ptMat.has_key(col):
            out+=[ptMat[col]]
        else:
            #print str(col)+' '+str(ptMat.keys())
            out+=[0]
    return out
def SQLHistC(cPlotVar,pName='',pXlabel='',bins=5,sqlAdd='',drawGraph=True,myalph=1,norm=True,maxRadius=65,minRadius=5,numCanals=5,canalNums='',stdVal='',localBone='',showLegend=True,cBone='',dpi=72,minLacun=5):
    if cBone!='': cBone=' AND SAMPLE_AIM_NUMBER LIKE "'+cBone+'" '
    if pName=='': pName=cPlotVar
    if pXlabel=='': pXlabel=cPlotVar
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    if localBone!='': localBone=cBone
    if canalNums=='' : 
        canalNums=[can for (can,lacCnt) in (cur.execute('select Canal_Number,Count(Lacuna_Number) from Lacuna where Project_Number="'+str(projectTitle)+'" AND Canal_Distance_Mean*1000 BETWEEN '+str(minRadius)+' AND '+str(maxRadius)+' '+cBone+sqlAdd+' group by Canal_Number order by Count(Lacuna_Number) DESC').fetchmany(numCanals)) if lacCnt>minLacun]
        

    (cmin,cmax,cmean,cstd)=_GetVarStats(cPlotVar,sqlAdd+localBone)
    if stdVal!='':
        cmin=max(cmin,cmean-cstd)
        cmax=min(cmax,cmean+cstd)
    crange=cmax-cmin
    cStepSize=crange/(bins+1)
    xBins=[]
    xVals=[]
    curFig=plt.figure()
    for cStep in range(1,bins+1):
        cX=cmin+cStepSize*cStep
        xBins+=[cX]
        cStepStr='AND '+cPlotVar+' BETWEEN '+str(cX-cStepSize/2)+' AND '+str(cX+cStepSize/2)
        ptMat=(cur.execute('select Canal_Number,COUNT(Lacuna_Number) from Lacuna WHERE Project_Number="'+str(projectTitle)+'" '+cStepStr+sqlAdd+' AND Canal_Distance_Mean*1000 BETWEEN '+str(minRadius)+' AND '+str(maxRadius)+cBone+' group by Canal_Number order by Count(Lacuna_Number) DESC')).fetchall()
        
        xVals+=[__histget__(ptMat,canalNums)]
    xBins=numpy.array(xBins)
    xVals=numpy.array(xVals,dtype=float)
    #print xVals
    if norm: 
        for i in range(0,xVals.shape[1]): xVals[:,i]/=sum(xVals[:,i])+0.0
    plot(xBins,xVals)
    if drawGraph:
        title(pName)
        xlabel(pXlabel)
        if norm:
            ylabel('Counts')
        else:
            ylabel('Probability')
        if showLegend: 
            legend(tuple(['Canal '+str((curCan)) for curCan in canalNums]))
            plt.setp(curFig,'size_inches',[15,6])
        savefig('histogram-'+randName()+'.png',dpi=dpi)
        close()
        
def MatchCanalsCorr(pVar1,pVar2,value,field='Canal_Number',cBone='',minCount=30,sqlAdd='',minRad=0,maxRad=75,useR2=False):
    if cBone!='':cBone=' AND Sample_AIM_Number = "'+cBone+'" '
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    corrStr='CORR('+pVar1+','+pVar2+')'
    
    if useR2: corrStr='SQ('+corrStr+')*100'
    
    sqlString='select '+field+',COUNT(Lacuna_Number),'+corrStr+' from Lacuna where Canal_Distance_Mean*1000 BETWEEN '+str(minRad)+' AND '+str(maxRad)+' AND Project_Number="'+str(projectTitle)+'" '+cBone+sqlAdd+' group by Canal_Number order by Count(Lacuna_Number) DESC '
    print sqlString
    results=(cur.execute(sqlString).fetchall())
    gt=[]
    lt=[]
    tk=[] 
    
    for (cnum,clac,corval) in results:
        if not ((corval is None)):
            if (clac>minCount):
                if corval>=value: 
                    gt+=[cnum];
                else: 
                    lt+=[cnum]
        else:
            print 'None Error--'+str((cnum,clac,corval))
    return (gt,lt)

def canalGrpStr(grpList,isLac=True):
    if isLac:
        curStr=' OR '.join([' (Canal_Number="'+str(cEle)+'") ' for cEle in grpList])
        return '('+curStr+')'
    else:
        def parseCanName(cStr):
            cList=cEle.split('_CAN_')
            cName=cList[0]
            cNameL=cName.split('_')
            cName='_'.join(cNameL[1:])
            cNum=cList[1]
            
            return (cName,cNum)
        temList=[parseCanName(cEle) for cEle in grpList]
        curList=[' ((SAMPLE_AIM_NUMBER LIKE "%'+str(cName)+'%") AND (ABS(CANAL_NUMBER)-'+str(cNum)+'<0.5)) ' for (cName,cNum) in temList]
        curStr=' OR '.join(curList)
        return '('+curStr+')'
    
def mccHist(pVar1,pVar2,value,graphVar,cBone='',minCount=30,showLegend=True,bins=15,sqlAdd='', norm=True,drawGraph=True,stdVal=2,localBone='',pName='',pXlabel='',dpi=72,minRad=0,maxRad=75,useR2=True):
    (gt,lt)=MatchCanalsCorr(pVar1,pVar2,value,cBone=cBone,minCount=minCount,field='Canal_Number',minRad=minRad,maxRad=maxRad,useR2=useR2)
    
    groups=[];
    cLegend=[];
    if len(gt)>0:
        groups+=[canalGrpStr(gt)]
        cLegend+=['Overvalue : '+str(len(gt))]
    if len(lt)>0:
        groups+=[canalGrpStr(lt)]
        cLegend+=['Undervalue : '+str(len(lt))]
    
    
    
    
    if len(groups)>0:
        if cBone!='':cBone=' AND Sample_AIM_Number = "'+cBone+'" '
        if pName=='': pName=graphVar
        if pXlabel=='': pXlabel=graphVar
        if sqlAdd!='': sqlAdd=' AND '+sqlAdd
        (cmin,cmax,cmean,cstd)=_GetVarStats(graphVar,sqlAdd+localBone)
        print 'Std:'+str(cstd/cmean*100)
        cmin=max(cmin,cmean-cstd)
        cmax=min(cmax,cmean+cstd)
        crange=cmax-cmin
        cStepSize=crange/(bins+1)
        xBins=[]
        xVals=[]
        curFig=plt.figure()
        for cStep in range(1,bins+1):
            cX=cmin+cStepSize*cStep
            xBins+=[cX]
            cStepStr='AND '+graphVar+' BETWEEN '+str(cX-cStepSize/2)+' AND '+str(cX+cStepSize/2)
            outrow=[]
            for cGroupStr in groups:
                sqlText='select COUNT(Lacuna_Number) from Lacuna WHERE Project_Number="'+str(projectTitle)+'" '+cStepStr+' AND '+cGroupStr+' '+' AND Canal_Distance_Mean*1000 BETWEEN '+str(minRad)+' AND '+str(maxRad)+cBone+' group by Sample_AIM_Number'
             
                ptMat=(cur.execute(sqlText)).fetchall()
                try:
                    zeCount=ptMat[0][0]
                except:
                    zeCount=0
                outrow+=[zeCount]
            xVals+=[outrow]
        xBins=numpy.array(xBins)
        xVals=numpy.array(xVals,dtype=float)
        #print xVals
        if norm: 
            for i in range(0,xVals.shape[1]): xVals[:,i]/=sum(xVals[:,i])+0.0
        plot(xBins,xVals)
        if drawGraph:
            title(pName)
            xlabel(pXlabel)
            if norm:
                ylabel('Probability')
            else:
                ylabel('Counts')
            if showLegend: 
                legend(tuple(cLegend))
                plt.setp(curFig,'size_inches',[15,6])
            savefig('histogram-'+randName()+'.png',dpi=dpi)
            close()       
    
    
def MatchCanals(parameter,value,stdLimit=-1,operator='AVG',field='Canal_Number',cBone='',minCount=30,sqlAdd='',secVar='Volume*1000*1000*1000'):
    if cBone!='':cBone=' AND Sample_AIM_Number = "'+cBone+'" '
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    secVar=',STD('+secVar+'),AVG('+secVar+')'
    sqlString='select '+field+',COUNT(Lacuna_Number),STD('+parameter+'),'+operator+'('+parameter+') '+secVar+' from Lacuna where Canal_Distance_Mean*1000<75 AND Project_Number="'+str(projectTitle)+'" '+cBone+sqlAdd+' group by Canal_Number order by Count(Lacuna_Number) DESC '
    print sqlString
    results=(cur.execute(sqlString).fetchall())
    gt=[]
    lt=[]
    tk=[] 
    
    fgtStd=0.0
    fgtMean=0.0
    fgtCnum=0
    
    fltStd=0.0
    fltMean=0.0
    fltCnum=0
    
    sfgtStd=0.0
    sfgtMean=0.0
    sfgtCnum=0
    
    sfltStd=0.0
    sfltMean=0.0
    sfltCnum=0
    for (cnum,clac,cstd,aval,ssval,saval) in results:
        #sval was squared val
        if not ((aval is None) or (saval is None)):
            stdpct=cstd/abs(aval)*100 # stdPercent
            stdtest=((stdpct<stdLimit) or (stdLimit<0))
            if ((clac>minCount) and  stdtest):
                if aval>=value: 
                    gt+=[cnum];
                    fgtMean+=aval*clac
                    fgtStd+=cstd*clac
                    fgtCnum+=clac
                    sfgtMean+=saval*clac
                    sfgtStd+=ssval*clac
                else: 
                    lt+=[cnum]
                    fltMean+=aval*clac
                    fltStd+=cstd*clac
                    sfltMean+=saval*clac
                    sfltStd+=ssval*clac
                    fltCnum+=clac
            else:
                tk+=[cnum]
        else:
            print 'None Error--'+str((cnum,clac,cstd,aval,ssval,saval))
    try:
        gtMean=fgtMean/(fgtCnum+0.0)
        gtStd=sqrt(fgtStd/fgtCnum)
        
        ltMean=fltMean/(fltCnum+0.0)
        ltStd=sqrt(fltStd/fltCnum)
        
        sgtMean=sfgtMean/(fgtCnum+0.0)
        sgtStd=sqrt(sfgtStd/fgtCnum)
        
        sltMean=sfltMean/(fltCnum+0.0)
        sltStd=sqrt(sfltStd/fltCnum)
        
        print 'Over Group : Mean '+str(gtMean)+', '+str(gtStd)
        print 'Under Group : Mean '+str(ltMean)+', '+str(ltStd)
        print 'SV - Over Group : Mean '+str(sgtMean)+', '+str(sgtStd)
        print 'SV - Under Group : Mean '+str(sltMean)+', '+str(sltStd)
    except:
    	print 'No Values...'
    return (gt,lt,tk)
    
    

    
def packstr(listIn):
    outStr=''
    for ele in listIn:
        outStr+=str(int(ele)).strip()
        outStr+=','
    return outStr[:-1]
def mcHist(parameter,value,operator='AVG',cBone='',minCount=30,showLegend=True,bins=15,sqlAdd='', norm=True,drawGraph=True,stdVal=2,localBone='',pName='',pXlabel='',dpi=72,stdLimit=-1,graphVar=''):
    if graphVar=='': graphVar=parameter
    (gt,lt,tk)=MatchCanals(parameter,value,operator=operator,cBone=cBone,minCount=minCount,field='Canal_Number',stdLimit=stdLimit,secVar=graphVar)
    if graphVar!='': parameter=graphVar
    groups=[];
    cLegend=[];
    tk=[]
    if len(gt)>0:
        groups+=[canalGrpStr(gt)]
        cLegend+=['Overvalue : '+str(len(gt))]
    if len(lt)>0:
        groups+=[canalGrpStr(lt)]
        cLegend+=['Undervalue : '+str(len(lt))]
    if len(tk)>0:
        groups+=[canalGrpStr(tk)]
        cLegend+=['Too Small : '+str(len(tk))]
    
    
    
    if len(groups)>0:
        cPlotVar=parameter
        if cBone!='':cBone=' AND Sample_AIM_Number = "'+cBone+'" '
        if pName=='': pName=cPlotVar
        if pXlabel=='': pXlabel=cPlotVar
        if sqlAdd!='': sqlAdd=' AND '+sqlAdd
        (cmin,cmax,cmean,cstd)=(cur.execute('select MIN('+cPlotVar+'),MAX('+cPlotVar+'),AVG('+cPlotVar+'),STD('+cPlotVar+') from Lacuna WHERE Project_Number="'+str(projectTitle)+'" '+sqlAdd+localBone)).fetchall()[0]
        print 'Std:'+str(cstd/cmean*100)
        cmin=max(cmin,cmean-cstd)
        cmax=min(cmax,cmean+cstd)
        crange=cmax-cmin
        cStepSize=crange/(bins+1)
        xBins=[]
        xVals=[]
        minRadius=0
        maxRadius=200
        curFig=plt.figure()
        for cStep in range(1,bins+1):
            cX=cmin+cStepSize*cStep
            xBins+=[cX]
            cStepStr='AND '+cPlotVar+' BETWEEN '+str(cX-cStepSize/2)+' AND '+str(cX+cStepSize/2)
            outrow=[]
            for cGroupStr in groups:
                sqlText='select COUNT(Lacuna_Number) from Lacuna WHERE Project_Number="'+str(projectTitle)+'" '+cStepStr+' AND '+cGroupStr+' '+' AND Canal_Distance_Mean*1000 BETWEEN '+str(minRadius)+' AND '+str(maxRadius)+cBone+' group by Sample_AIM_Number'
             
                ptMat=(cur.execute(sqlText)).fetchall()
                try:
                    zeCount=ptMat[0][0]
                except:
                    zeCount=0
                outrow+=[zeCount]
            xVals+=[outrow]
        xBins=numpy.array(xBins)
        xVals=numpy.array(xVals,dtype=float)
        #print xVals
        if norm: 
            for i in range(0,xVals.shape[1]): xVals[:,i]/=sum(xVals[:,i])+0.0
        plot(xBins,xVals)
        if drawGraph:
            title(pName)
            xlabel(pXlabel)
            if norm:
                ylabel('Probability')
            else:
                ylabel('Counts')
            if showLegend: 
                legend(tuple(cLegend))
                plt.setp(curFig,'size_inches',[15,6])
            savefig('histogram-'+randName()+'.png',dpi=dpi)
            close()       
def CollectCanalStr(parameter,value,operator='AVG',cBone='',minCount=5,sqlAdd='',sfx='mgen'):
    (gt,lt,tk)=MatchCanals(parameter,value,operator=operator,cBone=cBone,minCount=minCount,sqlAdd=sqlAdd)
    gtStr=''
    ltStr=''
    tkStr=''
    
    if len(gt)>0: gtStr=packstr(gt)+' 110 '
    if len(lt)>0: ltStr=packstr(lt)+' 90 '
    if len(tk)>0: tkStr=packstr(tk)+' 1 '
    return 'xjava "CollectCanals" '+cBone.lower()+'_canlb.aim '+cBone.lower()+'_'+sfx+'.aim '+gtStr+ltStr+tkStr


def htmlProjectView():
	htmlTable(['Sample_Name_Number','Lacuna','Avg.DC','AvgAng','AvgVol','L/W','W/H','Length','Shell Thickness','Absorption'],(cur.execute('select PJ.Project_Name,UNICNT(Sample_AIM_Number),AVG(CANAL_DISTANCE_MEAN)*1000,AVG(CANAL_ANGLE),AVG(Volume*1000*1000*1000),AVG(PROJ_PCA1/PROJ_PCA2),AVG(PROJ_PCA2/PROJ_PCA3),AVG(PROJ_PCA1*1000),AVG(VOLUME_Layer)/AVG(VOLUME)*100,AVG(Shell_Absorption*1000) from Lacuna LC, Project PJ  WHERE PJ.Project_Number=LC.Project_Number group by LC.Project_Number')).fetchall())
	globals()['projectList']=[obj[0] for obj in (cur.execute('select PJ.Project_Name from Lacuna LC, Project PJ  WHERE PJ.Project_Number=LC.Project_Number group by LC.Project_Number')).fetchall()]; print globals()['projectList']
def htmlSampleView():
	htmlTable(['Sample','Lacuna (#)','$kLac/mm^3$','$\mu m^3/Lac$','Canals','Avg.Can.Dist ($\mu m$)','Avg.Ang','Avg.V ($\mu m^3$)','PCA Box.Vol ($\mu m^3$)','Boxiness (%)','Avg. Box SA ($\mu m^2$)','Lac.V/CT.V (%)','Length ($\mu m$)','Width ($\mu m$)','Height ($\mu m$)'],(cur.execute('select SA.Sample_AIM_NAME,COUNT(LC.Sample_Aim_NUMBER),AVG(DENSITY/1000),AVG(DENSITY_Volume*1000*1000*1000),MAX(CANAL_NUMBER),AVG(CANAL_DISTANCE_MEAN)*1000,AVG(CANAL_ANGLE),AVG(Volume*1000*1000*1000),AVG(1000*1000*1000*PROJ_PCA1*PROJ_PCA2*PROJ_PCA3),AVG(100*Volume/(PROJ_PCA1*PROJ_PCA2*PROJ_PCA3)),AVG(1000*1000*(2*PROJ_PCA1*PROJ_PCA2+2*PROJ_PCA1*PROJ_PCA3+2*PROJ_PCA2*PROJ_PCA3)),Sum(Volume)/Sum(DENSITY_Volume)*100,AVG(PROJ_PCA1*1000),AVG(PROJ_PCA2*1000),AVG(PROJ_PCA3*1000), AVG(OBJ_RADIUS*1000), AVG(Volume_Layer*1000*1000*1000) from Lacuna LC, Sample SA where LC.Project_Number=SA.Project_Number AND LC.Sample_AIM_Number=SA.Sample_AIM_Number AND SA.Project_Number= '+str(getProjectNumber(projectTitle))+' group by LC.Sample_AIM_Number')).fetchall())
	globals()['bones']=[obj[0] for obj in (cur.execute('select SA.Sample_AIM_NAME,SA.SAMPLE_AIM_NUMBER from Lacuna LC, Sample SA WHERE LC.Project_Number=SA.Project_Number AND LC.Sample_AIM_Number=SA.Sample_Aim_Number AND SA.Project_Number = '+str(getProjectNumber(projectTitle))+' group by LC.Sample_AIM_NUMBER')).fetchall()]; print bones
	globals()['boneGroups']={}
	for cBone in bones:
		globals()['boneGroups'][cBone]='SAMPLE_AIM_NUMBER = "'+cBone[1]+'"'

def htmlSampleViewCortical(centered=False):
	if centered: htmlTable(['Sample','Ct.$\mu_x$','Ct.$\mu_y$','Ct.$\mu_z$','Ct.$\theta_{XY}$','Ct.$\theta_{XZ}$','Ct.$\theta_{YZ}$','Ct.Th E1 ($\mu m$)','Ct.Th E2($\mu m$)','Ct.Th E3($\mu m$)'],(cur.execute('select Sample_AIM_Number,WAVG(POS_CX*1000,DENSITY_VOLUME),WAVG(POS_CY*1000,DENSITY_VOLUME),WAVG(POS_CZ*1000,DENSITY_VOLUME),ATAN(WLINFIT(POS_CX,POS_CY,DENSITY_VOLUME)),ATAN(WLINFIT(POS_CZ,POS_CX,DENSITY_VOLUME)),ATAN(WLINFIT(POS_CZ,POS_CY,DENSITY_VOLUME)),2*1000*WSTD(POS_RADIUS,DENSITY_VOLUME),1000*SUM(DENSITY_VOLUME)/(6.28*WAVG(POS_RADIUS,DENSITY_VOLUME)*(MAX(POS_Z)-MIN(POS_Z))),1000*AVG(MASK_RADIUS_MAX-MASK_RADIUS_MIN) from Lacuna where SAMPLE_AIM_NUMBER>0 group by Sample_AIM_Number')).fetchall())
	else: htmlTable(['Sample','Ct.$\mu_x$','Ct.$\mu_y$','Ct.$\mu_z$','Ct.$\theta_{XY}$','Ct.$\theta_{XZ}$','Ct.$\theta_{YZ}$','Ct.Th E1 ($\mu m$)','Ct.Th E2($\mu m$)','Ct.Th E3($\mu m$)'],(cur.execute('select Sample_AIM_Number,WAVG(POS_X*1000,DENSITY_VOLUME),WAVG(POS_Y*1000,DENSITY_VOLUME),WAVG(POS_Z*1000,DENSITY_VOLUME),ATAN(WLINFIT(POS_X,POS_Y,DENSITY_VOLUME)),ATAN(WLINFIT(POS_Z,POS_X,DENSITY_VOLUME)),ATAN(WLINFIT(POS_Z,POS_Y,DENSITY_VOLUME)),2*1000*WSTD(POS_RADIUS,DENSITY_VOLUME),1000*SUM(DENSITY_VOLUME)/(6.28*WAVG(POS_RADIUS,DENSITY_VOLUME)*(MAX(POS_Z)-MIN(POS_Z))),1000*AVG(MASK_RADIUS_MAX-MASK_RADIUS_MIN) from Lacuna where SAMPLE_AIM_NUMBER>0 group by Sample_AIM_Number')).fetchall())

def BoneReportTable(gGroups,sqlAdd='',sqlAddCan='',countField='',groupField='',dispVars=False):
    fullParmList=[]
    canParmList=[]
    fullParmList+=[('Lacuna Position',['MASK_DISTANCE_MEAN*1000','CANAL_DISTANCE_MEAN*1000'])]
    fullParmList+=[('Lacuna Shape',['PROJ_PCA1*1000','VOLUME*1000*1000*1000','PROJ_PCA1/(PROJ_PCA2+PROJ_PCA3)*2','2*(PCA2_S-PCA3_S)/(PCA1_S-PCA3_S)-1','OBJ_RADIUS*1000'])]
    canParmList+=[('Canal Shape',['THICKNESS*1000','VOLUME*/(3.1415*THICKNESS*THICKNESS)*1000','VOLUME*1000*1000*1000','2*(PCA2_S-PCA3_S)/(PCA1_S-PCA3_S)-1','COUNT(*)','&SUM(VOLUME)/SUM(DENSITY_VOLUME)*100','1/AVG(DENSITY_VOLUME)'])]
    fullParmList+=[('Orientation',['PCA1_PHI','CANAL_ANGLE','&TEXTURE(PCA1_X,PCA1_Y,PCA1_Z)','&TEXTURE(PCA2_X,PCA2_Y,PCA2_Z)'])]
    dispVarList=[]
    if dispVars: dispVarList=['DISPLACEMENT_MEAN*1000',strNDispLac]
    if groupField=='': 
	   densVariable='DENSITY/1000'
	   packFact='NEAREST_NEIGHBOR_DISTANCE/POW(DENSITY_VOLUME,0.33333)'
    else: 
	   densVariable='&1/AVG(DENSITY_VOLUME)/1000'
	   packFact='&AVG(NEAREST_NEIGHBOR_DISTANCE)/AVG(POW(DENSITY_VOLUME,0.33333))'
    fullParmList+=[('Environment',[densVariable,'SHELL_ABSORPTION*1000','SHELL_ABSORPTION_STD*1000','1/(36*3.14)*DENSITY_VOLUME_SHELL*DENSITY_VOLUME_SHELL*DENSITY_VOLUME_SHELL/(DENSITY_VOLUME*DENSITY_VOLUME)-1','NEAREST_NEIGHBOR_NEIGHBORS',packFact]+dispVarList)]
    outStr=r"\begin{tabular}{|c|}"
    for (cLabel,cVars) in canParmList:
           outStr+=r"\hline "
           outStr+=r"{\bf "+cLabel+r"}\\ "
           outStr+=r"\hline "
           outStr+=GroupTTestTable(gGroups,cVars,tableFunc=_grtTable,sqlAdd=sqlAddCan,countField=countField,groupField=groupField,nonZero=False,table='Canal')
    outStr+=r" \hline "
    for (cLabel,cVars) in fullParmList:
           outStr+=r"\hline "
           outStr+=r"{\bf "+cLabel+r"}\\ "
           outStr+=r"\hline "
           outStr+=GroupTTestTable(gGroups,cVars,tableFunc=_grtTable,sqlAdd=sqlAdd,countField=countField,groupField=groupField,nonZero=False)
    outStr+=r" \hline "
    
    outStr+=r"\end{tabular}"
    return outStr
    
def GroupReportTable(gGroups,sqlAdd='',countField='',groupField='',dispVars=False,inLineP=False,tTests=None):
    fullParmList=[]
    fullParmList+=[('Shape',['PCA1_S*1000','PCA2_S*1000','PCA3_S*1000','VOLUME*1000*1000*1000','2*(PCA2_S-PCA3_S)/(PCA1_S-PCA3_S)-1'])]
    
    
    dispVarList=[]
    if dispVars: dispVarList=['DISPLACEMENT_MEAN*1000',strNDispLac]
    if groupField=='': 
	   densVariable='DENSITY/1000'
	   packFact='NEAREST_NEIGHBOR_DISTANCE/POW(DENSITY_VOLUME,0.33333)'
	   saVar='NEAREST_NEIGHBOR_DISTANCE/(POW(DENSITY_VOLUME,0.33333))'
    else: 
	   densVariable='&1/AVG(DENSITY_VOLUME)/1000'
	   packFact='&AVG(NEAREST_NEIGHBOR_DISTANCE)/AVG(POW(DENSITY_VOLUME,0.33333))'
	   saVar='&AVG(NEAREST_NEIGHBOR_DISTANCE)/AVG(POW(DENSITY_VOLUME,0.33333))'
    fullParmList+=[('Local Environment',['DENSITY_VOLUME*1000*1000*1000',densVariable,'NEAREST_NEIGHBOR_DISTANCE*1000','NEAREST_NEIGHBOR_NEIGHBORS',saVar]+dispVarList)]
    fullParmList+=[('Position',['CANAL_DISTANCE_MEAN*1000','MASK_DISTANCE_MEAN*1000'])]
    outStr=r"\begin{tabular}{|c|}"
    for (cLabel,cVars) in fullParmList:
           outStr+=r"\hline "
           outStr+=r"{\bf "+cLabel+r"}\\ "
           outStr+=r"\hline "
           outStr+=GroupTTestTable(gGroups,cVars,tableFunc=_grtTable,sqlAdd=sqlAdd,countField=countField,groupField=groupField,nonZero=False,inLineP=inLineP,tTests=tTests)
    outStr+=r" \hline "
    outStr+=r"\end{tabular}"
    return outStr
def GroupTTestTable(gGroups,cVars,tTests=None,inLineP=False,inLineP1=0.05,inLineP2=0.01,inLineP3=0.001,tableFunc=htmlTable,sqlAdd='',countField='',groupField='',drawError=False,table='Lacuna',nonZero=False,ssigKeys='*&%$#@^!?'):
    """GroupTTestTable(gGroups,cVars,tableFunc=htmlTable,sqlAdd='',countField='')
    GroupTTestTable is a function to make a table of the TTest between 2-groups defined in the gGroup dictionary with SQL where compatible statements
    
    """
    minProb=1e-2
    maxProb=1.1e-2
    
    if nonZero:
        sqlAdd+=' AND '.join([cVar+'>0 ' for cVar in cVars])
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    keyList=gGroups.keys()
    keyList.sort()
    
    headerList=['Variable']
    for cKey in keyList:
        headerList+=[cKey]
    
    if tTests is None: tTests=combinations(range(len(keyList)),2)
    
    if not inLineP: 
        for cTest in tTests: headerList+=['p-Val('+str(cTest)+')']
    dataTable=[]
    errorTable=[]
    for cVar in cVars:
        cSigLetter=-1        
	if cVar[0]=='&': 
            cVar=cVar[1:]
	    curSQLcmd=cVar+',0'
	    gCurSQLcmd=cVar
        else: 	
	    curSQLcmd='AVG('+cVar+'),POW(STD('+cVar+'),2)'
	    gCurSQLcmd='AVG('+cVar+')'
	if (countField==''): curSQLcmd+=',COUNT(*)'
        else: curSQLcmd+=',UNICNT('+countField+')'
        fehler=0
        gBins=[]
        for cGroup in keyList:
            try:
                if groupField is '':
                    results=dbExecute(curSQLcmd,table=table,wherei=gGroups[cGroup]+' '+sqlAdd)[0]
                else:
                    results=dbExecute(gCurSQLcmd,table=table,wherei=gGroups[cGroup]+' '+sqlAdd,groupbyi=groupField)
                    results=[mean(results),var(results),len(results)]
            except:
                results=[None]
            print (results,curSQLcmd)
            if results[0] is not None:
                gBins+=[results]
            else:
                fehler+=1
       
        if fehler==0:
            rawResults=[]
            for cResults in range(len(keyList)):
                cResultTab=gBins[cResults]
                headerList[cResults+1]=keyList[cResults]+' (n='+str(cResultTab[2])+')'
                rawResults+=[strd(cResultTab[0])+'$\pm$'+strd(sqrt(cResultTab[1]))]
                #headerList[cResults]=keyList[cResults]+' (n='+str(cResultTab[2])+')'
            tTestsResults=[]	
            for cTest in tTests:
                pResult=gBins[cTest[0]]
                cResult=gBins[cTest[1]]
                (t,prob)=kttest(cResult[0],cResult[1],cResult[2],pResult[0],pResult[1],pResult[2])
                probStr=str(round(prob,4))
                if prob<minProb: probStr='$<$'+str(round(minProb,4))
                if prob>maxProb: probStr='n.s.'
                if inLineP:
                    
                    cSigLetter+=1
                    cLet=ssigKeys[cSigLetter]
                    if len(cTest)>2: cLet=str(cTest[2])
                    cAddStr=''
                    if prob<inLineP1: 
                        cAddStr+=cLet
                        if prob<inLineP2: cAddStr+=cLet
                        if prob<inLineP3: cAddStr+=cLet
                        rawResults[cTest[0]]+='    '+cAddStr
                        rawResults[cTest[1]]+='    '+cAddStr
                    
                else:    
                    tTestsResults+=[probStr]
            dataTable+=[[cVar]+rawResults+tTestsResults] 
            
        
        else:
            errorTable+=[[ptName(cVar),0,0,'ERROR']]
    return tableFunc(headerList,dataTable)
    if drawError: 
        if len(errorTable)>0: tableFunc(headerList,errorTable)

def _grtTable(header,data):
    """latexTable(header,data)
    """
    
    colHeader='|'+''.join(['p{'+str(2.5)+'cm}|']*len(header))
    
    outStr=r"\begin{tabular}{"+colHeader+"}"
    outStr+=r"\hline\\"
    outStr+=r" & ".join([latexFriendlyName(sobj) for sobj in header])+r" \\"
    outStr+=r"\hline "
    outStr+=r"\hline "
    outStr+=r"\\ \hline ".join([r" & ".join([strd(sobj) for sobj in obj]) for obj in data])+r"\\"
    outStr+=r"\hline "
    outStr+=r"\end{tabular}\\"
    return outStr

def htmlQuickROI(roiD=roiCutFact):        
	allROI=generateROI(roiD,useMask=True)
	# show how many are inside and outside
	htmlTable(['Bone Sample','Lacuna'],(cur.execute('select SA.Sample_Aim_Name,COUNT(LC.Sample_Aim_Number) from Lacuna LC, Sample SA WHERE '+inRoiSuf+' AND LC.Sample_Aim_Number=SA.Sample_Aim_Number AND LC.Project_NUMBER = '+str(projectTitle)+' group by LC.Sample_AIM_Number')).fetchall())

def old_boneCOM(samplename,sqlAdd='',posLabel='POS_'):
    #dbCentPos={}
    #if not sqlAdd=='': dbCentPos.clear()
    #if not dbCentPos.has_key(samplename):
    axVec=['X','Y','Z']
    posStrVec=['SUM('+posLabel+cAx+'*DENSITY_VOLUME)/SUM(DENSITY_VOLUME)' for cAx in axVec]
    posVec=(cur.execute('select '+','.join(posStrVec)+' from Lacuna where Project = "'+str(projectTitle)+'" and Sample_AIM_Number = "'+samplename+'" '+sqlAdd+' group by Sample_AIM_Number')).fetchall()[0]
    posUax=[(axVec[i],posVec[i]) for i in range(len(axVec))]
    
    
    #posStrVec=['SUM(sq('+posLabel+cAx+'-('+str(cPos)+'))*DENSITY_VOLUME)/SUM(DENSITY_VOLUME)' for (cAx,cPos) in posUax]
    #posStrVec=['SUM(('+posLabel+'X-('+str(posVec[0])+'))*('+posLabel+'Y-('+str(posVec[1])+'))*DENSITY_VOLUME)/SUM(DENSITY_VOLUME)','SUM(sq('+posLabel+'X-('+str(posVec[0])+'))*DENSITY_VOLUME)/SUM(DENSITY_VOLUME)']
    
    # Angle Through Weighted Standard Deviation
    #posVec2=(cur.execute('select '+','.join(posStrVec)+' from Lacuna where Project_Number = "'+str(projectTitle)+'" and Sample_AIM_Number = "'+samplename+'" '+sqlAdd+' group by Sample_AIM_Number')).fetchall()[0]
    #offAng=(180/pi*atan2(posVec2[0],posVec2[1]),)
    
    # Angle Through Linear Curve Fitting
    posStrVec=['WLINFIT('+posLabel+'X,'+posLabel+'Y,DENSITY_VOLUME*1000*1000*1000)','WLINFIT('+posLabel+'Z,'+posLabel+'X,DENSITY_VOLUME*1000*1000*1000)','WLINFIT('+posLabel+'Z,'+posLabel+'Y,DENSITY_VOLUME*1000*1000*1000)']
    posVec2=(cur.execute('select '+','.join(posStrVec)+' from Lacuna where Sample_AIM_Number = "'+samplename+'" '+sqlAdd+' group by Sample_AIM_Number')).fetchall()[0]
    offAng=(posVec2[0],posVec2[1],posVec2[2])
    return posVec+offAng
    #return globals()['dbCentPos'][samplename]
    
def old_dbCenterSample(cName='',samplefilter='%',tables=['Canal','Lacuna'],sqlAdd='',isLocal=False):
    if cName=='':
        samples=[obj[0] for obj in (cur.execute('select SAMPLE_AIM_NUMBER from Lacuna WHERE SAMPLE_AIM_NUMBER Like "'+samplefilter+'" '+' group by SAMPLE_AIM_NUMBER')).fetchall()]
    	print 'Running dbCenterSample in Batch mode... (%d) remaining' % len(samples)
	for samplename in samples: dbCenterSample(samplename,tables=tables,sqlAdd=sqlAdd)
	htmlSampleViewCortical()
	return 0
	
    posVec=boneCOM(cName,sqlAdd=sqlAdd)
    if isLocal: posVec=posVec[0:3]+(0,0,0)
    setCmd='COV_X='+str(posVec[0])+',COV_Y='+str(posVec[1])+',COV_Z='+str(posVec[2])+',COV_THETA='+str(-180.0/pi*atan(posVec[3]))+',POS_CX=0,POS_CY=0,POS_CZ=0,POS_RADIUS=-1,POS_DISTANCE=-1,POS_THETA=0,MASK_THETA=0,MASK_RADIUS_MIN=-1,MASK_RADIUS_MAX=-1,MASK_RADIUS_MEAN=-1'
    setCmd+=' WHERE Sample_AIM_Number = '+cName+' '
    for tableName in tables:
        updateCmd='UPDATE '+tableName+' SET '+setCmd
        nout=cur.execute(updateCmd)
	# Center and Unskew the Samples (C) coordinates
	rotC=lambda tx,ty,tz: '(POS_X-COV_X)*(%f)+(POS_Y-COV_Y)*(%f)+(POS_Z-COV_Z)*(%f)' % (tx,ty,tz)
	
	#rotStr='(POS_X-COV_X)-(POS_Z-COV_Z)*('+str(posVec[4])+'),(POS_Y-COV_Y)-(POS_X-COV_X)*('+str(posVec[3])+')-(POS_Z-COV_Z)*('+str(posVec[5])+'),POS_Z-COV_Z'

	txz=-atan(posVec[4])
	tyz=-atan(posVec[5])
	rotStr=','.join([rotC(cos(txz),-sin(txz)*sin(tyz),-sin(txz)*cos(tyz)),rotC(cos(txz),-sin(txz)*sin(tyz),-sin(txz)*cos(tyz)),rotC(cos(txz),-sin(txz)*sin(tyz),-sin(txz)*cos(tyz))])
        selectCmd='SELECT '+rotStr+',ATAN2(POS_Y-COV_Y,POS_X-COV_X),'+tableName+'_Number  FROM '+tableName+' WHERE Sample_AIM_Number = '+cName+' GROUP BY '+tableName+'_Number'
	selectResults=cur.execute(selectCmd).fetchall()
        
        updateCmd='Update Lacuna SET POS_CX=?,POS_CY=?,POS_CZ=?,POS_THETA=? WHERE '+tableName+'_Number=? AND Sample_AIM_Number = "'+cName+'"' 
        nout=cur.executemany(updateCmd,selectResults)
	if isLocal: print cName+' was successfully centered in '+tableName
	else: print cName+' was successfully untilted in '+tableName
	lacDB.commit()
	posVecC=boneCOM(cName,posLabel='POS_C',sqlAdd=sqlAdd)
	txy=-atan(posVecC[3])
	# Correct for the XY Tilt
	rotC=lambda tx,ty,tz: '(POS_CX)*(%f)+(POS_CY)*(%f)+(POS_CZ)*(%f)' % (tx,ty,tz)
	rotCStr=','.join([rotC(cos(txy),-sin(txy),0),rotC(sin(txy),cos(txy),0)])
	
	selectCmd='SELECT '+rotCStr+','+tableName+'_Number  FROM '+tableName+' WHERE Sample_AIM_Number = "'+cName+'" GROUP BY '+tableName+'_Number'
        selectResults=cur.execute(selectCmd).fetchall()
	updateCmd='Update Lacuna SET POS_CX=?,POS_CY=? WHERE '+tableName+'_Number=? AND Sample_AIM_Number = "'+cName+'" ' 
        nout=cur.executemany(updateCmd,selectResults)
	print cName+' was successfully radially tilted '+tableName
	lacDB.commit()
	#Now Record the actual mask_angle
	selectCmd='SELECT SQRT(SQ(POS_CX)+SQ(POS_CY)),SQRT(SQ(POS_Cx)+SQ(POS_CY)+SQ(POS_CZ)),ATAN2(POS_CY,POS_CX),'+tableName+'_Number  FROM '+tableName+' WHERE Sample_AIM_Number = "'+cName+'" GROUP BY '+tableName+'_Number'
        selectResults=cur.execute(selectCmd).fetchall()
        
        updateCmd='Update Lacuna SET POS_RADIUS=?,POS_DISTANCE=?,MASK_THETA=? WHERE '+tableName+'_Number=? AND Sample_AIM_Number = '+cName+''
        nout=cur.executemany(updateCmd,selectResults)
        print cName+' was successfully entered in '+tableName
    
    lacDB.commit()    
def old_wPosFun(xvar,outFun,scalar=5):
    outstr=outFun(xvar)
    if xvar.upper()=='Z': outstr+='*('+str(scalar)+')'
    return outstr 
def old_boneMOM(countText='COUNT(POS_X)',varFun=lambda cAx: 'POS_'+cAx,grpBy='SAMPLE_AIM_NUMBER',sqlAdd=None,weight='DENSITY_VOLUME*1000*1000*1000',tables=['Canal','Lacuna']):
    if sqlAdd is None : sqlAdd=''
    else: sqlAdd=' AND '+sqlAdd
    avgFun=lambda cVar: 'SUM(('+cVar+')*('+weight+'))/SUM('+weight+')'
    
    
    # Center Object 
    posMatrix=[avgFun(varFun(cAx)) for cAx in 'XYZ']
    nposMatrix=['POS_'+cAx+'=('+varFun(cAx)+'-(?))' for cAx in 'XYZ']
    curQuery=cur.execute('SELECT '+','.join(posMatrix+posMatrix)+',SAMPLE_AIM_NUMBER from Lacuna where '+sqlAdd+' group by '+grpBy)
    
    # Clear the other variables
    clearCmd='POS_RADIUS=-1,POS_DISTANCE=-1,POS_THETA=0'
    clearCmd+=',MASK_THETA=0,MASK_RADIUS_MIN=-1,MASK_RADIUS_MAX=-1,MASK_RADIUS_MEAN=-1'
    
    # Execute the Update Command to Center the Samples
    cResults=curQuery.fetchall()
    for tableName in tables:
        execStr='UPDATE '+tableName+' SET COV_X=?,COV_Y=?,COV_Z=?,'+','.join(nposMatrix)+','+clearCmd+' WHERE SAMPLE_AIM_NUMBER=? '
        print execStr
        print cResults
        curUpdate=cur.executemany(execStr,cResults)
    lacDB.commit()    
    
    
    posFun=lambda cAx: '('+varFun(cAx)+'-COV_'+cAx+')'   
    #posFun=lambda cAx: 'POS_C'+cAx
    tposFun=lambda cAx: wPosFun(cAx,posFun,10)
    textMatrix=[avgFun(tposFun(cAx[0])+'*'+tposFun(cAx[1])) for cAx in ['XX','YY','ZZ','XY','XZ','YZ']]
    textQuery=countText+','+','.join(textMatrix)
    print textQuery
    curQuery=cur.execute('SELECT SAMPLE_AIM_NUMBER,'+textQuery+' from Lacuna where Sample_Aim_Number>0 '+sqlAdd+' group by '+grpBy)
    
    outText=curQuery.fetchall()

    mArr=lambda tQ: numpy.array([[tQ[0],tQ[3],tQ[4]],[tQ[3],tQ[1],tQ[5]],[tQ[4],tQ[5],tQ[2]]])
    
    for cOut in outText:
        print cOut
        p=mArr(cOut[1+1:1+8])
        (w,v)=numpy.linalg.eigh(p)
        v*=sign(v[2,2])
        sqlStr=_multDatabaseStr(matrix(v.transpose(1,0)),posFunc=posFun,outPosFunc=lambda x: 'POS_'+x)
        p1sqlStr=_multDatabaseStr(matrix(v.transpose(1,0)),posFunc=lambda x: 'PCA1_'+x,outPosFunc=lambda x: 'PCA1_'+x)
        p2sqlStr=_multDatabaseStr(matrix(v.transpose(1,0)),posFunc=lambda x: 'PCA2_'+x,outPosFunc=lambda x: 'PCA2_'+x)
        p3sqlStr=_multDatabaseStr(matrix(v.transpose(1,0)),posFunc=lambda x: 'PCA3_'+x,outPosFunc=lambda x: 'PCA3_'+x)
        for tableName in tables:
            updateCmd='UPDATE '+tableName+' SET '+','.join([sqlStr,p1sqlStr,p2sqlStr,p3sqlStr])+' WHERE SAMPLE_AIM_NUMBER LIKE "'+cOut[0]+'" '
            nout=cur.execute(updateCmd)
            lacDB.commit()
            
            maStr=['POS_RADIUS=SQRT(SQ(POS_X)+SQ(POS_Y))']
            maStr+=['POS_DISTANCE=SQRT(SQ(POS_x)+SQ(POS_Y)+SQ(POS_Z))']
            maStr+=['MASK_THETA=ATAN2(POS_Y,POS_X)']
            updateCmd='Update '+tableName+' SET '+','.join(maStr)+' WHERE Sample_AIM_Number = "'+cOut[0]+'" '
            nout=cur.execute(updateCmd)
        lacDB.commit()
def old_dbMinRad(samplename='%',steps=18,tables=['Lacuna','Canal']):
    stpRange='FLOOR((MASK_THETA+180)/360*'+str(steps)+')'
    stpGroup='('+stpRange+' || SAMPLE_AIM_NUMBER)'
    tempOut=cur.execute('select MIN(POS_RADIUS),MAX(POS_RADIUS),WAVG(POS_RADIUS,DENSITY_VOLUME),Sample_AIM_Number,'+stpRange+' from Lacuna where Sample_AIM_Number = '+samplename+' group by '+stpGroup).fetchall()
    for cTable in tables: 
        cur.executemany('Update '+cTable+' SET MASK_RADIUS_MIN=?,MASK_RADIUS_MAX=?,MASK_RADIUS_MEAN=? where Sample_AIM_Number = ? AND '+stpRange+'=?',tempOut)
        
    lacDB.commit()
    print 'Finished!'
def old_dbMinRad(samplename='%',steps=18,tables=['Lacuna','Canal']):
    stpRange='FLOOR((MASK_THETA+180)/360*'+str(steps)+')'
    stpGroup='('+stpRange+' || SAMPLE_AIM_NUMBER)'
    tempOut=cur.execute('select MIN(POS_RADIUS),MAX(POS_RADIUS),WAVG(POS_RADIUS,DENSITY_VOLUME),Sample_AIM_Number,'+stpRange+' from Lacuna where Sample_AIM_Number = "'+samplename+'" group by '+stpGroup).fetchall()
    for cTable in tables: 
        cur.executemany('Update '+cTable+' SET MASK_RADIUS_MIN=?,MASK_RADIUS_MAX=?,MASK_RADIUS_MEAN=? where Sample_AIM_Number = ? AND '+stpRange+'=?',tempOut)
        
    lacDB.commit()
    print 'Finished!'
    
    
#auto
import numpy.linalg
from sage.plot.plot_field import *
import matplotlib.quiver as qv
class PlotColorField(GraphicPrimitive):
    """
    Primitive class that initializes the
    PlotColorField graphics type
    """
    def __init__(self, xpos_array, ypos_array, xvec_array, yvec_array,cvec_array, options):
        """
        Create the graphics primitive PlotColorField.  This sets options
        and the array to be plotted as attributes.

        EXAMPLES::

            sage: x,y = var('x,y')
            sage: R=plot_slope_field(x+y,(x,0,1),(y,0,1),plot_points=2)
            sage: r=R[0]
            sage: r.options()['headlength']
            0
            sage: r.xpos_array
            [0.0, 0.0, 1.0, 1.0]
            sage: r.yvec_array
            masked_array(data = [0.0 0.707106781187 0.707106781187 0.894427191],
                         mask = [False False False False],
                   fill_value = 1e+20)

        TESTS:

        We test dumping and loading a plot::

            sage: x,y = var('x,y')
            sage: P = plot_vector_field((sin(x), cos(y)), (x,-3,3), (y,-3,3))
            sage: Q = loads(dumps(P))
        """
        self.xpos_array = xpos_array
        self.ypos_array = ypos_array
        self.xvec_array = xvec_array
        self.yvec_array = yvec_array
        self.cvec_array = cvec_array
        
        GraphicPrimitive.__init__(self, options)

    def get_minmax_data(self):
        """
        Returns a dictionary with the bounding box data.

        EXAMPLES::

            sage: x,y = var('x,y')
            sage: d = plot_vector_field((.01*x,x+y), (x,10,20), (y,10,20))[0].get_minmax_data()
            sage: d['xmin']
            10.0
            sage: d['ymin']
            10.0
        """
        from sage.plot.plot import minmax_data
        return minmax_data(self.xpos_array, self.ypos_array, dict=True)

    def _allowed_options(self):
        """
        Returns a dictionary with allowed options for PlotField.

        EXAMPLES::

            sage: x,y = var('x,y')
            sage: P=plot_vector_field((sin(x), cos(y)), (x,-3,3), (y,-3,3))
            sage: d=P[0]._allowed_options()
            sage: d['pivot']
            'Where the arrow should be placed in relation to the point (tail, middle, tip)'
        """
        return {'plot_points':'How many points to use for plotting precision',
                'pivot': 'Where the arrow should be placed in relation to the point (tail, middle, tip)',
                'headwidth': 'Head width as multiple of shaft width, default is 3',
                'headlength': 'head length as multiple of shaft width, default is 5',
                'headaxislength': 'head length at shaft intersection, default is 4.5',
                'zorder':'The layer level in which to draw',
                'color':'The color of the arrows'}

    def _repr_(self):
        """
        String representation of PlotField graphics primitive.

        EXAMPLES::

            sage: x,y = var('x,y')
            sage: P=plot_vector_field((sin(x), cos(y)), (x,-3,3), (y,-3,3))
            sage: P[0]
            PlotField defined by a 20 x 20 vector grid
        """
        return "PlotColorField defined by a %s x %s vector grid"%(self.options()['plot_points'], self.options()['plot_points'])

    def _render_on_subplot(self, subplot):
        """
        TESTS::

            sage: x,y = var('x,y')
            sage: P=plot_vector_field((sin(x), cos(y)), (x,-3,3), (y,-3,3))
        """
        options = self.options()
        quiver_options = options.copy()
        quiver_options.pop('plot_points')
        subplot.quiver(self.xpos_array, self.ypos_array, self.xvec_array, self.yvec_array,self.cvec_array, **quiver_options)


        
class orientationQuery():
    def __init__(self,queryVars=['MASK_THETA'],varFun=lambda cAx: 'PCA1_'+cAx,avgFun=lambda cVar: 'AVG('+cVar+')',countText='COUNT(POS_X)',grpBy='SAMPLE_AIM_NUMBER',sqlAdd=None,ordBy='SAMPLE_AIM_NUMBER',flipAxis=0,graphTitle='',weight='',table='Lacuna',dummy=False):
        if dummy: return None
        if sqlAdd is None : sqlAdd=''
        else: sqlAdd=' AND '+sqlAdd
        if weight is not '': avgFun=lambda cVar: 'SUM(('+cVar+')*('+weight+'))/SUM('+weight+')'
        varFun2=varFun
        textMatrix=[avgFun(varFun2(cAx[0])+'*'+varFun2(cAx[1])) for cAx in ['XX','YY','ZZ','XY','XZ','YZ']]
        
        textQuery=countText+','+','.join(textMatrix)
        stdQuery=','.join([avgFun(varFun(cAx[0])) for cAx in 'XYZ'])
        self._queryVars=[cQ[1:] for cQ in queryVars if cQ[0]=='&']+['AVG('+cQ+')' for cQ in queryVars if cQ[0] is not '&']
        curQuery=cur.execute('SELECT '+','.join(self._queryVars)+','+textQuery+','+stdQuery+' from '+table+' where Sample_Aim_Number>0 '+sqlAdd+' group by '+grpBy+' order by '+ordBy+' LIMIT 20000')
        self.outText=curQuery.fetchall()
        
        self.flipAxis=flipAxis
        self.graphTitle=graphTitle
        self.__query__()
    
                
    
    def __strain__(self,logCorrection):
        self.strainMat=[]
        
        self.strainHeadMat=self._queryVars+['N','Tr','$\lambda_1$','$\lambda_2$','$\lambda_3$','Ud_1','Ud_2','Ud_3','||Ud||','AISO','V11','V12','V13','V21','V22','V23','V31','V32','V33','Pxx','Pxy','Pxz','Pyy','Pyz','Pzz']
        
        self.strainHeader={}
        for i in range(len(self.strainHeadMat)): self.strainHeader[self.strainHeadMat[i]]=i
        mArr=lambda tQ: numpy.array([[tQ[0],tQ[3],tQ[4]],[tQ[3],tQ[1],tQ[5]],[tQ[4],tQ[5],tQ[2]]])

        qvL=len(self._queryVars)
           
        for cOut in self.outText:
            outRow=list(cOut[0:qvL+1])
            p=mArr(cOut[qvL+1:qvL+8])
            if cOut[qvL]>0:
                try: fixP=logCorrection(p)
                except: print p    
                (w,v)=numpy.linalg.eigh(fixP)
                print w
                outRow+=list([fixP.trace()])
                
                outRow+=list(w)
                outRow+=list(w-fixP.trace()/3)
                outRow+=list([sqrt(sum((w-fixP.trace()/3)**2))])
                outRow+=[((w[2]-w[0])/w[2])]
                
                oVec=v[:,2]
                if self.flipAxis>=0: 
                    if oVec[self.flipAxis]<0: oVec*=-1
                outRow+=list(oVec)
                outRow+=list(v[:,1])
                outRow+=list(v[:,0])
                outRow+=list([fixP[0,0],fixP[0,1],fixP[0,2],fixP[1,1],fixP[1,2],fixP[2,2]])
                self.strainMat+=list([outRow])
            else:
                print 'No Edges!'
                
        self.strainMat=mat2num(array(self.strainMat))
        print str(sum(single(self.outMat[:,qvL])))+' investigated in '+str(len(self.outMat))+' groupings'
        
    def __query__(self):
        self.outMat=[]
        
        self.outHeadMat=self._queryVars+['N','Lc.Len','$\lambda_1$','$\lambda_2$','$\lambda_3$','AISO','$V_x$','$V_y$','$V_z$','$V_{\theta}$','$R_x$','$R_y$','$R_z$','$W_x$','$W_y$','$W_z$']
        self.outHeader={}
        for i in range(len(self.outHeadMat)): self.outHeader[self.outHeadMat[i]]=i
        mArr=lambda tQ: numpy.array([[tQ[0],tQ[3],tQ[4]],[tQ[3],tQ[1],tQ[5]],[tQ[4],tQ[5],tQ[2]]])

        qvL=len(self._queryVars)
           
        for cOut in self.outText:
            outRow=list(cOut[0:qvL+1])
            p=mArr(cOut[qvL+1:qvL+8])
            if cOut[qvL]>0:
                try: (w,v)=numpy.linalg.eigh(p)
                except: print cOut
                    
                    
                outRow+=list([sqrt(p.trace())])
                #w=np.sqrt(w)
                outRow+=list(w)
                outRow+=[((w[2]-w[0])/w[2])]
                
                oVec=v[:,2]
                if self.flipAxis>=0: 
                    if oVec[self.flipAxis]<0: oVec*=-1
                outRow+=list(oVec)
                outRow+=[N(180.0/pi*atan2(v[1,2],v[0,2]))]
                outRow+=cOut[qvL+7:]
                oVec=v[:,1]
                outRow+=list(oVec)
                self.outMat+=list([outRow])
            else:
                print 'No Edges!'
                
            #yData+=[N(180/pi*atan2(v[1,2],v[0,2]))]
            #yVal=180.0/pi*min(acos(v[0,2]*cos(cOut[0])+v[1,2]*sin(cOut[0])),acos(-v[0,2]*cos(cOut[0])-v[1,2]*sin(cOut[0])))
        self.outMat=mat2num(array(self.outMat))
        if len(self.outMat)<100: self.drawTable()
        #print self.outMat[:,qvL]
        print str(sum(single(self.outMat[:,qvL])))+' investigated in '+str(len(self.outMat))+' groupings'
    def drawTable(self):
        htmlTable(self.outHeadMat,self.outMat)
    def getCol(self,cVec='AISO'):
        if self.outHeader.has_key(cVec): cVecCol=self.outHeader[cVec]
        return (self.outMat[:,cVecCol])
    def plotHist(self,cVec='AISO',bins=20,title=None):
        if self.outHeader.has_key(cVec): cVecCol=self.outHeader[cVec]
        if title is None: title=self.graphTitle
        pylab.hist(list(self.outMat[:,cVecCol]),bins=bins)
        pylab.xlabel(cVec)
        pylab.ylabel('Occurence')
        pylab.savefig(strcsv(self.graphTitle+'_'+rndSuf())+'.png')
        pylab.savefig(strcsv(self.graphTitle+'_'+rndSuf())+'.pdf')
        pylab.close()
    def plotStability(self,yCol=0,axDef=(None,None,0,100),style='r-',bins=20):
        if style.upper()=='HEX': hexbin(self.outMat[:,yCol],self.outMat[:,self.outHeader['AISO']],gridsize=bins,marginals=False)
        else: pylab.plot(self.outMat[:,yCol],self.outMat[:,self.outHeader['AISO']],style)
        pylab.title('Ell.Edge Tensor Aiso  vs '+self.outHeadMat[yCol]+': R2='+str(np.corrcoef(self.outMat[:,yCol],self.outMat[:,self.outHeader['AISO']])[0,1]))

        pylab.xlabel(self.outHeadMat[yCol])
        pylab.ylabel('Orientation Stability ($\lambda_3, \%$)')
        cAxis=list(pylab.axis())
        for i in range(len(cAxis)):
            if axDef is not None: cAxis[i]=axDef[i]
        pylab.axis(cAxis)
        pylab.savefig(strcsv('OrientationStability'+self.graphTitle+rndSuf())+'.png')
        pylab.savefig(strcsv('OrientationStability'+self.graphTitle+rndSuf())+'.pdf')
        pylab.close()
    def plotQuiver(self,yColX=0,yColY=None,xVec='$V_x$',yVec='$V_y$',cVec='AISO',scVec='$\\lambda_3$',flatLine=False,axDef=(None,None,None,None),vlim=None,options={},useG=False,minPts=10,dpiIn=90,units='x',scale=None,title=None,unitScale=False):
        vList=self.outMat[:,self.outHeader['N']]>minPts # valid list of points
        print sum(vList)*100.0/len(vList)
        if len(self._queryVars)>1:
            if yColY is None: yColY=1
        if yColY is None:
            xPos=cos(list(pi/180*self.outMat[vList,yColX]))
            xName='X Position'
            yPos=sin(list(pi/180*self.outMat[vList,yColX]))
            yName='Y Position'
            axDef=(-1.1,1.1,-1.1,1.1)
        else:
            xPos=list(self.outMat[vList,yColX])
            xName=self.outHeadMat[yColX]
            yPos=list(self.outMat[vList,yColY])
            yName=self.outHeadMat[yColY]
        xVecCol=self.outHeader[xVec]
        yVecCol=self.outHeader[yVec]
        scVecCol=self.outHeader[scVec]
        if self.outHeader.has_key(cVec): cVecCol=self.outHeader[cVec]
        else:
           tryList=filter(lambda x: (x.upper().find(cVec.upper())>=0),self.outHeader.keys());
           if len(tryList)>0: 
               cVec=tryList[0]
               cVecCol=self.outHeader[cVec]   
           else: 
               options['color']=cVec
               cVecCol=-1
               print 'Using Default!'
               print self.outHeadMat[cVecCol]
        if flatLine:
            yPos=list(0*array(yPos))
        if unitScale:
            xyVec=list(self.outMat[vList,xVecCol]*self.outMat[vList,scVecCol])
            yyVec=list(self.outMat[vList,yVecCol]*self.outMat[vList,scVecCol])
        else:
            xyVec=list(self.outMat[vList,xVecCol])
            yyVec=list(self.outMat[vList,yVecCol])
        
        options['plot_points']=len(xyVec)
        
        if useG:
            g = Graphics() 
            if cVecCol>=0: g.add_primitive(PlotColorField(xPos, yPos, xyVec, yyVec,list(self.outMat[vList,cVecCol]), options)) 
            else: g.add_primitive(PlotField(xPos, yPos, xyVec, yyVec, options))
            return g
        else:
            
            pylab.quiver(xPos,yPos,xyVec,yyVec,list(self.outMat[vList,cVecCol]),scale=scale,units=units)

            self.cv=list(self.outMat[vList,cVecCol])
            self.xp=xPos
            self.yp=yPos
            self.xv=xyVec
            self.yv=yyVec
            
            pylab.xlabel(ptName(xName))
            pylab.ylabel(ptName(yName))
            pylab.title(self.graphTitle)
            pylab.colorbar()
            pylab.clim(vlim[0],vlim[1])
            cAxis=list(pylab.axis())
            for i in range(len(cAxis)):
                if axDef is not None: cAxis[i]=axDef[i]
            pylab.axis(cAxis)
            if flatLine: pylab.axis(cAxis[0:2]+[min(yyVec),max(yyVec)])
            if title is not None: pylab.title(title)
            pylab.savefig(strcsv(self.graphTitle+'_'+rndSuf())+'.png',dpi=dpiIn)
            pylab.savefig(strcsv(self.graphTitle+'_'+rndSuf())+'.pdf',dpi=dpiIn)
            pylab.close()
    def plotQuiver3D(self,yColX=0,yColY=1,yColZ=2,xVec='$V_x$',yVec='$V_y$',zVec='$V_z$',cVec='AISO',pcaGray=1,options={}):
        ## Quiver Plot 3D
        xPos=list(self.outMat[:,yColX])
        xName=self.outHeadMat[yColX]
        yPos=list(self.outMat[:,yColY])
        yName=self.outHeadMat[yColY]
        zPos=list(self.outMat[:,yColZ])
        zName=self.outHeadMat[yColZ]
        xVecCol=self.outHeader[xVec]
        yVecCol=self.outHeader[yVec]
        
        xVecCol=self.outHeader[xVec]
        yVecCol=self.outHeader[yVec]
        zVecCol=self.outHeader[zVec]
        cVecCol=self.outHeader[cVec]
        
        xyVec=list(self.outMat[:,xVecCol])
        yyVec=list(self.outMat[:,yVecCol])
        xzVec=list(self.outMat[:,zVecCol])
        cVec=list(self.outMat[:,cVecCol])
        Quiver3(xPos,yPos,zPos,xyVec,yyVec,xzVec,cVec,drawHead=(self.flipAxis<0))
             
        
class nhoodQuery(orientationQuery):
    def __init__(self,queryVars=['MASK_THETA'],avgFun=lambda cVar: 'AVG('+cVar+')',countText='COUNT(*)',grpBy='LAC1.SAMPLE_AIM_NUMBER',ordBy='LAC1.SAMPLE_AIM_NUMBER',flipAxis=0,graphTitle='',weight=''):

        varFun=lambda x: 'LAC1.POS_'+x
        dvarFun=lambda x: '(LAC1.POS_'+x+'-LAC2.POS_'+x+')'
        if weight is not '': avgFun=lambda cVar: 'SUM(('+cVar+')*('+weight+'))/SUM('+weight+')'
        textMatrix=[avgFun(dvarFun(cAx[0])+'*'+dvarFun(cAx[1])) for cAx in ['XX','YY','ZZ','XY','XZ','YZ']]
        textQuery=countText+','+','.join(textMatrix)
        stdQuery=','.join([avgFun(varFun(cAx[0])) for cAx in 'XYZ'])
       
        
        self._queryVars=['AVG(LAC1.'+cQ+'-LAC2.'+cQ+')' for cQ in queryVars if cQ[0] is not '&']
        qryStr=','.join(self._queryVars)+','+textQuery+','+stdQuery
        print qryStr
        lacStr1=' INNER JOIN (SELECT * FROM LACUNA WHERE SAMPLE_AIM_NUMBER>0) LAC1 ON (LAC1.Sample_AIM_Number=E1.SAMPLE_AIM_NUMBER AND E1.V1=LAC1.LACUNA_NUMBER) '
        lacStr2='LAC2'.join(lacStr1.split('LAC1'))
        lacStr2='E1.V2'.join(lacStr2.split('E1.V1'))
        
        print lacStr1
        print lacStr2
        curQuery=cur.execute('SELECT '+qryStr+' from Edge E1 '+lacStr1+lacStr2+' where E1.Project_Number="'+str(projectTitle)+'" group by '+grpBy+' order by '+ordBy+' LIMIT 10000')
        
        self.outText=curQuery.fetchall()
        
        self.flipAxis=flipAxis
        self.graphTitle=graphTitle
        self.__query__()


def drawArrow(t,centPos,diffPos,aniso,lenScale,mCol,drawHead,drawFaces=False):
    texName=md5.md5(str(i)+str(centPos)+'wow').hexdigest()[0:5]
    rOp=1
    
    
    botSpot=tuple(centPos-1*diffPos)
    topSpot=tuple(centPos+1*diffPos)
    midSpot=tuple(centPos)
    planeEq=lambda x,y,z: diffPos[0]*(x-modSpot[0])+diffPos[1]*(y-modSpot[1])+diffPos[2]*(z-modSpot[2])
    diffSum=sqrt(diffPos[0]**2+diffPos[1]**2+diffPos[2]**2)
    
    ksign=lambda x: (x==0)+sign(x)
    def smrtmin(x):
    	if abs(x)<0.05*diffSum: return ksign(x)*1
    	else: return x    
    def renorm(vec,nlen=1):
    	tsumr=sqrt(vec[0]**2+vec[1]**2+vec[2]**3)
    	print ('renorm',vec,tsumr)
    	return (nlen*vec[0]/tsumr,nlen*vec[1]/tsumr,nlen*vec[2]/tsumr)
    def triPt(c,d):
    	txr=-1/smrtmin(diffPos[0])
    	tyr=-1*d/smrtmin(diffPos[1])
    	tzr=-1*d/smrtmin(diffPos[2])
    	tsumr=sqrt(txr**2+tyr**2+tzr**2)
    	return numpy.array([txr,tyr,tzr])*c/tsumr
    t.texture(texName+'c', ambient=.3, diffuse=0.9, specular=0.1, opacity=1*rOp, color=mCol)
    t.fcylinder(botSpot,topSpot,lenScale/4,texName+'c')
    if drawHead:
		t.texture(texName, ambient=.3, diffuse=0.9, specular=.7*rOp, opacity=.3*rOp, color=(0,0,1))
		#t.sphere(topSpot, 1.5*lenScale/4,texName)
		t.sphere(botSpot, 2.5*lenScale,texName)
		t.sphere(topSpot, 2.5*lenScale,texName)
    if drawFaces:
    	t.texture(texName+'t', ambient=.3, diffuse=0.9, specular=.7*rOp, opacity=.3*rOp, color=(0,1,0))
    	pt1=triPt(diffSum/2,1)
    	pt3=numpy.cross(pt1,diffPos)/diffSum*2
    	t.triangle(pt1+midSpot,pt3+midSpot,midSpot,texName+'t')
    	pt1=triPt(diffSum/2,-1)
    	pt3=numpy.cross(pt1,diffPos)/diffSum*2
    	t.triangle(pt1+midSpot,pt3+midSpot,midSpot,texName+'t')
    return t
def drawTensorCross(t,centPos,arrowShape,lenScale,mColFunc):
    texName=md5.md5(str(i)+str(centPos)+'wow').hexdigest()[0:5]
    # Draws just an cylinder instead of an arrow
    rOp=1

    t.texture(texName+'a', ambient=.3, diffuse=0.9, specular=0.1, opacity=1*rOp, color=mColFunc(arrowShape[0]))
    diffPos=np.array([arrowShape[0]/2,0,0])
    botSpot=tuple(centPos-diffPos)
    topSpot=tuple(centPos+diffPos)
    t.fcylinder(botSpot,topSpot,lenScale/4,texName+'a')
    
    t.texture(texName+'b', ambient=.3, diffuse=0.9, specular=0.1, opacity=1*rOp, color=mColFunc(arrowShape[1]))
    diffPos=np.array([0,arrowShape[1]/2,0])
    botSpot=tuple(centPos-diffPos)
    topSpot=tuple(centPos+diffPos)
    t.fcylinder(botSpot,topSpot,lenScale/4,texName+'b')
    
    t.texture(texName+'c', ambient=.3, diffuse=0.9, specular=0.1, opacity=1*rOp, color=mColFunc(arrowShape[2]))
    diffPos=np.array([0,0,arrowShape[2]/2])
    botSpot=tuple(centPos-diffPos)
    topSpot=tuple(centPos+diffPos)
    t.fcylinder(botSpot,topSpot,lenScale/4,texName+'c')
    
    diffSum=sqrt(diffPos[0]**2+diffPos[1]**2+diffPos[2]**2)
    #t.texture(texName, ambient=.3, diffuse=0.9, specular=.7*rOp, opacity=.3*rOp, color=(0,0,1))
    #t.sphere(centPos, diffPos[1],texName)
    
    return t    
def gryByVal(cVal,maxVal,minVal=0,useLog=False):
    if cVal<minVal: return 0
    if cVal>maxVal: return 1
    cVal+=-minVal
    maxVal+=-minVal
    cMax=float(maxVal)
    if useLog:
        return log(cVal)/log(cMax)
    else:
        return float(cVal)/cMax    
arrowLimit=200000    
def Quiver3(xPos,yPos,zPos,xVec,yVec,zVec,cVec=None,camAng=10,objAng=0,fastScale=5,zoom=.8,pcaGray=1,cMapBlue=False,drawHead=False,imNum=None,arrowScalar=0.25,t=None,mColorFunc=None):
    camA2=camAng*pi/180
    objA2=objAng*pi/180
    
    rangeVec=[max(xPos)-min(xPos),max(yPos)-min(yPos),max(zPos)-min(zPos)]
    volPerArrow=prod(rangeVec)/len(xPos)
    sideLen=volPerArrow**(0.333)
    print sideLen*arrowScalar
    objCent=(0,0,0)
    zoomL=zoom/2
    camCent=(rangeVec[0]*cos(objA2)*cos(camA2)/zoom+min(xPos),rangeVec[1]*sin(objA2)*cos(camA2)/zoom+min(yPos),rangeVec[2]*sin(camA2)/zoom+min(zPos))
    lightCent=(rangeVec[0]*cos(objA2+pi/4)*cos(camA2)/zoomL+min(xPos),rangeVec[1]*sin(objA2+pi/4)*cos(camA2)/zoomL+min(yPos),rangeVec[2]*sin(camA2)/zoomL+min(zPos))
    
    objCent=(mean(xPos),mean(yPos),mean(zPos))
    rayD=5
    if fastScale<4: rayD=int(5+(4-fastScale))
    addBasics=False
    if t is None:
		t = Tachyon(xres=int(4000./fastScale),yres=int(3000/fastScale), camera_center=camCent, look_at=objCent,antialiasing=True,raydepth=rayD)
		t.light(lightCent, 1, (1,1,1))
		addBasics=True
    failed=0
    if cVec is not None:
		if pcaGray==0:
			cMean=numpy.mean(cVec)
			cStd=numpy.std(cVec)
			stdFact=1
			cMaxVal=cMean+cStd*stdFact
			cMinVal=cMean-cStd*stdFact
		elif pcaGray==1:
			cMaxVal=max(cVec)
			cMinVal=min(cVec)
			
		else:
			cMaxVal=max(pcaGray)
			cMinVal=min(pcaGray)
			
        
		if pcaGray>1: print (cMinVal,numpy.mean(cVec),cMaxVal)
    for i in range(0,min(arrowLimit,len(xPos))):
        
        if cVec is not None: 
        	cVal=cVec[i]
        	rOp=gryByVal(cVal,cMaxVal,cMinVal,(pcaGray==3))
        else: 
        	cVal=80
        	rOp=1
        
        mCol=(rOp,1-rOp,0)
        if cMapBlue: mCol=(1,1,rOp)
        if mColorFunc is not None: mCol=mColorFunc(cVal)

        drawArrow(t,array([xPos[i],yPos[i],zPos[i]]),array([xVec[i],yVec[i],zVec[i]]),cVal,sideLen*arrowScalar,mCol,drawHead) #sideLen/4
        
    
    print 'failed : '+str(failed)+' of '+str(len(xVec))
    
    t.texture('bgplane', color=(1,1,1), opacity=1, specular=1, diffuse=1)
    #t.plane((0,0,-1000), (0,0,-1000), 'bgplane') 
    if imNum is None : imName=strcsv(md5.md5(str(i)+str(xPos)+str(xVec)+'wow').hexdigest()[0:5])
    else: imName='%010d' % (imNum)
    t.save(imName+'.png')
    return t
    
def overviewPlots(sampleName='%'):
    corrTable(['DENSITY/1000','PROJ_PCA1/(PROJ_PCA2+PROJ_PCA3)*2','VOLUME*1000*1000*1000','NEAREST_NEIGHBOR_NEIGHBORS','DISPLACEMENT_MEAN*1000'],projectName=projectTitle,sqlAdd='SAMPLE_AIM_NUMBER LIKE "'+sampleName+'"',mode=0,maxFetch=50)
    SummaryPlot('Mask_Distance_Mean*1000','Mean Distance From Mask','Mask Distance ($\mu$m)',35,'SAMPLE_AIM_NUMBER LIKE "'+sampleName+'"')
    SummaryPlot('Volume*1000*1000*1000','Volume','Volume (um^3)',40,'SAMPLE_AIM_NUMBER LIKE "'+sampleName+'"') 
    SQLHistPlot('Volume*1000*1000*1000','&COUNT(*)','Volume','Volume (um^3)',bins=40,sqlAdd='SAMPLE_AIM_NUMBER LIKE "'+sampleName+'" AND VOLUME>0',logx=True,useEbar=False) 
    SummaryPlot('PROJ_PCA1*1000','Lacuna Length','Length (um)',40,'SAMPLE_AIM_NUMBER LIKE "'+sampleName+'"')
    SummaryPlot('PROJ_PCA2*1000','Lacuna Width','Width (um)',40,'SAMPLE_AIM_NUMBER LIKE "'+sampleName+'"')
    SummaryPlot('ABS(PCA1_Phi)','Lacuna Primary Angle','$\phi$ angle (deg)',35,'SAMPLE_AIM_NUMBER LIKE "'+sampleName+'"')
    SummaryPlot('DENSITY','Density','Density (1/$mm^3$)',35,'SAMPLE_AIM_NUMBER LIKE "'+sampleName+'"')
    SQLHistPlot('DENSITY','&COUNT(*)','Density','Density (1/$mm^3$)',bins=35,sqlAdd='SAMPLE_AIM_NUMBER LIKE "'+sampleName+'" AND DENSITY>0',logx=True,useEbar=False)
    SummaryPlot('PROJ_PCA1/(PROJ_PCA2+PROJ_PCA3)*2','Anisotropy','Anisotropy',40,'SAMPLE_AIM_NUMBER LIKE "'+sampleName+'"')
    SummaryPlot('1/(36*3.14)*DENSITY_VOLUME_SHELL*DENSITY_VOLUME_SHELL*DENSITY_VOLUME_SHELL/(DENSITY_VOLUME*DENSITY_VOLUME)-1','Voronoi Sphericity','Voronoi Sphericity',35,'SAMPLE_AIM_NUMBER LIKE "'+sampleName+'"')
    SQLHistPlot('NEAREST_NEIGHBOR_Neighbors','&COUNT(*)','Geometric Neighbors','Neighbors',sqlAdd='SAMPLE_AIM_NUMBER LIKE "'+sampleName+'" AND NEAREST_NEIGHBOR_NEIGHBORS<35',isInt=True,useEbar=False)
    SummaryPlot('DISPLACEMENT_MEAN*1000','Displacement Distance','Displacement ($\mu$m)',35)
    
from pylab import *
from scipy.interpolate import interp1d
import md5
numfixer=lambda cStr: filter(lambda cLet: (cLet.isdigit()) | (cLet=='.'),cStr)
def mat2num(inMat): # Mat2num fixes numpy arrays which because of a single bad value or lost digit get converted to string arrays
    outMat=np.zeros(inMat.shape)
    if len(inMat.shape)>1:
        for i in range(inMat.shape[0]): outMat[i]=mat2num(inMat[i])
    else:
        for i in range(inMat.shape[0]): 
            try: outMat[i]=single(inMat[i])
            except:
                try: outMat[i]=single(numfixer(inMat[i]))
                except:
                    print str(inMat[i])+' is invalid'
                    
    return outMat             

from scipy import optimize
class ValueHistogram():
    def __init__(self,data,bins=20,name='ValueHistogram',ymin=None,ymax=None,r_scale=sqrt(3)*1.4,vol_weighted=True,xscale_func=lambda x: x,volAdd=0,fillBlanks=False):
        self.ymin=ymin
        self.ymax=ymax
        self._yscale=1
        self._volAdd=volAdd
        #if name.find('DENS')>=0: self._yscale=1631.0/3276.700000
        self.r_xvals=list(data[:,0])
        self.r_yvals=list(self._yscale*data[:,2])
        self.r_func=interp1d(self.r_xvals,self.r_yvals)
        self.r_xscale_func=xscale_func
        self.r_ystds=list(data[:,3])
        self.r_volume=list(data[:,1])
        self.r_min=list(data[:,4])
        self.r_max=list(data[:,5])
        self.bins=bins
        self.r_scale=r_scale
        self.vol_weighted=vol_weighted
        self._fillBlanks=fillBlanks
        self._update()
        self._key=rand(1)[0]
        self.name=name
        self._grams=1

    def vhist(self,col='Y',bins=20,minVal=None,maxVal=None):
        if col.upper()=='X':  cData=self.r_xvals
        elif col.upper()=='Y':  cData=self.r_yvals
        elif col.upper()=='YS':  cData=self.r_ystds
        else: return -1
        if minVal is None: minVal=min(cData)
        if maxVal is None: maxVal=max(cData)
        binMat=linspace(minVal,maxVal,bins)
        outMat=0*binMat
        vData=numpy.array(self.r_volume)
        for cDx in range(len(binMat)-1):
            cPoints=(cData>binMat[cDx]) & (cData<=binMat[cDx+1])
            
            if sum(cPoints)>0: outMat[cDx]=sum(vData[cPoints])
        return (binMat,outMat)
    def fit(self,fitFunc,ythresh=None,doPlot=False,plotName='test.png'):
        self.fit_func=fitFunc
        tempX=numpy.array(map(self.r_xscale_func,self.r_xvals))
        tempY=numpy.array(self.r_yvals)
        validPoints=(tempX>self._minv) & (tempX<=self._maxv)
        if ythresh is not None:
            validPoints=validPoints & ((tempY>min(ythresh)) & (tempY<max(ythresh)))
        #print str(sum(validPoints))+'/'+str(len(validPoints))
        tempX=tempX[validPoints]
        tempY=tempY[validPoints]
        
        guessP=fitFunc[0](tempX,tempY)
        #self.fit_out=lsq(lambda p: (tempY-fitFunc[1](tempX,p)),guessP,full_output=1)
        def temp_fitfun(x,*p): return fitFunc[1](x,list(p))
        self.fit_out=optimize.curve_fit(temp_fitfun,tempX,tempY,p0=guessP,maxfev=40000)
        newP=self.fit_out[0]
        fullP=list(newP)
        ccov=self.fit_out[1]
        try:
            diagList=numpy.array(range(ccov.shape[0]))
            self.cstd=sqrt(ccov[diagList,diagList])
            
            fitdat=fitFunc[1](tempX,newP)
            if doPlot:
            	pylab.close()
            	pylab.plot(tempX,tempY,'r+')
            	pylab.plot(tempX,fitdat,'b-')
            	pylab.savefig(plotName)
            self.fit_r2=np.corrcoef(fitdat,tempY)[0,1]**2
            fullP+=[self.fit_r2]
            #print (self.name,'-> Fit Vals:',newP,'R^2:',self.fit_r2)
            self.fitraw_x=tempX
            self.fitraw_y=tempY
            self.fit_x=np.array(floatlist(linspace(min(tempX),max(tempX),800)))
            self.fit_func=lambda inX: floatlist(fitFunc[1](inX,newP))
            self.fit_y=floatlist(fitFunc[1](self.fit_x,newP))
            self.fit_ymin=floatlist(fitFunc[1](self.fit_x,numpy.array(newP)-self.cstd))
            self.fit_ymax=floatlist(fitFunc[1](self.fit_x,numpy.array(newP)+self.cstd))
            
        except:
            print 'Fitting Failed'
            self.cstd=[]
            self.fitraw_x=tempX
            self.fitraw_y=tempY
            self.fit_x=np.array(floatlist(linspace(min(tempX),max(tempX),800)))
            self.fit_y=0*self.fit_x
            self.fit_ymin=0*self.fit_x
            self.fit_ymax=0*self.fit_x
            fullP+=[0]
                
                
        return fullP    
    def _maxcenter(self,minX=0,maxX=15,rescale=False):
        self.r_xvals=numpy.array(self.r_xvals)
        self.r_yvals=numpy.array(self.r_yvals)
        valX=((self.r_xvals<maxX) & (self.r_xvals>minX))
        xShift=self.r_xvals[valX][self.r_yvals[valX].argmax()]
        print xShift
        if rescale: self.r_xvals=list((self.r_xvals-xShift)/xShift)
        else: self.r_xvals=list(self.r_xvals-xShift)
        self.r_yvals=list(self.r_yvals)
    def _update(self):
        tempX=numpy.array(map(self.r_xscale_func,self.r_xvals))
        self._minv=tempX.min()
        self._maxv=tempX.max()

        self._update2()
    def reylim(self,ymin=None,ymax=None):
        self.ymin=ymin
        self.ymax=ymax
        
        self._update2()
    def rebin(self,minV,maxV,bins,ymin=None,ymax=None):
        self._minv=minV
        self._maxv=maxV
        self.bins=bins
        self._update2()
           
    def _normY(self):
        self.yvals=numpy.array(self.yvals)/sum(self.yvals)
    def _intY(self):
        self.yvals=numpy.cumsum(self.yvals)        
    def _normY0(self):
        self.yvals=numpy.array(self.yvals)/self.yvals[0]
    def _normR0(self,r0):
        self.yvals=(numpy.array(self.xvals)+r0)/r0
    def _normV(self):
        self.volume=numpy.array(self.volume)/sum(self.volume)
    def _update2(self):    
        tempX=numpy.array(map(self.r_xscale_func,self.r_xvals))
        tempY=numpy.array(self.r_yvals)
        
        self.yvals=[]
        self.ystds=[]
        self.yerr=[]
        self.volume=[]
        self.min=[]
        self.max=[]
        
        if self.ymin is not None: self.yCheck=True
        else: self.yCheck=False
        self.xvals=numpy.linspace(self._minv,self._maxv,self.bins+1)[range(1,self.bins+1)]
        
        goodxVals=[]
        cminval=self._minv
        if self.yCheck: nvpts=(tempY>self.ymin) & (tempY<self.ymax)
        
        for i in range(0,len(self.xvals)):
            validPoints=(tempX>cminval) & (tempX<=self.xvals[i])
            
            if self.yCheck: validPoints = validPoints & nvpts
            
            if sum(validPoints)>0:
                tempV=numpy.array(self.r_volume)[validPoints]
                tempY=numpy.array(self.r_yvals)[validPoints]
                tempYS=numpy.array(self.r_ystds)[validPoints]
                tempMin=numpy.array(self.r_min)[validPoints]
                tempMax=numpy.array(self.r_max)[validPoints]
                if self.vol_weighted:
                    self.yvals+=[sum(tempV*tempY)/sum(tempV)]
                    self.ystds+=[sum(tempV*tempYS)/sum(tempV)]
                    self.yerr+=[sum(tempV*tempYS)/sum(tempV)/sqrt(len(tempYS))]
                else:
                    self.yvals+=[mean(tempY)]
                    self.ystds+=[mean(tempYS)]
                    self.yerr+=[sum(tempV*tempYS)/sum(tempV)/sqrt(len(tempYS))]
                
                self.volume+=[sum(tempV)]
                self.min+=[min(tempMin)]
                self.max+=[max(tempMax)]
            
                goodxVals+=[self.xvals[i]-(self.xvals[i]-cminval)/2]
                cminval=self.xvals[i]
          
            else:   
                if self._fillBlanks:
                    self.yvals+=[0]
                    self.ystds+=[0]
                    self.yerr+=[0]
                    self.volume+=[0]
                    self.min+=[0]
                    self.max+=[0]                    
                    goodxVals+=[self.xvals[i]-(self.xvals[i]-cminval)/2]
                    cminval=self.xvals[i]
                #self.xvals=
        self.nvals=numpy.array(self.volume)/sum(self.volume)
        self.xvals=list(numpy.array(goodxVals)*self.r_scale)
        #print (len(self.vals)
        self.ifun=interp1d(self.xvals,self.yvals)    
    def __iadd__(self,otherGram):
        self.r_xvals+=otherGram.r_xvals

        self.r_yvals+=otherGram.r_yvals
        self.r_ystds+=otherGram.r_ystds
        
        self.r_volume+=otherGram.r_volume
        self.r_min+=otherGram.r_min
        self.r_max+=otherGram.r_max
        self._grams+=otherGram._grams
        self._update()
        return self
    def labelSpacing(self,latSpacing=20):
        
        tempy=lambda x: (x,N(self.ifun(x)))
        annotate('NN.dist',tempy(latSpacing),xytext=(0,-100),textcoords='offset points',arrowprops={'width':1})
        annotate('NN.dist 1D',tempy(latSpacing/2),xytext=(0,100),textcoords='offset points',arrowprops={'width':1})
        annotate('NN.dist 2D',tempy(latSpacing*sqrt(2)/2),xytext=(0,-100),textcoords='offset points',arrowprops={'width':1})
        annotate('NN.dist 3D',tempy(latSpacing*sqrt(3)/2),xytext=(0,100),textcoords='offset points',arrowprops={'width':1})    
    def gVal(self,cVar,cx):
        xv=self.xvals
        yv=self.yvals
        if cVar.find('Y')>-1:
            yv=self.yvals
        elif cVar.find('MIN')>-1:
            yv=self.min
        elif cVar.find('MAX')>-1:
            yv=self.max 
        elif cVar.find('STD')>-1:
            yv=self.ystds  
        elif cVar.find('VOL')>-1:
            yv=self.volume
            
        tempfun=interp1d(xv,yv)
        #print tempfun
        #print (xv,yv)
        return tempfun(cx)
    def plot(self,pvar='yval',latSpacing=None):
        for cVar in pvar.upper().split(','):
            xv=self.xvals
            doPlot=True
            if cVar.find('Y')>-1:
                yv=self.yvals
            elif cVar.find('MIN')>-1:
                yv=self.min
            elif cVar.find('MAX')>-1:
                yv=self.max 
            elif cVar.find('STD')>-1:
                yv=self.ystds  
            elif cVar.find('VOL')>-1:
                yv=self.volume
            elif cVar.find('EBAR')>-1:
                yv=self.yvals
                errorbar(self.xvals,self.yvals,yerr=self.ystds, ms=0, mew=0)#,marker='s')
                doPlot=False  
            if doPlot: plot(xv,yv)
            self.ifun=interp1d(xv,yv)
        if latSpacing is not None: self.labelSpacing(latSpacing)
            
        title(self.name+' '+pvar)
        xlabel('Distance ($\mu$m)')
        
        savefig(strcsv(self.name+'_'+pvar+'_'+rndSuf()))
        close()
        
    def _findfit(self): # old and not very fancy, updated to fit which is compatible with SQLHist-style fit functions
        odata=[]
        for i in range(len(self.r_xvals)):
            odata+=[[self.r_xvals[i],self.r_yvals[i]]]
        return lambda model,guess: opt.find_fit(odata,model,guess,solution_dict=False,variables=[var('x')])   
    def __getitem__(self,giArgs):
        print giArgs
        numpy.array(self.xvals)

class HistoGroup():
    def __init__(self,inDict,validFun,bins=50):
        self.histList=[cKey for cKey in inDict.keys() if validFun(cKey)]
        start=True
        for cKey in self.histList:
            if start:
                self.histgram=ValueHistogram(numpy.array(inDict[cKey]),bins,name=cKey)
                start=False
            else:
                self.histgram+=ValueHistogram(numpy.array(inDict[cKey]))
                self.histgram.name+='; '+cKey
        
        #self.__getattr__=lambda name: getattr(self.histgram,name)
        #self.__setattr__=lambda name,value: setattr(self.histgram,name,value)
    def __getattr__(self,name):
        try:
            return getattr(self,name)
        except:
            return getattr(self.histgram,name)
def HistoGroup(inDict,validFun,bins=50):
    histList=[cKey for cKey in inDict.keys() if validFun(cKey)]
    start=True
    for cKey in histList:
        if start:
            histgram=ValueHistogram(numpy.array(inDict[cKey]),bins,name=cKey)
            start=False
        else:
            histgram+=ValueHistogram(numpy.array(inDict[cKey]))
            histgram.name+='; '+cKey
    return histgram         


class gaussInter():
    def __init__(self,x_vals,y_vals,fadDist=10):
        self.x_vals=np.array(x_vals)
        self.y_vals=np.array(y_vals)
        self.fadDist=fadDist
    def _weights(self,xp):
        rv=((self.x_vals-xp)**2)/self.fadDist**2
        return numpy.exp(-rv)
    def std(self,xp):
        weights=self._weights(xp)
        cmean=sum(weights*self.y_vals)/sum(weights)
        return sqrt(sum(weights*(cmean-self.y_vals)**2)/sum(weights))
    def ste(self,xp):
        return self.std(xp)/sqrt(len(self.x_vals))
    def mean(self,xp):
        weights=self._weights(xp)
        return sum(weights*self.y_vals)/sum(weights)
    def __call__(self,xp,mode='mean',npa=0):
        if mode.lower()=='mean': cFunc=self.mean
        elif mode.lower()=='std': cFunc=self.std
        elif mode.lower()=='ste': cFunc=self.ste
        else: return 'FAIL'
        try: 
            if npa: return map(cFunc,xp)
            else: return np.array(map(cFunc,xp))
        except:    
            return cFunc(xp)
class gaussInter2():
    def __init__(self,x_vals,y_vals,z_vals,fadDist=10):
        self.x_vals=np.array(x_vals)
        self.y_vals=np.array(y_vals)
        self.z_vals=np.array(z_vals)
        self.fadDist=fadDist
    
    def _weights(self,xp):
        rv=((self.x_vals-xp[0])**2+(self.y_vals-xp[1])**2)/self.fadDist**2
        return numpy.exp(-rv)
    def std(self,xp):
        weights=self._weights(xp)
        cmean=sum(weights*self.z_vals)/sum(weights)
        return sqrt(sum(weights*(cmean-self.z_vals)**2)/sum(weights))
    def ste(self,xp):
        return self.std(xp)/sqrt(len(self.x_vals))
    def mean(self,xp):
        weights=self._weights(xp)
        return sum(weights*self.z_vals)/sum(weights)
    def __call__(self,xp,yp=None,mode='mean',npa=0):
        if type(yp) is type(''): 
            mode=yp
            yp=None
        
        if mode.lower()=='mean': cFunc=self.mean
        elif mode.lower()=='std': cFunc=self.std
        elif mode.lower()=='ste': cFunc=self.ste
        else: return 'FAIL'
        
        if yp is not None:
            try:
                xp=zip(xp,yp)
            except:
                xp=(xp,yp)
            yp=None    
        if yp is None:
            if len(np.array(xp).shape)>1: 
                if npa: return map(cFunc,xp)
                else: return np.array(map(cFunc,xp))
            else:    
                return cFunc(xp)

