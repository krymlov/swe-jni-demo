package swisseph;

/*
 * A small command-line front end over the raw swe-api JNI bindings (SwephExp), styled after
 * astro.com's own reference program:
 *   https://www.astro.com/ftp/swisseph/src/swetest.c
 *
 * This is NOT a port of swetest.c - that file is several thousand lines wide (asteroids,
 * hypothetical bodies, heliacal risings, azimuth/altitude, Gauquelin sectors, topocentric and
 * horizontal coordinates, a dozen output-formatting switches, ...) and porting all of it would
 * dwarf the point of this project, which is to demonstrate calling the JNI layer directly, not
 * to reproduce swetest byte for byte. What is here is a deliberately small subset of its
 * command-line syntax - real swetest flags, parsed the same way swetest parses them - each one
 * dispatching to a different corner of SwephExp that the other two demos (SweMini, SweObama)
 * do not touch: sidereal mode and ayanamsa, fixed stars, sunrise/sunset, and a global solar
 * eclipse search.
 *
 * Supported flags, all straight from swetest's own vocabulary:
 *   -b<dd.mm.yyyy>          birth date, Gregorian (swetest's own Julian/Gregorian auto-switch
 *                           around 1582-10-15 is not reproduced here - always Gregorian)
 *   -ut<hh:mm:ss>           universal time, default 12:00:00
 *   -p<chars>               which bodies to compute - digits 0-9 for Sun..Pluto (swetest's own
 *                           SE_* numbering, so the character *is* the planet number), 'm'/'t'
 *                           for the mean/true lunar node, 'f' for a fixed star (name via -xf)
 *   -xf<name>               the fixed star name/number, when 'f' is in -p (swe_fixstar_ut)
 *   -house<lon>,<lat>,<L>   house cusps + ascendant/MC, <L> a house system letter (e.g. P)
 *   -sid<n>                 sidereal mode n (0 Fagan/Bradley, 1 Lahiri, ...) - see
 *                           swe_get_ayanamsa_name for what a given n means
 *   -true                   true/geometric rather than apparent positions (SEFLG_TRUEPOS)
 *   -rise / -set            next sunrise/sunset (or of the first -p body) at -house's lon/lat
 *   -solecl                 next global solar eclipse of any type, forward from the date
 *   -edir<path>             ephemeris directory, default "ephe"
 *
 * Example, matching real swetest usage:
 *   java swisseph.SweTest -b18.4.1976 -ut20:21:00 -p0123456789 -house27.13,49.45,P -sid1 -true
 */
public class SweTest {

    // local, not org.swisseph.SweConst - see SweMini/SweObama for why: that class lives in
    // swe-java-lib (swisseph:swe), and this project depends only on swe-api. Values are
    // swephexp.h's own.
    static final int SE_GREG_CAL = 1;
    static final int SEFLG_SWIEPH = 2;
    static final int SEFLG_SPEED = 256;
    static final int SEFLG_TRUEPOS = 16;
    static final int SEFLG_SIDEREAL = 64 * 1024;
    static final int SE_MEAN_NODE = 10;
    static final int SE_TRUE_NODE = 11;
    static final int SE_FIXSTAR = -1; // sentinel used only inside this class, not a real SE_* id

    static final int SE_CALC_RISE = 1;
    static final int SE_CALC_SET = 2;
    static final int SE_BIT_DISC_CENTER = 256;
    static final int SE_BIT_NO_REFRACTION = 512;

    static final int SE_ECL_CENTRAL = 1;
    static final int SE_ECL_NONCENTRAL = 2;
    static final int SE_ECL_TOTAL = 4;
    static final int SE_ECL_ANNULAR = 8;
    static final int SE_ECL_PARTIAL = 16;
    static final int SE_ECL_ANNULAR_TOTAL = 32;
    static final int SE_ECL_ALLTYPES_SOLAR = SE_ECL_CENTRAL | SE_ECL_NONCENTRAL | SE_ECL_TOTAL
            | SE_ECL_ANNULAR | SE_ECL_PARTIAL | SE_ECL_ANNULAR_TOTAL;

