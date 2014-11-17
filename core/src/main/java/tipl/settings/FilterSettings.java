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

    /**
     * Check the filter type and throw an error if it is not acceptable
     * @param i
     * @param throwException
     * @return
     */
    final public static boolean checkFilterType(int i, boolean throwException) {
        if(i>=0 && i<=4) return true;
        else if (throwException) throw new IllegalArgumentException("Filter type:"+i+" is unknown, please use one of the existing:"+filterHelpString);
        return false;
    }
	
	public static interface HasFilterSettings extends Serializable {
		public FilterSettings getFilterSettings();
		public void setFilterSettings(FilterSettings inSettings);
	}

	/** How a filter generating function looks */
	public interface filterGenerator extends Serializable {
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
	static private final String filterHelpString = NEAREST_NEIGHBOR+" - Nearest Neighbor,"+
            GAUSSIAN+" - Gaussian, "+GRADIENT+" - Gradient, "+LAPLACE+" - Laplace, "+MEDIAN+" - Median";

	public ArgumentParser setParameter(final ArgumentParser p,
			final String prefix) {
		final int filterType = p
				.getOptionInt(prefix + "filter", NEAREST_NEIGHBOR,
						filterHelpString);
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
		
		final D3int sigma = (filterParameter>0) ? new D3int((int) filterParameter) : upfactor;
		switch (filterType) {
		case NEAREST_NEIGHBOR:
			scalingFilterGenerator = new filterGenerator() {
				@Override
				public BaseTIPLPluginIn.filterKernel make() {
					return new BaseTIPLPluginIn.filterKernel() {
						double lastValue = -1;
						@Override
						public void addpt(double x1, double x2, double y1,
								double y2, double z1, double z2, double value) {
							lastValue = value;	
						}
						@Override
						public String filterName() {
							// TODO Auto-generated method stub
							return "Last Neighbor";
						}
						@Override
						public void reset() {}
						@Override
						public double value() {return lastValue;}
					};
				}
			};
			break;
		case GAUSSIAN:
				scalingFilterGenerator = new filterGenerator() {
				@Override
				public BaseTIPLPluginIn.filterKernel make() {
					return BaseTIPLPluginIn.gaussFilter(sigma.x, sigma.y, sigma.z);
				}
			};
			break;
		case GRADIENT:
			scalingFilterGenerator = new filterGenerator() {
				@Override
				public BaseTIPLPluginIn.filterKernel make() {
					return BaseTIPLPluginIn.gradientFilter();
				}
			};
			break;
		case LAPLACE:
			scalingFilterGenerator = new filterGenerator() {
				@Override
				public BaseTIPLPluginIn.filterKernel make() {
					return BaseTIPLPluginIn.laplaceFilter();
				}
			};
			break;
		case MEDIAN:
			scalingFilterGenerator = new filterGenerator() {
				@Override
				public BaseTIPLPluginIn.filterKernel make() {
					return BaseTIPLPluginIn.medianFilter(sigma.x, sigma.y, sigma.z);
				}
			};
			break;
		default:
			throw new IllegalArgumentException("Filter type:"+filterType+" does not exist!");
		}
		return p;

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