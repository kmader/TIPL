/**
 *
 */
package tipl.tests;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertSame;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import tipl.formats.TImgRO;
import tipl.settings.FilterSettings;
import tipl.tools.BaseTIPLPluginIn;
import tipl.tools.FilterScale;
import tipl.util.D3int;
import tipl.util.ITIPLPluginIO;
import tipl.util.TIPLGlobal;
import tipl.util.TIPLPluginManager;
import tipl.util.TIPLPluginManager.PluginInfo;
import tipl.util.TImgTools;

/**
 * @author mader
 */
@RunWith(value = Parameterized.class)
public class FilterTests {
    static protected final boolean saveImages = false;

    @Parameters
    public static Collection<PluginInfo[]> getPlugins() {
        List<PluginInfo> possibleClasses = TIPLPluginManager.getPluginsNamed("Filter");
        return TIPLTestingLibrary.wrapCollection(possibleClasses);
    }

    protected static ITIPLPluginIO makeFilter(final PluginInfo idPlugin, final TImgRO inImage) {
        final ITIPLPluginIO FL = TIPLPluginManager.getPluginIO(idPlugin);
        FL.LoadImages(new TImgRO[]{inImage});
        return FL;
    }

    final protected PluginInfo pluginId;

    public FilterTests(final PluginInfo pluginToUse) {
        System.out.println("Using Plugin:" + pluginToUse);
        pluginId = pluginToUse;
    }

    //@Test
    public void testDivideWorkBig() {
        System.out.println("Testing that work is divided into chunks between 1 and the last slice");
        final TImgRO testImg = TestPosFunctions.wrapIt(5 * TIPLGlobal.availableCores,
                new TestPosFunctions.DiagonalPlaneAndDotsFunction());
        ITIPLPluginIO cv = makeFilter(pluginId, testImg);
        cv.setParameter("-upfactor=1,1,1 -downfactor=1,1,1 -filter=" + FilterSettings.GAUSSIAN);
        TIPLTestingLibrary.testDivideWork(((BaseTIPLPluginIn) cv), 0, testImg.getDim().z, true);

    }

    //@Test
    public void testDivideWork() {
        System.out.println("Testing that work is divided into chunks between 1 and the last slice");
        for (int coreCount = TIPLGlobal.availableCores - 1; coreCount < 2 * TIPLGlobal
                .availableCores; coreCount += 2) {
            final TImgRO testImg = TestPosFunctions.wrapIt(coreCount,
                    new TestPosFunctions.DiagonalPlaneAndDotsFunction());
            ITIPLPluginIO cv = makeFilter(pluginId, testImg);
            cv.setParameter("-upfactor=1,1,1 -downfactor=1,1,1 -filter=" + FilterSettings.GAUSSIAN);
            TIPLTestingLibrary.testDivideWork(((BaseTIPLPluginIn) cv), 0, testImg.getDim().z, true);
        }

    }

    /**
     * Test dimensions of output image
     */
    @Test
    public void testOutDim() {
        // offset lines
        final TImgRO testImg = TestPosFunctions.wrapItAs(10,
                new TestPosFunctions.LinesFunction(), TImgTools.IMAGETYPE_FLOAT);

        ITIPLPluginIO RS = makeFilter(pluginId, testImg);
        RS.setParameter("-upfactor=1,1,1 -downfactor=2,2,2");
        System.out.println("Input Image Type" + testImg.getImageType());

        RS.execute();
        TImgRO outImg = RS.ExportImages(testImg)[0];

        TIPLTestingLibrary.checkDimensions(outImg, new D3int(5, 5, 5), new D3int(0, 0, 0));

        // second image
        RS = makeFilter(pluginId, testImg);
        RS.setParameter("-upfactor=1,1,1 -downfactor=1,2,5 -filter=" + FilterSettings.GAUSSIAN);
        System.out.println("Input Image Type" + testImg.getImageType());

        RS.execute();
        outImg = RS.ExportImages(testImg)[0];

        TIPLTestingLibrary.checkDimensions(outImg, new D3int(10, 5, 2), new D3int(0, 0, 0));
    }

