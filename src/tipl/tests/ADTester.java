/**
 * 
 */
package tipl.tests;


import static org.junit.Assert.assertEquals;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import tipl.blocks.AnalyzePhase;
import tipl.blocks.FilterBlock;
import tipl.blocks.FoamThresholdBlock;
import tipl.blocks.ITIPLBlock;
import tipl.blocks.BaseBlockRunner;
import tipl.blocks.ThicknessBlock;
import tipl.blocks.ThresholdBlock;
import tipl.blocks.XDFBlock;
import tipl.util.ArgumentDialog;
import tipl.util.ArgumentParser;
import tipl.util.D3float;
import tipl.util.TIPLGlobal;

/**
 * @author mader
 * 
 */
public class ADTester {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUp() throws Exception {
		// disable headlessness
		TIPLGlobal.activeParser("-@headless=false");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link tipl.util.ArgumentDialog#GUIBlock()}.
	 */
	@Test
	public void testGUIBlock() {
		ArgumentDialog.showDialogs=true;
		System.out.println(TIPLGlobal.activeParser(""));
		ITIPLBlock cBlock = new FilterBlock();
		ArgumentParser p = ArgumentDialog.GUIBlock(cBlock);
		p.checkForInvalid();
	}
	
	/**
	 * Test method for {@link tipl.util.ArgumentDialog#GUIBlock()}.
	 */
	@Test
	public void testGUIBlockWithFrames() {
		ImageJ ij = TIPLGlobal.getIJInstance();
		ImagePlus curImage = IJ.createImage("TestImge1", 100, 100, 100, 8);
		curImage.show();
		//WindowManager.addWindow(curImage.getWindow());
		testGUIBlock();
	}
	
	/**
	 * Test a multiblock runner
	 */
	@Test
	public void testGUIBlockRunner() {
		ArgumentDialog.showDialogs=true;
		
		BaseBlockRunner cr = new BaseBlockRunner();
		cr.add(new FilterBlock());
		cr.add(new ThresholdBlock());
		cr.add(new FoamThresholdBlock());
		cr.add(new ThicknessBlock());
		cr.add(new AnalyzePhase());
		
		ArgumentParser p = TIPLGlobal.activeParser("-gui");
		p = cr.setParameter(p);
		p.checkForInvalid();
	}

	/**
	 * Test method for
	 * {@link tipl.util.ArgumentParser#getOptionBoolean(java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public void testToDialog() {
		ArgumentDialog.showDialogs=true;
		final String[] strArr = new String[] { "-ted", "-bob=10",
				"-scale=1.0,1.0,1.0", "-tandy=0.1", "-david=joey" };
		final ArgumentParser p = TIPLGlobal.activeParser(strArr);
		p.getOptionChoiceString("name", "bob", "Choose the correct name", new String[] {"bob","dan","Kevin"});
		assertEquals(p.getOptionBoolean("ted", "Test-Help"), true);
		assertEquals(p.getOptionInt("bob", 10, "Test-Help", 10, 11), 10);
		final D3float fArgs = p
				.getOptionD3float("scale", new D3float(), "junk");
		assertEquals(fArgs.x, 1.0, .1);
		assertEquals(fArgs.y, 1.0, .1);
		assertEquals(fArgs.z, 1.0, .1);
		assertEquals(p.getOptionDouble("tandy", 0.1, "Test-Help"), 0.1, 0.01);
		assertEquals(p.getOptionString("david", "nudda", "Test-Help David"),
				"joey");
		final ArgumentDialog a = ArgumentDialog.newDialog(p, "whaddup",
				"nohelpforyou");
		p.checkForInvalid();
		System.out.println(a.scrapeDialog());
	}

}
