# Upload Previews using Paraview of all the files in a directory 
# 
# This script uses the tight integration of openmpi-1.4.3-intel-12.1 in SGE
# using the parallel environment (PE) "orte".  
# This script must be used only with qsub command - do NOT run it as a stand-alone 
# shell script because it will start all processes on the local node.   
# Define your job name, parallel environment with the number of slots, and run time: 
#$ -cwd
#$ -j y
#$ -pe smp 1
#$ -M kevinmader+merlinsge@gmail.com
#$ -m ae
#$ -o UploadPreview.log
#$ -N UploadPreviews
#$ -l s_rt=23:59:00,h_rt=23:59:00
###################################################
# Fix the SGE environment-handling bug (bash):
source /usr/share/Modules/init/sh
export -n -f module

exit 0

###################################################
