/**
 * 
 */
package tipl.util;

import java.util.LinkedHashMap;
import java.util.Vector;

// to make a dialog from the arguments

/**
 * The absolute core of ArgumentParser (should not really be used as a class by
 * itself) Implemented in this manner to control access strictly to options and
 * forwards
 * 
 * @author mader
 * 
 */
public class ArgumentList {
	public static interface Argument {
		public ArgumentCallback getCallback();

		public String getHelpText();

		public String getName();

		public int getType();

		public Object getValue();

		public String getValueAsString();

		/**
		 * a callback function used when the value is changed (usually in a
		 * dialog)
		 * 
		 * @param curCallback
		 *            callback to send the value to
		 */
		public void setCallback(ArgumentCallback curCallback);

		public boolean wasInput();

	}

	/**
	 * Argument is an interface for all argument types, the type is either 0
	 * (parameter without key), 1 (raw key and parameter) or 2 (fully supported
	 * parameter, key, default value, type, and help
	 * **/
	public static interface ArgumentCallback {
		public Object valueSet(Object value);
	}

	protected static class EmptyArgument extends GenericArgument {
		public EmptyArgument(final String inValue) {
			super("", inValue);
		}

		@Override
		public final int getType() {
			return 0;
		}
	}

	protected static class GenericArgument implements Argument {
		protected final String value;
		public final String name;
		public ArgumentCallback curCallback = emptyCallback;

		public GenericArgument(final String inName, final String inValue) {
			name = inName;
			value = inValue;
		}

		@Override
		public ArgumentCallback getCallback() {
			// TODO Auto-generated method stub
			return curCallback;
		}

		@Override
		public final String getHelpText() {
			return "";
		}

		@Override
		public final String getName() {
			return name;
		}

		@Override
		public int getType() {
			return 1;
		}

		@Override
		public final Object getValue() {
			return getValueAsString();
		}

		@Override
		public final String getValueAsString() {
			return value;
		}

		@Override
		public void setCallback(final ArgumentCallback inCallback) {
			curCallback = inCallback;
		}

		@Override
		public boolean wasInput() {
			return true;
		}

	}

	/**
	 * simply interface for performing operations on a list (like map in python)
	 * 
	 * @author mader
	 * 
	 */
	public static interface optionProcessor {
		public void process(Argument cArgument);
	}

	/**
	 * ranged arguments are arguments that have a range of a upper and lower
	 * bound, inclusive, as defined by the compareTo interface
	 **/
	public static class RangedArgument<T extends Comparable<T>> extends
			ArgumentList.ValidatedArgument<T> {
		final T minVal;
		final T maxVal;

		public RangedArgument(
				final ArgumentList.TypedArgument<T> basisArgument,
				final T iminVal, final T imaxVal) {
			super(basisArgument);
			minVal = iminVal;
			maxVal = imaxVal;
			final Vector<ArgumentParser.tValidate<T>> oVector = new Vector<ArgumentParser.tValidate<T>>();
			oVector.add(new ArgumentParser.tValidate<T>() {
				@Override
				public String getName() {
					return "value is greater than " + minVal;
				}

				@Override
				public boolean isValid(final T inValue) {
					return (inValue.compareTo(minVal) >= 0);
				}
			});
			oVector.add(new ArgumentParser.tValidate<T>() {
				@Override
				public String getName() {
					return "value is less than " + maxVal;
				}

				@Override
				public boolean isValid(final T inValue) {
					return (inValue.compareTo(maxVal) <= 0);
				}
			});
			validateList = oVector;
			validate();
		}

		@Override
		public int getType() {
			return 4;
		}
	}

	/** an parse a string to a generic of type T **/
	public static interface strParse<T> {
		public T valueOf(String inStr);
	}

	/** an interface used to validate an generic argument **/
	public static interface tValidate<T> {
		public String getName();

		public boolean isValid(T inValue);
	}
	/**
	 * To ensure that an argument matches the type it should
	 * @author mader
	 *
	 * @param <T> the type the argument should be
	 */
	public static class TypedArgument<T> implements Argument {
		protected final T value;
		// private Class<T> type;
		protected final T defaultValue;
		protected final boolean usedDefault;
		public final String name;
		public final String helpText;
		protected ArgumentCallback curCallback = emptyCallback;

		public TypedArgument(final Argument inArg, final String inHelpText,
				final T defValue, final ArgumentParser.strParse<T> tParse) {
			name = inArg.getName();
			helpText = inHelpText;
			value = tParse.valueOf(inArg.getValueAsString());
			defaultValue = defValue;
			usedDefault = false;
		}

