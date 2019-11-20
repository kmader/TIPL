# File-IO
This handles the standard file IO for TIPL and can be extended to support new formats. Annotations will serve to find and use the classes necessary to import various file formats.

## Files
The standard dataset to open is a file. This is where a single file encapsulates all of the information about the image using formats like TiffDirectory, ISQ, RAW, and HDF5. The built-in header constructs are used for storing additional information.
## Directory
The alternative form is a file system (local or hadoop/s3 etc) that contains a folder full of images. The images are read into a stack in alphanumeric order.

## Metadata
Some formats, like TIFF and HDF5, support extensive headers, for others If the proper text file is present it can be used for providing additional metadata about an image. The most important being position offset (of the upper right corner for dealing with chunked images), voxel size, and scale-factors.

## Hadoop
It is currently not clear exactly how file reading and writing will be done with Hadoop/HDFS and at the moment some of the standard code resides here and the rest is in the tipl-spark project.