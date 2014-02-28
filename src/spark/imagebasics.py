textImg=sc.textFile("2011text/block*.csv")
# convert csv to position, value
parse_line=lambda sx: ((int(sx[0]),int(sx[1]),int(sx[2])),float(sx[3]))
rImg=textImg.map(lambda cline: cline.split(",")).map(parse_line)

# perform a threshold
thImg=rImg.filter(lambda pvec: pvec[1]>0)

# perform a averaging filter
def fan_out_voxels(pvec):
    outpoints=[]
    cpt=pvec[0]
    cval=pvec[1]/9 # divide by the number of times it is sent out
    for x in range(cpt[0]-1,cpt[0]+2):
        for y in range(cpt[1]-1,cpt[1]+2):
            for z in range(cpt[2]-1,cpt[2]+2):
                outpoints+=[((x,y,z),cval)]
    return outpoints
# take the sum at every point    
filtImg=rImg.flatMap(fan_out_voxels).reduceByKey(lambda a,b: (a+b))
