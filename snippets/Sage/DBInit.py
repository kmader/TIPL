import os
# from DBTools import *
# import ctypes
# dll = ctypes.CDLL("/usr/local/mysql/lib/libmysqlclient.18.dylib")
# import MySQLdb as mdb
# Project Table
table_skel_proj = {}
table_skel_proj['Project_Number']={'sql':'INTEGER','primary_key':True,'unique':True}
table_skel_proj['Project_Name']={'sql':'TEXT'}

# Sample Table
table_skel_sample = {}
table_skel_sample['Project_Number']={'sql':'INTEGER'}
table_skel_sample['Sample_AIM_Number']={'sql':'INTEGER','primary_key':True,'unique':True}
table_skel_sample['Sample_AIM_Name']={'sql':'TEXT'}
table_skel_sample['Data_Path']={'sql':'TEXT'}

# Canal Table
table_skel_Canal = {}
table_skel_Canal['Project_Number']={'sql':'INTEGER'}
table_skel_Canal['Sample_Aim_Number']={'sql':'INTEGER'}
table_skel_Canal['Canal_Number']={'sql':'INTEGER'}
# Canal Unique Identifier (1e4*Project+Sample_Name_Number)*1e4+Canal_Number

table_skel_Canal['Vox_Size']={'sql':'REAL'}
# pos is original position in AIM file
table_skel_Canal['POS_X']={'sql':'REAL'}
table_skel_Canal['POS_Y']={'sql':'REAL'}
table_skel_Canal['POS_Z']={'sql':'REAL'}
# position theta is the theta when x,y-cov(x,y)
# but without the offset
table_skel_Canal['POS_THETA']={'sql':'REAL'}
table_skel_Canal['POS_RADIUS']={'sql':'REAL'}
table_skel_Canal['POS_DISTANCE']={'sql':'REAL'}
# corrected parameters
table_skel_Canal['Mask_Radius_MIN']={'sql':'REAL'}
table_skel_Canal['Mask_Radius_MAX']={'sql':'REAL'}
table_skel_Canal['Mask_Radius_MEAN']={'sql':'REAL'}
table_skel_Canal['Mask_Theta']={'sql':'REAL'}

# COV is center of mass cordinates
#table_skel_Canal['COV_X']={'sql':'REAL'}
#table_skel_Canal['COV_Y']={'sql':'REAL'}
#table_skel_Canal['COV_Z']={'sql':'REAL'}


# COV_THETA is the theta offset
#table_skel_Canal['COV_THETA']={'sql':'REAL'}

table_skel_Canal['STD_X']={'sql':'REAL'}
table_skel_Canal['STD_Y']={'sql':'REAL'}
table_skel_Canal['STD_Z']={'sql':'REAL'}
# PCA Analysis with raw components
table_skel_Canal['PCA1_X']={'sql':'REAL'}
table_skel_Canal['PCA1_Y']={'sql':'REAL'}
table_skel_Canal['PCA1_Z']={'sql':'REAL'}
table_skel_Canal['PCA1_S']={'sql':'REAL'}
# second
table_skel_Canal['PCA2_X']={'sql':'REAL'}
table_skel_Canal['PCA2_Y']={'sql':'REAL'}
table_skel_Canal['PCA2_Z']={'sql':'REAL'}
table_skel_Canal['PCA2_S']={'sql':'REAL'}
# third pca
table_skel_Canal['PCA3_X']={'sql':'REAL'}
table_skel_Canal['PCA3_Y']={'sql':'REAL'}
table_skel_Canal['PCA3_Z']={'sql':'REAL'}
table_skel_Canal['PCA3_S']={'sql':'REAL'}
table_skel_Canal['PCAS_Total']={'sql':'REAL'}
# Maximum Extent of Bone in X,Y,Z and PCA transformed X,Y,Z (pseudo, and real length, width, height)
table_skel_Canal['PROJ_X']={'sql':'REAL'}
table_skel_Canal['PROJ_Y']={'sql':'REAL'}
table_skel_Canal['PROJ_Z']={'sql':'REAL'}
table_skel_Canal['PROJ_PCA1']={'sql':'REAL'}
table_skel_Canal['PROJ_PCA2']={'sql':'REAL'}
table_skel_Canal['PROJ_PCA3']={'sql':'REAL'}

