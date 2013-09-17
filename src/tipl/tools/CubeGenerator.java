package tipl.tools;

public class CubeGenerator {

	private final int slice;
	private int line;
	private int voxel;
	private final boolean[][] highSlice, lowSlice;

	public CubeGenerator(boolean[][] hiSlice, boolean[][] loSlice, int it) {
		highSlice = hiSlice;
		lowSlice = loSlice;
		slice = it;
		startIterator();
	} // constructor

	public boolean hasNext() {
		voxel++;
		if (voxel < (lowSlice[0].length - 1)) {
			return true;
		} else {
			voxel = 0;
			line++;
			if (line < (lowSlice.length - 1)) {
				return true;
			} else {
				return false;
			}
		}
	} // hasNext

	public Cube next() {
		final boolean[] coordinates = new boolean[] { lowSlice[line][voxel],
				lowSlice[line + 1][voxel], lowSlice[line + 1][voxel + 1],
				lowSlice[line][voxel + 1], highSlice[line][voxel],
				highSlice[line + 1][voxel], highSlice[line + 1][voxel + 1],
				highSlice[line][voxel + 1] };
		return new Cube(coordinates, slice, line, voxel);
	} // next

	public void startIterator() {
		line = 0;
		voxel = -1;
	} // startIterator

} // CubeGenerator