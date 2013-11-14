package tipl.tools;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.TImgTools;

/**
 * EasyContour creates masks by traces around the edges of an object in 2D based
 * on the Contour2D interface/classes
 */
public class EasyContour extends BaseTIPLPluginBW {
	/**
	 * circContour is a class which uses a best-fit ellipsoid as the boundary
	 * for an object.
	 */
	public static class circContour extends Contour2D {
		public double a, b;

		@Override
		protected void ireset() {
			a = 0.0;
		}

		/** Calcualte Contour */
		@Override
		protected void irun() {
			a = 0;
			for (int i = 0; i < points.size(); i++) {
				final Point cpt = points.get(i);
				final double cx = cpt.x - xc;
				final double cy = cpt.y - yc;
				final double crx = Math.pow(cx, 2);
				final double cry = Math.pow(cy, 2);
				if (crx > a)
					a = crx;
				if (cry > a)
					a = cry;
			}
			a = Math.sqrt(a);

		}

		@Override
		public boolean isInside(final int x, final int y) {
			if (points.size() < 1)
				return false;
			final double cx = x - xc;
			final double cy = y - yc;

			return (Math.pow(cx, 2) + Math.pow(cy, 2)) < Math.pow(a, 2);
		}

		@Override
		public String toString() {
			final String outString = "Circ" + super.toString()
					+ " and radius (" + String.format("%.2f", a) + ")";
			return outString;
		}
	}

	/**
	 * Contour2D is the abstract class for creating 2D contours. It itself
	 * contains no functionality and serves as a framework for custom contour
	 * codes
	 */
	public static abstract class Contour2D {
		protected double xc, yc, vacuumDist = 3.0;
		protected ArrayList<Point> points = new ArrayList<Point>();
		/** amount of blurring for many sided objects */
		public double wid = 0.5;
		/** smallest radius possible */
		public double minRadius = 10;
		/** maximum radius, is calculated */
		protected double maxRadius = -1;
		/** operate in inner or outer contour mode */
		public boolean innerContourMode = false;

		/** Add a given voxel to the contour */
		public void addVox(final int x, final int y) {
			points.add(new Point(x, y));
		}

		/** Add a given voxel to the contour */
		public void addVox(final Point t) {
			points.add(t);
		}

		public void calcMax() {
			for (int i = 0; i < points.size(); i++) {
				final Point cpt = points.get(i);
				final double cRad = Math.sqrt(Math.pow(cpt.x - xc, 2)
						+ Math.pow(cpt.y - yc, 2));
				if (cRad > maxRadius)
					maxRadius = cRad;
			}
		}

		/**
		 * Calculate the radius from the center to a point t conversely when the
		 * program is operating in inner contour mode, calculate
		 */
		protected double getRad(final Point t) {
			final double cRadius = Math.sqrt(Math.pow(t.x - xc, 2)
					+ Math.pow(t.y - yc, 2));
			if (innerContourMode)
				return (maxRadius + minRadius) - cRadius;
			return cRadius;
		}

		protected abstract void ireset();

		protected abstract void irun();

		/** Is a given voxel inside the contour */
		public abstract boolean isInside(int x, int y);

		/** Is a given voxel inside the contour */
		public boolean isInside(final Point t) {
			return isInside(t.x, t.y);
		}

		/** The distance to the nearest voxel in the points list in voxels */
		public double minDist(final int x, final int y) {
			if (points.size() > 0) {
				Point cPoint = points.get(0);
				double mDist = Math.pow(x - cPoint.x, 2)
						+ Math.pow(y - cPoint.y, 2);
				for (int i = 1; i < points.size(); i++) {
					cPoint = points.get(i);
					final double cDist = Math.pow(x - cPoint.x, 2)
							+ Math.pow(y - cPoint.y, 2);
					if (cDist < mDist)
						mDist = cDist;
				}
				return Math.sqrt(mDist);
			} else
				return -1.0;
		}

		/** does it need to have voxels added first **/
		public boolean needsVoxels() {
			return true;
		}

