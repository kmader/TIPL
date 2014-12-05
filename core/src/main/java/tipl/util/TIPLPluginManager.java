/**
 *
 */
package tipl.util;

import ij.IJ;
import ij.gui.GenericDialog;
import org.scijava.annotations.Index;
import org.scijava.annotations.IndexItem;
import org.scijava.annotations.Indexable;
import tipl.formats.TImgRO;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;

import java.util.Map.Entry;


/**
 * @author mader
 */
public class TIPLPluginManager {
    private static final Map<PluginInfo, TIPLPluginFactory> pluginList = new HashMap<PluginInfo, TIPLPluginFactory>() {
        @Override
        public TIPLPluginFactory get(Object ikey) {
            final PluginInfo key = ((PluginInfo) ikey);
            final String cVal = key.toString();
            for (Entry<PluginInfo, TIPLPluginFactory> cKey : this.entrySet()) {
                if (cKey.getKey().toString().equalsIgnoreCase(cVal)) return cKey.getValue();
            }
            throw new IllegalArgumentException("Plugin:" + key.pluginType() + ":\t" + cVal + " cannot be found in the plugin tree");
        }
    };
    /**
     * The standard image size if none is provided
     */
    public static D3int DEFAULT_IMAGEDIM = new D3int(1000, 1000, 1000);

    /**
     * get the named plugin from the list
     * @param curInfo the information on the plugin
     * @return
     */
    public static ITIPLPlugin getPlugin(PluginInfo curInfo) {
        System.out.println("Requesting:" + curInfo.toString() + "\t" + pluginList.get(curInfo));


        if (getAllPlugins().contains(curInfo)) {
            for (Map.Entry<PluginInfo, TIPLPluginFactory> cf : pluginList.entrySet()) {
                if (cf.getKey().toString().equals(curInfo.toString())) {
                    System.out.println("Found a match:" + curInfo);
                    return cf.getValue().get();
                }
            }
            return pluginList.get(curInfo).get();
        } else
            throw new IllegalArgumentException("Plugin:" + curInfo.pluginType() + " with info" + curInfo + " has not yet been loaded");
    }

    /**
     * get the best suited plugin for the given images and return an io plugin instead of standard one
     * @param curInfo information on the plugin to get
     * @return an instance of the plugin
     * @throws InstantiationException
     */
    public static ITIPLPluginIO getPluginIO(PluginInfo curInfo) {
        return (ITIPLPluginIO) getPlugin(curInfo);
    }

    /**
     * get the best suited plugin for the given images and return an input plugin instead of
     * standard one
     * @param curInfo information on the plugin to get
     * @return an instance of the plugin
     */
    public static ITIPLPluginIn getPluginIn(PluginInfo curInfo) {
        return (ITIPLPluginIn) getPlugin(curInfo);
    }

    /**
     * get the best suited plugin for the given images and return an output plugin instead of
     * standard one
     * @param curInfo information on the plugin to get
     * @return an instance of the plugin

     */
    public static ITIPLPluginOut getPluginOut(PluginInfo curInfo) {
        return (ITIPLPluginOut) getPlugin(curInfo);
    }

    /**
     * Get a list of all the plugin factories that exist
     */
    public static List<PluginInfo> getAllPlugins() {
        if (pluginList.size() > 1) return new ArrayList<PluginInfo>(pluginList.keySet());
        for (Iterator<IndexItem<PluginInfo>> cIter = Index.load(PluginInfo.class).iterator(); cIter.hasNext(); ) {
            final IndexItem<PluginInfo> item = cIter.next();

            final PluginInfo bName = item.annotation();

            try {

                final TIPLPluginFactory dBlock = (TIPLPluginFactory) Class.forName(item.className()).newInstance();
                System.out.println(bName + " loaded as: " + dBlock);
                pluginList.put(bName, dBlock);
            } catch (InstantiationException e) {
                System.err.println("Plugin: " + bName.pluginType() + " " + bName.desc() + " could not be loaded or instantiated by plugin manager!\t" + e);
                if (TIPLGlobal.getDebug()) e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.err.println("Plugin: " + bName.pluginType() + " " + bName.desc() + " could not be found by plugin manager!\t" + e);
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                System.err.println("Plugin: " + bName.pluginType() + " " + bName.desc() + " was accessed illegally by plugin manager!\t" + e);
                e.printStackTrace();
            }
        }

        return new ArrayList<PluginInfo>(pluginList.keySet());
    }

    /**
     * A list of plugins with the given type/name
     * @param pluginType
     * @return a hashmap of the plugins
     * @throws InstantiationException
     */
    public static List<PluginInfo> getPluginsNamed(final String pluginType) {
        List<PluginInfo> outList = new ArrayList<PluginInfo>();
        for (PluginInfo cPlug : getAllPlugins()) {
            if (cPlug.pluginType().equalsIgnoreCase(pluginType))
                outList.add(cPlug);
        }
        return outList;
    }

