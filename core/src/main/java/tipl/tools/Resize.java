package tipl.tools;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.*;

/**
 * A plugin to crop, resize, and remove edges from Aim files
 * 
 * @author Kevin Mader
 */
public class Resize extends BaseTIPLPluginMult {
	@TIPLPluginManager.PluginInfo(pluginType = "Resize",
			desc="Slice-based and full memory resize command",
			sliceBased=true)
    final public static class rsFactory implements TIPLPluginManager.TIPLPluginFactory {
		@Override
		public ITIPLPlugin get() {
			return new Resize();
		}
	};
	/** Output aim length */
	int itAimLength;

	/** Is the full dataset loaded or just slices */
	boolean fullLoaded = true;
	boolean makeToMask = false;
	boolean forceMask = false;
	/** Selected Region of Interest */
	private int itLowx;
	private int itLowy;
	private int itLowz;
	private int itUppx;
	private int itUppy;
	private int itUppz;
	/** First input aim */
	public boolean[] outAimMask;

	/** First input aim */
	public char[] outAimByte;
	/** First input aim */
	public short[] outAimShort;
	/** First input aim */
	public int[] outAimInt;
	/** First input aim */
	public float[] outAimFloat;
	/** Store the input aim-file, allows for slice by slice resizing */
	private TImgRO _inputAim;

	/**
	 * Pad values for boolean data, these values are used to fill in the blanks
	 * when the ouput size is larger in any dimension than the input size
	 */
	public boolean padOutMask = false;

	/**
	 * Pad values for char data, these values are used to fill in the blanks
	 * when the ouput size is larger in any dimension than the input size
	 */
	public char padOutByte = (char) 0;

	/**
	 * Pad values for short data, these values are used to fill in the blanks
	 * when the ouput size is larger in any dimension than the input size
	 */
	public short padOutShort = (short) 0;

	/**
	 * Pad values for integer data, these values are used to fill in the blanks
	 * when the ouput size is larger in any dimension than the input size
	 */
	public int padOutInt = 0;

	/**
	 * Pad values for floating point data, these values are used to fill in the
	 * blanks when the ouput size is larger in any dimension than the input size
	 */
	public float padOutFloat = 0.0f;

	/** input position */
	public D3int pos;
	/** output position */
	public D3int opos;
	/** output dimensions */
	public D3int odim;
	private int off = 0;

	/*
	 * internal variables to allow the set parameter functionality to work more
	 * cleanly
	 */
	protected boolean pFindEdge = false;
	protected D3int pOutDim = new D3int(-1, -1, -1);

	protected D3int pOutPos = new D3int(-1, -1, -1);
	protected boolean pIsMask = false;
	public static final String kVer = "130903_002";

	/**
	 * The command line executable version of the code The code that is run
	 * (without reading the arguments) is
	 * 
	 * <pre>
	 *      <li> System.out.println("Loading "+inputFile+" ...");
	 *      <p> Read the input file as an aim
	 *      <li> VirtualAim inputAim=TImgTools.ReadTImg(inputFile);
	 *      <p> Create a new instance of the resize plugin using the aim file
	 *      <li> Resize myResizer=new Resize(inputAim);
	 *      <li> System.out.println("Resizing"+inputFile+" ...");
	 *      <p> Use the >0 (default) criterion for removing the edges of the image
	 *      <li>myResizer.find_edges();
	 *      <p> Run the plugin and generate the output image
	 *      <li>myResizer.run();
	 *      <p> Save the output image into an aim file outputAim, use inputAim and its procedure log as a template
	 *      <li>VirtualAim outputAim=myResizer.ExportAim(inputAim);
	 *      <p> Write the outputAim file to the hard disk as outputFile as an 8bit (0) image
	 *      <li>outputAim.WriteAim(outputFile,0);
	 * </pre>
	 */
	public static void main(final String[] args) {

		System.out.println("Resize v" + kVer);
		System.out.println(" Resizes Aim files based on given criteria");
		System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
		ArgumentParser p = TIPLGlobal.activeParser(args);
		final TypedPath inputFile = p.getOptionPath("input", "",
				"Input masked image");
		final TypedPath matchFile = p.getOptionPath("mask", "",
				"Matching masked image");
		final ITIPLPluginIO RS = new Resize();
		final TypedPath outputFile = p.getOptionPath("output", "resized.tif",
				"Output resized image");
		p = RS.setParameter(p, "");
		if (p.hasOption("?")) {
			System.out.println(" IPL Demo Help");
			System.out
					.println(" Analyzes Labeled Gray values inside of Int Labeled Regions");
			System.out.println(" Arguments::");
			System.out.println(" ");
			System.out.println(p.getHelp());
			System.exit(0);
		}

		if ((inputFile.length() > 0)) { // Read in labels, if find edge is
										// selected or a mask is given
			System.out.println("Loading " + inputFile + " ...");
			final TImg inputAim = TImgTools.ReadTImg(inputFile);
			RS.LoadImages(new TImgRO[] { inputAim });
			System.out.println("Resizing: " + inputFile + " ...");
			if (matchFile.length() > 0) {
				final TImg maskAim = TImgTools.ReadTImg(matchFile);
				// RS.cutROI(maskAim);
				RS.execute("cutroitimg", maskAim);

			}

			RS.execute();
			final TImg outputAim = RS.ExportImages(inputAim)[0];
			TImgTools.WriteTImg(outputAim,outputFile);

		}

	}

