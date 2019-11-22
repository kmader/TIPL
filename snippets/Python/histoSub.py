import sys, os
from numpy import *
from subprocess import *
import parseHistogram as ph
import preptext as pt
import prepedge as pe
from optparse import OptionParser

hsubOpt = OptionParser()
hsubOpt.add_option(
    "-p",
    "--project",
    dest="project",
    help="Project Title [default: %default]",
    default="UJAX_F2",
)
hsubOpt.add_option(
    "-f",
    "--file",
    dest="filename",
    help="Histograms to Analyze [default: %default]",
    default="maskdto.tif.csv",
)
hsubOpt.add_option(
    "-v",
    "--value",
    dest="valcol",
    help="Column number for value column [default: %default]",
    default=0,
)
hsubOpt.add_option(
    "-w",
    "--weight",
    dest="weightcol",
    help="Column number for weight column [default: %default]",
    default=1,
)
hsubOpt.add_option(
    "-t",
    "--texture",
    dest="textMode",
    help="Run Texture Analysis [default: %default]",
    default=False,
    action="store_true",
)
hsubOpt.add_option(
    "-e",
    "--prepedge",
    dest="prepedge",
    help="Skip PrepEdge Analysis [default: %default]",
    default=True,
    action="store_false",
)
hsubOpt.add_option(
    "-m",
    "--meanname",
    dest="meanname",
    help="Mean Parameter Name [default: %default]",
    default="CT_TH",
)
(opt, args) = hsubOpt.parse_args()
hsubOpt.add_option(
    "-s",
    "--stdname",
    dest="stdname",
    help="STD Parameter Name [default: %default]",
    default=opt.meanname + "_STD",
)

(opt, args) = hsubOpt.parse_args()
hsubOpt.print_help()

print (opt.filename, opt.valcol, opt.weightcol)
# sys.exit()

from glob import glob
from dbAddSample import *

cur = dbt.StartDatabase()
vmsFix = lambda wholeFile: "\\;".join(wholeFile.split(";"))


def canBeIntFilter(x):
    try:
        y = int(x)
        return True
    except:
        return False


for rt, drs, files in os.walk(os.getcwd(), topdown=False):
    ffiles = filter(lambda x: x.find(opt.filename) >= 0, files)
    for ifile in ffiles:
        cfile = rt + "/" + ifile
        print cfile
        if opt.textMode:
            steps = (5, 5, 5)
            lstep = numpy.array((42, 42, 42 * 1.5))
            dirV = ["DIR_X", "DIR_Y", "DIR_Z"]
            isoSteps = False
            useLstep = False
            useBubbles = False
            useRadPos = False
            doStrain = -1
            trimEdge = 1
            try:
                if (
                    opt.prepedge
                ):  # Generate prepared edge file first with correct positions
                    laclist = glob(rt + "/lacun_*.csv")
                    lacFile = filter(
                        lambda x: canBeIntFilter(x[x.rfind("_") + 1 : x.rfind(".csv")]),
                        sorted(laclist),
                    )[
                        -1
                    ]  # latest lacuna
                    print ("Making Edges", cfile, lacFile)
                    textFile = pe.prepareEdge(cfile, lacFile)
                else:
                    textFile = cfile

                tInfo = pt.runTexture(
                    textFile,
                    steps,
                    lstep,
                    dirV,
                    isoSteps,
                    useLstep,
                    useBubbles,
                    useRadPos,
                    doStrain,
                    trimEdge=trimEdge,
                )
                print tInfo
                tText = tInfo["AvgTexture"]
                cov = tText.tens()

                print (
                    cur,
                    cfile,
                    opt.meanname,
                    tText.aiso,
                    tText.obl,
                )  # projectTitle=opt.project)
                InsertMetric(
                    cur,
                    cfile,
                    opt.meanname + "_AISO",
                    tText.aiso,
                    projectTitle=opt.project,
                )
                InsertMetric(
                    cur,
                    cfile,
                    opt.meanname + "_OBLATENESS",
                    tText.obl,
                    projectTitle=opt.project,
                )
                for (cDex, cLabel) in enumerate("XYZ"):
                    for (rDex, rLabel) in enumerate("XYZ"):
                        InsertMetric(
                            cur,
                            cfile,
                            opt.meanname + "_" + cLabel + rLabel,
                            cov[cDex, rDex],
                            projectTitle=opt.project,
                        )
            except:
                print "Texture Failed!"
        else:
            try:
                (a, b) = ph.readHist(cfile)
                (meanVal, stdVal) = ph.meanStd(
                    b, valCol=opt.valcol, weightCol=opt.weightcol
                )
                print ("Submitting :", rt, "<Mean,STD>", meanVal, stdVal)
                if len(opt.meanname):
                    InsertMetric(
                        cur, cfile, opt.meanname, meanVal, projectTitle=opt.project
                    )
                if len(opt.stdname):
                    InsertMetric(
                        cur, cfile, opt.stdname, stdVal, projectTitle=opt.project
                    )
            except:
                print cfile + " IS INVALID!"
