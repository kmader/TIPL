package tipl.blocks;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.StackWindow;
import tipl.formats.TImg;
import tipl.ij.TImgToImageStack;
import tipl.tools.Resize;
import tipl.util.ArgumentDialog;
import tipl.util.ArgumentList;
import tipl.util.ArgumentParser;
import tipl.util.TIPLPluginIO;
import tipl.util.TImgTools;

/**
 * Block for Resizing an image, based on a number of different criteria
 * 
 * @author mader
 * 
 */
public class ResizeBlock extends BaseTIPLBlock {
	public final String prefix;
	protected final static String blockName = "Resize";
	public final IBlockImage[] inImages = new IBlockImage[] { new BlockImage(
			"input", "", "Input unfiltered image", true) };
	public final IBlockImage[] outImages = new IBlockImage[] { new BlockImage(
			"output", "cropped.tif", "Resized image", true) };
	TIPLPluginIO fs = new Resize();

	public ResizeBlock() {
		super(blockName);
		prefix = "";
	}

	public ResizeBlock(final String inPrefix) {
		super(blockName);
		prefix = inPrefix;
	}

	@Override
	protected IBlockImage[] bGetInputNames() {
		return inImages;
	}

	@Override
	protected IBlockImage[] bGetOutputNames() {
		return outImages;
	}

	@Override
	public boolean executeBlock() {
		final TImg inputAim = TImgTools.ReadTImg(getFileParameter("input"),
				true, true);
		fs.LoadImages(new TImg[] { inputAim });
		fs.execute();
		final TImg outputAim = fs.ExportImages(inputAim)[0];
		TImgTools.WriteTImg(outputAim, getFileParameter("output"), true);
		TImgTools.RemoveTImgFromCache(getFileParameter("input"));
		return true;
	}

	@Override
	protected String getDescription() {
		return "Generic Block for resizing an image";
	}

	@Override
	public String getPrefix() {
		return prefix;
	}

	public void resizeGUI(ArgumentParser p) {
		p.getOptionBoolean("showslice",
				"Show the slice and select the region using the ROI tool",
				new ArgumentList.ArgumentCallback() {

					@Override
					public Object valueSet(Object value) {
						final String cValue = value.toString();
						if (Boolean.parseBoolean(cValue))
							showSlicePreview();
						return value;
					}

				});
		final ArgumentDialog a = new ArgumentDialog(p, "Resize Block GUI",
				"Set the appropriate parameters for the Resize GUI");

		a.nbshow();

		// final TImg inputAim =
		// TImgTools.ReadTImg(getFileParameter("input"),true, true);
		// ImagePlus curImage=TImgToImagePlus.MakeImagePlus(inputAim);
		// ImageStack curStack=curImage.getImageStack();
	}

	@Override
	public ArgumentParser setParameterBlock(ArgumentParser p) {
		final ArgumentParser t = fs.setParameter(p, prefix);
		if (t.getOptionBoolean("gui", "Use a GUI to set the parameters"))
			resizeGUI(t);

		return t;
	}

	public void showSlicePreview() {
		final TImg inputAim = TImgTools.ReadTImg(getFileParameter("input"),
				true, true);

		final ImageStack curStack = TImgToImageStack.MakeImageStack(inputAim);
		final ImagePlus curImage = new ImagePlus(getFileParameter("input"),
				curStack);

		System.out.println(curImage.getTitle());
		final StackWindow curWindow = new StackWindow(curImage);
		curImage.draw();

		// final Roi newROI=new ij.gui.PolygonRoi(4,4,curImage);

		curWindow.addMouseListener(new MouseListener() {
			// Only worry about the clicks everything else is a waste of time
			@Override
			public void mouseClicked(MouseEvent arg0) {
				System.out.println("Mouse Clicked: x=" + arg0.getX() + ", y="
						+ arg0.getY());
				System.out.println("ROI:" + curImage.getRoi());
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

		});
		final Object lock = new Object();
		final Thread t = new Thread() {
			@Override
			public void run() {
				synchronized (lock) {
					while (curWindow.isShowing())
						try {
							lock.wait();
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
					System.out.println("Working now");
				}
			}
		};
		t.start();

		curWindow.addWindowListener(new WindowListener() {

			@Override
			public void windowActivated(WindowEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowClosed(WindowEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowClosing(WindowEvent arg0) {
				// TODO Auto-generated method stub

				synchronized (lock) {
					curWindow.hide();
					lock.notify();
				}

			}

			@Override
			public void windowDeactivated(WindowEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowDeiconified(WindowEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowIconified(WindowEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowOpened(WindowEvent arg0) {
				// TODO Auto-generated method stub

			}

		});

		try {
			t.join();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

	}

}