import sys, os
from numpy import *
from subprocess import *
from glob import glob
from dbAddSample import *

cur = StartDatabase()
vmsFix = lambda wholeFile: "\\;".join(wholeFile.split(";"))
showlen = 0
loadlac = 1
loadcan = 1
loadlacedge = 0
loadcanedge = 0
if len(sys.argv) < 2:
    for rt, drs, files in os.walk(os.getcwd(), topdown=False):
        ffiles = filter(lambda x: x.find("lacun_0.csv") >= 0, files)

        if len(ffiles) > 0:
            os.chdir(rt)
            # os.system('pwd')
            lacFile = glob(rt + "/lacun_0.csv")
            if showlen:
                for acsvFile in csvfiles:
                    os.system("wc -l " + acsvFile)
            if loadlac:
                if len(lacFile) > 0:
                    # InsertCSV(cur,lacFile[0],samplename=lacFile[0].split('/')[-2],projectTitle='UJAX_F2')
                    try:
                        InsertCSV(
                            cur,
                            lacFile[0],
                            samplename="_".join(lacFile[0].split("/")[-3:-1]),
                            projectTitle="Zihui",
                        )
                    except:
                        print "Failed Lac Insert!"
                else:
                    print rt + " is missing lacuna csv file!!"
            canFile = glob(rt + "/canal_0.csv")
            print canFile
            if loadcan:
                if len(canFile) > 0:
                    try:
                        InsertCSV(
                            cur,
                            canFile[0],
                            samplename="_".join(canFile[0].split("/")[-3:-1]),
                            projectTitle="Zihui",
                            tableName="Canal",
                            CanalMode=1,
                        )
                    except:
                        print "Failed Can Insert!"
                else:
                    print rt + " is missing canal csv file!!"
            edgeFile = glob(rt + "/lacun_edge.csv")