    public static List<PluginInfo> getPluginsBySize(final List<PluginInfo> inList, long voxelCount) {
        List<PluginInfo> outList = new ArrayList<PluginInfo>(inList.size());
        for (PluginInfo cPlug : inList) {
            if ((cPlug.maximumSize() > voxelCount) ||
                    (cPlug.maximumSize() < 0))
                outList.add(cPlug);
        }
        return outList;
    }

    /**
     * get the fastest plugin (by speed rank)
     * @param inList the list of possible plugins
     * @return info for a single plugin
     */
    public static PluginInfo getFastestPlugin(final List<PluginInfo> inList) {
        PluginInfo fastestPlug = inList.get(0);
        for (PluginInfo cPlug : inList) {
            if (cPlug.speedRank() > fastestPlug.speedRank()) fastestPlug = cPlug;
        }
        return fastestPlug;
    }

    /**
     * get the lowest memory usage plugin
     * @param inList the list of possible plugins
     * @return info for a single plugin
     */
    public static PluginInfo getLowestMemoryPlugin(final List<PluginInfo> inList) {
        PluginInfo lowestPlug = inList.get(0);
        for (PluginInfo cPlug : inList) {
            if (cPlug.bytesPerVoxel() < lowestPlug.bytesPerVoxel()) lowestPlug = cPlug;
        }
        return lowestPlug;
    }

    protected static <T extends TImgTools.HasDimensions> PluginInfo getBestPlugin(final String pluginType, final T[] inImages) {
        //TODO get size from other images at some point
        List<PluginInfo> namedPlugins = getPluginsNamed(pluginType);
        if (namedPlugins.size() < 1)
            throw new IllegalArgumentException("No plugins of type:" + pluginType + " have been loaded, check classpath, and annotation setup");
        String outPlugName;

        if (TIPLGlobal.getDebug()) {
            outPlugName = "Named Plugins Plugins for " + pluginType + " are: ";
            for (PluginInfo cPlug : namedPlugins) outPlugName += "," + cPlug.toString();
            System.out.println(outPlugName);
        }

        long imVoxCount = (long) inImages[0].getDim().prod();
        List<PluginInfo> bestPlugins = getPluginsBySize(namedPlugins, imVoxCount);
        if (bestPlugins.size() < 1)
            throw new IllegalArgumentException("No plugins of type:" + pluginType + " can handle images sized:" + imVoxCount);


        if (TIPLGlobal.getDebug()) {
            outPlugName = "Sized Plugins (>" + imVoxCount + ") for " + pluginType + " are: ";

            for (PluginInfo cPlug : bestPlugins) outPlugName += "," + cPlug.toString();
            System.out.println(outPlugName);
        }

        // remove spark plugins

        List<PluginInfo> noSparkPlugins = new ArrayList<PluginInfo>(bestPlugins.size());
        for (PluginInfo cPlug : bestPlugins) {
            if (!cPlug.sparkBased())
                noSparkPlugins.add(cPlug);
        }
        if (noSparkPlugins.size() < 1)
            throw new IllegalArgumentException("No plugins of type:" + pluginType + " can handle images sized:" + imVoxCount + " can run without Spark");

        if (TIPLGlobal.getDebug()) {
            outPlugName = "Available Plugins for " + pluginType + " are: ";
            for (PluginInfo cPlug : noSparkPlugins) outPlugName += "," + cPlug.toString();
            System.out.println(outPlugName);
        }

        return getFastestPlugin(noSparkPlugins);
    }

    /**
     * Create an empty default image with the correct size
     */
    public static final TImgTools.HasDimensions[] getDefaultImage() {
        return new TImgTools.HasDimensions[]{
                new TImgTools.HasDimensions() {
                    @Override
                    public D3int getDim() {
                        return DEFAULT_IMAGEDIM;
                    }

                    @Override
                    public D3float getElSize() {
                        return new D3float(1, 1, 1);
                    }

                    @Override
                    public D3int getOffset() {
                        return new D3int(0, 0, 0);
                    }

                    @Override
                    public D3int getPos() {
                        return new D3int(0, 0, 0);
                    }

                    @Override
                    public String getProcLog() {
                        return "";
                    }

                    @Override
                    public float getShortScaleFactor() {
                        return 1;
                    }
                }};
    }

    /**
     * get the best suited plugin for the given images
     * @param pluginType name of the plugin
     * @param inImages the input images
     * @return an instance of the plugin as a standard plugin
     */
    public static <T extends TImgTools.HasDimensions> ITIPLPlugin createBestPlugin(final String pluginType, final T[] inImages) {
        return getPlugin(getBestPlugin(pluginType, inImages));
    }

