package tipl.tools;

import tipl.util.ArgumentParser;
import tipl.util.ITIPLPlugin;

/**
 * Abstract Class for performing TIPLPlugin TIPLPlugin is the class for Plug-ins
 * in the TIPL framework. This is just the basic class for a plugin supporting
 * setting and changing parameters and can thus be used for input-only,
 * output-only, and input-output plugins
 * **/
abstract public class BaseTIPLPlugin implements ITIPLPlugin {

	public BaseTIPLPlugin() {
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

}
