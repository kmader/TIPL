/**
 *
 */
package tipl.util;

import org.scijava.annotations.Index;
import org.scijava.annotations.IndexItem;
import org.scijava.annotations.Indexable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import java.util.Map.Entry;
import tipl.util.ITIPLFileSystem.FileSystemInfo;

/**
 * A manager for the storage options to keep them separate from the core implementation
 * @author mader
 */
public class TIPLStorageManager {
    private static final Map<StorageInfo, TIPLStorageFactory> storageList = new HashMap<StorageInfo, TIPLStorageFactory>() {
        @Override
        public TIPLStorageFactory get(Object ikey) {
            if (super.containsKey(ikey)) return super.get(ikey);
            final StorageInfo key = ((StorageInfo) ikey);
            final String cVal = key.toString();
            for (Entry<StorageInfo, TIPLStorageFactory> cKey : this.entrySet()) {
                if (cKey.getKey().toString().equalsIgnoreCase(cVal)) return cKey.getValue();
            }
            throw new IllegalArgumentException("storage:" + key.storageType() + ":\t" + cVal + " cannot be found in the storage tree");
        }
    };
    /**
     * the default filesystem (if it is null, the first successful is used)
     */
    public static ITIPLFileSystem injectedFileSystem = null;

    /**
     * Go through all the available storage engines to find one capable of reading a path of this
     * type
     * @param pathStr
     * @return
     */
    public static TypedPath openPath(final String pathStr) {
        if (injectedFileSystem!=null) {
            if(injectedFileSystem.isValidPath(pathStr)) return injectedFileSystem.openPath(pathStr);
        }

        for(FileSystemInfo fsi : getAllFileSystem()) {
            if (getFileSystem(fsi).isValidPath(pathStr)) return getFileSystem(fsi).
                    openPath(pathStr);
        }

        throw new IllegalArgumentException("No suitable filesystem environment found:"+pathStr);
    }

    /**
     * For files or datasets not based directly on real files (transforms of images)
     * @note The construction of File or Stream objects from these objects will eventually throw an error
     * @param virtualName
     * @return
     */
    public static TypedPath createVirtualPath(final String virtualName) {
        return openPath("virtual://"+virtualName);
    }

    protected static TypedPath isPathValid(final ITIPLFileSystem cStorage,final String pathStr) {
        try {
            TypedPath outPath = cStorage.openPath(pathStr);
            if (outPath.exists()) return outPath;
            else return null;
        } catch (Exception e) {
            System.err.println(pathStr+" cannot be located within "+cStorage+" ->" + e);
            return null;
        }
    }


    /**
     * get the named storage from the list
     * @param curInfo the information on the storage
     * @return
     */
    public static ITIPLStorage getStorage(StorageInfo curInfo) {
        System.out.println("Requesting:" + curInfo.toString() + "\t" + storageList.
                containsKey(curInfo));

        if (getAllStorage().contains(curInfo)) {
            return storageList.get(curInfo).get();
        } else
            throw new IllegalArgumentException("storage:" + curInfo.storageType() + " with info" + curInfo + " has not yet been loaded");
    }


    public static ITIPLStorage getFirstStorage(boolean withSpark) {
       return getStorage(getStorages(withSpark).get(0));
    }



    /**
     * Get a list of all the storage factories that exist
     * @return
     * @throws InstantiationException
     */
    public static List<StorageInfo> getAllStorage() {
        if (storageList.size() > 1) return new ArrayList<StorageInfo>(storageList.keySet());
        for (Iterator<IndexItem<StorageInfo>> cIter = Index.load(StorageInfo.class).iterator(); cIter.hasNext(); ) {
            final IndexItem<StorageInfo> item = cIter.next();

            final StorageInfo bName = item.annotation();

            try {

                final TIPLStorageFactory dBlock = (TIPLStorageFactory) Class.forName(item.className()).newInstance();
                System.out.println(bName + " loaded as: " + dBlock);
                if (bName.enabled()) storageList.put(bName, dBlock);
            } catch (InstantiationException e) {
                System.err.println("storage: " + bName.storageType() + " " + bName.desc() + " could not be loaded or instantiated by storage manager!\t" + e);
                if (TIPLGlobal.getDebug()) e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.err.println("storage: " + bName.storageType() + " " + bName.desc() + " could not be found by storage manager!\t" + e);
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                System.err.println("storage: " + bName.storageType() + " " + bName.desc() + " was accessed illegally by storage manager!\t" + e);
                e.printStackTrace();
            }
        }

        return new ArrayList<StorageInfo>(storageList.keySet());
    }