    public static void main(String[] args) {
        SwephExp.loadSweCurrentLibrary();
        SwephExp sweph = new SwephExp();
        System.out.print(swe_test(sweph, args));
    }

    public static String swe_test(SwephExp sweph, String[] args) {
        // -b, -ut, -p, -xf, -house, -sid, -true, -rise, -set, -solecl, -edir
        String bdate = null, ut = "12:00:00", plist = "", star = "", house = null, ephe = "ephe";
        Integer sid = null;
        boolean trueFlag = false, rise = false, set = false, solecl = false;

        for (String arg : args) {
            if (arg.startsWith("-b")) bdate = arg.substring(2);
            else if (arg.startsWith("-ut")) ut = arg.substring(3);
            else if (arg.startsWith("-xf")) star = arg.substring(3);
            else if (arg.startsWith("-p")) plist = arg.substring(2);
            else if (arg.startsWith("-house")) house = arg.substring(6);
            else if (arg.startsWith("-sid")) sid = Integer.parseInt(arg.substring(4));
            else if (arg.equals("-true")) trueFlag = true;
            else if (arg.equals("-rise")) rise = true;
            else if (arg.equals("-set")) set = true;
            else if (arg.equals("-solecl")) solecl = true;
            else if (arg.startsWith("-edir")) ephe = arg.substring(5);
        }

        if (null == bdate) {
            return "usage: -b<dd.mm.yyyy> [-ut<hh:mm:ss>] [-p<0-9,m,t,f>] [-xf<star>]"
                    + " [-house<lon>,<lat>,<letter>] [-sid<n>] [-true] [-rise] [-set] [-solecl]"
                    + " [-edir<path>]\n";
        }

        sweph.swe_set_ephe_path(ephe);
        final StringBuilder out = new StringBuilder();

        final double[] date = parseDate(bdate);
        final double hour = parseTime(ut);
        final double tjdUt = sweph.swe_julday((int) date[2], (int) date[1], (int) date[0], hour, SE_GREG_CAL);
        out.append(String.format("UT: %.6f  (%s -b%s -ut%s)%n", tjdUt, "SweTest", bdate, ut));

        int iflag = SEFLG_SWIEPH | SEFLG_SPEED;
        if (trueFlag) iflag |= SEFLG_TRUEPOS;
        if (null != sid) {
            sweph.swe_set_sid_mode(sid, 0., 0.);
            iflag |= SEFLG_SIDEREAL;
            out.append(String.format("ayanamsa = %.6f  (%s)%n",
                    sweph.swe_get_ayanamsa_ut(tjdUt), sweph.swe_get_ayanamsa_name(sid)));
        }

        final double[] lonLatAlt = null == house ? null : parseGeopos(house);

        int firstIpl = -1;
        for (int c = 0; c < plist.length(); c++) {
            final int ipl = objectOf(plist.charAt(c));
            if (-1 == firstIpl && SE_FIXSTAR != ipl) firstIpl = ipl;

            final double[] xx = new double[6];
            final StringBuilder serr = new StringBuilder();
            final String name;
            final int rc;
            if (SE_FIXSTAR == ipl) {
                final StringBuilder starName = new StringBuilder(star);
                rc = sweph.swe_fixstar_ut(starName, tjdUt, iflag, xx, serr);
                name = starName.toString();
            } else {
                rc = sweph.swe_calc_ut(tjdUt, ipl, iflag, xx, serr);
                name = sweph.swe_get_planet_name(ipl);
            }

            if (rc < 0) out.append(String.format("%-12s error: %s%n", name, serr));
            else out.append(String.format("%-12s %12.6f  %9.6f  %9.6f  %9.6f%n",
                    name, xx[0], xx[1], xx[2], xx[3]));
        }

        if (null != lonLatAlt) {
            final char hsys = house.charAt(house.length() - 1);
            final double[] cusps = new double[13];
            final double[] ascmc = new double[10];
            final int houseFlags = null == sid ? 0 : SEFLG_SIDEREAL;
            final int rc = sweph.swe_houses_ex(tjdUt, houseFlags, lonLatAlt[1], lonLatAlt[0], hsys, cusps, ascmc);
            if (rc < 0) {
                out.append("houses: error\n");
            } else {
                out.append(String.format("Ascendant %10.6f   MC %10.6f   house system %s%n",
                        ascmc[0], ascmc[1], sweph.swe_house_name(hsys)));
                for (int h = 1; h <= 12; h++) out.append(String.format("house %2d %10.6f%n", h, cusps[h]));
            }
        }

        if (rise || set) {
            if (null == lonLatAlt) {
                out.append("rise/set needs -house<lon>,<lat>,<letter> for the observer's position\n");
            } else {
                final int ipl = -1 == firstIpl ? 0 /* Sun */ : firstIpl;
                final double[] geopos = {lonLatAlt[0], lonLatAlt[1], lonLatAlt.length > 2 ? lonLatAlt[2] : 0.};
                final double[] tret = new double[10];
                final StringBuilder serr = new StringBuilder();
                final int rsmi = (rise ? SE_CALC_RISE : SE_CALC_SET) | SE_BIT_DISC_CENTER | SE_BIT_NO_REFRACTION;
                final int rc = sweph.swe_rise_trans(tjdUt, ipl, new StringBuilder(), SEFLG_SWIEPH, rsmi,
                        geopos, 0., 0., tret, serr);
                if (rc < 0) out.append(String.format("%s: error: %s%n", rise ? "rise" : "set", serr));
                else out.append(String.format("%s of %s: julian day %.6f%n",
                        rise ? "rise" : "set", sweph.swe_get_planet_name(ipl), tret[0]));
            }
        }

        if (solecl) {
            final double[] tret = new double[10];
            final StringBuilder serr = new StringBuilder();
            final int eclFlags = sweph.swe_sol_eclipse_when_glob(
                    tjdUt, SEFLG_SWIEPH, SE_ECL_ALLTYPES_SOLAR, tret, 0, serr);
            if (eclFlags < 0) out.append(String.format("solecl: error: %s%n", serr));
            else out.append(String.format("next solar eclipse: julian day %.6f, type flags 0x%x%n",
                    tret[0], eclFlags));
        }

        return out.toString();
    }