		/** Clear out the old voxels and parameters */
		public void reset() {
			points = null;
			points = new ArrayList<Point>();
			xc = 0.0;
			yc = 0.0;
			ireset();
		}

		/**
		 * Generic run code, first calculate center then run the individual
		 * programs code
		 */
		public void run() {
			// Find Center
			xc = 0.0;
			yc = 0.0;

			for (int i = 0; i < points.size(); i++) {
				final Point cpt = points.get(i);
				xc += cpt.x;
				yc += cpt.y;
			}
			xc /= points.size();
			yc /= points.size();
			if (innerContourMode)
				calcMax();
			irun();
		}

		/** Print out infor as a string */
		@Override
		public String toString() {
			return "(" + String.format("%.2f", xc) + ", "
					+ String.format("%.2f", yc) + "), Vacuum:"
					+ String.format("%.1f", vacuumDist);
		}

		/** Is the object inside of the minimum distance */
		public boolean withinVacuumDistance(final int x, final int y) {
			if (points.size() > 0) {
				for (int i = 0; i < points.size(); i++) {
					final Point cPoint = points.get(i);
					final double cDist = Math.pow(x - cPoint.x, 2)
							+ Math.pow(y - cPoint.y, 2);
					if (cDist < vacuumDist)
						return true;
				}
				return false;
			} else
				return false;
		}

	}

	/** How a contour generating function looks (needed for threading) */
	public static interface ContourGenerator {
		public Contour2D make();
	}

	/**
	 * ellipseContour is a class which uses a best-fit ellipsoid as the boundary
	 * for an object.
	 */
	public static class ellipseContour extends Contour2D {
		public double a, b, th;

		@Override
		protected void ireset() {

			a = 0.0;
			b = 0.0;
			th = 0.0;

		}

		/** Calcualte Contour */
		@Override
		protected void irun() {
			a = 0;
			th = 0;
			for (int i = 0; i < points.size(); i++) {
				final Point cpt = points.get(i);
				final double cr = getRad(cpt);
				if (cr > a) {
					a = cr;
					final double cx = cpt.x - xc;
					final double cy = cpt.y - yc;
					th = Math.atan2(cy, cx);
				}
			}
			a = Math.sqrt(a);
			b = 0;
			// Find secondary axis
			for (int i = 0; i < points.size(); i++) {
				final Point cpt = points.get(i);
				final double cx = cpt.x - xc;
				final double cy = cpt.y - yc;
				// Unrotate points
				final Point2D.Double t = rot(cx, cy, false);

				final double bx = Math.abs(t.y)
						/ Math.sqrt(1 - Math.pow(t.x / a, 2));

				if ((bx <= a) & (bx > b))
					b = bx;
			}

		}

		@Override
		public boolean isInside(final int x, final int y) {
			if (points.size() < 1)
				return false;
			final double cx = x - xc;
			final double cy = y - yc;
			final Point2D.Double t = rot(cx, cy, false);

			return (Math.pow(t.x / a, 2) + Math.pow(t.y / b, 2)) < 1;
		}

		private Point2D.Double rot(final double ix, final double iy,
				final boolean forwards) {
			double rx;
			double ry;
			if (forwards) {
				rx = ix * Math.cos(th) - iy * Math.sin(th);
				ry = ix * Math.sin(th) + iy * Math.cos(th);
			} else {
				rx = ix * Math.cos(th) + iy * Math.sin(th);
				ry = -ix * Math.sin(th) + iy * Math.cos(th);
			}
			return new Point2D.Double(rx, ry);
		}

		@Override
		public String toString() {
			return "ELL" + super.toString() + " and half-axes ("
					+ String.format("%.2f", a) + ", "
					+ String.format("%.2f", b) + ") @"
					+ String.format("%.2f", Math.toDegrees(th)) + " deg";
		}
	}

	public static class fixCircContour extends circContour {
		protected final double fx, fy, fa;

		public fixCircContour(final double ix, final double iy, final double ia) {
			fx = ix;
			fy = iy;
			fa = ia;
		}

