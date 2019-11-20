package tipl.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.*;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test the storage backends of TIPL to ensure they handle images properly
 * Created by mader on 12/1/14.
 */
@RunWith(value = Parameterized.class)
public class StorageTests {

    final protected TIPLStorageManager.StorageInfo storageInfo;
    final ITIPLStorage istore;

    public StorageTests(final TIPLStorageManager.StorageInfo storageInfo) {
        System.out.println("Using Plugin:" + storageInfo);
        this.storageInfo = storageInfo;
        this.istore=TIPLStorageManager.getStorage(storageInfo);
    }

    @Parameterized.Parameters
    public static Collection<TIPLStorageManager.StorageInfo[]> getPlugins() {
        List<TIPLStorageManager.StorageInfo> possibleClasses = TIPLStorageManager.getAllStorage();
        return TIPLTestingLibrary.wrapCollection(possibleClasses);
    }

    public static final TypedPath tifPath=DirectoryIOTest.tifPath;


    /**
     * test to see if the standard Storage can read a directory
     * */
    @Test
    public void testReadFolder() {
        System.out.println(storageInfo+": Testing ReadFolder : tif="+tifPath);

        TImg curImg= istore.readTImg(tifPath);
        char[] alles=istore.makeTImgFullReadable(curImg).getByteAim();
        System.out.println(alles+" is loaded");
        assertEquals(alles.length, 1024 * 1024 * 8);

    }

    @Test
    public void allocateImage() {
        System.out.println(storageInfo+": Allocating a big image");
        istore.allocateTImg(new D3int(1000,1000,1000), TImgTools.IMAGETYPE_BOOL);

    }
    @Test
    public void savesSettings() {
        System.out.println(storageInfo+": Saving Settings");
        final int oldCL = istore.getCacheLevel();
        final boolean oldSW = istore.getUseScratch();
        final TypedPath oldSWPath = istore.getScratchDirectory();
        istore.setCacheLevel(TImgTools.SPEED_MEMORY_CALCULATE);
        System.out.println(storageInfo+": CacheLevel");
        assertEquals(TImgTools.SPEED_MEMORY_CALCULATE,istore.getCacheLevel());
        istore.setCacheLevel(oldCL);

        System.out.println(storageInfo+": Scratch");
        istore.setUseScratch(true);
        assertEquals(true,istore.getUseScratch());
        istore.setUseScratch(false);
        assertEquals(false,istore.getUseScratch());
        istore.setUseScratch(oldSW);

        istore.setScratchDirectory(TIPLStorageManager.createVirtualPath("junk"));
        assertEquals(TIPLStorageManager.createVirtualPath("junk").getPath(),istore.getScratchDirectory().getPath
                ());
        istore.setScratchDirectory(oldSWPath);
    }

    final static TImgRO bwPointImg = TestPosFunctions.wrapItAs(10,
            new TestPosFunctions.SinglePointFunction(5, 5, 5), TImgTools.IMAGETYPE_BOOL);
    @Test
    public void makeFullImage() {
        System.out.println(storageInfo+": Making a full image");
        int[] pointArray = istore.makeTImgFullReadable(bwPointImg).getIntAim();
        assertEquals((int) bwPointImg.getDim().prod(),pointArray.length);
        boolean[] bwPointArray = istore.makeTImgFullReadable(bwPointImg).getBoolAim();
        assertEquals((int) bwPointImg.getDim().prod(),bwPointArray.length);
    }



}
