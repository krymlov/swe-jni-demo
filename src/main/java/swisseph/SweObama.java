package swisseph;
/*
 * Swiss Ephemeris example: compute the birth chart for Barack Obama
 * Necessary data: date of birth, birth time in Universal time,
 * geographical coordinates of birthplace.
 *
 * The sample person is born on 4 August 1961 at 19:24 local time in Honolulu, Hawaii.
 * Swiss Ephemeris uses 24-hour clock times, with hour 0..23, minute 0..59, seconds 0..59.
 * The time zone in Hawaii was 10h west.
 * Expressed in Universal time, it is already 10 hours later.
 * That moves the birth to 5 August 1961, 05:24 Universal Time (UT).
 * Clock time decimal is 5 + 24/60 = 5.4 hours.
 * All time zone conversions must be done outside Swiss Ephemeris.
 * SE knows nothing about time zones and provides no features of time zone conversion.
 *
 * For Honolulu we use latitude 21n18, longitude 157w52.
 * 21n18' in decimal is 21 + 18/60.0 = 21.3°,
 * 157n52 in decimal is -157 - 52/60.0 = -157.86666667°.
 * Eastern longitude is positive, western longitude is negative.
 * Northen latitude is positive, southern latitude is negative.
 * All geographic coordinates must come from outside SE and must be
 * in decimal numbers with fractions, not in degrees, minutes and seconds.
 *
 * programming language: C
 * which is the native language of the Swiss Ephemeris library.
 *
 * This example program is in the Public Domain.
 */

import org.swisseph.ISwissEph;
import org.swisseph.SwephNative;

import static org.swisseph.api.ISweConstants.EPHE_PATH;
import static swisseph.SweConst.*;

public class SweObama {
    public static void main(String[] args) {
        try (ISwissEph sweph = new SwephNative(EPHE_PATH)) {
            swe_obama(sweph);
        }
    }

    public static int swe_obama(ISwissEph sweph) {
        int iday = 5;        // day  in range 1..31
        int imon = 8;        // month in range 1..12
        int iyar = 1961;    // year in range -12998 .. 16799
        // astronomical year counting, year -1 = 2 BC, year 0 = 1 BC.
        double dhour = 5.4;    // range 0.0 .. 23.999999; 24 h belongs to the next day as 0.00h
        double dlon = -157.86666667;    // geographical longitude range -180 .. 179.99999
        double dlat = 21.3;        // geographical latitude range -90 .. 90
        int ihsy = 'P';        // house system letter for Placidus
        // variables for later use
        int i, ipl, iret, iflag;
        double[] xx = new double[6];        // array of six doubles for returned coordindates;
        double[] cusps = new double[13];
        double[] ascmc = new double[10];    // arrays for house cusps, and asc. etc
        StringBuilder serr = new StringBuilder(0);
        //char serr[AS_MAXCH];	// space for error string;
        //char spname[AS_MAXCH];	// space for planet name;
        // show input valued
        System.out.printf("Date and time in UT: day=%d mon=%d year=%d decimal hour=%f\n", iday, imon, iyar, dhour);
        System.out.printf("\tdecimal geographical coordinates lat=%f, long=%f\n", dlat, dlon);
        // compute juldate of birth
        double jd_ut = sweph.swe_julday(iyar, imon, iday, dhour, SE_GREG_CAL);
        System.out.printf("\nJulday of birth = %f\n", jd_ut);
        // compute planets SE_SUN .. SE_TRUE_NODE which have the consecutive numbers 0..11
        iflag = (SEFLG_SWIEPH | SEFLG_SPEED);
        System.out.printf("Planet\tecl.long.\tecl.lat.\tdist. AU\tspeed deg/day\n");
        for (ipl = SE_SUN; ipl <= SE_TRUE_NODE; ipl++) {
            String spname = sweph.swe_get_planet_name(ipl);        // get the planet name
            //spname[7] = '\0';				// keep only 7 characters of name r
            System.out.printf("%s\t", spname);
            iret = sweph.swe_calc_ut(jd_ut, ipl, iflag, xx, serr);
            if (iret < 0) {    // negative return value indicates a problem, reason is in serr.
                System.out.printf("iret=%d, %s\n", iret, serr);
            }
            System.out.printf("%10.6f\t%9.6f\t%9.6f\t%9.6f\n", xx[0], xx[1], xx[2], xx[3]);
        }
        // compute ascendant and houses
        iflag = 0;
        iret = sweph.swe_houses_ex(jd_ut, iflag, dlat, dlon, ihsy, cusps, ascmc);
        if (iret < 0) {
            System.out.printf("Unknown problem with house calculation, iret=%d\n", iret);
            return ERR;
        }
        System.out.printf("\nAscendant %10.6f\tMC %10.6f\n", ascmc[0], ascmc[1]);
        System.out.printf("House system %s\n", sweph.swe_house_name(ihsy));
        for (i = 1; i <= 12; i++) {
            System.out.printf("cusp %2d\t%10.6f\n", i, cusps[i]);
        }
        System.out.print("\n");
        return OK;
    }
}