	/**
	 * Empty constructor, I would really like to avoid these somehow
	 */
	public Resize() {
		// I am not very happy!!
	}

	@Deprecated
	public Resize(final boolean[] inputmap, final D3int idim,
			final D3int ioffset, final D3int ipos) {
		ImportAim(inputmap, idim, ioffset);
		pos = ipos;
	}

	@Deprecated
	public Resize(final float[] inputmap, final D3int idim,
			final D3int ioffset, final D3int ipos) {
		ImportAim(inputmap, idim, ioffset);
		pos = ipos;
	}

	@Deprecated
	public Resize(final int[] inputmap, final D3int idim, final D3int ioffset,
			final D3int ipos) {
		ImportAim(inputmap, idim, ioffset);
		pos = ipos;
	}

	@Deprecated
	public Resize(final short[] inputmap, final D3int idim,
			final D3int ioffset, final D3int ipos) {
		ImportAim(inputmap, idim, ioffset);
		pos = ipos;
	}

	/**
	 * Constructor function, for aim files which still reside on disk the
	 * program will load one slice at a time allowing the user to take ROIs from
	 * much bigger aims
	 */
	public Resize(final TImg inputAim, final boolean isMask) {
		forceMask = isMask;
		if (inputAim instanceof TImgRO.FullReadable)
			LoadedAim(TImgTools.makeTImgFullReadable(inputAim), isMask);
		else {
			int asType=-1;
			if (isMask)
				asType=TImgTools.IMAGETYPE_BOOL;
			
			LoadImages(new TImgRO[] { inputAim },asType);
		}
		pos = inputAim.getPos();
	}

	/**
	 * Constructor function, for aim files which still reside on disk the
	 * program will load one slice at a time allowing the user to take ROIs from
	 * much bigger aims
	 */
	public Resize(final TImgRO inAim) {
		LoadImages(new TImgRO[] { inAim });
	}

	private void CalcOutDim() {
		opos = new D3int(itLowx + pos.x, itLowy + pos.y, itLowz + pos.z);
		odim = new D3int(itUppx - itLowx, itUppy - itLowy, itUppz - itLowz);
		itAimLength = (int) odim.prod();
		if (itAimLength < 1)
			System.out.println("Invalid Region Selected... 0 voxels");

		System.out.println("New output: dim-" + odim + ", pos:" + opos + ", "
				+ String.format("%.2f", odim.prod() * 100 / dim.prod())
				+ "% VolRed, ImgT:" + imageType);
		procLog += "New output: dim-" + odim + ", pos:" + opos + ", "
				+ String.format("%.2f", odim.prod() * 100 / dim.prod())
				+ "% VolRed, ImgT:" + imageType + "\n";

	}