    /**
     * Test multiple filter iterations
     */
    @Test
    public void testOutMultiple() {
        // offset lines
        final TImgRO testImg = TestPosFunctions.wrapItAs(6,
                new TestPosFunctions.LinesFunction(), TImgTools.IMAGETYPE_FLOAT);

        ITIPLPluginIO RS = makeFilter(pluginId, testImg);
        RS.setParameter("-upfactor=1,1,1 -downfactor=3,3,3");
        System.out.println("Input Image Type:" + testImg.getImageType());

        RS.execute();
        TImgRO outImg = RS.ExportImages(testImg)[0];
        TIPLTestingLibrary.checkDim(outImg, new D3int(2, 2, 2));

        // second image
        RS = makeFilter(pluginId, outImg);
        RS.setParameter("-upfactor=2,2,2 -downfactor=1,1,1 -filter=" + FilterSettings.GAUSSIAN);
        System.out.println("Input Image Type:" + testImg.getImageType());

        RS.execute();
        TImgRO outImg2 = RS.ExportImages(testImg)[0];

        TIPLTestingLibrary.checkDim(outImg2, new D3int(4, 4, 4));

        // third image
        RS = makeFilter(pluginId, outImg2);
        RS.setParameter("-upfactor=3,3,3 -downfactor=2,2,2 -filter=" + FilterSettings.GAUSSIAN);
        System.out.println("Input Image Type:" + testImg.getImageType());
        RS.execute();
        TImgRO outImg3 = RS.ExportImages(testImg)[0];

        TIPLTestingLibrary.checkDim(outImg3, new D3int(6, 6, 6));
    }

