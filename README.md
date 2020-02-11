![](https://github.com/kmader/TIPL/workflows/core/badge.svg) ![](https://github.com/kmader/TIPL/workflows/package/badge.svg) ![](https://github.com/kmader/TIPL/workflows/test/badge.svg)

[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/kmader/TIPL.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/kmader/TIPL/context:java)
[![Language grade: Python](https://img.shields.io/lgtm/grade/python/g/kmader/TIPL.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/kmader/TIPL/context:python)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/kmader/TIPL.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/kmader/TIPL/alerts/)

# Tomcat Image Processing Language (TIPL)
TIPL is a Java-based large scale 3D image processing framework designed for tomographic sized datasets from microCT and synchrotrons. It has been optimized for parallel and distributed computation using Apache Spark and scripting using Jython. Support for SciJava and ImageJ has been integrated but are most useful for slice-based operations or file IO. The project has not been actively developed in some time, but should still build. Unfortunately all of the tests were setup to run on my local/cluster environment and haven't been ported to run using modern CI tools.

## Checking Out
The project makes excessive use of git submodules and thus requires `git clone --recursive` in order to pull all of the code dependencies for the project. 

## Building

Building requires a few odd non-maven tracked dependencies and so it is probably easiest to follow the setup in the [workflow](https://github.com/kmader/TIPL/blob/maven/.github/workflows/package.yml)
- When using pom-imagej based dependencies and imglib2 and other libraries make sure you update the local version of scijava-common which supports parallel building without the `ConcurrentModificationException`

### Commiting Upgrade
After making many updates it is often easier to do a batch commit
```
commitMessage="Test Message"
git add pom.xml
git add *.iml
for cDir in */; do 
    cd $cDir; 
    git add pom.xml; 
    git add --all src; 
    git add *.iml; 
    cd ..; 
done
cd ccgeom/
git commit -m "$commitMessage"
cd ../volviewer
git commit -m "$commitMessage"
cd ../VIB-lib/
git commit -m "$commitMessage"
cd ..
git commit -m "$commitMessage"
```

To push all

```
cd ccgeom/
git push
cd ../volviewer
git push
cd ../VIB-lib/
git push
cd ..
git push
```
## Building Notes
- The project is meant to be built in a Java first then Scala manner with the exception of a few files. Primarily ScalaPlugins.java which is used for loading the Plugins written in Scala into the SezPoz-based TIPLPluginManager. A simple space entered into this file and save will cause it to recompile after the scala code has been compiled resolving all errors.
- The IDEA compiler can cause issues, using the SBT incrementally type works better
## Updating Libraries
### Updating Spark
The TIPL code itself is separated from Spark but involves the spark code in a number of key areas. 
- SGEJob sets up the path and includes reference to the jumbo jar file in the spark/lib folder, many tasks unrelated to Spark will not work if this is missing (to prevent library duplication)
