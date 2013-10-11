/**
 * 
 */
package tipl.ij;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.HistogramWindow;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

import java.awt.Image;

import tipl.formats.TImgRO;
import tipl.formats.VirtualAim;
import tipl.ij.volviewer.Volume_Viewer;

/**
 * @author mader
 * 
 */
public class TImgToImagePlus extends ImagePlus {

	/**
	 * Autoranger class is a thread which runs in the background and calculates
	 * means and std
	 */
	public static class autoRanger extends Thread {
		short[] spixels;
		float[] fpixels;
		char[] bpixels;
		ImageProcessor ip;
		float sum = 0;
		float ssum = 0;
		float cnt = 1;
		float minv = 0;
		float maxv = 0;
		int mode;
		HistogramWindow chw;

		public autoRanger(final ImageProcessor outIm,
				final HistogramWindow ichw, final char[] ipixels) {
			super("Charer");
			ip = outIm;
			bpixels = ipixels;
			chw = ichw;
			mode = 0;

		}

		public autoRanger(final ImageProcessor outIm,
				final HistogramWindow ichw, final float[] ipixels) {
			super("Floater");
			ip = outIm;
			fpixels = ipixels;
			chw = ichw;
			mode = 3;

		}

		public autoRanger(final ImageProcessor outIm,
				final HistogramWindow ichw, final short[] ipixels) {
			super("Shorter");
			ip = outIm;
			spixels = ipixels;
			chw = ichw;
			mode = 1;

		}

		@Override
		public void run() {
			switch (mode) {
			case 0:
				cnt = 0;
				sum = 0;
				ssum = 0;
				minv = bpixels[0];
				maxv = bpixels[0];
				for (int i = 0; i < bpixels.length; i++) {
					// if (bpixels[i]>0) {
					sum += bpixels[i];
					ssum += ((float) bpixels[i]) * bpixels[i];
					cnt++;
					if (bpixels[i] > maxv)
						maxv = bpixels[i];
					if (bpixels[i] < minv)
						minv = bpixels[i];
					// }
				}
				break;
			case 1:
				cnt = 0;
				sum = 0;
				ssum = 0;
				minv = spixels[0];
				maxv = spixels[0];
				for (int i = 0; i < spixels.length; i++) {
					// if (spixels[i]>0) {
					sum += spixels[i];
					ssum += ((float) spixels[i]) * spixels[i];
					cnt++;
					if (spixels[i] > maxv)
						maxv = spixels[i];
					if (spixels[i] < minv)
						minv = spixels[i];
					// }
				}
				break;
			case 3:
				cnt = 0;
				sum = 0;
				ssum = 0;
				minv = fpixels[0];
				maxv = fpixels[0];
				for (int i = 0; i < fpixels.length; i++) {
					sum += fpixels[i];
					ssum += fpixels[i] * fpixels[i];
					if (fpixels[i] > maxv)
						maxv = fpixels[i];
					if (fpixels[i] < minv)
						minv = fpixels[i];
					cnt++;
				}
				break;
			default:
				System.out.println("Not really sure what's up!" + mode + ", "
						+ ip);
			}
			final float mean = sum / cnt;
			final float std = (float) Math.sqrt(ssum / cnt - mean * mean);

			ip.setMinAndMax(mean - std, mean + std);
			// new HistogramWindow("Histogram of "+ip.getShortTitle(), ip, 200,
			// mean-std, mean+std, iyMax);
			final String mytitle = "AR:" + this;
			if (chw != null) {
				chw.showHistogram(new ImagePlus(mytitle, ip), 255,
						VirtualAim.max(minv, mean - std),
						VirtualAim.min(maxv, mean + std));
				chw.run();
			}
			System.out.println("AutoRanger:" + this + ", Finished:(" + (mean)
					+ " -> [" + minv + "," + (mean - std) + "," + (mean + std)
					+ "," + maxv + "])");

		}

	}

	/**
	 * factory function for making an imageplus from a TImg by first making an
	 * imagestack
	 * 
	 * @param curImg
	 * @return type TImgToImagePlus which has the additional rendering function integrating volume viewer
	 */
	public static TImgToImagePlus MakeImagePlus(final TImgRO curImg) {
		final ImageStack curImStack = new TImgToImageStack(curImg);
		// make use of this class
		final TImgToImagePlus curImPlus = new TImgToImagePlus(curImg.getSampleName(),
				curImStack);

		final Calibration cal = new Calibration();
		cal.pixelWidth = curImg.getElSize().x;
		cal.pixelHeight = curImg.getElSize().y;
		cal.pixelDepth = curImg.getElSize().z;
		cal.setUnit("mm");
		curImPlus.setCalibration(cal);
		return curImPlus;
	}
	// standard constructors
	/**
	 * @param title
	 * @param img
	 */
	public TImgToImagePlus(final String title, final Image img) {
		super(title, img);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param title
	 * @param ip
	 */
	public TImgToImagePlus(final String title, final ImageProcessor ip) {
		super(title, ip);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param title
	 * @param stack
	 */
	public TImgToImagePlus(final String title, final ImageStack stack) {
		super(title, stack);
		// TODO Auto-generated constructor stub
	}
	
	//fun functions
	public void render(String args) {
		Volume_Viewer cViewer = new Volume_Viewer();
		cViewer.tiplShowView("",this);
	}

}
