package tipl.util;



// to make a dialog from the arguments

/**
 * Class to Parse Input Arguments
 * 
 * @author Kevin Mader
 *         <p>
 *         The basic code used to parse command line arguments and store help
 *         <p>
 *         Change Log:
 *         <p>
 *         v4 Rewrote to as subclass of ArgumentLess (no direct access to
 *         options)
 *         <p>
 *         v3 Rewrote to allow subArguments
 *         <p>
 *         v2 Completely rewrote code using classes and more organized code
 *         <p>
 *         v1 Start
 */
public class ArgumentParser extends ArgumentList {
	/**
	 * An interface for the setParameters function used in many different
	 * objects
	 * 
	 * @author mader
	 * 
	 */
	public interface IsetParameter {
		/**
		 * A consistent interface for handling parameter setting
		 * 
		 * @param p
		 *            input argument parser
		 * @param prefix
		 *            prefix to add to all argument names (useful for nesting)
		 * @return an argument parser with the given parameter set
		 */
		public ArgumentParser setParameter(ArgumentParser p, String prefix);
	}

	public static String kVer = "14-08-2013 v4";

	protected ArgumentParser(final ArgumentList inArgs) {
		super(inArgs);
	}

	/*
	 * protected ArgumentParser(LinkedHashMap<String,ArgumentList.Argument>
	 * inOptions) { super(inOptions); }
	 */
	
	/**
	 * Creates a new arguments parser class from the standard string array input
	 * to the static main function in a Java Class
	 **/
	/**
	 * Creates a new arguments parser class from the standard string array input
	 * to the static main function in a Java Class
	 * @param args the arguments from the command line as a list of strings
	 * @param iKnowWhatImDoing an extravariable to prevent people from using this command directly
	 * (use TIPLGlobal.activeParser instead)
	 */
	@Deprecated
	public ArgumentParser(final String[] args,boolean iKnowWhatImDoing) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-") || args[i].startsWith("/")) {
				final int loc = args[i].indexOf("=");
				final String keyRaw = (loc > 0) ? args[i].substring(1, loc)
						: args[i].substring(1);

