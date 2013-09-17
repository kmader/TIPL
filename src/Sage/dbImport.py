import socket,numpy,os,sys
class CaseFreeDict:
    """Dictionary, that has case-insensitive keys.
    
    Keys are retained in their original form
    when queried with .keys() or .items().

    Implementation: An internal dictionary maps lowercase
    keys to (key,value) pairs. All key lookups are done
    against the lowercase keys, but all methods that expose
    keys to the user retrieve the original keys."""
    
    def __init__(self, dict=None):
        """Create an empty dictionary, or update from 'dict'."""
        self._dict = {}
        if dict:
            self.update(dict)

    def __getitem__(self, key):
        """Retrieve the value associated with 'key' (in any case)."""
        k = key.lower()
        return self._dict[k][1]

    def __setitem__(self, key, value):
        """Associate 'value' with 'key'. If 'key' already exists, but
        in different case, it will be replaced."""
        k = key.lower()
        self._dict[k] = (key, value)

    def has_key(self, key):
        """Case insensitive test wether 'key' exists."""
        k = key.lower()
        return self._dict.has_key(k)

    def keys(self):
        """List of keys in their original case."""
        return [v[0] for v in self._dict.values()]

    def values(self):
        """List of values."""
        return [v[1] for v in self._dict.values()]

    def items(self):
        """List of (key,value) pairs."""
        return self._dict.values()

    def get(self, key, default=None):
        """Retrieve value associated with 'key' or return default value
        if 'key' doesn't exist."""
        try:
            return self[key]
        except KeyError:
            return default

    def setdefault(self, key, default):
        """If 'key' doesn't exists, associate it with the 'default' value.
        Return value associated with 'key'."""
        if not self.has_key(key):
            self[key] = default
        return self[key]

    def update(self, dict):
        """Copy (key,value) pairs from 'dict'."""
        for k,v in dict.items():
            self[k] = v

    def __repr__(self):
        """String representation of the dictionary."""
        items = ", ".join([("%r: %r" % (k,v)) for k,v in self.items()])
        return "{%s}" % items

    def __str__(self):
        """String representation of the dictionary."""
        return repr(self)
class mysqlCurWrapper:
    """ A wrapper for the my sql function to remove case sensitivity (make everything uppercase) and return the cursor when queries are executed -> cur.execute(...).fetchall() now works """
    def __init__(self,rCon):
        self.__connection__=rCon
        self.__cursor__=rCon.cursor()
    def _fixquery(self,qryText,qryData):
        qryText=''.join(qryText.upper().split('BEGIN;'))
        qryText=''.join(qryText.upper().split('BEGIN ;'))
        qryText=''.join(qryText.upper().split('BEGIN'))
        qryText=''.join(qryText.upper().split('COMMIT;'))
        qryText=''.join(qryText.upper().split('COMMIT ;'))
        qryText=''.join(qryText.upper().split('COMMIT'))
        qrySplit=qryText.split('?')
        
        return '%s'.join(qrySplit)
        
        qrySplit.reverse()
        qryOut=qrySplit.pop()
        qrySplit.reverse()
        for (val,qryPostfix) in zip(qryData,qrySplit):
            tv=type(val)
            if True: qryOut+='%s' # very boring options
            elif tv is type(''): qryOut+='%s'
            elif tv is type(1): qryOut+='%s'
            elif tv is int: qryOut+='%d'
            elif tv is type(1.0): qryOut+='%f'
            elif tv is float: qryOut+='%f'
            else: qryOut+='%s'
            qryOut+=qryPostfix
        
        return qryOut
    def execute(self,*args,**keywords):
        nargs=list(args)
        nargs[0]=nargs[0].upper()
        if len(nargs)>1: nargs[0]=self._fixquery(nargs[0],nargs[1])
        self.__cursor__.execute(*tuple(nargs),**keywords)
        return self
    def executemany(self,*args,**keywords):
        nargs=list(args)
        nargs[0]=nargs[0].upper()
        if len(nargs)>1: nargs[0]=self._fixquery(nargs[0],nargs[1][0])
        self.__cursor__.executemany(*nargs,**keywords)
        return self
    def begin(self):
        return self.__connection__.begin()
    def commit(self):
        return self.__connection__.commit()
    def __getattr__(self,what):
        #print (what,type(what),'is missing checking mysql')
        try:
            return getattr(self.__cursor__,what)
        except:
            return getattr(self.__connection__,what)
