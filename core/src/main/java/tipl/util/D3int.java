
package tipl.util;

import java.io.Serializable;
/**
 * An implementation of a integer vector (x,y,z) used for storing positions, dimensions, and offsets
 * @author mader
 *
 */
public class D3int implements ID3int {
	public int x;
	public int y;
	public int z;


	public D3int() {
		setVals(0, 0, 0);
	}

	public D3int(final D3int xi) {
		setVals(xi.x, xi.y, xi.z);
	}

    public D3int(final ID2int xyi, final int zi) {setVals(xyi.gx(),xyi.gy(),zi);}

	public D3int(final int xi) {
		setVals(xi, xi, xi);
	}

	public D3int(final int xi, final int yi, final int zi) {
		setVals(xi, yi, zi);
	}

    /**
     * getHeight is made to get the height from the dimensions D3int in a manner
     * compatible with ImageJ definitions
     **/
    public int getHeight() {
        return y;
    }

    /**
     * getSlices is made to get the slice count from the dimensions D3int in a
     * manner compatible with ImageJ definitions
     **/
    public int getSlices() {
        return z - 1;
    }

    /**
     * getWidth is made to get the width from the dimensions D3int in a manner
     * compatible with ImageJ definitions
     **/
    public int getWidth() {
        return x;
    }

    public double prod() {
        double out = x;
        out *= y;
        out *= z;
        return out;
    }

    public void setVals(final int xi, final int yi, final int zi) {
        x = xi;
        y = yi;
        z = zi;
    }

    @Override
    public String toString() {
        return "" + x + "," + y + "," + z + "";
    }

    @Override
    public boolean equals(Object rawOther) {
        if(rawOther==null) return false;
        if(!(rawOther instanceof D3int)) return false;
        D3int other=(D3int) rawOther;
        if((this.x==other.x) & (this.y==other.y) & (this.z==other.z)) return true;
        return false;
    }

    /**
     * Hash code is needed for hashmap and groupbykey to work properly
     */
    @Override
    public int hashCode() {
        return x*73+y*89+z;
    }

    @Override
    public int gz() {
        return z;
    }

    @Override
    public void setPos(final int x, final int y, final int z) {
        setPos(x,y);
        this.z=z;
    }

    @Override
    public int gx() {
        return x;
    }

    @Override
    public int gy() {
        return y;
    }

    @Override
    public void setPos(final int x, final int y) {
        this.x=x;
        this.y=y;
    }

    public static D3int zero = new D3int(0,0,0);
}
