/*----------------------------------------------------------------------------
 * Class ConvexHull3D
 *
 * Application for computing convex hull of points in 3D.
 *---------------------------------------------------------------------------*/
import java.awt.*;
import java.io.*;

public class ConvexHull3D {
  /* Define flags */
  private static final boolean ONHULL = true;
  private static final boolean REMOVED = true;
  private static final boolean VISIBLE = true;
  private static final boolean PROCESSED = true;
  private static final int SAFE = 1000000;

  private boolean debug;  
  private boolean check;  
  boolean toDraw;
  private cVertexList list;
  private cEdgeList elist;
  private cFaceList flist;

  public static void main(String args[]) throws IOException {
    ConvexHull3D ch = new ConvexHull3D();
  }

  ConvexHull3D() throws IOException {
    list = new cVertexList();
    elist = new cEdgeList();
    flist = new cFaceList();
    debug = toDraw = false;
    check = false;
    String s;

    char c;
    char line[] = new char[20];
    int i = 0;
    int x, y, z;
    boolean flag;
    int counter;

    System.out.println("\n\nInput points:\nCoord-s must be seperated by a *tab*\n"+
		       "ENTER after each point"+"\nTo finish input type end + "+
		       "ENTER at the end"+
		       "\nExample:\n17      23      123\n34      5      1\nend\n"+
		       "-----------------start entering data-------------------");
    do {
      do {
	c = (char) System.in.read();
	line[i] = c;
	i++;
      } while (c !='\n' );
      s = new String(line);
      s = s.substring(0,i-1);
      if (s.equals("end"))
	break;
      flag = false;
      counter = 0;
      for (int j=0; j < s.length(); j++) {
	if (s.charAt(j) == '\t') 
	  counter++; 
	if (counter == 2) {
	  flag = true; break; 
	}
      }
      if (flag) {
	int t = s.indexOf('\t');
	int t1 = s.lastIndexOf('\t');
	x = Integer.parseInt(s.substring(0,t));
	y = Integer.parseInt(s.substring(t+1,t1));
	z = Integer.parseInt(s.substring(t1+1,s.length()));
	list.SetVertex3D(x,y,z);
	i=0;
      }
      else 
	break;
    } while (!s.equals("end"));

    ReadVertices();
    System.out.println("Data was accepted");
    list.PrintVertices3D();
    System.out.println("Calculating convex hull...");

    if (DoubleTriangle()) {
      toDraw = true;
      ConstructHull();
      Print();
      ClearLists();
    }
  }

  public void ReadVertices() 
  {
    cVertex v = list.head;
    int vnum = 0;
    do {
      v.vnum = vnum++;
            if (( Math.abs(v.v.x) > SAFE ) || ( Math.abs(v.v.y) > SAFE ) 
	  || ( Math.abs(v.v.z) > SAFE ) ) {
	System.out.println("Coordinate of vertex below might be too large...");
	v.PrintVertex3D(vnum);
      }
      v = v.next;
    } while ( v != list.head );
  }

  public void ClearLists()
  {
    elist.ClearEdgeList();
    flist.ClearFaceList();
    check = debug = toDraw = false;
  }

