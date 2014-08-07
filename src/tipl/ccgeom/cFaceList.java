package tipl.ccgeom;

import java.util.ArrayList;
import java.util.List;

/*-----------------------------------------------------------------------------
 * Class cFaceList -- stores faces in a form of a list datastructure
 * 
 * MakeNullFace()  -- makes default face and inserts it to the end of the list;
 * InsertBeforeHead(cFace e);
 * InsertBefore(cFace newF, cFace oldF);
 * Delete (cFace f);  
 * ClearFaceList() -- used to free up the resources;
 * PrintFace()     -- prints face to the console;
 *----------------------------------------------------------------------------*/
public class cFaceList {
  int n;                /* Number of faces in the list: 0 means empty */
  cFace head;

  cFaceList() {
    head = null;
    n = 0;
  }
  
  
  public List<cFaceBasic> toList() {
	  List<cFaceBasic> outList = new ArrayList<cFaceBasic>(n);
	  cFace cEle = head;
	  if (head==null) return outList;
	  do {
		  outList.add(cEle.asBasic());
		  cEle=cEle.next;
	  } while (cEle!=head);
	  return outList;
  }
  
  public double totalArea() {
	  cFace cEle = head;
	  double area=0.0;
	  if (head==null) return area;
	  
	  do {
		  area+=cEle.getArea();
		  cEle=cEle.next;
	  } while (cEle!=head);
	  return area;
  }

  public cFace MakeNullFace()
  {
    cFace f = new cFace();
    InsertBeforeHead(f);
    return f;
  }

  public void InitHead( cFace h )
  {
    head = new cFace();
    head = h;
    head.next = head.prev = head;
    n = 1;
  }

  public void ClearFaceList()
  {
    if (head != null)
      head = null;
    n = 0;
  }

  /*Inserts newF before oldF
   */
  public void InsertBeforeHead( cFace e ) {
    if ( head == null )
	InitHead( e );
    else {
        InsertBefore ( e, head );
    }
  }

  public void InsertBefore( cFace newF , cFace oldF ) { 		
    if ( head == null )
	InitHead( newF );
    else {
        oldF.prev.next = newF;		
    	newF.prev = oldF.prev;		
    	newF.next = oldF;			       
    	oldF.prev = newF;
	n++;
    }
  }

  public void Delete( cFace e ) {

    if ( head == head.next )
      head = null;
    else if ( e == head )
      head = head.next;

    e.prev.next = e.next;
    e.next.prev = e.prev;
    n--;
           
  }

  public void PrintFaces() {
    cFace temp = head;
    int i = 1;
    if (head != null) {
      do {
	temp.PrintFace(i);
	temp = temp.next;
	i++;
      } while ( temp != head );
    }
  }
  /**
   * cFace without C like pointers
   * @author mader
   *
   */
  public class cFaceBasic {
	  cEdge     edge[];             /* edges which compose the face */
	    cVertex   vertex[];           /* vertices which bound the face */
	    boolean   visible;	        /* T iff face visible from new point. */
	    boolean   lower;              /* T iff on the lower hull */
	    cFaceBasic()
	    {
	      edge = new cEdge[3];
	      edge[0] = edge[1] = edge[2] = null;
	      vertex = new cVertex[3];
	      vertex[0] = vertex[1] = vertex[2] = null;
	      visible = lower = false;
	      
	    }
	    cFaceBasic(cEdge[] edge, cVertex[] vertex,boolean visible, boolean lower) {
	    	this.edge=edge;
	    	this.vertex=vertex;
	    	this.visible=visible;
	    	this.lower=lower;
	    }
	    public String toString() {
	    	return visible+", A:"+getArea()+", E:"+edge[0]+"; "+edge[1]+"; "+edge[2];  
	    }
	    public void PrintFace(int k)
	    {
	      System.out.println("Face"+k+":: edges..."+"\n"+toString());
	    }
	    public boolean isVisible() { return visible;}
	    public cEdge[] getEdges() { return edge;}
	    public double getArea() {
	    	cPointi a = edge[0].getVec();
	    	cPointi b = edge[1].getVec();
	    	cPointi c = edge[2].getVec();
	    	double x = 1.0*a.y*b.z-a.z*b.y;
	    	double y = 1.0*a.z*b.x-a.x*b.z;
	    	double z = 1.0*a.x*b.y-a.y*b.x;
	    	return Math.sqrt(Math.pow(x, 2)+Math.pow(y, 2)+Math.pow(z, 2))/2;
	    }
  }
  /*--------------------------------------------------------------------------
   * Class cFace -- face of polyhydra or planar graph;
   *                in this case it is a triangular face
   *
   * cFace() - constructor
   * PrintFace(int k) - prints face k to the console
   *
   *-------------------------------------------------------------------------*/
  protected class cFace extends cFaceBasic {

    cFace     next, prev;

    cFace()
    {
      super();
      next = prev = null;
    }
    public cFaceBasic asBasic() {
    	return new cFaceBasic(edge,vertex,visible,lower);
    }


  }
}
