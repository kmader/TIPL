/**
 * 
 */
package tipl.ij.scripting;

import ij.IJ;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.spark.repl.SparkIMain;

import common.AbstractInterpreter;
import scala.Option;
import scala.collection.immutable.List;
import scala.tools.nsc.Settings;


/**
 * @author mader
 *
 */
public class Spark_Interpreter extends AbstractInterpreter {

	SparkIMain imain = null;
	String varname, vartype, varval, aline;
	Option<Object> varobj;
	@Override
	public void run(String args){
		Thread.currentThread().setContextClassLoader(IJ.getClassLoader());
		super.run(args);
		setTitle("Spark Interpreter");
		println("Starting Spark...");
		prompt.setEnabled(false);
		Settings settings = new Settings();
		List<String> param = List.make(1, "true");
		settings.usejavacp().tryToSet(param);
		PrintWriter stream = new PrintWriter(this.out);
		imain = new SparkIMain(settings, stream);

		preimport();
		String sparkCommand = "val sc = tipl.spark.SparkGlobal.getContext(\"FijiSpark\").sc";
		try {	
			eval(sparkCommand);

		} catch (Throwable e) {
			IJ.log("Failed starting spark " + sparkCommand);
			e.printStackTrace();
		}
		prompt.setEnabled(true);
		println("Ready.");
	}
	/**
	 * evaluates Scala commands. 
	 * value to be returned could probably be be simpler. 
	 */
	@Override
	protected Object eval(String arg0) throws Throwable {
		imain.interpret(arg0);
		varname = imain.mostRecentVar();
		varobj = imain.valueOfTerm(varname);
		if (varobj.toList().size()>0){
			varval = imain.valueOfTerm(varname).productElement(0).toString();
			vartype = imain.valueOfTerm(varname).productElement(0).getClass().getSimpleName();
			aline = varname + ": " + vartype + " = " + varval;
		} else{
			aline = varname + ": None";
		}
		return aline;
	}


	/**
	 * Overriding super abstract method.
	 * Implemented, but not used.
	 */
	@Override
	protected String getImportStatement(String packageName, Iterable<String> classNames) {
		// TODO Auto-generated method stub
		StringBuffer buffer = new StringBuffer();
		buffer.append("{");
		for (String className : classNames) {
			if (buffer.length() > 2)
				buffer.append(", ");
			buffer.append(className);
		}
		buffer.append("}");
		return "".equals(packageName) ?
				"import " + buffer + "\n":
					"import " + packageName + "." + buffer + "\n";
	}


	/** pre-imports ImageJ and Java classes.
	 * Work around of AbstractInterpreter.importAll()
	 */
	public static ArrayList<String> getPreimportStatements() {
		ArrayList<String> scalaStatements = Scala_Interpreter.getPreimportStatements();
		scalaStatements.add("org.apache.spark.SparkContext");
		scalaStatements.add("org.apache.spark.SparkContext._");
		scalaStatements.add("org.apache.spark._");
		return scalaStatements;
	}
	public void preimport() {
		for (String statement : getPreimportStatements()){
			try {
				eval("import " + statement);
			} catch (Throwable e) {
				IJ.log("Failed importing " + statement);
				e.printStackTrace();
			}
		}
	}

	@Override
	protected String getLineCommentMark() {
		return "//";
	}
	/**
	 * For debugging. 
	 * @param args
	 */
	static public void main(String[] args){
		Spark_Interpreter si = new Spark_Interpreter();
		si.run(null);
	}
}