# Gradient to Mask
table_skel_Canal['Mask_Angle']={'sql':'REAL'}
table_skel_Canal['Mask_Grad_X']={'sql':'REAL'}
table_skel_Canal['Mask_Grad_Y']={'sql':'REAL'}
table_skel_Canal['Mask_Grad_Z']={'sql':'REAL'}
table_skel_Canal['Mask_Distance_Mean']={'sql':'REAL'}
table_skel_Canal['Mask_Distance_STD']={'sql':'REAL'}
# entirely synthetic variables

table_skel_Canal['Obj_Radius']={'sql':'REAL'}
table_skel_Canal['Obj_Radius_Std']={'sql':'REAL'}

table_skel_Canal['Volume']={'sql':'REAL'}

table_skel_Canal['Volume_Layer']={'sql':'REAL'}
table_skel_Canal['Volume_Layer_2']={'sql':'REAL'}
table_skel_Canal['Volume_Box']={'sql':'REAL'}

#
table_skel_Canal['Thickness']={'sql':'REAL'}
table_skel_Canal['Thickness_STD']={'sql':'REAL'}

# Density Parameters (from shape, or lacbk)
table_skel_Canal['Density']={'sql':'REAL'}
table_skel_Canal['Density_Volume']={'sql':'REAL'}

# Absorption Values 
table_skel_Canal['Shell_Absorption']={'sql':'REAL'}
table_skel_Canal['Shell_Absorption_Std']={'sql':'REAL'}
table_skel_Canal['Lining_Absorption']={'sql':'REAL'}
table_skel_Canal['Lining_Absorption_Std']={'sql':'REAL'}
table_skel_Canal['Shell_Volume']={'sql':'REAL'}

table_skel_Canal['Volume_Deformed']={'sql':'REAL'}
table_skel_Canal['Vascularization']={'sql':'REAL'}

# use polar coordinates
table_skel_Canal['PCA1_Theta']={'sql':'REAL'}
table_skel_Canal['PCA1_Phi']={'sql':'REAL'}
table_skel_Canal['PCA2_Theta']={'sql':'REAL'}
table_skel_Canal['PCA2_Phi']={'sql':'REAL'}

# Lacuna Table
table_skel = {}
table_skel['Project_Number']={'sql':'INTEGER'}
table_skel['Sample_Aim_Number']={'sql':'INTEGER'}
table_skel['Lacuna_Number']={'sql':'INTEGER'}
table_skel['Vox_Size']={'sql':'REAL'}
# pos is original position in AIM file
table_skel['POS_X']={'sql':'REAL'}
table_skel['POS_Y']={'sql':'REAL'}
table_skel['POS_Z']={'sql':'REAL'}
# position theta is the theta when x,y-cov(x,y)
# but without the offset
table_skel['POS_THETA']={'sql':'REAL'}
table_skel['POS_RADIUS']={'sql':'REAL'}
table_skel['POS_DISTANCE']={'sql':'REAL'}
# corrected parameters
table_skel['Mask_Radius_MIN']={'sql':'REAL'}
table_skel['Mask_Radius_MAX']={'sql':'REAL'}
table_skel['Mask_Radius_MEAN']={'sql':'REAL'}
table_skel['Mask_Theta']={'sql':'REAL'}
# COV is center of mass cordinates
#table_skel['COV_X']={'sql':'REAL'}
#table_skel['COV_Y']={'sql':'REAL'}
#table_skel['COV_Z']={'sql':'REAL'}

# COV_THETA is the theta offset
#table_skel['COV_THETA']={'sql':'REAL'}


table_skel['STD_X']={'sql':'REAL'}
table_skel['STD_Y']={'sql':'REAL'}
table_skel['STD_Z']={'sql':'REAL'}
# PCA Analysis with raw components
table_skel['PCA1_X']={'sql':'REAL'}
table_skel['PCA1_Y']={'sql':'REAL'}
table_skel['PCA1_Z']={'sql':'REAL'}
table_skel['PCA1_S']={'sql':'REAL'}
# second
table_skel['PCA2_X']={'sql':'REAL'}
table_skel['PCA2_Y']={'sql':'REAL'}
table_skel['PCA2_Z']={'sql':'REAL'}
table_skel['PCA2_S']={'sql':'REAL'}
# third pca
#table_skel['PCA3_X']={'sql':'REAL'}
#table_skel['PCA3_Y']={'sql':'REAL'}
#table_skel['PCA3_Z']={'sql':'REAL'}
table_skel['PCA3_S']={'sql':'REAL'}
#table_skel['PCAS_Total']={'sql':'REAL'}
# Maximum Extent of Bone in X,Y,Z and PCA transformed X,Y,Z (pseudo, and real length, width, height)
table_skel['PROJ_X']={'sql':'REAL'}
table_skel['PROJ_Y']={'sql':'REAL'}
table_skel['PROJ_Z']={'sql':'REAL'}
table_skel['PROJ_PCA1']={'sql':'REAL'}
table_skel['PROJ_PCA2']={'sql':'REAL'}
table_skel['PROJ_PCA3']={'sql':'REAL'}

