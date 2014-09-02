package tipl.tests;

import static org.junit.Assert.*;

import java.awt.CardLayout;
import java.awt.Panel;

import javax.swing.JPanel;

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
		curDialog.addMessage("Hello");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void testAddPanelPanel() {
		Panel myPanel = new Panel(new CardLayout());
		myPanel.setBackground(java.awt.Color.RED);
		curDialog.addPanel(myPanel);
		curDialog.setCurrentLayer(myPanel);
		curDialog.addMessage("Hello from:"+myPanel);
		curDialog.resetCurrentLayer();
		
		Panel myPanel2 = new Panel(new CardLayout());
		myPanel2.setBackground(java.awt.Color.BLUE);
		curDialog.addPanel(myPanel2);
		curDialog.setCurrentLayer(myPanel2);
		curDialog.addMessage("Hello from:"+myPanel2);
		
		curDialog.show();
		while(curDialog.isVisible()) {
			try {
				Thread.currentThread().sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public final void testAddPanelPanelIntInsets() {
		curDialog.showDialog();
		
	}

}
