package tipl.tools;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.PlugInFilter;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.formats.VirtualAim;
import tipl.ij.ImageStackToTImg;

/**
 * IJPluginRunner is a framework for using ImageJ plugins inside the TIPL
 * environment
 * 
 * <p>
 * <p>
 * Example Program
 * <p>
 * Little test loop for imageJ plugins, do a gaussian blur on each slice and
 * then save it to a file and delete the output
 * <p>
 * Indicate that the data is not virtual (load all slices)
 * <li>ufiltAim.isVirtual=false;
 * <p>
 * Chose the imageJ plugin filter to run
 * <li>Object ijPluginToTest = new ij.plugin.filter.GaussianBlur();
 * <p>
 * //Object ijPluginToTest = new ij.plugin.filter.BackgroundSubtracter();
 * <p>
 * // Create IJPluginRunner
 * <li>IJPluginRunner testIJplugin = new
 * IJPluginRunner(ijPluginToTest,ufiltAim);
 * <p>
 * // Run the plugin
 * <li>testIJplugin.run();
 * <p>
 * // Move the output from the plugin to the ijfiltAim file
 * <li>VirtualAim ijfiltAim=testIJplugin.ExportAim(ufiltAim);
 * <p>
 * //Write the aim file to ijfiltName
 * <li>ijfiltAim.WriteAim(ijfiltName);
 * <p>
 * // Delete the plugins and data
 * <li>ijPluginToTest=null;
 * <li>testIJplugin=null;
 * <li>ijfiltAim=null;
 */
public class IJPluginRunner extends BaseTIPLPluginMult {
	/**
	 * Display an error message, telling the allowed image types
	 */
	static void wrongType(final int flags, final String cmd) {
		String s = "\"" + cmd + "\" requires an image of type:\n \n";
		if ((flags & PlugInFilter.DOES_8G) != 0)
			s += "    8-bit grayscale\n";
		if ((flags & PlugInFilter.DOES_8C) != 0)
			s += "    8-bit color\n";
		if ((flags & PlugInFilter.DOES_16) != 0)
			s += "    16-bit grayscale\n";
		if ((flags & PlugInFilter.DOES_32) != 0)
			s += "    32-bit (float) grayscale\n";
		if ((flags & PlugInFilter.DOES_RGB) != 0)
			s += "    RGB color\n";
		System.err.println(s);
	}

	// plugin parameters
	protected int flags;
	// protected VirtualAim inAim;
	protected ImagePlus ipImg;
	protected Object theFilter;
	protected String command;
	/**
	 * Should the stack be unloaded after it is inserted in the filter, lightens
	 * Aim files memory burden significantly but is very inefficient when
	 * running multiple filters on the same data set
	 */
	public boolean unloadStack = false;
	protected boolean doStack;

	public IJPluginRunner(final Object theFilter, final VirtualAim inAim) {
		SetupImage(inAim);
		SetupPlugin(theFilter, "", "");
	}

	/**
	 * Setup and initialize the plugin, this is usually where the dialog is
	 * shown if there is one, this can be skipped or automated usually through
	 * the command and arg arguments
	 * 
	 * @param theFilter
	 *            The filter itself, it must Implement PlugInFilter and ideally
	 *            ExtendedPlugInFilter
	 * @param command
	 *            IMAGEJ: Command argument sent to the filter (this can be
	 *            blank)
	 * @param arg
	 *            IMAGEJ: Arg argument sent to the filter (this can be blank)
	 */
	public IJPluginRunner(final Object theFilter, final VirtualAim inAim,
			final String command, final String arg) {
		SetupImage(inAim);
		SetupPlugin(theFilter, command, arg);

	}

	/**
	 * test whether an ImagePlus can be processed based on the flags specified
	 * and display an error message if not.
	 */
	private boolean checkImagePlus(final ImagePlus imp, final int flags,
			final String cmd) {
		final boolean imageRequired = (flags & PlugInFilter.NO_IMAGE_REQUIRED) == 0;
		if (imageRequired && imp == null) {
			IJ.noImage();
			return false;
		}
		if (imageRequired) {
			if (imp.getProcessor() == null) {
				wrongType(flags, cmd);
				return false;
			}
			final int type = imp.getType();
			switch (type) {
			case ImagePlus.GRAY8:
				if ((flags & PlugInFilter.DOES_8G) == 0) {
					wrongType(flags, cmd);
					return false;
				}
				break;
			case ImagePlus.COLOR_256:
				if ((flags & PlugInFilter.DOES_8C) == 0) {
					wrongType(flags, cmd);
					return false;
				}
				break;
			case ImagePlus.GRAY16:
				if ((flags & PlugInFilter.DOES_16) == 0) {
					wrongType(flags, cmd);
					return false;
				}
				break;
			case ImagePlus.GRAY32:
				if ((flags & PlugInFilter.DOES_32) == 0) {
					wrongType(flags, cmd);
					return false;
				}
				break;
			case ImagePlus.COLOR_RGB:
				if ((flags & PlugInFilter.DOES_RGB) == 0) {
					wrongType(flags, cmd);
					return false;
				}
				break;
			}
			if ((flags & PlugInFilter.ROI_REQUIRED) != 0
					&& imp.getRoi() == null) {
				System.err.println(cmd + ": This command requires a selection");
				return false;
			}
			if ((flags & PlugInFilter.STACK_REQUIRED) != 0
					&& imp.getStackSize() == 1) {
				System.err.println(cmd + ": This command requires a stack");
				return false;
			}
		} // if imageRequired
		return true;
	}

