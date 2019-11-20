package tipl.tests;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import tipl.formats.*;
import tipl.util.ITIPLStorage;
import tipl.util.TIPLPluginManager;
import tipl.util.TImgTools;
import tipl.util.TypedPath;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by mader on 12/1/14.
 */
@RunWith(value = Parameterized.class)
public class TIFFSliceTest {
    final static TImgRO bwPointImg = TestPosFunctions.wrapItAs(10,
            new TestPosFunctions.SinglePointFunction(5, 5, 5), TImgTools.IMAGETYPE_BOOL);
    final static TypedPath basePath = TIPLTestingLibrary.createTestFolder("io_tests");
    @Parameterized.Parameters
    public static Collection<Integer[]> getPlugins() {
        List<Integer> possibleTypes = new LinkedList<Integer>();
        possibleTypes.add(TImgTools.IMAGETYPE_BOOL);
        possibleTypes.add(TImgTools.IMAGETYPE_CHAR);
        possibleTypes.add(TImgTools.IMAGETYPE_SHORT);
        possibleTypes.add(TImgTools.IMAGETYPE_INT);
        possibleTypes.add(TImgTools.IMAGETYPE_FLOAT);
        possibleTypes.add(TImgTools.IMAGETYPE_LONG);
        possibleTypes.add(TImgTools.IMAGETYPE_DOUBLE);
        return TIPLTestingLibrary.wrapCollection(possibleTypes);
    }

    ITIPLStorage imgSto=null;


    final int imageTypeToUse;
    public TIFFSliceTest(final int imageTypeToUse) {
        System.out.println("Using Type:" + imageTypeToUse+": "+TImgTools.getImageTypeName(imageTypeToUse));
        this.imageTypeToUse = imageTypeToUse;
    }

    final TImgRO getGradientImage() {
        return TestPosFunctions.wrapItAs(10,
                new TestPosFunctions.ProgZImage(),imageTypeToUse);
    }



    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        imgSto = TImgTools.getStorage();
        TIPLTestingLibrary.cleanup=true;
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {

    }

    /**
     * Test a simple point image
     */
    @Test
    public void testBasicIO() {
        testIO(bwPointImg,basePath.append("test_image_"+this.imageTypeToUse+".tif"),this
                .imageTypeToUse);
    }

    /**
     * Test a simple point image
     */
    @Test
    public void testTypedIO() {
        testIO(getGradientImage(),basePath.append("test_image_"+this.imageTypeToUse+".tif"),this
                .imageTypeToUse);
    }

    /**
     * Test a simple point image
     */
    @Test
    public void testTypedIOWithVA() {
        testOutWithVA(getGradientImage(),basePath.append("test_image_"+this.imageTypeToUse+".tif"),
                this
                .imageTypeToUse);
    }



    public void testIO(TImgRO inImage, TypedPath path, int imgType) {
        TSliceWriter tsw = TSliceWriter.Writers.ChooseBest(inImage,path,imgType);
        tsw.SetupWriter(inImage,path,imgType);
        tsw.WriteHeader();
        System.out.println("Writing folder:"+path+" as "+tsw);
        TSliceWriter.Writers.SimpleWrite(tsw,inImage,imgType);


        DirectoryReader dr = DirectoryReader.ChooseBest(path);
        System.out.println("Reading folder:"+path+" as "+dr);
        dr.SetupReader(path);
        final TImgRO outImage = dr.getImage();

        TIPLTestingLibrary.doImagesMatch(inImage,outImage);
        for(int sliceNo=0;sliceNo<inImage.getDim().z;sliceNo++) {
            TIPLTestingLibrary.doSlicesMatchB(inImage, sliceNo, outImage, sliceNo);
        }

    }

    public void testOutWithVA(TImgRO inImage, TypedPath path, int imgType) {
        TSliceWriter tsw = TSliceWriter.Writers.ChooseBest(inImage,path,imgType);
        tsw.SetupWriter(inImage,path,imgType);
        tsw.WriteHeader();
        System.out.println("Writing folder:"+path+" as "+tsw);
        TSliceWriter.Writers.SimpleWrite(tsw,inImage,imgType);


        VirtualAim dr = new VirtualAim(path);
        System.out.println("Reading folder:"+path+" as "+dr);

        TIPLTestingLibrary.doImagesMatch(inImage,dr);
        for(int sliceNo=0;sliceNo<inImage.getDim().z;sliceNo++) {
            TIPLTestingLibrary.doSlicesMatchB(inImage, sliceNo, dr, sliceNo);
        }

    }
}