table_skel['Thickness']={'sql':'REAL'}
table_skel['Thickness_STD']={'sql':'REAL'}


table_skel['Canal_Grad_X']={'sql':'REAL'}
table_skel['Canal_Grad_Y']={'sql':'REAL'}
table_skel['Canal_Grad_Z']={'sql':'REAL'}
table_skel['Canal_Distance_Mean']={'sql':'REAL'}
table_skel['Canal_Distance_STD']={'sql':'REAL'}
table_skel['Canal_Angle']={'sql':'REAL'}
table_skel['Mask_Grad_X']={'sql':'REAL'}
table_skel['Mask_Grad_Y']={'sql':'REAL'}
table_skel['Mask_Grad_Z']={'sql':'REAL'}
table_skel['Mask_Distance_Mean']={'sql':'REAL'}
table_skel['Mask_Distance_STD']={'sql':'REAL'}
table_skel['Mask_Angle']={'sql':'REAL'}

# entirely synthetic variables
table_skel['Mask_Radius']={'sql':'REAL'}
table_skel['Mask_Theta']={'sql':'REAL'}
table_skel['Obj_Radius']={'sql':'REAL'}
table_skel['Obj_Radius_Std']={'sql':'REAL'}


table_skel['Volume']={'sql':'REAL'}

table_skel['Volume_Layer']={'sql':'REAL'}

table_skel['Volume_Box']={'sql':'REAL'}

# Density Parameters (from shape, or lacbk)
# Density alone is mask with Canals removed
table_skel['Density']={'sql':'REAL'}
table_skel['Density_Volume']={'sql':'REAL'}
# Volume of Bone around Lacuna
table_skel['Density_Volume_Bone']={'sql':'REAL'}
# Volume of Mask around Lacuna (for porosity)
table_skel['Density_Volume_Mask']={'sql':'REAL'}
table_skel['Density_Volume_Shell']={'sql':'REAL'}

# Shape Parameters for Density_Volume
table_skel['Density_X']={'sql':'REAL'}
table_skel['Density_Y']={'sql':'REAL'}
table_skel['Density_Z']={'sql':'REAL'}
table_skel['Density_STD_X']={'sql':'REAL'}
table_skel['Density_STD_Y']={'sql':'REAL'}
table_skel['Density_STD_Z']={'sql':'REAL'}

table_skel['Density_PROJ_PCA1']={'sql':'REAL'}
table_skel['Density_PROJ_PCA2']={'sql':'REAL'}
table_skel['Density_PROJ_PCA3']={'sql':'REAL'}

# Nearest Neighbor
table_skel['Nearest_Neighbor_Distance']={'sql':'REAL'}
table_skel['Nearest_Neighbor_Angle']={'sql':'REAL'}

# displacement parameters
table_skel['DISPLACEMENT_X']={'sql':'REAL'}
table_skel['DISPLACEMENT_Y']={'sql':'REAL'}
table_skel['DISPLACEMENT_Z']={'sql':'REAL'}
table_skel['DISPLACEMENT_MEAN']={'sql':'REAL'}

# Neighbors inside Given Ball
table_skel['Nearest_Neighbor_Neighbors']={'sql':'REAL'}
# Used for comparing average parameter in neighborhood to current value
table_skel['Nearest_Neighbor_AVG']={'sql':'REAL'}
# Absorption Values 
table_skel['Shell_Absorption']={'sql':'REAL'}
table_skel['Shell_Absorption_Std']={'sql':'REAL'}
table_skel['Lining_Absorption']={'sql':'REAL'}
table_skel['Lining_Absorption_Std']={'sql':'REAL'}


# FEMap Values 