	/** Call the run method of the plugin and deal with the stack appropriately */
	@Override
	public boolean execute() {
		ipImg.startTiming();
		// Only important when imageJ is running ipImg.lock();
		if (doStack) {
			for (int i = 1; i <= ipImg.getStackSize(); i++) {
				System.out.println("Processing Stack " + i + " of "
						+ ipImg.getStackSize());
				((PlugInFilter) theFilter)
						.run(ipImg.getStack().getProcessor(i));
			}
		} else {
			((PlugInFilter) theFilter).run(ipImg.getProcessor());
		}
		final long sTime = ipImg.getStartTime();
		ipImg.startTiming();
		procLog += "IJPlugin:" + (theFilter) + " (" + command
				+ ") has been run in:"
				+ String.format("%.2f", (ipImg.getStartTime() - sTime) / 1000F)
				+ "s\n";

		if ((flags & PlugInFilter.FINAL_PROCESSING) != 0)
			((PlugInFilter) theFilter).setup("final", ipImg);

		runCount++;
		return true;
	}

	/** Export result as an Aim file */
	@Override
	public TImg[] ExportImages(final TImgRO templateAim) {
		final TImg outImage=ImageStackToTImg.FromImagePlus(ipImg);
		outImage.setPos(templateAim.getPos());
		outImage.setOffset(templateAim.getOffset());
		outImage.setElSize(templateAim.getElSize());
		outImage.appendProcLog(templateAim.getProcLog());
		outImage.appendProcLog(procLog);
		return new TImg[] {outImage};
	}

	@Override
	public String getPluginName() {
		return "IJPluginRunner";
	}

	@Override
	public void InitByte() {
		return;
	}

	@Override
	public void InitFloat() {
		return;
	}

	@Override
	public void InitInt() {
		return;
	}

	/** Won't be needing any of these functions */
	@Override
	public void InitMask() {
		return;
	}

	@Override
	public void InitShort() {
		return;
	}


	@Override
	public void runByte() {
		return;
	}

	@Override
	public void runFloat() {
		return;
	}

	@Override
	public void runInt() {
		return;
	}

	@Override
	public void runMask() {
		return;
	}

	@Override
	public void runShort() {
		return;
	}

	protected void SetupImage(final VirtualAim inAim) {
		ipImg = inAim.getImagePlus();
		if (unloadStack)
			inAim.unloadStack();
	}

	protected void SetupPlugin(final Object inFilter, final String command,
			final String arg) {
		if (theFilter instanceof String) {
			final String cString = ((String) theFilter).toUpperCase();

			if (cString == "GAUSSIAN") {
				theFilter = new GaussianBlur();
			} else if (cString == "BACKGROUNDSUBTACTER") {
				theFilter = new BackgroundSubtracter();
			} else {
				System.err
						.println(cString
								+ " is not a known filter and can thus not be used any further!!!!!");
				return;
			}

		} else {
			theFilter = inFilter;
		}
		this.command = command;

		flags = ((PlugInFilter) theFilter).setup(arg, ipImg); // S E T U P
		if ((flags & PlugInFilter.DONE) != 0)
			return;
		if (!checkImagePlus(ipImg, flags, command))
			return; // check whether the PlugInFilter can handle this image type
		if ((flags & PlugInFilter.NO_IMAGE_REQUIRED) != 0)
			ipImg = null; // if the plugin does not want an image, it should not
							// get one

		if (theFilter instanceof ExtendedPlugInFilter) { // calling showDialog
															// required?
			flags = ((ExtendedPlugInFilter) theFilter).showDialog(ipImg,
					command, null); // D I A L O G (may include preview)

		} // if ExtendedPlugInFilter
		if ((flags & PlugInFilter.DONE) != 0) {
			if (ipImg != null)
				ipImg.unlock();
			return;
		} else if (ipImg == null) {
			((PlugInFilter) theFilter).run(null); // not DONE, but
													// NO_IMAGE_REQUIRED
			return;
		}
		final int slices = ipImg.getStackSize();

		doStack = slices > 1 && (flags & PlugInFilter.DOES_STACKS) != 0;

	}

}
