package tipl.tools;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.util.ITIPLPluginIO;
import tipl.util.TypedPath;

public interface IVoronoiTransform extends ITIPLPluginIO {

	/** Code for exporting the voronoi distances to an Aim class */
	public abstract TImg ExportDistanceAim(TImgRO.CanExport templateAim);

	/** Code for exporting the voronoi volumes to an Aim class */
	public abstract TImg ExportVolumesAim(TImgRO.CanExport templateAim);

	/** Code for writing the voronoi distances to an Aim file */
	public abstract void WriteDistanceAim(TImgRO.CanExport templateAim, TypedPath outname);

	/** Code for writing the voronoi volumes to an Aim file */
	public abstract void WriteVolumesAim(TImgRO.CanExport templateAim,
			TypedPath outname);

}