		// private final Class<T> mClass=T.class;
		public TypedArgument(final String inName, final String inHelpText,
				final T defValue) {
			name = inName;
			helpText = inHelpText;
			value = defValue;
			defaultValue = defValue;
			usedDefault = true;
		}

		protected TypedArgument(final TypedArgument<T> dumbClass) {
			value = dumbClass.value;
			name = dumbClass.name;
			helpText = dumbClass.helpText;
			usedDefault = dumbClass.usedDefault;
			defaultValue = dumbClass.defaultValue;
		}

		@Override
		public ArgumentCallback getCallback() {
			return curCallback;
		}

		@Override
		public String getHelpText() {
			// this.getClass().getGenericSuperclass()
			// Type myGeneric = this.getClass().getGenericSuperclass();
			// Type tType = ((ParameterizedType)
			// myGeneric).getActualTypeArguments()[0];
			return helpText + ", Default Value (:" + defaultValue + ")";
		}

		@Override
		public final String getName() {
			return name;
		}

		@Override
		public int getType() {
			return 2;
		}

		@Override
		public T getValue() {
			return value;
		}

		@Override
		public String getValueAsString() {
			return value.toString();
		}

		@Override
		public void setCallback(final ArgumentCallback inCallback) {
			curCallback = inCallback;
		}

		/** make a validated argument list from this typed argument **/
		public ValidatedArgument<T> toValidatedArgument(
				final Vector<ArgumentParser.tValidate<T>> validateList) {
			return new ValidatedArgument<T>(this, validateList);
		}

		@Override
		public boolean wasInput() {
			return !usedDefault;
		}
	}
	/**
	 * A class that allows paths to be specified in the format
	 * path::extension and be parsed properly / handled correctly by 
	 * IO plugins which need to save stacks in folders in the proper format
	 * the default extension is "" which maps to the first writing tool.
	 * @author mader
	 *
	 */
	public static class TypedPath {
		final protected String inStr;
		final protected String inType;
		public TypedPath(final TypedPath inTp) {
			this.inStr=inTp.getPath();
			this.inType=inTp.getType();
		}
		public TypedPath(final String inStr) {
			String[] spStr=inStr.split("::");
			if(spStr.length>1) {
				this.inStr=spStr[0];
				this.inType=spStr[1];
			} else {
				this.inStr=inStr;
				this.inType="";
			}
		}
		public TypedPath(final String aStr,final String aType) {
			this.inStr=aStr;
			this.inType=aType;
		}
		public String getType() {
			return this.inType;
		}
		public String getPath() {
			return this.inStr;
		}
		public String toString() {
			return inStr;
		}
	}
	

	/**
	 * validated argument is a typed argument that also has validation criteria,
	 * for example a range of acceptable values or certain string length
	 * requirements
	 **/
	public static class ValidatedArgument<T> extends TypedArgument<T> {
		protected Vector<ArgumentParser.tValidate<T>> validateList;

		protected ValidatedArgument(final TypedArgument<T> basisArgument) {
			super(basisArgument);
		}

		public ValidatedArgument(final TypedArgument<T> basisArgument,
				final Vector<ArgumentParser.tValidate<T>> InValidateList) {
			super(basisArgument);
			validateList = InValidateList;
			validate();
		}

		@Override
		public String getHelpText() {
			String outText = helpText + ", Validation Criteria:[";
			for (final ArgumentParser.tValidate<T> cTV : validateList) {
				outText += cTV.getName() + ", ";
			}
			return outText + "], Default Value (:" + defaultValue + ")";
		}

		@Override
		public int getType() {
			return 3;
		}

		protected void validate() {
			for (final ArgumentParser.tValidate<T> cValid : validateList) {
				// System.out.println("Validating variable:"+name+"="+value+" against "+cValid.getName()+": "+cValid.isValid(value));
				if (!cValid.isValid(value))
					throw new IllegalArgumentException(name + " = " + value
							+ " is not valid since " + cValid.getName());
			}
		}

	}

	public final static ArgumentCallback emptyCallback = new ArgumentCallback() {
		@Override
		public Object valueSet(final Object value) {
			return value;
		}
	};

	protected static String formatKey(final String inKey) {
		return inKey.toLowerCase();
	}