	public void cutROI(final D3int cpos) {
		itLowx = cpos.x - pos.x;
		itLowy = cpos.y - pos.y;
		itLowz = cpos.z - pos.z;

		itUppx = offset.x + dim.x - (cpos.x - pos.x);
		itUppy = offset.y + dim.y - (cpos.y - pos.y);
		itUppz = offset.z + dim.z - (cpos.z - pos.z);
		printSize();
	}

	/**
	 * Manually select the new object size
	 * 
	 * @param cpos
	 *            The starting position within the current aim
	 * @param cdim
	 *            The dimension to cut from this position
	 */
	public void cutROI(final D3int cpos, final D3int cdim) {

		itLowx = cpos.x - pos.x;
		itLowy = cpos.y - pos.y;
		itLowz = cpos.z - pos.z;

		itUppx = cpos.x - pos.x + cdim.x;
		itUppy = cpos.y - pos.y + cdim.y;
		itUppz = cpos.z - pos.z + cdim.z;
		printSize();
	}

	/**
	 * Specify the cut size so that it matches the given aim
	 * 
	 * @param boundingBoxAim
	 *            Given aim to match when cutting
	 */
	public void cutROI(final TImgRO boundingBoxAim) {
		cutROI(boundingBoxAim.getPos(), boundingBoxAim.getDim());
		printSize();
	}

	@Override
	public boolean execute() {
		if (pFindEdge)
			find_edges();

		if (pOutPos.x >= 0) {
			if (pOutDim.x >= 0) {
				cutROI(pOutPos, pOutDim);
			} else
				cutROI(pOutPos);
		} else {
			if (pOutDim.x >= 0) {
				cutROI(new D3int(0), pOutDim);
			}
		}

		if (fullLoaded)
			runAll();
		else
			runVirtualMode();
		return true;
	}

	@Override
	public boolean execute(final String command, final Object cObj) {
		if (command.equalsIgnoreCase("cutroitimg")) {
			if (cObj instanceof TImgRO) {
				cutROI((TImgRO) cObj);
				return true;
			}

		}
		return super.execute(command, cObj);
	}
	
	@Override
	public TImg[] ExportImages(final TImgRO templateImage) {
		final TImg cImg = TImgTools.WrapTImgRO(templateImage);
		return new TImg[] { CreateOutputImage(cImg) };
	}
	
	/**
	 * Exports the resized result based on a template aim
	 * 
	 * @param templateAim
	 *            input template aim file
	 */
	public TImg CreateOutputImage(final TImgRO.CanExport templateAim) {
		if (isInitialized) {
			if (runCount > 0) {
				TImg outVirtualAim;
				switch (imageType) {
				case 10: // Boolean
					outVirtualAim = templateAim.inheritedAim(outAimMask, odim,
							new D3int(0));
					break;
				case 0: // Byte
					outVirtualAim = templateAim.inheritedAim(outAimByte, odim,
							new D3int(0));
					break;
				case 1: // Short
					outVirtualAim = templateAim.inheritedAim(outAimShort, odim,
							new D3int(0));
					break;
				case 2: // Int
					outVirtualAim = templateAim.inheritedAim(outAimInt, odim,
							new D3int(0));
					break;
				case 3: // Float
					outVirtualAim = templateAim.inheritedAim(outAimFloat, odim,
							new D3int(0));
					break;
				default:
					System.err.println("Input type not supported" + imageType);
					return null;
				}
				outVirtualAim.setPos(opos);
				outVirtualAim.appendProcLog(procLog);
				return outVirtualAim;

			} else {
				System.err
						.println("The plug-in : "
								+ getPluginName()
								+ ", has not yet been run, exported does not exactly make sense, original data will be sent.");
				return null;
			}
		} else {
			System.err
					.println("The plug-in : "
							+ getPluginName()
							+ ", has not yet been initialized, exported does not make any sense");
			return null;

		}
	}

