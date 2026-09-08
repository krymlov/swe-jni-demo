package swisseph;

import org.junit.jupiter.api.Test;

import static swisseph.SweObama.swe_obama;

public class SweObamaTest {

    @Test
    void test_swe_obama() {
        SwephExp.loadSweCurrentLibrary();
        SwephExp sweph = new SwephExp();
        sweph.swe_set_ephe_path("ephe");
        swe_obama(sweph);
    }
}
