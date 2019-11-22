import sys
import tracktools as tt

lacFileA = sys.argv[1]
lacFileB = sys.argv[2]
if len(sys.argv) < 4:
    outFile = "_ptxt.csv".join(lacFileA.split(".csv"))
else:
    outFile = sys.argv[3]

lacA = tt.lacunData(lacFileA)
lacB = tt.lacunData(lacFileB)

out = open(outFile, "w")
inLine = lacA.preamble
# inLine+='; LAC_MIN='+str(tuple(lacA.lmin))+'; LAC_MAX='+str(tuple(lacA.lmax))
inLine += "\n"
out.write(inLine)
inLine = (
    "//Component 1, Component 2, Match_Quality, POS_X,POS_Y,POS_Z,DIR_X,DIR_Y,DIR_Z\n"
)
out.write(inLine)  # First two lines are garbage
matchDict = lacA.match(lacB)
print("Bubbles:", len(lacA.posMat), "Matches", len(matchDict.keys()))
for aLac in sorted(matchDict.keys()):
    (bLac, mQual) = matchDict[aLac]
    if lacA.has_key(aLac) & lacB.has_key(bLac):
        aPos = lacA.getPos(aLac)
        bPos = lacB.getPos(bLac)
        # avgPos=0.5*(aPos+bPos)
        # vecDir=0.5*(aPos-bPos)
        avgPos = aPos
        vecDir = bPos - aPos
        out.write(
            "%i,%i,%f,%f,%f,%f,%f,%f,%f\n"
            % (
                aLac,
                bLac,
                mQual,
                avgPos[0],
                avgPos[1],
                avgPos[2],
                vecDir[0],
                vecDir[1],
                vecDir[2],
            )
        )
