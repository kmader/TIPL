/**
 *
 */
package tipl.spark;

import tipl.util.ITIPLPlugin;
import tipl.util.TIPLPluginManager;

/**
 * A temporary hack of a plugin to load scala-based plugins into the plugin manager
 *
 * @author mader
 */
public final class ScalaPlugins {

    @TIPLPluginManager.PluginInfo(pluginType = "ShapeAnalysis",
            desc = "Spark-based shape analysis",
            sliceBased = false, sparkBased = true)
    final static public TIPLPluginManager.TIPLPluginFactory gaFactory = new TIPLPluginManager.TIPLPluginFactory() {
        @Override
        public ITIPLPlugin get() {
            return new ShapeAnalysis();

        }
    };
    
    @TIPLPluginManager.PluginInfo(pluginType = "Filter",
    	    desc = "Spark-based filtering and scale",
    	    sliceBased = false, sparkBased = true)
    final static public TIPLPluginManager.TIPLPluginFactory  svFactory = new TIPLPluginManager.TIPLPluginFactory() {
    	    public ITIPLPlugin get()  {
    	      return new SFilterScale();
    	    }
    	  };

    @TIPLPluginManager.PluginInfo(pluginType = "kVoronoi",
            desc = "Spark-based surface voronoi tesselation",
            sliceBased = false, sparkBased = true)
    final static public TIPLPluginManager.TIPLPluginFactory kvFactory = new TIPLPluginManager.TIPLPluginFactory() {
        @Override
        public ITIPLPlugin get() {
            return new SKVoronoi();
        }
    };


}
