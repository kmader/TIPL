package tipl.scripts;

import ij.IJ;
import tipl.formats.TImg;
import tipl.formats.VirtualAim;
import tipl.util.ArgumentParser;
import tipl.util.TIPLGlobal;
import tipl.util.TImgTools;

/**
 * Tools to Glue ImageJ and TIPL Together
 *
 * @author Kevin Mader
 *         <p/>
 *         Direct interface to imageJ
 *         <p/>
 *         Change Log:
 *         <p/>
 *         v1 Start
 */
public class IJGlue {

    public static void run(final TImg inImg, final String cmd, final String opts) {
        final VirtualAim vaImg = new VirtualAim(inImg);
        IJ.run(vaImg.getImagePlus(), cmd, opts);
    }

    public final int LASTSTAGE = 11;

    public static final String kVer = "121106_001";

    public static void main(final String[] args) {

        System.out
                .println("IJGlue Provides Access to ImageJ Functions and Scripting  v"
                        + kVer);
        System.out.println(" By Kevin Mader (kevin.mader@gmail.com)");
        final ArgumentParser p = TIPLGlobal.activeParser(args);
        p.getOptionInt(
                "stage",
                0,
                "Point to start script : 0 -Filter and Scale, 1 - Threshold and Generate Plat and" +
                        " Bubbles, 2 - Sample Masks and peeling, 3 - Distance map, " +
                        "4 - bubble labeling, 5 - bubble filling, 6 - calculate thickness, " +
                        "7 - Curvature Analysis, 8 - Shape Analysis 9 - Neighborhood Analysis, " +
                        "10 - structure analysis (XDF), 11 - Z/R Profiles");
        p.getOptionString("file", "", "Input File");
        run(TImgTools.ReadTImg("bubbles.tif"),
                "3D Project...",
                "projection=[Brightest Point] axis=Y-Axis slice=1 initial=0 total=180 rotation=6 " +
                        "lower=1 upper=255 opacity=10 surface=100 interior=50 interpolate");
    }

}