  /*---------------------------------------------------------------------
    Print: Prints out the vertices and the faces.  Uses the vnum indices 
    corresponding to the order in which the vertices were input.
    ---------------------------------------------------------------------*/
  private void	Print()
  {
    /* Pointers to vertices, edges, faces. */
    cVertex  v;
    cEdge    e;
    cFace    f;
    int xmin, ymin, xmax, ymax;
    int a[] = new int[3], b[] = new int[3];  /*used to compute normal vector */
    /* Counters for Euler's formula. */
    int 	V = 0, E = 0 , F = 0;
    /* Note: lowercase==pointer, uppercase==counter. */
    
    /*-- find X min & max --*/
    v = list.head;
    xmin = xmax = v.v.x;
    do {
      if( v.v.x > xmax ) xmax = v.v.x;
      else
	if( v.v.x < xmin ) xmin = v.v.x;
      v = v.next;
    } while ( v != list.head );
    
    /*-- find Y min & max --*/
    v = list.head;
    ymin = ymax = v.v.y;
    do {
      if( v.v.y > ymax ) ymax = v.v.y;
      else
	if( v.v.y < ymin ) ymin = v.v.y;
      v = v.next;
    } while ( v != list.head );
    
    
    /* Vertices. */
    v = list.head;
    do {                                 
      if( v.mark ) V++;           
      v = v.next;
    } while ( v != list.head );
 
    System.out.println("\nVertices:\tV = "+ V);
    System.out.println("index:\tx\ty\tz");
    do {                                 
      System.out.println( v.vnum+":\t"+v.v.x+"\t"+v.v.y+"\t"+v.v.z+"");
      System.out.println("newpath");
      System.out.println(v.v.x+"\t"+v.v.y+" 2 0 360 arc");
      System.out.println("closepath stroke\n");
      v = v.next;
    } while ( v != list.head );
    
    /* Faces. */
    /* visible faces are printed as PS output */
    f = flist.head;
    do {
      ++F;                              
      f  = f .next;
    } while ( f  != flist.head );
    System.out.println("\nFaces:\tF = "+F);
    System.out.println("Visible faces only:");
    do {           
      /* Print face only if it is lower */
      if ( f. lower )
	{
	  System.out.println("vnums:  "+f.vertex[0].vnum+"  "
			     +f.vertex[1].vnum+"  "+f.vertex[2].vnum);
	  System.out.println("newpath");
	  System.out.println(f.vertex[0].v.x+"\t"+f.vertex[0].v.y+"\tmoveto"); 
	  System.out.println(f.vertex[1].v.x+"\t"+f.vertex[1].v.y+"\tlineto");
	  System.out.println(f.vertex[2].v.x+"\t"+f.vertex[2].v.y+"\tlineto");
	  System.out.println("\n");
	}
      f = f.next;
    } while ( f != flist.head );
    
    /* prints a list of all faces */
    System.out.println("List of all faces:");
    System.out.println("\tv0\tv1\tv2\t(vertex indices)");
    do {
      System.out.println("\t"+f.vertex[0].vnum+
			 "\t"+f.vertex[1].vnum+
			 "\t"+f.vertex[2].vnum);
      f = f.next;
    } while ( f != flist.head );
    
    /* Edges. */	
    e = elist.head;
    do {
      E++;
      e = e.next;
    } while ( e != elist.head );
    System.out.println("\nEdges:\tE = "+E);
    /* Edges not printed out (but easily added). */
  
    check = true;
    CheckEuler( V, E, F );
  }

  /*---------------------------------------------------------------------
    SubVec:  Computes a - b and puts it into c.
    ---------------------------------------------------------------------*/
  private void    SubVec( int a[], int b[], int c[])
  {
    int  i;
    
    for( i=0; i < 2; i++ )
      c[i] = a[i] - b[i]; 
  }
  
