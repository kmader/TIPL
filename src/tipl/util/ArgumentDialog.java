/**
 *
 */
package tipl.util;

import tipl.blocks.ITIPLBlock;
import tipl.util.ArgumentList.RangedArgument;
import tipl.util.TIPLDialog.GUIControl;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * A class to render an argument list as a dialog (based on the GenericDialog of
 * ImageJ)
 *
 * @author mader
 */
public class ArgumentDialog implements ArgumentList.optionProcessor {

    final protected ArgumentList coreList;
    final protected TIPLDialog g;
    final private LinkedHashMap<String, GUIControl> controls = new LinkedHashMap<String, GUIControl>();

    public ArgumentDialog(final ArgumentList inList, final String title,
                          final String helpText) {
        coreList = inList;
        g = new TIPLDialog(title);
        g.addMessage(helpText, null, Color.red);
        inList.processOptions(this);
        // g.showDialog();

    }

    /**
     * Turn a TIPLBlock into a nice GUI
     *
     * @param blockToRun the intialized block
     * @return the block with setParameter run
     */
    public static ITIPLBlock GUIBlock(final ITIPLBlock blockToRun) {
        ArgumentParser args = TIPLGlobal.activeParser(new String[]{});
        args = blockToRun.setParameter(args);
        final ArgumentDialog guiArgs = new ArgumentDialog(args,
                blockToRun.toString(), blockToRun.getInfo().getDesc());

        args = TIPLGlobal.activeParser(guiArgs.scrapeDialog());
        System.out.println(args);
        blockToRun.setParameter(args);

        return blockToRun;
    }

    protected GUIControl addD3Control(final String cName, final D3float cStat,
                                      final String helpText) {
        addTextControl(helpText + ": " + cName + ".x", cStat.x, "help");
        addTextControl(cName + ".y", cStat.y, "");
        return addTextControl(cName + ".z", cStat.z, "");
    }

    protected GUIControl addTextControl(final String cName,
                                        final Object cValue, final String helpText) {
        final GUIControl f = g.appendStringField(cName, cValue.toString());
        return f;
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
        controls.put(cName, getControl(cArgument));
    }

    public String[] scrapeDialog() {
        String curArgs = "";
        for (final String objName : controls.keySet())
            curArgs += " -" + objName + "="
                    + controls.get(objName).getValueAsString();
        final String[] outArgs = curArgs.split(" ");
        return Arrays.copyOfRange(outArgs, 1, outArgs.length);
    }

    public void show() {
        g.showDialog();
    }

}
