/**
 * 
 */
package tipl.formats;

import java.util.ArrayList;

import tipl.formats.TImgRO.TImgStack;
import tipl.util.D3float;
import tipl.util.D3int;
import tipl.util.TImgTools;

/**
 * A simple image storage mechanism keeping the images in a large array of linear arrays [z][y*dim.x+x]
 * @author mader
 *
 */
public class SimpleStack<T> extends TImgStack {
	private final ArrayList<T> stack;
	protected final int imageType;
	D3int myDim;
	D3int myPos;
	D3float myElSize;
	
	protected String procLog="";
	public static SimpleStack<float[]> FloatStack(TImgTools.HasDimensions cDims) {return FloatStack(cDims.getDim(),cDims.getPos(),cDims.getElSize());}
	public static SimpleStack<float[]> FloatStack(D3int cDim,D3int cPos, D3float cEl) {
		int sliceDim=cDim.x*cDim.y;
		ArrayList<float[]> newStack=new ArrayList<float[]>();
		for(int z=0;z<=cDim.z;z++) newStack.add(new float[sliceDim]);
		return new SimpleStack<float[]>(cDim,cPos,cEl,"",newStack,false);
	}
	public static SimpleStack<boolean[]> BoolStack(TImgTools.HasDimensions cDims) {return BoolStack(cDims.getDim(),cDims.getPos(),cDims.getElSize());}
	public static SimpleStack<boolean[]> BoolStack(D3int cDim,D3int cPos, D3float cEl) {
		int sliceDim=cDim.x*cDim.y;
		ArrayList<boolean[]> newStack=new ArrayList<boolean[]>();
		for(int z=0;z<=cDim.z;z++) newStack.add(new boolean[sliceDim]);
		return new SimpleStack<boolean[]>(cDim,cPos,cEl,"",newStack,false);
	}
	public static SimpleStack<int[]> IntStack(TImgTools.HasDimensions cDims) {return IntStack(cDims.getDim(),cDims.getPos(),cDims.getElSize());}
	public static SimpleStack<int[]> IntStack(D3int cDim,D3int cPos, D3float cEl) {
		int sliceDim=cDim.x*cDim.y;
		ArrayList<int[]> newStack=new ArrayList<int[]>();
		for(int z=0;z<=cDim.z;z++) newStack.add(new int[sliceDim]);
		return new SimpleStack<int[]>(cDim,cPos,cEl,"",newStack,false);
	}
	
	
	
	/**
	 * Create a new SimpleStack from a dimension and an existing stack
	 */
	protected SimpleStack(D3int cDim,D3int cPos,D3float cEl,String log,ArrayList<T> istack,boolean makeCopy) {
		myDim=cDim;
		myPos=cPos;
		myElSize=cEl;
		procLog=log;
		
		imageType=TImgTools.identifySliceType(istack.get(0));
		if (makeCopy) {
			stack=new ArrayList<T>(cDim.z);
			for(T cSlice: istack) stack.add(cSlice);
		}
		else stack=istack;
	}


	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#appendProcLog(java.lang.String)
	 */
	@Override
	public String appendProcLog(String inData) {
		procLog+=inData;
		return procLog;
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#getCompression()
	 */
	@Override
	public boolean getCompression() {
		return false;
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#getImageType()
	 */
	@Override
	public int getImageType() {
		return imageType;
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#getPath()
	 */
	@Override
	public String getPath() {
		// TODO Auto-generated method stub
		return getSampleName();
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#getPolyImage(int, int)
	 */
	@Override
	public Object getPolyImage(int sliceNumber, int asType) {
		// TODO Auto-generated method stub
		return TImgTools.convertArrayType(getSlice(sliceNumber),imageType, asType, false,1.0f,128);
	}
	/**
	 * A method for getting the slice from the arraylist (allows overloading and thus replacement of arraylist with something much fancier)
	 * @param sliceNumber
	 * @return slice as an object
	 */
	protected T getSlice(int sliceNumber) {
		return stack.get(sliceNumber);
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#getSampleName()
	 */
	@Override
	public String getSampleName() {
		// TODO Auto-generated method stub
		return "RAM-Resident";
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#getShortScaleFactor()
	 */
	@Override
	public float getShortScaleFactor() {
		// TODO Auto-generated method stub
		return 1.0f;
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#getSigned()
	 */
	@Override
	public boolean getSigned() {
		return false;
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#isFast()
	 */
	@Override
	public int isFast() {
		return TImgTools.FAST_MEMORY_BASED;
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#isGood()
	 */
	@Override
	public boolean isGood() {
		return true;
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#WriteAim(java.lang.String)
	 */
	@Override
	public void WriteAim(String path) {
		TImgTools.WriteTImg(this, path);
	}

	/* (non-Javadoc)
	 * @see tipl.formats.TImgRO#WriteAim(java.lang.String, int, float, boolean)
	 */
	@Override
	public void WriteAim(String outpath, int outType, float scaleVal,
			boolean IisSigned) {
		TImgTools.WriteTImg(this, outpath,outType,scaleVal,IisSigned);

	}

	/* (non-Javadoc)
	 * @see tipl.util.TImgTools.HasDimensions#getDim()
	 */
	@Override
	public D3int getDim() {
		return myDim;
	}

	/* (non-Javadoc)
	 * @see tipl.util.TImgTools.HasDimensions#getElSize()
	 */
	@Override
	public D3float getElSize() {
		return myElSize;
	}

	/* (non-Javadoc)
	 * @see tipl.util.TImgTools.HasDimensions#getOffset()
	 */
	@Override
	public D3int getOffset() {
		return new D3int(0,0,0);
	}

	/* (non-Javadoc)
	 * @see tipl.util.TImgTools.HasDimensions#getPos()
	 */
	@Override
	public D3int getPos() {
		return myPos;
	}

	/* (non-Javadoc)
	 * @see tipl.util.TImgTools.HasDimensions#getProcLog()
	 */
	@Override
	public String getProcLog() {
		return procLog;
	}

}
