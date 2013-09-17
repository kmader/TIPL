/**
 * An implementation of a float vector (x,y,z) used for storing positions and voxel sizes
 **/
package tipl.util;

public class D3float {
	// called a float on VMS but for other systems double is probably more
	// reliable
	public double x = 0.0;
	public double y = 0.0;
	public double z = 0.0;

	public D3float() {
		setVals(0.0, 0.0, 0.0);
	}

	public D3float(D3float xi) {
		setVals(xi.x, xi.y, xi.z);
	}

	public D3float(double xi, double yi, double zi) {
		setVals(xi, yi, zi);
	}

	public double prod() {
		double out = x;
		out *= y;
		out *= z;
		return out;
	}

	public void setVals(double xi, double yi, double zi) {
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