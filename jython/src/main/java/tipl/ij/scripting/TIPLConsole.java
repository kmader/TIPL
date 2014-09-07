package tipl.ij.scripting;
import ij.IJ;
import imagej.script.AbstractScriptEngine;
import imagej.script.AdaptedScriptLanguage;
import imagej.script.ScriptLanguage;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.python.core.CompileMode;
import org.python.core.CompilerFlags;
import org.python.core.ParserFacade;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
//import org.scijava.plugin.Plugin;

import common.AbstractInterpreter;


public class TIPLConsole extends AbstractInterpreter {
	
	protected PythonInterpreter pi;
	protected PyDictionary globals = new PyDictionary();
	protected PySystemState pystate = new PySystemState();

	public TIPLConsole() { }	

	public TIPLConsole(PythonInterpreter pi) {
		this.pi = pi;
	}
	/**
	 * Import the default TIPL classes so images can be read and processed easily
	 */
	protected void importDefault() {
		
	}

	public void run(String arg) {
		super.run(arg);
		super.window.setTitle("TIPL Interpreter");
		super.prompt.setEnabled(false);
		print("Starting TIPL ...");
		// Create a python interpreter that can load classes from plugin jar files.
		ClassLoader classLoader = IJ.getClassLoader();
		if (classLoader == null)
			classLoader = getClass().getClassLoader();
		PySystemState.initialize(System.getProperties(), System.getProperties(), new String[] { }, classLoader);
		pystate.setClassLoader(classLoader);
		pi = new PythonInterpreter(globals, pystate);
		//redirect stdout and stderr to the screen for the interpreter
		pi.setOut(out);
		pi.setErr(out);
		//pre-import all ImageJ java classes and TrakEM2 java classes
		importAll();
		
		// fix back on closing
		super.window.addWindowListener(
			new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					pi.setOut(System.out);
					pi.setErr(System.err);
				}
			}
		);
		super.prompt.setEnabled(true);
		super.prompt.requestFocus();
		println("... done.");
	}

	@Override
	protected void windowClosing() {
		super.windowClosing();
		try {
			if (null != pi) pi.cleanup();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/** Evaluate python code. */
	protected Object eval(String text) {
		// Ensure MacOSX and Windows work fine until the alternative is tested
		if ( ! IJ.isLinux()) {
			pi.exec(text);
			return null;
		}

		// A. Prints None
		//Py.setSystemState(pystate);
		//PyObject po = Py.runCode(Py.compile_flags(text, "<string>", CompileMode.exec, Py.getCompilerFlags(0, false)), pi.getLocals(), globals);
		//Py.flushLine();
		//return po;

		// B. Prints None
		//return pi.eval(Py.compile_flags(text, "<string>", CompileMode.exec, Py.getCompilerFlags(0, false)));

		// C. Works! Prints to stdout the last evaluated expression if it is meaningful
		// (for example def something doesn't print, but a single number does.)
		CompilerFlags cflags = Py.getCompilerFlags(0, false);
		String filename = "<string>";
		pi.eval(Py.compile_flags(ParserFacade.parse(text, CompileMode.exec, filename, cflags), 
						Py.getName(), filename, true, true, cflags));
		return null;

	}

	/** Returns an ArrayList of String, each entry a possible word expansion. */
	protected ArrayList expandStub(String stub) {
		final ArrayList al = new ArrayList();
		PyObject py_vars = pi.eval("vars().keys()");
		if (null == py_vars) {
			p("No vars to search into");
			return al;
		}
		String[] vars = (String[])py_vars.__tojava__(String[].class);
		for (int i=0; i<vars.length; i++) {
			if (vars[i].startsWith(stub)) {
				//System.out.println(vars[i]);
				al.add(vars[i]);
			}
		}
		Collections.sort(al, String.CASE_INSENSITIVE_ORDER);
		System.out.println("stub: '" + stub + "'");
		return al;
	}

	protected String getImportStatement(String packageName, Iterable<String> classNames) {
		StringBuffer buffer = new StringBuffer();
		for (String className : classNames) {
			if (buffer.length() > 0)
				buffer.append(", ");
			buffer.append(className);
		}
		return "".equals(packageName) ?
			"import " + buffer + "\n":
			"from " + packageName + " import " + buffer + "\n";
	}

	protected String getLineCommentMark() {
		return "#";
	}
        
        



       /**
        @Plugin(type=ScriptLanguage.class)
        public class TIPLScriptLanguage extends AdaptedScriptLanguage
        {
          public TIPLScriptLanguage()
          {
            super("tipl");
          }

          public String getLanguageName()
          {
            return "TIPL Python";
          }

          public ScriptEngine getScriptEngine()
          {
            return new TIPLScriptEngine();
          }
        } **/
        
        
        public class TIPLScriptEngine extends AbstractScriptEngine
        {
          protected final PythonInterpreter interpreter;
        
          final protected Reader reader;
          final protected Writer writer;
          final protected Writer errorWriter;
          
          public TIPLScriptEngine(Reader reader,Writer writer,Writer errorWriter)
          {
        	this.reader=reader;
        	this.writer=writer;
        	this.errorWriter=errorWriter;
            this.interpreter = new PythonInterpreter();
            this.engineScopeBindings = new TIPLBindings(this.interpreter);
          }
          public TIPLScriptEngine()
          {
        	  super();
        	this.reader=getContext().getReader();
        	this.writer=getContext().getWriter();
        	this.errorWriter=getContext().getErrorWriter();
            this.interpreter = new PythonInterpreter();
            this.engineScopeBindings = new TIPLBindings(this.interpreter);
          }
        
          public Object eval(String script) throws ScriptException
          {
            setup();
            try {
              return this.interpreter.eval(script);
            }
            catch (Exception e) {
              throw new ScriptException(e);
            }
          }
        
          public Object eval(Reader reader) throws ScriptException
          {
            setup();
            try {
              String filename = getString("javax.script.filename");
              return Py.runCode(this.interpreter.compile(reader, filename), null, this.interpreter.getLocals());
            }
            catch (Exception e) {
              throw new ScriptException(e);
            }
          }
        
          protected void setup() {
            ScriptContext context = getContext();

            if (reader != null) {
              this.interpreter.setIn(reader);
            }
            if (writer != null) {
              this.interpreter.setOut(writer);
            }
            if (errorWriter != null)
              this.interpreter.setErr(errorWriter);
          }
        
          private String getString(String key)
          {
            Object result = get(key);
            return result == null ? null : result.toString();
          }
        }

}