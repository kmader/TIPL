# jython script based on TIPL to calculate lacuna distances and volumes
import ch.psi.tomcat.tipl.VirtualAim as VA
import ch.psi.tomcat.tipl.kVoronoiShrink as KV
VA.scratchLoading=True
KV.supportedCores=6
VA.supportedCores=6
lacun=VA('lacun.tif')
mask=VA('mask.tif')
vorn=KV(lacun,mask)
vorn.run()
lout=vorn.ExportDistanceAim(lacun)
lout.WriteAim('lacundist.tif')
lout=vorn.ExportVolumesAim(lacun)
lout.WriteAim('lacunvols.tif')
