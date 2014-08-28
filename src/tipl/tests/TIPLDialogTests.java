package tipl.tests;

import static org.junit.Assert.*;

import java.awt.Panel;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import tipl.util.TIPLDialog;
import tipl.util.TIPLGlobal;

public class TIPLDialogTests {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// disable headlessness
				TIPLGlobal.activeParser("-@headless=false");
			
	}
	protected TIPLDialog curDialog;
	@Before
	public void setUp() throws Exception {
		curDialog = new TIPLDialog("TestSuite:"+this);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void testAddPanelPanel() {
		curDialog.addPanel(new Panel() {
			
		});
		curDialog.NonBlockingShow();
	}

	@Test
	public final void testAddPanelPanelIntInsets() {
		fail("Not yet implemented"); // TODO
	}

}