#table_skel['FEML_SVM_Mean']={'sql':'REAL'}
#table_skel['FEML_SVM_Std']={'sql':'REAL'}
#table_skel['FEML_SED_Max']={'sql':'REAL'}
#table_skel['FEML_SED_Min']={'sql':'REAL'}
#table_skel['FEML_SED_Mean']={'sql':'REAL'}
#table_skel['FEML_SED_Std']={'sql':'REAL'}
#table_skel['FEML_E33_Mean']={'sql':'REAL'}
#table_skel['FEML_E33_Std']={'sql':'REAL'}

#table_skel['VOLUME_DEFORMED']={'sql':'REAL'}

# The parameters for the distance to the nearest Canal
#table_skel['Canal_Name']={'sql':'TEXT'} # Allows Sample and Canal to be linked
table_skel['Canal_Number']={'sql':'INTEGER'}
table_skel['Canal_Number_STD']={'sql':'REAL'}

# use polar coordinates
table_skel['PCA1_Theta']={'sql':'REAL'}
table_skel['PCA1_Phi']={'sql':'REAL'}
table_skel['PCA2_Theta']={'sql':'REAL'}
table_skel['PCA2_Phi']={'sql':'REAL'}

# Edge table
table_skeleton_edge = {'Project_Number':{'sql':'INTEGER'},'Sample_Aim_Number':{'sql':'INTEGER'},'ID':{'sql':'INTEGER'},'v1':{'sql':'INTEGER'},'v2':{'sql':'INTEGER'},'area':{'sql':'REAL'}}
for cax in 'XYZ':
    table_skeleton_edge['POS_'+cax]={'sql':'REAL'}
    table_skeleton_edge['DIR_'+cax]={'sql':'REAL'}

import os
# Create Database tables
def sqlDictParse(cKey,notnull=True):
	curStr=''
	if cKey.has_key('sql'): curStr+=' '+cKey['sql']
	if cKey.has_key('primary_key'): 
		if cKey['primary_key']: 
			curStr+=' PRIMARY KEY'
			if True: curStr+=' AUTO_INCREMENT'
		
	if cKey.has_key('unique'): 
		if cKey['unique']: curStr+=' UNIQUE'
	if notnull: curStr+=' NOT NULL'
	return curStr
def dbAddColumn(cur,tableName,colName,colInfo):
    cSQL='ALTER TABLE %s ADD %s ' % (tableName,colName)+sqlDictParse(colInfo,notnull=False)
    print cSQL
    cur.execute(cSQL)
    cur.commit()
def makeTable(cur,table_name,table_skeleton):
    outStr=[]
    for (cName,cKey) in table_skeleton.items():
        curStr=cName+sqlDictParse(cKey)
        outStr+=[curStr]
    cur.execute('BEGIN')
    curStr='CREATE TABLE '+table_name+' '
    if len(outStr)>0: curStr+='('+','.join(outStr)+')'
    try:
        cur.execute(curStr)
        cur.execute('COMMIT')
        print 'Creation succeeded'
        return 1
    except:
        print 'Creation failed:'+curStr+', rolling back'
        cur.execute('ROLLBACK')
        return 0
def safeExec(cur,*args,**kwargs):
    try:
        if kwargs.has_key('verbose'): 
        	if kwargs['verbose']: print (str(args))
        cur.execute(*args)
        return True
    except:
        print 'Execution failed of:'+str(args)+', rolling back..'
        try:
            cur.execute('ROLLBACK')
            return False
        except:
            print 'Even rollback failed...'
            return False
# Reset or recreate an entire database
# with mysql : mysqldump -u root -pkevin --add-drop-table --no-data lacuna | grep ^DROP | mysql -u root -pkevin lacuna
def ResetEntireDB(tableName,doad,debugMode=False,delFirst=True):
    cur=StartDatabase('Lacuna',mysql=True,doad=doad)
    if delFirst:
        for cTable in cur.execute('SHOW TABLES'):
        	if safeExec(cur,'BEGIN; DROP TABLE '+str(cTable[0])+'; COMMIT',verbose=True): print str(cTable[0])+' deleted!'
        	
    

    # Create New Project Table
    makeTable(cur,'Project', table_skel_proj)
    # Create New Sample Table
    makeTable(cur,'Sample', table_skel_sample)
    # Create New Canal Table
    makeTable(cur,'Canal', table_skel_Canal)
    # Create New Canal Table
    makeTable(cur,'Lacuna', table_skel)
    # Create New Edge Table
    makeTable(cur,'Edge', table_skeleton_edge)
    
