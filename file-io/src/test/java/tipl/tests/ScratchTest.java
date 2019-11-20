package tipl.tests;

import org.junit.Test;
import tipl.util.ArgumentParser;
import tipl.util.TIPLGlobal;

import static org.junit.Assert.assertEquals;

/**
 * Make sure the scratch directories are working correctly
 * Created by mader on 12/16/14.
 */
public class ScratchTest {
    @Test
    public void getDefaultArgumnts() {
        ArgumentParser ap = TIPLGlobal.activeParser(new String[]{});
        assertEquals(ap.getArgumentList().size(),10);
    }
}
