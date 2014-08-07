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
        List<cFaceBasic> faceList = cch.GetFaces();
        double area = 0;
        for (cFaceBasic cEle: faceList) {
        	area+=cEle.getArea();
        }
        System.out.println("Comparing Surface Area:");
        
        double goalTol = 0.05;
        System.out.println("Area:"+area+", Actual Surface Area:"+suArea+", Tolerance:"+goalTol*suArea+", Pct:"+goalTol*100+"%");
        assertEquals(suArea,area,goalTol*suArea);
	}


	 @Test
	    public void testConvexHullSphere() {
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

	        
	    }
	 @Test
	    public void testConvexHullCube() {
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
	        final ArrayList<D3int> keepPoints = new ArrayList<D3int>();
	        final int windSize=15;
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
    public void testConvexHull() {
        System.out.println("Testing Component volume limits");
        final TImgRO testImg = TestPosFunctions.wrapIt(10,
                new TestPosFunctions.DiagonalPlaneAndDotsFunction());
        
        fail();

        
    }


}