def CreateDB(cur,makeIndex):    
    # Create New Project Table
    makeTable(cur,'Project', table_skel_proj)
    # Create New Sample Table
    makeTable(cur,'Sample', table_skel_sample)
    # Create New Canal Table
    makeTable(cur,'Canal', table_skel_Canal)
    # Create New Canal Table
    makeTable(cur,'Lacuna', table_skel)
    # Create New Edge Table
    makeTable(cur,'Edge', table_skeleton_edge)
    if makeIndex: CreateIndices(cur)
# Create the proper indices so searches happen in a reasonable amount of time
def CreateIndices(cur,Edge=True,Lacuna=True,Canal=True):
    if Edge:
        cur.execute('CREATE INDEX ebonev1 ON Edge (SAMPLE_AIM_NUMBER, V1);')
        cur.execute('CREATE INDEX ebonev2 ON Edge (SAMPLE_AIM_NUMBER, V2);')
        cur.execute('CREATE INDEX eprojbon ON Edge (PROJECT_NUMBER, SAMPLE_AIM_NUMBER);')
        cur.execute('CREATE INDEX eprojbonev1 ON Edge (PROJECT_NUMBER, SAMPLE_AIM_NUMBER, V1);')
        cur.execute('CREATE INDEX eprojbonev2 ON Edge (PROJECT_NUMBER, SAMPLE_AIM_NUMBER, V2);')
    if Lacuna:
        cur.execute('CREATE INDEX lbonlac ON Lacuna (SAMPLE_AIM_NUMBER,Lacuna_NUMBER);')
        cur.execute('CREATE INDEX lprojbon ON Lacuna (PROJECT_NUMBER, SAMPLE_AIM_NUMBER);')
        cur.execute('CREATE INDEX lprojbonlac ON Lacuna (PROJECT_NUMBER, SAMPLE_AIM_NUMBER, Lacuna_NUMBER);')
    if Canal:
        cur.execute('CREATE INDEX cboncan ON Canal (SAMPLE_AIM_NUMBER, Canal_NUMBER);')
        cur.execute('CREATE INDEX cprojbon ON Canal (PROJECT_NUMBER, SAMPLE_AIM_NUMBER);')
        cur.execute('CREATE INDEX cprojboncan ON Canal (PROJECT_NUMBER, SAMPLE_AIM_NUMBER, Canal_NUMBER);')


    
def namefix(inStr): 
    curStr=inStr.upper()
    curStr=''.join(curStr.split('_UPPER'))
    curStr=''.join(curStr.split('_LOWER'))
    return curStr
def processInputName(rawFilename,lcFilename):
    filename=namefix(rawFilename[0:rawFilename.find(lcFilename)]) # remove extension (not useful)
    numOffset=0
    if rawFilename.find('_UPPER')>-1: numOffset = 32768
    return (filename,numOffset)
from numpy import *    

def lacpa_adddb_OLD(ptList,oTable,rawFilename,processName=True,tableName='Lacuna',CanalMode=0):
    lacNumOffset=0
    if processName: (filename,lacNumOffset)=processInputName(rawFilename,lacFilename)
    else: filename=rawFilename
    dbLen=len(ptList['SCALE_X'])
    if not oTable.has_key('SAMPLE'): 
        oTable['SAMPLE']=''
        print filename+' is missing sample name' 
    dx=numpy.median(ptList['SCALE_X'])
    dy=numpy.median(ptList['SCALE_Y'])
    dz=numpy.median(ptList['SCALE_Z'])
    dr=sqrt(dx**2+dy**2+dz**2)

    
    lacTemp={}
    if type(projectTitle) is type(''): cProjNum=getProjNum(projectTitle)
    else: cProjNum=projectTitle
    cSampleNum=getSampleNum(filename,cProjNum)
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
    radialVars=['MASK_DISTANCE_MEAN','MASK_DISTANCE_COV','MASK_DISTANCE_STD']
    
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
    outMat=outMat[find(invalidRows==0),:]
    globals()['Om']=outMat
    outMat=[tuple(obj) for obj in outMat]
    addRowsToTable(tableName,outMat,entry_order=outKeys)
    print filename+' was successfully entered %05d, invalid  %03d' % (lacNumOffset,sum(invalidRows))