		@Override
		public boolean isInside(final int x, final int y) {
			final double cx = x - fx;
			final double cy = y - fy;
			return (Math.pow(cx, 2) + Math.pow(cy, 2)) < Math.pow(fa, 2);

		}

		@Override
		public boolean needsVoxels() {
			return false;
		}

		@Override
		public String toString() {
			final String outString = "Fixed_Circ(" + fx + "," + fy
					+ ") and radius (" + String.format("%.2f", fa) + ")";
			return outString;
		}
	}

	/**
	 * polyContour is a class which uses a piecewise defined ellipsoid thing as
	 * the boundary for an object. The number of pieces are set by changing the
	 * number of points in the initializations. The smoothness of the resulting
	 * shape is adjusted by changing the wid parameter (default 0.5)
	 */
	public static class polyContour extends Contour2D {
		protected ArrayList<Double> edge;
		protected ArrayList<Integer> edgeCount;
		protected int cpts;
		public boolean interpolate = true;
		public boolean smooth = true;
		public Integer zero = new Integer(0);
		public Double zeroD = new Double(0);

		public polyContour(final int npts) {
			cpts = npts;
			edge = new ArrayList<Double>(cpts);
			edgeCount = new ArrayList<Integer>(cpts);
		}

		protected void bumpBinCount(final Point t) {
			final int rBin = getBin(t);
			edgeCount.set(rBin, new Integer(getBinCount(rBin) + 1));
		}

		protected double getAng(final double cBin) {
			return (cBin % cpts) / (cpts) * (2 * Math.PI);
		}

		protected double getAng(final int cBin) {
			return getAng((double) cBin);
		}

		protected int getBin(final Point t) {
			final int cBin = (int) Math.round(getBinD(t));
			if (cBin == cpts)
				return 0;
			return cBin;
		}

		protected int getBinCount(final int whichBin) {
			return edgeCount.get(whichBin).intValue();
		}

		protected double getBinD(final Point t) {
			final double cAng = Math.atan2(t.y - yc, t.x - xc) + Math.PI;
			final double cBin = cAng / (2 * Math.PI) * (cpts-1);
			return cBin;
		}

		protected double getBinVal(final Point t) {
			if (interpolate) {
				final double cBin = getBinD(t);
				double weights = 0.0;
				double cVal = 0.0;

				for (int j = 0; j < cpts; j++) {

					double jDist = Math.abs(j - cBin);
					if (jDist > ((cpts) / 2))
						jDist = (cpts) - jDist; // circular

					if (jDist < Math.ceil(2 * wid)) { // not every point
						final double jWeight = getBinCount(j)
								* Math.exp(-Math.pow(jDist / wid, 2));

						weights += jWeight;
						cVal += getVal(j) * jWeight;
					}
				}
				return cVal / weights;
			} else
				return getVal(getBin(t));
		}

		protected Point2D.Double getPoint(final int cBin) {
			final double cAng = getAng(cBin) - Math.PI;
			final double cx = Math.cos(cAng) * getVal(cBin) + xc;
			final double cy = Math.sin(cAng) * getVal(cBin) + yc;
			return new Point2D.Double(cx, cy);
		}

		protected double getVal(final int index) {
			return edge.get(index).doubleValue();
		}

		@Override
		public void ireset() {
			edge = null;
			edge = new ArrayList<Double>(cpts);
			edgeCount = null;
			edgeCount = new ArrayList<Integer>(cpts);
			for (int j = 0; j < cpts; j++) {
				edge.add(zeroD);
				edgeCount.add(zero);
			}
		}

		@Override
		public void irun() {
			final boolean ointerpolate = interpolate;
			interpolate = false;
			for (int i = 0; i < points.size(); i++) {
				final Point cpt = points.get(i);
				final double cRad = getRad(cpt);
				if (cRad > getBinVal(cpt))
					setBinVal(cpt, cRad);

			}

			if (ointerpolate) {
				for (int i = 0; i < points.size(); i++) {
					final Point cpt = points.get(i);
					final double cRad = getRad(cpt);
					if (Math.abs(cRad - getBinVal(cpt)) < 3.5)
						bumpBinCount(cpt);
				}
			}
			interpolate = ointerpolate;
		}

