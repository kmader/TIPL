/**
 * 
 */
package tipl.util;

import java.awt.Color;
import java.util.Arrays;
import java.util.LinkedHashMap;

import tipl.blocks.TIPLBlock;
import tipl.util.ArgumentList.RangedArgument;
import tipl.util.TIPLDialog.GUIControl;

/**
 * A class to render an argument list as a dialog (based on the GenericDialog of
 * ImageJ)
 * 
 * @author mader
 * 
 */
public class ArgumentDialog implements ArgumentList.optionProcessor {

	/**
	 * Turn a TIPLBlock into a nice GUI
	 * 
	 * @param blockToRun
	 *            the intialized block
	 * @return the block with setParameter run
	 */
	public static TIPLBlock GUIBlock(TIPLBlock blockToRun) {
		ArgumentParser args = new ArgumentParser(new String[] {});
		args = blockToRun.setParameter(args);
		final ArgumentDialog guiArgs = new ArgumentDialog(args,
				blockToRun.toString(), blockToRun.getInfo().getDesc());

		args = new ArgumentParser(guiArgs.scrapeDialog());
		System.out.println(args);
		blockToRun.setParameter(args);

		return blockToRun;
	}

	final protected ArgumentList coreList;
	final protected TIPLDialog g;

	final private LinkedHashMap<String, GUIControl> controls = new LinkedHashMap<String, GUIControl>();

	public ArgumentDialog(final ArgumentList inList, final String title,
			final String helpText) {
		coreList = inList;
		g = new TIPLDialog(title);
		g.addMessage(helpText, null, Color.red);
		inList.processOptions(this);
		g.showDialog();
	}

	protected GUIControl addD3Control(String cName, D3float cStat,
			String helpText) {
		addTextControl(helpText + ": " + cName + ".x", cStat.x, "help");
		addTextControl(cName + ".y", cStat.y, "");
		return addTextControl(cName + ".z", cStat.z, "");
	}

	protected GUIControl addTextControl(String cName, Object cValue,
			String helpText) {
		final GUIControl f = g.appendStringField(cName, cValue.toString());
		return f;
	}

	protected GUIControl getControl(ArgumentList.Argument cArgument) {
		final String cName = cArgument.getName();
		final String cHelp = cArgument.getHelpText();
		final String fName = cName + " [" + cHelp + "]:";
		final Object cValue = cArgument.getValue();
		if (cArgument instanceof RangedArgument<?>) {
			final RangedArgument rArg = (RangedArgument<?>) cArgument;
			if (cValue instanceof Double) {
				final double minValue = ((Double) rArg.minVal).doubleValue();
				final double maxValue = ((Double) rArg.maxVal).doubleValue();
				return g.appendSlider(fName, minValue, maxValue,
						(maxValue + minValue) / 2);
			}
		}
		if (cValue instanceof Double) {
			return g.appendNumericField(fName, ((Double) cValue).doubleValue(),
					3);
		} else if (cValue instanceof Integer) {
			return g.appendNumericField(fName, ((Integer) cValue).intValue(), 0);
		} else if (cValue instanceof Boolean) {
			final boolean cStat = ((Boolean) cValue).booleanValue();
			return g.appendCheckbox(fName, cStat);
		} else if (cValue instanceof D3float) {
			final D3float cStat = (D3float) cValue;
			return addD3Control(cName, cStat, cHelp);
		}

		return g.appendStringField(fName, cArgument.getValueAsString());

	}

	@Override
	public void process(ArgumentList.Argument cArgument) {
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
