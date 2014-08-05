/*----------------------------------------------------------------------
 * class cPointi.
 *
 * This corresponds to the C struct cPointi -- type point, integer.
 * It holds the x and y coordinates as integer data fields.
 * It contains all the methods that specifically work on points
 * (as opposed to those that are vertex-specific): triangle area
 * (Area2), Collinear, Between, etc.
 * Also there are routines that compute the distance between
 * two points (Dist), the distance between a point and a segment
 * (DistEdgePoint); these functions are called during polygon
 * editing, to determine which point or edge the user has selected
 * with a mouse click.
 * SegSegInt: Finds the point of intersection p between two closed
 *
 *---------------------------------------------------------------------*/

class cPointi {
  int x;
  int y;
  int z;

  cPointi() 
  { 
    x=y=z=0;
  }

  cPointi (int x, int y) 
  {
    this.x = x;
    this.y = y;
    this.z = 0;
  }

  cPointi (int x, int y, int z) 
  {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  /*Returns the distance of the input point 
   *from its perp. proj. to the e1 edge.
   *Uses method detailed in comp.graphics.algorithms FAQ 
   */
  public double DistEdgePoint(cPointi a, cPointi b, cPointi c) 
  {
    double r, s;
    double length;
    double dproj = 0.0;
    length =  Math.sqrt(Math.pow((b.x - a.x), 2) +
			Math.pow((b.y - a.y), 2));

    if (length == 0.0) {
      System.out.println("DistEdgePoint: Length = 0");
      a.PrintPoint();
      b.PrintPoint();
      c.PrintPoint();
    }
    r = (((a.y - c.y) * (a.y - b.y))
       - ((a.x - c.x) * (b.x - a.x))) / (length * length);  
    s = (((a.y - c.y) * (b.x - a.x))
       - ((a.x - c.x) * (b.y - a.y))) / (length * length); 

    dproj =  Math.abs (s * length);

    //    System.out.println("XI = " + (a.x + r *(b.x-a.x))+" YI = "+(a.y+r*(b.y-a.y)));
    if((s != 0.0) && ((0.0 <= r) && (r <= 1.0)))
       return dproj;
    if((s == 0.0) && Between( a, b, c ))
       return 0.0;
    else 
    {
       double ca = Dist(a,c);
       double cb = Dist(b,c);
       return Math.min(ca, cb);
    }
  }

  public double Dist(cPointi p, cPointi p1) //returns the distance of two points
  {
    double l = Math.sqrt(Math.pow((p.x-p1.x),2)+Math.pow((p.y-p1.y),2));
    return l;
  }
  
  public double Dist(cPointi p) //returns the distance of two points
  {
    double l = Math.sqrt(Math.pow((p.x-this.x),2)+Math.pow((p.y-this.y),2));
    return l;
  }

  /*Centroid of triangle is just an average of vertices
   */
  public cPointi Centroid3( cPointi p1, cPointi p2, cPointi p3)
  {
      cPointi c = new cPointi();
      c.x = p1.x + p2.x + p3.x;
      c.y = p1.y + p2.y + p3.y;
      return c;
  }

  /*The signed area of the triangle det. by a,b,c; pos. if ccw, neg. if cw
   */
  public int Area2(cPointi a, cPointi b, cPointi c)
  {	   
    int area=((c.x - b.x)*(a.y - b.y)) - ((a.x - b.x)*(c.y - b.y));
      return area;
  }

  public int AreaSign( cPointi a, cPointi b, cPointi c )
  {
    double area2;
    
    area2 = ( b.x - a.x ) * (double)( c.y - a.y ) -
            ( c.x - a.x ) * (double)( b.y - a.y );

    
    /* The area should be an integer. */
    if      ( area2 >  0.5 ) return  1;
    else if ( area2 < -0.5 ) return -1;
    else                     return  0;
  }

  /*---------------------------------------------------------------------
   *Returns true iff c is strictly to the left of the directed
   *line through a to b.
   */
  public boolean Left( cPointi a, cPointi b, cPointi c )
  {
    return  AreaSign( a, b, c ) > 0;
  }

  public boolean LeftOn( cPointi a, cPointi b , cPointi c)
  {
    return  AreaSign( a, b, c) >= 0;
  }

  public boolean Collinear( cPointi a, cPointi b, cPointi c)
  {
    return  AreaSign( a, b, c) == 0;
  }

  /*---------------------------------------------------------------------
   *Returns true iff point c lies on the closed segement ab.
   *First checks that c is collinear with a and b.
   */
  public boolean Between( cPointi a, cPointi b, cPointi c)
  {
    cPointi      ba, ca;
    
    if ( ! Collinear( a, b, c) )
      return  false;
    
    /* If ab not vertical, check betweenness on x; else on y. */
    if ( a.x != b.x )
      return ((a.x <= c.x) && (c.x <= b.x)) ||
	     ((a.x >= c.x) && (c.x >= b.x));
    else
      return ((a.y <= c.y) && (c.y <= b.y)) ||
	     ((a.y >= c.y) && (c.y >= b.y));
  }

  /*---------------------------------------------------------------------
   *Returns TRUE iff segments ab & cd intersect, properly or improperly.
   */
  public boolean  Intersect( cPointi a, cPointi b, cPointi c, cPointi d )
  {
    if      ( IntersectProp( a, b, c, d ) )
      return  true;

    else if (   Between( a, b, c)
	     || Between( a, b, d )
	     || Between( c, d, a )
	     || Between( c, d, b ) )
      return  true;

    else    
      return  false;
  }

  public boolean IntersectProp( cPointi a, cPointi b, cPointi c, cPointi d )
  {
    /* Eliminate improper cases. */
    if (
	Collinear(a,b,c) ||
	Collinear(a,b,d) ||
	Collinear(c,d,a) ||
	Collinear(c,d,b))
      return false;
    
    return
         Xor( Left(a,b,c), Left(a,b,d) )
      && Xor( Left(c,d,a), Left(c,d,b) );
  }

  /*---------------------------------------------------------------------
   *Exclusive or: true iff exactly one argument is true.
   */
  public boolean Xor( boolean x, boolean y )
  {
    /* The arguments are negated to ensure that they are 0/1 values. */
    /* (Idea due to Michael Baldwin.) */
    return   !x ^ !y;
  }

  /*---------------------------------------------------------------------
    SegSegInt: Finds the point of intersection p between two closed
    segments ab and cd.  Returns p and a char with the following meaning:
    'e': The segments collinearly overlap, sharing a point.
    'v': An endpoint (vertex) of one segment is on the other segment,
    but 'e' doesn't hold.
    '1': The segments intersect properly (i.e., they share a point and
    neither 'v' nor 'e' holds).
    '0': The segments do not intersect (i.e., they share no points).
    Note that two collinear segments that share just one point, an endpoint
    of each, returns 'e' rather than 'v' as one might expect.
    ---------------------------------------------------------------------*/
  public char SegSegInt( cPointi a, cPointi b, cPointi c, cPointi d, cPointd p, cPointd q )
  {
    double  s, t;       /* The two parameters of the parametric eqns. */
    double num, denom;  /* Numerator and denoninator of equations. */
    char code = '?';    /* Return char characterizing intersection. */
    p.x = p.y = 100.0;  /* For testing purposes only... */

    denom = a.x * (double)( d.y - c.y ) +
            b.x * (double)( c.y - d.y ) +
            d.x * (double)( b.y - a.y ) +
            c.x * (double)( a.y - b.y );

    /* If denom is zero, then segments are parallel: handle separately. */
    if (denom == 0.0) 
      return  ParallelInt(a, b, c, d, p, q);
    
    num =    a.x * (double)( d.y - c.y ) +
	     c.x * (double)( a.y - d.y ) +
             d.x * (double)( c.y - a.y );
    if ( (num == 0.0) || (num == denom) ) code = 'v';
    s = num / denom;
    System.out.println("SegSegInt: num=" + num + ",denom=" + denom + ",s="+s);
    
    num = -( a.x * (double)( c.y - b.y ) +
	     b.x * (double)( a.y - c.y ) +
	     c.x * (double)( b.y - a.y ) );
    if ( (num == 0.0) || (num == denom) ) code = 'v';
    t = num / denom;
    System.out.println("SegSegInt: num=" +num + ",denom=" + denom + ",t=" + t);
    
    if      ( (0.0 < s) && (s < 1.0) &&
	      (0.0 < t) && (t < 1.0) )
      code = '1';
    else if ( (0.0 > s) || (s > 1.0) ||
	      (0.0 > t) || (t > 1.0) )
      code = '0';
    
    p.x = a.x + s * ( b.x - a.x );
    p.y = a.y + s * ( b.y - a.y );
    
    return code;
  }
  
  public char ParallelInt( cPointi a,cPointi b,cPointi c,cPointi d,cPointd p, cPointd q )
  {
    if ( !a.Collinear( a, b, c) )
      return '0';

   if ( Between1( a, b, c ) && Between1( a, b, d ) ) {
      Assigndi( p, c );
      Assigndi( q, d );
      return 'e';
   }
   if ( Between1( c, d, a ) && Between1( c, d, b ) ) {
      Assigndi( p, a );
      Assigndi( q, b );
      return 'e';
   }
   if ( Between1( a, b, c ) && Between1( c, d, b ) ) {
      Assigndi( p, c );
      Assigndi( q, b );
      return 'e';
   }
   if ( Between1( a, b, c ) && Between1( c, d, a ) ) {
      Assigndi( p, c );
      Assigndi( q, a );
      return 'e';
   }
   if ( Between1( a, b, d ) && Between1( c, d, b ) ) {
      Assigndi( p, d );
      Assigndi( q, b );
      return 'e';
   }
   if ( Between1( a, b, d ) && Between1( c, d, a ) ) {
      Assigndi( p, d );
      Assigndi( q, a );
      return 'e';
   }
   return '0';
   /*    
    if ( Between1( a, b, c ) ) {
      Assigndi( p, c );
      return 'e';
    }
    if ( Between1( a, b, d ) ) {
      Assigndi( p, d );
      return 'e';
    }
    if ( Between1( c, d, a ) ) {
      Assigndi( p, a );
      return 'e';
    }
    if ( Between1( c, d, b ) ) {
      Assigndi( p, b );
      return 'e';
    }
    return '0';
   */
  }
  
  public void Assigndi( cPointd p, cPointi a )
  {
    p.x = a.x;
    p.y = a.y;
  }

  /*---------------------------------------------------------------------
    Returns TRUE iff point c lies on the closed segement ab.
    Assumes it is already known that abc are collinear.
    (This is the only difference with Between().)
    ---------------------------------------------------------------------*/
  public boolean Between1( cPointi a, cPointi b, cPointi c )
  {
    cPointi      ba, ca;
    
    /* If ab not vertical, check betweenness on x; else on y. */
    if ( a.x != b.x )
      return ((a.x <= c.x) && (c.x <= b.x)) ||
	((a.x >= c.x) && (c.x >= b.x));
    else
      return ((a.y <= c.y) && (c.y <= b.y)) ||
	((a.y >= c.y) && (c.y >= b.y));
  }

  public void PrintPoint() 
  {
    System.out.println ( " (" + x + "," + y + ")" );
  }
}







