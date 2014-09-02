package tipl.tools;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.TImgTools;
import tipl.util.TypedPath;

/**
 * Abstract class for performing voronoi like transformations from labeled
 * objects into masks
 */
public abstract class VoronoiTransform extends BaseTIPLPluginIO implements IVoronoiTransform {
	/** The labeled input objects */
	protected int[] labels;
	/** The labeled volumes (output) */
	protected int[] outlabels;
	/** The mask to be filled (if given) */
	protected boolean[] mask;
	/** The output distancemap */
	protected int[] distmap;

	protected int maxlabel;
	/** Maximum distance (as float) which can be output */
	protected int MAXDIST = 4000;
	/** Maximum distance (as int) which can be output */
	protected int MAXDISTVAL = 32765;
	/** scaling factor between ints and floats */
	protected double distScalar = (MAXDIST + 0.0) / (MAXDISTVAL + 0.0);
	protected int gtmode = -1;
	/** limit maximum distance for distance map **/
	protected double maxUsuableDistance = -1;

	/**
	 * preserveLabels copies the input to a new variable and thus leaves the
	 * input intact, this is safer but requires twice as much memory
	 */
	protected boolean preserveLabels = false;

	protected boolean alreadyCopied = false;
	
	protected boolean includeEdges=false;
	
	@Override
	public ArgumentParser setParameter(final ArgumentParser p,
			final String prefix) {
		final ArgumentParser args = super.setParameter(p, prefix);
		preserveLabels = args.getOptionBoolean(prefix + "preservelabels", preserveLabels,
				"Preserve the labels in old image");
		alreadyCopied = args.getOptionBoolean(prefix + "alreadycopied", alreadyCopied,
				"Has the image already been copied");
		includeEdges = args.getOptionBoolean(prefix + "includeedges", includeEdges,
				"Include the edges");
		maxUsuableDistance = args.getOptionDouble(prefix + "maxdistance", maxUsuableDistance,
				"The maximum distance to run the voronoi tesselation until");
		return args;
	}
	/**
	 * Constructor used for custom initialization routines, please only experts
	 * meddle here
	 */
	protected VoronoiTransform() {
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


	/* (non-Javadoc)
	 * @see tipl.tools.IVoronoiTransform#ExportDistanceAim(tipl.formats.TImgRO.CanExport)
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see tipl.tools.IVoronoiTransform#ExportVolumesAim(tipl.formats.TImgRO.CanExport)
	 */
	@Override
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
		customInitSteps();
	}
	/**
	 * the code to run at the end of the initialization
	 */
	abstract protected void customInitSteps();

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
		if (inImages.length < 2) {
			ImportAim(labelImg);
		} else {
			final TImgRO maskImg = inImages[1];
			ImportAim(labelImg, maskImg);
		}
		
	}

	/* (non-Javadoc)
	 * @see tipl.tools.IVoronoiTransform#WriteDistanceAim(tipl.formats.TImg, java.lang.String)
	 */
	@Override
	public void WriteDistanceAim(final TImgRO.CanExport templateAim, final TypedPath outname) {
		final TImg newAim = ExportDistanceAim(templateAim);
		newAim.appendProcLog(procLog);
		TImgTools.WriteTImg(newAim,outname, 1, (float) distScalar, false,false);
	}

	/* (non-Javadoc)
	 * @see tipl.tools.IVoronoiTransform#WriteVolumesAim(tipl.formats.TImgRO.CanExport, java.lang.String)
	 */
	@Override
	public void WriteVolumesAim(final TImgRO.CanExport templateAim,
			final TypedPath outname) {
		final TImg newAim = ExportVolumesAim(templateAim);
		newAim.appendProcLog(procLog);
		TImgTools.WriteTImg(newAim,outname, 1, 1.0f, false,false);
	}
}