def addRowsToTable(tableName,outMat,entry_order=[]):
    #globals()['test']=(tableName,outMat,entry_order)
    # old command
    #lacDB.add_rows(tableName,outMat,entry_order=entry_order)
    
    cur.execute('BEGIN')
    sqlString='INSERT INTO '+tableName+' ('+','.join(entry_order)+') VALUES ('+','.join(['?']*len(entry_order))+');'
    cur.executemany(sqlString,outMat)
    #print(sqlString)
    #for cRow in outMat: 
    #    globals()['cRow']=cRow
    #    cur.execute(sqlString,cRow)
    cur.execute('COMMIT')

# Code for realigning the samples based on bone volume
def wPosFun(xvar,outFun,scalar=5):
    outstr=outFun(xvar)
    if xvar.upper()=='Z': outstr+='*('+str(scalar)+')'
    return outstr 
def rotateDatabaseStr(txy,txz,tyz,posFunc=lambda x: 'POS_C'+x,outPosFunc=lambda x: 'POS_C'+x):
    xyarr=matrix([[cos(txy),-sin(txy),0],[sin(txy),cos(txy),0],[0,0,1]])
    xzarr=matrix([[cos(txz),0,-sin(txz)],[0,1,0],[sin(txz),0,cos(txz)]])
    yzarr=matrix([[1,0,0],[0,cos(tyz),-sin(tyz)],[0,sin(tyz),cos(tyz)]])
    finArr=xyarr*xzarr*yzarr
    return _multDatabaseStr(finArr,posFunc=posFunc,outPosFunc=outPosFunc)
def _multDatabaseStr(finArr,posFunc=lambda x: 'POS_C'+x,outPosFunc=lambda x: 'POS_C'+x):    
    outStr=[]
    for cDex in range(3):
        nOut=tuple(finArr[cDex,:].tolist()[0])

        outLine=outPosFunc('XYZ'[cDex])+'=(('+posFunc('X')+')*(%f)+('+posFunc('Y')+')*(%f)+('+posFunc('Z')+')*(%f))'
        outStr+=[outLine % nOut]
    return ','.join(outStr)