	/** Find the bounding box for voxels which are greater than zero */
	public void find_edges() {
		final BaseTIPLPluginIn.TIPLFilter curFilter = new BaseTIPLPluginIn.TIPLFilter() {
			@Override
			public boolean accept(final boolean a) {
				return a;
			}

			@Override
			public boolean accept(final float a) {
				return (a > 0);
			}

			@Override
			public boolean accept(final int a) {
				return (a > 0);
			}

			@Override
			public boolean accept(final int a, final int b) {
				return (a > 0) & (b > 0);
			}

			@Override
			public boolean accept(final int a, final int b, final int c) {
				return ((a > 0) & (b > 0) & (c > 0));
			}
		};
		find_edges(curFilter);
		printSize();
	}

	/**
	 * Check loaded checks to see if the aim is loaded protected void
	 * checkLoaded() { if (!fullLoaded) { System.out.println(
	 * "For given function the entire aim file must be loaded, loading ..."); if
	 * (makeToMask) ImportAim(_inputAim,10); else ImportAim(_inputAim);
	 * fullLoaded=true; _inputAim=null; } }
	 */
	/**
	 * Find the bounding box for voxels which meet the given criteria
	 * 
	 * @param curFilter
	 *            The input filter, an example is given below which simply takes
	 *            all pixels greater than 0 <li>TIPLPlugin.TIPLFilter
	 *            curFilter=new TIPLPlugin.TIPLFilter() { <li>public boolean
	 *            accept(boolean a) {return a;} <li>public boolean accept(int a)
	 *            {return (a>0);} // handles both short and int data types <li>
	 *            public boolean accept(float a) {return (a>0);} <li>public
	 *            boolean accept(int a,int b) {return (a>0) & (b>0);} <li>public
	 *            boolean accept(int a,int b, int c) {return ((a>0) & (b>0) &
	 *            (c>0)); } <li>;
	 */
	public void find_edges(final BaseTIPLPluginIn.TIPLFilter curFilter) {
		boolean firstPoint = true;
		double validVoxels = 0.0;

		// checkLoaded();
		if (fullLoaded) {
			for (int z = lowz; z < uppz; z++) {
				for (int y = lowy; y < uppy; y++) {
					off = (z * dim.y + y) * dim.x + lowx;
					for (int x = lowx; x < uppx; x++, off++) {
						boolean isValidVoxel = false;
						switch (imageType) {
						case TImgTools.IMAGETYPE_BOOL: // Boolean
							if (curFilter.accept(inAimMask[off]))
								isValidVoxel = true;
							break;
						case TImgTools.IMAGETYPE_CHAR: // Byte
							if (curFilter.accept((inAimByte[off])))
								isValidVoxel = true;
							break;
						case TImgTools.IMAGETYPE_SHORT: // Short
							if (curFilter.accept(inAimShort[off]))
								isValidVoxel = true;
							break;
						case TImgTools.IMAGETYPE_INT: // Int
							if (curFilter.accept(inAimInt[off]))
								isValidVoxel = true;
							break;
						case TImgTools.IMAGETYPE_FLOAT: // Float
							if (curFilter.accept(inAimFloat[off]))
								isValidVoxel = true;
							break;
						default:
							System.err.println("Input type not supported"
									+ imageType);
							return;
						}
						// String outString="";
						// outString+=", "+x+":"+y+":"+z+","+((int)
						// inAimByte[off])+","+isValidVoxel;
						// System.out.println(outString);

						if (isValidVoxel) {
							validVoxels++;
							if (firstPoint) {
								// Reset all parameters
								itLowx = x;
								itUppx = x + 1;

								itLowy = y;
								itUppy = y + 1;

								itLowz = z;
								itUppz = z + 1;

								firstPoint = false;
								System.out.println("First point " + itLowx
										+ ", " + itLowy + ", " + itLowz);
							} else {
								if (x < itLowx)
									itLowx = x;
								if (x >= itUppx)
									itUppx = x + 1;

								if (y < itLowy)
									itLowy = y;
								if (y >= itUppy)
									itUppy = y + 1;

								if (z < itLowz)
									itLowz = z;
								if (z >= itUppz)
									itUppz = z + 1;
							}
						}
					}
				}
			}
		} else {
			final int cImageType = imageType;
			final int inImageType = _inputAim.getImageType();
			final TImg.TImgFull fullInputAim = new TImg.TImgFull(_inputAim);
			for (int z = lowz; z < uppz; z++) {
				boolean[] aMask = null;
				char[] aByte = null;
				short[] aShort = null;
				int[] aInt = null;
				float[] aFloat = null;

				switch (inImageType) { // Only way to be sure the data is
										// actually the right type
				case 10: // Boolean
					aMask = fullInputAim.getBoolArray(z);
					break;
				case 0: // Byte
					aByte = fullInputAim.getByteArray(z);
					break;
				case 1: // Short
					aShort = fullInputAim.getShortArray(z);
					break;
				case 2: // Integer
					aInt = fullInputAim.getIntArray(z);
					break;
				case 3: // Float
					aFloat = fullInputAim.getFloatArray(z);
					break;
				}
				for (int y = lowy; y < uppy; y++) {
					off = (y) * dim.x + lowx;
					for (int x = lowx; x < uppx; x++, off++) {
						boolean isValidVoxel = false;

						switch (cImageType) {
						case 10: // Boolean
							if (curFilter.accept(aMask[off]))
								isValidVoxel = true;
							break;
						case 0: // Byte
							if (curFilter.accept((aByte[off])))
								isValidVoxel = true;
							break;
						case 1: // Short
							if (curFilter.accept(aShort[off]))
								isValidVoxel = true;
							break;
						case 2: // Boolean
							if (curFilter.accept(aInt[off]))
								isValidVoxel = true;
							break;
						case 3: // Float
							if (curFilter.accept(aFloat[off]))
								isValidVoxel = true;
							break;
						default:
							System.err.println("Input type not supported"
									+ imageType);
							return;
						}
						// String outString="";
						// outString+=", "+x+":"+y+":"+z+","+((int)
						// inAimByte[off])+","+isValidVoxel;
						// System.out.println(outString);

						if (isValidVoxel) {
							validVoxels++;
							if (firstPoint) {
								// Reset all parameters
								itLowx = x;
								itUppx = x + 1;

								itLowy = y;
								itUppy = y + 1;

								itLowz = z;
								itUppz = z + 1;

								firstPoint = false;
								System.out.println("First point " + itLowx
										+ ", " + itLowy + ", " + itLowz);
							} else {
								if (x < itLowx)
									itLowx = x;
								if (x >= itUppx)
									itUppx = x + 1;

								if (y < itLowy)
									itLowy = y;
								if (y >= itUppy)
									itUppy = y + 1;

								if (z < itLowz)
									itLowz = z;
								if (z >= itUppz)
									itUppz = z + 1;
							}
						}
					}
				}
			}
		}
		System.out
				.println("Valid Voxels: "
						+ String.format("%.2f", validVoxels / aimLength)
						+ ", "
						+ String.format(
								"%.2f",
								validVoxels
										/ ((itUppx - itLowx)
												* (itUppy - itLowy) * (itUppz - itLowz))));

	}