	public String delimiter = ",";
	public String defaultPath = "";
	protected int paramIndex = 0;
	private LinkedHashMap<String, ArgumentList.Argument> options = new LinkedHashMap<String, ArgumentList.Argument>();
	private LinkedHashMap<String, String> forwards = new LinkedHashMap<String, String>();
	public static final int ARGUMENTTYPES = 4;
	private boolean overwritable = true;
	/**
	 * code to parse boolean arguments (old version just assumed something being
	 * present was evidence of its truth
	 */
	protected strParse<Boolean> boolParse = new strParse<Boolean>() {
		@Override
		public Boolean valueOf(final String inStr) {
			if (inStr.toLowerCase().trim().contains("false"))
				return new Boolean(false);
			return new Boolean(true);
		}
	};
	protected strParse<D3float> d3fparse = new strParse<D3float>() {
		@Override
		public D3float valueOf(final String inStr) {
			String sStr = inStr.trim();
			final int sPos = sStr.indexOf("(");
			final int fPos = sStr.lastIndexOf(")");
			if ((sPos >= 0) & (fPos > 0))
				sStr = sStr.substring(sPos + 1, fPos);
			final String[] temp = sStr.split(delimiter);
			if (temp.length < 3) {
				System.err.println("Not a valid D3float input: " + inStr);
				throw new IllegalArgumentException("Not a valid D3float input: " + inStr);
				
			}
			return new D3float(Float.valueOf(temp[0]).floatValue(), Float
					.valueOf(temp[1]).floatValue(), Float.valueOf(temp[2])
					.floatValue());
		}
	};
	protected strParse<D3int> d3iparse = new strParse<D3int>() {
		@Override
		public D3int valueOf(final String inStr) {
			String sStr = inStr.trim();
			final int sPos = sStr.indexOf("(");
			final int fPos = sStr.lastIndexOf(")");
			if ((sPos >= 0) & (fPos > 0))
				sStr = sStr.substring(sPos + 1, fPos);
			final String[] temp = sStr.split(delimiter);
			if (temp.length < 3) {
				System.err.println("Not a valid D3int input: " + inStr);
				throw new IllegalArgumentException("Not a valid D3int input: " + inStr);
			}
			return new D3int(Integer.valueOf(temp[0]).intValue(), Integer
					.valueOf(temp[1]).intValue(), Integer.valueOf(temp[2])
					.intValue());
		}
	};
	protected strParse<Double> dblParse = new strParse<Double>() {
		@Override
		public Double valueOf(final String inStr) {
			return Double.valueOf(inStr);
		}
	};
	protected strParse<Float> floatParse = new strParse<Float>() {
		@Override
		public Float valueOf(final String inStr) {
			return Float.valueOf(inStr);
		}
	};
	protected strParse<Integer> intParse = new strParse<Integer>() {
		@Override
		public Integer valueOf(final String inStr) {
			return Integer.valueOf(inStr);
		}
	};
	protected strParse<String> stringParse = new strParse<String>() {
		@Override
		public String valueOf(final String inStr) {
			return inStr;
		}
	};
	protected strParse<TypedPath> typePathParse = new strParse<TypedPath>() {
		@Override
		public TypedPath valueOf(final String inStr) {
			return new TypedPath(inStr);
		}
	};

	public ArgumentList() {
	}

	protected ArgumentList(final ArgumentList inArgumentList) {
		options = inArgumentList.sneakyGetOptions();
		forwards = inArgumentList.sneakyGetForwards();
	}

	protected ArgumentList(
			final LinkedHashMap<String, ArgumentList.Argument> inOptions,
			final LinkedHashMap<String, String> inForwards) {
		options = inOptions;
		forwards = inForwards;
	}

	/**
	 * prevent arguments from being overwritten
	 */
	public void blockOverwrite() {
		overwritable = false;
	}
	
	/**
	 * allows arguments to be overwritten
	 */
	public void releaseOverwrite() {
		overwritable = false;
	}

	/**
	 * can arguments be overwritten (used in blocks)
	 * 
	 * @return
	 */
	protected boolean canOverwrite() {
		return overwritable;
	}

	/**
	 * Forces outOption to be equal to in option
	 * 
	 * @param inOption
	 * @param outOption
	 */
	public void forceMatchingValues(final String inOption,
			final String outOption) {
		forwards.put(outOption, inOption);
	}

	protected String getByType(final int type) {
		String outText = "";
		for (final ArgumentList.Argument value : options.values()) {
			if (value.getType() == type)
				outText += value.getName() + "\t=\t" + value.getValueAsString()
						+ "\n";
		}
		return outText;
	}

	protected int[] getDistribution() {
		final int[] outVals = new int[ARGUMENTTYPES + 1];
		for (final ArgumentList.Argument value : options.values()) {
			outVals[value.getType()]++;
		}
		return outVals;
	}

