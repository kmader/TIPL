package tipl.formats;

import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgTools;

/**
	TImg is the central read/writable class for image data used for moving around and exporting images
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



	/** Set the short scalar factor in the image data **/
	@Override
	public void setShortScaleFactor(float ssf);

	public void setSigned(boolean inData);

}
