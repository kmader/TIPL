#!/bin/sh
# remove the existing files
rm /Users/mader/Dropbox/Informatics/TIPL_Open/src/tipl/*/*.java 
# Copy the parts of the project which can be open-sourced / shared to the public repository
cp /Users/mader/Dropbox/TIPL/src/tipl/formats/*.java /Users/mader/Dropbox/Informatics/TIPL_Open/src/tipl/formats
cp /Users/mader/Dropbox/TIPL/src/tipl/util/*.java /Users/mader/Dropbox/Informatics/TIPL_Open/src/tipl/util
cp /Users/mader/Dropbox/TIPL/src/tipl/blocks/*TIPL*.java /Users/mader/Dropbox/Informatics/TIPL_Open/src/tipl/blocks
cp /Users/mader/Dropbox/TIPL/src/tipl/tools/Base*.java /Users/mader/Dropbox/Informatics/TIPL_Open/src/tipl/tools
cp /Users/mader/Dropbox/TIPL/src/tipl/tools/DistLabel.java /Users/mader/Dropbox/Informatics/TIPL_Open/src/tipl/tools
cp /Users/mader/Dropbox/TIPL/src/tipl/ij/*.java /Users/mader/Dropbox/Informatics/TIPL_Open/src/tipl/ij
cp /Users/mader/Dropbox/TIPL/src/tipl/ij/volviewer/*.java /Users/mader/Dropbox/Informatics/TIPL_Open/src/tipl/ij/volviewer