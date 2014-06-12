package tipl.tests;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import tipl.formats.TImgRO;
import tipl.tools.ComponentLabel;
import tipl.util.D3float;
import tipl.util.ITIPLPluginIO;
import tipl.util.ITIPLPluginIn;
import tipl.util.TIPLPluginManager;
import tipl.util.TImgTools;
import tipl.util.TIPLPluginManager.PluginInfo;

/**
 * Test the shape analysis on a synthetically created label image
 * 
 * @author mader
 * 
 */
@RunWith(value=Parameterized.class)
public class ShapeAnalysisTest {
	@Parameters
	public static Collection<PluginInfo[]> getPlugins() {
		List<PluginInfo> possibleClasses = TIPLPluginManager.getPluginsNamed("ShapeAnalysis");
		return TIPLTestingLibrary.wrapCollection(possibleClasses);
	}
	
	protected ITIPLPluginIn makeSA(final TImgRO sImg) {
		
		final ITIPLPluginIn SA =(ITIPLPluginIn) TIPLPluginManager.getPlugin(pluginId);
		SA.LoadImages(new TImgRO[] { sImg });
		return SA;
	}
	final protected PluginInfo pluginId;
	
	public ShapeAnalysisTest(final PluginInfo pluginToUse) {
		System.out.println("Using Plugin:"+pluginToUse);
		pluginId=pluginToUse;
	}

	@Test
	public void testSimpleImage() {
		System.out.println("Testing Component volume limits");
		final TImgRO testImg = TestPosFunctions.wrapItAs(10,
				new TestPosFunctions.DiagonalPlaneAndDotsFunction(),TImgTools.IMAGETYPE_INT);
		
		ITIPLPluginIn SA =  makeSA(testImg);
		SA.setParameter("-csvname="+pluginId.sparkBased()+"_testing.csv");
		SA.execute();
		
		
	}

	

}
