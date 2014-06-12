/**
 * 
 */
package tipl.spark;
import tipl.util.ITIPLPlugin;
import tipl.util.TIPLPluginManager;
/**
 * A temporary hack of a plugin to load scala-based plugins into the plugin manager
 * @author mader
 *
 */
public final class ScalaPlugins {
	
	@TIPLPluginManager.PluginInfo(pluginType = "GrayAnalysis",
			desc="Spark-based gray value analysis",
			sliceBased=false,sparkBased=true)
	final static public TIPLPluginManager.TIPLPluginFactory gaFactory  = new TIPLPluginManager.TIPLPluginFactory() {
		@Override 
		public ITIPLPlugin get() {
			return new ShapeAnalysis();
		}
	};
  
}
