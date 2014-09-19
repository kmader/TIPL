import numpy as np
cimport numpy as np
from math import *
from scipy import stats
# Start With Pure Database Extension Functions

def clacdb_rdist(double a, double b, double c, double x, double y, double z):
    cdef double r
    r=sqrt((a-x)*(a-x)+(b-y)*(b-y)+(c-z)*(c-z))
    return r
    
cdef int lspace(float var,float minV,float maxV,int bins):
    ## Make bins for a histogram in SQL
    cdef float oVar=0
    cdef float nVar=0
    nVar+=var+0.0
    if var<minV: oVar=0
    elif var>maxV: oVar=bins
    else: oVar=round((var-minV)/(maxV-minV+0.0)*bins)
    return int(oVar)	

def clacdb_LINSPACE(float var,float minV,float maxV, int bins):
    ## Make bins for a histogram in SQL
    oVar=0
    return int(lspace(var,minV,maxV,bins))  
def clacdb_FLINSPACE(float var,float minV,float maxV, int bins):
    ## Make bins for a histogram in SQL
    oVar=0
    return float(lspace(var,minV,maxV,bins))*(maxV-minV)/bins+minV		
def clacdb_SPLIT(char *inStr,char *spChar,int spot): 
    ## 2D Grid for Group By Function in SQL
    cdef list cArr=inStr.split(spChar)
    if len(cArr)>spot: return cArr[spot]
    return ''
def clacdb_ISPLIT(char *inStr,char *spChar,int spot): 
    ## 2D Grid for Group By Function in SQL
    cdef list cStr
    cdef list dStr=[]
    try:
        
        cStr=list(clacdb_SPLIT(inStr,spChar,spot))
        
        for cPos in range(len(cStr)):
            if cStr[cPos].isdigit(): dStr+=cStr[cPos]
        return int(''.join(dStr))	
    except:
        return -1
def clacdb_FSPLIT(char *inStr,char *spChar,int spot): 
    ## 2D Grid for Group By Function in SQL
    cdef list cStr
    cdef list dStr=[]
    try:
        
        cStr=list(clacdb_SPLIT(inStr,spChar,spot))
        
        for cPos in range(len(cStr)):
            if cStr[cPos].isdigit(): dStr+=cStr[cPos]
        return float(''.join(dStr))	
    except:
        return -1.0
def c_fixang(double ang): return (ang+180)%360-180
def c_zame2(char *x,char *y): return x+','+y
def c_zame3(char *x,char *y, char *z): return x+','+y+','+z


# Aggregate Function
cdef class clacDB_VAR:
    cdef int count
    cdef double sum
    cdef double sum2
    def __init__(self):
        count = 0
        sum=0.0
        sum2 = 0.0

    def step(self,double value):
        if value is None: return 0
        self.count+=1
        self.sum+=value
        self.sum2 += value*value

    def finalize(self):
        if self.count<1: return 0
        xmean=self.sum/(self.count+0.0)
        xsmean=self.sum2/(self.count+0.0)
        cVal=xsmean-xmean*xmean
        return abs(cVal)
    @classmethod
    def factory(cls):
        return cls(), cls.step, cls.finalize
cdef class clacDB_MED:
    cdef np.ndarray vals
    def __init__(self):
        self.vals=np.array([])
    def step(self, double value):
        self.vals.resize(self.vals.shape[0]+1)
        self.vals[-1]=value
    
    def finalize(self):
        return np.median(self.vals)
    @classmethod
    def factory(cls):
        return cls(), cls.step, cls.finalize
cdef class clacDB_WAVG:
    cdef double sumw
    cdef double sum
    def __init__(self):
        self.sumw = 0.0
        self.sum=0.0
    
    def step(self, double value,double weight):
        self.sumw+=weight
        self.sum+=value*weight
    
    def finalize(self):
        cdef double xmean=self.sum/(self.sumw)
        return xmean
    @classmethod
    def factory(cls):
        return cls(), cls.step, cls.finalize
cdef class clacDB_WVAR:
    cdef double sumw
    cdef double sum
    cdef double sum2
    def __init__(self):
        self.sumw = 0.0
        self.sum=0.0
        self.sum2 = 0.0

    def step(self, double value,double weight):
        if value is None: return 0
        if weight is None: return 0
        self.sumw+=weight
        self.sum+=value*weight
        self.sum2 += value*value*weight

    def finalize(self):
        if self.sumw==0: return -1
        xmean=self.sum/(self.sumw)
        xsmean=self.sum2/(self.sumw)
        cVal=xsmean-xmean*xmean
        return abs(cVal)
    @classmethod
    def factory(cls):
        return cls(), cls.step, cls.finalize        
cdef class clacDB_STD(clacDB_VAR):
    def finalize(self):
        return sqrt(clacDB_VAR.finalize(self))	
    @classmethod
    def factory(cls):
        return cls(), cls.step, cls.finalize
cdef class clacDB_WSTD(clacDB_WVAR):
    def finalize(self):
        return sqrt(clacDB_WVAR.finalize(self))
    @classmethod
    def factory(cls):
        return cls(), cls.step, cls.finalize
