package tipl.spark;

import com.esotericsoftware.kryo.Kryo;
import org.apache.spark.serializer.KryoRegistrator;

/**
 * A class to load several of the more heavily used classes into the Kyro serializer for increased performance and smaller sizes
 *
 * @author mader
 */
public class TIPLRegistrator implements KryoRegistrator {

    @Override
    public void registerClasses(Kryo arg0) {

        arg0.register(tipl.util.D3int.class);
        arg0.register(tipl.util.D4int.class);
        arg0.register(tipl.util.TImgBlock.class);
        arg0.register(tipl.util.ITIPLPlugin.class);
        arg0.register(tipl.util.ITIPLPluginIn.class);
        arg0.register(tipl.util.ITIPLPluginOut.class);
        arg0.register(tipl.util.ITIPLPluginIO.class);
        arg0.register(tipl.spark.DTImg.class);
        arg0.register(tipl.spark.CL.OmnidirectionalMap.class);
        arg0.register(tipl.spark.CL.PassthroughHashMap.class);
        //arg0.setRegistrationRequired(true);
    }

}