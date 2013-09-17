package tipl.tools;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.D3int;
import tipl.util.TImgTools;

/**
 * Abstract class for performing voronoi like transformations from labeled
 * objects into masks
 */
public abstract class VoronoiTransform extends BaseTIPLPlugin {
	/** The labeled input objects */
	public int[] labels;
	/** The labeled volumes (output) */
	public int[] outlabels;
	/** The mask to be filled (if given) */
	public boolean[] mask;
	/** The output distancemap */
	public int[] distmap;

	public int maxlabel;
	/** Maximum distance (as float) which can be output */
	public int MAXDIST = 4000;
	/** Maximum distance (as int) which can be output */
	public int MAXDISTVAL = 32765;
	/** scaling factor between ints and floats */
	double distScalar = (MAXDIST + 0.0) / (MAXDISTVAL + 0.0);
	int gtmode = -1;
	/** limit maximum distance for distance map **/
	public double maxUsuableDistance = -1;

	/**
	 * preserveLabels copies the input to a new variable and thus leaves the
	 * input intact, this is safer but requires twice as much memory
	 */
	public static boolean preserveLabels = false;

	protected boolean alreadyCopied = false;

	/**
	 * Constructor used for custom initialization routines, please only experts
	 * meddle here
	 */
	protected VoronoiTransform(int iPromiseIknowWhatIamDoing) {
	}

	/**
	 * Create a voronoitransform using labeled regions and a solid filled
	 * background
	 */
	public VoronoiTransform(TImgRO labelAim) {
		ImportAim(labelAim);
	}

	/** Create a voronoitransform using labeled regions and a given mask */
	public VoronoiTransform(TImgRO labelAim, TImgRO maskAim) {
		ImportAim(labelAim, maskAim);
	}

	/** This function is not finished yet... */
	protected boolean checkGrowthTemplate(int idx1, int idx2, int gtmode) {
		if (gtmode == -1)
			return true;
		return true;
		/*
		 * switch(gtmode) { // Only the short case is coded now, lets get things
		 * working first case D1Tshort: return(sgtdat[idx1]==sgtdat[idx2]);
		 * break; case D1Tchar: return(cgtdat[idx1]==cgtdat[idx2]); break; case
		 * D1Tfloat: return(fgtdat[idx1]==fgtdat[idx2]); break; default:
		 * return(true); }
		 */
	}

	@Override
	public TImg ExportAim(TImgRO.CanExport templateAim) {
		return ExportVolumesAim(templateAim);
	}

	/** Code for exporting the voronoi distances to an Aim class */
	public TImg ExportDistanceAim(TImgRO.CanExport templateAim) {
		if (isInitialized) {
			if (runCount > 0) {
				final TImg outAim = templateAim.inheritedAim(distmap, dim,
						offset);
				outAim.setShortScaleFactor((float) distScalar);
				return outAim;
			} else {
				throw new IllegalArgumentException(
						"The plug-in : "
								+ getPluginName()
								+ ", has not yet been run, exported does not exactly make sense, original data will be sent.");
			}
		} else {
			throw new IllegalArgumentException(
					"The plug-in : "
							+ getPluginName()
							+ ", has not yet been initialized, exported does not make any sense");

		}
	}

	/**
	 * This implementation exports the volume image and then the distance image
	 */
	@Override
	public TImg[] ExportImages(TImgRO templateImage) {
		// TODO Auto-generated method stub
		final TImg cImg = TImgTools.WrapTImgRO(templateImage);
		return new TImg[] { ExportVolumesAim(cImg), ExportDistanceAim(cImg) };
	}

	/** Code for exporting the voronoi volumes to an Aim class */
	public TImg ExportVolumesAim(TImgRO.CanExport templateAim) {
		if (isInitialized) {
			if (runCount > 0) {
				return templateAim.inheritedAim(outlabels, dim, offset);
			} else {
				throw new IllegalArgumentException(
						"The plug-in : "
								+ getPluginName()
								+ ", has not yet been run, exported does not exactly make sense, original data will be sent.");

			}
		} else {
			throw new IllegalArgumentException(
					"The plug-in : "
							+ getPluginName()
							+ ", has not yet been initialized, exported does not make any sense");

		}
	}

