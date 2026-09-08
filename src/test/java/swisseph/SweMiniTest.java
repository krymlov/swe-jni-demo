package swisseph;

import org.junit.jupiter.api.Test;

import static swisseph.SweMini.swe_mini;

public class SweMiniTest {

    @Test
    void test_swe_mini() {
        SwephExp.loadSweCurrentLibrary();
        SwephExp sweph = new SwephExp();
        sweph.swe_set_ephe_path("ephe");
        swe_mini(sweph, 1, 1, 2022);
    }
}
