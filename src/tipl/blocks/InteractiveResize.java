/**
 * 
 */
package tipl.blocks;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.StackWindow;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import tipl.formats.TImg;
import tipl.ij.TImgToImageStack;
import tipl.util.ArgumentDialog;
import tipl.util.ArgumentList;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;

/**
 * An interactive resizing tool
 * @author mader
 *
 */
public class InteractiveResize {
	
	public static void main(String[] args) {
		InteractiveResize irObj = new InteractiveResize(args);
	}
	
	protected volatile ArgumentParser curArgs;
	final protected BaseTIPLBlock resizeBlock;
	public InteractiveResize(String[] args) {
		resizeBlock = new ResizeBlock("");
		curArgs = TIPLGlobal.activeParser(args);
		
		curArgs = resizeBlock.setParameter(curArgs);
		new Thread(new Runnable() {
			public void run() {
				resizeGUI(curArgs);
			}
		},"ResizeDialog").start();
	}
	
	public void resizeGUI(final ArgumentParser p) {
		p.getOptionBoolean("showslice",false,
				"Show the slice and select the region using the ROI tool",
				new ArgumentList.ArgumentCallback() {
					@Override
					public Object valueSet(final Object value) {
						final String cValue = value.toString();
						// if it is false now, it will be true
						boolean nextState = !Boolean.parseBoolean(cValue);
						if (nextState) showSlicePreview();
						else closeSlicePreview();
						return value;
					}
				});
		
		final ArgumentDialog a = ArgumentDialog.newDialog(p, "Resize Block GUI",
				"Set the appropriate parameters for the Resize GUI");
			curArgs=a.scrapeDialog();
		
	}
	
	protected static Thread sspThread = null;
	protected static StackWindow curWindow = null;
	/**
	 * A wrapper to launch the proper function in a new thread
	 */
	public void showSlicePreview() {
		if (sspThread==null) {
			final InteractiveResize curBlock = this;
			sspThread = new Thread(new Runnable() {
				@Override
				public void run() {curBlock.showSlicePreviewFunction();}		
			},"SlicePreviewThread");
			sspThread.start();
		}
	}
	
	public void closeSlicePreview() {
		if(sspThread!=null) sspThread.interrupt();
		sspThread=null;
		if(curWindow!=null) curWindow.close();
		curWindow=null;
	}
	protected static class slicePreviewROI implements Runnable {
		protected final String inPath;
		public slicePreviewROI(final String inPath) {
			this.inPath=inPath;
		}
		@Override
		public void run() {
			final TImg inputAim = TImgTools.ReadTImg(inPath,
					true, true);

			final ImageStack curStack = TImgToImageStack.MakeImageStack(inputAim);
			final ImagePlus curImage = new ImagePlus(inPath,
					curStack);
			
			//curWindow = new StackWindow(curImage);
			D3int imSize = inputAim.getDim();
			curImage.setRoi(10, 10, imSize.x-10, imSize.y-10);
			
			curImage.show("Hai!");
			//final Roi newROI=new ij.gui.PolygonRoi(4,4,curImage);
			
		}
		
	}
	protected void showSlicePreviewFunction() {



	}

}
