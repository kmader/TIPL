package tipl.ij.plugins;

import ij.gui.GUI;
import ij.plugin.frame.PlugInFrame;
import tipl.blocks.BaseBlockRunner;
import tipl.blocks.IBlockRunner;
import tipl.blocks.ITIPLBlock;
import tipl.util.ArgumentParser;
import tipl.util.SGEJob;
import tipl.util.TIPLGlobal;

import java.awt.*;

public class BlockRunnerFrame extends PlugInFrame {
    public final static String kVer = "140821_02";

    public BlockRunnerFrame() {
        super("Select Blocks");
        runBlockRunner(new String[]{"-blocknames=FilterBlock,FoamThresholdBlock,AnalyzePhase,ThicknessBlock", "-gui"});

    }

    protected String getBlocksToRun() {
        TextArea ta = new TextArea(15, 50);
        add(ta);
        ta.setText("Hello:" + TIPLGlobal.activeParser(new String[]{"hey"}));
        pack();
        GUI.center(this);
        show();
        return "";
    }

    protected void runBlockRunner(String[] args) {
        System.out.println("BaseBlockRunner v" + kVer);
        System.out.println("Runs a series of blocks by their name");
        System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");

        ArgumentParser p = TIPLGlobal.activeParser(args);
        final String blocknames = p.getOptionString("blocknames", "",
                "Class names of the blocks to run");
        final boolean simpleNames = p.getOptionBoolean("simplenames", false, "Use simple names (b1 instead of thresholdblock1");
        if (blocknames.length() > 0) {
            IBlockRunner cr = new BaseBlockRunner();
            int blockIndex = 1;
            for (String iblockname : blocknames.split(",")) {
                String blockname = iblockname;
                try {
                    Object junk = (ITIPLBlock) Class.forName(blockname).newInstance();
                } catch (Exception e) {
                    // try adding the right prefix
                    blockname = "tipl.blocks." + iblockname;
                }
                ITIPLBlock cBlock = null;
                try {
                    cBlock = (ITIPLBlock) Class.forName(blockname).newInstance();
                    String blockClassName = cBlock.getClass().getSimpleName();
                    if (simpleNames) {
                        cBlock.setPrefix(blockClassName.replaceAll("[^A-Z]", "") + "B" + blockIndex + ":");
                    } else {
                        cBlock.setPrefix(blockClassName + blockIndex + ":");
                    }
                    cr.add(cBlock);
                } catch (final ClassNotFoundException e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException("Block Class:" + blockname
                            + " was not found, does it exist?");
                } catch (final Exception e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException("Block Class:" + blockname
                            + " could not be created, sorry!");
                }
                blockIndex++;
            }

            p = cr.setParameter(p.subArguments("blocknames", true).subArguments("simplenames", true));

            // code to enable running as a job
            final boolean runAsJob = p
                    .getOptionBoolean("sge:runasjob",
                            "Run this script as an SGE job (adds additional settings to this task");
            SGEJob jobToRun = null;
            if (runAsJob)
                jobToRun = SGEJob.runAsJob(BaseBlockRunner.class.getName(), p.subArguments("gui", true),
                        "sge:");

            p.checkForInvalid();

            if (runAsJob) {
                jobToRun.submit();
            } else {
                cr.execute();
            }


        }
    }

}