def StartDatabase(dbName='lacuna',mysql=True,debugMode=False):
    hostComputer=socket.gethostname().upper()
    if debugMode: dbName=dbName+'-Debug'
    globals()['homeDb']=('tomquant.sql.psi.ch','tomquant_rw','8y0jz0','tomquant')
    print 'Sage is Running on : '+hostComputer+' using database - '+str(globals()['homeDb'])
    import MySQLdb as mdb
    con=mdb.connect(*globals()['homeDb'])
    cur=mysqlCurWrapper(con)
    lacTemp={}
    return (con,cur)
# Loopkup the current sample number and if it does not exist, make it
def getSampleNum(cur,sampleName,projNum,tries=0):
    if tries>2: 
        print 'getSampleNum has failed, please check the integrity of the database'
        return -1
    cQry=cur.execute('SELECT SAMPLE_AIM_NUMBER,SAMPLE_AIM_NAME FROM SAMPLE WHERE PROJECT_NUMBER=? AND SAMPLE_AIM_NAME LIKE ?',(int(projNum),sampleName)).fetchall()
    print cQry
    if (len(cQry)>0): is_match=cQry[0][0]
    else: is_match=0
    if (is_match < 1):
        try: 
            cur.execute('INSERT INTO SAMPLE (PROJECT_NUMBER,SAMPLE_AIM_NAME) VALUES (?,?)',(projNum,sampleName))
        except:
            cur.execute('ROLLBACK')
        return getSampleNum(cur,sampleName,projNum,tries+1)
 
    else: return is_match  
      
# Loopkup the current project number and if it does not exist, make it

def getProjNum(cur,projName,tries=0):
    if tries>2: 
        print 'getProjNum has failed, please check the integrity of the database'
        return -1
    cQry=cur.execute('SELECT PROJECT_NUMBER,PROJECT_NAME FROM PROJECT WHERE PROJECT_NAME LIKE ?',(projName,)).fetchall()
    print cQry
    if (len(cQry)>0): is_match=cQry[0][0]
    else: is_match=0
    if (is_match < 1):
        cur.execute('INSERT INTO PROJECT (PROJECT_NAME) VALUES (?)',(projName,))
        try: 
            print 4
        except:
            cur.execute('COMMIT')
        return getProjNum(cur,projName,tries+1)
    else: return is_match
from numpy import *    
import numpy
def LoadCSVFile(filename):
    try:
        rawtext=''.join(open(filename).readlines())
    except:
        print filename+' is garbage'
    try:
        (outrows,a,b)=parseCSV(rawtext)
        if outrows>2:
            return (a,b)
        else:
            print cFile+' is too short!'
    except:
        print filename+' is junk:'+rawtext[0:20] 
