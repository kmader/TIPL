package tipl.formats;

import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgTools;

/**
 * The basis for the other image processing tools this class serves as a
 * framework for storing, reading, and writing 3D image data. It is built using
 * the memory model from OpenVMS and the framework of ImageJ so it is in theory
 * 100% compatible with both approaches to image processing.
 * 
 * <pre> Oct 10, 2011 - Fixed support for reading in float arrays from float named images
 * 
 * <pre> Dec 18, 2011 - Recoded to work on normal systems (windows, osx, linux) using tiff stacks
 * 
 * <pre> Jan 25, 2012 - Restructure class as ImageStack from ImageJ and added preview, ImagePlus and image processing capabilities
 */
public interface TImg extends TImgRO, TImgRO.CanExport,
		TImgTools.ChangesDimensions {
	/**
	 * Is the image signed (should an offset be added / subtracted when the data
	 * is loaded to preserve the sign)
	 */
	@Override
	public boolean getSigned();

	/** The function the reader uses to initialize an AIM */
	public boolean InitializeImage(D3int dPos, D3int cDim, D3int dOffset,
			D3float elSize, int imageType);

	/** Is the data in good shape */
	@Override
	public boolean isGood();

	public void setCompression(boolean inData);

	/**
	 * The aim type of the image (0=char, 1=short, 2=int, 3=float, 10=bool, -1
	 * same as input)
	 */
	public void setImageType(int inData);

	/** Set the short scalar factor in the image data **/
	@Override
	public void setShortScaleFactor(float ssf);

	public void setSigned(boolean inData);

}
