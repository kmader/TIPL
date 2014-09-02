package tipl.tools;

import tipl.formats.TImgRO;
import tipl.util.D3int;
import tipl.util.TImgTools;

// Used as a replacement for the moment function as it allows much more control over data
// and communication with webservices (potentially?)
/**
 * Class for plugins with multiple input types, has abstract function for every
 * possible datatype
 */
abstract public class BaseTIPLPluginMult extends BaseTIPLPluginIO {
	/** First input aim */
	public volatile boolean[] inAimMask;
	/** First input aim */
	public volatile char[] inAimByte;
	/** First input aim */
	public volatile short[] inAimShort;
	/** First input aim */
	public volatile int[] inAimInt;
	/** First input aim */
	public volatile float[] inAimFloat;

	/**
	 * The type of the image (0=char, 1=short, 2=int, 3=float, 10=bool, -1 same
	 * as input)
	 */
	public int imageType;

	public BaseTIPLPluginMult() {
		isInitialized = false;
	}

	/**
	 * initializer function taking boolean (other castings just convert the
	 * array first) linear array and the dimensions
	 */
	@Deprecated
	public void ImportAim(final boolean[] inputmap, final D3int idim,
			final D3int ioffset) {
		aimLength = inputmap.length;

		imageType = 10;
		inAimMask = inputmap;

		InitLabels(idim, ioffset);
	}

	/** initializer function taking a float linear array and the dimensions */
	@Deprecated
	public void ImportAim(final float[] inputmap, final D3int idim,
			final D3int ioffset) {
		aimLength = inputmap.length;

		imageType = 3;
		inAimFloat = inputmap;

		InitLabels(idim, ioffset);
	}

	/** initializer function taking int linear array and the dimensions */
	@Deprecated
	public void ImportAim(final int[] inputmap, final D3int idim,
			final D3int ioffset) {
		aimLength = inputmap.length;

		imageType = 2;
		inAimInt = inputmap;

		InitLabels(idim, ioffset);
	}

	/** initializer function taking short linear array and the dimensions */
	@Deprecated
	public void ImportAim(final short[] inputmap, final D3int idim,
			final D3int ioffset) {
		aimLength = inputmap.length;
		imageType = 1;
		inAimShort = inputmap;
		InitLabels(idim, ioffset);
	}

	/** initializer function taking an aim-file */
	@Deprecated
	public void ImportAim(final TImgRO inAim) {
		ImportAim(inAim, inAim.getImageType());
	}

	/** initializer function taking an aim-file, specify type */
	@Deprecated
	public void ImportAim(final TImgRO inImg, final int inType) {
		final TImgRO.FullReadable inAim = TImgTools.makeTImgFullReadable(inImg);
		int cType = inType;
		if (inType < 0)
			cType = inImg.getImageType();
		imageType = cType;
		switch (cType) {
		case 10: // Boolean
			inAimMask = inAim.getBoolAim();
			aimLength = inAimMask.length;
			break;
		case 0: // Byte
			inAimByte = inAim.getByteAim();
			aimLength = inAimByte.length;
			break;
		case 1: // Short
			inAimShort = inAim.getShortAim();
			aimLength = inAimShort.length;
			break;
		case 2: // Int
			inAimInt = inAim.getIntAim();
			aimLength = inAimInt.length;
			break;
		case 3: // Float
			inAimFloat = inAim.getFloatAim();
			aimLength = inAimFloat.length;
			break;
		default:
			System.err.println("Input type not supported: "+cType);
			return;
		}
		InitLabels(inAim.getDim(), inAim.getOffset());
	}

	abstract protected void InitByte();

	abstract protected void InitFloat();

	abstract protected void InitInt();

	protected void InitLabels(final D3int idim, final D3int ioffset) {
		InitDims(idim, ioffset);
		isInitialized = true;
		switch (imageType) {
		case 10: // Boolean
			InitMask();
			break;
		case 0: // Byte
			InitByte();
			break;
		case 1: // Short
			InitShort();
			break;
		case 2: // Integer
			InitInt();
			break;
		case 3: // Float
			InitFloat();
			break;
		default:
			System.err.println("Input type not supported");
			isInitialized = false;
			return;
		}

	}

	abstract protected void InitMask();

	abstract protected void InitShort();

	@Override
	public void LoadImages(final TImgRO[] inImages) {
		// TODO Auto-generated method stub
		if (inImages.length < 1)
			throw new IllegalArgumentException(
					"Too few arguments for LoadImages in:" + getPluginName());
		final TImgRO inImg = inImages[0];
		ImportAim(inImg);
	}


	protected void runAll() {
		if (isInitialized) {
			switch (imageType) {
			case 10: // Boolean
				runMask();
				break;
			case 0: // Byte
				runByte();
				break;
			case 1: // Short
				runShort();
				break;
			case 2: // Integer
				runInt();
				break;
			case 3: // Float
				runFloat();
				break;
			default:
				System.err.println("Input type" + imageType + " not supported");
				return;
			}
			runCount++;
		} else {
			System.err.println("Plug-in not correctly initialized");
			return;
		}
	}

	abstract protected void runByte();

	abstract protected void runFloat();

	abstract protected void runInt();

	abstract protected void runMask();

	abstract protected void runShort();

}
