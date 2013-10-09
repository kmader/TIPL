/**
 * 
 */
package tipl.util;

/**
 * The TIPLPlugin interface is designed to unify the interface for all plugins
 * to a fixed set of commands this makes it easier to automate usage, reduces
 * the number of errors, and makes future integration with ImageJ much easier as
 * plugins there have a common interface as well To use plugins in code I
 * recommend TIPLPlugin curPlugin=new ComponentLabel(argss...);, this then
 * limits you to the appropriate interfaces
 * 
 * @author mader
 * 
 */
public interface TIPLPlugin extends ArgumentParser.IsetParameter, Runnable {
	/**
	 * forces the execution of the plugin otherwise lazy evaluation might be
	 * used
	 */
	public boolean execute();

	/**
	 * execute a given task
	 * 
	 * @param actionToExecute
	 */
	public boolean execute(String actionToExecute)
			throws IllegalArgumentException;

	/**
	 * executes the plugin with a given action and a specific object
	 * 
	 * @param actionToExecute
	 * @param objectToUse
	 * @throws IllegalArgumentException
	 */
	public boolean execute(String actionToExecute, Object objectToUse)
			throws IllegalArgumentException;

	/**
	 * get information from a plugin (normally after it has been executed)
	 * 
	 * @param request
	 *            string containing the command
	 * @return some sort of object containing the requested information
	 */
	public Object getInfo(String request);

	/**
	 * Returns the name of the plugin
	 */
	public String getPluginName();

	/**
	 * Returns the activity log for the plugin
	 * 
	 * @return log as string
	 */
	public String getProcLog();

	/**
	 * sets parameters from an command line style string
	 * 
	 * @param p
	 * @return ArgumentParser in case it is replaced or something
	 */
	public ArgumentParser setParameter(String inp);

	/**
	 * Sets parameters for the plugin
	 * 
	 * @param parameterName
	 *            name of the parameter to set
	 * @param parameterValue
	 *            value for the parameter
	 */
	@Deprecated
	public void setParameter(String parameterName, Object parameterValue)
			throws IllegalArgumentException;
}