	/**
	 * Help text generation
	 * 
	 * @return a help message as a string
	 */
	public String getHelp() {
		String outText = "";
		for (final String key : options.keySet()) {
			if (forwards.containsKey(key)) {
				final String sKey = forwards.get(key);
				outText += "\t-" + key + "-> " + sKey + " (Hard Forward)\n";
			} else {
				final Argument value = options.get(key);
				outText += "\t-" + key + " = " + (value.getHelpText()) + "\n";
			}
		}
		final int[] vDist = getDistribution();
		outText += "        Parameters\t" + vDist[0] + "\n";
		outText += "  GenericArguments\t" + vDist[1] + "\n";
		outText += "    TypedArguments\t" + vDist[2] + "\n";
		outText += "ValidatedArguments\t" + vDist[3] + "\n";
		outText += "   RangedArguments\t" + vDist[4] + "\n";
		outText += " ForwardedAguments\t" + forwards.size() + "\n";
		for (int i = 5; i < vDist.length; i++) {
			outText += "Unknown Type: " + i + "\t" + vDist[i] + "\n";
		}

		return outText;
	}

	protected ArgumentList.Argument getOption(final String opt) {
		String keyName = formatKey(opt);
		if (forwards.containsKey(keyName))
			keyName = forwards.get(keyName);
		return options.get(keyName);
	}

	public boolean hasOption(final String opt) {
		String keyName = formatKey(opt);
		if (forwards.containsKey(keyName))
			keyName = forwards.get(keyName);
		return options.containsKey(keyName);
	}

	@Deprecated
	public String nextParam() {
		if (paramIndex < ParamCount()) {
			int cIndex = 0;
			for (final ArgumentList.Argument value : options.values()) {
				if (cIndex > paramIndex) {
					if (value.getType() < 1)
						return value.getValueAsString();
				}
			}
			cIndex++;
		}
		return null;
	}

	@Deprecated
	public int ParamCount() {
		int paramCnt = 0;
		for (final ArgumentList.Argument value : options.values()) {
			if (value.getType() < 1)
				paramCnt++;
		}
		return paramCnt;
	}

	@Deprecated
	public void ParamZero() {
		paramIndex = 0;
	}

	/**
	 * simply perform a function on each option in the list
	 * 
	 * @param cProc
	 *            the procedure to execute
	 */
	public void processOptions(final optionProcessor cProc) {
		for (final ArgumentList.Argument cArgument : options.values())
			cProc.process(cArgument);
	}

	/**
	 * put an argument and check to see if it is being overwritten
	 * 
	 * @param opt
	 * @param curObj
	 */
	protected void putArg(final String opt, final ArgumentList.Argument curObj) {
		if (hasOption(opt) & (!canOverwrite())) {
			// only complain if it isn't genericargument
			if (!(getOption(opt) instanceof ArgumentList.GenericArgument)) {
				System.err
						.println(" Argument:"
								+ opt
								+ " already exists and sensitive mode is on, will not overwrite");
				return;
			}
		}
		options.put(opt, curObj);
	}

	@Deprecated
	final public LinkedHashMap<String, String> sneakyGetForwards() {
		return forwards;
	}

	@Deprecated
	final public LinkedHashMap<String, ArgumentList.Argument> sneakyGetOptions() {
		return options;
	}

	/**
	 * Returns an argument parser class which does not contain arguments with
	 * the given text (all forwards are passed since unused ones don't hurt
	 * 
	 * @param withoutText
	 * @return an argumentlist with just the filtered arguments
	 */
	public ArgumentList subArguments(final String withoutText) {
		final LinkedHashMap<String, ArgumentList.Argument> newOptions = new LinkedHashMap<String, ArgumentList.Argument>();
		for (final ArgumentList.Argument value : options.values()) {
			if (!value.getName().contains(withoutText))
				newOptions.put(value.getName(), value);
		}

		return new ArgumentList(newOptions, forwards);
	}

	@Override
	public String toString() {
		return toString(" ");
	}

	/**
	 * Creates string representation of list
	 * 
	 * @param delimChar
	 *            specifies which character to use to delimit the arguments when
	 *            printing them out (default is " ", but when used in
	 *            combination with SGEJob it should be &
	 * @return all arguments as a single string
	 */
	public String toString(final String delimChar) {
		String outText = "";
		for (final ArgumentList.Argument value : options.values()) {
			outText += delimChar + "-" + value.getName() + "="
					+ (value.getValueAsString());
		}
		return outText;
	}

}
