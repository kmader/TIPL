/**
 *
 */
package tipl.util;

import tipl.blocks.ITIPLBlock;
import tipl.util.ArgumentList.ArgumentCallback;
import tipl.util.ArgumentList.RangedArgument;
import tipl.util.TIPLDialog.GUIControl;

import java.awt.*;
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

    
    public static ArgumentDialog newDialog(final ArgumentList inList, final String title,
                          final String helpText) {
    	return new ArgumentDialog(inList,title,helpText);
    }
    
    protected ArgumentDialog(final ArgumentList inList, final String title,
                          final String helpText) {
        coreList = inList;
        g = new TIPLDialog(title);
        g.addMessage(helpText, null, Color.red);
        inList.processOptions(this);
        if(showDialogs) g.showDialog();

    }
    
    protected ArgumentDialog(final ArgumentList inList, final String title,
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
    	final private ArgumentList.Argument cArg;
    	final private GUIControl guiC;
    	public ArgumentBasedControl(final GUIControl wrapIt,final ArgumentList.Argument curArgument) {
    		this.guiC = wrapIt;
    		this.cArg = curArgument;
    	}
    	
		@Override
		public String getValueAsString() {
			return guiC.getValueAsString();
		}
		
		@Override
		public void setValueFromString(String newValue) {
			guiC.setValueFromString(newValue);
		} 

		@Override
		public void setValueCallback(ArgumentCallback iv) {
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
        }
        if (cValue instanceof Double) {
            return g.appendNumericField(fName, (Double) cValue,
                    3);
        } else if (cValue instanceof Integer) {
            return g.appendNumericField(fName, (Integer) cValue, 0);
        } else if (cValue instanceof Boolean) {
            final boolean cStat = (Boolean) cValue;
            final GUIControl cChecks = g.appendCheckbox(fName, cStat);
            cChecks.setValueCallback(cArgument.getCallback());
        } else if (cValue instanceof D3float) {
            final D3float cStat = (D3float) cValue;
            return addD3Control(cName, cStat, cHelp);
        }

        return g.appendStringField(fName, cArgument.getValueAsString());

    }

    public void nbshow() {
        g.NonBlockingShow();
    }

    @Override
    public void process(final ArgumentList.Argument cArgument) {
        final String cName = cArgument.getName();
        controls.put(cName, new ArgumentBasedControl(getControl(cArgument),cArgument));
    }
    public void waitOnDialog() {
    	while(g.isVisible() && showDialogs) {
    		try {
				Thread.currentThread().sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
    /**
     * Gets all the values and puts them into a command line string
     * @return
     */
    public String[] scrapeDialogAsString() {
        String curArgs = "";
        waitOnDialog();
        for (final String objName : controls.keySet())
            curArgs += " -" + objName + "="
                    + controls.get(objName).getValueAsString();
        final String[] outArgs = curArgs.split(" ");
        return Arrays.copyOfRange(outArgs, 1, outArgs.length);
    }
    /**
     * Gets all of the values and puts them into an argumentparser object
     * @return
     */
    public ArgumentParser scrapeDialog() {
    	ArgumentParser newAp = new ArgumentParser(coreList);
    	waitOnDialog();
    	for (final String objName : controls.keySet()) {
    		IArgumentBasedControl aControl = controls.get(objName);
    		// make a copy of the argument but insert the value from the textbox / control
    		newAp.putArg(objName, aControl.getArgument().cloneWithNewValue(aControl.getValueAsString()));
    	}
    	return newAp;
    }
    

    public void show() {
        g.showDialog();
    }

}
