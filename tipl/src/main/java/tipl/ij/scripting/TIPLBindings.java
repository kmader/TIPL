package tipl.ij.scripting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.script.Bindings;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.util.PythonInterpreter;

public class TIPLBindings
implements Bindings
{
	protected final PythonInterpreter interpreter;

	public TIPLBindings(PythonInterpreter interpreter)
	{
		this.interpreter = interpreter;
	}

	public int size()
	{
		return this.interpreter.getLocals().__len__();
	}

	public boolean isEmpty()
	{
		return size() == 0;
	}

	public boolean containsKey(Object key)
	{
		return get(key) != null;
	}

	public boolean containsValue(Object value)
	{
		for (Iterator i$ = values().iterator(); i$.hasNext(); ) { Object value2 = i$.next();
		if (value.equals(value2)) return true;
		}
		return false;
	}

	public Object get(Object key)
	{
		try {
			return this.interpreter.get((String)key); } catch (Error e) {
			}
		return null;
	}

	public Object put(String key, Object value)
	{
		Object result = get(key);
		try {
			this.interpreter.set(key, value);
		}
		catch (Error e) {
		}
		return result;
	}

	public Object remove(Object key)
	{
		Object result = get(key);
		if (result != null) this.interpreter.getLocals().__delitem__((String)key);
		return result;
	}

	public void putAll(Map<? extends String, ? extends Object> toMerge)
	{
		for (Map.Entry entry : toMerge.entrySet())
			put((String)entry.getKey(), entry.getValue());
	}

	private PyStringMap dict()
	{
		return (PyStringMap)this.interpreter.getLocals();
	}

	public void clear()
	{
		dict().clear();
	}

	public Set<String> keySet()
	{
		Set result = new HashSet();
		for (Iterator i$ = dict().keys().iterator(); i$.hasNext(); ) { Object name = i$.next();
		result.add(name.toString());
		}
		return result;
	}

	public Collection<Object> values()
	{
		List result = new ArrayList();
		for (Iterator i$ = dict().keys().iterator(); i$.hasNext(); ) { Object name = i$.next();
		try { result.add(get(name));
		} catch (Error exc)
		{
		} }
		return result;
	}

	public Set<Map.Entry<String, Object>> entrySet()
	{
		Set result = new HashSet();
		for (Iterator i$ = dict().keys().iterator(); i$.hasNext(); ) { final Object name = i$.next();
		result.add(new Map.Entry()
		{
			public String getKey()
			{
				return name.toString();
			}

			public Object getValue()
			{
				return TIPLBindings.this.get(name);
			}

			public Object setValue(Object value)
			{
				throw new UnsupportedOperationException();
			}
		});
		}
		return result;
	}
}

