
rStr=lambda x: '\\"'.join(x.split('"'))

r.library('DBI')
r.library('RSQLite')
r.library('RMySQL')
r.library('qtl')
r.library('beanplot')
r.library('xtable')
r.library('irr')
r.library('car')

#r.eval('drv <- dbDriver("SQLite")')
#r.eval('dbCon <- dbConnect(drv, dbname = "'+homeDb+'")')
r.eval('dbCon<-dbConnect(MySQL(), user="sage", password="test263",dbname="lacuna",host="pc7819.psi.ch")')

rFriendlyName=lambda x: ''.join([cX for cX in latexFriendlyName(x) if cX.isalnum()])	
lastVal='eee'
def rSumTable(rSumName):
    rSumObj=r.summary(rSumName)._sage_()
    outText='<table border="1"><tr><td>'
    outText+='</td><td>'.join(['']+[cObj.strip() for cObj in rSumObj['_Dimnames']['#1']])
    outText+='</td></tr>'
    globals()['lastVal']='eee'
    def clrNone(y):
        if y is None: 
            if globals()['lastVal'] is y: return ''
            else:
                globals()['lastVal']=y 
                return '&&&&'
        else: 
            globals()['lastVal']=y
            return y
    cOutput='$$$$'.join(map(clrNone,rSumObj['DATA']))
    cGrid=cOutput.split('&&&&')
    cRowHeaders=['Min.','1st Qu.','Median','Mean','3rd Qu.','Max.','']
    for cRow in range(0,7):
        outText+='<tr><td>'+cRowHeaders[cRow]+'</td>'
        for cCol in range(len(cGrid)):
            tGrid=filter(lambda y: len(y.strip())>0,cGrid[cCol].split('$$$$'))
            outText+='<td>'
            if len(tGrid)>cRow: 
                cVal=tGrid[cRow].strip()
                if cVal.find(':')>=0: cVal=cVal.split(':')[1]
                outText+=cVal
            outText+='</td>'
        outText+='</tr>'
    outText+='</table>'
    html(outText)


        
            