    /** {@code "0123456789mtf"} -> the SE_* planet number, or {@link #SE_FIXSTAR} for 'f' */
    static int objectOf(char c) {
        if ('m' == c) return SE_MEAN_NODE;
        if ('t' == c) return SE_TRUE_NODE;
        if ('f' == c) return SE_FIXSTAR;
        if (c >= '0' && c <= '9') return c - '0';
        throw new IllegalArgumentException("unsupported -p object '" + c + "'");
    }

    /** {@code "dd.mm.yyyy"} -> {day, month, year} */
    static double[] parseDate(String dmy) {
        final String[] parts = dmy.split("\\.");
        return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2])};
    }

    /** {@code "hh:mm:ss"} or {@code "hh:mm"} -> decimal hours */
    static double parseTime(String hms) {
        final String[] parts = hms.split(":");
        double h = Double.parseDouble(parts[0]);
        if (parts.length > 1) h += Double.parseDouble(parts[1]) / 60.;
        if (parts.length > 2) h += Double.parseDouble(parts[2]) / 3600.;
        return h;
    }

    /** {@code "<lon>,<lat>[,<letter>]"} -> {lon, lat} */
    static double[] parseGeopos(String lonLatLetter) {
        final String[] parts = lonLatLetter.split(",");
        return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
    }
}