    /**
     * A list of storages with the given type/name
     * @param storageType
     * @return a hashmap of the storages
     * @throws InstantiationException
     */
    public static List<StorageInfo> getStoragesNamed(final String storageType) {
        List<StorageInfo> outList = new ArrayList<StorageInfo>();
        for (StorageInfo cPlug : getAllStorage()) {
            if (cPlug.storageType().equalsIgnoreCase(storageType))
                outList.add(cPlug);
        }
        return outList;
    }
    public static List<StorageInfo> getStorages(final boolean withSpark) {
        List<StorageInfo> outList = new ArrayList<StorageInfo>();
        for (StorageInfo cPlug : getAllStorage()) {
            if (cPlug.sparkBased()==withSpark)
                outList.add(cPlug);
        }
        return outList;
    }



    /**
     * StorageINfo stores information about the different storage options
     *
     * @author mader
     *
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @Indexable
    public static @interface StorageInfo {
        /**
         * The name of the storage (VfilterScale is Filter so is FilterScale)
         * @return
         */
        String storageType();

        /**
         * short description of the storage
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
         * the speed rank of the storage from 0 slowest to 10 average to 20 highest (everything else being equal the fastest storage is taken)
         * @return
         */
        int speedRank() default 10;

        /**
         * does the storage require Spark in order to run
         * @return
         */
        boolean sparkBased() default false;

        /**
         * is the storage type enabled in production
         */
        boolean enabled() default true;
    }


    /**
     * The static method to create a new TIPLstorage
     * @author mader
     *
     */
    public static interface TIPLStorageFactory {
        public ITIPLStorage get();
    }



    private static final Map<ITIPLFileSystem.FileSystemInfo, ITIPLFileSystem> fsList = new HashMap<FileSystemInfo,
            ITIPLFileSystem>() {
        @Override
        public ITIPLFileSystem get(Object ikey) {
            if (super.containsKey(ikey)) return super.get(ikey);

            final FileSystemInfo key = ((FileSystemInfo) ikey);
            final String cVal = key.toString();
            for (Entry<FileSystemInfo, ITIPLFileSystem> cKey : this.entrySet()) {
                if (cKey.getKey().toString().equalsIgnoreCase(cVal)) return cKey.getValue();
            }
            throw new IllegalArgumentException("storage:" + key.name() + ":\t" + cVal + " cannot be " +
                    "found in the storage tree");
        }
    };



    /**
     * Get a list of all the storage factories that exist
     * @return
     * @throws InstantiationException
     */
    public static List<FileSystemInfo> getAllFileSystem() {
        if (fsList.size() > 1) return new ArrayList<FileSystemInfo>(fsList.keySet());

        for (Iterator<IndexItem<FileSystemInfo>> cIter = Index.load(FileSystemInfo.class).
                iterator(); cIter.hasNext(); ) {
            final IndexItem<FileSystemInfo> item = cIter.next();

            final FileSystemInfo bName = item.annotation();

            try {

                final ITIPLFileSystem dBlock = (ITIPLFileSystem) Class.forName(item.className()).newInstance();
                System.out.println(bName + " loaded as: " + dBlock);
                if (bName.enabled()) fsList.put(bName, dBlock);
            } catch (InstantiationException e) {
                System.err.println("filesystem: " + bName.name() + " " + bName.desc() + " could " +
                        "not " +
                        "be loaded or instantiated by storage manager!\t" + e);
                if (TIPLGlobal.getDebug()) e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.err.println("filesystem: " + bName.name() + " " + bName.desc() + " could not be found by storage manager!\t" + e);
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                System.err.println("filesystem: " + bName.name() + " " + bName.desc() + " was accessed illegally by storage manager!\t" + e);
                e.printStackTrace();
            }
        }

        return new ArrayList<ITIPLFileSystem.FileSystemInfo>(fsList.keySet());
    }


    /**
     * get the named storage from the list
     * @param curInfo the information on the storage
     * @return
     */
    public static ITIPLFileSystem getFileSystem(ITIPLFileSystem.FileSystemInfo curInfo) {
        System.out.println("Requesting:" + curInfo.toString() + "\t" + fsList.containsKey(curInfo));

        if (getAllFileSystem().contains(curInfo)) {
            return fsList.get(curInfo);
        } else
            throw new IllegalArgumentException("storage:" + curInfo.name() + " with info" + curInfo
                    + " has not yet been loaded");
    }



}