  /*---------------------------------------------------------------------
    DoubleTriangle builds the initial double triangle.  It first finds 3 
    noncollinear points and makes two faces out of them, in opposite order.
    It then finds a fourth point that is not coplanar with that face.  The  
    vertices are stored in the face structure in counterclockwise order so 
    that the volume between the face and the point is negative. Lastly, the
    3 newfaces to the fourth point are constructed and the data structures
    are cleaned up. 
    ---------------------------------------------------------------------*/
  private boolean DoubleTriangle()
  {
    cVertex  v0, v1, v2, v3, t;
    cFace    f0, f1 = null;
    cEdge    e0, e1, e2, s;
    int      vol;
    
    
    /* Find 3 non-Collinear points. */
    v0 = list.head;
    while ( Collinear( v0, v0.next, v0.next.next ) )
      if ( ( v0 = v0.next ) == list.head ) {
	System.out.println("DoubleTriangle:  All points are Collinear!");
	return false;
      }
    v1 = v0.next;
    v2 = v1.next;
    
    /* Mark the vertices as processed. */
    v0.mark = PROCESSED;
    v1.mark = PROCESSED;
    v2.mark = PROCESSED;
    
    /* Create the two "twin" faces. */
    f0 = MakeFace( v0, v1, v2, f1 );
    f1 = MakeFace( v2, v1, v0, f0 );
    
    /* Link adjacent face fields. */
    f0.edge[0].adjface[1] = f1;
    f0.edge[1].adjface[1] = f1;
    f0.edge[2].adjface[1] = f1;
    f1.edge[0].adjface[1] = f0;
    f1.edge[1].adjface[1] = f0;
    f1.edge[2].adjface[1] = f0;
    
    /* Find a fourth, non-coplanar point to form tetrahedron. */
    v3 = v2.next;
    vol = VolumeSign( f0, v3 );
    while ( vol == 0 ) {
      if ( ( v3 = v3.next ) == v0 ) { 
	System.out.println("DoubleTriangle:  All points are coplanar!");
	return false;
      }
      vol = VolumeSign( f0, v3 );
    }
  
    /* Insure that v3 will be the first added. */
    list.head = v3;
    if ( debug ) {
      System.out.println("DoubleTriangle: finished. Head repositioned at v3.");
      PrintOut( list.head );
    }
    return true;
  }

  
  /*---------------------------------------------------------------------
    ConstructHull adds the vertices to the hull one at a time.  The hull
    vertices are those in the list marked as onhull.
    ---------------------------------------------------------------------*/
  private void	ConstructHull()
  {
    cVertex  v, vnext;
    int      vol;
    boolean     changed;	/* T if addition changes hull; not used. */
    
    v = list.head;
    do {
      vnext = v.next;
      if ( !v.mark ) {
	v.mark = PROCESSED;
	changed = AddOne( v );
	CleanUp();
	
	if ( check ) {
	  System.out.println("ConstrHull:After Add of "+v.vnum+"& Cleanup:");
	  Checks();
	}
	if ( debug )
	  PrintOut( v );
      }
      v = vnext;
    } while ( v != list.head );
  }
  
  /*---------------------------------------------------------------------
    AddOne is passed a vertex.  It first determines all faces visible from 
    that point.  If none are visible then the point is marked as not 
    onhull.  Next is a loop over edges.  If both faces adjacent to an edge
    are visible, then the edge is marked for deletion.  If just one of the
    adjacent faces is visible then a new face is constructed.
    ---------------------------------------------------------------------*/
  
  private  boolean 	AddOne( cVertex p )
  {
    cFace   f; 
    cEdge   e;
    int     vol;
    boolean vis = false;
    
    if ( debug ) {
      System.out.println("AddOne: starting to add v"+p.vnum);
      PrintOut( list.head );
    }
    
    /* Mark faces visible from p. */
    f = flist.head;
    do {
      vol = VolumeSign( f, p );
      if (debug) 
	System.out.println("faddr: "+f+"   paddr: "+p+"   Vol = "+vol);
      if ( vol < 0 ) {
	f.visible = VISIBLE;  
	vis = true;                      
      }
      f = f.next;
    } while ( f != flist.head );
    
    /* If no faces are visible from p, then p is inside the hull. */
    if ( !vis ) {
      p.onhull = !ONHULL;  
      return false; 
    }
    
    /* Mark edges in interior of visible region for deletion.
       Erect a newface based on each border edge. */
    e = elist.head;
    do {
      cEdge temp;
      temp = e.next;
      if ( e.adjface[0].visible && e.adjface[1].visible )
	/* e interior: mark for deletion. */
	e.delete = REMOVED;
      else if ( e.adjface[0].visible || e.adjface[1].visible ) 
	/* e border: make a new face. */
	e.newface = MakeConeFace( e, p );
      e = temp;
    } while ( e != elist.head );
    return true;
  }
  
