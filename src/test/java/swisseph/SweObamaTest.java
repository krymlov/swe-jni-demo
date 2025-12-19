package swisseph;

import org.junit.jupiter.api.Test;
import org.swisseph.ISwissEph;
import org.swisseph.SwephNative;

import static org.swisseph.api.ISweConstants.EPHE_PATH;
import static swisseph.SweObama.swe_obama;

public class SweObamaTest {

    @Test
    void test_swe_obama() {
        try (ISwissEph swissEph = new SwephNative(EPHE_PATH)) {
            swe_obama(swissEph);
        }
    }
}