		@Override
		public boolean isInside(final int x, final int y) {
			if (points.size() < 1)
				return false;
			final Point cpt = new Point(x, y);
			final double cRad = getRad(cpt);
			if (cRad < getBinVal(cpt)) {
				return true;
			} else
				return false;
		}

		public void nojumps() {
			for (int j = 0; j < cpts; j++) {
				if (j == 0) {
					getVal(cpts - 1);
					getVal(1);
				}
				if (j == (cpts - 1)) {
					getVal(j - 1);
					getVal(0);
				}
				// edge.get(j)
			}
		}

		protected void resetBinCount(final Point t) {
			edgeCount.set(getBin(t), zero);
		}

		protected void setBinVal(final Point t, final double value) {
			edge.set(getBin(t), new Double(value));
		}

		@Override
		public String toString() {

			String outs = "Poly" + super.toString() + cpts + " edges [";
			if (edge.size() > 0)
				outs += String.format("%.2f", edge.get(0));
			for (int j = 1; j < edge.size(); j++)
				outs += ", " + String.format("%.2f", edge.get(j)) + ":"
						+ edgeCount.get(j);

			return outs + "]";
		}

	}

	/**
	 * polygonContour is a class which uses polygon as the boundary for an
	 * object. The number of sides is set by changing the number of points in
	 * the initialization.
	 */
	public static class polygonContour extends polyContour {
		public polygonContour(final int npts) {
			super(npts);
		}

		@Override
		public boolean isInside(final int x, final int y) {
			if (points.size() < 1)
				return false;
			final Point cpt = new Point(x, y);
			final double nBin = getBinD(cpt);
			getRad(cpt);
			final int sBin = (int) Math.floor(nBin);
			int fBin = (int) Math.ceil(nBin);
			if (fBin == cpts)
				fBin = 0;
			
			final Point2D.Double sPt = getPoint(sBin);
			final Point2D.Double fPt = getPoint(fBin);
			final double tx = fPt.x - sPt.x;
			final double ty = fPt.y - sPt.y;
			final double ax = x - sPt.x;
			final double ay = y - sPt.y;
			if (innerContourMode)
				return (ax * ty - ay * tx) > 0;
			else
				return (ax * ty - ay * tx) < 0;
			// return (cRad<((nBin-sBin)/(fBin-sBin)*(fRad-sRad)+sRad));
		}
	}

