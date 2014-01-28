
package tipl.util;

import java.io.Serializable;
/**
 * An implementation of a float vector (x,y,z) used for storing positions and voxel sizes
 * @author mader
 *
 */
public class D3float implements Serializable {
	// called a float on VMS but for other systems double is probably more
	// reliable
	public double x = 0.0;
	public double y = 0.0;
	public double z = 0.0;

	public D3float() {
		setVals(0.0, 0.0, 0.0);
	}

	public D3float(final D3float xi) {
		setVals(xi.x, xi.y, xi.z);
	}

	public D3float(final double xi, final double yi, final double zi) {
		setVals(xi, yi, zi);
	}

	public double prod() {
		double out = x;
		out *= y;
		out *= z;
		return out;
	}

	public void setVals(final double xi, final double yi, final double zi) {
		x = xi;
		y = yi;
		z = zi;
	}

	@Override
	public String toString() {
		return "" + String.format("%.4f", x) + "," + String.format("%.4f", y)
				+ "," + String.format("%.4f", z) + "";
	}
}