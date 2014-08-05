/*----------------------------------------------------------------------------
 * Class cPointd  -- point with double coordinates
 *
 * PrintPoint() -- prints point to the console;
 *
 *---------------------------------------------------------------------------*/
class cPointd {
  double x;
  double y;
       
  cPointd() 
  {
    x = y = 0;
  }

  cPointd(int x, int y) 
  {
    x = x;
    y = y;
  }

  public void PrintPoint() 
  {
    System.out.println ( " (" + x + "," + y + ")" );
  }
}
