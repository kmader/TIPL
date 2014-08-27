/**
 *
 */
package tipl.util;

import tipl.blocks.BaseTIPLBlock;
import tipl.blocks.ITIPLBlock;
import tipl.util.ArgumentList.ArgumentCallback;
import tipl.util.ArgumentList.RangedArgument;
import tipl.util.TIPLDialog.GUIControl;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.util.Arrays;
import java.util.LinkedHashMap;

import org.apache.tools.ant.types.Commandline.Argument;

/**
 * A class to render an argument list as a dialog (based on the GenericDialog of
 * ImageJ)
 *
 * @author mader
 */
public class ArgumentDialog implements ArgumentList.optionProcessor,TIPLDialog.DialogInteraction {
	/**
	 * A Gui for the SGEJob console
	 * @param className
	 * @param taskArgs
	 */
	public static void SGEJob(final String className,final ArgumentParser taskArgs) {
		ArgumentParser sgeArgs = TIPLGlobal.activeParser("");
		// just for getting the arguments
		SGEJob.runAsJob("",sgeArgs,
				"sge:");
		ArgumentDialog aDialog = ArgumentDialog.newDialog(sgeArgs, "Sun Grid Engine Arguments",
				"Set the appropriate parameters Sun Grid Engine Submission");
		ArgumentParser sgeFinalArgs = aDialog.scrapeDialog();
		
		SGEJob jobToRun = SGEJob.runAsJob(className,taskArgs.appendToList(sgeFinalArgs.subArguments("sge:execute", true)),"sge:");
		jobToRun.submit();
	}
	
	public String getKey(String keyName) {
		return controls.get(keyName).getValueAsString();
	}
	public void setKey(String keyName, String newValue) {
		controls.get(keyName).setValueFromString(newValue);
	}
	
	public static boolean showDialogs = !TIPLGlobal.isHeadless();
    final protected ArgumentList coreList;
    final protected TIPLDialog g;
    final private LinkedHashMap<String, IArgumentBasedControl> controls = new LinkedHashMap<String, IArgumentBasedControl>();

    /**
     * Prepare the call-backs for the dialog correctly
     * @param inList
     * @param title
     * @param helpText
     * @return
     */
    public static ArgumentDialog newDialog(final ArgumentList inList, final String title,
                          final String helpText) {
    	final ArgumentDialog outDialog = new ArgumentDialog(inList,title,helpText);
    	outDialog.g.addDisposalTasks(new Runnable() {
			@Override
			public void run() { outDialog.shutdownFunctions();}
    	});
    	return outDialog;
    }
    
    
    
    private ArgumentDialog(final ArgumentList inList, final String title,
                          final String helpText) {
        coreList = inList;
        g = new TIPLDialog(title);
        g.addMessage(helpText, null, Color.red);
        inList.processOptions(this);
        if(showDialogs) g.showDialog();

    }
    
    private ArgumentDialog(final ArgumentList inList, final String title,
            final String helpText, final Frame parent) {
    	coreList = inList;
    	g = new TIPLDialog(title,parent);
    	g.addMessage(helpText, null, Color.red);
    	inList.processOptions(this);
    	if(showDialogs) g.showDialog();
    }
    
    public static <T extends ITIPLBlock> ArgumentParser GUIBlock(final T blockToRun) {
    	return GUIBlock(blockToRun,TIPLGlobal.activeParser(new String[]{}));
    }
    /**
     * Turn a TIPLBlock into a nice GUI (returning the exact same block)
     *
     * @param blockToRun the intialized block
     * @return the block with setParameter run
     */
    public static <T extends ITIPLBlock> ArgumentParser GUIBlock(final T blockToRun,final ArgumentParser args) {
        ArgumentParser p = blockToRun.setParameter(args);
        final ArgumentDialog guiArgs = ArgumentDialog.newDialog(args,
                blockToRun.toString(), blockToRun.getInfo().getDesc());
        
        p = guiArgs.scrapeDialog();
        System.out.println(p);
        return blockToRun.setParameter(p);
    }

    protected GUIControl addD3Control(final String cName, final D3float cStat,
                                      final String helpText) {
    	final GUIControl x = addTextControl(helpText + ": " + cName + ".x", cStat.x, "help");
        final GUIControl y = addTextControl(cName + ".y", cStat.y, "");
        final GUIControl z = addTextControl(cName + ".z", cStat.z, "");
    	return new GUIControl() {

			@Override
			public String getValueAsString() {
				return x.getValueAsString()+","+y.getValueAsString()+","+z.getValueAsString();
			}

			@Override
			public void setValueCallback(ArgumentCallback iv) {
				x.setValueCallback(iv);
				y.setValueCallback(iv);
				z.setValueCallback(iv);
			}

			@Override
			public void setValueFromString(String newValue) {
				D3float floatVal = ArgumentList.d3fparse.valueOf(newValue);
				x.setValueFromString(""+floatVal.x);
				y.setValueFromString(""+floatVal.y);
				z.setValueFromString(""+floatVal.z);
			}
        	
        };	
    }

