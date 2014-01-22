/**
 * An implementation of a integer vector (x,y,z) used for storing positions, dimensions, and offsets
 **/
package tipl.util;

import java.io.Serializable;

public class D3int implements Serializable {
	public int x;
	public int y;
	public int z;

	public D3int() {
		setVals(0, 0, 0);
	}

	public D3int(final D3int xi) {
		setVals(xi.x, xi.y, xi.z);
	}

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

}
