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
class cFaceList {
  int n;                /* Number of faces in the list: 0 means empty */
  cFace head;

  cFaceList() {
    head = null;
    n = 0;
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
}
