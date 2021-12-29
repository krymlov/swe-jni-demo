package swisseph;

import org.junit.jupiter.api.Test;
import org.swisseph.SwephNative;

import static swisseph.SweMini.swe_mini;

public class SweMiniTest {

    @Test
    void test_swe_mini() {
        swe_mini(new SwephNative(null), 1, 1, 2022);
    }
}
