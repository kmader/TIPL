package tipl.util;

/**
 * The interface for the D3int object
 * Created by mader on 10/15/14.
 */
public interface ID3int extends ID2int {
    /**
     * Get the z position
     * @return
     */
    public int gz();
    @Deprecated // these should be immutable
    public ID3int setPos(int x,int y, int z);
}