	/**
	 * Find the bounding box for voxels which are greater than the given
	 * threshValue
	 */
	public void find_edges(final double threshValue) {
		final BaseTIPLPluginIn.TIPLFilter curFilter = new BaseTIPLPluginIn.TIPLFilter() {
			@Override
			public boolean accept(final boolean a) {
				return a;
			}

			@Override
			public boolean accept(final float a) {
				return (a > (float) threshValue);
			}

			@Override
			public boolean accept(final int a) {
				return (a > (int) threshValue);
			}

			@Override
			public boolean accept(final int a, final int b) {
				return (a > (int) threshValue) & (b > (int) threshValue);
			}

			@Override
			public boolean accept(final int a, final int b, final int c) {
				return ((a > (int) threshValue) & (b > (int) threshValue) & (c > (int) threshValue));
			}
		};
		find_edges(curFilter);
		printSize();
	}

	/**
	 * Find the bounding box for voxels which are greater than the given
	 * threshValue
	 */
	public void find_edges(final int threshValue) {
		final BaseTIPLPluginIn.TIPLFilter curFilter = new BaseTIPLPluginIn.TIPLFilter() {
			@Override
			public boolean accept(final boolean a) {
				return a;
			}

			@Override
			public boolean accept(final float a) {
				return (a > threshValue);
			}

			@Override
			public boolean accept(final int a) {
				return (a > threshValue);
			}

			@Override
			public boolean accept(final int a, final int b) {
				return (a > threshValue) & (b > threshValue);
			}

			@Override
			public boolean accept(final int a, final int b, final int c) {
				return ((a > threshValue) & (b > threshValue) & (c > threshValue));
			}
		};
		find_edges(curFilter);
		printSize();
	}