    protected GUIControl addTextControl(final String cName,
                                        final Object cValue, final String helpText) {
        final GUIControl f = g.appendStringField(cName, cValue.toString());
        return f;
    }
    /**
     * Allow the controls to store the original argument
     * @author mader
     *
     */
    public static interface IArgumentBasedControl extends GUIControl {
    	/** 
    	 * 
    	 * @return the argument value stored in the control
    	 */
    	public ArgumentList.Argument getArgument();
    	
    }
    /** 
     * A basic model for the argument based controls
     * @author mader
     *
     */
    public static class ArgumentBasedControl implements IArgumentBasedControl {
    	final private String cName;
    	final private ArgumentList.Argument cArg;
    	final private GUIControl guiC;
    	public ArgumentBasedControl(final String inName,final GUIControl wrapIt,final ArgumentList.Argument curArgument) {
    		this.cName = inName;
    		this.guiC = wrapIt;
    		this.cArg = curArgument;
    		if (guiC==null) throw new IllegalArgumentException("Cannot intialize null object:"+cName+" gui control is not present"+cArg);
			
    	}
    	
		@Override
		public String getValueAsString() {
			if (guiC==null) throw new IllegalArgumentException(cName+" gui control is not present"+cArg);
			return guiC.getValueAsString();
		}
		
		@Override
		public void setValueFromString(String newValue) {
			if (guiC==null) throw new IllegalArgumentException(cName+" gui control is not present"+cArg);
			guiC.setValueFromString(newValue);
		} 

		@Override
		public void setValueCallback(ArgumentCallback iv) {
			if (guiC==null) throw new IllegalArgumentException(cName+" gui control is not present"+cArg);
			guiC.setValueCallback(iv);
		}
		@Override
		public ArgumentList.Argument getArgument() { return cArg;}

    	
    }
    protected GUIControl getControl(final ArgumentList.Argument cArgument) {
        final String cName = cArgument.getName();
        final String cHelp = cArgument.getHelpText();
        final String fName = cName + " [" + cHelp + "]:";
        final Object cValue = cArgument.getValue();
        if (cArgument instanceof RangedArgument<?>) {
            final RangedArgument rArg = (RangedArgument<?>) cArgument;
            if (cValue instanceof Double) {
                final double minValue = (Double) rArg.minVal;
                final double maxValue = (Double) rArg.maxVal;
                return g.appendSlider(fName, minValue, maxValue,
                        (maxValue + minValue) / 2); 
            }
            if (cValue instanceof Integer) {
                final int minValue = (Integer) rArg.minVal;
                final int maxValue = (Integer) rArg.maxVal;
                return g.appendSlider(fName, minValue, maxValue,
                        (maxValue + minValue) / 2); 
            }
        } else if (cValue instanceof Double) {
            return g.appendNumericField(fName, (Double) cValue,
                    3);
        } else if (cValue instanceof Integer) {
            return g.appendNumericField(fName, (Integer) cValue, 0);
        } else if (cValue instanceof Boolean) {
            final boolean cStat = (Boolean) cValue;
            final GUIControl cChecks = g.appendCheckbox(fName, cStat);
            cChecks.setValueCallback(cArgument.getCallback());
            return cChecks;
        } else if (cValue instanceof D3float) {
            final D3float cStat = (D3float) cValue;
            return addD3Control(cName, cStat, cHelp);
        } else {
        	return g.appendStringField(fName, cArgument.getValueAsString());
        }
        throw new IllegalArgumentException(cName+" control should not be null "+cArgument);

    }

    public void nbshow() {
        g.NonBlockingShow();
    }

    @Override
    public void process(final ArgumentList.Argument cArgument) {
        final String cName = cArgument.getName();
        GUIControl guiC = getControl(cArgument);
        controls.put(cName, new ArgumentBasedControl(cName,guiC,cArgument));
    }
    public synchronized void waitOnDialog() {
    	while(!properlyClosed) {
    		try {
				Thread.currentThread().sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("waiting for dialog:"+e);
			}
    	}
    	if(g.wasCanceled()) throw new IllegalArgumentException(this+" was cancelled cannot continue!");
    }
    private String[] isdString=null;
    /**
     * Gets all the values and puts them into a command line string
     * @return
     */
    private void intScrapeDialogAsString() {
        String curArgs = "";
        
        for (final String objName : controls.keySet())
            curArgs += " -" + objName + "="
                    + controls.get(objName).getValueAsString();
        final String[] outArgs = curArgs.split(" ");
        isdString=Arrays.copyOfRange(outArgs, 1, outArgs.length);
    }
    private ArgumentParser isdParser;
    /**
     * Gets all of the values and puts them into an argumentparser object
     * @return
     */
    private void intScrapeDialog() {
    	ArgumentParser newAp = new ArgumentParser(coreList);
    	for (final String objName : controls.keySet()) {
    		IArgumentBasedControl aControl = controls.get(objName);
    		// make a copy of the argument but insert the value from the textbox / control
    		newAp.putArg(objName, aControl.getArgument().cloneWithNewValue(aControl.getValueAsString()));
    	}
    	isdParser=newAp;
    }
    protected boolean properlyClosed=false;
    protected void shutdownFunctions() {
    	intScrapeDialog();
    	intScrapeDialogAsString();
    	properlyClosed=true;
    }
    
    
    public String[] scrapeDialogAsString() {
    	waitOnDialog();
    	if(isdString==null) throw new IllegalArgumentException(this+":scrapeDialogAsString is null, ");
    	return isdString;
    }
    public ArgumentParser scrapeDialog() { 
    	waitOnDialog();
    	if(isdParser==null) throw new IllegalArgumentException(this+":scrapeDialog is null, ");
    	return isdParser;
    }
  

    public void show() {
        g.showDialog();
    }

}
