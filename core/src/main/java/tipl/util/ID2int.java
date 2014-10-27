package tipl.util;

import java.io.Serializable;

/**
 * Created by mader on 10/15/14.
 */
public interface ID2int extends Serializable {
    /**
     * Get the x position
     * @return
     */
    public int gx();

    /**
     * Get the y position
     * @return
     */
    public int gy();
    public void setPos(int x,int y);

}
