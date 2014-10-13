/**
 * 
 */
package tipl.tests;

import org.junit.Test;
import tipl.util.*;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author maderk
 * 
 */
public class TIPLGlobalTests {


    @Test
    public void testPluginManager() {
        TIPLPluginManager.getAllPlugins();

        List<TIPLPluginManager.PluginInfo> allKV = TIPLPluginManager.getPluginsNamed("Filter");
        assertEquals(2,allKV.size());
        for (TIPLPluginManager.PluginInfo cInfo : allKV) {
            TIPLPluginManager.getPlugin(cInfo);
        }


    }


}
