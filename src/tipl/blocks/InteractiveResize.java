/**
 * 
 */
package tipl.blocks;

import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.gui.StackWindow;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import tipl.formats.TImg;
import tipl.ij.TImgToImagePlus;
import tipl.ij.TImgToImageStack;
import tipl.util.ArgumentDialog;
import tipl.util.ArgumentList;
import tipl.util.ArgumentParser;
import tipl.util.D3int;
import tipl.util.TIPLDialog;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;

/**
 * An interactive resizing tool
 * @author mader
 *
 */
public class InteractiveResize implements TIPLDialog.DialogInteraction {
	
	public static void main(String[] args) {
		InteractiveResize irObj = new InteractiveResize(args);
		irObj.execute();
	}
	
	final protected BaseTIPLBlock resizeBlock;
	final Future<ArgumentParser> fixedArgs;
	public InteractiveResize(String[] args) {
		ArgumentDialog.showDialogs=true;
		resizeBlock = new ResizeBlock("");
		ExecutorService dialogFetch = TIPLGlobal.getTaskExecutor();
		
		final ArgumentParser curArgs = resizeBlock.setParameter(TIPLGlobal.activeParser(args));
		fixedArgs = dialogFetch.submit(new Callable<ArgumentParser>() {
			public ArgumentParser call() {
				return resizeGUI(curArgs);
			}
		});
		
	}
	
	public void execute() {
		try {
			resizeBlock.setParameter(fixedArgs.get());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		resizeBlock.execute();
	}
	
	public ArgumentParser resizeGUI(final ArgumentParser p) {
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
		
		  aDialog = ArgumentDialog.newDialog(p, "Resize Block GUI",
				"Set the appropriate parameters for the Resize GUI");
		  
		  return aDialog.scrapeDialog().subArguments("showslice", true);
		  
		
	}
	
	
	protected ArgumentDialog aDialog = null;
	
	protected Thread sspThread = null;
	protected slicePreviewROI spObj= null; 
	protected StackWindow curWindow = null;
	/**
	 * A wrapper to launch the proper function in a new thread
	 */
	public void showSlicePreview() {
		
		if (sspThread==null) {
			spObj = new InteractiveResize.slicePreviewROI(resizeBlock.getFileParameter("input"),this);
			sspThread = new Thread(spObj,"SlicePreviewThread");
			sspThread.start();
		}
	}
	
	public void closeSlicePreview() {
		if(sspThread!=null) sspThread.interrupt();
		sspThread=null;
		if(curWindow!=null) curWindow.close();
		curWindow=null;
	}
	/**
	 * A class for performing the preview and region of interest selection manually
	 * @author mader
	 *
	 */
	protected static class slicePreviewROI implements Runnable {
		protected final String inPath;
		protected ImagePlus curImage;
		protected TIPLDialog.DialogInteraction diagPipe;
		public slicePreviewROI(final String inPath, final TIPLDialog.DialogInteraction diagPipe) {
			this.inPath=inPath;
			this.diagPipe = diagPipe;
		}

		@Override
		public void run() {
			final TImg inputAim = TImgTools.ReadTImg(inPath,
					true, true);
			final D3int basePos = inputAim.getPos();
			final D3int baseDim = inputAim.getDim();
			D3int guiPos = ArgumentList.d3iparse.valueOf(diagPipe.getKey("pos"));
			D3int guiDim = ArgumentList.d3iparse.valueOf(diagPipe.getKey("dim"));
			if (guiDim.x<0) { // invalid roi
				guiPos = basePos;
				guiDim = baseDim;
			}
			TImgToImageStack.useAutoRanger=true;
			curImage = TImgToImagePlus.MakeImagePlus(inputAim);

			
			//curWindow = new StackWindow(curImage);

			curImage.setRoi(guiPos.x-basePos.x, guiPos.y-basePos.y, basePos.x+guiDim.x,basePos.y+guiDim.y);
			
			curImage.show("Hai!");
			curImage.getCanvas().addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent e) {}
				@Override
				public void mousePressed(MouseEvent e) {}
				@Override
				public void mouseEntered(MouseEvent e) {}
				@Override
				public void mouseExited(MouseEvent e) {}

				@Override
				public void mouseReleased(MouseEvent e) {
					Roi curRoi = curImage.getRoi();
					if (curRoi != null) {
						
						D3int newPos = new D3int(basePos.x+curImage.getRoi().getBounds().x,
								                 basePos.y+curImage.getRoi().getBounds().y,
								                 basePos.z);
						D3int newDim = new D3int(curImage.getRoi().getBounds().width,
												 curImage.getRoi().getBounds().height,
												 baseDim.z);
						
						diagPipe.setKey("pos", newPos.toString());
						diagPipe.setKey("dim", newDim.toString());
					}
					if (TIPLGlobal.getDebug()) System.out.println(curImage.getRoi()+" is the last region of interest");
					
				}
				
			});
			
		}
		
		
	}
	/**
	 * Pass the dialog pipe commands directly through
	 */
	
	@Override
	public String getKey(String keyName) {
		return aDialog.getKey(keyName);
	}

	@Override
	public void setKey(String keyName, String newValue) {
		aDialog.setKey(keyName, newValue);
	}

}