    @Test
    public void hasSettings() {
        final TImgRO testImg = TestPosFunctions.wrapItAs(10,
                new TestPosFunctions.LinesFunction(), TImgTools.IMAGETYPE_FLOAT);

        ITIPLPluginIO fs = makeFilter(pluginId, testImg);
        try {
            FilterSettings.HasFilterSettings hfs = (FilterSettings.HasFilterSettings) fs;
            System.out.println(hfs.getFilterSettings());
        } catch (Exception e) {
            fail("Does not implement has filter settings interface correctly" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Not really related to the standard filter test, but a good verification of the single point
     * functions
     */
    @Test
    public void testPosFunctions() {
        final TImgRO pointImage = TestPosFunctions.wrapItAs(10,
                new TestPosFunctions.SinglePointFunction(5, 5, 5), TImgTools.IMAGETYPE_FLOAT);
        // make sure the single point function is ok
        TIPLTestingLibrary.doPointsMatch(pointImage, 5, 5, 5, 1f, 0.01f);

        final TImgRO progImage = TestPosFunctions.wrapItAs(10,
                new TestPosFunctions.ProgZImage(), 3);

        TIPLTestingLibrary.doPointsMatch(progImage, 5, 5, 5, 5f, 0.01f);

        // offset lines
        int boxSize = 10;
        int layerWidth = 3;
        final TImgRO sheetImage = TestPosFunctions.wrapItAs(boxSize,
                new TestPosFunctions.SphericalLayeredImage(boxSize / 2, boxSize / 2, boxSize / 2,
                        0, 1, layerWidth), TImgTools.IMAGETYPE_FLOAT);

    }

    /**
     *
     */
    @Test
    public void testOutSheetGaussian() {
        // offset lines
        int boxSize = 10;
        int layerWidth = 3;
        final TImgRO sheetImage = TestPosFunctions.wrapItAs(boxSize,
                new TestPosFunctions.SphericalLayeredImage(boxSize / 2, boxSize / 2, boxSize / 2,
                        0, 1, layerWidth), 3);

        ITIPLPluginIO RS = makeFilter(pluginId, sheetImage);
        RS.setParameter("-upfactor=2,2,2 -downfactor=2,2,2 -filter=" + FilterSettings.GAUSSIAN +
                " -filtersetting=1.0");
        RS.execute();

        TImgRO outImg = RS.ExportImages(sheetImage)[0];

        TIPLTestingLibrary.doSliceMeanValueMatch(outImg, 5, .411, 0.01);

        TIPLTestingLibrary.doPointsMatch(outImg, 3, 3, 3, .11f, 0.01f);
        TIPLTestingLibrary.doPointsMatch(outImg, 5, 5, 4, .66f, 0.01f);
        TIPLTestingLibrary.doPointsMatch(outImg, 4, 4, 4, .29f, 1e-3f);
    }

    /**
     * Test the gaussian filter on a single point
     */

    @Test
    public void testOutGaussian() {
        // offset lines
        final TImgRO pointImage = TestPosFunctions.wrapItAs(10,
                new TestPosFunctions.SinglePointFunction(5, 5, 5), TImgTools.IMAGETYPE_FLOAT);

        ITIPLPluginIO RS = makeFilter(pluginId, pointImage);
        RS.setParameter("-upfactor=2,2,2 -downfactor=2,2,2 -filter=" + FilterSettings.GAUSSIAN +
                " -filtersetting=1");
        RS.execute();

        TImgRO outImg = RS.ExportImages(pointImage)[0];

        TIPLTestingLibrary.doPointsMatch(outImg, 3, 3, 3, .01f, 0.01f);
        TIPLTestingLibrary.doPointsMatch(outImg, 5, 5, 4, .068f, 0.01f);
        TIPLTestingLibrary.doPointsMatch(outImg, 4, 4, 4, 5e-6f, 1e-5f);

        // now change sigma and ensure it still works
        RS = makeFilter(pluginId, pointImage);
        RS.setParameter("-upfactor=2,2,2 -downfactor=2,2,2 -filter=" + FilterSettings.GAUSSIAN +
                " -filtersetting=1");
        RS.execute();
        outImg = RS.ExportImages(pointImage)[0];

        TIPLTestingLibrary.doPointsMatch(outImg, 2, 2, 2, .0f, 0.01f);
        TIPLTestingLibrary.doPointsMatch(outImg, 5, 5, 5, .18f, 0.01f);

        // now change the dimensions and see if it still works
        final TImgRO pointImage2 = TestPosFunctions.wrapItAs(10,
                new TestPosFunctions.SinglePointFunction(6, 6, 6), TImgTools.IMAGETYPE_FLOAT);
        RS = makeFilter(pluginId, pointImage2);
        RS.setParameter("-upfactor=1,1,1 -downfactor=2,2,2 -filter=" + FilterSettings.GAUSSIAN +
                " -filtersetting=0.5");
        RS.execute();
        outImg = RS.ExportImages(pointImage)[0];

        TIPLTestingLibrary.doPointsMatch(outImg, 3, 3, 3, 0.89f, 0.01f);
    }

    /**
     * Test the laplace filter on a single point
     */
    @Test
    public void testOutLaplace() {
        // offset lines

        final TImgRO gradImage = TestPosFunctions.wrapItAs(10,
                new TestPosFunctions.ProgZImage(), TImgTools.IMAGETYPE_FLOAT);

        ITIPLPluginIO RS = makeFilter(pluginId, gradImage);
        RS.setParameter("-upfactor=2,2,2 -downfactor=2,2,2 -filter=" + FilterSettings.LAPLACE);
        RS.execute();
        TImgRO outImg = RS.ExportImages(gradImage)[0];
        TIPLTestingLibrary.doPointsMatch(outImg, 5, 5, 5, 0.33f, 0.01f);

        // now change sigma and ensure it still works
        RS = makeFilter(pluginId, gradImage);
        RS.setParameter("-upfactor=1,1,1 -downfactor=2,2,2 -filter=" + FilterSettings.LAPLACE);
        RS.execute();
        outImg = RS.ExportImages(gradImage)[0];
        TIPLTestingLibrary.doPointsMatch(outImg, 2, 2, 2, 0.33f, 0.01f);

    }

    /**
     * Test the gradient filter on a single point
     */
    @Test
    public void testOutGradient() {
        // offset lines

        final TImgRO gradImage = TestPosFunctions.wrapItAs(10,
                new TestPosFunctions.ProgZImage(), TImgTools.IMAGETYPE_FLOAT);

        ITIPLPluginIO RS = makeFilter(pluginId, gradImage);
        RS.setParameter("-upfactor=2,2,2 -downfactor=2,2,2 -filter=" + FilterSettings.GRADIENT);
        RS.execute();
        TImgRO outImg = RS.ExportImages(gradImage)[0];
        TIPLTestingLibrary.doPointsMatch(outImg, 5, 5, 4, 0.33f, 0.01f);

        // now change sigma and ensure it still works
        RS = makeFilter(pluginId, gradImage);
        RS.setParameter("-upfactor=1,1,1 -downfactor=2,2,2 -filter=" + FilterSettings.GRADIENT);
        RS.execute();
        outImg = RS.ExportImages(gradImage)[0];
        TIPLTestingLibrary.doPointsMatch(outImg, 5, 5, 5, 0.33f, 0.01f);

    }

    /**
     * A special function (should normally be turned off) to get the name of the plugin since
     * knowing [0] isn't useful in JUnit
     */
    @Test
    public void testName() {
        assertSame(pluginId.desc(), "No expectations!");
    }

}
