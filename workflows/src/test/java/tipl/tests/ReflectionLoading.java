package tipl.tests;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reflections.Reflections;

import tipl.blocks.BaseTIPLBlock;
import tipl.blocks.ITIPLBlock;
import tipl.blocks.BaseTIPLBlock.BlockMaker;
import tipl.blocks.BaseTIPLBlock.TIPLBlockFactory;
import tipl.util.TIPLGlobal;

public class ReflectionLoading {

	public static abstract class RefClass<T> {
		protected abstract T make() throws InstantiationException, IllegalAccessException;
		public T get() {
			try {
				return make();
			} catch (InstantiationException e) {
				e.printStackTrace();
				throw new IllegalArgumentException(e+" cannot be initiated");
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				throw new IllegalArgumentException(e+" cannot be accessed");
			}
		}
	}
	
	/**
	 * Gets the list of all the blocks available in the tipl library
	 * @return a dictionary of all blocks
	 */
	public static <T> Map<String,RefClass<T>> getObjList(String basePath,Class<T> subType) {
		Reflections reflections = new Reflections(basePath); 
		Set<Class<? extends T>> classes = reflections.getSubTypesOf(subType);
		HashMap<String,RefClass<T>> blocks = new HashMap<String,RefClass<T>>();
		for(final Class<? extends T> curClass : classes) {
			if (TIPLGlobal.getDebug()) System.out.println("tipl:Loading Class:"+curClass.getSimpleName());
			blocks.put(curClass.getCanonicalName(),
					new RefClass<T>() {
						@Override
						protected T make()
								throws InstantiationException,
								IllegalAccessException {
							return curClass.newInstance();
						}			
			});
		}
		return blocks;
	}
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TIPLGlobal.setDebug(TIPLGlobal.DEBUG_ALL);
		org.apache.log4j.BasicConfigurator.configure();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void testAnnotations() {
		
		try {
			BaseTIPLBlock.getAllBlockFactories();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Test
	public final void testReflections() {

			BaseTIPLBlock.getBlockList();
	}
	
	@Test
	public final void testPlugins() {
		getObjList("tipl.tools",tipl.util.ITIPLPlugin.class);
	}

}