  /*---------------------------------------------------------------------
    VolumeSign returns the sign of the volume of the tetrahedron determined 
    by f and p.  VolumeSign is +1 iff p is on the negative side of f,
    where the positive side is determined by the rh-rule.  So the volume 
    is positive if the ccw normal to f points outside the tetrahedron.
    The final fewer-multiplications form is due to Robert Fraczkiewicz.
    ---------------------------------------------------------------------*/
  private int  VolumeSign( cFace f, cVertex p )
  {
    double  vol;
    int     voli = 0;
    double  ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz;
    double  bxdx, bydy, bzdz, cxdx, cydy, czdz;
    
    ax = f.vertex[0].v.x;
    ay = f.vertex[0].v.y;
    az = f.vertex[0].v.z;
    bx = f.vertex[1].v.x;
    by = f.vertex[1].v.y;
    bz = f.vertex[1].v.z;
    cx = f.vertex[2].v.x;
    cy = f.vertex[2].v.y;
    cz = f.vertex[2].v.z;
    dx = p.v.x;
    dy = p.v.y;
    dz = p.v.z;
    
    bxdx=bx-dx;
    bydy=by-dy;
    bzdz=bz-dz;
    cxdx=cx-dx;
    cydy=cy-dy;
    czdz=cz-dz;
    vol =   (az-dz) * (bxdx*cydy - bydy*cxdx)
      + (ay-dy) * (bzdz*cxdx - bxdx*czdz)
      + (ax-dx) * (bydy*czdz - bzdz*cydy);
    
    if ( debug )
      System.out.println("Face="+f+"; Vertex="+p.vnum
			 +": vol(int) = "+voli+", vol(double) = "+vol);
    
    /* The volume should be an integer. */
    if      ( vol > 0.5 )   return  1;
    else if ( vol < -0.5 )  return -1;
    else                    return  0;
  }
  /*---------------------------------------------------------------------*/
  private int  Volumei( cFace f, cVertex p )
  {
    int 	   vol;
    int 	   ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz;
    int	   bxdx, bydy, bzdz, cxdx, cydy, czdz;
    double  vold;
    int	   i;
    
    ax = f.vertex[0].v.x;
    ay = f.vertex[0].v.y;
    az = f.vertex[0].v.z;
    bx = f.vertex[1].v.x;
    by = f.vertex[1].v.y;
    bz = f.vertex[1].v.z;
    cx = f.vertex[2].v.x;
    cy = f.vertex[2].v.y;
    cz = f.vertex[2].v.z;
    dx = p.v.x;
    dy = p.v.y;
    dz = p.v.z;
   
    bxdx=bx-dx;
    bydy=by-dy;
    bzdz=bz-dz;
    cxdx=cx-dx;
    cydy=cy-dy;
    czdz=cz-dz;
    vol =   (az-dz)*(bxdx*cydy-bydy*cxdx)
      + (ay-dy)*(bzdz*cxdx-bxdx*czdz)
      + (ax-dx)*(bydy*czdz-bzdz*cydy);
    
    return vol;
  }		
  
  /*---------------------------------------------------------------------
    Volumed is the same as VolumeSign but computed with doubles.  For 
    protection against overflow.
    ---------------------------------------------------------------------*/
  private double 	Volumed( cFace f, cVertex p )
  {
    double  vol;
    double  ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz;
    double  bxdx, bydy, bzdz, cxdx, cydy, czdz;
    
    ax = f.vertex[0].v.x;
    ay = f.vertex[0].v.y;
    az = f.vertex[0].v.z;
    bx = f.vertex[1].v.x;
    by = f.vertex[1].v.y;
    bz = f.vertex[1].v.z;
    cx = f.vertex[2].v.x;
    cy = f.vertex[2].v.y;
    cz = f.vertex[2].v.z;
    dx = p.v.x;
    dy = p.v.y;
    dz = p.v.z;
    
    bxdx=bx-dx;
    bydy=by-dy;
    bzdz=bz-dz;
    cxdx=cx-dx;
    cydy=cy-dy;
    czdz=cz-dz;
    vol = (az-dz)*(bxdx*cydy-bydy*cxdx)
      + (ay-dy)*(bzdz*cxdx-bxdx*czdz)
      + (ax-dx)*(bydy*czdz-bzdz*cydy);
    
    return vol;
  }
  
