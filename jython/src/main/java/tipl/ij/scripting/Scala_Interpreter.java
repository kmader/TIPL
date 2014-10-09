/**
 * 
 */
package tipl.ij.scripting;


/** Scala_Interpreter.java
 * 
 * ImageJ/Fiji plugin for scala REPL,
 * extending AbstractInterpreter abstract class.
 *  
 * @author Kota Miura (miura@embl)
 * http://cmci.embl.de
 * Nov 12, 2012 -
 */
 
import java.io.PrintWriter;
import java.util.ArrayList;

import ij.IJ;
import common.AbstractInterpreter;
import scala.Option;
import scala.collection.immutable.List;
import scala.tools.nsc.Settings;
import scala.tools.nsc.interpreter.IMain;

public class Scala_Interpreter extends AbstractInterpreter{

	private IMain imain = null;
	private String varname;
    private String vartype;
    private String varval;
    private String aline;
	private Option<Object> varobj;
	
	public void run(String args){
		Thread.currentThread().setContextClassLoader(IJ.getClassLoader());
		super.run(args);
		setTitle("Scala Interpreter");
		println("Starting Scala...");
		prompt.setEnabled(false);
		Settings settings = new Settings();
		//val settings = new Settings; settings.usejavacp.value = true
		List<String> param = List.make(1, "true");
		settings.usejavacp().tryToSet(param);
		PrintWriter stream = new PrintWriter(this.out);
		imain = new IMain(settings, stream);
		//import all ImageJ classes, maybe upgrade this later. 
		//using IMain.quiteImport() should be faster
		//instead of importAll();
		preimport();
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
		ArrayList<String> scalaStatements =  new ArrayList<String>();
		scalaStatements.add("ij._");
		scalaStatements.add("java.lang.String");
		scalaStatements.add("script.imglib.math.Compute");
		scalaStatements.add("scala.math._");
		scalaStatements.add("tipl.util.TImgBlock");
		scalaStatements.add("tipl.util.TImgTools");
		scalaStatements.add("tipl.util.TIPLOps._");
		scalaStatements.add("tipl.ij.scripting.scOps._");
		return scalaStatements;
	}
	void preimport() {
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
		Scala_Interpreter si = new Scala_Interpreter();
		si.run(null);
	}
}
