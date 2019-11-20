package tipl.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import tipl.ccgeom.ConvexHull3D;
import tipl.ccgeom.cFaceList.cFaceBasic;
import tipl.formats.TImgRO;
import tipl.tools.ComponentLabel;
import tipl.util.*;
import tipl.util.TIPLPluginManager.PluginInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test the ccgeometry package using a few basic images
 *
 * @author mader
 */

public class CCGeomTest {
	public static void checkSA(ArrayList<D3int> keepPoints,double suArea) {
		ConvexHull3D cch = ConvexHull3D.HullFromD3List(keepPoints);
		
		double area = cch.getArea();
		System.out.println("Comparing Surface Area:"+keepPoints.size()+" pts");

		double goalTol = 0.05;
		System.out.println("Area:"+area+", Actual Surface Area:"+suArea+", Tolerance:"+goalTol*suArea+", Pct:"+goalTol*100+"%");
		assertEquals(suArea,area,goalTol*suArea);
	}
	/**
	 * Checks the surface area on a pointwise basis rather than the entire list
	 * @param keepPoints
	 * @param suArea
	 */
	public static void checkSAPointwise(ArrayList<D3int> keepPoints,double suArea) {
		ConvexHull3D.AdaptiveHull cah = ConvexHull3D.createAdaptiveHull();
		for(D3int cPoint : keepPoints) {
			boolean newish = cah.addPoint(cPoint);
			double newarea = cah.getArea();
			if(newish) System.out.println(cPoint+" is:"+newish+", updated area:"+newarea);
			
		}
		double area = cah.getArea();
		System.out.println("Comparing Point-wise Surface Area:");

		double goalTol = 0.05;
		System.out.println("Area:"+area+", Actual Surface Area:"+suArea+", Tolerance:"+goalTol*suArea+", Pct:"+goalTol*100+"%");
		assertEquals(suArea,area,goalTol*suArea);
	}


	@Test
	public void testConvexHullSphere() {
		System.out.println("****** Sphere Test ******");
		final ArrayList<D3int> keepPoints = new ArrayList<D3int>();
		final int windSize=10;
		final double sphereRadius=10;

		for(int x=-windSize;x<=windSize;x++) {
			for(int y=-windSize;y<=windSize;y++) {
				for(int z=-windSize;z<=windSize;z++) {
					double r = Math.sqrt(Math.pow(x, 2)+Math.pow(y, 2)+Math.pow(z, 2));
					if (r<=sphereRadius) keepPoints.add(new D3int(x,y,z));
				}
			}
		}
		double suArea = 4*Math.PI*Math.pow(sphereRadius, 2);
		checkSA(keepPoints,suArea);
		
		checkSAPointwise(keepPoints,suArea);

	}
	
	

	/**
	 * A test for 2d images since sometimes this will be applied to slices
	 */
	@Test
	public void testConvexHullCircle() {
		System.out.println("****** Circle Test ******");
		final ArrayList<D3int> keepPoints = new ArrayList<D3int>();
		final int windSize=10;
		final double sphereRadius=10;
		int z =1;
		for(int x=-windSize;x<=windSize;x++) {
			for(int y=-windSize;y<=windSize;y++) {
				double r = Math.sqrt(Math.pow(x, 2)+Math.pow(y, 2)+Math.pow(z, 2));
				if (r<=sphereRadius) keepPoints.add(new D3int(x,y,z));
			}
		}
		double suArea = Math.PI*Math.pow(sphereRadius, 2);
		checkSAPointwise(keepPoints,suArea);
		checkSA(keepPoints,suArea);


	}

	@Test
	public void testConvexHullCube() {
		System.out.println("****** Cube Test ******");
		final ArrayList<D3int> keepPoints = new ArrayList<D3int>();
		final int windSize=10;
		final double sphereRadius=10;

		for(int x=-windSize;x<=windSize;x++) {
			for(int y=-windSize;y<=windSize;y++) {
				for(int z=-windSize;z<=windSize;z++) {
					if ((Math.abs(x)<=sphereRadius) && (Math.abs(y)<=sphereRadius) && (Math.abs(z)<=sphereRadius))  keepPoints.add(new D3int(x,y,z));
				}
			}
		}
		double suArea = 6*Math.pow(2*sphereRadius, 2);
		checkSA(keepPoints,suArea);


	}
	
	@Test
	public void testConvexHullManyPoints() {
		testConvexHullShell(15,0);
	}
	
	@Test
	public void testConvexHullBigShell() {
		testConvexHullShell(30,29);
	}
	
	public void testConvexHullShell(final double sphereRadius,final double innerRadius) {
		System.out.println("****** Many Points Test ******");
		final ArrayList<D3int> keepPoints = new ArrayList<D3int>();
		final int windSize=(int) (Math.round(sphereRadius)+1);
		

		for(int x=-windSize;x<=windSize;x++) {
			for(int y=-windSize;y<=windSize;y++) {
				for(int z=-windSize;z<=windSize;z++) {
					double r = Math.sqrt(Math.pow(x, 2)+Math.pow(y, 2)+Math.pow(z, 2));
					if ((r<=sphereRadius) && (r>=innerRadius)) keepPoints.add(new D3int(x,y,z));
				}
			}
		}
		double suArea = 4*Math.PI*Math.pow(sphereRadius, 2);
		checkSA(keepPoints,suArea);
	}
	
	/**
	 * For other fun shapes
	 */
	@Test
	public void testConvexHullSpiral() {
		System.out.println("****** Convex Shapes Test ******");
		final ArrayList<D3int> keepPoints = new ArrayList<D3int>();
		final double radius = 10;
		final double height = 10;
		for(double t=0.0;t<2*Math.PI;t+=0.1) {
			int x = (int) Math.round(radius*Math.cos(t));
			int y = (int) Math.round(radius*Math.sin(t));
			int z = (int) Math.round(t/6.28*height);
			keepPoints.add(new D3int(x,y,z));
		}
		// cylinder surface area
		double suArea = 2*Math.PI*Math.pow(radius, 2)+2*Math.PI*radius*height;

	}
	
	/**
	 * Test for real images
	 */
	 @Test
	 public void testConvexHullTImg() {
		 System.out.println("Testing Component volume limits");
		 final TImgRO testImg = TestPosFunctions.wrapIt(10,
				 new TestPosFunctions.DiagonalPlaneAndDotsFunction());

		 fail();


	 }


}
