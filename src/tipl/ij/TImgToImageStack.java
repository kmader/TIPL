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

/**
 * @author mader
 * 
 */
public class TImgToImageStack extends ImageStack {
	public static ImageStack MakeImageStack(final TImgRO inputImage) {
		return new TImgToImageStack(inputImage);
	}

	final protected TImgRO coreTImg;
	final protected TImgRO.TImgFull coreTFull;
	/** ImageJ.ImageStack colormodel */
	protected static ColorModel cm = null;
	// ImagePlus curImPlus = null;
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
		coreTFull = new TImgRO.TImgFull(inputImage);
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
		switch (imageType) {
		case 0:
		case 10:
			char[] bpixels = null;
			// if (!isLoaded)
			bpixels = coreTFull.getByteArray(n - 1);
			final byte[] rbpixels = new byte[bpixels.length];
			for (int i = 0; i < bpixels.length; i++)
				rbpixels[i] = (byte) bpixels[i];
			ip = new ByteProcessor(wid, het, rbpixels, cm);
			ip.setSnapshotPixels(rbpixels);
			ip.setMinAndMax(Byte.MIN_VALUE, Byte.MAX_VALUE);
			(new TImgToImagePlus.autoRanger(ip, curHistWind, bpixels)).start();
			break;
		case 1:
		case 2:
			short[] spixels = null;
			// if (!isLoaded)
			spixels = coreTFull.getShortArray(n - 1);
			ip = new ShortProcessor(wid, het, spixels, cm);
			ip.setSnapshotPixels(spixels);
			ip.setMinAndMax(Short.MIN_VALUE, Short.MAX_VALUE);
			(new TImgToImagePlus.autoRanger(ip, curHistWind, spixels)).start();
			break;

		case 3:
			float[] fpixels = null;
			// if (!isLoaded)
			fpixels = coreTFull.getFloatArray(n - 1);
			ip = new FloatProcessor(wid, het, fpixels, cm);
			ip.setSnapshotPixels(fpixels);
			ip.setMinAndMax(-Double.MAX_VALUE, Double.MAX_VALUE);
			(new TImgToImagePlus.autoRanger(ip, curHistWind, fpixels)).start();
			break;

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

	/**
	 * Convert the loaded image to a stack Warning loading an image as a stack
	 * requires twice as much memory due to the different methods used in Aim
	 * and ImageJ data models, and the lack of operator overloading in java :-(
	 * 
	 * public static void loadAimfromStack(ImageStack istack,TImg outImage) {
	 * System.out.println("Loading ImageJ stack as Aim"); Object[]
	 * stack=istack.getImageArray(); int width=istack.getWidth(); int
	 * height=istack.getHeight();
	 * 
	 * D3int dim=outImage.getDim(); boolean changedSize = false; if (dim.z !=
	 * stack.length) { dim.z = stack.length; changedSize = true; } if (dim.x !=
	 * width) { dim.x = width; changedSize = true; } if (dim.y != height) {
	 * dim.y = height; changedSize = true; } if (changedSize)
	 * System.out.println("Volume has changed size, adjusting:" + dim); final
	 * int imgVoxCnt = dim.x * dim.y; int imageType; // Figure out what is in
	 * the stack if (stack[0] instanceof char[]) imageType = 0; else if
	 * (stack[0] instanceof short[]) imageType = 1; else if (stack[0] instanceof
	 * int[]) imageType = 2; else if (stack[0] instanceof float[]) imageType =
	 * 3; else { System.out.println("Unreadable!!!"); return; }
	 * 
	 * // Erase old data aimMask = null; aimByte = null; aimShort = null; aimInt
	 * = null; aimFloat = null; System.gc();
	 * 
	 * // Pre allocate array and read in data switch (imageType) { case 10:
	 * aimMask = new boolean[imgVoxCnt * dim.z]; break; case 0: aimByte = new
	 * char[imgVoxCnt * dim.z]; break; case 1: aimShort = new short[imgVoxCnt *
	 * dim.z]; break; case 2: aimInt = new int[imgVoxCnt * dim.z]; break; case
	 * 3: aimFloat = new float[imgVoxCnt * dim.z]; break; default:
	 * System.out.println("Hats dir im gring gschisse? So s'typ hans ned " +
	 * imageType);
	 * 
	 * } System.out.println("Copying Slices..."); int cPos = 0; for (int i = 0;
	 * i < dim.z; i++) { switch (imageType) { case 10: final char[] bstack =
	 * (char[]) stack[i]; for (int j = 0; j < imgVoxCnt; j++) aimMask[cPos + j]
	 * = (bstack[j] > 0); break; case 0: System.arraycopy(stack[i], 0, aimByte,
	 * cPos, imgVoxCnt); break; case 1: System.arraycopy(stack[i], 0, aimShort,
	 * cPos, imgVoxCnt); break; case 2: final short[] sstack = (short[])
	 * stack[i]; for (int j = 0; j < imgVoxCnt; j++) aimInt[cPos + j] =
	 * (sstack[j]); break; case 3: System.arraycopy(stack[i], 0, aimFloat, cPos,
	 * imgVoxCnt); break; default: System.out
	 * .println("Hats dir im gring gschisse? So s'typ hans ned " + imageType); }
	 * cPos += imgVoxCnt; } }
	 */

}
