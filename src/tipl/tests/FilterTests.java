/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import tipl.formats.TImgRO;
import tipl.tools.FilterScale;
import tipl.util.D3int;
import tipl.util.ITIPLPluginIO;
import tipl.util.TIPLPluginManager;
import tipl.util.TIPLPluginManager.PluginInfo;

/**
 * @author mader
 *
 */
@RunWith(value=Parameterized.class)
public class FilterTests {
	@Parameters
	public static Collection<PluginInfo[]> getPlugins() {
		List<PluginInfo> possibleClasses = TIPLPluginManager.getPluginsNamed("Filter");
		return TIPLTestingLibrary.wrapCollection(possibleClasses);
	}
	
	protected static ITIPLPluginIO makeFilter(final PluginInfo idPlugin,final TImgRO inImage) {
		final ITIPLPluginIO FL = TIPLPluginManager.getPluginIO(idPlugin);
		FL.LoadImages(new TImgRO[] { inImage });
		return FL;
	}
	
	final protected PluginInfo pluginId;
	public FilterTests(final PluginInfo pluginToUse) {
		System.out.println("Using Plugin:"+pluginToUse);
		pluginId=pluginToUse;
	}
	
	/**
	 * Test dimensions of output image
	 */
	@Test
	public void testOutDim() {
		// offset lines
		final TImgRO testImg = TestPosFunctions.wrapItAs(10,
				new TestPosFunctions.LinesFunction(),3);

		ITIPLPluginIO RS = makeFilter(pluginId,testImg);
		RS.setParameter("-upfactor=1,1,1 -downfactor=2,2,2");
		System.out.println("Input Image Type"+testImg.getImageType());
		
		RS.execute();
		TImgRO outImg = RS.ExportImages(testImg)[0];
		
		TIPLTestingLibrary.checkDimensions(outImg, new D3int(5, 5, 5), new D3int(0, 0, 0));
		
		// second image
		RS = makeFilter(pluginId,testImg);
		RS.setParameter("-upfactor=1,1,1 -downfactor=1,2,3 -filter="+FilterScale.GAUSSIAN);
		System.out.println("Input Image Type"+testImg.getImageType());
		
		RS.execute();
		outImg = RS.ExportImages(testImg)[0];
		
		TIPLTestingLibrary.checkDimensions(outImg, new D3int(10, 5, 3), new D3int(0, 0, 0));
	}
	/**
	 * Not really related to the standard filter test, but a good verification of the single point functions
	 */
	@Test 
	public void testPosFunctions() {
		final TImgRO pointImage = TestPosFunctions.wrapItAs(10,
				new TestPosFunctions.SinglePointFunction(5, 5, 5),3);
		// make sure the single point function is ok
		TIPLTestingLibrary.doPointsMatch(pointImage, 5, 5, 5, 1f, 0.01f);
		
		final TImgRO progImage = TestPosFunctions.wrapItAs(10,
				new TestPosFunctions.ProgZImage(),3);
		
		TIPLTestingLibrary.doPointsMatch(progImage, 5, 5, 5, 5f, 0.01f);
	}
	/**
	 * Test the gaussian filter on a single point
	 */
	@Test
	public void testOutGaussian() {
		// offset lines
		final TImgRO pointImage = TestPosFunctions.wrapItAs(10,
				new TestPosFunctions.SinglePointFunction(5, 5, 5),3);
		
		ITIPLPluginIO RS = makeFilter(pluginId,pointImage);
		RS.setParameter("-upfactor=2,2,2 -downfactor=2,2,2 -filter="+FilterScale.GAUSSIAN+" -filtersetting=0.5");
		RS.execute();
		TImgRO outImg = RS.ExportImages(pointImage)[0];
		TIPLTestingLibrary.doPointsMatch(outImg, 5, 5, 5, .89f, 0.01f);
		TIPLTestingLibrary.doPointsMatch(outImg, 5, 5, 4, .016f, 0.01f);
		TIPLTestingLibrary.doPointsMatch(outImg, 4, 4, 4, 5e-6f, 1e-5f);
		
		// now change sigma and ensure it still works
		RS = makeFilter(pluginId,pointImage);
		RS.setParameter("-upfactor=2,2,2 -downfactor=2,2,2 -filter="+FilterScale.GAUSSIAN+" -filtersetting=1");
		RS.execute();
		outImg = RS.ExportImages(pointImage)[0];
		TIPLTestingLibrary.doPointsMatch(outImg, 5, 5, 5, .18f, 0.01f);
		TIPLTestingLibrary.doPointsMatch(outImg, 5, 5, 4, .06f, 0.01f);
		
		// now change the dimensions and see if it still works
		final TImgRO pointImage2 = TestPosFunctions.wrapItAs(10,
				new TestPosFunctions.SinglePointFunction(6, 6, 6),3);
		RS = makeFilter(pluginId,pointImage2);
		RS.setParameter("-upfactor=1,1,1 -downfactor=2,2,2 -filter="+FilterScale.GAUSSIAN+" -filtersetting=0.5");
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
				new TestPosFunctions.ProgZImage(),3);
		
		ITIPLPluginIO RS = makeFilter(pluginId,gradImage);
		RS.setParameter("-upfactor=2,2,2 -downfactor=2,2,2 -filter="+FilterScale.LAPLACE);
		RS.execute();
		TImgRO outImg = RS.ExportImages(gradImage)[0];
		TIPLTestingLibrary.doPointsMatch(outImg, 5, 5, 5, 0.33f, 0.01f);
		
		// now change sigma and ensure it still works
		RS = makeFilter(pluginId,gradImage);
		RS.setParameter("-upfactor=1,1,1 -downfactor=2,2,2 -filter="+FilterScale.LAPLACE);
		RS.execute();
		outImg = RS.ExportImages(gradImage)[0];
		TIPLTestingLibrary.doPointsMatch(outImg, 2, 2, 2, 0.33f, 0.01f);
		
	}
	

}