  /*---------------------------------------------------------------------
    MakeConeFace makes a new face and two new edges between the 
    edge and the point that are passed to it. It returns a pointer to
    the new face.
    ---------------------------------------------------------------------*/
  private cFace	MakeConeFace( cEdge e, cVertex p )
  {
    cEdge  new_edge[] = new cEdge[2];
    cFace  new_face;
    int 	  i, j;
    
    /* Make two new edges (if don't already exist). */
    for ( i = 0; i < 2; ++i ) { 
      /* If the edge exists, copy it into new_edge. */
      new_edge[i] = e.endpts[i].duplicate;
      if ( new_edge[i] == null ) {
	/* Otherwise (duplicate is null), MakeNullEdge. */
	new_edge[i] = elist.MakeNullEdge();
	new_edge[i].endpts[0] = e.endpts[i];
	new_edge[i].endpts[1] = p;
	e.endpts[i].duplicate = new_edge[i];
      }
    }
    
    /* Make the new face. */
    new_face = flist.MakeNullFace();   
    new_face.edge[0] = e;
    new_face.edge[1] = new_edge[0];
    new_face.edge[2] = new_edge[1];
    MakeCcw( new_face, e, p ); 
    
    /* Set the adjacent face pointers. */
    for ( i=0; i < 2; ++i )
      for ( j=0; j < 2; ++j )  
	/* Only one NULL link should be set to new_face. */
	if ( new_edge[i].adjface[j] == null ) {
	  new_edge[i].adjface[j] = new_face;
	  break;
	}
        
    return new_face;
  }

  /*---------------------------------------------------------------------
    MakeCcw puts the vertices in the face structure in counterclock wise 
    order.  We want to store the vertices in the same 
    order as in the visible face.  The third vertex is always p.
    ---------------------------------------------------------------------*/
  private void	MakeCcw( cFace f, cEdge e, cVertex p )
  {
    cFace  fv;                  /* The visible face adjacent to e */
    int    i;                   /* Index of e.endpoint[0] in fv. */
    cEdge  s = new cEdge();     /* Temporary, for swapping */
    
    if  ( e.adjface[0].visible )      
      fv = e.adjface[0];
    else fv = e.adjface[1];
    
    /* Set vertex[0] & [1] of f to have the same orientation
       as do the corresponding vertices of fv. */ 
    for ( i=0; fv.vertex[i] != e.endpts[0]; ++i )
      ;
    /* Orient f the same as fv. */
    if ( fv.vertex[ (i+1) % 3 ] != e.endpts[1] ) {
      f.vertex[0] = e.endpts[1];  
      f.vertex[1] = e.endpts[0];    
    }
    else {                               
      f.vertex[0] = e.endpts[0];   
      f.vertex[1] = e.endpts[1];      
      Swap( s, f.edge[1], f.edge[2] );
    }
    /* This swap is tricky. e is edge[0]. edge[1] is based on endpt[0],
       edge[2] on endpt[1].  So if e is oriented "forwards," we
       need to move edge[1] to follow [0], because it precedes. */
    
    f.vertex[2] = p;
  }

  /*---------------------------------------------------------------------
    MakeFace creates a new face structure from three vertices (in ccw
    order).  It returns a pointer to the face.
    ---------------------------------------------------------------------*/
  private cFace   MakeFace( cVertex v0, cVertex v1, cVertex v2, cFace fold )
    {
      cFace  f;
      cEdge  e0, e1, e2;
      
      /* Create edges of the initial triangle. */
      if( fold == null ) {
	e0 = elist.MakeNullEdge();
	e1 = elist.MakeNullEdge();
	e2 = elist.MakeNullEdge();
      }
      else { /* Copy from fold, in reverse order. */
	e0 = fold.edge[2];
	e1 = fold.edge[1];
	e2 = fold.edge[0];
      }
      e0.endpts[0] = v0;              e0.endpts[1] = v1;
      e1.endpts[0] = v1;              e1.endpts[1] = v2;
      e2.endpts[0] = v2;              e2.endpts[1] = v0;
      
      /* Create face for triangle. */
      f = flist.MakeNullFace();
      f.edge[0]   = e0;  f.edge[1]   = e1; f.edge[2]   = e2;
      f.vertex[0] = v0;  f.vertex[1] = v1; f.vertex[2] = v2;
      
      /* Link edges to face. */
      e0.adjface[0] = e1.adjface[0] = e2.adjface[0] = f;
      
      return f;
    }
  
