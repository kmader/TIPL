/**
 *
 */
package tipl.util;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Vector;


// to make a dialog from the arguments

/**
 * The absolute core of ArgumentParser (should not really be used as a class by
 * itself) Implemented in this manner to control access strictly to options and
 * forwards
 *
 * @author mader
 */

public class ArgumentList implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 5651833083594630638L;
	public final static ArgumentCallback emptyCallback = new ArgumentCallback() {
        @Override
        public Object valueSet(final Object value) {
            return value;
        }
    };
    public static final int ARGUMENTTYPES = 4;
    final static public String delimiter = ",";
    static public strParse<D3float> d3fparse = new strParse<D3float>() {
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
            return new D3float(Float.valueOf(temp[0]), Float
                    .valueOf(temp[1]), Float.valueOf(temp[2]));
        }
    };
    static public strParse<D3int> d3iparse = new strParse<D3int>() {
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
            return new D3int(Integer.valueOf(temp[0]), Integer
                    .valueOf(temp[1]), Integer.valueOf(temp[2]));
        }
    };
    public String defaultPath = "";
    protected int paramIndex = 0;
    /**
     * code to parse boolean arguments (old version just assumed something being
     * present was evidence of its truth
     */
    static public strParse<Boolean> boolParse = new strParse<Boolean>() {
        @Override
        public Boolean valueOf(final String inStr) {
            if (inStr.toLowerCase().trim().contains("false"))
                return false;
            return true;
        }
    };
    static public strParse<Double> dblParse = new strParse<Double>() {
        @Override
        public Double valueOf(final String inStr) {
            return Double.valueOf(inStr);
        }
    };
    static public strParse<Float> floatParse = new strParse<Float>() {
        @Override
        public Float valueOf(final String inStr) {
            return Float.valueOf(inStr);
        }
    };
    static public strParse<Integer> intParse = new strParse<Integer>() {
        @Override
        public Integer valueOf(final String inStr) {
            return Integer.valueOf(inStr);
        }
    };
    static public strParse<String> stringParse = new strParse<String>() {
        @Override
        public String valueOf(final String inStr) {
            return inStr;
        }
    };
    static public strParse<TypedPath> typePathParse = new strParse<TypedPath>() {
        @Override
        public TypedPath valueOf(final String inStr) {
            return new TypedPath(inStr);
        }
    };
    private LinkedHashMap<String, ArgumentList.Argument> options = new LinkedHashMap<String, ArgumentList.Argument>();
    private LinkedHashMap<String, String> forwards = new LinkedHashMap<String, String>();
    private boolean overwritable = true;

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
     * Appends the current list to another object given as an input and returned
     * @param listToAppend list to be appended
     * @return a new appended list
     */
    public <T extends ArgumentList> T appendToList(T listToAppend ) {
    	for(String cKey: options.keySet())
    		listToAppend.putArg(cKey,options.get(cKey));
    	return listToAppend;
    }
    protected static String formatKey(final String inKey) {
        return inKey.toLowerCase();
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
     * @param cProc the procedure to execute
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
    /**
     * Remove an object from the argument list
     * @param opt the name of the argument to remove
     * @return was the removal successful (did the object exist)
     */
    protected boolean removeArg(final String opt) {
    	if (hasOption(opt)) {
    		options.remove(opt);
    		return true;
    	}
    	return false;
    	
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
    public ArgumentList subArguments(final String withoutText)  {
    	return subArguments(withoutText,false);
    }
    /**
     * Returns an argument parser class which does not contain arguments with
     * the given text (all forwards are passed since unused ones don't hurt
     *
     * @param withoutText
     * @param strict (only exact matches not just contains)
     * @return an argumentlist with just the filtered arguments
     */
    public ArgumentList subArguments(final String withoutText,final boolean strict) {
        final LinkedHashMap<String, ArgumentList.Argument> newOptions = new LinkedHashMap<String, ArgumentList.Argument>();
        for (final ArgumentList.Argument value : options.values()) {
        	boolean keep=true;
        	if (strict) keep = value.getName().equalsIgnoreCase(withoutText);
        	else keep = value.getName().contains(withoutText);
            if (!keep)
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
     * @param delimChar specifies which character to use to delimit the arguments when
     *                  printing them out (default is " ", but when used in
     *                  combination with SGEJob it should be &
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

    public static interface Argument {
        public ArgumentCallback getCallback();

        /**
         * a callback function used when the value is changed (usually in a
         * dialog)
         *
         * @param curCallback callback to send the value to
         */
        public void setCallback(ArgumentCallback curCallback);

        public String getHelpText();

        public String getName();

        public int getType();

        public Object getValue();

        public String getValueAsString();

        public boolean wasInput();
        
        /**
         * For re-reading parameters
         * @param newValue
         */
        public Argument cloneWithNewValue(String newValue);

    }

    /**
     * Argument is an interface for all argument types, the type is either 0
     * (parameter without key), 1 (raw key and parameter) or 2 (fully supported
     * parameter, key, default value, type, and help
     * *
     */
    public static interface ArgumentCallback {
        public Object valueSet(Object value);
    }

    /**
     * simply interface for performing operations on a list (like map in python)
     *
     * @author mader
     */
    public static interface optionProcessor {
        public void process(Argument cArgument);
    }

    /**
     * an parse a string to a generic of type T *
     */
    public static interface strParse<T> {
        public T valueOf(String inStr);
    }

    /**
     * an interface used to validate an generic argument *
     */
    public static interface tValidate<T> {
        public String getName();

        public boolean isValid(T inValue);
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
        public final String name;
        protected final String value;

        public GenericArgument(final String inName, final String inValue) {
            name = inName;
            value = inValue;
        }

        public ArgumentCallback curCallback = emptyCallback;

        @Override
        public ArgumentCallback getCallback() {
            // TODO Auto-generated method stub
            return curCallback;
        }

        @Override
        public void setCallback(final ArgumentCallback inCallback) {
            curCallback = inCallback;
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
        public boolean wasInput() {
            return true;
        }

		@Override
		public Argument cloneWithNewValue(String newValue) {
			return new GenericArgument(name,newValue);
		}


    }

    /**
     * ranged arguments are arguments that have a range of a upper and lower
     * bound, inclusive, as defined by the compareTo interface
     */
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

    /**
     * To ensure that an argument matches the type it should
     *
     * @param <T> the type the argument should be
     * @author mader
     */
    public static class TypedArgument<T> implements Argument {
    	public final String name;
        public final String helpText;
        protected final T value;
        protected final T defaultValue;
        protected final boolean usedDefault;
        protected final ArgumentParser.strParse<T> parseTool;

        public TypedArgument(final Argument inArg, final String inHelpText,
                             final T defValue, final ArgumentParser.strParse<T> tParse) {
            name = inArg.getName();
            helpText = inHelpText;
            value = tParse.valueOf(inArg.getValueAsString());
            defaultValue = defValue;
            usedDefault = false;
            parseTool=tParse;
        }
        /** just for the clone argument
         * 
         * @param inArg
         * @param inHelpText
         * @param defValue
         * @param tParse
         */
        protected TypedArgument(final String inName,final String inValue, final String inHelpText,
                final T defValue, final ArgumentParser.strParse<T> tParse) {
        	name = inName;
        	helpText = inHelpText;
        	value = tParse.valueOf(inValue);
        	defaultValue = defValue;
        	usedDefault = false;
        	parseTool=tParse;
        }
        
		@Override
		public Argument cloneWithNewValue(String newValue) {
			return new TypedArgument<T>(name,newValue,helpText,defaultValue,parseTool);
		}
        
        protected ArgumentCallback curCallback = emptyCallback;

        public TypedArgument(final String inName, final String inHelpText,
                             final T defValue,final ArgumentParser.strParse<T> tParse) {
            name = inName;
            helpText = inHelpText;
            value = defValue;
            defaultValue = defValue;
            usedDefault = true;
            parseTool=tParse;
        }

        protected TypedArgument(final TypedArgument<T> dumbClass) {
            value = dumbClass.value;
            name = dumbClass.name;
            helpText = dumbClass.helpText;
            usedDefault = dumbClass.usedDefault;
            defaultValue = dumbClass.defaultValue;
            parseTool=dumbClass.parseTool;
        }

        @Override
        public ArgumentCallback getCallback() {
            return curCallback;
        }

        @Override
        public void setCallback(final ArgumentCallback inCallback) {
            curCallback = inCallback;
        }

        @Override
        public String getHelpText() {
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

        /**
         * make a validated argument list from this typed argument *
         */
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
     * validated argument is a typed argument that also has validation criteria,
     * for example a range of acceptable values or certain string length
     * requirements
     */
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
    /**
     * A class for handling selecting one item from a list
     * @author mader
     *
     */
    public static class MultipleChoiceArgument extends ValidatedArgument<String> {
    	final String[] acceptableAnswers;
		public MultipleChoiceArgument(TypedArgument<String> basisArgument,final String[] acceptableAnswers) {
			super(basisArgument);
			this.acceptableAnswers=acceptableAnswers;
			validateList=new Vector<tValidate<String>>(1);
			validateList.add(new tValidate<String> (){

				@Override
				public String getName() {
					String outString = "Contained in: (";
					for(String cStr : acceptableAnswers) outString+=cStr+", ";
					return outString;
				}

				@Override
				public boolean isValid(String inValue) {
					for(String cStr : acceptableAnswers) if(cStr.equalsIgnoreCase(inValue)) return true;
					return false;
				}
				
			});
			
		}
    	
    }

}