def lacpa_adddb(cur,ptList,oTable,rawFilename,processName=True,tableName='Lacuna',CanalMode=0,projectTitle='None'):
    lacNumOffset=0
    if processName: (filename,lacNumOffset)=processInputName(rawFilename,lacFilename)
    else: filename=rawFilename
    ptList=CaseFreeDict(ptList)
    dbLen=len(ptList['SCALE_X'])
    if not oTable.has_key('SAMPLE'): 
        oTable['SAMPLE']=''
        print filename+' is missing sample name' 
    dx=numpy.median(ptList['SCALE_X'])
    dy=numpy.median(ptList['SCALE_Y'])
    dz=numpy.median(ptList['SCALE_Z'])
    dr=sqrt(dx**2+dy**2+dz**2)
    lacTemp={}
    if type(projectTitle) is type(''): cProjNum=getProjNum(cur,projectTitle)
    else: cProjNum=projectTitle
    cSampleNum=getSampleNum(cur,filename,cProjNum)
    lacTemp['SAMPLE_AIM_Number']=(cSampleNum,)*dbLen
    lacTemp['Project_Number']=(cProjNum,)*dbLen
    lacunIds=[lacId+lacNumOffset for lacId in ptList['Lacuna_NUMBER']]
    lacTemp[tableName+'_Number']=tuple(lacunIds)
    # Variables that scale directly with x,y,z voxel size
    lacTemp['VOX_SIZE']=tuple(numpy.abs(ptList['SCALE_X']*1000))
    scaleVars=['POS','STD','PROJ']
    for cVar in scaleVars:
        for cAx in ['X','Y','Z']:
            lacTemp[cVar+'_'+cAx]=tuple(ptList[cVar+'_'+cAx]*ptList['SCALE_'+cAx]) 
    # This doesnt work since I dont save PCA1,2,3 dumb
    # Variables that scale with PCA 1,2,3 voxel size * denotes PCA1, PCA2, PCA3
    pcaScaleVars=['*_S','PROJ_*']
    for cAx in ['PCA1','PCA2','PCA3']:
        cDr=numpy.sqrt((ptList[cAx+'_X']*dx)**2+(ptList[cAx+'_Y']*dy)**2+(ptList[cAx+'_Z']*dz)**2)
        for cVar in pcaScaleVars:
            rcVar=cAx.join(cVar.split('*'))
            lacTemp[rcVar]=tuple(ptList[rcVar]*cDr)
	# Normal Variables 
    normalVars= ['PCA1_X','PCA1_Y','PCA1_Z','PCA2_X','PCA2_Y','PCA2_Z']
    normalVars+=['MASK_GRAD_X','MASK_GRAD_Y','MASK_GRAD_Z','MASK_ANGLE']
    if CanalMode==0: normalVars+=['Canal_ANGLE','Canal_GRAD_X','Canal_GRAD_Y','Canal_GRAD_Z']
    for cVar in normalVars:
        if ptList.has_key(cVar): lacTemp[cVar]=tuple(ptList[cVar])
        elif ((cVar.find('GRAD')>=0) | (cVar.find('ANGLE'))): lacTemp[cVar]=(-1,)*dbLen
        else: print 'Missing important column:'+cVar+', what the frick!'    
    # Variables that require a radial scaling factor
    radialVars=['MASK_DISTANCE_MEAN','MASK_DISTANCE_STD'] # 'MASK_DISTANCE_COV'
    radialVars+=['OBJ_RADIUS','OBJ_RADIUS_STD']
    if CanalMode==0:
        if ptList.has_key(cVar): radialVars+=['Canal_DISTANCE_MEAN','Canal_DISTANCE_STD']
    for cVar in radialVars:
        if ptList.has_key(cVar): lacTemp[cVar]=tuple(numpy.abs(ptList[cVar]*dr))
    # Variables that require a radial cubed scaling factor
    volVars=['VOLUME','VOLUME_BOX']        
    for cVar in volVars:
        lacTemp[cVar]=tuple(numpy.abs(ptList[cVar]*dx*dy*dz))
    if ptList.has_key('SHELL_CNT'):
        lacTemp['VOLUME_LAYER']=tuple(numpy.abs((ptList['VOLUME']-ptList['SHELL_CNT'])*dx*dy*dz))   
    # GrayAnalysis Columns
    if ptList.has_key('MASK'): # new Lacuna method
        lacTemp['MASK_DISTANCE_MEAN']=tuple(numpy.abs(ptList['MASK']*dr))
        lacTemp['MASK_DISTANCE_STD']=tuple(numpy.abs(ptList['MASK_STD']*dr))        
    if ptList.has_key('MASK_WX'):
        lacTemp['MASK_GRAD']=tuple(ptList['MASK'])
        lacTemp['MASK_DISTANCE_STD']=tuple(ptList['MASK_STD']) 
    if ptList.has_key('SHELL_ABSORPTION'):
        lacTemp['SHELL_ABSORPTION']=tuple(ptList['SHELL_ABSORPTION']) 
    if ptList.has_key('SHELL_ABSORPTION_STD'):
        lacTemp['SHELL_ABSORPTION_STD']=tuple(ptList['SHELL_ABSORPTION_STD'])
    else:
        lacTemp['SHELL_ABSORPTION']=(-1,)*dbLen
        lacTemp['SHELL_ABSORPTION_STD']=(-1,)*dbLen
    # Lining Absorption
    if ptList.has_key('LINING_ABSORPTION'):
        lacTemp['LINING_ABSORPTION']=tuple(ptList['LINING_ABSORPTION']) 
    if ptList.has_key('LINING_ABSORPTION_STD'):
        lacTemp['LINING_ABSORPTION_STD']=tuple(ptList['LINING_ABSORPTION_STD'])
    else:
        lacTemp['LINING_ABSORPTION']=(-1,)*dbLen
        lacTemp['LINING_ABSORPTION_STD']=(-1,)*dbLen   
    if CanalMode==0:
        # This doesnt work since I dont save PCA1,2,3 dumb
        # Variables that scale with PCA 1,2,3 voxel size * denotes PCA1, PCA2, PCA3
        for cAx in ['PCA1','PCA2','PCA3']:
            cDr=numpy.sqrt((ptList[cAx+'_X']*dx)**2+(ptList[cAx+'_Y']*dy)**2+(ptList[cAx+'_Z']*dz)**2)
            rcVar='DENSITY_PROJ_'+cAx
            if ptList.has_key('DENSITY_VOLUME_PROJ_'+cAx):
                lacTemp[rcVar]=tuple(ptList[rcVar]*cDr)
            else:
                lacTemp[rcVar]=(-1,)*dbLen
        if ptList.has_key('Canal_NUMBER'):
            lacTemp['Canal_NUMBER']=tuple(ptList['Canal_NUMBER'])
            #lacTemp['Canal_NAME']=tuple([projectTitle+'_'+filename+'_CAN_'+str(int(curCan)) for curCan in ptList['Canal_NUMBER']])
        if ptList.has_key('Canal_NUMBER_STD'):
            lacTemp['Canal_NUMBER_STD']=tuple(ptList['Canal_NUMBER_STD'])
        else:
            lacTemp['Canal_NUMBER_STD']=(-1,)*dbLen
        # Nearest Neighbors
        lacTemp['NEAREST_NEIGHBOR_DISTANCE']=(-1,)*dbLen
        lacTemp['NEAREST_NEIGHBOR_ANGLE']=(-1,)*dbLen
        
        if ptList.has_key('NEIGHBORS'):
            lacTemp['NEAREST_NEIGHBOR_NEIGHBORS']=tuple(ptList['NEIGHBORS'])
        else:
            lacTemp['NEAREST_NEIGHBOR_NEIGHBORS']=(-1,)*dbLen
        # Mask Params
        lacTemp['POS_RADIUS']=(-1,)*dbLen
        lacTemp['MASK_RADIUS']=(-1,)*dbLen
        lacTemp['MASK_RADIUS_MIN']=(-1,)*dbLen
        lacTemp['MASK_RADIUS_MAX']=(-1,)*dbLen
        lacTemp['MASK_RADIUS_MEAN']=(-1,)*dbLen
        lacTemp['MASK_THETA']=(-1,)*dbLen
    if ptList.has_key('THICKNESS'):
        lacTemp['THICKNESS']=tuple(ptList['THICKNESS'])
    else:
        lacTemp['THICKNESS']=(-1,)*dbLen    
    if ptList.has_key('THICKNESS_STD'):
        lacTemp['THICKNESS_STD']=tuple(ptList['THICKNESS_STD'])
    else:
        lacTemp['THICKNESS_STD']=(-1,)*dbLen   
    # Lacuna Density / Volume
    if ptList.has_key('DENSITY_VOLUME'):
        lacTemp['DENSITY_VOLUME']=tuple(numpy.abs(ptList['DENSITY_VOLUME']*dx*dy*dz))    
        lacTemp['DENSITY']=tuple(numpy.abs(1/(ptList['DENSITY_VOLUME']*dx*dy*dz)))
    elif ptList.has_key('DENSITY_VOLUME_CNT'):
        lacTemp['DENSITY_VOLUME']=tuple(numpy.abs(ptList['DENSITY_VOLUME_CNT']*dx*dy*dz))    
        lacTemp['DENSITY']=tuple(numpy.abs(1/(ptList['DENSITY_VOLUME_CNT']*dx*dy*dz)))
    else:
        lacTemp['DENSITY_VOLUME']=(-1,)*dbLen
        lacTemp['DENSITY']=(-1,)*dbLen
    if CanalMode==0:
        # Lacuna Territory Shape
        lacTemp['DISPLACEMENT_MEAN']=(-1,)*dbLen
        if ptList.has_key('NEIGHBOR_AREA'):
            lacTemp['DENSITY_VOLUME_SHELL']=tuple(numpy.abs(ptList['NEIGHBOR_AREA']*dx*dy))
        elif ptList.has_key('MASK_VOLUME_SHELL_CNT'):
            ## Old Definition of Shell
            lacTemp['DENSITY_VOLUME_SHELL']=tuple(numpy.abs(ptList['MASK_VOLUME_SHELL_CNT']*dx*dy*dz))    
        else:
            lacTemp['DENSITY_VOLUME_SHELL']=(-1,)*dbLen
        # Lacuna Territory that is mineralized    
        if ptList.has_key('BONE_VOLUME_CNT'):
            lacTemp['DENSITY_VOLUME_BONE']=tuple(numpy.abs(ptList['BONE_VOLUME_CNT']*dx*dy*dz))    
        else:
            lacTemp['DENSITY_VOLUME_BONE']=(-1,)*dbLen         
        # Lacuna Territory that is part of the mask (for porosity calculations)
        if ptList.has_key('MASK_VOLUME_CNT'):
            lacTemp['DENSITY_VOLUME_MASK']=tuple(numpy.abs(ptList['MASK_VOLUME_CNT']*dx*dy*dz))    
        else:
            lacTemp['DENSITY_VOLUME_MASK']=(-1,)*dbLen
        # PCA1 is a makeshift holding place for STD until the table is once again updated
        terrShapeMap={'DENSITY_VOLUME_C':'DENSITY_','DENSITY_VOLUME_S':'DENSITY_STD_'}
        for cKey in terrShapeMap.keys():
            missingKeys=False
            for cAx in ['X','Y','Z']:
                #print cKey+cAx
                if ptList.has_key(cKey+cAx):
                    #print 'isch da'
                    lacTemp[terrShapeMap[cKey]+cAx]=tuple(ptList[cKey+cAx]*ptList['SCALE_'+cAx])
                else:
                    if cKey=='DENSITY_VOLUME_C': missingKeys=True
                    else:
                        lacTemp[terrShapeMap[cKey]+cAx]=(-1,)*dbLen
            if not missingKeys:
                if cKey=='DENSITY_VOLUME_C':
                    dispMean=numpy.sqrt(((ptList[cKey+'X']-ptList['POS_X'])*dx)**2+((ptList[cKey+'Y']-ptList['POS_Y'])*dy)**2+((ptList[cKey+'Z']-ptList['POS_Z'])*dz)**2)
                    lacTemp['DISPLACEMENT_MEAN']=tuple(dispMean)
                    lacTemp['DISPLACEMENT_X']=tuple((ptList[cKey+'X']-ptList['POS_X'])*dx)
                    lacTemp['DISPLACEMENT_Y']=tuple((ptList[cKey+'Y']-ptList['POS_Y'])*dy)
                    lacTemp['DISPLACEMENT_Z']=tuple((ptList[cKey+'Z']-ptList['POS_Z'])*dz)
    # Polar Coordinates Hints
    # Only really valid for Full Femur, but Lacuna angle can be useful
    mR=numpy.sqrt(((ptList['POS_X']-numpy.mean(ptList['POS_X']))*dx)**2+((ptList['POS_Y']-numpy.mean(ptList['POS_Y']))*dy)**2)
    for cPCA in [1,2]:
        pR=numpy.sqrt(ptList['PCA'+str(cPCA)+'_X']**2+ptList['PCA'+str(cPCA)+'_Y']**2)
        pPhi=180/pi*numpy.arctan2(ptList['PCA'+str(cPCA)+'_Z'],pR)
        lacTemp['PCA'+str(cPCA)+'_Phi']= tuple(pPhi)
        lacTemp['PCA'+str(cPCA)+'_Theta']=tuple(180/pi*numpy.arccos(ptList['PCA'+str(cPCA)+'_X']/pR)) # update
    # Junk Angles
    lacTemp['POS_THETA']=(-1,)*dbLen
    lacTemp['MASK_THETA']=(-1,)*dbLen
    lacTemp['POS_DISTANCE']=(-1,)*dbLen
    lacTemp['NEAREST_NEIGHBOR_AVG']=(-1,)*dbLen
    lacTemp['NEAREST_NEIGHBOR_DISTANCE']=(-1,)*dbLen
    lacTemp['NEAREST_NEIGHBOR_DISTANCE']=(-1,)*dbLen
    # Normalize PCA
    pcastot=dr*numpy.sqrt(ptList['PCA1_S']**2+ptList['PCA2_S']**2+ptList['PCA3_S']**2)
    #lacTemp['PCAS_TOTAL']=tuple(pcastot)
    #for tz in lacTemp.keys(): print tz+' '+str(len(lacTemp[tz]))
    outKeys=lacTemp.keys()
    outArr=[lacTemp[cKey] for cKey in outKeys]
    #for cKey in outKeys: print (cKey,len(lacTemp[cKey]))
    outMat=numpy.array(outArr).swapaxes(1,0)
    invalidRows=numpy.sum(numpy.isnan(outMat),1)
    outMat=outMat[numpy.nonzero(invalidRows==0)[0],:]
    globals()['Om']=outMat
    outMat=[tuple(obj) for obj in outMat]
    addRowsToTable(cur,tableName,outMat,entry_order=outKeys)
    print filename+' was successfully entered %05d, invalid  %03d' % (lacNumOffset,sum(invalidRows))

