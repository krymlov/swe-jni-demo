package swisseph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static swisseph.SweTest.SE_FIXSTAR;
import static swisseph.SweTest.SE_MEAN_NODE;
import static swisseph.SweTest.SE_TRUE_NODE;
import static swisseph.SweTest.objectOf;
import static swisseph.SweTest.parseDate;
import static swisseph.SweTest.parseGeopos;
import static swisseph.SweTest.parseTime;
import static swisseph.SweTest.swe_test;

public class SweTestTest {

    @Test
    void objectOf_digitsMatchTheSwephexpPlanetNumbers() {
        // 0=Sun .. 9=Pluto, the same numbers swephexp.h itself defines
        for (int i = 0; i <= 9; i++) assertEquals(i, objectOf(Character.forDigit(i, 10)));
        assertEquals(SE_MEAN_NODE, objectOf('m'));
        assertEquals(SE_TRUE_NODE, objectOf('t'));
        assertEquals(SE_FIXSTAR, objectOf('f'));
        assertThrows(IllegalArgumentException.class, () -> objectOf('x'));
    }

    @Test
    void parseDate_dayMonthYear() {
        assertEquals(18., parseDate("18.4.1976")[0]);
        assertEquals(4., parseDate("18.4.1976")[1]);
        assertEquals(1976., parseDate("18.4.1976")[2]);
    }

    @Test
    void parseTime_decimalHours() {
        assertEquals(20.35, parseTime("20:21:00"), 1e-6);
        assertEquals(12., parseTime("12:00"), 1e-6);
    }

    @Test
    void parseGeopos_longitudeThenLatitude() {
        final double[] p = parseGeopos("27.13,49.45,P");
        assertEquals(27.13, p[0]);
        assertEquals(49.45, p[1]);
    }

    /**
     * One command line, every supported flag at once - the same live scenario driven from
     * {@code main()}. Structural assertions only (this is a demo, not a numeric regression
     * suite - {@code swe-java-lib}'s own tests already prove the underlying arithmetic).
     */
    @Test
    void swe_test_everyFlagAtOnce() {
        SwephExp.loadSweCurrentLibrary();
        final SwephExp sweph = new SwephExp();
        sweph.swe_set_ephe_path("ephe");

        final String out = swe_test(sweph, new String[]{
                "-b18.4.1976", "-ut20:21:00", "-p0123456789mtf", "-xfSpica",
                "-house27.13,49.45,P", "-sid1", "-true", "-rise", "-solecl"});

        assertTrue(out.contains("UT:"), out);
        assertTrue(out.contains("ayanamsa"), out);
        assertTrue(out.contains("Sun"), out);
        assertTrue(out.contains("Spica"), out);
        assertTrue(out.contains("Ascendant"), out);
        assertTrue(out.contains("house 12"), out);
        assertTrue(out.contains("rise of"), out);
        assertTrue(out.contains("next solar eclipse"), out);
    }

    @Test
    void swe_test_withoutBirthDatePrintsUsage() {
        final String out = swe_test(new SwephExp(), new String[]{"-ut12:00:00"});
        assertTrue(out.startsWith("usage:"), out);
    }
}
