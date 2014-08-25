/**
 * 
 */
package tipl.ij;

import ij.ImageStack;
import ij.gui.HistogramWindow;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.image.ColorModel;

import tipl.formats.TImgRO;
import tipl.util.TImgTools;

/**
 * @author mader
 * 
 */
public class TImgToImageStack extends ImageStack {

	public static ImageStack MakeImageStack(final TImgRO inputImage) {
		return new TImgToImageStack(inputImage);
	}
	
	final protected TImgRO coreTImg;
	/** ImageJ.ImageStack colormodel */
	protected static ColorModel cm = null;
	HistogramWindow curHistWind = null;
	// Image Stack Implementation Code
	protected Object[] stack = null;

	protected boolean isVirtual = true;

	/**
	 * Take an input image and convert it to an imagestack (read-only, won't be
	 * forced but it won't sync)
	 * 
	 * @param inputImage
	 *            the image to use
	 */
	public TImgToImageStack(final TImgRO inputImage) {
		super(inputImage.getDim().x, inputImage.getDim().y,
				inputImage.getDim().z);
		coreTImg = inputImage;
	}

	@Override
	public void addSlice(final String sliceLabel, final ImageProcessor ip) {
		throw new IllegalArgumentException("cannot add new slices!");
	}

	@Override
	public void addSlice(final String sliceLabel, final ImageProcessor ip,
			final int n) {
		throw new IllegalArgumentException("cannot add new slices!");
	}

	@Override
	public void addSlice(final String sliceLabel, final Object pixels) {
		throw new IllegalArgumentException("cannot add new slices!");
	}

	@Override
	public void deleteSlice(final int n) {
		throw new IllegalArgumentException("cannot add new slices!");
	}

	@Override
	/** Returns null. */
	public Object[] getImageArray() {
		return null;
	}

	/** Returns the pixel array for the specified slice, were 1<=n<=nslices. */
	@Override
	public Object getPixels(final int n) {
		final ImageProcessor ip = getProcessor(n);
		if (ip != null)
			return ip.getPixels();
		else
			return null;
	}
	public boolean useAutoRanger=true;
	/**
	 * Returns an ImageProcessor for the specified slice, were 1<=n<=nslices.
	 * Returns null if the stack is empty.
	 */
	@Override
	public ImageProcessor getProcessor(final int n) {
		final int wid = getWidth();
		final int het = getHeight();
		final int imageType = coreTImg.getImageType();
		System.out.println("getProcessor: " + n + ", Type=" + imageType
				+ ", wid=" + wid + " , het=" + het);
		ImageProcessor ip = null;
		switch (TImgTools.imageTypeToClass(imageType)) {
		case TImgTools.IMAGECLASS_BINARY:
			char[] bpixels = (char[]) coreTImg.getPolyImage(n-1, TImgTools.IMAGETYPE_CHAR);
			final byte[] rbpixels = new byte[bpixels.length];
			for (int i = 0; i < bpixels.length; i++)
				rbpixels[i] = (byte) bpixels[i];
			ip = new ByteProcessor(wid, het, rbpixels, cm);
			ip.setSnapshotPixels(rbpixels);
			ip.setMinAndMax(Byte.MIN_VALUE, Byte.MAX_VALUE);
			if (useAutoRanger) (new TImgToImagePlus.autoRanger(ip, curHistWind, bpixels)).start();
			break;
		case TImgTools.IMAGECLASS_LABEL:
			short[] spixels = (short[]) coreTImg.getPolyImage(n-1, TImgTools.IMAGETYPE_SHORT);
			ip = new ShortProcessor(wid, het, spixels, cm);
			ip.setSnapshotPixels(spixels);
			ip.setMinAndMax(Short.MIN_VALUE, Short.MAX_VALUE);
			if (useAutoRanger) (new TImgToImagePlus.autoRanger(ip, curHistWind, spixels)).start();
			break;
		case TImgTools.IMAGECLASS_VALUE:
			float[] fpixels = (float[]) coreTImg.getPolyImage(n-1, TImgTools.IMAGETYPE_FLOAT);
			ip = new FloatProcessor(wid, het, fpixels, cm);
			ip.setSnapshotPixels(fpixels);
			ip.setMinAndMax(-Double.MAX_VALUE, Double.MAX_VALUE);
			if (useAutoRanger) (new TImgToImagePlus.autoRanger(ip, curHistWind, fpixels)).start();
			break;
		default: 
			throw new IllegalArgumentException("Image Class Other is not supported, image type ="+imageType+", "+TImgTools.getImageTypeName(imageType));

		}

		// if (isVirtual()) { // Set the processor for the stack
		// ip.setPixels(stack[n - 1]);
		// ip.setSnapshotPixels(stack[n - 1]);
		// }
		return ip;

	}

	@Override
	public int getSize() {
		return coreTImg.getDim().z;
	}

	@Override
	/** Returns the label of the Nth image. */
	public String getSliceLabel(final int n) {
		return "TIPL:Slice:" + n;
	}

	/** Always return true. */
	@Override
	public boolean isVirtual() {
		return isVirtual; // Virtual images suck
	}

	/**
	 * Assigns a pixel array to the specified slice, were 1<=n<=nslices.
	 */
	@Override
	public void setPixels(final Object pixels, final int n) {
		System.out
				.println("setPixels not defined but we can pretend like it is:"
						+ pixels + " @ " + n);
		// throw new
		// IllegalArgumentException("setPixels function has not yet been implemented");
	}

}
