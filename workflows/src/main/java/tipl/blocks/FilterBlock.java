package tipl.blocks;

import tipl.formats.TImg;
import tipl.formats.TImgRO;
import tipl.settings.FilterSettings;
import tipl.util.*;

/**
 * Generic Block for filtering and rescaling an image
 *
 * @author mader
 */
public class FilterBlock extends LocalTIPLBlock {

    @BaseTIPLBlock.BlockIdentity(blockName = "FilterBlock",
            inputNames = {"unfiltered image"},
            outputNames = {"filtered image"})
    final public static class filtBlockFactory implements BaseTIPLBlock.TIPLBlockFactory {
        @Override
        public ITIPLBlock get() {
            return new FilterBlock();
        }
    }


    ;

    public String prefix;
    protected final static String blockName = "Filter";
    public final IBlockImage[] inImages = new IBlockImage[]{new BlockImage(
            "ufilt", "", "Input unfiltered image", true)};
    public final IBlockImage[] outImages = new IBlockImage[]{new BlockImage(
            "gfilt", "gfilt.tif", "Post-filtering image", true)};


    @Deprecated
    public FilterBlock() {
        this(new LocalTIPLBlock.LocalIOHelper(),"");
    }

    public FilterBlock(final BlockIOHelper helperTools,final String inPrefix) {
        super(helperTools,blockName);
        prefix = inPrefix;
    }

    @Override
    protected IBlockImage[] bGetInputNames() {
        return inImages;
    }

    @Override
    protected IBlockImage[] bGetOutputNames() {
        return outImages;
    }

    protected boolean changeElSize = false;
    protected D3float forcedElSize = new D3float(1.0f, 1.0f, 1.0f);

    /**
     * wrapper to get the ufilt image and change the voxel size if necessary
     *
     * @return the image (with changed voxel size)
     */
    protected TImgRO getUfiltImage() {
        final TImgRO ufiltAim = getInputFile("ufilt");
        if (!changeElSize) return ufiltAim;
        TImg ufiltAimEditable = TImgTools.WrapTImgRO(ufiltAim);
        ufiltAimEditable.setElSize(forcedElSize);
        return ufiltAimEditable;
    }

    @Override
    public boolean executeBlock() {
        final TImgRO ufiltAim = getUfiltImage();
        ITIPLPluginIO fs = TIPLPluginManager.createBestPluginIO("Filter",
                new TImgTools.HasDimensions[]{ufiltAim});
        fs.LoadImages(new TImgRO[]{ufiltAim});
        ((FilterSettings.HasFilterSettings) fs).setFilterSettings(curSettings);
        fs.execute();
        final TImg gfiltAim = fs.ExportImages(ufiltAim)[0];
        SaveImage(gfiltAim, "gfilt");
        TImgTools.RemoveTImgFromCache(getFileParameter("ufilt"));
        return true;
    }

    @Override
    protected String getDescription() {
        return "Generic Block for filtering and rescaling an image";
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void setPrefix(String newPrefix) {
        prefix = newPrefix;

    }

    protected FilterSettings curSettings = new FilterSettings();

    @Override
    public ArgumentParser setParameterBlock(final ArgumentParser p) {
        changeElSize = p.getOptionBoolean(prefix + "changeelsize", changeElSize,
                "Change the voxel size in the ufilt image");
        forcedElSize = p.getOptionD3float(prefix + "elsize", forcedElSize,
                "New voxel size for ufilt image");
        return curSettings.setParameter(p, prefix);
    }

}