package tipl.util;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The representation of a single block in an image (typically a slice) containing the original position and any offset from this position which is useful for filtering
 * 
 * @author mader
 *
 * @param <V> The class of the data inside (typically int[] or boolean[])
 */
public class TImgBlock<V extends Cloneable> implements Serializable {
	/**
	 * the dimensions of the slice
	 */
	final D3int dim;
	/**
	 * the position of the slice and/or block
	 */
	final D3int pos;
	/**
	 * the offset from the original slice number of the image (used in filtering)
	 */
	final D3int offset;

	/**
	 * the contents of the slice itself
	 */
	final private V sliceData;
	final static D3int zero=new D3int(0);
	protected static Method cloneMethod=null;
	{
		try { 
			cloneMethod = Object.class.getMethod("clone");
		} catch(NoSuchMethodException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Clone method is missing"+e.getMessage());
		}
	}
	/**
	 * create a new block given a chunk of data and a position and dimensions
	 * @param pos
	 * @param cSlice
	 * @param dim
	 */
	public TImgBlock(V cSlice,D3int pos,D3int dim) {
		this.sliceData=cSlice;
		this.pos=pos;
		this.offset=zero;
		this.dim=dim;
	}
	/**
	 * Create a new block with an offset given a chunk of data and position, dimensions
	 * @param pos
	 * @param cSlice
	 * @param dim
	 * @param offset
	 */
	public TImgBlock(V cSlice,D3int pos,D3int dim,D3int offset) {
		this.sliceData=cSlice;
		this.pos=pos;
		this.offset=offset;
		this.dim=dim;
	}
	public V get() {return sliceData;}
	public V getClone() {try {
		return (V) cloneMethod.invoke(sliceData);
	} catch (IllegalAccessException e) {
		e.printStackTrace();
		throw new IllegalArgumentException("Clone method is missing"+e.getMessage());
	} catch (IllegalArgumentException e) {
		e.printStackTrace();
		throw new IllegalArgumentException("Clone method is missing"+e.getMessage());
	} catch (InvocationTargetException e) {
		e.printStackTrace();
		throw new IllegalArgumentException("Clone method is missing"+e.getMessage());
	}}
	public D3int getPos() {return pos;}
	public D3int getDim() {return dim;}
	public D3int getOffset() {return offset;}
}