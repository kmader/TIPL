package tipl.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import tipl.formats.TImgRO;
import tipl.tools.BaseTIPLPluginIn;
import tipl.tools.ComponentLabel;
import tipl.util.D3float;
import tipl.util.ITIPLPluginIO;
import tipl.util.ITIPLPluginIn;
import tipl.util.TIPLGlobal;
import tipl.util.TIPLPluginManager;
import tipl.util.TImgTools;
import tipl.util.TIPLPluginManager.PluginInfo;

/**
 * Test the component labeling class using a synthetically created image
 * 
 * @author mader
 * 
 */
@RunWith(value=Parameterized.class)
public class CurvatureTests {
	@Parameters
	public static Collection<PluginInfo[]> getPlugins() {
		List<PluginInfo> possibleClasses = TIPLPluginManager.getPluginsNamed("Curvature");
		return TIPLTestingLibrary.wrapCollection(possibleClasses);
	}
	
	protected ITIPLPluginIO makeCurvature(final TImgRO sImg) {
		final ITIPLPluginIO cv = TIPLPluginManager.getPluginIO(pluginId);
		cv.LoadImages(new TImgRO[] { sImg });
		return cv;
	}
	
	final protected PluginInfo pluginId;
	public CurvatureTests(final PluginInfo pluginToUse) {
		TIPLGlobal.setDebug(TIPLGlobal.DEBUG_ALL);
		System.out.println("Using Plugin:"+pluginToUse);
		pluginId=pluginToUse;
	}
	@Test
	public void testCurvature() {
		System.out.println("Testing Curvature volume limits");
		final TImgRO testImg = TestPosFunctions.wrapIt(8,
				new TestPosFunctions.DiagonalPlaneAndDotsFunction());
		ITIPLPluginIO cv = makeCurvature(testImg);
		cv.execute();
	}
	
	@Test
	public void testDivideWork() {
		System.out.println("Testing that work is divided into chunks between 1 and the last slice");
		for (int coreCount=TIPLGlobal.availableCores-1;coreCount<2*TIPLGlobal.availableCores;coreCount+=2) {
			final TImgRO testImg = TestPosFunctions.wrapIt(coreCount,
					new TestPosFunctions.DiagonalPlaneAndDotsFunction());
			ITIPLPluginIO cv = makeCurvature(testImg);
			TIPLTestingLibrary.testDivideWork(((BaseTIPLPluginIn) cv),1, testImg.getDim().z-1,true);
		}
		
		
	}

	/**
	 * Test method for {@link tipl.tools.ComponentLabel#execute()}.
	 */
	//@Test
	public void testExecute() {
		System.out.println("Testing execute");
		final TImgRO testImg = TestPosFunctions.wrapIt(50,
				new TestPosFunctions.SheetImageFunction());
		final ITIPLPluginIn CL = makeCurvature(testImg);
		CL.execute();
	}


}