cdef class clacDB_LINFIT:
    cdef int count
    cdef double suma
    cdef double suma2
    cdef double sumb
    cdef double sumb2
    cdef double sumab
    def __init__(self):
        self.count = 0
        self.suma=0.0
        self.suma2 = 0.0
        self.sumb=0.0
        self.sumb2 = 0.0
        self.sumab=0.0
    def step(self, double valueA, double valueB):
        if valueA is None: return 0
        if valueB is None: return 0
        self.count+=1
        self.suma+=valueA
        self.suma2 += valueA*valueA
        
        self.sumb+=valueB
        self.sumb2 += valueB*valueB
        
        self.sumab += valueA*valueB

    def finalize(self):
        if self.count<1: return 0
        ncount=self.count+0.0
        denom=(ncount*self.suma2-self.suma**2)
        if denom==0: return 0
        return (ncount*self.sumab-self.suma*self.sumb)/denom
    @classmethod
    def factory(cls):
        return cls(), cls.step, cls.finalize	
cdef class clacDB_WLINFIT:
    cdef double sumw
    cdef double suma
    cdef double suma2
    cdef double sumb
    cdef double sumb2
    cdef double sumab
    def __init__(self):
        self.sumw = 0
        self.suma=0.0
        self.suma2 = 0.0
        self.sumb=0.0
        self.sumb2 = 0.0
        self.sumab=0.0
    def step(self, double valueA,double valueB,double weight):
        if valueA is None: return 0
        if valueB is None: return 0
        if weight is None: return 0
        self.sumw+=weight
        self.suma+=valueA*weight
        self.suma2 += valueA*valueA*weight
        
        self.sumb+=valueB*weight
        self.sumb2 += valueB*valueB*weight
        
        self.sumab += valueA*valueB*weight

    def finalize(self):
        if self.sumw==0: return 0
        ncount=self.sumw
        return (ncount*self.sumab-self.suma*self.sumb)/(ncount*self.suma2-self.suma**2)
    @classmethod
    def factory(cls):
        return cls(), cls.step, cls.finalize        
cdef class clacDB_CORR:
    cdef int count
    cdef double suma
    cdef double suma2
    cdef double sumb
    cdef double sumb2
    cdef double sumab
    def __init__(self):
        self.count = 0
        self.suma=0.0
        self.suma2 = 0.0
        self.sumb=0.0
        self.sumb2 = 0.0
        self.sumab=0.0
    def step(self, double valueA,double valueB):
        if valueA is None: return 0
        if valueB is None: return 0
        self.count+=1
        self.suma+=valueA
        self.suma2 += valueA*valueA
        
        self.sumb+=valueB
        self.sumb2 += valueB*valueB
        
        self.sumab += valueA*valueB

    def finalize(self):
        if self.count<1: return 0
        ncount=self.count+0.0
        xmean=self.suma/ncount
        xsmean=self.suma2/ncount
        xstd=sqrt(xsmean-xmean*xmean)
        
        ymean=self.sumb/ncount
        ysmean=self.sumb2/ncount
        ystd=sqrt(ysmean-ymean*ymean)
        
        xymean=self.sumab/ncount
        
        cVal=xymean-xmean*ymean
        if (xstd*ystd>0): cVal/=(xstd*ystd)
        else: return 1
        return cVal
    @classmethod
    def factory(cls):
        return cls(), cls.step, cls.finalize
def kttest(float mean1,float v1,int n1,float mean2,float v2,int n2):
    """Calculates the T-test for the means of TWO INDEPENDENT samples of scores.
    stolen from /home/scratch/sage-4.4.2/local/lib/python2.6/site-packages/scipy/stats/stats.py
    """
    return (0.1,c_kttest(mean1,v1,n1,mean2,v2,n2))
	

cdef float c_kttest(float mean1,float v1,int n1,float mean2,float v2,int n2):
    """Calculates the T-test for the means of TWO INDEPENDENT samples of scores.
    stolen from /home/scratch/sage-4.4.2/local/lib/python2.6/site-packages/scipy/stats/stats.py
    """
    
    cdef int df = n1+n2-2
    
    cdef float d = mean1 - mean2
    cdef float svar = ((n1-1)*v1+(n2-1)*v2) / float(df)
    cdef double t
    cdef double prob
    if svar==0: return 0,0
    if (n1>0) & (n2>0) & (svar>0):
    
        t = d/sqrt(svar*(1.0/n1 + 1.0/n2))
        if (d==0): t=1
        
        prob = stats.distributions.t.sf(abs(t),df)*2 #use np.abs to get upper tail
    
        return prob
    else:
        return 1

import numpy.linalg

