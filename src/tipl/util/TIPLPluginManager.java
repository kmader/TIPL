/**
 * 
 */
package tipl.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;

import net.java.sezpoz.Index;
import net.java.sezpoz.IndexItem;
import net.java.sezpoz.Indexable;
import tipl.formats.TImgRO;

/**
 * @author mader
 *
 */
public class TIPLPluginManager {
	/**
	 * PluginInfo stores information about each plugin so that the proper one can be loaded for the given situation
	 * 
	 * @author mader
	 *
	 */
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.SOURCE)
	@Indexable(type = TIPLPluginFactory.class)
	public static @interface PluginInfo {
		/**
		 * The name of the plugin (VfilterScale is Filter so is FilterScale)
		 * @return
		 */
		String pluginType();
		/**
		 * short description of the plugin
		 * @return
		 */
		String desc() default "";
		/**
		 * does it operate on slices
		 * @return
		 */
		boolean sliceBased() default false;
		/**
		 * the largest image it can handle in voxels (default -1 means unlimited, or memory limited)
		 * @return
		 */
		long maximumSize() default -1;
		/**
		 * the number of bytes used per voxel in the input image approximately, -1 means it cannot be estimated)
		 * @return
		 */
		long bytesPerVoxel() default 10;
		/**
		 * the speed rank of the plugin from 0 slowest to 10 average to 20 highest (everything else being equal the fastest plugin is taken)
		 * @return
		 */
		int speedRank() default 0;
	}
	/**
	 * The static method to create a new TIPLPlugin 
	 * @author mader
	 *
	 */
	public static abstract interface TIPLPluginFactory {
		public ITIPLPlugin get();
	}
	/**
	 * Get a list of all the plugin factories that exist
	 * @return
	 * @throws InstantiationException
	 */
	public static HashMap<PluginInfo, TIPLPluginFactory> getAllPlugins() {
		
		final HashMap<PluginInfo, TIPLPluginFactory> current = new HashMap<PluginInfo, TIPLPluginFactory>();

		for (final IndexItem<PluginInfo, TIPLPluginFactory> item : Index.load(
				PluginInfo.class, TIPLPluginFactory.class)) {
			final PluginInfo bName = item.annotation();
			try {
				final TIPLPluginFactory dBlock = item.instance();
				System.out.println(bName + " loaded as: " + dBlock);
				current.put(bName, dBlock);
			} catch (InstantiationException e) {
				System.err.println("Plugin: "+bName.pluginType()+" "+bName.desc()+" could not be instanciated!");
			}
		}
		return current;
	}
	/**
	 * A list of plugins with the given type/name
	 * @param pluginType
	 * @return a hashmap of the plugins
	 * @throws InstantiationException
	 */
	public static HashMap<PluginInfo, TIPLPluginFactory> getAllPlugins(final String pluginType) 
			 {
		final HashMap<PluginInfo, TIPLPluginFactory> current=getAllPlugins();
		for(PluginInfo curPlug: current.keySet()) {
			if (!curPlug.pluginType().toUpperCase().equals(pluginType.toUpperCase())) current.remove(curPlug);
		}
		return current;
		
	}
	/** 
	 * get the best suited plugin (the first)
	 * @param pluginType
	 * @return an instance of the plugin
	 */
	public static ITIPLPlugin getBestPlugin(final String pluginType) {
		final HashMap<PluginInfo, TIPLPluginFactory> current=getAllPlugins(pluginType);
		
		for(PluginInfo curPlug: current.keySet()) {
			return current.get(curPlug).get();
		}
		throw new IllegalArgumentException("Plugin of type "+pluginType+" was not found");
	}
	/**
	 * get the best suited plugin for the given images
	 * @param pluginType name of the plugin
	 * @param inImages the input images
	 * @return an instance of the plugin
	 * @throws InstantiationException
	 */
	public static ITIPLPluginIn getBestPlugin(final String pluginType,final TImgRO[] inImages) {
		final HashMap<PluginInfo, TIPLPluginFactory> current=getAllPlugins(pluginType);
		//TODO update the code to actually look at the image
		for(PluginInfo curPlug: current.keySet()) {
			return (ITIPLPluginIn) current.get(curPlug).get();
		}
		throw new IllegalArgumentException("Plugin of type "+pluginType+" was not found");
	}
	/**
	 * get the best suited plugin for the given images and return an io plugin instead of standard one
	 * @param pluginType name of the plugin
	 * @param inImages the input images
	 * @return an instance of the plugin
	 * @throws InstantiationException
	 */
	public static ITIPLPluginIO getBestPluginIO(final String pluginType,final TImgRO[] inImages) {
		return (ITIPLPluginIO) getBestPlugin(pluginType,inImages);
	}
	/**
	 * Converts a TIPLPlugin into a runnable to it can be given to a thread (this allows run to be permanently taken out of plugin function)
	 * @param inPlug the plugin
	 * @return a runnable which runs the execute method
	 */
	public static Runnable PluginAsRunnable(final ITIPLPlugin inPlug) {
		return new Runnable() {

			@Override
			public void run() {
				inPlug.execute();
				
			}
			
		};
	}
	public TIPLPluginManager() {
	}

}
