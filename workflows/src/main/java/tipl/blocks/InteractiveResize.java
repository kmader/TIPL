/**
 *
 */
package tipl.blocks;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.StackWindow;
import tipl.formats.TImg;
import tipl.ij.TImgToImagePlus;
import tipl.ij.TImgToImageStack;
import tipl.util.*;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * An interactive resizing tool
 *
 * @author mader
 */
public class InteractiveResize implements ITIPLDialog.DialogInteraction {

    public static void main(String[] args) {
        InteractiveResize irObj = new InteractiveResize(args);
        irObj.execute();
    }

    final protected BaseTIPLBlock resizeBlock;
    final Future<ArgumentParser> fixedArgs;
    final ExecutorService dialogFetch;

    public InteractiveResize(String[] args) {
        ArgumentDialog.showDialogs = true;
        resizeBlock = new ResizeBlock("");
        dialogFetch = TIPLGlobal.getTaskExecutor();

        final ArgumentParser curArgs = resizeBlock.setParameter(TIPLGlobal.activeParser(args));
        final boolean runAsJob = curArgs
                .getOptionBoolean("sge:runasjob",
                        "Run this script as an SGE job (adds additional settings to this task");
        fixedArgs = dialogFetch.submit(new Callable<ArgumentParser>() {
            public ArgumentParser call() {
                return resizeGUI(curArgs);
            }
        });

    }

    public void execute() {
        final ArgumentParser finalArgs;
        try {
            finalArgs = resizeBlock.setParameter(fixedArgs.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }

        if (finalArgs.getOptionBoolean("sge:runasjob", "")) {
            finalArgs.getOptionString("-blockname", ResizeBlock.class.getSimpleName(),
                    "Name of resize block to use");
            ArgumentDialog.SGEJob(BaseTIPLBlock.class.getName(), finalArgs);
        } else {
            resizeBlock.execute();
        }

        dialogFetch.shutdown();
        TIPLGlobal.closeAllWindows();
        TIPLGlobal.getIJInstance().quit();
    }

    public ArgumentParser resizeGUI(final ArgumentParser p) {
        aDialog = ArgumentDialog.newDialog(p, "Resize Block GUI",
                "Set the appropriate parameters for the Resize GUI");
        showSlicePreview();
        return aDialog.scrapeDialog();
    }

    protected ArgumentDialog aDialog = null;

    protected slicePreviewROI spObj = null;
    protected StackWindow curWindow = null;

    /**
     * A wrapper to launch the proper function in a new thread
     */
    public void showSlicePreview() {
        spObj = new InteractiveResize.slicePreviewROI(resizeBlock.getFileParameter("input"), this);
        Thread sspThread = new Thread(spObj, "SlicePreviewThread");
        sspThread.start();
    }

    /**
     * A class for performing the preview and region of interest selection manually
     *
     * @author mader
     */
    protected static class slicePreviewROI implements Runnable {
        protected final TypedPath inPath;
        protected ImagePlus curImage;
        protected ITIPLDialog.DialogInteraction diagPipe;

        public slicePreviewROI(final TypedPath inPath, final ITIPLDialog.DialogInteraction
                diagPipe) {
            this.inPath = inPath;
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
            if (guiDim.x < 0) { // invalid roi
                guiPos = basePos;
                guiDim = baseDim;
            }
            TImgToImageStack.useAutoRanger = true;
            curImage = TImgToImagePlus.MakeImagePlus(inputAim);

            //curWindow = new StackWindow(curImage);

            curImage.setRoi(guiPos.x - basePos.x, guiPos.y - basePos.y, basePos.x + guiDim.x,
                    basePos.y + guiDim.y);

            curImage.show("Hai!");

            curImage.getCanvas().addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                }

                @Override
                public void mousePressed(MouseEvent e) {
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                }

                @Override
                public void mouseExited(MouseEvent e) {
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    Roi curRoi = curImage.getRoi();
                    if (curRoi != null) {

                        D3int newPos = new D3int(basePos.x + curImage.getRoi().getBounds().x,
                                basePos.y + curImage.getRoi().getBounds().y,
                                basePos.z);
                        D3int newDim = new D3int(curImage.getRoi().getBounds().width,
                                curImage.getRoi().getBounds().height,
                                baseDim.z);

                        diagPipe.setKey("pos", newPos.toString());
                        diagPipe.setKey("dim", newDim.toString());
                    }
                    if (TIPLGlobal.getDebug())
                        System.out.println(curImage.getRoi() + " is the last region of interest");

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