				final String value = (loc > 0) ? args[i].substring(loc + 1)
						: "";
				final String key = formatKey(keyRaw);
				putArg(key, new ArgumentList.GenericArgument(key, value));

			} else {
				// FLAT_0 is unreachable since the key is uppercase
				putArg("FLAT_" + i, new ArgumentList.EmptyArgument(args[i]));
			}
		}
	}

	@Deprecated
	public void addOption(final String inOpt, final String value) {
		final String opt = formatKey(inOpt);
		putArg(opt, new ArgumentList.GenericArgument(opt, value));
	}

	/**
	 * checks for parameters (no = sign and just bare arguments) and unrequested
	 * (generic) arguments and throws an exception
	 **/
	public void checkForInvalid() {
		String outText = "Checking for extraneous or invalid parameters...\n";
		outText += "Arguments:" + toString() + "\n";
		final int[] vDist = getDistribution();
		outText += "        Parameters\t" + vDist[0] + "\n";
		outText += "  GenericArguments\t" + vDist[1] + "\n";
		outText += "    TypedArguments\t" + vDist[2] + "\n";
		outText += "ValidatedArguments\t" + vDist[3] + "\n";
		outText += "   RangedArguments\t" + vDist[4] + "\n";
		for (int i = 5; i < vDist.length; i++) {
			outText += "Unknown Type: " + i + "\t" + vDist[i] + "\n";
		}
		if (vDist[0] > 0 || vDist[1] > 0)
			outText = getHelp() + "\n" + outText; // add help text if there is
													// an issue
		System.out.println(outText);
		if (vDist[0] > 0)
			throw new IllegalArgumentException(
					"Parameters are not allowed as input for this function\n"
							+ getByType(0));
		if (vDist[1] > 0)
			throw new IllegalArgumentException(
					"Extraneous arguments are not allowed as input\n"
							+ getByType(1));

	}

	public String getOptionAsString(final String opt) {
		return getOption(opt).getValueAsString();
	}

	public boolean getOptionBoolean(final String inOpt,final boolean defValue, final String helpString) {
		return getOptionBoolean(inOpt,defValue, helpString, emptyCallback);
	}
	public boolean getOptionBoolean(final String inOpt, final String helpString) {
		return getOptionBoolean(inOpt,false, helpString, emptyCallback);
	}

	public boolean getOptionBoolean(final String inOpt, final boolean defValue,
			final String helpString, final ArgumentCallback inCallback) {
		final String opt = formatKey(inOpt);
		ArgumentList.TypedArgument<Boolean> cArg;
		final Boolean defBooleanValue = new Boolean(defValue);
		if (hasOption(opt)) {
			cArg = new ArgumentList.TypedArgument<Boolean>(getOption(opt),
					helpString, defBooleanValue, boolParse);
		} else {
			cArg = new ArgumentList.TypedArgument<Boolean>(opt, helpString, defBooleanValue);
		}
		cArg.setCallback(inCallback);
		putArg(opt, cArg);
		return cArg.getValue().booleanValue();
	}

	public D3float getOptionD3float(final String inOpt, final D3float defVal,
			final String helpString) {
		final String opt = formatKey(inOpt);
		ArgumentList.TypedArgument<D3float> cArg;

		if (hasOption(opt)) {
			cArg = new ArgumentList.TypedArgument<D3float>(getOption(opt),
					helpString, defVal, d3fparse);
		} else {
			cArg = new ArgumentList.TypedArgument<D3float>(opt, helpString,
					defVal);
		}
		putArg(opt, cArg);
		return cArg.getValue();
	}

	public D3int getOptionD3int(final String inOpt, final D3int defVal,
			final String helpString) {
		final String opt = formatKey(inOpt);
		ArgumentList.TypedArgument<D3int> cArg;
		if (hasOption(opt)) {
			cArg = new ArgumentList.TypedArgument<D3int>(getOption(opt),
					helpString, defVal, d3iparse);
		} else {
			cArg = new ArgumentList.TypedArgument<D3int>(opt, helpString,
					defVal);
		}
		putArg(opt, cArg);
		return cArg.getValue();
	}

	/**
	 * getOptionDouble allows for double arguments to be parsed from the input
	 * arguments
	 * 
	 * @param inOpt
	 *            is the name of argument
	 * @param inDefValue
	 *            is the default value
	 * @param helpString
	 *            is the associated help string
	 * @return the value from the arguments or the default value
	 */
	public double getOptionDouble(final String inOpt, final double inDefValue,
			final String helpString) {
		final String opt = formatKey(inOpt);
		ArgumentList.TypedArgument<Double> cArg;

		final Double defValue = new Double(inDefValue);
		if (hasOption(opt)) {
			cArg = new ArgumentList.TypedArgument<Double>(getOption(opt),
					helpString, defValue, dblParse);
		} else {
			cArg = new ArgumentList.TypedArgument<Double>(opt, helpString,
					defValue);
		}
		putArg(opt, cArg);
		return cArg.getValue().doubleValue();
	}

	/**
	 * getOptionDouble allows for double arguments to be parsed from the input
	 * arguments
	 * 
	 * @param inOpt
	 *            is the name of argument
	 * @param inDefValue
	 *            is the default value
	 * @param helpString
	 *            is the associated help string
	 * @param minVal
	 *            is the minimum value
	 * @param maxVal
	 *            is the maximum value
	 * @return the value from the arguments or the default value
	 */
	public double getOptionDouble(final String inOpt, final double inDefValue,
			final String helpString, final double minVal, final double maxVal) {
		final String opt = formatKey(inOpt);

		final Double defValue = new Double(inDefValue);
		final Double minValue = new Double(minVal);
		final Double maxValue = new Double(maxVal);

		ArgumentList.TypedArgument<Double> cArg;
		if (hasOption(opt)) {
			cArg = new ArgumentList.TypedArgument<Double>(getOption(opt),
					helpString, defValue, dblParse);
		} else {
			cArg = new ArgumentList.TypedArgument<Double>(opt, helpString,
					defValue);
		}
		final ArgumentList.RangedArgument<Double> dArg = new ArgumentList.RangedArgument<Double>(
				cArg, minValue, maxValue);
		putArg(opt, dArg);
		return dArg.getValue().doubleValue();
	}

	public float getOptionFloat(final String inOpt, final float inDefValue,
			final String helpString) {
		final String opt = formatKey(inOpt);
		ArgumentList.TypedArgument<Float> cArg;

		final Float defValue = new Float(inDefValue);
		if (hasOption(opt)) {
			cArg = new ArgumentList.TypedArgument<Float>(getOption(opt),
					helpString, defValue, floatParse);
		} else {
			cArg = new ArgumentList.TypedArgument<Float>(opt, helpString,
					defValue);
		}
		putArg(opt, cArg);
		return cArg.getValue().floatValue();
	}

	public float getOptionFloat(final String inOpt, final float inDefValue,
			final String helpString, final float minVal, final float maxVal) {
		final String opt = formatKey(inOpt);
		final Float defValue = new Float(inDefValue);
		final Float minValue = new Float(minVal);
		final Float maxValue = new Float(maxVal);
		ArgumentList.TypedArgument<Float> cArg;
		if (hasOption(opt)) {
			cArg = new ArgumentList.TypedArgument<Float>(getOption(opt),
					helpString, defValue, floatParse);
		} else {
			cArg = new ArgumentList.TypedArgument<Float>(opt, helpString,
					defValue);
		}
		final ArgumentList.RangedArgument<Float> dArg = new ArgumentList.RangedArgument<Float>(
				cArg, minValue, maxValue);
		putArg(opt, dArg);
		return dArg.getValue().floatValue();
	}

	public int getOptionInt(final String inOpt, final int inDefValue,
			final String helpString) {
		final String opt = formatKey(inOpt);
		ArgumentList.TypedArgument<Integer> cArg;

		final Integer defValue = new Integer(inDefValue);
		if (hasOption(opt)) {
			cArg = new ArgumentList.TypedArgument<Integer>(getOption(opt),
					helpString, defValue, intParse);
		} else {
			cArg = new ArgumentList.TypedArgument<Integer>(opt, helpString,
					defValue);
		}
		putArg(opt, cArg);
		return cArg.getValue().intValue();
	}

	/**
	 * getOptionInt allows for double arguments to be parsed from the input
	 * arguments
	 * 
	 * @param inOpt
	 *            is the name of argument
	 * @param inDefValue
	 *            is the default value
	 * @param helpString
	 *            is the associated help string
	 * @param minVal
	 *            is the minimum value
	 * @param maxVal
	 *            is the maximum value
	 * @return the value from the arguments or the default value
	 */
	public int getOptionInt(final String inOpt, final int inDefValue,
			final String helpString, final int minVal, final int maxVal) {
		final String opt = formatKey(inOpt);

		final Integer defValue = new Integer(inDefValue);
		final Integer minValue = new Integer(minVal);
		final Integer maxValue = new Integer(maxVal);

		ArgumentList.TypedArgument<Integer> cArg;
		if (hasOption(opt)) {
			cArg = new ArgumentList.TypedArgument<Integer>(getOption(opt),
					helpString, defValue, intParse);
		} else {
			cArg = new ArgumentList.TypedArgument<Integer>(opt, helpString,
					defValue);
		}

		final ArgumentList.RangedArgument<Integer> dArg = new ArgumentList.RangedArgument<Integer>(
				cArg, minValue, maxValue);
		putArg(opt, dArg);
		return dArg.getValue().intValue();
	}

	public String getOptionPath(final String inOpt, final String defFilename,
			final String helpString) {
		final String opt = formatKey(inOpt);
		ArgumentList.TypedArgument<String> cArg;
		if (hasOption(opt)) {
			if (defaultPath.length() < 1)
				if (defFilename.lastIndexOf("//") > 0)
					defaultPath = defFilename.substring(0,
							defFilename.lastIndexOf("//") + 1);
			cArg = new ArgumentList.TypedArgument<String>(getOption(opt),
					helpString,defaultPath + defFilename, stringParse);
			// helptext.put(opt,helpString+((defFilename.length()>0) ?
			// ", Default Location (Path:"+defFilename+")" : "(string)"));
		} else {
			cArg = new ArgumentList.TypedArgument<String>(opt, helpString,
					defaultPath + defFilename);
		}
		putArg(opt, cArg);
		return cArg.getValue();
	}

	public String getOptionString(final String inOpt, final String defVal,
			final String helpString) {
		final String opt = formatKey(inOpt);
		ArgumentList.TypedArgument<String> cArg;
		if (hasOption(opt)) {
			cArg = new ArgumentList.TypedArgument<String>(getOption(opt),
					helpString, defVal, stringParse);
		} else {
			cArg = new ArgumentList.TypedArgument<String>(opt, helpString,
					defVal);
		}
		putArg(opt, cArg);
		return cArg.getValue();
	}

	@Override
	public ArgumentParser subArguments(final String withoutText) {
		return new ArgumentParser(super.subArguments(withoutText));
	}

}
