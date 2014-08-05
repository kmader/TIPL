/*---------------------------------------------------------------------------
 * Class cEdgeList -- used for storing a group of edges
 *
 * MakeNullEdge() -- makes a default edge and inserts it to the end of the list
 * ClearEdgeList()-- freeing up resourses
 * InsertBeforeHead (cEdge e)
 * InsertBefore (cEdge newE , cEdge oldE) 
 * Delete (cEdge e)
 * PrintEdges() -- prints to the console
 *
 */
class cEdgeList {
  int n;                /* number of elements: 0 means empty */
  cEdge head;           /* head pointer to the beginning of the list */

  cEdgeList() {
    head = null;
    n = 0;
  }

  public cEdge MakeNullEdge()
  {
    cEdge e = new cEdge();
    InsertBeforeHead(e);
    return e;
  }

  public void InitHead( cEdge h )
  {
    head = new cEdge();
    head = h;
    head.next = head.prev = head;
    n = 1;
  }

  public void ClearEdgeList()
  {
    if (head != null)
      head = null;
    n = 0;
  }

  /*Inserts newE before oldE
   */
  public void InsertBeforeHead( cEdge e ) {
    if ( head == null )
	InitHead( e );
    else {
        InsertBefore ( e, head );
    }
  }

  public void InsertBefore( cEdge newE , cEdge oldE ) { 		
    if ( head == null )
	InitHead( newE );
    else {
        oldE.prev.next = newE;		
    	newE.prev = oldE.prev;		
    	newE.next = oldE;			       
    	oldE.prev = newE;
	n++;
    }
  }

  public void Delete( cEdge e ) {

    if ( head == head.next )
      head = null;
    else if ( e == head )
      head = head.next;

    e.prev.next = e.next;
    e.next.prev = e.prev;
    n--;
           
  }

  public void PrintEdges() {
    cEdge temp = head;
    int i = 1;
    if (head != null) {
      do {
	temp.PrintEdge(i);
	temp = temp.next;
	i++;
      } while ( temp != head );
    }
  }
}