class SimpleStudy(kdict):
    def __init__(self,_miceSQL,_measSQL,sampleSelect=None,project=None,minMeasurements=0):
        whereStats=[]
        if project is None: projectStr=globals()['projectTitle']
        if len(project)>0: whereStats+=['PROJECT LIKE "'+project+'"']
        if sampleSelect is not None: whereStats+=[sampleSelect]
        
        self.currentStudySQL=' AND '.join(whereStats)
        self.miceSQL=_miceSQL
        self.measSQL=_measSQL
        self.results={}
        self.minMeasurements=minMeasurements
        
        # Code to List the Samples
        #self.measurements=(cur.execute('select '+self.measSQL+',COUNT(Sample_Name_Aim) from Lacuna where '+self.currentStudySQL+' group by '+self.measSQL)).fetchall()
        #self.mice=(cur.execute('select '+self.miceSQL+',COUNT(Sample_Name_Aim) from Lacuna where '+self.currentStudySQL+' group by '+self.miceSQL)).fetchall()
    
    
    def run(self,iVars):
        if type(iVars) is type('bob'): iVars=[iVars]
        def _fixVarName(cVar):
            if cVar[0]!='&': return 'AVG('+cVar+')'
            else: return cVar[1:]
        varList=tuple(map(_fixVarName,iVars))
        self.results[varList]=(cur.execute('select ('+self.miceSQL+'),('+self.measSQL+'),'+','.join(varList)+' from Lacuna where '+self.currentStudySQL+' group by '+self.miceSQL+' || "_k_" || '+self.measSQL)).fetchall()
        for cVar in iVars:
            if self.has_key(cVar): del self[cVar]
        for tResults in self.results[varList]:
            for iVarDex in range(0,len(iVars)):
                cMouse=tResults[0]
                cMeas=tResults[1]
                cVar=iVars[iVarDex]
                cAvg=tResults[2+iVarDex] 
                self[str(cVar)][str(cMouse)][str(cMeas)]=cAvg
        if self.minMeasurements>0:
            for cVar in iVars:
                delMouse=False
                for cMouse in self[str(cVar)].keys():
                     if len(self[str(cVar)][str(cMouse)].keys())<self.minMeasurements: 
                         
                         print str((str(cVar),str(cMouse)))+' has been removed because it had too few measurements :'+str(len(self[str(cVar)][str(cMouse)].keys()))
                         del self[str(cVar)][str(cMouse)]
                      
    def html(self,nVar=None):
        if nVar is None: 
            for cVar in sorted(self.keys()): self.html(cVar)
        else:
            outStr='<tr><td>'+latexFriendlyName(nVar)+'</td><td>Measurements (DOF='+str(len(self[nVar].keys())*(len(self[nVar].values()[0].keys())-1))+')</td></tr>'
            outStr+='<tr><td>Sample</td><td>'+self[nVar]._html()+'</td></tr>'
            html('<table>'+outStr+'</table>') 
            
    def _rfullframe(self,**kwArgs):
        colNames=['Sample','Measurement']
        outArray=kdict()
        finalOut={}
        for nCol in colNames+kwArgs.keys(): finalOut[nCol]=[]
        
        isFirst=True
        for (cVarIn,curResults) in self.items():
            cVar1=rFriendlyName(cVarIn)
            finalOut[cVar1]=[]    
            for (cCol1,cData) in curResults.items():
                for (cCol2,cLine) in cData.items():
                    outArray[(cCol1,cCol2)][cVar1]=cLine
                    if isFirst: 
                        for (clamVar,cFunc) in kwArgs.items(): 
                            outArray[(cCol1,cCol2)][clamVar]=cFunc(cCol1,cCol2,0)
            isFirst=False
        self.outArray=outArray 
        for (cObj,colVals) in outArray.items():
            finalOut[colNames[0]]+=['"'+cObj[0]+'"']
            finalOut[colNames[1]]+=['"'+cObj[1]+'"']
            
            for cVarIn in self.keys()+kwArgs.keys():
                cVar=rFriendlyName(cVarIn)
                if colVals.has_key(cVar): 
                    if type(colVals[cVar]) is type(''):
                        finalOut[cVar]+=['"'+colVals[cVar]+'"']
                    else:
                        finalOut[cVar]+=[colVals[cVar]]
                else: finalOut[cVar]+=[0]
        self.finalOut=finalOut
        return r.data_frame(**finalOut)        
    def _rdataframe(self,nVar,**kwArgs):
        tempOut={'Sample':[],'Measurement':[],'Value':[]}
        for cCol in kwArgs.keys(): tempOut[cCol]=[]
        for (cSample,cData) in self[nVar].items(): 
            for (cMeas,cVal) in cData.items(): 
                tempOut['Sample']+=['"'+cSample+'"']
                
                tempOut['Measurement']+=['"'+cMeas+'"']
                tempOut['Value']+=[cVal]
                for (cCol,cFunc) in kwArgs.items(): tempOut[cCol]+=[cFunc(cSample,cMeas,cVal)]
        self.tempOut=tempOut
        return r.data_frame(**tempOut)
    def iccplot(self,nVar=None,boxplot=False,dualplot=False,xlab=None,plotForm='Value~Sample',drawLine=True,**kwArgs):
        if nVar is None: 
            for cVar in sorted(self.keys()):
                self.iccplot(cVar,plotForm,boxplot=boxplot,dualplot=dualplot,xlab=xlab,drawLine=drawLine,**kwArgs)
        else: 
            nVar=self._inexactMatch(nVar)
            r.setwd('"%s"'%os.path.abspath('.'))
            cName=seqName()
            iccVal=self.icc(nVar).sage()['DATA']['value']
            cData=self._rdataframe(nVar,**kwArgs)
            yName=latexFriendlyName(nVar)
            if drawLine: linfit=r.lm(plotForm,data=cData)
            plotName=yName.join(plotForm.split('Value'))+': ICC:'+str(iccVal)
            # Additional Plot Arguments
            plotArgs={}
            if xlab is not None: plotArgs['xlab']=xlab
            
            for outImgFormat in ['png','pdf']:
                r.eval(outImgFormat+'("'+cName+'.'+outImgFormat+'")')
                if boxplot: r.boxplot(plotForm,data=cData,main='"'+plotName+'"',ylab='"'+yName+'"')
                else: 
                    if dualplot:
                        r.beanplot(plotForm,data=cData,main='"'+plotName+'"',ylab='"'+yName+'"',side='"both"',col = 'list("black", c("grey", "white"))',**plotArgs)
                    else:
                        r.beanplot(plotForm,data=cData,main='"'+plotName+'"',ylab='"'+yName+'"')
                if drawLine: 
                    print linfit.name()+'$fitted.values~'+plotForm.split('~')[1]
                    r.par(new=True)
                    r.plot(linfit.name()+'$fitted.values~'+plotForm.split('~')[1],data=cData,col="red")
                
                
                r.dev_off()             
    def icc(self,nVar=None):
        if nVar is None: 
            for cVar in sorted(self.keys()):
                print latexFriendlyName(cVar) 
                print self.icc(cVar)
        else:
            
            try:
                # Transpose the Table for ICC
                self.tempTable=kdict()
                for key1 in self[nVar].keys():
                    for key2 in self[nVar][key1].keys():
                        self.tempTable[key2][key1]=self[nVar][key1][key2]
                return r.icc(r.data_frame(*self.tempTable.tup()),model='"t"')
            except:
                print 'Key '+str(nVar)+' was not found in : '+str(self.keys())
                return r.icc(r.data_frame([0,0],[0,0],[0,0]))           