cdef class clacDB_TTEST:
    # TTEST taken from scipy.stats.ttest_ind
    # returns the p-value between two variables
    cdef int count
    cdef double suma
    cdef double suma2
    cdef double sumb
    cdef double sumb2
    cdef double sumab
    def __init__(self):
        self.count = 0
        self.suma=0.0
        self.suma2 = 0.0
        self.sumb=0.0
        self.sumb2 = 0.0
        self.sumab=0.0
    def step(self, double valueA,double valueB):
        if valueA is None: return 0
        if valueB is None: return 0
        self.count+=1
        self.suma+=valueA
        self.suma2 += valueA*valueA
        
        self.sumb+=valueB
        self.sumb2 += valueB*valueB
        
        self.sumab += valueA*valueB

    def finalize(self):
        if self.count<1: return 0
        cdef double ncount=self.count
        cdef double xmean=self.suma/ncount
        cdef double xsmean=self.suma2/ncount
        cdef double xvar=xsmean-xmean*xmean
        
        cdef double ymean=self.sumb/ncount
        cdef double ysmean=self.sumb2/ncount
        cdef double yvar=ysmean-ymean*ymean
        cdef double xymean=self.sumab/ncount
        
        
        cdef double cVal=c_kttest(xmean,xvar,self.count,ymean,yvar,self.count)
        return cVal
    @classmethod
    def factory(cls):
        return cls(), cls.step, cls.finalize
        
        


cdef class ctexture:
    cdef np.ndarray textMat
    cdef np.ndarray _w
    cdef np.ndarray _v
    def __init__(self,np.ndarray[np.float64_t, ndim=2] textMat):
        self.textMat=textMat
        (self._w,self._v)=np.linalg.eigh(textMat)
        #self._w=np.sqrt(self._w) # No sqrt in texture tensor
    def mean(self):
        return sqrt(self.textMat.trace())
    def v1(self):
        return self._v[:,2]
    def alignment(self):
        return 3.0/2.0*(self._w[2]/sum(self._w)-1.0/3.0)*100
cdef class clacDB_TEXT:
    cdef float count
    cdef int countn
    cdef float sumx2
    cdef float sumxy
    cdef float sumxz
    cdef float sumy2
    cdef float sumyz
    cdef float sumz2
    
    def __init__(self):
        self.count = 0.0
        self.countn= 0
        self.sumx2 = 0.0
        self.sumxy=0.0
        self.sumxz=0.0
        self.sumy2 = 0.0
        self.sumyz=0.0
        self.sumz2=0.0
        

    def step(self, valueX,valueY,valueZ,valueW=1.0):
        if valueX is None: return 0
        if valueY is None: return 0
        if valueZ is None: return 0
        if valueW is None: return 0
        self.count+=valueW
        self.countn+=1
        self.sumx2+=valueW*valueX**2
        self.sumxy+=valueW*valueX*valueY
        self.sumxz+=valueW*valueX*valueZ
        self.sumy2+=valueW*valueY**2
        self.sumyz+=valueW*valueY*valueZ
        self.sumz2+=valueW*valueZ**2

    def textMat(self):
        return np.array([[self.sumx2/self.count,self.sumxy/self.count,self.sumxz/self.count],[self.sumxy/self.count,self.sumy2/self.count,self.sumyz/self.count],[self.sumxz/self.count,self.sumyz/self.count,self.sumz2/self.count]])
        
    def finalize(self):
        if self.countn<2: return 100
        
        return ctexture(self.textMat()).alignment()
    @classmethod
    def factory(cls):
        return cls(), cls.step, cls.finalize
        
# Here are the Nearest Neighbor Functions
cdef np.ndarray cartdistfun(np.ndarray[np.float64_t,ndim=2] lacOutArr,int i):
    return np.sqrt((lacOutArr[:,1]-lacOutArr[i,1])*(lacOutArr[:,1]-lacOutArr[i,1])+(lacOutArr[:,2]-lacOutArr[i,2])*(lacOutArr[:,2]-lacOutArr[i,2])+(lacOutArr[:,3]-lacOutArr[i,3])*(lacOutArr[:,3]-lacOutArr[i,3]))
cdef np.ndarray cyldistfun(np.ndarray[np.float64_t,ndim=2] lacOutArr,int i):
    dr=(lacOutArr[:,1]-lacOutArr[i,1])*(lacOutArr[:,1]-lacOutArr[i,1])
    dth=(lacOutArr[:,2]-lacOutArr[i,2])
    idth=-dth
    
    
    dz=(lacOutArr[:,3]-lacOutArr[i,3])*(lacOutArr[:,3]-lacOutArr[i,3])

def pdistmat(np.ndarray[np.float64_t,ndim=2] lacOutArr,float neighbordist=50):
    cdef list outArr=[]
    cdef np.ndarray parmMat=lacOutArr[:,2]
    cdef np.ndarray rArr
    cdef np.ndarray tVec
    cdef float v1
    cdef float v2
    for i in range(lacOutArr.shape[0]):
        rArr=cartdistfun(lacOutArr,i)
        rArr[i]=9e9
        minLac=rArr.argmin()
        #tVec=np.array([lacOutArr[minLac,1]-lacOutArr[i,1],lacOutArr[minLac,2]-lacOutArr[i,2],lacOutArr[minLac,3]-lacOutArr[i,3]])
        #if tVec[2]<0: tVec*=-1
        # (lacid,mindist,neigh,x,y,z,theta)
        outArr+=[(rArr[minLac],lacOutArr[i,0])]
        if (i%1000)==0: print i
    return outArr