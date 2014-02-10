
package tipl.util;

/**
 * D4int is an index with the addition of an offset which can be useful for addressing specific regions in an image
 * can also eventually be used to better sort 4d data
 * @author mader
 *
 */
public class D4int extends D3int {
	public int offset=0;

	public D4int() {
		super();
		offset=0;
	}

	public D4int(final D3int xi) {
		super(xi);
	}
	
	public D4int(final D4int xi) {
		super(xi);
		this.offset=xi.offset;
	}

	public D4int(final int xi) {
		super(xi);
	}

	public D4int(final int xi, final int yi, final int zi) {
		super(xi, yi, zi);
	}
	public D4int(final int xi, final int yi, final int zi,int ioffset) {
		super(xi, yi, zi);
		offset=ioffset;
	}


	public void setVals(final int xi, final int yi, final int zi,final int ioffset) {
		setVals(xi,yi,zi);
		offset=ioffset;
	}



}
