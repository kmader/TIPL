/**
 *
 */
package tipl.util;

import tipl.spark.SFilterScale;
import tipl.spark.SKVoronoi;
import tipl.spark.SResize;
import tipl.spark.ShapeAnalysis;

/**
 * A temporary hack of a plugin to have all plugins annotations in the same file / module in TIPL
 *
 * @author mader
 */
public final class TIPLPluginAnnotation {
    @TIPLPluginManager.PluginInfo(pluginType = "ShapeAnalysis",
            desc = "Spark-based shape analysis",
            sliceBased = false, sparkBased = true)
    final public static class gaSparkFactory implements TIPLPluginManager.TIPLPluginFactory {
        @Override
        public ITIPLPlugin get() {
            return new ShapeAnalysis();

        }
    }

    @TIPLPluginManager.PluginInfo(pluginType = "Filter",
            desc = "Spark-based filtering and scale",
            sliceBased = false, sparkBased = true)
    final public static class filterSparkFactory implements TIPLPluginManager.TIPLPluginFactory {
        public ITIPLPlugin get() {
            return new SFilterScale();
        }
    }

    @TIPLPluginManager.PluginInfo(pluginType = "kVoronoi",
            desc = "Spark-based surface voronoi tesselation",
            sliceBased = false, sparkBased = true)
    final public static class kvSparkFactory implements TIPLPluginManager.TIPLPluginFactory {
        @Override
        public ITIPLPlugin get() {
            return new SKVoronoi();
        }
    }

    @TIPLPluginManager.PluginInfo(pluginType = "Resize",
            desc = "Spark-based Resize tool",
            sliceBased = true, sparkBased = true)
    final public static class rsSparkFactory implements TIPLPluginManager.TIPLPluginFactory {
        @Override
        public ITIPLPlugin get() {
            return new SResize();
        }
    }
}


