from dbAddSample import dbt,dbAddImage
from glob import glob
cur=dbt.StartDatabase(dorw=True)
goodLoad=0
badLoad=0
for cFile in glob('/gpfs/home/mader/previews/*.png'):
	#dbAddImage(cur,cFile)
	try:
		dbAddImage(cur,cFile)
		goodLoad+=1
	except:
		print ('FAILED',cFile)
		badLoad+=1
print ('FINISHED','SUCCESSFUL-',goodLoad,'FAILED',badLoad)
