/*-----------------------------------------------------------------------------
 * Class cEdge is used to represent an edge of a polygon or polyhydra
 *-----------------------------------------------------------------------------*/
public class cEdge {
  
  cFace    adjface[];          /* adjacent face; 2 */
  cVertex  endpts[];           /* end points of the edge */ 
  cFace    newface;            /* pointer to incident cone face. */
  boolean  delete;	       /* T iff edge should be delete. */
  cEdge    next, prev;         /* pointers to neighbours in cEdgeList */
  
  cEdge()
  {
    adjface = new cFace[2];
    adjface[0] = adjface[1] = null;
    endpts = new cVertex[2];
    endpts[0] = endpts[1] = null;
    newface = null;
    delete = false;
    next = prev = null;
  }

  public void PrintEdge(int n)
  {
    if (this != null) {
      System.out.print("Edge" + n + ": ");
      endpts[0].PrintVertex();
      System.out.print(" ");
      endpts[1].PrintVertex();
      System.out.print("; ");
      System.out.println("");
    }
    else 
      System.out.println("no edge");
  }
}