def addRowsToTable(cur,tableName,outMat,entry_order=[]):
    #globals()['test']=(tableName,outMat,entry_order)
    # old command
    #lacDB.add_rows(tableName,outMat,entry_order=entry_order)
    print (len(entry_order),numpy.array(outMat[0]).shape)
    cur.execute('BEGIN')
    sqlString='INSERT INTO '+tableName+' ('+','.join(entry_order)+') VALUES ('+','.join(['?']*len(entry_order))+');'
    cur.executemany(sqlString,outMat)
    #print(sqlString)
    #for cRow in outMat: 
    #    globals()['cRow']=cRow
    #    cur.execute(sqlString,cRow)
    cur.execute('COMMIT')
def parseCSV(text,filename=''):
    def temp_clean(text):
        return (''.join(text.split('/'))).upper().strip()    
    def temp_parse(temp):
        ntemp=[]
        errCount=0
        for val in temp:
            if val.strip().upper()=='NAN':
                cval='0'
                errCount+=1
            else:
                cval=val
            try:
                cval=single(cval)
            except:
                cval=-1
                errCount+=1        
            ntemp+=[cval]
        return (ntemp,errCount) 
    rows=text.split('\n')
    # First row is header
    head1=rows[0]
    newStr=[cEle.strip().split(':') for cEle in head1[head1.find('//')+1:].strip().split(',')]
    fileDict={}
    for cEle in newStr: fileDict[temp_clean(cEle[0])]=cEle[1].split('/')[-1].strip()
    fTime=True
    head2=rows[1]
    head2=[temp_clean(cEle) for cEle in head2[head2.find('//')+1:].strip().split(',')]
    # Check for duplicates in header string (and just use the last entry)
    # Generate a dictionary of all header entries
    cleanHeader={}
    for k in range(len(head2)):
        cleanHeader[head2[k]]=k
    # create a new null filled header
    head2=['NULL']*len(head2)
    # use the dictionary to repopulate the head2 entry
    for cKey in cleanHeader.keys(): head2[cleanHeader[cKey]]=cKey  
    outTable={}
    for col in head2: outTable[col]=[]
    for row in rows[2:]:
        temp=row.split(',')
        try:
            (ntemp,errs)=temp_parse(temp)
            if errs<2:
                if len(ntemp)==len(head2):
                    for k in range(0,len(head2)): outTable[head2[k]]+=[ntemp[k]]     
        except:
            #if fTime: print (len(ntemp),len(head2))
            fTime=False
            temp=[]
    for col in head2: outTable[col]=numpy.array(outTable[col])
    outrows=len(outTable[head2[0]])
    print 'Parsed .. '+str(outrows)+' of '+str(len(rows))
    return (outrows,fileDict,outTable)

def InsertCSV(filename):
    (a,b)=LoadCSVFile(filename)
    lacpa_adddb(cur,b,a,filename,False)
(con,cur)=StartDatabase('lacuna')
#InsertCSV('/gpfs/home/mader/Pistone/mi3_00_/labels.tif_clpor_2.csv')
