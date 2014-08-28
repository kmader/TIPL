package tipl.tests;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import tipl.formats.TImgRO;
import tipl.spark.SparkGlobal;
import tipl.util.*;
import tipl.util.TIPLPluginManager.PluginInfo;

import java.util.Collection;
import java.util.List;

/**
 * Test the shape analysis on a synthetic label image
 *
 * @author mader
 */
@RunWith(value = Parameterized.class)
public class KVoronoiTest {
    final protected PluginInfo pluginId;
    final int imageSize=10;//50;
    final TestPosFunctions bgLayers = new TestPosFunctions.LayeredImage(1, 2, 25, 0, 0);
    final TestPosFunctions densePart = new TestPosFunctions.EllipsoidFunction(75, 75, 75,
            10, 10, 10);
    final TImgRO layeredImage = TestPosFunctions
            .wrapIt(3*imageSize, new TestPosFunctions.BGPlusPhase(bgLayers, densePart, 3));
    final TImgRO diagonalPlane = TestPosFunctions.wrapItAs(imageSize,
            new TestPosFunctions.DiagonalPlaneAndDotsFunction(), TImgTools.IMAGETYPE_INT);

    public KVoronoiTest(final PluginInfo pluginToUse) {
        System.out.println("Using Plugin:" + pluginToUse);
        pluginId = pluginToUse;
    }

    @Parameters
    public static Collection<PluginInfo[]> getPlugins() {
        List<PluginInfo> possibleClasses = TIPLPluginManager.getPluginsNamed("kVoronoi");
        return TIPLTestingLibrary.wrapCollection(possibleClasses);
    }

    protected static ITIPLPluginIn makeSA(PluginInfo plugName, final TImgRO sImg) {

        final ITIPLPluginIn SA = (ITIPLPluginIO) TIPLPluginManager.getPlugin(plugName);
        SA.LoadImages(new TImgRO[]{sImg});
        return SA;
    }

    /**
     * the function to run the test on an image
     */
    public static ITIPLPluginIn testImage(String outName, PluginInfo pluginId, TImgRO inputImage) {
        System.out.println("Testing Component volume limits");

        ITIPLPluginIn SA = makeSA(pluginId, inputImage);
        //SA.setParameter("-csvname="+outName);
        SA.execute();
        return SA;

    }

    @Before
    public void setUp() {
        SparkGlobal.getContext(this.getClass().getName());
        TIPLGlobal.setDebug(TIPLGlobal.DEBUG_OFF);

    }

    @Test
    public void testDiagonalImage() {
        ITIPLPluginIn SA = testImage("diag_" + pluginId.sparkBased() + ".csv", pluginId, diagonalPlane);
        //assertEquals(1L,SA.getInfo("groups"));
        //assertEquals(63775.0,SA.getInfo("average_volume"));
    }

    @Test
    public void testLayered() {
        ITIPLPluginIn SA = testImage("layered_" + pluginId.sparkBased() + ".csv", pluginId, layeredImage);
        //assertEquals(3L,SA.getInfo("groups"));
        //assertEquals(1125000.0,SA.getInfo("average_volume"));
    }


}