	public void ImportAim(boolean[] inputmap, boolean[] inputmask, D3int idim,
			D3int ioffset) {
		aimLength = inputmap.length;
		labels = new int[aimLength];
		mask = inputmask;
		for (int i = 0; i < aimLength; i++) {
			if (inputmap[i])
				labels[i] = 1;
		}
		alreadyCopied = true;
		Init(idim, ioffset);
	}

	public void ImportAim(int[] inputmap, boolean[] inputmask, D3int idim,
			D3int ioffset) {
		aimLength = inputmap.length;
		labels = inputmap;
		mask = inputmask;
		Init(idim, ioffset);
	}

	public void ImportAim(int[] inputmap, D3int idim, D3int ioffset) {
		aimLength = inputmap.length;
		labels = inputmap; // Since the input map will be modified we should
							// have our own

		mask = new boolean[aimLength];

		for (int i = 0; i < aimLength; i++)
			mask[i] = true;
		Init(idim, ioffset);
	}

	public void ImportAim(short[] inputmap, boolean[] inputmask, D3int idim,
			D3int ioffset) {
		aimLength = inputmap.length;
		labels = new int[aimLength];
		mask = inputmask;
		for (int i = 0; i < aimLength; i++) {
			labels[i] = inputmap[i];
		}
		alreadyCopied = true;
		Init(idim, ioffset);
	}

	public void ImportAim(short[] inputmap, D3int idim, D3int ioffset) {
		aimLength = inputmap.length;
		labels = new int[aimLength];
		mask = new boolean[aimLength];
		for (int i = 0; i < aimLength; i++) {
			labels[i] = inputmap[i];
			mask[i] = true;
		}
		alreadyCopied = true;
		Init(idim, ioffset);
	}

	public void ImportAim(TImgRO labelImg) {
		ImportAim(TImgTools.makeTImgFullReadable(labelImg).getIntAim(),
				labelImg.getDim(), labelImg.getOffset());
	}

	public void ImportAim(TImgRO labelImg, TImgRO maskImg) {
		ImportAim(TImgTools.makeTImgFullReadable(labelImg).getIntAim(),
				TImgTools.makeTImgFullReadable(maskImg).getBoolAim(),
				labelImg.getDim(), labelImg.getOffset());
	}

	private void Init(D3int idim, D3int ioffset) {
		if (labels.length != mask.length) {
			System.out.println("SIZES DO NOT MATCH!!!!!!!!");
			return;
		}
		distmap = new int[aimLength];
		if ((preserveLabels) || (alreadyCopied)) {
			outlabels = labels;
		} else {
			outlabels = new int[aimLength];
			System.arraycopy(labels, 0, outlabels, 0, aimLength);
			alreadyCopied = true;
		}
		InitDims(idim, ioffset);
		isInitialized = true;

	}

	/**
	 * LoadImages assumes the first image is the label image and the second is
	 * the mask image (if present)
	 */
	@Override
	public void LoadImages(TImgRO[] inImages) {
		// TODO Auto-generated method stub
		if (inImages.length < 1)
			throw new IllegalArgumentException(
					"Too few arguments for LoadImages in:" + getPluginName());
		final TImgRO labelImg = inImages[0];
		if (inImages.length < 2)
			ImportAim(labelImg);
		final TImgRO maskImg = inImages[1];
		ImportAim(labelImg, maskImg);
	}

	/** Code for writing the voronoi distances to an Aim file */
	public void WriteDistanceAim(TImg templateAim, String outname) {
		final TImg newAim = ExportDistanceAim(templateAim);
		newAim.appendProcLog(procLog);

		newAim.WriteAim(outname, 1, (float) distScalar, false);
	}

	/** Code for writing the voronoi volumes to an Aim file */
	public void WriteVolumesAim(TImgRO.CanExport templateAim, String outname) {
		final TImg newAim = ExportVolumesAim(templateAim);
		newAim.appendProcLog(procLog);
		newAim.WriteAim(outname, 1, 1.0f, false);
	}
}
