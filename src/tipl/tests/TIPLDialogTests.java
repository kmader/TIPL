package tipl.tests;

import static org.junit.Assert.*;

import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.Panel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

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
		curDialog.createNewLayer("Blank");
		curDialog.addMessage("Hello");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void testAddPanelPanel() {
		
		curDialog.addMessage("Hello");
		
		curDialog.createNewLayer("Test");
		curDialog.addMessage("Hello Test");
		
		curDialog.createNewLayer("Dog");
		curDialog.addMessage("Hello dog");

		
		curDialog.NonBlockingShow();
		while(curDialog.isVisible()) {
			try {
				Thread.currentThread().sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Test
	public final void testAddJPanel() {
		
		
		JTabbedPane jtbExample = new JTabbedPane();
		JPanel jplInnerPanel1 = createInnerPanel("Tab 1 Contains Tooltip and Icon");
		jtbExample.addTab("One", jplInnerPanel1);
		jtbExample.setSelectedIndex(0);
		JPanel jplInnerPanel2 = createInnerPanel("Tab 2 Contains Icon only");
		jtbExample.addTab("Two", jplInnerPanel2);
		curDialog.showDialog();
		while(curDialog.isVisible()) {
			try {
				Thread.currentThread().sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	static protected JPanel createInnerPanel(String text) {
		JPanel jplPanel = new JPanel();
		JLabel jlbDisplay = new JLabel(text);
		jlbDisplay.setHorizontalAlignment(JLabel.CENTER);
		jplPanel.setLayout(new GridLayout(1, 1));
		jplPanel.add(jlbDisplay);
		return jplPanel;
	}

	@Test
	public final void testAddPanelPanelIntInsets() {
		curDialog.showDialog();
		
	}
	

}