    /**
     * get the best suited plugin taking input for the given images
     * @param pluginType name of the plugin
     * @param inImages the input images
     * @return an instance of the plugin as a pluginin object (supporting loadimages)
     */
    public static <T extends TImgTools.HasDimensions> ITIPLPluginIn createBestPluginIn(final String
                                                                                            pluginType, final T[] inImages) {
        return getPluginIn(getBestPlugin(pluginType, inImages));
    }

    /**
     * get the best suited plugin for the given images
     * @param pluginType name of the plugin
     * @return an instance of the plugin as a standard plugin
     */
    public static ITIPLPlugin createBestPlugin(final String pluginType) {
        return getPlugin(getBestPlugin(pluginType, getDefaultImage()));
    }

    /**
     * get the best suited plugin for the given images
     * @param pluginType name of the plugin
     * @param inImages the input images
     * @return an instance of the plugin as an io plugin
     */
    public static <T extends TImgTools.HasDimensions> ITIPLPluginIO createBestPluginIO(final String pluginType, final T[] inImages) {
        return getPluginIO(getBestPlugin(pluginType, inImages));
    }

    /**
     * get the best suited plugin for the given images
     * @param pluginType name of the plugin
     * @return an instance of the plugin as an io plugin
     */
    public static ITIPLPluginIO createBestPluginIO(final String pluginType) {
        return getPluginIO(getBestPlugin(pluginType, getDefaultImage()));
    }

    /**
     * get the fastest (or first) suited plugin for the given images
     * @param pluginType name of the plugin
     * @return an instance of the plugin as an io plugin
     */
    public static ITIPLPluginIO createFirstPluginIO(final String pluginType) {
        return getPluginIO(getFastestPlugin(getPluginsNamed(pluginType)));
    }

    /**
     * get the fastest (or first) suited plugin for the given images
     * @param pluginType name of the plugin
     * @return an instance of the plugin as  plugin
     */
    public static ITIPLPlugin createFirstPlugin(final String pluginType) {
        return getPlugin(getFastestPlugin(getPluginsNamed(pluginType)));
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

    public static void main(String[] args) {

        String outPlugName = "Installed Plugins are: ";

        for (PluginInfo cPlug : getAllPlugins())
            outPlugName += "\nType:" + cPlug.pluginType() + "\t" + cPlug.toString();
        System.out.println(TIPLPluginManager.class.getName() + " showing all plugins available");
        System.out.println(outPlugName);
    }

    /**
     * PluginInfo stores information about each plugin so that the proper one can be loaded for the given situation
     *
     * @author mader
     *
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @Indexable
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
         * the largest image it can handle in voxels ( -1 means unlimited, or memory limited), default is the longest an array is allowed to be
         * @return
         */
        long maximumSize() default Integer.MAX_VALUE - 1;

        /**
         * the number of bytes used per voxel in the input image approximately, -1 means it cannot be estimated)
         * @return
         */
        long bytesPerVoxel() default -1;

        /**
         * the speed rank of the plugin from 0 slowest to 10 average to 20 highest (everything else being equal the fastest plugin is taken)
         * @return
         */
        int speedRank() default 10;

        /**
         * does the plugin require Spark in order to run
         * @return
         */
        boolean sparkBased() default false;
    }


    /**
     * The static method to create a new TIPLPlugin
     * @author mader
     *
     */
    public static interface TIPLPluginFactory {
        public ITIPLPlugin get();
    }

    public static interface TIPLPluginFactorSmart extends TIPLPluginFactory {
        public long estimateMemory(TImgRO[] inImgs);

        public long estimateSpeed(TImgRO[] inImgs);
    }

    /**
     * The connection between TIPL and ImageJ plugins. It creates a general interface for using all of the plugins on imageJ images.
     * @author mader
     *
     */
    public static class IJlink implements ij.plugin.PlugIn {

        @Override
        public void run(String arg0) {
            IJ.log("Starting plugin:" + this);
            GenericDialog localGenericDialog = new GenericDialog("TIPL Function to Run");
            List<PluginInfo> plugins = TIPLPluginManager.getAllPlugins();
            String[] plugNames = new String[plugins.size()];
            int i = 0;
            for (PluginInfo curPlug : plugins) {
                plugNames[i] = curPlug.pluginType() + ":" + curPlug.toString();
                i++;
            }

            localGenericDialog.addChoice("plugin", plugNames, plugNames[0]);
            localGenericDialog.showDialog();
            if (localGenericDialog.wasCanceled())
                return;
            int n = localGenericDialog.getNextChoiceIndex();
            IJ.log(plugNames[n] + " has been selected!");


        }

	}


}
