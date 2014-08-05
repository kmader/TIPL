/*--------------------------------------------------------------------------
 * Class cFace -- face of polyhydra or planar graph;
 *                in this case it is a triangular face
 *
 * cFace() - constructor
 * PrintFace(int k) - prints face k to the console
 *
 *-------------------------------------------------------------------------*/
public class cFace {

  cEdge     edge[];             /* edges which compose the face */
  cVertex   vertex[];           /* vertices which bound the face */
  boolean   visible;	        /* T iff face visible from new point. */
  boolean   lower;              /* T iff on the lower hull */
  cFace     next, prev;

  cFace()
  {
    edge = new cEdge[3];
    edge[0] = edge[1] = edge[2] = null;
    vertex = new cVertex[3];
    vertex[0] = vertex[1] = vertex[2] = null;
    visible = lower = false;
    next = prev = null;
  }

  public void PrintFace(int k)
  {
    System.out.println("Face"+k+":: edges...");
    edge[0].PrintEdge(0);
    edge[1].PrintEdge(1);
    edge[2].PrintEdge(2);    
  }
}