	@Override
	public String getPluginName() {
		return "Resize";
	}

	// TIPLFilter validvox;
	@Override
	protected void InitByte() {
		InitMask();
	}

	@Override
	protected void InitFloat() {
		InitMask();
	}

	@Override
	protected void InitInt() {
		InitMask();
	}

	@Override
	protected void InitMask() {
		itLowx = lowx;
		itLowy = lowy;
		itLowz = lowz;
		itUppx = uppx;
		itUppy = uppy;
		itUppz = uppz;
		return;
	}

	@Override
	protected void InitShort() {
		InitMask();
	}

	protected void LoadedAim(final TImgRO.FullReadable inAim,
			final boolean isMask) {
		if (makeToMask)
			ImportAim(inAim, 10);
		else
			ImportAim(inAim);
		pos = inAim.getPos();
		InitLabels(inAim.getDim(), inAim.getDim());
	}
	@Override
	public void LoadImages(final TImgRO[] inImages) {
		LoadImages(inImages,-1);
	}
	/**
	 * To read in the images
	 * @param inImages
	 * @param asType to set the type of the images being opened
	 */
	
	public void LoadImages(final TImgRO[] inImages,final int asType) {
		if (inImages.length < 1)
			throw new IllegalArgumentException("Too few input images given!");
		final TImgRO inAim = inImages[0];
		_inputAim = inAim;
		if(asType>=0) {
			TImgTools.isValidType(asType);
			imageType=asType;
		} else {
			imageType = inAim.getImageType();
		}
		fullLoaded = false;
		makeToMask = false;
		InitLabels(inAim.getDim(), inAim.getOffset());
		pos = inAim.getPos();
		System.out.println("lowz : " + lowz + ", " + uppz + ", " + pos + ", "
				+ inAim.getDim() + ", " + inAim.getOffset());

	}

	protected void printSize() {
		final String nString = "New Range = <" + itLowx + ", " + itLowy + ", "
				+ itLowz + "> -- <" + itUppx + ", " + itUppy + ", " + itUppz
				+ ">";
		procLog += nString + "\n";
		System.out.println(nString);
	}

	@Override
	protected void runByte() {
		CalcOutDim();
		outAimByte = new char[itAimLength];

		for (int z = itLowz; z < itUppz; z++) {
			final boolean zvalid = ((z >= lowz) & (z < uppz));
			for (int y = itLowy; y < itUppy; y++) {
				final boolean yvalid = ((y >= lowy) & (y < uppy));
				final int iz = z - itLowz;
				final int iy = y - itLowy;
				off = (iz * odim.y + iy) * odim.x + 0;

				for (int x = itLowx; x < itUppx; x++, off++) {
					final boolean xvalid = ((x >= lowx) & (x < uppx));
					if ((xvalid) & (yvalid) & (zvalid)) {

						final int off2 = (z * dim.y + y) * dim.x + x;
						outAimByte[off] = inAimByte[off2];
					} else {
						outAimByte[off] = padOutByte;
					}
				}
			}
		}

	}

