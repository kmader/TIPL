/*------------------------------------------------------------------
class cVertex.

This corresponds to the C struct cVertex -- type vertex.
It is one cell of a vertex list, holding the point v,
next and previous pointers, and Boolean flags (e.g., ear).
Its only methods besides constuctors are printing methods
and ResetVertexTo3D (which raises a point onto a paraboloid).
-------------------------------------------------------------------*/

class cVertex {
    
  cVertex prev, next;
  cPointi v;
  boolean ear = false;
  int vnum;
  cEdge duplicate;
  boolean  onhull;		/* T iff point on hull. */
  boolean  mark;

  cVertex() {
    prev = next = null;
    v = new cPointi();
    vnum = 0;
    duplicate = null;
    onhull = false;
    mark = false;
  }

  cVertex(int i, int j) {	
    v = new cPointi();
    v.x = i;
    v.y = j;
    v.z = i*i + j*j;
    prev = next = null;
  }

  cVertex(int x, int y, int z) {
    v = new cPointi();
    v.x = x;
    v.y = y;
    v.z = z;
    prev = next = null;
  }

  /* Raises point to 3D by placing in on paraboloid */
  public void ResetVertex3D()
  {
    v.z = v.x*v.x + v.y*v.y;
  }
   
  public void PrintVertex(int index) {
    System.out.print ( "V" + index + " = " );
    v.PrintPoint();
  }

  public void PrintVertex() {
    v.PrintPoint();
  }

  public void PrintVertex3D()
  {
    System.out.print("V"+vnum+" = ("+ v.x + ", " + v.y + ", " + v.z+"); ");
  }

  public void PrintVertex3D(int k)
  {
    System.out.print("V"+k+" = ("+ v.x + ", " + v.y + ", " + v.z + "); ");
  }
} 