  /*---------------------------------------------------------------------
    CleanUp goes through each data structure list and clears all
    flags and NULLs out some pointers.  The order of processing
    (edges, faces, vertices) is important.
    ---------------------------------------------------------------------*/
  private void	CleanUp()
  {
    CleanEdges();
    CleanFaces();
    CleanVertices();
  }
  
  /*---------------------------------------------------------------------
    CleanEdges runs through the edge list and cleans up the structure.
    If there is a newface then it will put that face in place of the 
    visible face and NULL out newface. It also deletes so marked edges.
    ---------------------------------------------------------------------*/
  private void	CleanEdges()
  {
    cEdge  e;	/* Primary index into edge list. */
    cEdge  t;	/* Temporary edge pointer. */
    
    /* Integrate the newface's into the data structure. */
    /* Check every edge. */
    e = elist.head;
    do {
      if ( e.newface != null ) { 
	if ( e.adjface[0].visible )
	    e.adjface[0] = e.newface; 
	else	e.adjface[1] = e.newface;
	e.newface = null;
      }
      e = e.next;
    } while ( e != elist.head );
    
    /* Delete any edges marked for deletion. */
    while ( elist.head != null && elist.head.delete ) { 
      e = elist.head;
      elist.Delete( e );
    }
    e = elist.head.next;
    do {
      if ( e.delete ) {
	t = e;
	e = e.next;
	elist.Delete( t );
      }
      else e = e.next;
    } while ( e != elist.head );
  }
  
  /*---------------------------------------------------------------------
    CleanFaces runs through the face list and deletes any face marked visible.
    ---------------------------------------------------------------------*/
  private void	CleanFaces()
  {
    cFace  f;	/* Primary pointer into face list. */
    cFace  t;	/* Temporary pointer, for deleting. */
    
    
    while ( flist.head != null && flist.head.visible ) { 
      f = flist.head;
      flist.Delete( f );
    }
    f = flist.head.next;
    do {
      if ( f.visible ) {
	t = f;
	f = f.next;
	flist.Delete( t );
      }
      else f = f.next;
    } while ( f != flist.head );
  }
  
  /*---------------------------------------------------------------------
    CleanVertices runs through the vertex list and deletes the 
    vertices that are marked as processed but are not incident to any 
    undeleted edges. 
    ---------------------------------------------------------------------*/
  private void	CleanVertices()
  {
    cEdge    e;
    cVertex  v, t;
    
    /* Mark all vertices incident to some undeleted edge as on the hull. */
    e = elist.head;
    do {
      e.endpts[0].onhull = e.endpts[1].onhull = ONHULL;
      e = e.next;
    } while (e != elist.head);
    
    /* Delete all vertices that have been processed but
       are not on the hull. */
    while ( list.head != null&& list.head.mark && !list.head.onhull ) { 
      v = list.head;
      list.Delete( v );
    }
    v = list.head.next;
    do {
      if ( v.mark && !v.onhull ) {    
	t = v; 
	v = v.next;
	list.Delete( t );
      }
      else v = v.next;
    } while ( v != list.head );
    
    /* Reset flags. */
    v = list.head;
    do {
      v.duplicate = null; 
      v.onhull = !ONHULL; 
      v = v.next;
    } while ( v != list.head );
  }
  