	@Override
	protected void runFloat() {
		CalcOutDim();
		outAimFloat = new float[itAimLength];

		for (int z = itLowz; z < itUppz; z++) {
			final boolean zvalid = ((z >= lowz) & (z < uppz));
			for (int y = itLowy; y < itUppy; y++) {
				final boolean yvalid = ((y >= lowy) & (y < uppy));
				final int iz = z - itLowz;
				final int iy = y - itLowy;
				off = (iz * odim.y + iy) * odim.x + 0;

				for (int x = itLowx; x < itUppx; x++, off++) {
					final boolean xvalid = ((x >= lowx) & (x < uppx));
					if ((xvalid) & (yvalid) & (zvalid)) {

						final int off2 = (z * dim.y + y) * dim.x + x;
						outAimFloat[off] = inAimFloat[off2];
					} else {
						outAimFloat[off] = padOutFloat;
					}
				}
			}
		}

	}

	@Override
	protected void runInt() {
		CalcOutDim();
		outAimInt = new int[itAimLength];

		for (int z = itLowz; z < itUppz; z++) {
			final boolean zvalid = ((z >= lowz) & (z < uppz));
			for (int y = itLowy; y < itUppy; y++) {
				final boolean yvalid = ((y >= lowy) & (y < uppy));
				final int iz = z - itLowz;
				final int iy = y - itLowy;
				off = (iz * odim.y + iy) * odim.x + 0;

				for (int x = itLowx; x < itUppx; x++, off++) {
					final boolean xvalid = ((x >= lowx) & (x < uppx));
					if ((xvalid) & (yvalid) & (zvalid)) {

						final int off2 = (z * dim.y + y) * dim.x + x;
						outAimInt[off] = inAimInt[off2];
					} else {
						outAimInt[off] = padOutInt;
					}
				}
			}
		}

	}

	@Override
	protected void runMask() {
		CalcOutDim();
		outAimMask = new boolean[itAimLength];

		for (int z = itLowz; z < itUppz; z++) {
			final boolean zvalid = ((z >= lowz) & (z < uppz));
			for (int y = itLowy; y < itUppy; y++) {
				final boolean yvalid = ((y >= lowy) & (y < uppy));
				final int iz = z - itLowz;
				final int iy = y - itLowy;
				off = (iz * odim.y + iy) * odim.x + 0;

				for (int x = itLowx; x < itUppx; x++, off++) {
					final boolean xvalid = ((x >= lowx) & (x < uppx));
					if ((xvalid) & (yvalid) & (zvalid)) {

						final int off2 = (z * dim.y + y) * dim.x + x;
						outAimMask[off] = inAimMask[off2];
					} else {
						outAimMask[off] = padOutMask;
					}
				}
			}
		}

	}

	@Override
	protected void runShort() {
		CalcOutDim();
		outAimShort = new short[itAimLength];

		for (int z = itLowz; z < itUppz; z++) {
			final boolean zvalid = ((z >= lowz) & (z < uppz));
			for (int y = itLowy; y < itUppy; y++) {
				final boolean yvalid = ((y >= lowy) & (y < uppy));
				final int iz = z - itLowz;
				final int iy = y - itLowy;
				off = (iz * odim.y + iy) * odim.x + 0;

				for (int x = itLowx; x < itUppx; x++, off++) {
					final boolean xvalid = ((x >= lowx) & (x < uppx));
					if ((xvalid) & (yvalid) & (zvalid)) {

						final int off2 = (z * dim.y + y) * dim.x + x;
						outAimShort[off] = inAimShort[off2];
					} else {
						outAimShort[off] = padOutShort;
					}
				}
			}
		}

	}