def boneMOM(countText='COUNT(POS_X)',varFun=lambda cAx: 'POS_'+cAx,grpBy='Sample_Aim_Number',sqlAdd=None,weight='DENSITY_VOLUME*1000*1000*1000',tables=['Canal','Lacuna'],doRotate=True,doCenter=True):
    if sqlAdd is None : sqlAdd=''
    else: sqlAdd=' AND '+sqlAdd
    avgFun=lambda cVar: 'SUM(('+cVar+')*('+weight+'))/SUM('+weight+')'
    
    
    # Center Object 
    posMatrix=[avgFun(varFun(cAx)) for cAx in 'XYZ']
    nposMatrix=['POS_'+cAx+'=('+varFun(cAx)+'-(?))' for cAx in 'XYZ']
    projectNumber=str(getProjNum(projectTitle))
    covSqlCmd=lambda posCopies: 'SELECT '+','.join(posMatrix*posCopies)+',SAMPLE_AIM_NUMBER from Lacuna where Project_Number='+projectNumber+' '+sqlAdd+' group by '+grpBy
    outText=cur.execute(covSqlCmd(1)).fetchall()
    # Clear the other variables
    clearCmd='POS_RADIUS=-1,POS_DISTANCE=-1,POS_THETA=0'
    clearCmd+=',MASK_THETA=0,MASK_RADIUS_MIN=-1,MASK_RADIUS_MAX=-1,MASK_RADIUS_MEAN=-1'
    
    # Execute the Update Command to Center the Samples
    for tableName in tables:
        execStr='UPDATE '+tableName+' SET '+','.join(nposMatrix)+','+clearCmd+' WHERE SAMPLE_AIM_NUMBER=? AND Project_Number='+projectNumber+''
        print execStr
        curUpdate=cur.executemany(execStr,outText)
    lacDB.commit()    
    
    
    #posFun=lambda cAx: '('+varFun(cAx)+'-COV_'+cAx+')'   
    posFun=lambda cAx: 'POS_'+cAx
    tposFun=lambda cAx: wPosFun(cAx,posFun,10)
    textMatrix=[avgFun(tposFun(cAx[0])+'*'+tposFun(cAx[1])) for cAx in ['XX','YY','ZZ','XY','XZ','YZ']]
    textQuery=countText+','+','.join(textMatrix)
    print textQuery
    curQuery=cur.execute('SELECT SAMPLE_AIM_NUMBER,'+textQuery+' from Lacuna where Project_Number='+projectNumber+' '+sqlAdd+' group by '+grpBy)
    
    outText=curQuery.fetchall()

    mArr=lambda tQ: numpy.array([[tQ[0],tQ[3],tQ[4]],[tQ[3],tQ[1],tQ[5]],[tQ[4],tQ[5],tQ[2]]])
    if doRotate:
        for cOut in outText:
            print cOut
            p=mArr(cOut[1+1:1+8])
            (w,v)=numpy.linalg.eigh(p)
            v*=sign(v[2,2])
            sqlStr=_multDatabaseStr(matrix(v.transpose(1,0)),posFunc=posFun,outPosFunc=lambda x: 'POS_'+x)
            p1sqlStr=_multDatabaseStr(matrix(v.transpose(1,0)),posFunc=lambda x: 'PCA1_'+x,outPosFunc=lambda x: 'PCA1_'+x)
            p2sqlStr=_multDatabaseStr(matrix(v.transpose(1,0)),posFunc=lambda x: 'PCA2_'+x,outPosFunc=lambda x: 'PCA2_'+x)
            p3sqlStr=_multDatabaseStr(matrix(v.transpose(1,0)),posFunc=lambda x: 'PCA3_'+x,outPosFunc=lambda x: 'PCA3_'+x)
            dsqlStr=_multDatabaseStr(matrix(v.transpose(1,0)),posFunc=lambda x: 'DISPLACEMENT_'+x,outPosFunc=lambda x: 'DISPLACEMENT_'+x)
            for tableName in tables:
                execList=[sqlStr,p1sqlStr,p2sqlStr]#,p3sqlStr]
                if tableName.upper()=='Lacuna': execList+=[dsqlStr]
                updateCmd='UPDATE '+tableName+' SET '+','.join(execList)+' WHERE SAMPLE_AIM_NUMBER = '+str(cOut[0])+' AND Project_Number='+str(projectNumber)+''
                if doRotate: nout=cur.execute(updateCmd)
                lacDB.commit()
        # Find the new CoV for Lacuna
        print 'Finding new CoV...'
        outText=cur.execute(covSqlCmd(1)).fetchall()
        # Apply the new COV
        print 'Applying new CoV...' 
        for tableName in tables:
            execStr='UPDATE '+tableName+' SET '+','.join(nposMatrix)+' WHERE SAMPLE_AIM_NUMBER=? AND Project_NUMBER='+projectNumber+''
            curUpdate=cur.executemany(execStr,outText)    
        lacDB.commit()
    print 'Updating Polar Values...'
    maStr=['POS_RADIUS=SQRT(SQ(POS_X)+SQ(POS_Y))']
    maStr+=['POS_DISTANCE=SQRT(SQ(POS_X)+SQ(POS_Y)+SQ(POS_Z))']
    maStr+=['MASK_THETA=ATAN2(POS_Y,POS_X)']
    cur.execute('BEGIN')
    updateCmd='Update '+tableName+' SET '+','.join(maStr)+' WHERE Project_Number = '+str(projectNumber)+''
    nout=cur.execute(updateCmd)
    cur.execute('COMMIT')

def dbMinRad(samplename='%',steps=18,tables=['Lacuna','Canal']):
    stpRange='FLOOR((MASK_THETA+180)/360*'+str(steps)+')'
    stpGroup='ZAME('+stpRange+',SAMPLE_AIM_NUMBER)'
    tempOut=cur.execute('select MIN(POS_RADIUS),MAX(POS_RADIUS),WAVG(POS_RADIUS,DENSITY_VOLUME),SAMPLE_AIM_NUMBER,'+stpRange+' from Lacuna where Project LIKE "'+projectTitle+'" and SAMPLE_AIM_NUMBER LIKE "'+samplename+'" group by '+stpGroup).fetchall()
    for cTable in tables: 
        print (cTable,len(tempOut))
        cur.executemany('Update '+cTable+' SET MASK_RADIUS_MIN=?,MASK_RADIUS_MAX=?,MASK_RADIUS_MEAN=? where Project LIKE "'+projectTitle+'" and SAMPLE_AIM_NUMBER LIKE ? AND '+stpRange+'=?',tempOut)
        
    lacDB.commit()
    print 'Finished!'
    
