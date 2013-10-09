package tipl.tools;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.D3int;
import tipl.util.TImgTools;

/**
 * Abstract class for performing voronoi like transformations from labeled
 * objects into masks
 */
public abstract class VoronoiTransform extends BaseTIPLPluginIO {
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
	protected VoronoiTransform(final int iPromiseIknowWhatIamDoing) {
	}

	/**
	 * Create a voronoitransform using labeled regions and a solid filled
	 * background
	 */
	public VoronoiTransform(final TImgRO labelAim) {
		ImportAim(labelAim);
	}

	/** Create a voronoitransform using labeled regions and a given mask */
	public VoronoiTransform(final TImgRO labelAim, final TImgRO maskAim) {
		ImportAim(labelAim, maskAim);
	}

	/** This function is not finished yet... */
	protected boolean checkGrowthTemplate(final int idx1, final int idx2,
			final int gtmode) {
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
	public TImg ExportAim(final TImgRO.CanExport templateAim) {
		return ExportVolumesAim(templateAim);
	}

	/** Code for exporting the voronoi distances to an Aim class */
	public TImg ExportDistanceAim(final TImgRO.CanExport templateAim) {
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
	public TImg[] ExportImages(final TImgRO templateImage) {
		// TODO Auto-generated method stub
		final TImg cImg = TImgTools.WrapTImgRO(templateImage);
		return new TImg[] { ExportVolumesAim(cImg), ExportDistanceAim(cImg) };
	}

	/** Code for exporting the voronoi volumes to an Aim class */
	public TImg ExportVolumesAim(final TImgRO.CanExport templateAim) {
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

	public void ImportAim(final boolean[] inputmap, final boolean[] inputmask,
			final D3int idim, final D3int ioffset) {
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

	public void ImportAim(final int[] inputmap, final boolean[] inputmask,
			final D3int idim, final D3int ioffset) {
		aimLength = inputmap.length;
		labels = inputmap;
		mask = inputmask;
		Init(idim, ioffset);
	}

	public void ImportAim(final int[] inputmap, final D3int idim,
			final D3int ioffset) {
		aimLength = inputmap.length;
		labels = inputmap; // Since the input map will be modified we should
							// have our own

		mask = new boolean[aimLength];

		for (int i = 0; i < aimLength; i++)
			mask[i] = true;
		Init(idim, ioffset);
	}

	public void ImportAim(final short[] inputmap, final boolean[] inputmask,
			final D3int idim, final D3int ioffset) {
		aimLength = inputmap.length;
		labels = new int[aimLength];
		mask = inputmask;
		for (int i = 0; i < aimLength; i++) {
			labels[i] = inputmap[i];
		}
		alreadyCopied = true;
		Init(idim, ioffset);
	}

	public void ImportAim(final short[] inputmap, final D3int idim,
			final D3int ioffset) {
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

	public void ImportAim(final TImgRO labelImg) {
		ImportAim(TImgTools.makeTImgFullReadable(labelImg).getIntAim(),
				labelImg.getDim(), labelImg.getOffset());
	}

	public void ImportAim(final TImgRO labelImg, final TImgRO maskImg) {
		ImportAim(TImgTools.makeTImgFullReadable(labelImg).getIntAim(),
				TImgTools.makeTImgFullReadable(maskImg).getBoolAim(),
				labelImg.getDim(), labelImg.getOffset());
	}

	private void Init(final D3int idim, final D3int ioffset) {
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
	public void LoadImages(final TImgRO[] inImages) {
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
	public void WriteDistanceAim(final TImg templateAim, final String outname) {
		final TImg newAim = ExportDistanceAim(templateAim);
		newAim.appendProcLog(procLog);

		newAim.WriteAim(outname, 1, (float) distScalar, false);
	}

	/** Code for writing the voronoi volumes to an Aim file */
	public void WriteVolumesAim(final TImgRO.CanExport templateAim,
			final String outname) {
		final TImg newAim = ExportVolumesAim(templateAim);
		newAim.appendProcLog(procLog);
		newAim.WriteAim(outname, 1, 1.0f, false);
	}
}
