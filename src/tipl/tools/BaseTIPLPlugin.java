package tipl.tools;

import tipl.util.ArgumentParser;
import tipl.util.ITIPLPlugin;
import tipl.util.TIPLGlobal;

/**
 * Abstract Class for performing TIPLPlugin TIPLPlugin is the class for Plug-ins
 * in the TIPL framework. This is just the basic class for a plugin supporting
 * setting and changing parameters and can thus be used for input-only,
 * output-only, and input-output plugins
 * **/
abstract public class BaseTIPLPlugin implements ITIPLPlugin {

	public BaseTIPLPlugin() {
		TIPLGlobal.getUsage().registerPlugin(getPluginName(), this.toString());
	}

	/**
	 * Turn the string into an argumentparser and send it on through default is
	 * no prefix
	 * */
	public ArgumentParser setParameter(final ArgumentParser p) {
		return setParameter(p, "");
	}

	/**
	 * Turn the string into an argumentparser and send it on through default is
	 * no prefix, this is by default strictly checked because we will not assume
	 * the output is going somewhere else
	 * */
	@Override
	public ArgumentParser setParameter(final String p) {
		final ArgumentParser t = setParameter(new ArgumentParser(p.split(" ")));
		t.checkForInvalid();
		return t;
	}

	/**
	 * The default action is just do nothing, other features can be implemented
	 * on a case by case basis
	 */
	@Override
	public void setParameter(final String argumentName,
			final Object argumentValue) {
		if (!argumentName.equals(""))
			throw new IllegalArgumentException(
					"SetParameter is not implemented for this plugins"
							+ getPluginName());
	}

	/**
	 * Turn the string into an argumentparser and send it on through
	 * */
	public ArgumentParser setParameter(final String p, final String prefix) {
		return setParameter(new ArgumentParser(p.split("\\s+")), prefix);
	}
	
	/**
	 * The default action is just to run execute, other features can be
	 * implemented on a case by case basis
	 */
	@Override
	public boolean execute(final String action) {
		if (!action.equals(""))
			throw new IllegalArgumentException(
					"Execute Does not offer any control in this plugins"
							+ getPluginName());
		return execute();
	}

	/**
	 * The default action is just to run execute, other features can be
	 * implemented on a case by case basis
	 */
	@Override
	public boolean execute(final String action, final Object objectToExecute) {
		if (!action.equals(""))
			throw new IllegalArgumentException(
					"Execute Does not offer any control in this plugins"
							+ getPluginName());
		return execute();
	}

	/**
	 * if this function has not been overridden, it will cause an error
	 */
	@Override
	public Object getInfo(final String request) {
		throw new IllegalArgumentException(
				"getInfo does not offer any information in this plugin:"
						+ getPluginName());
	}

	@Override
	abstract public String getPluginName();

	/**
	 * Procedure Log for the function, should be added back to the aim-file
	 * after function operation is complete
	 */
	protected String procLog = "";
	
	@Override
	public String getProcLog() {
		return procLog;
	}

}