	protected void runVirtualMode() {
		if (isInitialized) {
			System.out
					.println("Performing Resize in Virtual Mode, Slice by Slice...");
			procLog += "Performing Resize in Virtual Mode, Slice by Slice...\n";
			CalcOutDim();

			final int inImageType;
			if (!forceMask)
				inImageType = _inputAim.getImageType();
			else
				inImageType = 10;

			// final int cImageType=imageType;
			switch (inImageType) {
			case 10: // Boolean
				outAimMask = new boolean[itAimLength];
				break;
			case 0: // Byte
				outAimByte = new char[itAimLength];
				break;
			case 1: // Short
				outAimShort = new short[itAimLength];
				break;
			case 2: // Integer
				outAimInt = new int[itAimLength];
				break;
			case 3: // Float
				outAimFloat = new float[itAimLength];
				break;
			default:
				System.err.println("Input type" + imageType + " not supported");
				return;
			}
			final TImg.TImgFull fullInputAim = new TImg.TImgFull(_inputAim);

			for (int z = itLowz; z < itUppz; z++) {
				final boolean zvalid = ((z >= lowz) & (z < uppz));
				boolean[] aMask = null;
				char[] aByte = null;
				short[] aShort = null;
				int[] aInt = null;
				float[] aFloat = null;
				if (zvalid) {

					switch (inImageType) { // Only way to be sure the data is
											// actually the right type
					case TImgTools.IMAGETYPE_BOOL: // Boolean
						aMask = fullInputAim.getBoolArray(z);
						break;
					case TImgTools.IMAGETYPE_CHAR: // Byte
						aByte = fullInputAim.getByteArray(z);
						break;
					case TImgTools.IMAGETYPE_SHORT: // Short
						aShort = fullInputAim.getShortArray(z);
						break;
					case TImgTools.IMAGETYPE_INT: // Integer
						aInt = fullInputAim.getIntArray(z);
						break;
					case TImgTools.IMAGETYPE_FLOAT: // Float
						aFloat = fullInputAim.getFloatArray(z);
						break;
					default: // No type?
						throw new IllegalArgumentException("Type is invalid!"+inImageType);
					}

				} else throw new IllegalArgumentException("Z position is not valid"+z+" should be ["+lowz+","+uppz+")");
				for (int y = itLowy; y < itUppy; y++) {
					final boolean yvalid = ((y >= lowy) & (y < uppy));
					final int iz = z - itLowz;
					final int iy = y - itLowy;
					off = (iz * odim.y + iy) * odim.x + 0;

					for (int x = itLowx; x < itUppx; x++, off++) {
						final boolean xvalid = ((x >= lowx) & (x < uppx));
						
						if ((xvalid) & (yvalid) & (zvalid)) {

							final int sOff2 = (y) * dim.x + x; // Offset in the
							switch (inImageType) { // Only way to be sure the
													// data is actually the
													// right type
							case TImgTools.IMAGETYPE_BOOL: // Boolean
								outAimMask[off] = aMask[sOff2];
								break;
							case TImgTools.IMAGETYPE_CHAR: // Byte
								outAimByte[off] = aByte[sOff2];
								break;
							case TImgTools.IMAGETYPE_SHORT: // Short
								outAimShort[off] = aShort[sOff2];
								break;
							case TImgTools.IMAGETYPE_INT: // Integer
								outAimInt[off] = aInt[sOff2];
								break;
							case TImgTools.IMAGETYPE_FLOAT: // Float
								outAimFloat[off] = aFloat[sOff2];
								break;
							}
						} else {
							switch (inImageType) { // Only way to be sure the
													// data is actually the
													// right type
							case TImgTools.IMAGETYPE_BOOL: // Boolean
								outAimMask[off] = padOutMask;
								break;
							case TImgTools.IMAGETYPE_CHAR: // Byte
								outAimByte[off] = padOutByte;
								break;
							case TImgTools.IMAGETYPE_SHORT: // Short
								outAimShort[off] = padOutShort;
								break;
							case TImgTools.IMAGETYPE_INT: // Integer
								outAimInt[off] = padOutInt;
								break;
							case TImgTools.IMAGETYPE_FLOAT: // Float
								outAimFloat[off] = padOutFloat;
								break;
							}

						}
					}
				}
			}

			runCount++;
		} else {
			System.err.println("Plug-in not correctly initialized");
			return;
		}

	}

	@Override
	public ArgumentParser setParameter(final ArgumentParser p,
			final String cPrefix) {
		pFindEdge = p.getOptionBoolean(cPrefix + "find_edges",
				"Find borders by scanning image");
		pOutDim = p.getOptionD3int(cPrefix + "dim", pOutDim,
				"Size / dimensions of output image");
		pOutPos = p.getOptionD3int(cPrefix + "pos", pOutPos,
				"Starting Position");
		pIsMask = p.getOptionBoolean(cPrefix + "ismask",
				"Is input image a mask");

		return p;
	}
}