  /*---------------------------------------------------------------------
    Collinear checks to see if the three points given are collinear,
    by checking to see if each element of the cross product is zero.
    ---------------------------------------------------------------------*/
  private boolean Collinear( cVertex a, cVertex b, cVertex c )
  {
    return 
      ( c.v.z - a.v.z ) * ( b.v.y - a.v.y ) -
      ( b.v.z - a.v.z ) * ( c.v.y - a.v.y ) == 0
      && ( b.v.z - a.v.z ) * ( c.v.x - a.v.x ) -
      ( b.v.x - a.v.x ) * ( c.v.z - a.v.z ) == 0
      && ( b.v.x - a.v.x ) * ( c.v.y - a.v.y ) -
      ( b.v.y - a.v.y ) * ( c.v.x - a.v.x ) == 0  ;
  }
  
  /*---------------------------------------------------------------------
    Computes the z-coordinate of the vector normal to face f.
    ---------------------------------------------------------------------*/
  private int	Normz( cFace f )
  {
    cVertex a, b, c;
    /*double ba0, ca1, ba1, ca0,z;*/
    
    a = f.vertex[0];
    b = f.vertex[1];
    c = f.vertex[2];
    
    /*
      ba0 = ( b.v.x - a.v.x );
      ca1 = ( c.v.y - a.v.y );
      ba1 = ( b.v.y - a.v.y );
      ca0 = ( c.v.x - a.v.x );
      
      z = ba0 * ca1 - ba1 * ca0; 
      System.out.println("Normz = %lf=%g\n", z,z);
      if      ( z > 0.0 )  return  1;
      else if ( z < 0.0 )  return -1;
      else                 return  0;
    */
    return 
      ( b.v.x - a.v.x ) * ( c.v.y - a.v.y ) -
      ( b.v.y - a.v.y ) * ( c.v.x - a.v.x );
  }

  /*---------------------------------------------------------------------
    Consistency runs through the edge list and checks that all
    adjacent faces have their endpoints in opposite order.  This verifies
    that the vertices are in counterclockwise order.
    ---------------------------------------------------------------------*/
  private void	Consistency()
  {
    cEdge  e;
    int    i, j;
    
    e = elist.head;
    
    do {
      /* find index of endpoint[0] in adjacent face[0] */
      for ( i = 0; e.adjface[0].vertex[i] != e.endpts[0]; ++i )
	;
      
      /* find index of endpoint[0] in adjacent face[1] */
      for ( j = 0; e.adjface[1].vertex[j] != e.endpts[0]; ++j )
	;
      
      /* check if the endpoints occur in opposite order */
      if ( !( e.adjface[0].vertex[ (i+1) % 3 ] ==
	      e.adjface[1].vertex[ (j+2) % 3 ] ||
	      e.adjface[0].vertex[ (i+2) % 3 ] ==
	      e.adjface[1].vertex[ (j+1) % 3 ] )  )
	break;
      e = e.next;
      
    } while ( e != elist.head );
    
    if ( e != elist.head )
      System.out.println("Checks: edges are NOT consistent.");
    else
      System.out.println("Checks: edges consistent.");
  }
  
  /*---------------------------------------------------------------------
    Convexity checks that the volume between every face and every
    point is negative.  This shows that each point is inside every face
    and therefore the hull is convex.
    ---------------------------------------------------------------------*/
  private void	Convexity()
  {
    cFace    f;
    cVertex  v;
    int               vol;
    
    f = flist.head;
    
    do {
      v = list.head;
      do {
	if ( v.mark ) {
	  vol = VolumeSign( f, v );
	  if ( vol < 0 )
	    break;
	}
	v = v.next;
      } while ( v != list.head );
      
      f = f.next;
      
    } while ( f != flist.head );
    
    if ( f != flist.head )
      System.out.println("Checks: NOT convex.");
    else if ( check ) 
      System.out.println("Checks: convex.");
  }
  