	/**
	 * The command line executable version of the code The code run in the main
	 * function looks like this
	 * 
	 * <pre>
	 *      <p> Read in the inputFile as an VirtualAim
	 *      <li>VirtualAim inputAim=TImgTools.ReadTImg(inputFile);
	 *      <p> Create a new Peel object with the input file, using float (3) type, and a (2*peelS+1) x (2*peelS+1) x (2*peelS+1) sized space ranging from -peelS to +peelS in x,y, and z
	 *      <li>Peel cPeel=new Peel(inputAim,3,new D3int(peelS,peelS,peelS));
	 *      <li>System.out.println("Calculating Peel "+inputFile+" ...");
	 *      <p> Run the Peel analysis with rdfIter iterations
	 *      <li>cPeel.run(rdfIter);
	 *      <p> Save the result using the inputAim as a template into the new aim outputAim
	 *      VirtualAim outputAim=cPeel.ExportAim(inputAim);
	 *      <p> Write the output aim as a short to the dist using the probability scalar from Peel
	 *      <li>outputAim.WriteAim(outputFile,1,(float) cPeel.probScalar());
	 * </pre>
	 */
	public static void main(final String[] args) {
		final String kVer = "120105_001";
		System.out.println("Peel v" + kVer);
		System.out.println(" Contours (and peels) given images v" + kVer);
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		final ArgumentParser p = new ArgumentParser(args);
		final String maskFile = p.getOptionString("input", "",
				"Input mask image for contouring");
		final String inputFile = p.getOptionString("target", "", "Input image");
		final String outputMaskFile = p.getOptionString("contour", "",
				"contour image");
		final String outputFile = p.getOptionString("output", "",
				"Peeled image");
		final int ContourSteps = p.getOptionInt("contoursteps", 48,
				"Number of steps to use for the contouring of the mask");
		final double ContourBW = p
				.getOptionDouble(
						"contourwidth",
						0.8,
						"Amount to blur the edges of the contouring of the mask (normalized to number of steps)");

		final int peelS = p.getOptionInt("peelS", 1, "Peel Layers");

		if (p.hasOption("?")) {
			System.out.println(" EasyContour Demo Help");
			System.out.println(" Arguments::");
			System.out.println(" ");
			System.out.println(p.getHelp());
			System.exit(0);
		}
		if (maskFile.length() > 0) {
			final TImg maskAim = TImgTools.ReadTImg(maskFile);
			final EasyContour myContour = new EasyContour(maskAim);
			myContour.usePoly(ContourSteps, ContourBW);
			myContour.execute();
			final TImg contouredAim = myContour.ExportAim(maskAim);
			if (outputFile.length() > 0)
				TImgTools.WriteTImg(maskAim,outputMaskFile);
			if (inputFile.length() > 0) { // Read in labels
				System.out.println("Loading " + inputFile + " ...");
				final TImg inputAim = TImgTools.ReadTImg(inputFile);
				final Peel cPeel = new Peel(inputAim, contouredAim, new D3int(
						peelS));

				System.out.println("Calculating Peel " + inputFile + " ...");
				cPeel.run();
				final TImg outputAim = cPeel.ExportAim(inputAim);
				if (outputFile.length() > 0)
					TImgTools.WriteTImg(outputAim,outputFile);

			}
		}
	}

	// plugin parameters
	public double vacuumDist = 6.0;

	public ContourGenerator cContourGenerator = null;

	public boolean innerContourMode = false;
	public boolean bothContourModes = false;
	public boolean supportsThreading = true;
	/** number of slices to group together for contour calculations **/
	public int zGroup = 0;

	/** constructor function using TIPLPluginBW standard classes */
	public EasyContour(final TImgRO inputAim) {
		ImportAim(inputAim);
	}

	@Override
	public boolean execute() {
		if (Thread.currentThread() == launchThread) {
			Contour2D cContour;
			if (cContourGenerator == null)
				cContour = new ellipseContour();
			else
				cContour = cContourGenerator.make();
			System.out.println("EasyContour run " + uppz + ": " + cContour);
			procLog += "EasyContour:(" + uppz + "), SliceGrouping:" + zGroup
					+ ": " + cContour + "\n";
			runCount++;
		}
		
		runMulticore();
		return true;
	}

	@Override
	public String getPluginName() {
		return "EasyContour";
	}

	@Override
	public void processWork(final Object currentWork) {
		final int[] range = (int[]) currentWork;
		final int bSlice = range[0];
		final int tSlice = range[1];
		runContouring(bSlice, tSlice);
	}

	/**
	 * LoadImages assumes the first image is the label image and the second is
	 * the mask image (if present)
	 */

	@Override
	@Deprecated
	public void run() {
		runMulticore();
	}

