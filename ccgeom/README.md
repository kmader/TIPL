ccgeom
======

Taken from CodeForge http://www.codeforge.com/article/197089 

The Java code in this directory is an adaptation of the C code
described in "Computational Geometry in C" (Second Edition).
It is not written to be comprehensible without the
explanation in that book.  See http://cs.smith.edu/~orourke/
for further information.

The Java code is generally a straightforward translation of the C code.
Variable names were chosen to coincide, with some modifications,
e.g., all tNames in the C code (t=type) are cNames in the Java code
(c=class).  Nine programs from the textbook are incorporated into
a single applet called CompGeom.  Three more programs are left
as stand-alone Java applications.  The programs are as follows:

Applet Operations:
Area/Centroid:		Chapter 1, Code 1.5; Exercise 1.6.5.
Triangulate:		Chapter 1, Code 1.14
Convex Hull (2D):	Chapter 3, Code 3.8
Delaunay Triangulation:	Chapter 5, Code 5.2
SegSegInt:		Chapter 7, Code 7.2.
In Poly?:		Chapter 7, Code 7.13
Inter. 2 Conv. Poly:	Chapter 7, Code 7.17
Minkowski Convolution:	Chapter 8, Code 8.5
Arm Move:		Chapter 8, Code 8.7

Java Applications:
Convex Hull (3D):	Chapter 4, Code 4.8
sphere.c:		Chapter 4, Figure 4.15
Point-in-Polyhedron:	Chapter 7, Code 7.15


All the programs in the CompGeom applet share a common data structure,
a doubly-linked circular list of points, class cVertexList.  Whether
these points represent a polygon, a multilink arm, a set of points,
and so on, is determined by "regime" variables.  These regimes
control the painting display of those objects:  for example,
polygons are filled but point sets are not.  A set of editing operations
work on all regimes, so the user can easily modify the input.
Specific geometric operations are chosen from a menu, and the
appropriate result displayed. The code writes extensive diagnostic
output to the console.

The CompGeom applet is rather complicated, consisting of over 8000 lines
of code.  Attached below is a list of all the separate java class files,
and indented beneath each, the method names in those files.

C code written by Joseph O'Rourke.
Java code written b			

			...
			...
			... to be continued.

  This is a preview. To get the complete source file, 
  please click here to download the whole source code package.
