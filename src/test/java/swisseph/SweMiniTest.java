package swisseph;

import org.junit.jupiter.api.Test;
import org.swisseph.ISwissEph;
import org.swisseph.SwephNative;

import static org.swisseph.api.ISweConstants.EPHE_PATH;
import static swisseph.SweMini.swe_mini;

public class SweMiniTest {

    @Test
    void test_swe_mini() {
        try (ISwissEph swissEph = new SwephNative(EPHE_PATH)) {
            swe_mini(swissEph, 1, 1, 2022);
        }
    }
}
