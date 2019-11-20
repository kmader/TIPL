# Spark ImageIO

A Spark library for reading in images using the java-based ImageIO and loading images as tiles
instead of entire datasets

## GeoTIFF

Download geotiff-jai.jar from http://sourceforge.net/projects/geotiff-jai/ and manually install it to your local repo as follows (making sure you are not in the MrGeo source directory):

'''
mvn install:install-file  -Dfile=<PATH-TO_GEOTIFF-JAI.JAR> -DgroupId=geotiff -DartifactId=geotiff-jai -Dversion=0.0 -Dpackaging=jar -DlocalRepositoryPath=<PATH-TO-SPECIFIC-LOCAL-REPO>
'''