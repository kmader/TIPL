package tipl.settings;

import java.io.Serializable;

import tipl.tools.BaseTIPLPluginIn;
import tipl.tools.BaseTIPLPluginIn.filterKernel;
import tipl.settings.FilterSettings.filterGenerator;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.TImgTools;

/** 
 * A class for storing settings for all objects which fulfill the Filter plugin functionality
 * @author mader
 *
 */
public class FilterSettings implements Serializable {
	/** filter types
	0 - Nearest Neighbor, 1 - Gaussian, 2 - Gradient, 3 - Laplace, 4 - Median
	**/
	final public static int NEAREST_NEIGHBOR=0;
	final public static int GAUSSIAN=1;
	final public static int GRADIENT=2;
	final public static int LAPLACE=3;
	final public static int MEDIAN=4;
	
	public static interface HasFilterSettings extends Serializable {
		public FilterSettings getFilterSettings();
		public void setFilterSettings(FilterSettings inSettings);
	}

	/** How a filter generating function looks */
	public interface filterGenerator {
		public BaseTIPLPluginIn.filterKernel make();
	}
	public D3int upfactor = new D3int(1,1,1);
	public D3int downfactor = new D3int(2,2,2);
	/**
	 * Filter generating function (needed for threading to make multiple copies
	 * of the filter for each region)
	 */
	public filterGenerator scalingFilterGenerator = null;
	
	/**
	 * Set imagetype of output image (default = -1 is the same as the input type
	 */
	public int oimageType = -1;
	
	public ArgumentParser setParameter(final ArgumentParser p,
			final String prefix) {
		final int filterType = p
				.getOptionInt(prefix + "filter", 0,
						NEAREST_NEIGHBOR+" - Nearest Neighbor,"+
				GAUSSIAN+" - Gaussian, "+GRADIENT+" - Gradient, "+LAPLACE+" - Laplace, "+MEDIAN+" - Median");
		final double filterParameter = p
				.getOptionDouble(
						prefix + "filtersetting",
						-1.0,
						"For gaussian it is the Sigma Value, for Median it is the window size (values less than 0 are ignored)");
		oimageType = p
				.getOptionInt(prefix+"output", oimageType,
						"-1 is same as input, "+TImgTools.IMAGETYPE_HELP);
		upfactor = p.getOptionD3int(prefix + "upfactor", upfactor, "Upscale factor");
		downfactor = p.getOptionD3int(prefix + "downfactor",downfactor, "Downscale factor");

		switch (filterType) {
		case NEAREST_NEIGHBOR:
			break;
		case GAUSSIAN:
			if (filterParameter > 0)
				setGaussFilter(filterParameter);
			else
				setGaussFilter();
			break;
		case GRADIENT:
			setGradientFilter();
			break;
		case LAPLACE:
			setLaplaceFilter();
			break;
		case MEDIAN:
			if (filterParameter > 0)
				setMedianFilter((int) filterParameter);
			else
				setMedianFilter();
			break;
		default:
			throw new IllegalArgumentException("Filter type:"+filterType+" does not exist!");
		}
		
		
		return p;

	}
	/** Use a gaussian filter */
	@Deprecated
	private void setGaussFilter() {
		scalingFilterGenerator = new filterGenerator() {
			@Override
			public BaseTIPLPluginIn.filterKernel make() {
				return BaseTIPLPluginIn.gaussFilter(upfactor.x, upfactor.y, upfactor.z);
			}
		};

	}

	/** Use a gaussian filter */
	@Deprecated
	private void setGaussFilter(final double sigma) {

		scalingFilterGenerator = new filterGenerator() {
			@Override
			public BaseTIPLPluginIn.filterKernel make() {
				return BaseTIPLPluginIn.gaussFilter(sigma, sigma, sigma);
			}
		};

	}

	/** Use a gradient filter */
	@Deprecated
	private void setGradientFilter() {
		scalingFilterGenerator = new filterGenerator() {
			@Override
			public BaseTIPLPluginIn.filterKernel make() {
				return BaseTIPLPluginIn.gradientFilter();
			}
		};

	}

	/** Use a laplace filter */
	@Deprecated
	private void setLaplaceFilter() {
		scalingFilterGenerator = new filterGenerator() {
			@Override
			public BaseTIPLPluginIn.filterKernel make() {
				return BaseTIPLPluginIn.laplaceFilter();
			}
		};

	}

	@Deprecated
	private void setMedianFilter() {

		scalingFilterGenerator = new filterGenerator() {
			@Override
			public BaseTIPLPluginIn.filterKernel make() {
				return BaseTIPLPluginIn.medianFilter(upfactor.x, upfactor.y, upfactor.z);
			}
		};

	}

	/** Use a Median filter */
	@Deprecated
	private void setMedianFilter(final int size) {

		scalingFilterGenerator = new filterGenerator() {
			@Override
			public BaseTIPLPluginIn.filterKernel make() {
				return BaseTIPLPluginIn.medianFilter(size, size, size);
			}
		};

	}
	
}