	public void runContouring(final int bSlice, final int tSlice) {
		Contour2D cContour;
		if (cContourGenerator == null)
			cContour = new ellipseContour();
		else
			cContour = cContourGenerator.make();

		cContour.vacuumDist = vacuumDist;

		int off = 0;
		int middleZ = bSlice;
		while (middleZ < tSlice) {
			cContour.innerContourMode = false;
			if ((innerContourMode) & (!bothContourModes))
				cContour.innerContourMode = true;
			cContour.reset();
			final int startZ = max(bSlice, middleZ - zGroup);
			final int finalZ = min(tSlice - 1, middleZ + zGroup);
			if (cContour.needsVoxels()) {
				for (int z = startZ; z <= finalZ; z++) {
					for (int y = lowy; y < uppy; y++) {
						off = (z * dim.y + y) * dim.x + lowx;
						for (int x = lowx; x < uppx; x++, off++) {
							if (inAim[off])
								cContour.addVox(x, y);
						}
					}
				}
				cContour.run();
			}

			for (int z = startZ; z <= finalZ; z++) {
				if ((z + 1) % 100 == 0)
					System.out.println(z + " of " + uppz + ": " + cContour);

				for (int y = lowy; y < uppy; y++) {
					off = (z * dim.y + y) * dim.x + lowx;
					for (int x = lowx; x < uppx; x++, off++) {
						if (cContour.isInside(x, y))
							outAim[off] = true;
						else
							outAim[off] = false;
					}
				}
			}
			if (bothContourModes) {
				cContour.ireset();
				cContour.innerContourMode = true;

				cContour.calcMax();
				cContour.irun();
				for (int z = startZ; z <= finalZ; z++) {
					if ((z + 1) % 100 == 50)
						System.out.println(z + " of " + uppz + ": ic-"
								+ cContour);
					for (int y = lowy; y < uppy; y++) {
						off = (z * dim.y + y) * dim.x + lowx;
						for (int x = lowx; x < uppx; x++, off++) {
							if (outAim[off]) {
								if (!cContour.isInside(x, y))
									outAim[off] = false;
							}
						}
					}
				}

			}
			middleZ = max(finalZ, middleZ + 1);
			/**
			 * Vacuum up the remains for (int y=lowy; y < uppy; y++) { off =
			 * (z*dim.y + y)*dim.x + lowx; for (int x=lowx; x < uppx; x++,off++)
			 * { if (outAim[off]) { if (cContour.withinVacuumDistance(x,y))
			 * outAim[off]=false; }
			 * 
			 * } }
			 */

		}
	};

	/**
	 * Use the circular mask, with largest on axis radius (remove edges from
	 * images)
	 */
	public void useCirc() {
		cContourGenerator = new ContourGenerator() {
			@Override
			public Contour2D make() {
				return (new circContour());
			}
		};
	}

	/**
	 * Use the circular mask, with largest on axis radius (remove edges from
	 * images)
	 */
	public void useFixedCirc() {
		useFixedCirc(1.0);
	};

	/**
	 * Use the circular mask, with largest on axis radius (remove edges from
	 * images)
	 * 
	 * @param scaleRad
	 *            scale radius by mean side length (1.0= 100%)
	 */
	public void useFixedCirc(final double scaleRad) {
		final double cx1 = (uppx - lowx) / 2.0;
		final double cy1 = (uppy - lowy) / 2.0;
		final double mrad = scaleRad
				* (0.5 * (uppx - cx1) + 0.5 * (uppy - cy1));
		// final double crad=Math.sqrt(
		cContourGenerator = new ContourGenerator() {
			@Override
			public Contour2D make() {
				return (new fixCircContour(cx1, cy1, mrad));
			}
		};
	};

	/** Use the polyellipsoid mask intead of the ellipsoid */
	public void usePoly(final int sides) {
		final int fsides = sides;
		cContourGenerator = new ContourGenerator() {
			@Override
			public Contour2D make() {
				final Contour2D cContour = new polyContour(fsides);
				return cContour;
			}
		};
	};

	/**
	 * Use the polyellipsoid mask intead of the ellipsoid
	 * 
	 * @param sides
	 *            number of steps in piecewise function
	 * @param wid
	 *            smoothing distance (in units of steps)
	 */
	public void usePoly(final int sides, final double wid) {
		final int fsides = sides;
		final double fwid = wid;
		cContourGenerator = new ContourGenerator() {
			@Override
			public Contour2D make() {
				final Contour2D cContour = new polyContour(fsides);
				cContour.wid = fwid;
				return cContour;
			}
		};

	}

	/** Use the polygonal mask intead of the ellipsoid */
	public void usePolygon(final int sides) {
		final int fsides = sides;
		cContourGenerator = new ContourGenerator() {
			@Override
			public Contour2D make() {
				final Contour2D cContour = new polygonContour(fsides);
				return cContour;
			}
		};

	}

}