def RSQLBeanPlot(cPlotVar,cPlotVarY,pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,norm=False,stdVal='',imgDPI=72,cPlotVarY2='',cPlotW='',useEbar=True,useR2=True,plotEbar=False,cGroupVar=None,fixVals=None,logx=False,logy=False,isInt=False):
    """SQLHistPlot(cPlotVar,cPlotVarY,pName='',pXlabel='',pYlabel='',bins=20,sqlAdd='',drawGraph=True,myalph=1,norm=True,stdVal='',imgDPI=72,cPlotVarY2='',cPlotW='',useEbar=True,useR2=True,plotEbar=False)
    cPlotVarY2 does a correlation between the two variables
    cPlotW uses a weighted mean and std 
    """
    
    if pXlabel=='': pXlabel=ptName(cPlotVar)
    if pYlabel=='': pYlabel=ptName(cPlotVarY)
    if sqlAdd!='': sqlAdd=' AND '+sqlAdd
    
    curSQLcmd=','.join([cPlotVar,cPlotVarY])
    
    fullSQL='SELECT '+curSQLcmd+' FROM Lacuna WHERE Project="'+projectTitle+'" '+sqlAdd
    #+' SORT BY '+cPlotVar
    return fullSQL
    r.eval('cQry<- dbGetQuery(dbCon, "'+rStr(oStr)+'")')
    
    
    #if norm==1: xVals=xVals/sum(xVals)
    #elif norm==2: xVals=xVals/xVals[0]*100
    #elif norm==3: xVals=xVals/max(xVals)
    #if useEbar: errorbar(xBins,xVals,yerr=eVals)
    #elif plotEbar: plot(xBins,eVals) 
    #elif logx: semilogx(xBins,xVals,linewidth=3)
    #elif logy: semilogy(xBins,xVals,linewidth=3)
    #else: plot(xBins,xVals,linewidth=3)

    #if drawGraph:
    #    title(pName)
    #    xlabel(pXlabel)
    #    ylabel(pYlabel)
    #    savefig('histogram-'+strcsv(pName),dpi=imgDPI)
    #    savefig('histogram-'+strcsv(pName)+'.eps',dpi=imgDPI)
    #    close()
lastVal='eee'
def rSumTable(rSumName):
    rSumObj=r.summary(rSumName)._sage_()
    outText='<table border="1"><tr><td>'
    outText+='</td><td>'.join(['']+[cObj.strip() for cObj in rSumObj['_Dimnames']['#1']])
    outText+='</td></tr>'
    
    def clrNone(y):
        if y is None: 
            if globals()['lastVal'] is y: return ''
            else:
                globals()['lastVal']=y 
                return '&&&&'
        else: 
            globals()['lastVal']=y
            return y
    cOutput='$$$$'.join(map(clrNone,rSumObj['DATA']))
    cGrid=cOutput.split('&&&&')
    cRowHeaders=['Min.','1st Qu.','Median','Mean','3rd Qu.','Max.','']
    for cRow in range(0,7):
        outText+='<tr><td>'+cRowHeaders[cRow]+'</td>'
        for cCol in range(len(cGrid)):
            tGrid=filter(lambda y: len(y.strip())>0,cGrid[cCol].split('$$$$'))
            outText+='<td>'
            if len(tGrid)>cRow: 
                cVal=tGrid[cRow].strip()
                if cVal.find(':')>=0: cVal=cVal.split(':')[1]
                outText+=cVal
            outText+='</td>'
        outText+='</tr>'
    outText+='</table>'
    html(outText)