# FTP Settings
ifbVMShost='dalton.ethz.ch'
ifbVMSusername='sage'
ifbVMSpassword='ifb_sage_admin'


# Import Raw CSV File
from ftplib import FTP
from fnmatch import fnmatch
def vms_name(cname): return cname.upper().split(';')[0]
def listFtpFiles(cDir,filtFuncI,delDup=True):
    if type(filtFuncI) is type(''): filtFunc=lambda x: fnmatch(x,filtFuncI.upper())
    elif type(filtFuncI) is type(lambda x: True): filtFunc=filtFuncI
    else: 
        print 'What the frick did you hand me? '+str(filtFuncI)
        return []
        
    ifbserv=ifbservF()
    print ifbserv.cwd(cDir)
    tempList=map(vms_name,ifbserv.nlst())
    ifbserv.close()
    if delDup: tempList=unique(tempList)
    return filter(filtFunc,tempList)


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
        print cFile+' is garbage'    
def readFtpCSV(cDir,fileList,fixname=lambda x: x):
    lacun={}
    ifbserv=ifbservF()
    print ifbserv.cwd(cDir)
    
    for cFile in fileList:
        cFile
        globals()['tempStr']=''
        def cOutFun(text): globals()['tempStr']+=text
        ifbserv.retrbinary('RETR '+cFile,cOutFun)
        try:
            (outrows,a,b)=parseCSV(globals()['tempStr'])
            if outrows>2:
                lacun[fixname(cFile)]=(a,b)
            else:
                print cFile+' is too short!'
        except:
            print cFile+' is garbage'
    ifbserv.close()
    return lacun

    
       
# IFB Login
ifbservF=lambda : FTP(ifbVMShost,ifbVMSusername,ifbVMSpassword)