  /*---------------------------------------------------------------------
    CheckEuler checks Euler's relation, as well as its implications when
    all faces are known to be triangles.  Only prints positive information
    when debug is true, but always prints negative information.
    ---------------------------------------------------------------------*/
  private void	CheckEuler( int V, int E, int F )
  {
    if ( check )
      System.out.print("Checks: V, E, F = "+V+", "+E+", "+F+"\n");
    
    if ( (V - E + F) != 2 )
      System.out.print(" Checks: V-E+F != 2\n");
    else if ( check )
      System.out.print(" V-E+F = 2\t\n");
    
    
    if ( F != (2 * V - 4) )
      System.out.print(" Checks: F=" +F+ " != 2V-4=" +(2*V-4)+ "; V=" +V);
    
    else if ( check ) 
      System.out.println ("F = 2V-4\t");
    
    if ( (2 * E) != (3 * F) )
      System.out.println(" Checks: 2E="+2*E+" != 3F="+3*F+"; E="+E+", F="+F);
    else if ( check ) 
      System.out.println(" 2E = 3F");
  }
  
  /*-------------------------------------------------------------------*/
  private void	Checks()
  {
    cVertex  v;
    cEdge    e;
    cFace    f;
    int 	   V = 0, E = 0 , F = 0;
    
    Consistency();
    Convexity();
    
    v = list.head;
    if ( v != null )
      do {
	if (v.mark) V++;
	v = v.next;
      } while ( v != list.head );
    
    e = elist.head;
    if ( e != null )
      do {
	E++;
	e = e.next;
      } while ( e != elist.head );
    
    f = flist.head;
    if ( f != null )
      do {
	F++;
	f  = f.next;
      } while ( f  != flist.head );
    CheckEuler( V, E, F );
  }
  
  
  /*===================================================================
    These functions are used whenever the debug flag is set.
    They print out the entire contents of each data structure.  
    Printing is to standard error.  To grab the output in a file in the csh, 
    use this:
    chull < i.file >&! o.file
    =====================================================================*/
  /*-------------------------------------------------------------------*/
  private void	PrintOut( cVertex v )
  {
    System.out.println("Head vertex "+v.vnum+" =  "+v+"\t:");
    PrintVertices();
    PrintEdges();
    PrintFaces();
  }
  
  /*-------------------------------------------------------------------*/
  private void	PrintVertices()
  {
    cVertex  temp;
    
    temp = list.head;
    System.out.println ("Vertex List");
    if (list.head != null) do {
      System.out.print("  addr "+list.head+"\t");
      System.out.print("  vnum "+list.head.vnum);
      System.out.println("   ("+list.head.v.x+","
			 +list.head.v.y+","
			 +list.head.v.z+")");
      System.out.println("   active:"+list.head.onhull);
      System.out.println("   dup:"+list.head.duplicate );
      System.out.println("   mark:\n"+ list.head.mark );
      list.head = list.head.next;
    } while ( list.head != temp );
    
  }
  
  /*-------------------------------------------------------------------*/
  private void PrintEdges()
  {
    cEdge  temp;
    int 	  i;
    
    temp = elist.head;
    System.out.println ("Edge List");
    if (elist.head != null) do {
      System.out.print("  addr: "+elist.head+"\t");
      System.out.print("adj: ");
      for (i=0; i<2; ++i) 
	System.out.print( elist.head.adjface[i] );
      System.out.print("  endpts:");
      for (i=0; i<2; ++i) 
	System.out.print(elist.head.endpts[i].vnum);
      System.out.print( "  del:"+elist.head.delete+"\n");
      elist.head = elist.head.next; 
    } while (elist.head != temp );
    
  }
  
  /*-------------------------------------------------------------------*/
  private void	PrintFaces()
  {
    int 	  i;
    cFace  temp;
    
    temp = flist.head;
    System.out.print ( "Face List\n");
    if (flist.head != null) do {
      System.out.print( "  addr: "+flist.head+"\t");
      System.out.print( "  edges:");
      for( i=0; i<3; ++i )
	System.out.print( flist.head.edge[i] );
      System.out.print( "  vert:");
      for ( i=0; i<3; ++i)
	System.out.print( flist.head.vertex[i].vnum );
      System.out.print( "  vis: "+flist.head.visible+"\n");
      flist.head= flist.head.next;
    } while ( flist.head != temp );  
  }
  
  private void Swap(cEdge t, cEdge x, cEdge y)
  {
    t = x;
    x = y;
    y = t;
  } 
}
