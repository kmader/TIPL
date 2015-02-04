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

    /**
     * when changing the position return the updated position (many times the same)
     * @param x
     * @param y
     * @return a position with the new x and y
     */
    @Deprecated
    public ID2int setPos(int x,int y);

}
