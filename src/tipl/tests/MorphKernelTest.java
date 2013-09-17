/**
 * 
 */
package tipl.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import tipl.tools.BaseTIPLPluginIn;

/**
 * @author mader
 * 
 */
public class MorphKernelTest {
	final private BaseTIPLPluginIn.morphKernel sph1 = BaseTIPLPluginIn
			.sphKernel(1, 1, 1);

	final private BaseTIPLPluginIn.morphKernel sph3 = BaseTIPLPluginIn
			.sphKernel(Math.sqrt(3) + 0.1, Math.sqrt(3) + 0.1,
					Math.sqrt(3) + 0.1);

	private final BaseTIPLPluginIn.morphKernel d = BaseTIPLPluginIn.fullKernel;
	private void checkD(BaseTIPLPluginIn.morphKernel b) {
		assertEquals(b.inside(0, 0, 10, 10, 10, 10, 10, 10), true);
		// x
		assertEquals(b.inside(0, 0, 10, 9, 10, 10, 10, 10), true);
		assertEquals(b.inside(0, 0, 10, 11, 10, 10, 10, 10), true);
		// y
		assertEquals(b.inside(0, 0, 10, 10, 10, 9, 10, 10), true);
		assertEquals(b.inside(0, 0, 10, 10, 10, 11, 10, 10), true);
		// z
		assertEquals(b.inside(0, 0, 10, 10, 10, 10, 10, 9), true);
		assertEquals(b.inside(0, 0, 10, 10, 10, 10, 10, 11), true);
	}

	private void checkFull(BaseTIPLPluginIn.morphKernel b) {
		for (int i = -1; i <= 1; i++)
			for (int j = -1; j <= 1; j++)
				for (int k = -1; k <= 1; k++)
					assertEquals(
							b.inside(0, 0, 10, 10 + i, 10, 10 + j, 10, 10 + k),
							true);
	}

	/**
	 * Test method for {@link tipl.tools.BaseTIPLPluginIn#sphKernel(double)}.
	 */
	@Test
	public void testSphKernelDouble() {

		// BaseTIPLPluginIn.printKernel(sph1);
		checkD(sph1);
		// xy should not belong
		assertEquals(sph1.inside(0, 0, 10, 9, 10, 9, 10, 10), false);
		// nor xz
		assertEquals(sph1.inside(0, 0, 10, 9, 10, 10, 10, 9), false);

		// BaseTIPLPluginIn.printKernel(sph3);
		checkFull(sph3);

	}

	/**
	 * Test method for {@link tipl.tools.BaseTIPLPluginIn#stationaryKernel}.
	 */
	@Test
	public void testStationaryKernel() {
		BaseTIPLPluginIn.morphKernel b = new BaseTIPLPluginIn.stationaryKernel(
				sph1);
		assertEquals(BaseTIPLPluginIn.printKernel(b),
				BaseTIPLPluginIn.printKernel(sph1));
		// checkD(b);
		b = new BaseTIPLPluginIn.stationaryKernel(sph3);
		assertEquals(BaseTIPLPluginIn.printKernel(b),
				BaseTIPLPluginIn.printKernel(sph3));
		// checkFull(b);
	}

	/**
	 * Test method for {@link tipl.tools.BaseTIPLPluginIn#useDKernel()}.
	 */
	@Test
	public void testUseDKernel() {
		final BaseTIPLPluginIn.morphKernel b = BaseTIPLPluginIn.dKernel;
		checkD(b);
		// xy should not belong
		assertEquals(b.inside(0, 0, 10, 9, 10, 9, 10, 10), false);
		// nor xz
		assertEquals(b.inside(0, 0, 10, 9, 10, 10, 10, 9), false);
	}

	/**
	 * Test method for {@link tipl.tools.BaseTIPLPluginIn#useFullKernel()}.
	 */
	@Test
	public void testUseFullKernel() {
		checkFull(d);
	}

}
