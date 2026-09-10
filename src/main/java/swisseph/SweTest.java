package swisseph;

import java.util.Locale;

import org.swisseph.ISwissEph;
import org.swisseph.SwephNative;

/**
 * A Java port of astro.com's own reference program, <b>swetest.c</b>, on top of the raw JNI
 * bindings in {@link SwephExp} - reached through {@link org.swisseph.ISwissEph} rather than
 * called on {@code SwephExp} directly, which is what lets this run against either engine:
 * <p>
 * <a href="https://www.astro.com/ftp/swisseph/src/swetest.c">swetest.c</a>
 * <p>
 * Every call that reaches Swiss Ephemeris goes through {@link #sw}, an
 * {@link org.swisseph.ISwissEph} - the same 106 entry points {@code SwephExp} declares, as
 * instance methods rather than static ones, so this doubles as the largest worked example the
 * project has of using that layer directly: {@code double[]} out-parameters,
 * {@code StringBuilder} for {@code char*} buffers, and the {@code SE_*}/{@code SEFLG_*} flag
 * vocabulary.
 *
 * <h2>Two engines, one command line</h2>
 * {@code ISwissEph} has two implementations in {@code swe-java-lib}: the native
 * {@link org.swisseph.SwephNative} (the default here, and everywhere else in this workspace)
 * and the pure-Java {@code swisseph.SwissEph} port. {@link #swe_test(ISwissEph, String[])} runs
 * a command line against whichever one is handed to it, which is what
 * {@code SwissEphEngineComparisonTest} uses to run the same line through both and diff the
 * output - the whole reason this project, alone among the demo modules, depends on
 * {@code swe-java-lib} rather than {@code swe-api} alone.
 *
 * <h2>It is deliberately shaped like the C, not like idiomatic Java</h2>
 * The static fields below are swetest.c's own file-scope globals, under their own names; the
 * methods are its functions, under their own names ({@code print_line}, {@code dms},
 * {@code letter_to_ipl}, {@code do_special_event}, ...); the option parsing is the same
 * if/else chain in the same order. That is what makes the two files diffable side by side,
 * which is the only practical way to keep a port of this size honest as upstream moves.
 * <p>
 * The one structural difference: output is collected into {@link #out} rather than written to
 * stdout, so {@link #swe_test(String[])} can be called from a test and its result compared
 * with the real program's. {@link #main(String[])} prints it.
 *
 * <h2>Constants are generated, not imported</h2>
 * {@code swephexp.h}'s {@code SE_*}/{@code SEFLG_*} values are hand-generated locally by
 * {@code tools/extract-swetest-consts.py} rather than imported from {@code SweConst}
 * ({@code swe-java-lib}), because {@code swe-java-lib} was not on this project's classpath when
 * that generator was written, and duplicating one hundred already-correct constants a second
 * way was not worth undoing now that it is. Transcribing flag values by hand is how a demo ends
 * up quietly computing something else, which is the reason for generating them at all.
 *
 * <h2>What is not ported, and why</h2>
 * <ul>
 *   <li>the interactive prompt (swetest asks for a date on stdin when {@code -b} is absent).
 *       This hands its output back as a String and is meant to be callable from a test or a
 *       UI, where blocking on stdin is not an answer, so it says what is missing instead;</li>
 *   <li>{@code make_ephemeris_path()}'s CD-ROM-era search for ephemeris files, whose own
 *       comment in the C calls it an override of the library's simpler mechanism. Here
 *       {@code -edir} decides, defaulting to {@code ephe};</li>
 *   <li>the {@code -hocal} listing and a few Astrodienst-internal switches that exist to
 *       generate source code for other programs.</li>
 * </ul>
 *
 * @author Yura Krymlov
 * @version 1.0, 2026-09
 */
public class SweTest {

    // ------------------------------- swephexp.h's own values, generated, see tools/
    // extract-swetest-consts.py - swe-jni-demo depends on swe-api alone, so SweConst
    // (swe-java-lib) is not on the classpath and these cannot be imported.

    static final int SEFLG_BARYCTR = (16*1024);
    static final int SEFLG_CENTER_BODY = (1024*1024);
    static final int SEFLG_DPSIDEPS_1980 = (256*1024);
    static final int SEFLG_EQUATORIAL = (2*1024);
    static final int SEFLG_HELCTR = 8;
    static final int SEFLG_ICRS = (128*1024);
    static final int SEFLG_J2000 = 32;
    static final int SEFLG_JPLEPH = 1;
    static final int SEFLG_JPLHOR = SEFLG_DPSIDEPS_1980;
    static final int SEFLG_JPLHOR_APPROX = (512*1024);
    static final int SEFLG_MOSEPH = 4;
    static final int SEFLG_NOABERR = 1024;
    static final int SEFLG_NOGDEFL = 512;
    static final int SEFLG_NONUT = 64;
    static final int SEFLG_SIDEREAL = (64*1024);
    static final int SEFLG_SPEED = 256;
    static final int SEFLG_SPEED3 = 128;
    static final int SEFLG_SWIEPH = 2;
    static final int SEFLG_TOPOCTR = (32*1024);
    static final int SEFLG_TRUEPOS = 16;
    static final int SEFLG_XYZ = (4*1024);
    static final int SE_AST_OFFSET = 10000;
    static final double SE_AUNIT_TO_KM = (149597870.700);
    static final double SE_AUNIT_TO_LIGHTYEAR = (1.0/63241.07708427);
    static final int SE_BIT_DISC_BOTTOM = 8192;
    static final int SE_BIT_DISC_CENTER = 256;
    static final int SE_BIT_GEOCTR_NO_ECL_LAT = 128;
    static final int SE_BIT_NO_REFRACTION = 512;
    static final int SE_CALC_ITRANSIT = 8;
    static final int SE_CALC_MTRANSIT = 4;
    static final int SE_CALC_RISE = 1;
    static final int SE_CALC_SET = 2;
    static final int SE_CERES = 17;
    static final int SE_CHIRON = 15;
    static final int SE_CUPIDO = 40;
    static final int SE_EARTH = 14;
    static final int SE_ECL2HOR = 0;
    static final int SE_ECL_1ST_VISIBLE = 512;
    static final int SE_ECL_2ND_VISIBLE = 1024;
    static final int SE_ECL_3RD_VISIBLE = 2048;
    static final int SE_ECL_4TH_VISIBLE = 4096;
    static final int SE_ECL_ANNULAR = 8;
    static final int SE_ECL_ANNULAR_TOTAL = 32;
    static final int SE_ECL_CENTRAL = 1;
    static final int SE_ECL_NONCENTRAL = 2;
    static final int SE_ECL_NUT = -1;
    static final int SE_ECL_OCC_BEG_DAYLIGHT = 8192;
    static final int SE_ECL_OCC_END_DAYLIGHT = 16384;
    static final int SE_ECL_ONE_TRY = (32*1024);
    static final int SE_ECL_PARTBEG_VISIBLE = 512;
    static final int SE_ECL_PARTEND_VISIBLE = 4096;
    static final int SE_ECL_PARTIAL = 16;
    static final int SE_ECL_PENUMBBEG_VISIBLE = 8192;
    static final int SE_ECL_PENUMBEND_VISIBLE = 16384;
    static final int SE_ECL_PENUMBRAL = 64;
    static final int SE_ECL_TOTAL = 4;
    static final int SE_ECL_TOTBEG_VISIBLE = 1024;
    static final int SE_ECL_TOTEND_VISIBLE = 2048;
    static final int SE_EQU2HOR = 1;
    static final int SE_EVENING_FIRST = 3;
    static final int SE_FICT_MAX = 999;
    static final int SE_FICT_OFFSET_1 = 39;
    static final int SE_FIXSTAR = -10;
    static final String SE_FNAME_DE200 = "de200.eph";
    static final String SE_FNAME_DE431 = "de431.eph";
    static final String SE_FNAME_DFT = SE_FNAME_DE431;
    static final int SE_GREG_CAL = 1;
    static final int SE_HELFLAG_AV = (1 << 16);
    static final int SE_HELFLAG_OPTICAL_PARAMS = 512;
    static final int SE_HELIACAL_RISING = 1;
    static final int SE_HELIACAL_SETTING = 2;
    static final int SE_INTP_APOG = 21;
    static final int SE_INTP_PERG = 22;
    static final int SE_JUL_CAL = 0;
    static final int SE_JUNO = 19;
    static final int SE_MEAN_APOG = 12;
    static final int SE_MEAN_NODE = 10;
    static final int SE_MERCURY = 2;
    static final int SE_MOON = 1;
    static final int SE_MORNING_LAST = 4;
    static final int SE_NEPTUNE = 8;
    static final int SE_NODBIT_FOPOINT = 256;
    static final int SE_NODBIT_MEAN = 1;
    static final int SE_NODBIT_OSCU = 2;
    static final int SE_OSCU_APOG = 13;
    static final int SE_PALLAS = 18;
    static final int SE_PHOLUS = 16;
    static final int SE_PLMOON_OFFSET = 9000;
    static final int SE_PLUTO = 9;
    static final int SE_SIDBIT_ECL_T0 = 256;
    static final int SE_SIDBIT_SSY_PLANE = 512;
    static final int SE_SIDBIT_USER_UT = 1024;
    static final int SE_SIDM_FAGAN_BRADLEY = 0;
    static final int SE_SIDM_USER = 255;
    static final int SE_SPLIT_DEG_ROUND_MIN = 2;
    static final int SE_SPLIT_DEG_ROUND_SEC = 1;
    static final int SE_SUN = 0;
    static final int SE_TRUE_NODE = 11;
    static final int SE_VENUS = 3;
    static final int SE_VESTA = 20;
    static final int SE_WALDEMATH = 58;
    static final int SEFLG_TEST_PLMOON = (2*1024*1024 | SEFLG_J2000 | SEFLG_ICRS | SEFLG_HELCTR | SEFLG_TRUEPOS);
    static final int SE_BIT_HINDU_RISING = (SE_BIT_DISC_CENTER|SE_BIT_NO_REFRACTION|SE_BIT_GEOCTR_NO_ECL_LAT);
    static final int SE_ECL_ALLTYPES_LUNAR = (SE_ECL_TOTAL|SE_ECL_PARTIAL|SE_ECL_PENUMBRAL);
    static final int SE_ECL_ALLTYPES_SOLAR = (SE_ECL_CENTRAL|SE_ECL_NONCENTRAL|SE_ECL_TOTAL|SE_ECL_ANNULAR|SE_ECL_PARTIAL|SE_ECL_ANNULAR_TOTAL);

    // sweodef.h's return codes, and swetest.c's own mask over the three ephemeris bits
    static final int OK = 0;
    static final int ERR = -1;
    static final int SEFLG_EPHMASK = SEFLG_JPLEPH | SEFLG_SWIEPH | SEFLG_MOSEPH;

    // ------------------------------------------------------------------ swetest.c's #defines

    static final double J2000 = 2451545.0;

    static final int BIT_ROUND_SEC = 1;
    static final int BIT_ROUND_MIN = 2;
    static final int BIT_ZODIAC = 4;
    static final int BIT_LZEROES = 8;

    static final int BIT_TIME_LZEROES = 8;
    static final int BIT_TIME_LMT = 16;
    static final int BIT_TIME_LAT = 32;
    static final int BIT_ALLOW_361 = 64;

    static final String PLSEL_D = "0123456789mtA";
    static final String PLSEL_P = "0123456789mtABCcgDEFGHI";
    static final String PLSEL_H = "JKLMNOPQRSTUVWXYZw";
    static final String PLSEL_A = "0123456789mtABCcgDEFGHIJKLMNOPQRSTUVWXYZw";

    static final char DIFF_DIFF = 'd';
    static final char DIFF_GEOHEL = 'h';
    static final char DIFF_MIDP = 'D';

    static final int MODE_HOUSE = 1;
    static final int MODE_LABEL = 2;
    static final int MODE_AYANAMSA = 4;

    static final int SP_LUNAR_ECLIPSE = 1;
    static final int SP_SOLAR_ECLIPSE = 2;
    static final int SP_OCCULTATION = 3;
    static final int SP_RISE_SET = 4;
    static final int SP_MERIDIAN_TRANSIT = 5;
    static final int SP_HELIACAL = 6;

    static final int SP_MODE_HOW = 2;
    static final int SP_MODE_LOCAL = 8;
    static final int SP_MODE_HOCAL = 4096;

    static final int SEARCH_RANGE_LUNAR_CYCLES = 20000;

    /** the degree sign swetest prints; its ODEGREE_STRING is this one on every modern build */
    static final String ODEGREE_STRING = "°";

    static final String[] zod_nam = {"ar", "ta", "ge", "cn", "le", "vi",
            "li", "sc", "sa", "cp", "aq", "pi"};

    static final String[] hs_nam = {"undef", "Ascendant", "MC", "ARMC", "Vertex", "equat. Asc.",
            "co-Asc. W.Koch", "co-Asc Munkasey", "Polar Asc."};

    // ------------------------------------------------------- swetest.c's file-scope globals

    static String star = "algol", star2 = "";
    static String sastno = "433";
    static String spmoon = "9501";      // Jupiter Moon Io
    static String shyp = "1";

    static String fmt = "PLBRS";
    static String gap = " ";
    static double t, te, tut, jut = 0, tstep = 1;
    static int jmon, jday, jyear;
    static int ipl = SE_SUN, ipldiff = SE_SUN, nhouses = 12;
    static int iplctr = SE_SUN;
    static String spnam = "", spnam2 = "", se_pname = "";
    static final StringBuilder serr = new StringBuilder();
    static String serr_save = "", serr_warn = "";
    static int gregflag = SE_GREG_CAL;
    static boolean gregflag_auto = true;
    static char diff_mode = 0;
    static boolean use_dms = false;
    static boolean has_n = false;
    static boolean universal_time = false;
    static boolean universal_time_utc = false;
    static int round_flag = 0;
    static int time_flag = 0;
    static boolean short_output = false;
    static boolean list_hor = false;
    static int special_event = 0;
    static int special_mode = 0;
    static boolean do_orbital_elements = false;
    static boolean hel_using_AV = false;
    static boolean with_header = true;
    static boolean with_chart_link = false;
    static double[] x = new double[6], x2 = new double[6], xequ = new double[6],
            xcart = new double[6], xcartq = new double[6], xobl = new double[6],
            xaz = new double[6], xt = new double[6], xsv = new double[6];
    static double hpos, hpos2, hposj, armc;
    static int hpos_meth = 0;
    static double[] geopos = new double[10];
    static double[] attr = new double[20], tret = new double[20],
            datm = new double[4], dobs = new double[6];
    static int iflag = 0, iflag2;
    static int direction = 1;
    static boolean direction_flag = false;
    static boolean step_in_minutes = false, step_in_seconds = false;
    static boolean step_in_years = false, step_in_months = false;
    static int helflag = 0;
    static double tjd = 2415020.5;
    static int nstep = 1, istep;
    static int search_flag = 0;
    static int whicheph = SEFLG_SWIEPH;
    static char psp;                    // the object letter being printed, C's *psp
    static int norefrac = 0, disccenter = 0, discbottom = 0, hindu = 0;
    static String astro_models = "";
    static boolean do_set_astro_models = false;
    static String smod = "";
    static boolean inut = false;
    static boolean have_gap_parameter = false;
    static boolean use_swe_fixstar2 = false;
    static boolean output_extra_prec = false;
    static boolean show_file_limit = false;

    /** where every {@code printf} of the C ends up */
    static StringBuilder out = new StringBuilder();

    /** what -edir was given, or the directory this project ships; print_asteroids reads it */
    static String ephePath = "ephe";

    /**
     * The engine every {@code swe_*} call in this class reaches Swiss Ephemeris through.
     * {@link #swe_test(String[])} defaults it to a {@link SwephNative} the first time it runs,
     * so every existing caller (including {@code main()} and the tests already written against
     * the single-argument overload) keeps working unchanged; {@link #useSwissEph(ISwissEph)} and
     * {@link #swe_test(ISwissEph, String[])} are how a caller picks the pure-Java
     * {@code swisseph.SwissEph} instead - see the class javadoc.
     */
    static ISwissEph sw;

    /**
     * Selects the {@link ISwissEph} that {@link #swe_test(String[])} runs against from here on,
     * for callers that want to reuse the single-argument overload (or {@code main()}'s own
     * argument parsing) across more than one call. Most callers want
     * {@link #swe_test(ISwissEph, String[])} instead, which sets this and runs one command line
     * in a single call.
     */
    public static void useSwissEph(final ISwissEph swissEph) {
        sw = swissEph;
    }

    // --------------------------------------------------------------------------- the output

    static void p(String s) {
        out.append(s);
    }

    /**
     * C's {@code printf}. {@link Locale#ROOT} is not decoration: this machine's default locale
     * renders a decimal comma, which would make every number here differ from the reference
     * program's by punctuation alone.
     */
    static void pf(String format, Object... args) {
        out.append(cformat(format, args));
    }

    static final java.util.regex.Pattern SPEC = java.util.regex.Pattern.compile(
            "%([-+ 0#,(]*)(\\d+)?(?:\\.(\\d+))?([a-zA-Z%])");

    /**
     * {@code String.format} for everything except {@code %f}, which it formats the way C does.
     *
     * <h3>Why %f cannot be left to Java</h3>
     * Java formats a double from its <b>shortest representation</b> and pads with zeros beyond
     * it; C formats the <b>exact binary value</b>. Asking for more digits than the shortest
     * repr carries therefore gives two different answers for one double, and swetest asks for
     * exactly that - {@code %.9f} of a julian day:
     * <pre>
     *   C     TT:  2451545.000738761
     *   Java  TT:  2451545.000738760
     * </pre>
     * Neither is wrong about the double; they answer different questions. {@code BigDecimal}
     * of a double is that exact binary value, so rounding it reproduces the C - with
     * HALF_EVEN, which is the rounding mode C's printf uses by default.
     */
    static String cformat(String format, Object... args) {
        final StringBuilder s = new StringBuilder();
        final java.util.regex.Matcher m = SPEC.matcher(format);
        int at = 0, arg = 0;

        while (m.find()) {
            s.append(format, at, m.start());
            at = m.end();

            final char conv = m.group(4).charAt(0);
            if ('%' == conv) {
                s.append('%');
                continue;
            }
            if ('f' != conv) {
                Object v = args[arg++];
                // C's printf renders a NULL char* as "(null)" - swe_get_ayanamsa_name() returns
                // one for SE_SIDM_USER, and the JNI hands that across as a Java null, which
                // String.format would render as "null". One character, in the middle of a line.
                if (null == v && 's' == conv) v = "(null)";
                s.append(String.format(Locale.ROOT, m.group(), v));
                continue;
            }

            final String flags = m.group(1);
            final int width = null == m.group(2) ? 0 : Integer.parseInt(m.group(2));
            final int prec = null == m.group(3) ? 6 : Integer.parseInt(m.group(3));
            s.append(cdouble(((Number) args[arg++]).doubleValue(), flags, width, prec));
        }
        s.append(format.substring(at));
        return s.toString();
    }

    /** one {@code %f} conversion, C's way */
    static String cdouble(double v, String flags, int width, int prec) {
        String body;
        if (Double.isNaN(v)) {
            body = "nan";
        } else if (Double.isInfinite(v)) {
            body = v < 0 ? "-inf" : "inf";
        } else {
            // new BigDecimal(double) is the EXACT binary value - the whole point here
            body = new java.math.BigDecimal(v)
                    .setScale(prec, java.math.RoundingMode.HALF_EVEN).toPlainString();
            if (0 == prec && flags.indexOf('#') >= 0) body = body + ".";
            if (v >= 0) {
                if (flags.indexOf('+') >= 0) body = "+" + body;
                else if (flags.indexOf(' ') >= 0) body = " " + body;
            }
        }

        if (body.length() >= width) return body;
        final StringBuilder pad = new StringBuilder();
        for (int i = body.length(); i < width; i++) pad.append(flags.indexOf('0') >= 0 ? '0' : ' ');

        if (flags.indexOf('-') >= 0) return body + pad;
        if (flags.indexOf('0') >= 0 && !body.isEmpty()
                && ('-' == body.charAt(0) || '+' == body.charAt(0) || ' ' == body.charAt(0))) {
            // zero padding goes after the sign, never before it
            return body.charAt(0) + pad.toString() + body.substring(1);
        }
        return pad + body;
    }

    // ----------------------------------------------------------------------------- entry

    public static void main(String[] args) {
        SwephExp.loadSweCurrentLibrary();
        // sweodef.h's ODEGREE_STRING is UTF-8, and swetest puts the Windows console into
        // code page 65001 before printing it (SetConsoleOutputCP). The JVM's default here
        // is cp1251, which turns every degree sign into a question mark - so say it.
        final java.io.PrintStream stdout;
        try {
            stdout = new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out),
                    true, "UTF-8");
        } catch (java.io.UnsupportedEncodingException never) {
            throw new IllegalStateException(never);
        }
        // C's stdout is a TEXT stream: on Windows the CRT turns every LF into CRLF on
        // its way out, which is why swetest's own output has CRLF line endings there.
        // The port keeps LF internally - exactly the bytes swetest.c itself writes -
        // and does that translation here, at the one place the C runtime does it.
        stdout.print(swe_test(args).replace("\n", System.lineSeparator()));
        stdout.flush();
    }

    /**
     * {@link #swe_test(String[])} against a specific {@link ISwissEph} rather than whatever
     * {@link #useSwissEph(ISwissEph)} last set (or the default native engine, the first time).
     * This is the entry point {@code SwissEphEngineComparisonTest} drives both engines through.
     */
    public static String swe_test(final ISwissEph swissEph, final String[] argv) {
        useSwissEph(swissEph);
        return swe_test(argv);
    }

    /** swetest.c's {@code main()}, with its output returned rather than printed */
    public static String swe_test(String[] argv) {
        // swetest.c has no notion of a second engine, so its own main() has nothing to default -
        // this is the one line with no C original: SwephNative is what every other class in the
        // workspace defaults to, and every pre-existing caller of this overload expects the
        // native engine's numbers, so a caller that never touches useSwissEph() sees no change.
        if (null == sw) sw = new SwephNative(ephePath);
        out = new StringBuilder();
        resetGlobals();

        String sdate_save = "";
        String s1, s2;
        String sp, sp2;
        String plsel = PLSEL_D;
        int i, j, n, iflag_f = -1, iflgt;
        int line_count, line_limit = 36525;     // days in a century
        double[] daya = new double[1];
        double top_long = 0.0;                  // Greenwich UK
        double top_lat = 51.5;
        double top_elev = 0;
        boolean have_geopos = false;
        int ihsy = 'P';
        int year_start = 0, mon_start = 1, day_start = 1;
        boolean do_houses = false;
        String ephepath = "";
        String fname = SE_FNAME_DFT;
        String sdate;
        String begindate = null;
        String stimein = "";
        int iflgret;
        boolean is_first = true;
        boolean with_glp = false;
        boolean with_header_always = false;
        boolean do_ayanamsa = false;
        boolean do_planeto_centric = false;
        double aya_t0 = 0, aya_val0 = 0;
        boolean no_speed = false;
        int sid_mode = SE_SIDM_FAGAN_BRADLEY;
        double t2, thour = 0;
        double delt;
        double tid_acc = 0;
        double orb = 0.5;
        double astpos = -1;

        datm[0] = 1013.25; datm[1] = 15; datm[2] = 40; datm[3] = 0;

        for (i = 0; i < argv.length; i++) {
            final String a = argv[i];
            if (a.startsWith("-utc")) {
                universal_time = true;
                universal_time_utc = true;
                if (a.length() > 4) stimein = a.substring(4);
            } else if (a.startsWith("-ut")) {
                universal_time = true;
                if (a.length() > 3) stimein = a.substring(3);
            } else if (a.startsWith("-glp")) {
                with_glp = true;
            } else if (a.startsWith("-hor")) {
                list_hor = true;
            } else if (a.startsWith("-head")) {
                with_header = false;
            } else if (a.startsWith("+head")) {
                with_header_always = true;
            } else if (a.equals("-j2000")) {
                iflag |= SEFLG_J2000;
            } else if (a.equals("-icrs")) {
                iflag |= SEFLG_ICRS;
            } else if (a.equals("-cob")) {
                iflag |= SEFLG_CENTER_BODY;
            } else if (a.startsWith("-ay")) {
                do_ayanamsa = true;
                sid_mode = atoi(a.substring(3));
            } else if (a.startsWith("-sidt0")) {
                iflag |= SEFLG_SIDEREAL;
                sid_mode = atoi(a.substring(6));
                if (sid_mode == 0) sid_mode = SE_SIDM_FAGAN_BRADLEY;
                sid_mode |= SE_SIDBIT_ECL_T0;
            } else if (a.startsWith("-sidsp")) {
                iflag |= SEFLG_SIDEREAL;
                sid_mode = atoi(a.substring(6));
                if (sid_mode == 0) sid_mode = SE_SIDM_FAGAN_BRADLEY;
                sid_mode |= SE_SIDBIT_SSY_PLANE;
            } else if (a.startsWith("-sidudef")) {
                iflag |= SEFLG_SIDEREAL;
                sid_mode = SE_SIDM_USER;
                s1 = a.substring(8);
                aya_t0 = atof(s1);
                final int comma = s1.indexOf(',');
                if (comma >= 0) aya_val0 = atof(s1.substring(comma + 1));
                if (s1.contains("jdisut")) sid_mode |= SE_SIDBIT_USER_UT;
            } else if (a.startsWith("-sidbit")) {
                sid_mode |= atoi(a.substring(7));
            } else if (a.startsWith("-sid")) {
                iflag |= SEFLG_SIDEREAL;
                sid_mode = atoi(a.substring(4));
            } else if (a.equals("-jplhora")) {
                iflag |= SEFLG_JPLHOR_APPROX;
            } else if (a.equals("-tpm")) {
                iflag |= SEFLG_TEST_PLMOON;
            } else if (a.equals("-jplhor")) {
                iflag |= SEFLG_JPLHOR;
            } else if (a.startsWith("-j")) {
                begindate = a.substring(1);
            } else if (a.startsWith("-ejpl")) {
                whicheph = SEFLG_JPLEPH;
                if (a.length() > 5) fname = a.substring(5);
            } else if (a.startsWith("-edir")) {
                if (a.length() > 5) ephepath = a.substring(5);
            } else if (a.equals("-eswe")) {
                whicheph = SEFLG_SWIEPH;
            } else if (a.equals("-emos")) {
                whicheph = SEFLG_MOSEPH;
            } else if (a.startsWith("-helflag")) {
                helflag = atoi(a.substring(8));
                if (helflag >= SE_HELFLAG_AV) hel_using_AV = true;
            } else if (a.equals("-hel")) {
                iflag |= SEFLG_HELCTR;
            } else if (a.equals("-bary")) {
                iflag |= SEFLG_BARYCTR;
            } else if (a.startsWith("-house")) {
                sp = strip(a.substring(6));
                final String[] f = sp.split(",");
                final int nf = scanned(f);
                if (nf > 0) top_long = atof(f[0]);
                if (nf > 1) top_lat = atof(f[1]);
                top_elev = 0;
                if (nf > 1 && f.length > 2 && !f[2].isEmpty()) ihsy = f[2].charAt(0);
                do_houses = true;
                have_geopos = true;
            } else if (a.startsWith("-hsy")) {
                sp = a.substring(4);
                ihsy = sp.isEmpty() ? 'P' : sp.charAt(0);
                if (sp.length() > 1) hpos_meth = atoi(sp.substring(1));
                have_geopos = true;
            } else if (a.startsWith("-topo")) {
                iflag |= SEFLG_TOPOCTR;
                final String[] f = strip(a.substring(5)).split(",");
                final int nf = scanned(f);
                if (nf > 0) top_long = atof(f[0]);
                if (nf > 1) top_lat = atof(f[1]);
                if (nf > 2) top_elev = atof(f[2]);
                have_geopos = true;
            } else if (a.startsWith("-geopos")) {
                final String[] f = strip(a.substring(7)).split(",");
                final int nf = scanned(f);
                if (nf > 0) top_long = atof(f[0]);
                if (nf > 1) top_lat = atof(f[1]);
                if (nf > 2) top_elev = atof(f[2]);
                have_geopos = true;
            } else if (a.equals("-true")) {
                iflag |= SEFLG_TRUEPOS;
            } else if (a.equals("-noaberr")) {
                iflag |= SEFLG_NOABERR;
            } else if (a.equals("-nodefl")) {
                iflag |= SEFLG_NOGDEFL;
            } else if (a.equals("-nonut")) {
                iflag |= SEFLG_NONUT;
            } else if (a.equals("-speed3")) {
                iflag |= SEFLG_SPEED3;
            } else if (a.equals("-speed")) {
                iflag |= SEFLG_SPEED;
            } else if (a.equals("-nospeed")) {
                no_speed = true;
            } else if (a.startsWith("-testaa")) {
                // Astronomical Almanac test cases: DE200 through the JPL reader, Mars, ET, and
                // the AA's own columns. The two-digit suffix picks the AA year's example date.
                whicheph = SEFLG_JPLEPH;
                fname = SE_FNAME_DE200;
                final String aa = a.substring(7);
                if (aa.equals("95")) begindate = "j2449975.5";
                if (aa.equals("96")) begindate = "j2450442.5";
                if (aa.equals("97")) begindate = "j2450482.5";
                fmt = "PADRu";
                universal_time = false;
                plsel = "3";
            } else if (a.startsWith("-lmt")) {
                universal_time = true;
                time_flag |= BIT_TIME_LMT;
                if (a.length() > 4) stimein = a.substring(4);
            } else if (a.equals("-lat")) {
                universal_time = true;
                time_flag |= BIT_TIME_LAT;
            } else if (a.equals("-lim")) {
                show_file_limit = true;
            } else if (a.equals("-clink")) {
                with_chart_link = true;
            } else if (a.equals("-lunecl")) {
                special_event = SP_LUNAR_ECLIPSE;
            } else if (a.equals("-solecl")) {
                special_event = SP_SOLAR_ECLIPSE;
                have_geopos = true;
            } else if (a.equals("-short")) {
                short_output = true;
            } else if (a.equals("-occult")) {
                special_event = SP_OCCULTATION;
                have_geopos = true;
            } else if (a.equals("-ep")) {
                output_extra_prec = true;
            } else if (a.equals("-hocal")) {
                special_mode |= SP_MODE_HOCAL;
            } else if (a.equals("-how")) {
                special_mode |= SP_MODE_HOW;
            } else if (a.equals("-total")) {
                search_flag |= SE_ECL_TOTAL;
            } else if (a.equals("-annular")) {
                search_flag |= SE_ECL_ANNULAR;
            } else if (a.equals("-anntot")) {
                search_flag |= SE_ECL_ANNULAR_TOTAL;
            } else if (a.equals("-partial")) {
                search_flag |= SE_ECL_PARTIAL;
            } else if (a.equals("-penumbral")) {
                search_flag |= SE_ECL_PENUMBRAL;
            } else if (a.equals("-noncentral")) {
                search_flag &= ~SE_ECL_CENTRAL;
                search_flag |= SE_ECL_NONCENTRAL;
            } else if (a.equals("-central")) {
                search_flag &= ~SE_ECL_NONCENTRAL;
                search_flag |= SE_ECL_CENTRAL;
            } else if (a.equals("-local")) {
                special_mode |= SP_MODE_LOCAL;
            } else if (a.equals("-rise")) {
                special_event = SP_RISE_SET;
                have_geopos = true;
            } else if (a.equals("-norefrac")) {
                norefrac = 1;
            } else if (a.equals("-disccenter")) {
                disccenter = 1;
            } else if (a.equals("-hindu")) {
                hindu = 1;
                norefrac = 1;
                disccenter = 1;
            } else if (a.equals("-discbottom")) {
                discbottom = 1;
            } else if (a.equals("-metr")) {
                special_event = SP_MERIDIAN_TRANSIT;
                have_geopos = true;
            } else if (a.startsWith("-amod")) {
                astro_models = a.substring(5);
                do_set_astro_models = true;
            } else if (a.startsWith("-tidacc")) {
                tid_acc = atof(a.substring(7));
            } else if (a.startsWith("-hev")) {
                special_event = SP_HELIACAL;
                search_flag = 0;
                sp = strip(a.substring(4));
                if (!sp.isEmpty()) search_flag = atoi(sp);
                have_geopos = true;
                if (a.contains("AV")) hel_using_AV = true;
            } else if (a.startsWith("-at")) {
                final String[] f = strip(a.substring(3)).split(",");
                for (j = 0; j < 4 && j < scanned(f); j++) datm[j] = atof(f[j]);
            } else if (a.startsWith("-obs")) {
                final String[] f = strip(a.substring(4)).split(",");
                final int nf = scanned(f);
                if (nf > 0) dobs[0] = atof(f[0]);
                if (nf > 1) dobs[1] = atof(f[1]);
            } else if (a.startsWith("-opt")) {
                final String[] f = strip(a.substring(4)).split(",");
                for (j = 0; j < 6 && j < scanned(f); j++) dobs[j] = atof(f[j]);
            } else if (a.startsWith("-astpos")) {
                final String[] f = a.substring(7).split(",");
                final int nf = scanned(f);
                if (nf > 0) {
                    final double x1 = atof(f[0]);
                    if (x1 >= 0 && x1 < 360) astpos = x1;
                }
                if (nf > 1) {
                    final double x2v = atof(f[1]);
                    if (x2v >= 0 && x2v <= 1) orb = x2v;
                }
            } else if (a.equals("-orbel")) {
                do_orbital_elements = true;
            } else if (a.equals("-bwd")) {
                direction = -1;
                direction_flag = true;
            } else if (a.startsWith("-pc")) {
                iplctr = atoi(a.substring(3));
                do_planeto_centric = true;
            } else if (a.startsWith("-p")) {
                final String spno = a.substring(2);
                final char c0 = spno.isEmpty() ? '\0' : spno.charAt(0);
                switch (c0) {
                    case 'd': plsel = PLSEL_D; break;
                    case 'p': plsel = PLSEL_P; break;
                    case 'h': plsel = PLSEL_H; break;
                    case 'a': plsel = PLSEL_A; break;
                    default:  plsel = spno;
                }
            } else if (a.startsWith("-xs")) {
                sastno = a.substring(3);
            } else if (a.startsWith("-xv")) {
                spmoon = a.substring(3);
            } else if (a.startsWith("-xf")) {
                star = a.substring(3);
            } else if (a.startsWith("-xz")) {
                shyp = a.substring(3);
            } else if (a.startsWith("-x")) {
                star = a.substring(2);
            } else if (a.equals("-nut")) {
                inut = true;
            } else if (a.startsWith("-n")) {
                nstep = atoi(a.substring(2));
                has_n = true;
                if (nstep == 0) nstep = 20;
            } else if (a.startsWith("-i")) {
                iflag_f = atoi(a.substring(2));
                if ((iflag_f & SEFLG_XYZ) != 0) fmt = "PX";
            } else if (a.equals("-swefixstar2")) {
                use_swe_fixstar2 = true;
            } else if (a.startsWith("-s")) {
                tstep = atof(a.substring(2));
                final char last = a.charAt(a.length() - 1);
                if ('m' == last) step_in_minutes = true;
                if ('s' == last) step_in_seconds = true;
                if ('y' == last) step_in_years = true;
                if ('o' == last) { step_in_minutes = false; step_in_months = true; }
            } else if (a.startsWith("-b")) {
                begindate = a.substring(2);
            } else if (a.startsWith("-f")) {
                fmt = a.substring(2);
            } else if (a.startsWith("-g")) {
                gap = a.substring(2);
                have_gap_parameter = true;
                if (gap.isEmpty()) gap = "\t";
            } else if (a.equals("-dms")) {
                use_dms = true;
            } else if (a.startsWith("-d") || a.startsWith("-D")) {
                diff_mode = a.charAt(1);        // 'd' or 'D'
                sp = a.substring(2);
                if (sp.startsWith("h")) {
                    sp = sp.substring(1);
                    diff_mode = DIFF_GEOHEL;
                }
                ipldiff = letter_to_ipl(sp.isEmpty() ? '\0' : sp.charAt(0));
                if (ipldiff < 0) ipldiff = SE_SUN;
                spnam2 = sw.swe_get_planet_name(ipldiff);
            } else if (a.equals("-roundsec")) {
                round_flag |= BIT_ROUND_SEC;
            } else if (a.equals("-roundmin")) {
                round_flag |= BIT_ROUND_MIN;
            } else if (a.startsWith("-t")) {
                if (a.length() > 2) stimein = stimein + a.substring(2);
            } else if (a.startsWith("-h") || a.startsWith("-?")) {
                return SweTestInfo.help(a.length() > 2 ? a.charAt(2) : '\0');
            } else {
                pf("illegal option %s\n", a);
                return out.toString();
            }
        }

        if (special_event == SP_OCCULTATION || special_event == SP_RISE_SET
                || special_event == SP_MERIDIAN_TRANSIT || special_event == SP_HELIACAL) {
            final char c0 = plsel.isEmpty() ? '\0' : plsel.charAt(0);
            ipl = letter_to_ipl(c0);
            if ('f' == c0) {
                ipl = SE_FIXSTAR;
            } else {
                if ('s' == c0) ipl = atoi(sastno) + SE_AST_OFFSET;
                star = "";
            }
            if (special_event == SP_OCCULTATION && ipl == 1)
                ipl = 2;    /* no occultation of moon by moon */
        }

        if (!stimein.isEmpty()) {
            double tt = 0;
            final int colon = stimein.indexOf(':');
            if (colon >= 0) {
                final int colon2 = stimein.indexOf(':', colon + 1);
                if (colon2 >= 0) tt += atof(stimein.substring(colon2 + 1)) / 60.0;
                tt += atoi(stimein.substring(colon + 1));
                tt /= 60.0;
            }
            if (atoi(stimein) < 0) tt = -tt;
            tt += atoi(stimein);
            thour = tt;
        }

        if (with_header) {
            for (String s : argv) { p(s); p(" "); }
        }

        iflag = (iflag & ~SEFLG_EPHMASK) | whicheph;
        if (strpbrk(fmt, "SsQ") && (iflag & SEFLG_SPEED3) == 0 && !no_speed)
            iflag |= SEFLG_SPEED;

        // swetest.c hunts for the ephemeris here (make_ephemeris_path); this takes -edir, or
        // the directory the project ships, which is what its own tests and demos use
        if (ephepath.isEmpty()) ephepath = "ephe";
        ephePath = ephepath;
        if (whicheph != SEFLG_MOSEPH) sw.swe_set_ephe_path(ephepath);
        if ((whicheph & SEFLG_JPLEPH) != 0) sw.swe_set_jpl_file(fname);

        if (do_set_astro_models) {
            sw.swe_set_astro_models(new StringBuilder(astro_models), iflag);
            final StringBuilder sdet = new StringBuilder();
            sw.swe_get_astro_models(new StringBuilder(astro_models), sdet, iflag);
            smod = sdet.toString();
        }
        // the legacy signature takes AS_BOOL as an int, not a Java boolean
        if (inut) sw.swe_set_interpolate_nut(1);

        if ((iflag & SEFLG_SIDEREAL) != 0 || do_ayanamsa) {
            if ((sid_mode & SE_SIDM_USER) != 0)
                sw.swe_set_sid_mode(sid_mode, aya_t0, aya_val0);
            else
                sw.swe_set_sid_mode(sid_mode, 0, 0);
        }

        geopos[0] = top_long;
        geopos[1] = top_lat;
        geopos[2] = top_elev;
        sw.swe_set_topo(top_long, top_lat, top_elev);
        if (tid_acc != 0) sw.swe_set_tid_acc(tid_acc);
        serr.setLength(0);
        serr_save = "";
        serr_warn = "";

        if (null == begindate) {
            return SweTestInfo.usage();
        }
        sdate = begindate;

        // ------------------------------------------------------------------ the date itself
        final int[] jd = new int[3];
        final double[] jt = new double[1];

        if (sdate.startsWith("j")) {                    /* it's a day number */
            tjd = atof(sdate.substring(1).replace(',', '.'));
            gregflag = tjd < 2299160.5 ? SE_JUL_CAL : SE_GREG_CAL;
            if (sdate.contains("jul")) { gregflag = SE_JUL_CAL; gregflag_auto = false; }
            else if (sdate.contains("greg")) { gregflag = SE_GREG_CAL; gregflag_auto = false; }
            sw.swe_revjul(tjd, gregflag, jd, jt);
            jyear = jd[0]; jmon = jd[1]; jday = jd[2]; jut = jt[0];
            year_start = jyear; mon_start = jmon; day_start = jday;
        } else {
            final int[] dmy = scanDate(sdate);
            if (null == dmy) return out.toString();
            jday = dmy[0]; jmon = dmy[1]; jyear = dmy[2];
            year_start = jyear; mon_start = jmon; day_start = jday;
            gregflag = ((long) jyear * 10000L + (long) jmon * 100L + jday < 15821015L)
                    ? SE_JUL_CAL : SE_GREG_CAL;
            if (sdate.contains("jul")) { gregflag = SE_JUL_CAL; gregflag_auto = false; }
            else if (sdate.contains("greg")) { gregflag = SE_GREG_CAL; gregflag_auto = false; }
            jut = 0;
            if (universal_time_utc) {
                int ih = 0, im = 0;
                double ds = 0.0;
                if (!stimein.isEmpty()) {
                    final String[] hms = stimein.split(":");
                    if (hms.length > 0) ih = atoi(hms[0]);
                    if (hms.length > 1) im = atoi(hms[1]);
                    if (hms.length > 2) ds = atof(hms[2]);
                }
                final double[] dret = new double[2];
                if (sw.swe_utc_to_jd(jyear, jmon, jday, ih, im, ds, gregflag, dret, serr) == ERR) {
                    pf(" error in swe_utc_to_jd(): %s\n", serr);
                    return out.toString();
                }
                tjd = dret[1];
            } else {
                tjd = sw.swe_julday(jyear, jmon, jday, jut, gregflag);
                tjd += thour / 24.0;
                jut = thour;
            }
        }

        if (special_event > 0) {
            do_special_event(tjd, ipl, star, special_event, special_mode, geopos, datm, dobs);
            sw.swe_close();
            return out.toString();
        }

        line_count = 0;
        for (t = tjd, istep = 1; istep <= nstep; t += tstep, istep++) {
            if (step_in_minutes) t = tjd + (istep - 1) * tstep / 1440;
            if (step_in_seconds) t = tjd + (istep - 1) * tstep / 86400;
            if (step_in_years)
                t = sw.swe_julday(year_start + (istep - 1) * (int) tstep, mon_start, day_start, jut, gregflag);
            if (step_in_months) {
                jmon = mon_start + (istep - 1) * (int) tstep;
                jyear = year_start + (jmon - 1) / 12;
                jmon = ((jmon - 1) % 12) + 1;
                t = sw.swe_julday(jyear, jmon, day_start, jut, gregflag);
            }
            if (gregflag_auto) gregflag = t < 2299160.5 ? SE_JUL_CAL : SE_GREG_CAL;
            // must repeat because gregflag may have changed
            if (step_in_years)
                t = sw.swe_julday(year_start + (istep - 1) * (int) tstep, mon_start, day_start, jut, gregflag);
            if (step_in_months) {
                jmon = mon_start + (istep - 1) * (int) tstep;
                jyear = year_start + (jmon - 1) / 12;
                jmon = ((jmon - 1) % 12) + 1;
                t = sw.swe_julday(jyear, jmon, day_start, jut, gregflag);
            }

            delt = sw.swe_deltat_ex(t, iflag, serr);
            if (!universal_time) delt = sw.swe_deltat_ex(t - delt, iflag, serr);
            t2 = t;
            sw.swe_revjul(t2, gregflag, jd, jt);
            jyear = jd[0]; jmon = jd[1]; jday = jd[2]; jut = jt[0];

            if (with_header) {
                if (with_glp) pf("\npath: %s", sw.swe_get_library_path());
                pf("\ndate (dmy) %d.%d.%04d", jday, jmon, jyear);
                p(gregflag != 0 ? " greg." : " jul.");
                p(jd_to_time_string(jut));
                if (universal_time) {
                    p((time_flag & BIT_TIME_LMT) != 0 ? " LMT" : " UT");
                } else {
                    p(" TT");
                }
                pf("\t\tversion %s", sw.swe_version());
            }

            if (universal_time) {
                if ((time_flag & BIT_TIME_LMT) != 0) {
                    if (with_header) {
                        pf("\nLMT: %.9f", t);
                        t -= geopos[0] / 15.0 / 24.0;
                    }
                }
                if (with_header) {
                    pf("\nUT:  %.9f", t);
                    pf("     delta t: %f sec", delt * 86400.0);
                }
                te = t + delt;
                tut = t;
            } else {
                te = t;
                tut = t - delt;
                if (with_header) {
                    pf("\nUT:  %.9f", tut);
                    pf("     delta t: %f sec", delt * 86400.0);
                }
            }

            sw.swe_calc(te, SE_ECL_NUT, iflag, xobl, serr);

            if (with_header) {
                pf("\nTT:  %.9f", te);
                if ((iflag & SEFLG_SIDEREAL) != 0) {
                    if (sw.swe_get_ayanamsa_ex(te, iflag, daya, serr) == ERR) {
                        pf("   error in swe_get_ayanamsa_ex(): %s\n", serr);
                        return out.toString();
                    }
                    pf("   ayanamsa = %s (%s)", dms(daya[0], round_flag),
                            sw.swe_get_ayanamsa_name(sid_mode));
                }
                if (have_geopos)
                    pf("\ngeo. long %f, lat %f, alt %f", geopos[0], geopos[1], geopos[2]);
                if (iflag_f >= 0) iflag = iflag_f;
                if (plsel.indexOf('o') < 0) {
                    if ((iflag & (SEFLG_NONUT | SEFLG_SIDEREAL)) != 0) {
                        pf("\n%-15s %s", "Epsilon (m)", dms(xobl[0], round_flag));
                    } else {
                        pf("\n%-15s %s%s", "Epsilon (t/m)", dms(xobl[0], round_flag), gap);
                        p(dms(xobl[1], round_flag));
                    }
                }
                if (plsel.indexOf('n') < 0 && (iflag & (SEFLG_NONUT | SEFLG_SIDEREAL)) == 0) {
                    p("\nNutation        ");
                    p(dms(xobl[2], round_flag));
                    p(gap);
                    p(dms(xobl[3], round_flag));
                }
                p("\n");
                if (do_houses) {
                    if (!universal_time) {
                        do_houses = false;
                        p("option -house requires option -ut for Universal Time\n");
                    } else {
                        s1 = dms(top_long, round_flag);
                        s2 = dms(top_lat, round_flag);
                        pf("Houses system %c (%s) for long=%s, lat=%s\n",
                                (char) ihsy, sw.swe_house_name(ihsy), s1, s2);
                    }
                }
            }
            if (with_header && !with_header_always) with_header = false;

            if (astpos >= 0) {
                print_asteroids(tjd, astpos, orb);
                sw.swe_close();
                return out.toString();
            }

            if (do_ayanamsa) {
                if (sw.swe_get_ayanamsa_ex(te, iflag, daya, serr) == ERR) {
                    pf("   error in swe_get_ayanamsa_ex(): %s\n", serr);
                    return out.toString();
                }
                x[0] = daya[0];
                print_line(MODE_AYANAMSA, true, sid_mode);
                continue;
            }

            if (t == tjd && plsel.indexOf('e') >= 0) {
                if (list_hor) {
                    is_first = true;
                    for (int k = 0; k < plsel.length(); k++) {
                        psp = plsel.charAt(k);
                        if ('e' == psp) continue;
                        ipl = letter_to_ipl(psp);
                        spnam = "";
                        if (ipl >= SE_SUN && ipl <= SE_VESTA)
                            spnam = sw.swe_get_planet_name(ipl);
                        print_line(MODE_LABEL, is_first, 0);
                        is_first = false;
                    }
                    p("\n");
                } else {
                    print_line(MODE_LABEL, true, 0);
                }
            }

            is_first = true;
            for (int k = 0; k < plsel.length(); k++) {
                psp = plsel.charAt(k);
                if ('e' == psp) continue;
                ipl = letter_to_ipl(psp);
                if (ipl == -2) {
                    pf("illegal parameter -p%s\n", plsel);
                    return out.toString();
                }
                if ('f' == psp) ipl = SE_FIXSTAR;
                else if ('s' == psp) ipl = atoi(sastno) + 10000;
                else if ('v' == psp) ipl = atoi(spmoon);
                else if ('z' == psp) ipl = atoi(shyp) + SE_FICT_OFFSET_1;

                if ((iflag & SEFLG_HELCTR) != 0) {
                    if (ipl == SE_SUN || ipl == SE_MEAN_NODE || ipl == SE_TRUE_NODE
                            || ipl == SE_MEAN_APOG || ipl == SE_OSCU_APOG) continue;
                } else if ((iflag & SEFLG_BARYCTR) != 0) {
                    if (ipl == SE_MEAN_NODE || ipl == SE_TRUE_NODE
                            || ipl == SE_MEAN_APOG || ipl == SE_OSCU_APOG) continue;
                } else {        /* geocentric */
                    if (ipl == SE_EARTH && !do_orbital_elements) continue;
                }

                /* ecliptic position */
                if (iflag_f >= 0) iflag = iflag_f;
                if (ipl == SE_FIXSTAR) {
                    iflgret = call_swe_fixstar(star, te, iflag, x);
                    if (iflgret != ERR && strpbrk(fmt, "=")) {
                        final double[] mag = new double[1];
                        final StringBuilder sn = new StringBuilder(star);
                        sw.swe_fixstar_mag(sn, mag, serr);
                        attr[4] = mag[0];
                    }
                    se_pname = star;
                } else if (do_planeto_centric) {
                    iflgret = sw.swe_calc_pctr(te, ipl, iplctr, iflag, x, serr);
                    se_pname = sw.swe_get_planet_name(ipl);
                } else {
                    iflgret = sw.swe_calc(te, ipl, iflag, x, serr);
                    if (iflgret != ERR && strpbrk(fmt, "+-*/="))
                        iflgret = sw.swe_pheno(te, ipl, iflag, attr, serr);
                    se_pname = sw.swe_get_planet_name(ipl);
                    if (show_file_limit) {
                        final double[] tfstart = new double[1], tfend = new double[1];
                        final int[] denum = new int[1];
                        int ifno = 3;
                        if (ipl == SE_SUN || (ipl >= SE_MERCURY && ipl < SE_CHIRON)) ifno = 0;
                        else if (ipl == SE_MOON) ifno = 1;
                        else if (ipl <= SE_VESTA) ifno = 2;
                        final String fnam = sw.swe_get_current_file_data(ifno, tfstart, tfend, denum);
                        if (null != fnam && !fnam.isEmpty()) {
                            sw.swe_revjul(tfstart[0], gregflag, jd, jt);
                            final String sbeg = String.format(Locale.ROOT, "%d.%02d.%04d", jd[2], jd[1], jd[0]);
                            sw.swe_revjul(tfend[0], gregflag, jd, jt);
                            final String send = String.format(Locale.ROOT, "%d.%02d.%04d", jd[2], jd[1], jd[0]);
                            pf("range %s: %.1f = %s to %.1f = %s de=%d\n",
                                    fnam, tfstart[0], sbeg, tfend[0], send, denum[0]);
                            show_file_limit = false;
                        }
                    }
                }

                if ('q' == psp) {       /* delta t */
                    x[0] = sw.swe_deltat_ex(tut, iflag, serr) * 86400;
                    x[1] = x[2] = x[3] = 0;
                    x[1] = x[0] / 3600.0;   // to hours
                    se_pname = "Delta T";
                }
                if ('x' == psp) {       /* sidereal time */
                    x[0] = sw.swe_degnorm(sw.swe_sidtime(tut) * 15 + geopos[0]);
                    x[1] = x[2] = x[3] = 0;
                    se_pname = "Sidereal Time";
                }
                if ('o' == psp) {       /* ecliptic is wanted, remove nutation */
                    x[2] = x[3] = 0;
                    se_pname = "Ecl. Obl.";
                }
                if ('n' == psp) {       /* nutation is wanted, remove ecliptic */
                    x[0] = x[2];
                    x[1] = x[3];
                    x[2] = x[3] = 0;
                    se_pname = "Nutation";
                }
                if ('y' == psp) {       /* time equation */
                    final double[] teq = new double[1];
                    iflgret = sw.swe_time_equ(tut, teq, serr);
                    x[0] = teq[0] * 86400;  /* in seconds */
                    x[1] = x[2] = x[3] = 0;
                    se_pname = "Time Equ.";
                }
                if ('b' == psp) {       /* ayanamsha */
                    if (sw.swe_get_ayanamsa_ex(te, iflag, daya, serr) == ERR) {
                        pf("   error in swe_get_ayanamsa_ex(): %s\n", serr);
                        iflgret = -1;
                    }
                    x[0] = daya[0];
                    x[1] = 0;
                    se_pname = "Ayanamsha";
                }

                if (iflgret < 0) {
                    if (!serr.toString().equals(serr_save)
                            && (ipl == SE_SUN || ipl == SE_MOON || ipl <= SE_PLUTO
                            || ipl == SE_MEAN_NODE || ipl == SE_TRUE_NODE
                            || ipl == SE_CERES || ipl == SE_PALLAS || ipl == SE_JUNO || ipl == SE_VESTA
                            || ipl == SE_CHIRON || ipl == SE_PHOLUS || ipl == SE_CUPIDO
                            || (ipl > SE_FICT_OFFSET_1 && ipl <= SE_FICT_MAX)
                            || ipl >= SE_PLMOON_OFFSET
                            || ipl >= SE_AST_OFFSET || ipl == SE_FIXSTAR
                            || 'y' == psp)) {
                        p("error: ");
                        p(serr.toString());
                        p("\n");
                    }
                    serr_save = serr.toString();
                } else if (serr.length() != 0 && serr_warn.isEmpty()) {
                    if (!serr.toString().contains("'seorbel.txt' not found"))
                        serr_warn = serr.toString();
                }

                if (diff_mode != 0) {
                    sw.swe_calc(te, ipldiff, iflag, x2, serr);
                    if (diff_mode == DIFF_GEOHEL)
                        sw.swe_calc(te, ipldiff, iflag | SEFLG_HELCTR, x2, serr);
                    if (diff_mode == DIFF_DIFF || diff_mode == DIFF_GEOHEL) {
                        for (i = 1; i < 6; i++) x[i] -= x2[i];
                        x[0] = sw.swe_difdeg2n(x[0], x2[0]);
                    } else {    /* DIFF_MIDP */
                        for (i = 1; i < 6; i++) x[i] = (x[i] + x2[i]) / 2;
                        x[0] = sw.swe_deg_midp(x[0], x2[0]);
                    }
                }

                /* equator position */
                if (strpbrk(fmt, "aADdQmzx")) {
                    iflag2 = iflag | SEFLG_EQUATORIAL;
                    if (ipl == SE_FIXSTAR) call_swe_fixstar(star, te, iflag2, xequ);
                    else if (do_planeto_centric) sw.swe_calc_pctr(te, ipl, iplctr, iflag2, xequ, serr);
                    else sw.swe_calc(te, ipl, iflag2, xequ, serr);
                    if (diff_mode != 0) {
                        sw.swe_calc(te, ipldiff, iflag2, x2, serr);
                        if (diff_mode == DIFF_DIFF || diff_mode == DIFF_GEOHEL) {
                            if (diff_mode == DIFF_GEOHEL)
                                sw.swe_calc(te, ipldiff, iflag2 | SEFLG_HELCTR, x2, serr);
                            for (i = 1; i < 6; i++) xequ[i] -= x2[i];
                            xequ[0] = sw.swe_difdeg2n(xequ[0], x2[0]);
                        } else {
                            for (i = 1; i < 6; i++) xequ[i] = (xequ[i] + x2[i]) / 2;
                            xequ[0] = sw.swe_deg_midp(xequ[0], x2[0]);
                        }
                    }
                }

                /* azimuth and height */
                if (strpbrk(fmt, "IiHhKk")) {
                    iflgt = whicheph | SEFLG_EQUATORIAL | SEFLG_TOPOCTR;
                    if (ipl == SE_FIXSTAR) call_swe_fixstar(star, te, iflgt, xt);
                    else sw.swe_calc(te, ipl, iflgt, xt, serr);
                    sw.swe_azalt(tut, SE_EQU2HOR, geopos, datm[0], datm[1], xt, xaz);
                    if (diff_mode != 0) {
                        sw.swe_calc(te, ipldiff, iflgt, xt, serr);
                        sw.swe_azalt(tut, SE_EQU2HOR, geopos, datm[0], datm[1], xt, x2);
                        if (diff_mode == DIFF_DIFF || diff_mode == DIFF_GEOHEL) {
                            if (diff_mode == DIFF_GEOHEL) {
                                sw.swe_calc(te, ipldiff, iflgt | SEFLG_HELCTR, xt, serr);
                                sw.swe_azalt(tut, SE_EQU2HOR, geopos, datm[0], datm[1], xt, x2);
                            }
                            for (i = 1; i < 3; i++) xaz[i] -= x2[i];
                            xaz[0] = sw.swe_difdeg2n(xaz[0], x2[0]);
                        } else {
                            for (i = 1; i < 3; i++) xaz[i] = (xaz[i] + x2[i]) / 2;
                            xaz[0] = sw.swe_deg_midp(xaz[0], x2[0]);
                        }
                    }
                }

                /* ecliptic cartesian position */
                if (strpbrk(fmt, "XU")) {
                    iflag2 = iflag | SEFLG_XYZ;
                    if (ipl == SE_FIXSTAR) call_swe_fixstar(star, te, iflag2, xcart);
                    else if (do_planeto_centric) sw.swe_calc_pctr(te, ipl, iplctr, iflag2, xcart, serr);
                    else sw.swe_calc(te, ipl, iflag2, xcart, serr);
                    if (diff_mode != 0) {
                        sw.swe_calc(te, ipldiff, iflag2, x2, serr);
                        if (diff_mode == DIFF_DIFF || diff_mode == DIFF_GEOHEL) {
                            if (diff_mode == DIFF_GEOHEL)
                                sw.swe_calc(te, ipldiff, iflag2 | SEFLG_HELCTR, x2, serr);
                            for (i = 0; i < 6; i++) xcart[i] -= x2[i];
                        }
                    }
                }

                /* equator cartesian position */
                if (strpbrk(fmt, "xu")) {
                    iflag2 = iflag | SEFLG_XYZ | SEFLG_EQUATORIAL;
                    if (ipl == SE_FIXSTAR) call_swe_fixstar(star, te, iflag2, xcartq);
                    else if (do_planeto_centric) sw.swe_calc_pctr(te, ipl, iplctr, iflag2, xcartq, serr);
                    else sw.swe_calc(te, ipl, iflag2, xcartq, serr);
                    if (diff_mode != 0) {
                        sw.swe_calc(te, ipldiff, iflag2, x2, serr);
                        if (diff_mode == DIFF_DIFF || diff_mode == DIFF_GEOHEL) {
                            if (diff_mode == DIFF_GEOHEL)
                                sw.swe_calc(te, ipldiff, iflag2 | SEFLG_HELCTR, x2, serr);
                            for (i = 0; i < 6; i++) xcartq[i] -= x2[i];
                        }
                    }
                }

                /* house position */
                if (strpbrk(fmt, "gGjzm")) {
                    armc = sw.swe_degnorm(sw.swe_sidtime(tut) * 15 + geopos[0]);
                    System.arraycopy(x, 0, xsv, 0, 6);
                    if (hpos_meth == 1) xsv[1] = 0;
                    star2 = ipl == SE_FIXSTAR ? star : "";
                    if (hpos_meth >= 2 && Character.toUpperCase(ihsy) == 'G') {
                        final double[] dgsect = new double[1];
                        sw.swe_gauquelin_sector(tut, ipl, new StringBuilder(star2), iflag,
                                hpos_meth, geopos, 0, 0, dgsect, serr);
                        hposj = dgsect[0];
                    } else {
                        if (ihsy == 'i' || ihsy == 'I') {
                            final double[] cusp = new double[13], ascmc = new double[10];
                            sw.swe_houses_ex(t, iflag, top_lat, top_long, ihsy, cusp, ascmc);
                        }
                        hposj = sw.swe_house_pos(armc, geopos[1], xobl[0], ihsy, xsv, serr);
                    }
                    hpos = Character.toUpperCase(ihsy) == 'G' ? (hposj - 1) * 10 : (hposj - 1) * 30;
                    if (diff_mode != 0) {
                        System.arraycopy(x2, 0, xsv, 0, 6);
                        if (hpos_meth == 1) xsv[1] = 0;
                        hpos2 = sw.swe_house_pos(armc, geopos[1], xobl[0], ihsy, xsv, serr);
                        hpos2 = Character.toUpperCase(ihsy) == 'G' ? (hpos2 - 1) * 10 : (hpos2 - 1) * 30;
                        if (diff_mode == DIFF_DIFF || diff_mode == DIFF_GEOHEL)
                            hpos = sw.swe_difdeg2n(hpos, hpos2);
                        else
                            hpos = sw.swe_deg_midp(hpos, hpos2);
                    }
                }

                spnam = se_pname;
                print_line(0, is_first, 0);
                is_first = false;
                if (!list_hor) line_count++;
                if (do_orbital_elements) {
                    orbital_elements(te, ipl, iflag);
                    continue;
                }
                if (line_count >= line_limit) {
                    pf("****** line count %d was exceeded\n", line_limit);
                    break;
                }
            }   /* for psp */

            if (list_hor) {
                p("\n");
                line_count++;
            }

            if (do_houses) {
                final double[] cusp = new double[37], cusp_speed = new double[37];
                final double[] ascmc = new double[10], ascmc_speed = new double[10];
                int iofs;
                if (Character.toUpperCase(ihsy) == 'G') nhouses = 36;  // Gauquelin has 36 cusps
                iofs = nhouses + 1;
                iflgret = sw.swe_houses_ex2(t, iflag, top_lat, top_long, ihsy,
                        cusp, ascmc, cusp_speed, ascmc_speed, serr);
                // when swe_houses_ex() fails it always returns Porphyry cusps instead
                if (iflgret < 0) {
                    final String msg = "House method " + sw.swe_house_name(ihsy)
                            + " failed, Porphyry calculated instead";
                    if (!msg.equals(serr_save)) {
                        p("error: ");
                        p(msg);
                        p("\n");
                    }
                    serr_save = msg;
                    ihsy = 'O';
                    nhouses = 12;   // instead of 36 with 'G'
                    iofs = nhouses + 1;
                }
                is_first = true;
                for (ipl = 1; ipl < iofs + 8; ipl++) {
                    x[0] = cusp[ipl];
                    if (ipl >= iofs) {
                        x[0] = ascmc[ipl - iofs];
                        x[3] = ascmc_speed[ipl - iofs];
                    } else {
                        x[3] = cusp_speed[ipl];
                    }
                    x[1] = 0;       /* latitude */
                    x[2] = 1.0;     /* pseudo radius vector */
                    if (ipl == iofs + 2) {      /* armc is already equatorial! */
                        xequ[0] = x[0];
                        xequ[1] = x[1];
                        xequ[2] = x[2];
                    } else if (strpbrk(fmt, "aADdQ")) {
                        sw.swe_cotrans(x, xequ, -xobl[0]);
                    }
                    if (strpbrk(fmt, "IiHhKk")) {
                        final double[] gpos = {top_long, top_lat, 0};
                        sw.swe_azalt(t, SE_ECL2HOR, gpos, datm[0], datm[1], x, xaz);
                    }
                    if (strpbrk(fmt, "gGj")) {
                        hposj = sw.swe_house_pos(armc, geopos[1], xobl[0], ihsy, x, serr);
                        hpos = Character.toUpperCase(ihsy) == 'G' ? (hposj - 1) * 10 : (hposj - 1) * 30;
                    }
                    print_line(MODE_HOUSE, is_first, 0);
                    is_first = false;
                    if (!list_hor) line_count++;
                }
                if (list_hor) {
                    p("\n");
                    line_count++;
                }
            }

            if (line_count >= line_limit) {
                pf("****** line count %d was exceeded\n", line_limit);
                break;
            }
        }   /* for tjd */

        if (!serr_warn.isEmpty()) {
            p("\nwarning: ");
            p(serr_warn);
            p("\n");
        }

        if (do_set_astro_models) p(smod);
        sw.swe_close();
        return out.toString();
    }

    /** the C's file-scope state is reset per run, so two calls in one JVM cannot interfere */
    static void resetGlobals() {
        star = "algol"; star2 = ""; sastno = "433"; spmoon = "9501"; shyp = "1";
        fmt = "PLBRS"; gap = " ";
        t = te = tut = 0; jut = 0; tstep = 1;
        jmon = jday = jyear = 0;
        ipl = SE_SUN; ipldiff = SE_SUN; nhouses = 12; iplctr = SE_SUN;
        spnam = ""; spnam2 = ""; se_pname = "";
        serr.setLength(0); serr_save = ""; serr_warn = "";
        gregflag = SE_GREG_CAL; gregflag_auto = true;
        diff_mode = 0; use_dms = false; has_n = false;
        universal_time = false; universal_time_utc = false;
        round_flag = 0; time_flag = 0; short_output = false; list_hor = false;
        special_event = 0; special_mode = 0;
        do_orbital_elements = false; hel_using_AV = false;
        with_header = true; with_chart_link = false;
        x = new double[6]; x2 = new double[6]; xequ = new double[6];
        xcart = new double[6]; xcartq = new double[6]; xobl = new double[6];
        xaz = new double[6]; xt = new double[6]; xsv = new double[6];
        hpos = hpos2 = hposj = armc = 0; hpos_meth = 0;
        geopos = new double[10];
        attr = new double[20]; tret = new double[20];
        datm = new double[4]; dobs = new double[6];
        iflag = 0; iflag2 = 0;
        direction = 1; direction_flag = false;
        step_in_minutes = step_in_seconds = step_in_years = step_in_months = false;
        helflag = 0; tjd = 2415020.5; nstep = 1; istep = 0; search_flag = 0;
        whicheph = SEFLG_SWIEPH; psp = 0;
        norefrac = disccenter = discbottom = hindu = 0;
        astro_models = ""; do_set_astro_models = false; smod = ""; inut = false;
        have_gap_parameter = false; use_swe_fixstar2 = false;
        output_extra_prec = false; show_file_limit = false;
    }


    // ------------------------------------------------------------------ swetest.c's own -h

    /**
     * The text swetest prints for {@code -h}. Generated from swetest.c by
     * {@code ai-github-projects/swe-jni-demo/tools/extract-swetest-help.py} - some 550 lines
     * of continued C string literal that would only collect typos if retyped, and that the
     * script can re-emit when upstream edits them.
     */
    static final class SweTestInfo {

        private SweTestInfo() {
        }

    // generated by ai-github-projects/swe-jni-demo/tools/extract-swetest-help.py
    // from swetest.c - do not edit by hand, re-run the script instead
        static final String infocmd0 =
                "\n"
                +             "  Swetest computes a complete set of geocentric planetary positions,\n"
                +             "  for a given date or a sequence of dates.\n"
                +             "  Input can either be a date or an absolute julian day number.\n"
                +             "  0:00 (midnight).\n"
                +             "  With the proper options, swetest can be used to output a printed\n"
                +             "  ephemeris and transfer the data into other programs like spreadsheets\n"
                +             "  for graphical display.\n"
                +             "  Version:                                                                                   \n"
                +             "\n";

        static final String infocmd1 =
                "\n"
                +             "  Command line options:\n"
                +             "  Note: spaces have to be observed carefully, as given\n"
                +             "     help commands:\n"
                +             "        -?, -h  display whole info\n"
                +             "        -hcmd   display commands\n"
                +             "        -hplan  display planet numbers\n"
                +             "        -hform  display format characters\n"
                +             "        -hdate  display input date format\n"
                +             "        -hexamp  display examples\n"
                +             "        -glp  report file location of library\n"
                +             "     input time formats:\n"
                +             "        -bDATE  begin date; e.g. -b31.1.1992 for 31 January 1991\n"
                +             "                Note: the date format is day month year (European style).\n"
                +             "        -bj...  begin date as an absolute Julian day number; e.g. -bj2415020.5\n"
                +             "        -j...   same as -bj\n"
                +             "        -tHH[:MM[:SS]]  input time (as Ephemeris Time)\n"
                +             "        -ut     input date is Universal Time (UT1)\n"
                +             "	-utHH[:MM[:SS]] input time (as Universal Time)\n"
                +             "	-utcHH[:MM[:SS]] input time (as Universal Time Coordinated UTC)\n"
                +             "		H,M,S can have one or two digits. Their limits are unchecked.\n"
                +             "     output time for eclipses, occultations, risings/settings is UT by default\n"
                +             "        -lmt    output date/time is LMT (with -geopos)\n"
                +             "        -lat    output date/time is LAT (with -geopos)\n"
                +             "     object, number of steps, step with\n"
                +             "        -pSEQ   planet sequence to be computed.\n"
                +             "                See the letter coding below.\n"
                +             "        -dX     differential ephemeris: print differential ephemeris between\n"
                +             "                body X and each body in list given by -p\n"
                +             "                example: -p2 -d0 -fJl -n366 -b1.1.1992 prints the longitude\n"
                +             "                distance between SUN (planet 0) and MERCURY (planet 2)\n"
                +             "                for a full year starting at 1 Jan 1992.\n"
                +             "        -dhX    differential ephemeris: print differential ephemeris between\n"
                +             "                heliocentric body X and each body in list given by -p\n"
                +             "                example: -p8 -dh8 -ftl -n36600 -b1.1.1500 -s5 prints the longitude\n"
                +             "                distance between geocentric and heliocentric Neptune (planet 8)\n"
                +             "                for 500 year starting at 1 Jan 1500.\n"
                +             "		Using this option mostly makes sense for a single planet\n"
                +             "		to find out how much its geocentric and heliocentric positions can differ\n"
                +             "		over extended periods of time\n"
                +             "	-DX	midpoint ephemeris, works the same way as the differential\n"
                +             "		mode -d described above, but outputs the midpoint position.\n"
                +             "        -nN     output data for N consecutive timesteps; if no -n option\n"
                +             "                is given, the default is 1. If the option -n without a\n"
                +             "                number is given, the default is 20.\n"
                +             "        -sN     timestep N days, default 1. This option is only meaningful\n"
                +             "                when combined with option -n.\n"
                +             "                If an 'y' is appended, the time step is in years instead of days, \n"
                +             "                for example -s10y for a time step of 10 years.\n"
                +             "                If an 'mo' is appended, the time step is in months instead of days, \n"
                +             "                for example -s3mo for a time step of 3 months.\n"
                +             "                If an 'm' is appended, the time step is in minutes instead of days, \n"
                +             "                for example -s15m for a time step of 15 minutes.\n"
                +             "                If an 's' is appended, the time step is in seconds instead of days, \n"
                +             "                for example -s1s for a time step of 1 second.\n";

        static final String infocmd2 =
                "     output format:\n"
                +             "        -fSEQ   use SEQ as format sequence for the output columns;\n"
                +             "                default is PLBRS.\n"
                +             "        -head   don\'t print the header before the planet data. This option\n"
                +             "                is useful when you want to paste the output into a\n"
                +             "                spreadsheet for displaying graphical ephemeris.\n"
                +             "        +head   header before every step (with -s..) \n"
                +             "        -gPPP   use PPP as gap between output columns; default is a single\n"
                +             "                blank.  -g followed by white space sets the\n"
                +             "                gap to the TAB character; which is useful for data entry\n"
                +             "                into spreadsheets.\n"
                +             "        -hor	list data for multiple planets 'horizontally' in same line.\n"
                +             "		all columns of -fSEQ are repeated except time colums tTJyY.\n"
                +             "     astrological house system:\n"
                +             "        -house[long,lat,hsys]	\n"
                +             "		include house cusps. The longitude, latitude (degrees with\n"
                +             "		DECIMAL fraction) and house system letter can be given, with\n"
                +             "		commas separated, + for east and north. If none are given,\n"
                +             "		Greenwich UK and Placidus is used: 0.00,51.50,p.\n"
                +             "		The output lists 12 house cusps, Asc, MC, ARMC, Vertex,\n"
                +             "		Equatorial Ascendant, co-Ascendant as defined by Walter Koch, \n"
                +             "		co-Ascendant as defined by Michael Munkasey, and Polar Ascendant. \n"
                +             "		Houses can only be computed if option -ut is given.\n"
                +             "                   A  equal\n"
                +             "                   B  Alcabitius\n"
                +             "                   C  Campanus\n"
                +             "                   D  equal / MC\n"
                +             "                   E  equal = A\n"
                +             "                   F  Carter poli-equatorial\n"
                +             "                   G  36 Gauquelin sectors\n"
                +             "                   H  horizon / azimuth\n"
                +             "                   I  Sunshine\n"
                +             "                   i  Sunshine alternative\n"
                +             "                   K  Koch\n"
                +             "                   L  Pullen S-delta\n"
                +             "                   M  Morinus\n"
                +             "                   N  Whole sign, Aries = 1st house\n"
                +             "                   O  Porphyry\n"
                +             "                   P  Placidus\n"
                +             "                   Q  Pullen S-ratio\n"
                +             "                   R  Regiomontanus\n"
                +             "                   S  Sripati\n"
                +             "                   T  Polich/Page (\"topocentric\")\n"
                +             "                   U  Krusinski-Pisa-Goelzer\n"
                +             "                   V  equal Vehlow\n"
                +             "                   W  equal, whole sign\n"
                +             "                   X  axial rotation system/ Meridian houses\n"
                +             "                   Y  APC houses\n"
                +             "		 The use of lower case letters is deprecated. They will have a\n"
                +             "		 different meaning in future releases of Swiss Ephemeris.\n"
                +             "        -hsy[hsys]	\n"
                +             "		 house system to be used (for house positions of planets)\n"
                +             "		 for long, lat, hsys, see -house\n"
                +             "		 The use of lower case letters is deprecated. They will have a\n"
                +             "		 different meaning in future releases of Swiss Ephemeris.\n";

        static final String infocmd3 =
                "        -geopos[long,lat,elev]	\n"
                +             "		Geographic position. Can be used for azimuth and altitude\n"
                +             "                or house cusps calculations.\n"
                +             "                The longitude, latitude (degrees with DECIMAL fraction)\n"
                +             "		and elevation (meters) can be given, with\n"
                +             "		commas separated, + for east and north. If none are given,\n"
                +             "		Greenwich is used: 0,51.5,0.\n"
                +             "		For topocentric planet positions please user the parameter -topo\n"
                +             "     sidereal astrology:\n"
                +             "	-ay..   ayanamsha, with number of method, e.g. ay0 for Fagan/Bradley\n"
                +             "	-sid..    sidereal, with number of method (see below)\n"
                +             "	-sidt0..  dito, but planets are projected on the ecliptic plane of the\n"
                +             "	          reference date of the ayanamsha (more info in general documentation\n"
                +             "		  www.astro.com/swisseph/swisseph.htm)\n"
                +             "	-sidsp..  dito, but planets are projected on the solar system plane.\n"
                +             "		  (see www.astro.com/swisseph/swisseph.htm)\n"
                +             "        -sidudef[jd,ay0,...]  sidereal, with user defined ayanamsha; \n"
                +             "	          jd=julian day number in TT/ET\n"
                +             "	          ay0=initial value of ayanamsha, \n"
                +             "		  ...=optional parameters, comma-sparated:\n"
                +             "		  'jdisut': ayanamsha reference date is UT\n"
                +             "		  'eclt0':  project on ecliptic of reference date (like -sidt0..)\n"
                +             "		  'ssyplane':  project on solar system plane (like -sidsp..)\n"
                +             "		  e.g. '-sidudef2452163.8333333,25.0,jdisut': ayanamsha is 25.0Â° on JD 2452163.8333333 UT\n"
                +             "           number of ayanamsha method:\n"
                +             "	   0 for Fagan/Bradley\n"
                +             "	   1 for Lahiri\n"
                +             "	   2 for De Luce\n"
                +             "	   3 for Raman\n"
                +             "	   4 for Usha/Shashi\n"
                +             "	   5 for Krishnamurti\n"
                +             "	   6 for Djwhal Khul\n"
                +             "	   7 for Yukteshwar\n"
                +             "	   8 for J.N. Bhasin\n"
                +             "	   9 for Babylonian/Kugler 1\n"
                +             "	   10 for Babylonian/Kugler 2\n"
                +             "	   11 for Babylonian/Kugler 3\n"
                +             "	   12 for Babylonian/Huber\n"
                +             "	   13 for Babylonian/Eta Piscium\n"
                +             "	   14 for Babylonian/Aldebaran = 15 Tau\n"
                +             "	   15 for Hipparchos\n"
                +             "	   16 for Sassanian\n"
                +             "	   17 for Galact. Center = 0 Sag\n"
                +             "	   18 for J2000\n"
                +             "	   19 for J1900\n"
                +             "	   20 for B1950\n"
                +             "	   21 for Suryasiddhanta\n"
                +             "	   22 for Suryasiddhanta, mean Sun\n"
                +             "	   23 for Aryabhata\n"
                +             "	   24 for Aryabhata, mean Sun\n"
                +             "	   25 for SS Revati\n"
                +             "	   26 for SS Citra\n"
                +             "	   27 for True Citra\n"
                +             "	   28 for True Revati\n"
                +             "	   29 for True Pushya (PVRN Rao)\n"
                +             "	   30 for Galactic (Gil Brand)\n"
                +             "	   31 for Galactic Equator (IAU1958)\n"
                +             "	   32 for Galactic Equator\n"
                +             "	   33 for Galactic Equator mid-Mula\n"
                +             "	   34 for Skydram (Mardyks)\n"
                +             "	   35 for True Mula (Chandra Hari)\n"
                +             "	   36 Dhruva/Gal.Center/Mula (Wilhelm)\n"
                +             "	   37 Aryabhata 522\n"
                +             "	   38 Babylonian/Britton\n"
                +             "   	   39 Vedic/Sheoran\n"
                +             "	   40 Cochrane (Gal.Center = 0 Cap)\n"
                +             "	   41 Galactic Equator (Fiorenza)\n"
                +             "	   42 Vettius Valens\n"
                +             "	   43 Lahiri 1940\n"
                +             "	   44 Lahiri VP285 (1980)\n"
                +             "	   45 Krishnamurti VP291\n"
                +             "	   46 Lahiri ICRC\n"
                +             "     ephemeris specifications:\n"
                +             "        -edirPATH change the directory of the ephemeris files \n"
                +             "        -eswe   swiss ephemeris\n"
                +             "        -ejpl   jpl ephemeris (DE431), or with ephemeris file name\n"
                +             "                -ejplde200.eph \n"
                +             "        -emos   moshier ephemeris\n"
                +             "        -true             true positions\n"
                +             "        -noaberr          no aberration\n"
                +             "        -nodefl           no gravitational light deflection\n"
                +             "	-noaberr -nodefl  astrometric positions\n"
                +             "        -j2000            no precession (i.e. J2000 positions)\n"
                +             "        -icrs             ICRS (use Internat. Celestial Reference System)\n"
                +             "        -nonut            no nutation \n";

        static final String infocmd4 =
                "        -speed            calculate high precision speed \n"
                +             "        -speed3           'low' precision speed from 3 positions \n"
                +             "                          do not use this option. -speed parameter\n"
                +             "			  is faster and more precise \n"
                +             "	-iXX	          force iflag to value XX\n"
                +             "        -testaa96         test example in AA 96, B37,\n"
                +             "                          i.e. venus, j2450442.5, DE200.\n"
                +             "                          attention: use precession IAU1976\n"
                +             "                          and nutation 1980 (s. swephlib.h)\n"
                +             "        -testaa95\n"
                +             "        -testaa97\n"
                +             "\n"
                +             "     special purpose options:\n"
                +             "        -roundsec         round to seconds\n"
                +             "        -roundmin         round to minutes\n"
                +             "	-ep		  use extra precision in output for some data\n"
                +             "	-dms              use dms instead of fractions, at some places\n"
                +             "	-lim		  print ephemeris file range\n"
                +             "	-astposDDD	  list named asteroids within orb of 0.5Â° of position DDD\n"
                +             "	-astposDDD,orb	  list named asteroids within orb of position DDD\n"
                +             "			  DDD is must be decimal between 0.00Â° and 359.99999Â°\n"
                +             "			  orb must be deciaml between 0.01Â° and 1.0Â°\n"
                +             "			  \n"
                +             "     observer position:\n"
                +             "        -hel    compute heliocentric positions\n"
                +             "        -bary   compute barycentric positions (bar. earth instead of node) \n"
                +             "        -topo[long,lat,elev]	\n"
                +             "		topocentric positions. The longitude, latitude (degrees with\n"
                +             "		DECIMAL fraction) and elevation (meters) can be given, with\n"
                +             "		commas separated, + for east and north. If none are given,\n"
                +             "		Greenwich is used 0.00,51.50,0\n"
                +             "        -pc...  compute planetocentric positions\n"
                +             "                to specify the central body, use the internal object number\n"
                +             "		of Swiss Ephemeris, e.g. 3 for Venus, 4 for Mars, \n"
                +             "        -pc3 	Venus-centric \n"
                +             "        -pc4 	Mars-centric \n"
                +             "        -pc5 	Jupiter-centric (barycenter)\n"
                +             "	-pc9599 Jupiter-centric (center of body)\n"
                +             "	-pc9699 Saturn-centric (center of body)\n"
                +             "		For asteroids use MPC number + 10000, e.g.\n"
                +             "	-pc10433 Eros-centric (Eros = 433 + 10000)\n"
                +             "     orbital elements:\n"
                +             "        -orbel  compute osculating orbital elements relative to the\n"
                +             "	        mean ecliptic J2000. (Note, all values, including time of\n"
                +             "		pericenter vary considerably depending on the date for which the\n"
                +             "		osculating ellipse is calculated\n"
                +             "\n"
                +             "     special events:\n"
                +             "        -solecl solar eclipse\n"
                +             "                output 1st line:\n"
                +             "                  eclipse date,\n"
                +             "                  time of maximum (UT):\n"
                +             "		    geocentric angle between centre of Sun and Moon reaches minimum.\n"
                +             "                  core shadow width (negative with total eclipses),\n"
                +             "		  eclipse magnitudes:\n"
                +             "		    1. NASA method (= 2. with partial ecl. and \n"
                +             "		       ratio lunar/solar diameter with total and annular ecl.)\n"
                +             "		    2. fraction of solar diameter covered by moon;\n"
                +             "		       if the value is > 1, it means that Moon covers more than\n"
                +             "		       just the solar disk\n"
                +             "		    3. fraction of solar disc covered by moon (obscuration)\n"
                +             "		       with total and annular eclipses it is the ratio of\n"
                +             "		       the sizes of the solar disk and the lunar disk.\n"
                +             "		  Saros series and eclipse number\n"
                +             "		  Julian day number (6-digit fraction) of maximum\n"
                +             "                output 2nd line:\n"
                +             "                  start and end times for partial and total phases\n"
                +             "		  delta t in sec\n"
                +             "                output 3rd line:\n"
                +             "                  geographical longitude and latitude of maximum eclipse,\n"
                +             "                  totality duration at that geographical position,\n"
                +             "                output with -local, see below.\n"
                +             "        -occult occultation of planet or star by the moon. Use -p to \n"
                +             "                specify planet (-pf -xfAldebaran for stars) \n"
                +             "                output format same as with -solecl, with the following differences:\n"
                +             "		  Magnitude is defined like no. 2. with solar eclipses.\n"
                +             "		  There are no saros series.\n";

        static final String infocmd5 =
                "        -lunecl lunar eclipse\n"
                +             "                output 1st line:\n"
                +             "                  eclipse date,\n"
                +             "                  time of maximum (UT),\n"
                +             "                  eclipse magnitudes: umbral and penumbral\n"
                +             "		    method as method 2 with solar eclipses\n"
                +             "		  Saros series and eclipse number \n"
                +             "		  Julian day number (6-digit fraction) of maximum\n"
                +             "                output 2nd line:\n"
                +             "                  6 contacts for start and end of penumbral, partial, and\n"
                +             "                  total phase\n"
                +             "		  delta t in sec\n"
                +             "                output 3rd line:\n"
                +             "                  geographic position where the Moon is in zenith at maximum eclipse\n"
                +             "        -local  only with -solecl or -occult, if the next event of this\n"
                +             "                kind is wanted for a given geogr. position.\n"
                +             "                Use -geopos[long,lat,elev] to specify that position.\n"
                +             "                If -local is not set, the program \n"
                +             "                searches for the next event anywhere on earth.\n"
                +             "                output 1st line:\n"
                +             "                  eclipse date,\n"
                +             "                  time of maximum,\n"
                +             "                  eclipse magnitudes, as with global solar eclipse function \n"
                +             "		    (with occultations: only diameter method, see solar eclipses, method 2)\n"
                +             "		  Saros series and eclipse number (with solar eclipses only)\n"
                +             "		  Julian day number (6-digit fraction) of maximum\n"
                +             "                output 2nd line:\n"
                +             "                  local eclipse duration for totality (zero with partial occultations)\n"
                +             "                  local four contacts,\n"
                +             "		  delta t in sec\n"
                +             "		Occultations with the remark \"(daytime)\" cannot be observed because\n"
                +             "		they are taking place by daylight. Occultations with the remark\n"
                +             "		\"(sunrise)\" or \"(sunset)\" can be observed only partly because part\n"
                +             "		of them takes place in daylight.\n"
                +             "        -hev[type] heliacal events,\n"
                +             "		type 1 = heliacal rising\n"
                +             "		type 2 = heliacal setting\n"
                +             "		type 3 = evening first\n"
                +             "		type 4 = morning last\n"
                +             "	        type 0 or missing = all four events are listed.\n"
                +             "        -rise   rising and setting of a planet or star.\n"
                +             "                Use -geopos[long,lat,elev] to specify geographical position.\n"
                +             "        -metr   southern and northern meridian transit of a planet of star\n"
                +             "                Use -geopos[long,lat,elev] to specify geographical position.\n"
                +             "     specifications for eclipses:\n"
                +             "        -total  total eclipse (only with -solecl, -lunecl)\n"
                +             "        -partial partial eclipse (only with -solecl, -lunecl)\n"
                +             "        -annular annular eclipse (only with -solecl)\n"
                +             "        -anntot annular-total (hybrid) eclipse (only with -solecl)\n"
                +             "        -penumbral penumbral lunar eclipse (only with -lunecl)\n"
                +             "        -central central eclipse (only with -solecl, nonlocal)\n"
                +             "        -noncentral non-central eclipse (only with -solecl, nonlocal)\n";

        static final String infocmd6 =
                "     specifications for risings and settings:\n"
                +             "        -norefrac   neglect refraction (with option -rise)\n"
                +             "        -disccenter find rise of disc center (with option -rise)\n"
                +             "        -discbottom find rise of disc bottom (with option -rise)\n"
                +             "	-hindu      hindu version of sunrise (with option -rise)\n"
                +             "     specifications for heliacal events:\n"
                +             "        -at[press,temp,rhum,visr]:\n"
                +             "	            pressure in hPa\n"
                +             "		    temperature in degrees Celsius\n"
                +             "		    relative humidity in %\n"
                +             "		    visual range, interpreted as follows:\n"
                +             "		      > 1 : meteorological range in km\n"
                +             "		      1>visr>0 : total atmospheric coefficient (ktot)\n"
                +             "		      = 0 : calculated from press, temp, rhum\n"
                +             "		    Default values are -at1013.25,15,40,0\n"
                +             "         -obs[age,SN] age of observer and Snellen ratio\n"
                +             "	            Default values are -obs36,1\n"
                +             "         -opt[age,SN,binocular,magn,diam,transm]\n"
                +             "	            age and SN as with -obs\n"
                +             "		    0 monocular or 1 binocular\n"
                +             "		    telescope magnification\n"
                +             "		    optical aperture in mm\n"
                +             "		    optical transmission\n"
                +             "		    Default values: -opt36,1,1,1,0,0 (naked eye)\n"
                +             "     backward search:\n"
                +             "        -bwd\n";

        static final String infoplan =
                "\n"
                +             "  Planet selection letters:\n"
                +             "     planetary lists:\n"
                +             "        d (default) main factors 0123456789mtABCcg\n"
                +             "        p main factors as above, plus main asteroids DEFGHI\n"
                +             "        h ficticious factors J..X\n"
                +             "        a all factors\n"
                +             "        (the letters above can only appear as a single letter)\n"
                +             "\n"
                +             "     single body numbers/letters:\n"
                +             "        0 Sun (character zero)\n"
                +             "        1 Moon (character 1)\n"
                +             "        2 Mercury\n"
                +             "        3 Venus\n"
                +             "        4 Mars\n"
                +             "        5 Jupiter\n"
                +             "        6 Saturn\n"
                +             "        7 Uranus\n"
                +             "        8 Neptune\n"
                +             "        9 Pluto\n"
                +             "        m mean lunar node\n"
                +             "        t true lunar node\n"
                +             "        n nutation\n"
                +             "        o obliquity of ecliptic\n"
                +             "	q delta t\n"
                +             "	y time equation\n"
                +             "	b ayanamsha\n"
                +             "        A mean lunar apogee (Lilith, Black Moon) \n"
                +             "        B osculating lunar apogee \n"
                +             "        c intp. lunar apogee \n"
                +             "        g intp. lunar perigee \n"
                +             "        C Earth (in heliocentric or barycentric calculation)\n"
                +             "        For planets Jupiter to Pluto the center of body (COB) can be\n"
                +             "        calculated using the additional parameter -cob\n"
                +             "     dwarf planets, plutoids\n"
                +             "        F Ceres\n"
                +             "	9 Pluto\n"
                +             "	s -xs136199   Eris\n"
                +             "	s -xs136472   Makemake\n"
                +             "	s -xs136108   Haumea\n"
                +             "     some minor planets:\n"
                +             "        D Chiron\n"
                +             "        E Pholus\n"
                +             "        G Pallas \n"
                +             "        H Juno \n"
                +             "        I Vesta \n"
                +             "        s minor planet, with MPC number given in -xs\n"
                +             "     some planetary moons and center of body of a planet:\n"
                +             "        v with moon number given in -xv:\n"
                +             "        v -xv9501 Io/Jupiter:\n"
                +             "        v -xv9599 Jupiter, center of body (COB):\n"
                +             "        v -xv94.. Mars moons:\n"
                +             "        v -xv95.. Jupiter moons and COB:\n"
                +             "        v -xv96.. Saturn moons and COB:\n"
                +             "        v -xv97.. Uranus moons and COB:\n"
                +             "        v -xv98.. Neptune moons and COB:\n"
                +             "        v -xv99.. Pluto moons and COB:\n"
                +             "          The numbers of the moons are given here: \n"
                +             "	  https://www.astro.com/ftp/swisseph/ephe/sat/plmolist.txt\n"
                +             "     fixed stars:\n"
                +             "        f fixed star, with name or number given in -xf option\n"
                +             "	f -xfSirius   Sirius\n"
                +             "     fictitious objects:\n"
                +             "        J Cupido \n"
                +             "        K Hades \n"
                +             "        L Zeus \n"
                +             "        M Kronos \n"
                +             "        N Apollon \n"
                +             "        O Admetos \n"
                +             "        P Vulkanus \n"
                +             "        Q Poseidon \n"
                +             "        R Isis (Sevin) \n"
                +             "        S Nibiru (Sitchin) \n"
                +             "        T Harrington \n"
                +             "        U Leverrier's Neptune\n"
                +             "        V Adams' Neptune\n"
                +             "        W Lowell's Pluto\n"
                +             "        X Pickering's Pluto\n"
                +             "        Y Vulcan\n"
                +             "        Z White Moon\n"
                +             "	w Waldemath's dark Moon\n"
                +             "        z hypothetical body, with number given in -xz\n"
                +             "     sidereal time:\n"
                +             "        x sidereal time\n"
                +             "        e print a line of labels\n"
                +             "          \n";

        static final String infoform =
                "\n"
                +             "  Output format SEQ letters:\n"
                +             "  In the standard setting five columns of coordinates are printed with\n"
                +             "  the default format PLBRS. You can change the default by providing an\n"
                +             "  option like -fCCCC where CCCC is your sequence of columns.\n"
                +             "  The coding of the sequence is like this:\n"
                +             "        y year\n"
                +             "        Y year.fraction_of_year\n"
                +             "        p planet index\n"
                +             "        P planet name\n"
                +             "        J absolute juldate\n"
                +             "        T date formatted like 23.02.1992 \n"
                +             "        t date formatted like 920223 for 1992 february 23\n"
                +             "        L longitude in degree ddd mm'ss\"\n"
                +             "        l longitude decimal\n"
                +             "        Z longitude ddsignmm'ss\"\n"
                +             "        S speed in longitude in degree ddd:mm:ss per day\n"
                +             "        SS speed for all values specified in fmt\n"
                +             "        s speed longitude decimal (degrees/day)\n"
                +             "        ss speed for all values specified in fmt\n"
                +             "        B latitude degree\n"
                +             "        b latitude decimal\n"
                +             "        R distance decimal in AU\n"
                +             "        r distance decimal in AU, Moon in seconds parallax\n"
                +             "        W distance decimal in light years\n"
                +             "        w distance decimal in km\n"
                +             "        q relative distance (1000=nearest, 0=furthest)\n"
                +             "        A right ascension in hh:mm:ss\n"
                +             "        a right ascension hours decimal\n"
                +             "	m Meridian distance \n"
                +             "	z Zenith distance \n"
                +             "        D declination degree\n"
                +             "        d declination decimal\n"
                +             "        I azimuth degree\n"
                +             "        i azimuth decimal\n"
                +             "        H altitude degree\n"
                +             "        h altitude decimal\n"
                +             "        K altitude (with refraction) degree\n"
                +             "        k altitude (with refraction) decimal\n"
                +             "        G house position in degrees\n"
                +             "        g house position in degrees decimal\n"
                +             "        j house number 1.0 - 12.99999\n"
                +             "        X x-, y-, and z-coordinates ecliptical\n"
                +             "        x x-, y-, and z-coordinates equatorial\n"
                +             "        U unit vector ecliptical\n"
                +             "        u unit vector equatorial\n"
                +             "        Q l, b, r, dl, db, dr, a, d, da, dd\n"
                +             "	n nodes (mean): ascending/descending (Me - Ne); longitude decimal\n"
                +             "	N nodes (osculating): ascending/descending, longitude; decimal\n"
                +             "	f apsides (mean): perihelion, aphelion, second focal point; longitude dec.\n"
                +             "	F apsides (osc.): perihelion, aphelion, second focal point; longitude dec.\n"
                +             "	+ phase angle\n"
                +             "	- phase\n"
                +             "	* elongation\n"
                +             "	/ apparent diameter of disc (without refraction)\n"
                +             "	= magnitude\n";

        static final String infoform2 =
                "        v (reserved)\n"
                +             "        V (reserved)\n"
                +             "	\n";

        static final String infodate =
                "\n"
                +             "  Date entry:\n"
                +             "  In the interactive mode, when you are asked for a start date,\n"
                +             "  you can enter data in one of the following formats:\n"
                +             "\n"
                +             "        1.2.1991        three integers separated by a nondigit character for\n"
                +             "                        day month year. Dates are interpreted as Gregorian\n"
                +             "                        after 4.10.1582 and as Julian Calendar before.\n"
                +             "                        Time is always set to midnight (0 h).\n"
                +             "                        If the three letters jul are appended to the date,\n"
                +             "                        the Julian calendar is used even after 1582.\n"
                +             "                        If the four letters greg are appended to the date,\n"
                +             "                        the Gregorian calendar is used even before 1582.\n"
                +             "\n"
                +             "        j2400123.67     the letter j followed by a real number, for\n"
                +             "                        the absolute Julian daynumber of the start date.\n"
                +             "                        Fraction .5 indicates midnight, fraction .0\n"
                +             "                        indicates noon, other times of the day can be\n"
                +             "                        chosen accordingly.\n"
                +             "\n"
                +             "        <RETURN>        repeat the last entry\n"
                +             "        \n"
                +             "        .               stop the program\n"
                +             "\n"
                +             "        +20             advance the date by 20 days\n"
                +             "\n"
                +             "        -10             go back in time 10 days\n";

        static final String infoexamp =
                "\n"
                +             "\n"
                +             "  Examples:\n"
                +             "\n"
                +             "    swetest -p2 -b1.12.1900 -n15 -s2\n"
                +             "	ephemeris of Mercury (-p2) starting on 1 Dec 1900,\n"
                +             "	15 positions (-n15) in two-day steps (-s2)\n"
                +             "\n"
                +             "    swetest -p2 -b1.12.1900 -n15 -s2 -fTZ -roundsec -g, -head\n"
                +             "	same, but output format =  date and zodiacal position (-fTZ),\n"
                +             "	separated by comma (-g,) and rounded to seconds (-roundsec),\n"
                +             "	without header (-head).\n"
                +             "\n"
                +             "    swetest -ps -xs433 -b1.12.1900\n"
                +             "	position of asteroid 433 Eros (-ps -xs433)\n"
                +             "\n"
                +             "    swetest -pf -xfAldebaran -b1.1.2000\n"
                +             "	position of fixed star Aldebaran \n"
                +             "\n"
                +             "    swetest -p1 -d0 -b1.12.1900 -n10 -fPTl -head\n"
                +             "	angular distance of moon (-p1) from sun (-d0) for 10\n"
                +             "	consecutive days (-n10).\n"
                +             "\n"
                +             "    swetest -p6 -DD -b1.12.1900 -n100 -s5 -fPTZ -head -roundmin\n"
                +             "      Midpoints between Saturn (-p6) and Chiron (-DD) for 100\n"
                +             "      consecutive steps (-n100) with 5-day steps (-s5) with\n"
                +             "      longitude in degree-sign format (-f..Z) rounded to minutes (-roundmin)\n"
                +             "\n"
                +             "    swetest -b5.1.2002 -p -house12.05,49.50,K -ut12:30\n"
                +             "	Koch houses for a location in Germany at a given date and time\n"
                +             "\n"
                +             "    swetest -b1.1.2016  -g -fTlbR -p0123456789Dmte -hor -n366 -roundsec\n"
                +             "	tabular ephemeris (all planets Sun - Pluto, Chiron, mean node, true node)\n"
                +             "	in one horizontal row, tab-separated, for 366 days. For each planet\n"
                +             "	list longitude, latitude and geocentric distance.\n";

        static final String[] ALL = {infocmd0, infocmd1, infocmd2, infocmd3, infocmd4, infocmd5, infocmd6, infoplan, infoform, infoform2, infodate, infoexamp};


        /** the sections swetest prints for -h, -hc, -hp, -hf, -hd, -he */
        static String help(char which) {
            final StringBuilder s = new StringBuilder();
            if ('c' == which || '\0' == which) {
                s.append(infocmd0.replace("Version:", "Version: " + sw.swe_version()));
                s.append(infocmd1).append(infocmd2).append(infocmd3);
                s.append(infocmd4).append(infocmd5).append(infocmd6);
            }
            if ('p' == which || '\0' == which) s.append(infoplan);
            if ('f' == which || '\0' == which) s.append(infoform).append(infoform2);
            if ('d' == which || '\0' == which) s.append(infodate);
            if ('e' == which || '\0' == which) s.append(infoexamp);
            return s.toString();
        }

        /**
         * What swetest does when it has no date: it asks for one on stdin. A port with no
         * console loop says so instead of pretending to have read one.
         */
        static String usage() {
            return "swetest: no date given - pass -b<dd.mm.yyyy> or -j<julian day>."
                    + " Try -h for the full option list.\n";
        }
    }

    // -------------------------------------------------------------------------- print_line

    static int print_line(int mode, boolean is_first, int sid_mode) {
        double t2, ju2 = 0;
        double y_frac;
        double ar, sinp;
        final double[] dret = new double[20];
        final String slon;
        final String pnam;
        final boolean is_house = (mode & MODE_HOUSE) != 0;
        final boolean is_label = (mode & MODE_LABEL) != 0;
        final boolean is_ayana = (mode & MODE_AYANAMSA) != 0;
        int iflgret, dar;

        // build planet name column, just in case
        if (is_house) {
            pnam = ipl <= nhouses ? String.format(Locale.ROOT, "house %2d       ", ipl)
                    : String.format(Locale.ROOT, "%-15s", hs_nam[ipl - nhouses]);
        } else if (diff_mode == DIFF_DIFF) {
            pnam = String.format(Locale.ROOT, "%.3s-%.3s", spnam, spnam2);
        } else if (diff_mode == DIFF_GEOHEL) {
            pnam = String.format(Locale.ROOT, "%.3s-%.3sHel", spnam, spnam2);
        } else if (diff_mode == DIFF_MIDP) {
            pnam = String.format(Locale.ROOT, "%.3s/%.3s", spnam, spnam2);
        } else {
            pnam = String.format(Locale.ROOT, "%-15.15s", spnam);
        }
        if (list_hor && fmt.indexOf('P') < 0) {
            slon = String.format(Locale.ROOT, "%.8s %s", pnam, "long.");
        } else {
            slon = String.format(Locale.ROOT, "%-14s", "long.");
        }

        for (int si = 0; si < fmt.length(); si++) {
            final char sc = fmt.charAt(si);
            if (is_house && "bBrRxXuUQnNfFj+-*/=".indexOf(sc) >= 0) continue;
            if (is_ayana && "bBsSrRxXuUQnNfFj+-*/=".indexOf(sc) >= 0) continue;
            if (si != 0) p(gap);
            if (si == 0 && list_hor && !is_first && "yYJtT".indexOf(sc) < 0) p(gap);

            switch (sc) {
                case 'y':
                    if (list_hor && !is_first) break;
                    if (is_label) { p("year"); break; }
                    pf("%d", jyear);
                    break;
                case 'Y':
                    if (list_hor && !is_first) break;
                    if (is_label) { p("year"); break; }
                    t2 = sw.swe_julday(jyear, 1, 1, ju2, gregflag);
                    y_frac = (t - t2) / 365.0;
                    pf("%.2f", jyear + y_frac);
                    break;
                case 'p':
                    if (is_label) { p("obj.nr"); break; }
                    if (!is_house && diff_mode == DIFF_DIFF) pf("%d-%d", ipl, ipldiff);
                    else if (!is_house && diff_mode == DIFF_GEOHEL) pf("%d-%dhel", ipl, ipldiff);
                    else if (!is_house && diff_mode == DIFF_MIDP) pf("%d/%d", ipl, ipldiff);
                    else pf("%d", ipl);
                    break;
                case 'P':
                    if (is_label) { pf("%-15s", "name"); break; }
                    if (is_house) {
                        if (ipl <= nhouses) pf("house %2d       ", ipl);
                        else pf("%-15s", hs_nam[ipl - nhouses]);
                    } else if (is_ayana) {
                        pf("Ayanamsha %s ", sw.swe_get_ayanamsa_name(sid_mode));
                    } else if (diff_mode == DIFF_DIFF || diff_mode == DIFF_GEOHEL) {
                        pf("%.3s-%.3s", spnam, spnam2);
                    } else if (diff_mode == DIFF_MIDP) {
                        pf("%.3s/%.3s", spnam, spnam2);
                    } else {
                        pf("%-15s", spnam);
                    }
                    break;
                case 'J':
                    if (list_hor && !is_first) break;
                    if (is_label) { p("julday"); break; }
                    y_frac = (t - Math.floor(t)) * 100;
                    if (Math.floor(y_frac) != y_frac) pf("%.5f", t);
                    else pf("%.2f", t);
                    break;
                case 'T':
                    if (list_hor && !is_first) break;
                    if (is_label) { p("date    "); break; }
                    pf("%02d.%02d.%04d", jday, jmon, jyear);
                    if (gregflag == SE_JUL_CAL) p("j");
                    if (jut != 0 || step_in_minutes || step_in_seconds) {
                        final int[] hms = new int[3];
                        final double[] dsecfr = new double[1];
                        final int[] isgn = new int[1];
                        int roundflag = SE_SPLIT_DEG_ROUND_SEC;
                        if ((tstep < 1 && tstep > -1) && step_in_seconds) {
                            roundflag = 0;
                            sw.swe_split_deg(jut, roundflag, hms, dsecfr, isgn);
                            pf(" %d:%02d:%02.2f", hms[0], hms[1], hms[2] + dsecfr[0]);
                        } else {
                            sw.swe_split_deg(jut, roundflag, hms, dsecfr, isgn);
                            pf(" %d:%02d:%02d", hms[0], hms[1], hms[2]);
                        }
                        p(universal_time ? " UT" : " TT");
                    }
                    break;
                case 't':
                    if (list_hor && !is_first) break;
                    if (is_label) { p("date"); break; }
                    pf("%02d%02d%02d", jyear % 100, jmon, jday);
                    break;
                case 'L':
                    if (is_label) { p(slon); break; }
                    if ('q' == psp || 'y' == psp) {  /* delta t or time equation */
                        pf("%# 11.7f", x[0]);
                        p("s");
                        break;
                    }
                    p(dms(x[0], round_flag));
                    break;
                case 'l':
                    if (is_label) { p(slon); break; }
                    if ((round_flag & BIT_ROUND_MIN) != 0) pf("%# 6.2f", x[0]);
                    else if (output_extra_prec) pf("%# 11.11f", x[0]);
                    else pf("%# 11.7f", x[0]);
                    break;
                case 'G':
                    if (is_label) { p("housPos"); break; }
                    p(dms(hpos, round_flag));
                    break;
                case 'g':
                    if (is_label) { p("housPos"); break; }
                    pf("%# 11.7f", hpos);
                    break;
                case 'j':
                    if (is_label) { p("houseNr"); break; }
                    pf("%# 11.7f", hposj);
                    break;
                case 'Z':
                    if (is_label) { p(slon); break; }
                    p(dms(x[0], round_flag | BIT_ZODIAC));
                    break;
                case 'S':
                case 's': {
                    final char next = si + 1 < fmt.length() ? fmt.charAt(si + 1) : '\0';
                    if ('S' == next || 's' == next || strpbrk(fmt, "XUxu")) {
                        for (int s2i = 0; s2i < fmt.length(); s2i++) {
                            final char sc2 = fmt.charAt(s2i);
                            if (s2i != 0) p(gap);
                            switch (sc2) {
                                case 'L':   /* speed! */
                                case 'Z':
                                    if (is_label) { p("lon/day"); break; }
                                    p(dms(x[3], round_flag));
                                    break;
                                case 'l':
                                    if (is_label) { p("lon/day"); break; }
                                    if (output_extra_prec) pf("%# 11.9f", x[3]);
                                    else pf("%# 11.7f", x[3]);
                                    break;
                                case 'B':
                                    if (is_label) { p("lat/day"); break; }
                                    p(dms(x[4], round_flag));
                                    break;
                                case 'b':
                                    if (is_label) { p("lat/day"); break; }
                                    if (output_extra_prec) pf("%# 11.9f", x[4]);
                                    else pf("%# 11.7f", x[4]);
                                    break;
                                case 'A':
                                    if (is_label) { p("RA/day"); break; }
                                    p(dms(xequ[3] / 15, round_flag | SEFLG_EQUATORIAL));
                                    break;
                                case 'a':
                                    if (is_label) { p("RA/day"); break; }
                                    if (output_extra_prec) pf("%# 11.9f", xequ[3]);
                                    else pf("%# 11.7f", xequ[3]);
                                    break;
                                case 'D':
                                    if (is_label) { p("dcl/day"); break; }
                                    p(dms(xequ[4], round_flag));
                                    break;
                                case 'd':
                                    if (is_label) { p("dcl/day"); break; }
                                    if (output_extra_prec) pf("%# 11.9f", xequ[4]);
                                    else pf("%# 11.7f", xequ[4]);
                                    break;
                                case 'R':
                                case 'r':
                                    if (is_label) { p("AU/day"); break; }
                                    if (output_extra_prec) pf("%# 18.16f", x[5]);
                                    else pf("%# 14.9f", x[5]);
                                    break;
                                case 'U':
                                case 'X':
                                    if (is_label) {
                                        p("speed_0"); p(gap); p("speed_1"); p(gap); p("speed_2");
                                        break;
                                    }
                                    ar = 'U' == sc ? Math.sqrt(square_sum(xcart)) : 1;
                                    pf("%# 14.9f", xcart[3] / ar);
                                    p(gap);
                                    pf("%# 14.9f", xcart[4] / ar);
                                    p(gap);
                                    pf("%# 14.9f", xcart[5] / ar);
                                    break;
                                case 'u':
                                case 'x':
                                    if (is_label) {
                                        p("speed_0"); p(gap); p("speed_1"); p(gap); p("speed_2");
                                        break;
                                    }
                                    ar = 'u' == sc ? Math.sqrt(square_sum(xcartq)) : 1;
                                    pf("%# 14.9f", xcartq[3] / ar);
                                    p(gap);
                                    pf("%# 14.9f", xcartq[4] / ar);
                                    p(gap);
                                    pf("%# 14.9f", xcartq[5] / ar);
                                    break;
                                default:
                                    break;
                            }
                        }
                        if ('S' == next || 's' == next) si++;
                    } else if ('S' == sc) {
                        int flag = round_flag;
                        if (is_house) flag |= BIT_ALLOW_361;  // speed of houses can be > 360
                        if (is_label) { p("deg/day"); break; }
                        p(dms(x[3], flag));
                    } else {
                        if (is_label) { p("deg/day"); break; }
                        if (output_extra_prec) pf("%# 11.17f", x[3]);
                        else pf("%# 11.7f", x[3]);
                    }
                    break;
                }
                case 'B':
                    if (is_label) { p("lat.    "); break; }
                    if ('q' == psp) {   /* delta t */
                        pf("%# 11.7f", x[1]);
                        p("h");
                        break;
                    }
                    p(dms(x[1], round_flag));
                    break;
                case 'b':
                    if (is_label) { p("lat.    "); break; }
                    if (output_extra_prec) pf("%# 11.11f", x[1]);
                    else pf("%# 11.7f", x[1]);
                    break;
                case 'A':       /* right ascension */
                    if (is_label) { p("RA      "); break; }
                    p(dms(xequ[0] / 15, round_flag | SEFLG_EQUATORIAL));
                    break;
                case 'a':
                    if (is_label) { p("RA      "); break; }
                    if (output_extra_prec) pf("%# 11.11f", xequ[0]);
                    else pf("%# 11.7f", xequ[0]);
                    break;
                case 'D':       /* declination */
                    if (is_label) { p("decl      "); break; }
                    p(dms(xequ[1], round_flag));
                    break;
                case 'd':
                    if (is_label) { p("decl      "); break; }
                    if (output_extra_prec) pf("%# 11.11f", xequ[1]);
                    else pf("%# 11.7f", xequ[1]);
                    break;
                case 'I':       /* azimuth */
                    if (is_label) { p("azimuth"); break; }
                    p(dms(xaz[0], round_flag));
                    break;
                case 'i':
                    if (is_label) { p("azimuth"); break; }
                    pf("%# 11.7f", xaz[0]);
                    break;
                case 'H':       /* height */
                    if (is_label) { p("height"); break; }
                    p(dms(xaz[1], round_flag));
                    break;
                case 'h':
                    if (is_label) { p("height"); break; }
                    pf("%# 11.7f", xaz[1]);
                    break;
                case 'K':       /* height (apparent) */
                    if (is_label) { p("hgtApp"); break; }
                    p(dms(xaz[2], round_flag));
                    break;
                case 'k':
                    if (is_label) { p("hgtApp"); break; }
                    pf("%# 11.7f", xaz[2]);
                    break;
                case 'R':
                    if (is_label) { p("distAU   "); break; }
                    if (output_extra_prec) pf("%# 18.16f", x[2]);
                    else pf("%# 14.9f", x[2]);
                    break;
                case 'W':
                    if (is_label) { p("distLY   "); break; }
                    pf("%# 14.9f", x[2] * SE_AUNIT_TO_LIGHTYEAR);
                    break;
                case 'w':
                    if (is_label) { p("distkm   "); break; }
                    pf("%# 14.9f", x[2] * SE_AUNIT_TO_KM);
                    break;
                case 'r':
                    if (is_label) { p("dist"); break; }
                    if (ipl == SE_MOON) {   /* for moon print parallax */
                        sw.swe_pheno(te, ipl, iflag, dret, serr);
                        pf("%# 13.5f\"", dret[5] * 3600);
                    } else {
                        pf("%# 14.9f", x[2]);
                    }
                    break;
                case 'q':
                    if (is_label) { p("reldist"); break; }
                    dar = get_geocentric_relative_distance(te, ipl, iflag);
                    pf("% 5d", dar);
                    break;
                case 'U':
                case 'X':
                    ar = 'U' == sc ? Math.sqrt(square_sum(xcart)) : 1;
                    pf("%# 14.9f", xcart[0] / ar);
                    p(gap);
                    pf("%# 14.9f", xcart[1] / ar);
                    p(gap);
                    pf("%# 14.9f", xcart[2] / ar);
                    break;
                case 'u':
                case 'x':
                    if (is_label) {
                        p("x0"); p(gap); p("x1"); p(gap); p("x2");
                        break;
                    }
                    ar = 'u' == sc ? Math.sqrt(square_sum(xcartq)) : 1;
                    if (output_extra_prec) {
                        pf("%# .17f", xcartq[0] / ar);
                        p(gap);
                        pf("%# .17f", xcartq[1] / ar);
                        p(gap);
                        pf("%# .17f", xcartq[2] / ar);
                    } else {
                        pf("%# 14.9f", xcartq[0] / ar);
                        p(gap);
                        pf("%# 14.9f", xcartq[1] / ar);
                        p(gap);
                        pf("%# 14.9f", xcartq[2] / ar);
                    }
                    break;
                case 'Q':
                    if (is_label) { p("Q"); break; }
                    pf("%-15s", spnam);
                    p(dms(x[0], round_flag));
                    p(dms(x[1], round_flag));
                    pf("  %# 14.9f", x[2]);
                    p(dms(x[3], round_flag));
                    p(dms(x[4], round_flag));
                    pf("  %# 14.9f\n", x[5]);
                    pf("               %s", dms(xequ[0], round_flag));
                    p(dms(xequ[1], round_flag));
                    pf("                %s", dms(xequ[3], round_flag));
                    p(dms(xequ[4], round_flag));
                    break;
                case 'N':
                case 'n': {
                    final double[] xasc = new double[6], xdsc = new double[6];
                    final int imeth = Character.isLowerCase(sc) ? SE_NODBIT_MEAN : SE_NODBIT_OSCU;
                    iflgret = sw.swe_nod_aps(te, ipl, iflag, imeth, xasc, xdsc, null, null, serr);
                    if (iflgret >= 0 && (ipl <= SE_NEPTUNE || 'N' == sc)) {
                        if (is_label) {
                            p("nodAsc"); p(gap); p("nodDesc");
                            break;
                        }
                        if (use_dms) p(dms(xasc[0], round_flag | BIT_ZODIAC));
                        else pf("%# 11.7f", xasc[0]);
                        p(gap);
                        if (use_dms) p(dms(xdsc[0], round_flag | BIT_ZODIAC));
                        else pf("%# 11.7f", xdsc[0]);
                    }
                    break;
                }
                case 'F':
                case 'f': {
                    if (!is_house) {
                        final double[] xfoc = new double[6], xaph = new double[6], xper = new double[6];
                        int imeth = Character.isLowerCase(sc) ? SE_NODBIT_MEAN : SE_NODBIT_OSCU;
                        iflgret = sw.swe_nod_aps(te, ipl, iflag, imeth, null, null, xper, xaph, serr);
                        if (iflgret >= 0 && (ipl <= SE_NEPTUNE || 'F' == sc)) {
                            if (is_label) {
                                p("peri"); p(gap); p("apo"); p(gap); p("focus");
                                break;
                            }
                            pf("%# 11.7f", xper[0]);
                            p(gap);
                            pf("%# 11.7f", xaph[0]);
                        }
                        imeth |= SE_NODBIT_FOPOINT;
                        iflgret = sw.swe_nod_aps(te, ipl, iflag, imeth, null, null, xper, xfoc, serr);
                        if (iflgret >= 0 && (ipl <= SE_NEPTUNE || 'F' == sc)) {
                            p(gap);
                            pf("%# 11.7f", xfoc[0]);
                        }
                    }
                    break;
                }
                case '+':
                    if (is_house) break;
                    if (is_label) { p("phase"); break; }
                    // if decimal longitude is present, do phase angle also decimal
                    if (fmt.indexOf('l') >= 0) pf("%# 11.7f", attr[0]);
                    else p(dms(attr[0], round_flag));
                    break;
                case '-':
                    if (is_label) { p("phase"); break; }
                    if (is_house) break;
                    pf("  %# 14.9f", attr[1]);
                    break;
                case '*':
                    if (is_label) { p("elong"); break; }
                    if (is_house) break;
                    if (fmt.indexOf('l') >= 0) pf("%# 11.7f", attr[2]);
                    else p(dms(attr[2], round_flag));
                    break;
                case '/':
                    if (is_label) { p("diamet"); break; }
                    if (is_house) break;
                    p(dms(attr[3], round_flag));
                    break;
                case '=':
                    if (is_label) { p("magn"); break; }
                    if (is_house) break;
                    pf("  %# 6.3fm", attr[4]);
                    break;
                case 'V':       /* human design gates */
                case 'v': {
                    final int[] hexa = {1, 43, 14, 34, 9, 5, 26, 11, 10, 58, 38, 54, 61, 60, 41,
                            19, 13, 49, 30, 55, 37, 63, 22, 36, 25, 17, 21, 51, 42, 3, 27, 24, 2,
                            23, 8, 20, 16, 35, 45, 12, 15, 52, 39, 53, 62, 56, 31, 33, 7, 4, 29,
                            59, 40, 64, 47, 6, 46, 18, 48, 57, 32, 50, 28, 44};
                    if (is_label) { p("hds"); break; }
                    if (is_house) break;
                    final double xhds = sw.swe_degnorm(x[0] - 223.25);
                    final int ihex = (int) Math.floor(xhds / 5.625);
                    final int iline = ((int) Math.floor(xhds / 0.9375)) % 6 + 1;
                    final int igate = hexa[ihex];
                    pf("%2d.%d", igate, iline);
                    if ('V' == sc) pf(" %2d%%", sw.swe_d2l(100 * (xhds / 0.9375 % 1)));
                    break;
                }
                case 'm': {     // Meridian distance
                    if (is_label) { p("MD      "); break; }
                    double md = sw.swe_difdeg2n(xequ[0], armc);
                    if (md < 0) md = -md;
                    if (output_extra_prec) pf("%# 11.11f", md);
                    else pf("%# 11.7f", md);
                    break;
                }
                case 'z': {     // Zenith distance
                    if (is_label) { p("ZD      "); break; }
                    sw.swe_azalt(tut, SE_EQU2HOR, geopos, datm[0], datm[1], xequ, xaz);
                    final double zd = 90 - xaz[1];
                    if (output_extra_prec) pf("%# 11.11f", zd);
                    else pf("%# 11.7f", zd);
                    break;
                }
                default:
                    break;
            }   /* switch */
        }       /* for sp */

        if (!list_hor) p("\n");
        return OK;
    }

    // --------------------------------------------------------------------------------- dms

    static String dms(double xv, int iflg) {
        int izod;
        long k, kdeg, kmin, ksec;
        String c = ODEGREE_STRING;
        final StringBuilder s = new StringBuilder();
        final int sgn;

        if (Double.isNaN(xv)) return "nan";
        if (xv >= 360 && (iflg & BIT_ALLOW_361) == 0) xv = 0;
        if ((iflg & SEFLG_EQUATORIAL) != 0) c = "h";
        if (xv < 0) {
            xv = -xv;
            sgn = -1;
        } else {
            sgn = 1;
        }

        if ((iflg & BIT_ROUND_MIN) != 0) {
            if ((iflg & BIT_ALLOW_361) == 0) xv = sw.swe_degnorm(xv + 0.5 / 60);
        } else if ((iflg & BIT_ROUND_SEC) != 0) {
            if ((iflg & BIT_ALLOW_361) == 0) xv = sw.swe_degnorm(xv + 0.5 / 3600);
        } else {
            /* rounding 0.9999999999 to 1 */
            if (output_extra_prec) xv += (xv < 0 ? -1 : 1) * 0.000000005 / 3600.0;
            else xv += (xv < 0 ? -1 : 1) * 0.00005 / 3600.0;
        }

        if ((iflg & BIT_ZODIAC) != 0) {
            izod = (int) (xv / 30);
            if (izod == 12) izod = 0;
            xv = xv % 30;
            kdeg = (long) xv;
            s.append(String.format(Locale.ROOT, "%2d %s ", kdeg, zod_nam[izod]));
        } else {
            kdeg = (long) xv;
            s.append(String.format(Locale.ROOT, " %3d%s", kdeg, c));
        }

        xv -= kdeg;
        xv *= 60;
        kmin = (long) xv;
        if ((iflg & BIT_ZODIAC) != 0 && (iflg & BIT_ROUND_MIN) != 0) {
            s.append(String.format(Locale.ROOT, "%2d", kmin));
        } else {
            s.append(String.format(Locale.ROOT, "%2d'", kmin));
        }

        if ((iflg & BIT_ROUND_MIN) == 0) {
            xv -= kmin;
            xv *= 60;
            ksec = (long) xv;
            if ((iflg & BIT_ROUND_SEC) != 0) {
                s.append(String.format(Locale.ROOT, "%2d\"", ksec));
            } else {
                s.append(String.format(Locale.ROOT, "%2d", ksec));
            }
            if ((iflg & BIT_ROUND_SEC) == 0) {
                xv -= ksec;
                if (output_extra_prec) {
                    k = (long) (xv * 100000000);
                    s.append(String.format(Locale.ROOT, ".%08d", k));
                } else {
                    k = (long) (xv * 10000);
                    s.append(String.format(Locale.ROOT, ".%04d", k));
                }
            }
        }

        // the C writes the sign into the character before the first digit, in place
        if (sgn < 0) {
            for (int i = 0; i < s.length(); i++) {
                if (Character.isDigit(s.charAt(i))) {
                    if (i > 0) s.setCharAt(i - 1, '-');
                    break;
                }
            }
        }
        if ((iflg & BIT_LZEROES) != 0) {
            for (int i = 2; i < s.length(); i++) {
                if (' ' == s.charAt(i)) s.setCharAt(i, '0');
            }
        }
        return s.toString();
    }

    // ------------------------------------------------------------------------- the helpers

    static int letter_to_ipl(int letter) {
        if (letter >= '0' && letter <= '9') return letter - '0' + SE_SUN;
        if (letter >= 'A' && letter <= 'I') return letter - 'A' + SE_MEAN_APOG;
        if (letter >= 'J' && letter <= 'Z') return letter - 'J' + SE_CUPIDO;
        switch (letter) {
            case 'm': return SE_MEAN_NODE;
            case 'c': return SE_INTP_APOG;
            case 'g': return SE_INTP_PERG;
            case 'n':
            case 'o': return SE_ECL_NUT;
            case 't': return SE_TRUE_NODE;
            case 'f': return SE_FIXSTAR;
            case 'w': return SE_WALDEMATH;
            case 'e':   /* swetest: a line of labels */
            case 'q':   /* swetest: delta t */
            case 'y':   /* swetest: time equation */
            case 'x':   /* swetest: sidereal time */
            case 'b':   /* swetest: ayanamsha */
            case 's':   /* swetest: an asteroid, with number given in -xs[number] */
            case 'v':   /* swetest: a planetary moon, with number given in -xv[number] */
            case 'z':   /* swetest: a fictitious body, number given in -xz[number] */
            case 'd':   /* swetest: default (main) factors 0123456789mtABC */
            case 'p':   /* swetest: main factors ('d') plus main asteroids DEFGHI */
            case 'h':   /* swetest: fictitious factors JKLMNOPQRSTUVWXYZw */
            case 'a':   /* swetest: all factors, like 'p'+'h' */
                return -1;
            default:
                return -2;
        }
    }

    static int call_swe_fixstar(String star, double te, int iflag, double[] x) {
        final StringBuilder sn = new StringBuilder(star);
        final int rc = use_swe_fixstar2
                ? sw.swe_fixstar2(sn, te, iflag, x, serr)
                : sw.swe_fixstar(sn, te, iflag, x, serr);
        SweTest.star = sn.toString();
        return rc;
    }

    /**
     * How far the object is from its own mean distance, as a percentage - swetest's {@code q}
     * column. 0 is the mean distance, -100 the perihelion, 100 the aphelion.
     */
    static int get_geocentric_relative_distance(double tjd_et, int ipl, int iflag) {
        final double[] dmax = new double[1], dmin = new double[1], dtrue = new double[1];
        if (sw.swe_orbit_max_min_true_distance(tjd_et, ipl, iflag, dmax, dmin, dtrue, serr) == ERR)
            return 0;
        if (dmax[0] - dmin[0] == 0) return 0;
        final double dtemp = (dtrue[0] - dmin[0]) / (dmax[0] - dmin[0]);
        return (int) (1000.001 - dtemp * 1000);
    }

    static String jd_to_time_string(double jut) {
        double t2 = jut + 0.5 / 3600000.0;       // rounding to millisec
        final StringBuilder s = new StringBuilder(String.format(Locale.ROOT, "  % 2d:", (int) t2));
        t2 = (t2 - (int) t2) * 60;
        s.append(String.format(Locale.ROOT, "%02d:", (int) t2));                 // min
        t2 = (t2 - (int) t2) * 60;
        s.append(String.format(Locale.ROOT, "%02d", (int) t2));                  // sec
        t2 = (t2 - (int) t2) * 1000;
        if ((int) t2 > 0) s.append(String.format(Locale.ROOT, ".%03d", (int) t2)); // millisec
        return s.toString();
    }

    static String hms_from_tjd(double tjd) {
        /* tjd may be negative, 0h corresponds to day number 9999999.5 */
        double x = tjd % 1;             /* may be negative ! */
        x = (x + 1.5) % 1;              /* is positive day fraction */
        return hms(x * 24, BIT_LZEROES) + " ";
    }

    /**
     * {@code dms} re-punctuated as a clock time, exactly as the C does it: the degree sign
     * becomes the first colon, the minutes' apostrophe the second, and the string is cut
     * after the tenth of a second - which is also what the added half of 0.1s rounds to.
     */
    static String hms(double x, int iflag) {
        x += 0.5 / 36000.0;             /* round to 0.1 sec */
        final String s = dms(x, iflag);
        final int at = s.indexOf(ODEGREE_STRING);
        if (at < 0) return s;

        final StringBuilder b = new StringBuilder(s.length());
        b.append(s, 0, at).append(':').append(s.substring(at + ODEGREE_STRING.length()));
        if (b.length() > at + 3) b.setCharAt(at + 3, ':');
        return b.length() > at + 8 ? b.substring(0, at + 8) : b.toString();
    }

    static double square_sum(double[] v) {
        return v[0] * v[0] + v[1] * v[1] + v[2] * v[2];
    }

    // ---------------------------------------------------------------------- the eclipses

    static int call_lunar_eclipse(double t_ut, int whicheph, int special_mode, double[] geopos) {
        int retc = OK, eclflag, ecl_type = 0;
        final double[] eattr = new double[30], xx = new double[6];
        final double[] geopos_max = new double[3];
        String styp = "none", sgj;
        final int[] jd = new int[3];
        final double[] jt = new double[1];
        final double[] tmp = new double[1];

        if (with_chart_link) p("<pre>");
        /* no selective eclipse type set, set all */
        if ((search_flag & SE_ECL_ALLTYPES_LUNAR) == 0) search_flag |= SE_ECL_ALLTYPES_LUNAR;
        if ((special_mode & SP_MODE_LOCAL) != 0 && with_header)
            pf("\ngeo. long %f, lat %f, alt %f", geopos[0], geopos[1], geopos[2]);
        p("\n");

        for (int ii = 0; ii < nstep; ii++, t_ut += direction) {
            final StringBuilder s = new StringBuilder();
            String sout_short = "";

            /* swetest -lunecl -how: type and percentage for a given time */
            if ((special_mode & SP_MODE_HOW) != 0) {
                eclflag = sw.swe_lun_eclipse_how(t_ut, whicheph, geopos, eattr, serr);
                if (eclflag == ERR) { p(serr.toString()); return ERR; }
                if ((eclflag & SE_ECL_TOTAL) != 0)
                    pf("total lunar eclipse: %f o/o \n", eattr[0]);
                else if ((eclflag & SE_ECL_PARTIAL) != 0)
                    pf("partial lunar eclipse: %f o/o \n", eattr[0]);
                else if ((eclflag & SE_ECL_PENUMBRAL) != 0)
                    pf("penumbral lunar eclipse: %f o/o \n", eattr[0]);
                else
                    p("no lunar eclipse \n");
                continue;
            }

            if ((special_mode & SP_MODE_LOCAL) != 0) {
                /* locally visible lunar eclipse */
                eclflag = sw.swe_lun_eclipse_when_loc(t_ut, whicheph, geopos, tret, eattr,
                        direction_flag ? 1 : 0, serr);
                if (eclflag == ERR) { p(serr.toString()); return ERR; }
                if ((time_flag & (BIT_TIME_LMT | BIT_TIME_LAT)) != 0) {
                    for (int i = 0; i < 10; i++) {
                        if (tret[i] != 0) {
                            retc = ut_to_lmt_lat(tret[i], geopos, tmp);
                            tret[i] = tmp[0];
                            if (retc == ERR) { p(serr.toString()); return ERR; }
                        }
                    }
                }
                t_ut = tret[0];
                if ((eclflag & SE_ECL_TOTAL) != 0) { s.setLength(0); s.append("total   "); ecl_type = 3; }
                if ((eclflag & SE_ECL_PENUMBRAL) != 0) { s.setLength(0); s.append("penumb. "); ecl_type = 1; }
                if ((eclflag & SE_ECL_PARTIAL) != 0) { s.setLength(0); s.append("partial "); ecl_type = 2; }
                s.append("lunar eclipse\t");

                sw.swe_revjul(t_ut, gregflag, jd, jt);
                jyear = jd[0]; jmon = jd[1]; jday = jd[2]; jut = jt[0];
                sgj = get_gregjul(gregflag, jyear);
                final double dt = (tret[3] - tret[2]) * 24 * 60;
                final String s1 = String.format(Locale.ROOT, "%d min %4.2f sec", (int) dt, (dt % 1) * 60);
                final String saros = String.format(Locale.ROOT, "%d/%d", (int) eattr[9], (int) eattr[10]);

                sout_short = String.format(Locale.ROOT, "%s\t%2d.%2d.%4d%s\t%s\t%.3f\t%s\t%s\n",
                        s, jday, jmon, jyear, sgj, hms(jut, 0), eattr[8], s1, saros);
                s.append(String.format(Locale.ROOT, "%2d.%02d.%04d%s\t%s\t%.4f/%.4f\tsaros %s\t%.6f\n",
                        jday, jmon, jyear, sgj, hms(jut, BIT_LZEROES), eattr[0], eattr[1], saros, t_ut));

                if (have_gap_parameter) s.append('\t');
                if ((eclflag & SE_ECL_PENUMBBEG_VISIBLE) != 0)
                    s.append("  ").append(hms_from_tjd(tret[6])).append(' ');
                else s.append("      -         ");
                if (have_gap_parameter) s.append('\t');
                s.append(visibleOrDash(eclflag, SE_ECL_PARTBEG_VISIBLE, tret[2]));
                if (have_gap_parameter) s.append('\t');
                s.append(visibleOrDash(eclflag, SE_ECL_TOTBEG_VISIBLE, tret[4]));
                if (have_gap_parameter) s.append('\t');
                s.append(visibleOrDash(eclflag, SE_ECL_TOTEND_VISIBLE, tret[5]));
                if (have_gap_parameter) s.append('\t');
                s.append(visibleOrDash(eclflag, SE_ECL_PARTEND_VISIBLE, tret[3]));
                if (have_gap_parameter) s.append('\t');
                s.append(visibleOrDash(eclflag, SE_ECL_PENUMBEND_VISIBLE, tret[7]));
                if (have_gap_parameter) s.append('\t');
                s.append(String.format(Locale.ROOT, "dt=%.1f",
                        sw.swe_deltat_ex(tret[0], whicheph, serr) * 86400.0));
                s.append('\n');
            } else {
                /* global lunar eclipse */
                eclflag = sw.swe_lun_eclipse_when(t_ut, whicheph, search_flag, tret,
                        direction_flag ? 1 : 0, serr);
                if (eclflag == ERR) { p(serr.toString()); return ERR; }
                t_ut = tret[0];
                if ((eclflag & SE_ECL_TOTAL) != 0) { styp = "Total"; s.setLength(0); s.append("total "); ecl_type = 3; }
                if ((eclflag & SE_ECL_PENUMBRAL) != 0) { styp = "Penumbral"; s.setLength(0); s.append("penumb. "); ecl_type = 1; }
                if ((eclflag & SE_ECL_PARTIAL) != 0) { styp = "Partial"; s.setLength(0); s.append("partial "); ecl_type = 2; }
                s.append("lunar eclipse\t");

                eclflag = sw.swe_lun_eclipse_how(t_ut, whicheph, geopos, eattr, serr);
                if (eclflag == ERR) { p(serr.toString()); return ERR; }
                if ((time_flag & (BIT_TIME_LMT | BIT_TIME_LAT)) != 0) {
                    for (int i = 0; i < 10; i++) {
                        if (tret[i] != 0) {
                            retc = ut_to_lmt_lat(tret[i], geopos, tmp);
                            tret[i] = tmp[0];
                            if (retc == ERR) { p(serr.toString()); return ERR; }
                        }
                    }
                }
                t_ut = tret[0];
                final StringBuilder e1 = new StringBuilder();
                if (sw.swe_calc_ut(t_ut, SE_MOON, whicheph | SEFLG_EQUATORIAL, xx, e1) < 0) {
                    p(e1.toString());
                    p("\n");
                }
                sw.swe_revjul(t_ut, gregflag, jd, jt);
                jyear = jd[0]; jmon = jd[1]; jday = jd[2]; jut = jt[0];
                geopos_max[0] = sw.swe_degnorm(xx[0] - sw.swe_sidtime(t_ut) * 15);
                if (geopos_max[0] > 180) geopos_max[0] -= 360;
                geopos_max[1] = xx[1];
                sgj = get_gregjul(gregflag, jyear);
                final double dt = (tret[3] - tret[2]) * 24 * 60;
                final String s1 = String.format(Locale.ROOT, "%d min %4.2f sec", (int) dt, (dt % 1) * 60);
                final String saros = String.format(Locale.ROOT, "%d/%d", (int) eattr[9], (int) eattr[10]);

                sout_short = String.format(Locale.ROOT, "%s\t%2d.%2d.%4d%s\t%s\t%.3f\t%s\t%s\n",
                        s, jday, jmon, jyear, sgj, hms(jut, 0), eattr[8], s1, saros);
                s.append(String.format(Locale.ROOT, "%2d.%02d.%04d%s\t%s\t%.4f/%.4f\tsaros %s\t%.6f\n",
                        jday, jmon, jyear, sgj, hms(jut, BIT_LZEROES), eattr[0], eattr[1], saros, t_ut));

                if (have_gap_parameter) s.append('\t');
                s.append("  ").append(hms_from_tjd(tret[6])).append(' ');
                if (have_gap_parameter) s.append('\t');
                s.append(setOrDash(tret[2]));
                if (have_gap_parameter) s.append('\t');
                s.append(setOrDash(tret[4]));
                if (have_gap_parameter) s.append('\t');
                s.append(setOrDash(tret[5]));
                if (have_gap_parameter) s.append('\t');
                s.append(setOrDash(tret[3]));
                if (have_gap_parameter) s.append('\t');
                s.append(hms_from_tjd(tret[7]));
                if (have_gap_parameter) s.append('\t');
                s.append(String.format(Locale.ROOT, "dt=%.1f",
                        sw.swe_deltat_ex(tret[0], whicheph, serr) * 86400.0));
                s.append('\n');

                if ((special_mode & SP_MODE_HOCAL) != 0) {
                    final int[] hms = new int[3];
                    final double[] dfrc = new double[1];
                    final int[] isgn = new int[1];
                    sw.swe_split_deg(jut, SE_SPLIT_DEG_ROUND_MIN, hms, dfrc, isgn);
                    s.setLength(0);
                    s.append(String.format(Locale.ROOT, "\"%04d%s %02d %02d %02d.%02d %d\",\n",
                            jyear, sgj, jmon, jday, hms[0], hms[1], ecl_type));
                }
                s.append(String.format(Locale.ROOT, "\t%s\t%s\n",
                        dms(geopos_max[0], BIT_ROUND_SEC), dms(geopos_max[1], BIT_ROUND_SEC)));
            }

            p(short_output ? sout_short : insert_gap_string_for_tabs(s.toString()));
            if (with_chart_link) chart_link("Lunar Eclipse", styp, geopos_max);
        }
        if (with_chart_link) p("</pre>\n");
        return OK;
    }

    static int call_solar_eclipse(double t_ut, int whicheph, int special_mode, double[] geopos) {
        int retc = OK, eclflag, ecl_type = 0;
        final double[] eattr = new double[30], geopos_max = new double[10];
        String styp = "none", sgj;
        boolean has_found;
        final int[] jd = new int[3];
        final double[] jt = new double[1];
        final double[] tmp = new double[1];

        if (with_chart_link) p("<pre>");
        /* no selective eclipse type set, set all */
        if ((search_flag & SE_ECL_ALLTYPES_SOLAR) == 0) search_flag |= SE_ECL_ALLTYPES_SOLAR;
        /* for local eclipses: set geographic position of observer */
        if ((special_mode & SP_MODE_LOCAL) != 0) {
            sw.swe_set_topo(geopos[0], geopos[1], geopos[2]);
            if (with_header)
                pf("\ngeo. long %f, lat %f, alt %f", geopos[0], geopos[1], geopos[2]);
        }
        p("\n");

        for (int ii = 0; ii < nstep; ii++, t_ut += direction) {
            final StringBuilder s = new StringBuilder();

            /* -solecl -local: next eclipse observable from a given position */
            if ((special_mode & SP_MODE_LOCAL) != 0) {
                eclflag = sw.swe_sol_eclipse_when_loc(t_ut, whicheph, geopos, tret, eattr,
                        direction_flag ? 1 : 0, serr);
                if (eclflag == ERR) { p(serr.toString()); return ERR; }
                has_found = false;
                t_ut = tret[0];
                if ((search_flag & SE_ECL_TOTAL) != 0 && (eclflag & SE_ECL_TOTAL) != 0) {
                    s.setLength(0); s.append("total   "); has_found = true; ecl_type = 6;
                }
                if ((search_flag & SE_ECL_ANNULAR) != 0 && (eclflag & SE_ECL_ANNULAR) != 0) {
                    s.setLength(0); s.append("annular "); has_found = true; ecl_type = 5;
                }
                if ((search_flag & SE_ECL_PARTIAL) != 0 && (eclflag & SE_ECL_PARTIAL) != 0) {
                    s.setLength(0); s.append("partial "); has_found = true; ecl_type = 4;
                }
                if (have_gap_parameter) s.append('\t');
                if (!has_found) {
                    ii--;
                    continue;
                }
                sw.swe_calc(t_ut + sw.swe_deltat_ex(t_ut, whicheph, serr),
                        SE_ECL_NUT, 0, x, serr);
                if ((time_flag & (BIT_TIME_LMT | BIT_TIME_LAT)) != 0) {
                    for (int i = 0; i < 10; i++) {
                        if (tret[i] != 0) {
                            retc = ut_to_lmt_lat(tret[i], geopos, tmp);
                            tret[i] = tmp[0];
                            if (retc == ERR) { p(serr.toString()); return ERR; }
                        }
                    }
                }
                t_ut = tret[0];
                sw.swe_revjul(t_ut, gregflag, jd, jt);
                jyear = jd[0]; jmon = jd[1]; jday = jd[2]; jut = jt[0];
                final double dt = (tret[3] - tret[2]) * 24 * 60;
                sgj = get_gregjul(gregflag, jyear);
                final String saros = String.format(Locale.ROOT, "%d/%d", (int) eattr[9], (int) eattr[10]);
                s.append(String.format(Locale.ROOT,
                        "%2d.%02d.%04d%s\t%s\t%.4f/%.4f/%.4f\tsaros %s\t%.6f\n",
                        jday, jmon, jyear, sgj, hms(jut, BIT_LZEROES),
                        eattr[8], eattr[0], eattr[2], saros, t_ut));
                s.append(String.format(Locale.ROOT, "\t%d min %4.2f sec\t", (int) dt, (dt % 1) * 60));
                s.append(visibleOrDash(eclflag, SE_ECL_1ST_VISIBLE, tret[1]));
                if (have_gap_parameter) s.append('\t');
                s.append(visibleOrDash(eclflag, SE_ECL_2ND_VISIBLE, tret[2]));
                if (have_gap_parameter) s.append('\t');
                s.append(visibleOrDash(eclflag, SE_ECL_3RD_VISIBLE, tret[3]));
                if (have_gap_parameter) s.append('\t');
                s.append(visibleOrDash(eclflag, SE_ECL_4TH_VISIBLE, tret[4]));
                if (have_gap_parameter) s.append('\t');
                s.append(String.format(Locale.ROOT, "dt=%.1f",
                        sw.swe_deltat_ex(tret[0], whicheph, serr) * 86400.0));
                s.append('\n');
                p(insert_gap_string_for_tabs(s.toString()));
                continue;
            }

            /* -solecl: next eclipse observable from anywhere on earth */
            eclflag = sw.swe_sol_eclipse_when_glob(t_ut, whicheph, search_flag, tret,
                    direction_flag ? 1 : 0, serr);
            if (eclflag == ERR) { p(serr.toString()); return ERR; }
            t_ut = tret[0];
            if ((eclflag & SE_ECL_TOTAL) != 0) { styp = "Total"; s.setLength(0); s.append("total"); ecl_type = 6; }
            if ((eclflag & SE_ECL_ANNULAR) != 0) { styp = "Annular"; s.setLength(0); s.append("annular"); ecl_type = 5; }
            if ((eclflag & SE_ECL_ANNULAR_TOTAL) != 0) { styp = "Annular-Total"; s.setLength(0); s.append("ann-tot"); ecl_type = 5; }
            if ((eclflag & SE_ECL_PARTIAL) != 0) { styp = "Partial"; s.setLength(0); s.append("partial"); ecl_type = 4; }
            if ((eclflag & SE_ECL_NONCENTRAL) != 0 && (eclflag & SE_ECL_PARTIAL) == 0)
                s.append(" non-central");
            s.append(" solar\t");

            sw.swe_sol_eclipse_where(t_ut, whicheph, geopos_max, eattr, serr);
            if ((time_flag & (BIT_TIME_LMT | BIT_TIME_LAT)) != 0) {
                for (int i = 0; i < 10; i++) {
                    if (tret[i] != 0) {
                        retc = ut_to_lmt_lat(tret[i], geopos, tmp);
                        tret[i] = tmp[0];
                        if (retc == ERR) { p(serr.toString()); return ERR; }
                    }
                }
            }
            sw.swe_revjul(tret[0], gregflag, jd, jt);
            jyear = jd[0]; jmon = jd[1]; jday = jd[2]; jut = jt[0];
            sgj = get_gregjul(gregflag, jyear);
            final String saros = String.format(Locale.ROOT, "%d/%d", (int) eattr[9], (int) eattr[10]);

            final StringBuilder sshort = new StringBuilder(String.format(Locale.ROOT,
                    "%s\t%2d.%2d.%4d%s\t%s\t%.3f", s, jday, jmon, jyear, sgj, hms(jut, 0), eattr[8]));
            s.append(String.format(Locale.ROOT,
                    "%2d.%02d.%04d%s\t%s\t%f km\t%.4f/%.4f/%.4f\tsaros %s\t%.6f\n",
                    jday, jmon, jyear, sgj, hms(jut, 0), eattr[3],
                    eattr[8], eattr[0], eattr[2], saros, tret[0]));
            s.append('\t').append(hms_from_tjd(tret[2])).append(' ');
            if (have_gap_parameter) s.append('\t');
            s.append(setOrDash(tret[4]));
            if (have_gap_parameter) s.append('\t');
            s.append(setOrDash(tret[5]));
            if (have_gap_parameter) s.append('\t');
            s.append(hms_from_tjd(tret[3]));
            if (have_gap_parameter) s.append('\t');
            s.append(String.format(Locale.ROOT, "dt=%.1f",
                    sw.swe_deltat_ex(tret[0], whicheph, serr) * 86400.0));
            s.append('\n');
            s.append(String.format(Locale.ROOT, "\t%s\t%s",
                    dms(geopos_max[0], BIT_ROUND_SEC), dms(geopos_max[1], BIT_ROUND_SEC)));
            s.append('\t');
            sshort.append('\t');

            if ((eclflag & SE_ECL_PARTIAL) == 0 && (eclflag & SE_ECL_NONCENTRAL) == 0) {
                eclflag = sw.swe_sol_eclipse_when_loc(t_ut - 10, whicheph, geopos_max,
                        tret, eattr, 0, serr);
                if (eclflag == ERR) { p(serr.toString()); return ERR; }
                if (Math.abs(tret[0] - t_ut) > 2) p("when_loc returns wrong date\n");
                final double dt = (tret[3] - tret[2]) * 24 * 60;
                final String s1 = String.format(Locale.ROOT, "%d min %4.2f sec", (int) dt, (dt % 1) * 60);
                s.append(s1);
                sshort.append(s1);
            }
            sshort.append(String.format(Locale.ROOT, "\t%d\t%d", (int) eattr[9], (int) eattr[10]));
            s.append('\n');
            sshort.append('\n');

            if ((special_mode & SP_MODE_HOCAL) != 0) {
                final int[] hms = new int[3];
                final double[] dfrc = new double[1];
                final int[] isgn = new int[1];
                sw.swe_split_deg(jut, SE_SPLIT_DEG_ROUND_MIN, hms, dfrc, isgn);
                s.setLength(0);
                s.append(String.format(Locale.ROOT, "\"%04d%s %02d %02d %02d.%02d %d\",\n",
                        jyear, sgj, jmon, jday, hms[0], hms[1], ecl_type));
            }

            p(short_output ? sshort.toString() : insert_gap_string_for_tabs(s.toString()));
            if (with_chart_link) chart_link("Solar Eclipse", styp, geopos_max);
        }
        if (with_chart_link) p("</pre>\n");
        return OK;
    }

    // ------------------------------------------------------- occultations and heliacal events

    static int call_lunar_occultation(double t_ut, int ipl, String star, int whicheph,
                                      int special_mode, double[] geopos) {
        int ecl_type = 0, eclflag, retc = OK;
        final double[] oattr = new double[30], geopos_max = new double[3];
        final double[] tmp = new double[1];
        final int[] jd = new int[3];
        final double[] jt = new double[1];
        boolean has_found;
        int nloops = 0;
        final StringBuilder sn = new StringBuilder(null == star ? "" : star);

        /* no selective eclipse type set, set all */
        if ((search_flag & SE_ECL_ALLTYPES_SOLAR) == 0) search_flag |= SE_ECL_ALLTYPES_SOLAR;
        /* for local occultations: set geographic position of observer */
        if ((special_mode & SP_MODE_LOCAL) != 0) {
            sw.swe_set_topo(geopos[0], geopos[1], geopos[2]);
            if (with_header)
                pf("\ngeo. long %f, lat %f, alt %f", geopos[0], geopos[1], geopos[2]);
        }
        p("\n");

        for (int ii = 0; ii < nstep; ii++) {
            final StringBuilder s = new StringBuilder();
            nloops++;
            if (nloops > SEARCH_RANGE_LUNAR_CYCLES) {
                pf("event search ended after %d lunar cycles at jd=%f\n",
                        SEARCH_RANGE_LUNAR_CYCLES, t_ut);
                return ERR;
            }

            if ((special_mode & SP_MODE_LOCAL) != 0) {
                /* local search, one lunar cycle only (SE_ECL_ONE_TRY) */
                if (ipl != SE_SUN) {
                    search_flag &= ~(SE_ECL_ANNULAR | SE_ECL_ANNULAR_TOTAL);
                    if (search_flag == 0) search_flag = SE_ECL_ALLTYPES_SOLAR;
                }
                eclflag = sw.swe_lun_occult_when_loc(t_ut, ipl, sn, whicheph, geopos,
                        tret, oattr, (direction_flag ? 1 : 0) | SE_ECL_ONE_TRY, serr);
                if (eclflag == ERR) { p(serr.toString()); return ERR; }
                if (eclflag == 0) {     /* event not found, try next conjunction */
                    t_ut = tret[0] + direction * 10;
                    ii--;
                    continue;
                }
                t_ut = tret[0];
                if ((time_flag & (BIT_TIME_LMT | BIT_TIME_LAT)) != 0) {
                    for (int i = 0; i < 10; i++) {
                        if (tret[i] != 0) {
                            retc = ut_to_lmt_lat(tret[i], geopos, tmp);
                            tret[i] = tmp[0];
                            if (retc == ERR) { p(serr.toString()); return ERR; }
                        }
                    }
                }
                has_found = false;
                if ((search_flag & SE_ECL_TOTAL) != 0 && (eclflag & SE_ECL_TOTAL) != 0) {
                    s.append("total"); has_found = true; ecl_type = 6;
                }
                if ((search_flag & SE_ECL_ANNULAR) != 0 && (eclflag & SE_ECL_ANNULAR) != 0) {
                    s.append("annular"); has_found = true; ecl_type = 5;
                }
                if ((search_flag & SE_ECL_PARTIAL) != 0 && (eclflag & SE_ECL_PARTIAL) != 0) {
                    s.append("partial"); has_found = true; ecl_type = 4;
                }
                if (ipl != SE_SUN) {
                    final boolean beg = (eclflag & SE_ECL_OCC_BEG_DAYLIGHT) != 0;
                    final boolean end = (eclflag & SE_ECL_OCC_END_DAYLIGHT) != 0;
                    if (beg && end) s.append("(daytime)");
                    else if (beg) s.append("(sunset) ");
                    else if (end) s.append("(sunrise)");
                }
                if (have_gap_parameter) s.append('\t');
                while (s.length() < 17) s.append(' ');
                if (!has_found) {
                    ii--;
                    continue;
                }
                sw.swe_calc_ut(t_ut, SE_ECL_NUT, 0, x, serr);
                sw.swe_revjul(tret[0], gregflag, jd, jt);
                jyear = jd[0]; jmon = jd[1]; jday = jd[2]; jut = jt[0];
                final double dt = (tret[3] - tret[2]) * 24 * 60;
                s.append(String.format(Locale.ROOT, "%2d.%02d.%04d\t%s\t%f\t%.6f\n",
                        jday, jmon, jyear, hms(jut, BIT_LZEROES), oattr[0], tret[0]));
                s.append(String.format(Locale.ROOT, "\t%d min %4.2f sec\t", (int) dt, (dt % 1) * 60));
                s.append(visibleOrDash(eclflag, SE_ECL_1ST_VISIBLE, tret[1]));
                if (have_gap_parameter) s.append('\t');
                s.append(visibleOrDash(eclflag, SE_ECL_2ND_VISIBLE, tret[2]));
                if (have_gap_parameter) s.append('\t');
                s.append(visibleOrDash(eclflag, SE_ECL_3RD_VISIBLE, tret[3]));
                if (have_gap_parameter) s.append('\t');
                s.append(visibleOrDash(eclflag, SE_ECL_4TH_VISIBLE, tret[4]));
                if (have_gap_parameter) s.append('\t');
                s.append(String.format(Locale.ROOT, "dt=%.1f",
                        sw.swe_deltat_ex(tret[0], whicheph, serr) * 86400.0));
                s.append('\n');
                p(insert_gap_string_for_tabs(s.toString()));
            } else {
                /* global search, one lunar cycle only (SE_ECL_ONE_TRY) */
                eclflag = sw.swe_lun_occult_when_glob(t_ut, ipl, sn, whicheph, search_flag,
                        tret, (direction_flag ? 1 : 0) | SE_ECL_ONE_TRY, serr);
                if (eclflag == ERR) { p(serr.toString()); return ERR; }
                if (eclflag == 0) {     /* nothing at this conjunction, try the next */
                    t_ut = tret[0] + direction;
                    ii--;
                    continue;
                }
                if ((eclflag & SE_ECL_TOTAL) != 0) { s.setLength(0); s.append("total   "); ecl_type = 6; }
                if ((eclflag & SE_ECL_ANNULAR) != 0) { s.setLength(0); s.append("annular "); ecl_type = 5; }
                if ((eclflag & SE_ECL_ANNULAR_TOTAL) != 0) { s.setLength(0); s.append("ann-tot "); ecl_type = 5; }
                if ((eclflag & SE_ECL_PARTIAL) != 0) { s.setLength(0); s.append("partial "); ecl_type = 4; }
                if ((eclflag & SE_ECL_NONCENTRAL) != 0 && (eclflag & SE_ECL_PARTIAL) == 0)
                    s.append("non-central ");
                t_ut = tret[0];
                sw.swe_lun_occult_where(t_ut, ipl, sn, whicheph, geopos_max, oattr, serr);
                if ((time_flag & (BIT_TIME_LMT | BIT_TIME_LAT)) != 0) {
                    for (int i = 0; i < 10; i++) {
                        if (tret[i] != 0) {
                            retc = ut_to_lmt_lat(tret[i], geopos, tmp);
                            tret[i] = tmp[0];
                            if (retc == ERR) { p(serr.toString()); return ERR; }
                        }
                    }
                }
                sw.swe_revjul(tret[0], gregflag, jd, jt);
                jyear = jd[0]; jmon = jd[1]; jday = jd[2]; jut = jt[0];
                s.append(String.format(Locale.ROOT, "%2d.%02d.%04d\t%s\t%f km\t%f\t%.6f\n",
                        jday, jmon, jyear, hms(jut, BIT_LZEROES), oattr[3], oattr[0], tret[0]));
                s.append('\t').append(hms_from_tjd(tret[2])).append(' ');
                if (have_gap_parameter) s.append('\t');
                s.append(setOrDash(tret[4]));
                if (have_gap_parameter) s.append('\t');
                s.append(setOrDash(tret[5]));
                if (have_gap_parameter) s.append('\t');
                s.append(hms_from_tjd(tret[3]));
                if (have_gap_parameter) s.append('\t');
                s.append(String.format(Locale.ROOT, "dt=%.1f",
                        sw.swe_deltat_ex(tret[0], whicheph, serr) * 86400.0));
                s.append('\n');
                s.append(String.format(Locale.ROOT, "\t%s\t%s",
                        dms(geopos_max[0], BIT_ROUND_MIN), dms(geopos_max[1], BIT_ROUND_MIN)));
                if ((eclflag & SE_ECL_PARTIAL) == 0 && (eclflag & SE_ECL_NONCENTRAL) == 0) {
                    eclflag = sw.swe_lun_occult_when_loc(t_ut - 10, ipl, sn, whicheph,
                            geopos_max, tret, oattr, 0, serr);
                    if (eclflag == ERR) { p(serr.toString()); return ERR; }
                    if (Math.abs(tret[0] - t_ut) > 2) p("when_loc returns wrong date\n");
                    final double dt = (tret[3] - tret[2]) * 24 * 60;
                    s.append(String.format(Locale.ROOT, "\t%d min %4.2f sec", (int) dt, (dt % 1) * 60));
                }
                s.append('\n');

                String line = insert_gap_string_for_tabs(s.toString());
                if ((special_mode & SP_MODE_HOCAL) != 0) {
                    final int[] hms = new int[3];
                    final double[] dfrc = new double[1];
                    final int[] isgn = new int[1];
                    sw.swe_split_deg(jut, SE_SPLIT_DEG_ROUND_MIN, hms, dfrc, isgn);
                    line = String.format(Locale.ROOT, "\"%04d %02d %02d %02d.%02d %d\",\n",
                            jyear, jmon, jday, hms[0], hms[1], ecl_type);
                }
                p(line);
            }
            t_ut += direction;
        }
        return OK;
    }

    static final String[] sevtname = {"",
            "heliacal rising ",
            "heliacal setting",
            "evening first   ",
            "morning last    ",
            "evening rising  ",
            "morning setting "};

    static void do_print_heliacal(double[] dret, int event_type, String obj_name) {
        String stz = "UT";
        if ((time_flag & BIT_TIME_LMT) != 0) stz = "LMT";
        if ((time_flag & BIT_TIME_LAT) != 0) stz = "LAT";

        final int[] jd = new int[3];
        final double[] jt = new double[1];
        sw.swe_revjul(dret[0], gregflag, jd, jt);
        jyear = jd[0]; jmon = jd[1]; jday = jd[2]; jut = jt[0];

        final String stim0 = remove_whitespace(hms_from_tjd(dret[0]));
        if (event_type <= 4) {
            if (hel_using_AV) {
                /* only the beginning of visibility */
                pf("%s %s: %d/%02d/%02d %s %s (%.5f)\n", obj_name, sevtname[event_type],
                        jyear, jmon, jday, stim0, stz, dret[0]);
            } else {
                /* the moment of beginning and of optimum visibility */
                final String stim1 = remove_whitespace(hms_from_tjd(dret[1]));
                final String stim2 = remove_whitespace(hms_from_tjd(dret[2]));
                pf("%s %s: %d/%02d/%02d %s %s (%.5f), opt %s, end %s, dur %.1f min\n",
                        obj_name, sevtname[event_type], jyear, jmon, jday, stim0, stz,
                        dret[0], stim1, stim2, (dret[2] - dret[0]) * 1440);
            }
        } else {
            pf("%s %s: %d/%02d/%02d %s %s (%f)\n", obj_name, sevtname[event_type],
                    jyear, jmon, jday, stim0, stz, dret[0]);
        }
    }

    static int call_heliacal_event(double t_ut, int ipl, String star, int whicheph,
                                   double[] geopos, double[] datm, double[] dobs) {
        int retc = OK, event_type, retflag;
        final double[] dret = new double[40];
        double tsave1, tsave2 = 0;
        final String obj_name;

        helflag |= whicheph;
        /* if an invalid heliacal event type was required, take any type */
        if (search_flag < 0 || search_flag > 6) search_flag = 0;
        /* optical instruments used: */
        if (dobs[3] > 0) helflag |= SE_HELFLAG_OPTICAL_PARAMS;
        if (hel_using_AV) helflag |= SE_HELFLAG_AV;

        obj_name = ipl == SE_FIXSTAR ? star : sw.swe_get_planet_name(ipl);
        if (with_header) {
            pf("\ngeo. long %f, lat %f, alt %f", geopos[0], geopos[1], geopos[2]);
            p("\n");
        }

        for (int ii = 0; ii < nstep; ii++, t_ut = dret[0] + 1) {
            if (search_flag > 0) event_type = search_flag;
            else if (ipl == SE_MOON) event_type = SE_EVENING_FIRST;
            else event_type = SE_HELIACAL_RISING;

            retflag = sw.swe_heliacal_ut(t_ut, geopos, datm, dobs, obj_name,
                    event_type, helflag, dret, serr);
            if (retflag == ERR) { p(serr.toString()); return ERR; }
            if ((retc = heliacal_to_lmt_lat(dret, geopos)) == ERR) { p(serr.toString()); return ERR; }
            do_print_heliacal(dret, event_type, obj_name);

            /* list all events within one synodic cycle */
            if (search_flag != 0) continue;

            if (ipl == SE_VENUS || ipl == SE_MERCURY) {
                /* we have the heliacal rising (morning first), now the morning last */
                event_type = SE_MORNING_LAST;
                retflag = sw.swe_heliacal_ut(dret[0], geopos, datm, dobs, obj_name,
                        event_type, helflag, dret, serr);
                if (retflag == ERR) { p(serr.toString()); return ERR; }
                if ((retc = heliacal_to_lmt_lat(dret, geopos)) == ERR) { p(serr.toString()); return ERR; }
                do_print_heliacal(dret, event_type, obj_name);
                tsave1 = dret[0];

                /* Mercury can have several evening appearances without any morning
                 * appearance in between, so find the next morning one first and then
                 * every evening appearance before it */
                if (ipl == SE_MERCURY) {
                    event_type = SE_HELIACAL_RISING;
                    retflag = sw.swe_heliacal_ut(dret[0], geopos, datm, dobs, obj_name,
                            event_type, helflag, dret, serr);
                    if (retflag == ERR) { p(serr.toString()); return ERR; }
                    tsave2 = dret[0];
                }

                /* evening first */
                event_type = SE_EVENING_FIRST;
                retflag = sw.swe_heliacal_ut(tsave1, geopos, datm, dobs, obj_name,
                        event_type, helflag, dret, serr);
                if (retflag == ERR) { p(serr.toString()); return ERR; }
                if (ipl == SE_MERCURY && dret[0] > tsave2) continue;
                if ((retc = heliacal_to_lmt_lat(dret, geopos)) == ERR) { p(serr.toString()); return ERR; }
                do_print_heliacal(dret, event_type, obj_name);
            }

            if (ipl == SE_MOON) {
                /* morning last */
                event_type = SE_MORNING_LAST;
                retflag = sw.swe_heliacal_ut(dret[0], geopos, datm, dobs, obj_name,
                        event_type, helflag, dret, serr);
                if (retflag == ERR) { p(serr.toString()); return ERR; }
                if ((retc = heliacal_to_lmt_lat(dret, geopos)) == ERR) { p(serr.toString()); return ERR; }
                do_print_heliacal(dret, event_type, obj_name);
            } else {
                /* heliacal setting (evening last) */
                event_type = SE_HELIACAL_SETTING;
                retflag = sw.swe_heliacal_ut(dret[0], geopos, datm, dobs, obj_name,
                        event_type, helflag, dret, serr);
                if (retflag == ERR) { p(serr.toString()); return ERR; }
                if ((retc = heliacal_to_lmt_lat(dret, geopos)) == ERR) { p(serr.toString()); return ERR; }
                do_print_heliacal(dret, event_type, obj_name);
            }
        }
        return retc;
    }

    /** the three moments a heliacal event returns, converted together */
    static int heliacal_to_lmt_lat(double[] dret, double[] geopos) {
        if ((time_flag & (BIT_TIME_LMT | BIT_TIME_LAT)) == 0) return OK;
        final double[] tmp = new double[1];
        for (int i = 0; i < 3; i++) {
            final int retc = ut_to_lmt_lat(dret[i], geopos, tmp);
            dret[i] = tmp[0];
            if (retc == ERR) return ERR;
        }
        return OK;
    }

    static String remove_whitespace(String s) {
        return s.replace(" ", "");
    }

    /** the times swetest prints only when the corresponding contact is visible */
    static String visibleOrDash(int eclflag, int bit, double tjd) {
        return (eclflag & bit) != 0 ? hms_from_tjd(tjd) + " " : "   -         ";
    }

    /** ... and the ones it prints whenever the moment itself exists */
    static String setOrDash(double tjd) {
        return tjd != 0 ? hms_from_tjd(tjd) + " " : "   -         ";
    }

    /** swetest's {@code -clink}: the two astro.com chart links after an eclipse line */
    static void chart_link(String what, String styp, double[] geopos_max) {
        final String[] lonlat = format_lon_lat(geopos_max[0], geopos_max[1]);
        String stim = hms(jut, BIT_LZEROES);
        while (stim.startsWith(" ")) stim = stim.substring(1);
        if (stim.startsWith("0")) stim = stim.substring(1);
        final char cal = gregflag != 0 ? 'g' : 'j';
        final String snat = String.format(Locale.ROOT,
                "%s %s,%s,e,%d,%d,%d,%s,h0e,%cnu,%d,%s,,%s,%s,u,0,0,0",
                what, "saros", styp, jday, jmon, jyear, stim, cal, 0,
                what.startsWith("Lunar") ? "Moon Zenith location" : "Location of Maximum",
                lonlat[0], lonlat[1]);
        pf("<a href='/cgi/chart.cgi?muasp=1;nhor=1;act=chmnat;nd1=%s;rs=1;iseclipse=1'"
                + " target='eclipse'>chart link</a>\n\n", snat);
    }

    // ------------------------------------------------------------------- the special events

    static int do_special_event(double tjd, int ipl, String star, int special_event,
                                int special_mode, double[] geopos, double[] datm, double[] dobs) {
        int retc = 0;
        /* risings, settings, meridian transits */
        if (special_event == SP_RISE_SET || special_event == SP_MERIDIAN_TRANSIT)
            retc = call_rise_set(tjd, ipl, star, whicheph, geopos);
        /* lunar eclipses */
        if (special_event == SP_LUNAR_ECLIPSE)
            retc = call_lunar_eclipse(tjd, whicheph, special_mode, geopos);
        /* solar eclipses */
        if (special_event == SP_SOLAR_ECLIPSE)
            retc = call_solar_eclipse(tjd, whicheph, special_mode, geopos);
        /* occultations by the moon */
        if (special_event == SP_OCCULTATION)
            retc = call_lunar_occultation(tjd, ipl, star, whicheph, special_mode, geopos);
        /* heliacal event */
        if (special_event == SP_HELIACAL)
            retc = call_heliacal_event(tjd, ipl, star, whicheph, geopos, datm, dobs);
        return retc;
    }

    static int ut_to_lmt_lat(double t_ut, double[] geopos, double[] t_ret) {
        int iflgret = OK;
        if ((time_flag & (BIT_TIME_LMT | BIT_TIME_LAT)) != 0) {
            t_ut += geopos[0] / 360.0;
            if ((time_flag & BIT_TIME_LAT) != 0) {
                final double[] tl = new double[1];
                iflgret = sw.swe_lmt_to_lat(t_ut, geopos[0], tl, serr);
                t_ut = tl[0];
            }
        }
        t_ret[0] = t_ut;
        return iflgret;
    }

    /** the C replaces the tabs it built the line with by the -g string, if one was given */
    static String insert_gap_string_for_tabs(String s) {
        if (!have_gap_parameter || "\t".equals(gap)) return s;
        return s.replace("\t", gap);
    }

    static int print_rise_set_line(double trise, double tset, double[] geopos) {
        int retc = OK;
        final double[] tmp = new double[1];
        if (trise != 0) { retc = ut_to_lmt_lat(trise, geopos, tmp); trise = tmp[0]; }
        if (tset != 0) { retc = ut_to_lmt_lat(tset, geopos, tmp); tset = tmp[0]; }

        final int[] jd = new int[3];
        final double[] jt = new double[1];
        final StringBuilder s = new StringBuilder("rise     ");
        if (have_gap_parameter) s.append('\t');
        if (trise == 0) {
            s.append("         -\t           -    ");
        } else {
            sw.swe_revjul(trise, gregflag, jd, jt);
            jyear = jd[0]; jmon = jd[1]; jday = jd[2]; jut = jt[0];
            s.append(String.format(Locale.ROOT, "%2d.%02d.%04d\t%s    ",
                    jday, jmon, jyear, hms(jut, BIT_LZEROES)));
        }
        if (have_gap_parameter) s.append('\t');
        s.append("set      ");
        if (have_gap_parameter) s.append('\t');
        if (tset == 0) {
            s.append("         -\t           -    ");
        } else {
            sw.swe_revjul(tset, gregflag, jd, jt);
            jyear = jd[0]; jmon = jd[1]; jday = jd[2]; jut = jt[0];
            s.append(String.format(Locale.ROOT, "%2d.%02d.%04d\t%s    ",
                    jday, jmon, jyear, hms(jut, BIT_LZEROES)));
        }
        if (trise != 0 && tset != 0) {
            if (have_gap_parameter) s.append('\t');
            s.append("dt =");
            if (have_gap_parameter) s.append('\t');
            s.append(hms((tset - trise) * 24, BIT_LZEROES));
        }
        s.append('\n');
        p(insert_gap_string_for_tabs(s.toString()));
        return retc;
    }

    static int call_rise_set(double t_ut, int ipl, String star, int whicheph, double[] geopos) {
        int rval, loop_count;
        int rsmi;
        double dayfrac = 0.0001;
        final double[] tretl = new double[10];
        final double[] trisev = new double[1], tsetv = new double[1];
        double trise, tset, tnext, tret1sv = 0;
        boolean do_rise, do_set;
        boolean last_was_empty = false;
        int retc = OK;
        int rsmior = 0;

        if (norefrac != 0) rsmior |= SE_BIT_NO_REFRACTION;
        if (disccenter != 0) rsmior |= SE_BIT_DISC_CENTER;
        if (discbottom != 0) rsmior |= SE_BIT_DISC_BOTTOM;
        if (hindu != 0) rsmior |= SE_BIT_HINDU_RISING;
        if (Math.abs(geopos[1]) < 60 && ipl >= SE_SUN && ipl <= SE_PLUTO) dayfrac = 0.01;

        sw.swe_set_topo(geopos[0], geopos[1], geopos[2]);
        if (with_header) pf("\ngeo. long %f, lat %f, alt %f", geopos[0], geopos[1], geopos[2]);
        p("\n");

        final StringBuilder sn = new StringBuilder(null == star ? "" : star);
        tnext = t_ut;
        // designed for looping with -nxxx over many days, during which the object might
        // become circumpolar, or never rise at all
        while (special_event == SP_RISE_SET && tnext < t_ut + nstep) {
            // avoids unnecessary calculation for circumpolar objects; without it the output
            // would still be correct, only slower
            if (last_was_empty && (null == star || star.isEmpty())) {
                rval = sw.swe_calc_ut(tnext, ipl, whicheph | SEFLG_EQUATORIAL, tretl, serr);
                if (rval >= 0) {
                    final double edist = geopos[1] + tretl[1];
                    final double edist2 = geopos[1] - tretl[1];
                    if ((edist - 2 > 90 || edist + 2 < -90)
                            || (edist2 - 2 > 90 || edist2 + 2 < -90)) {
                        tnext += 1;
                        continue;
                    }
                }
            }

            /* rising */
            rsmi = SE_CALC_RISE | rsmior;
            rval = sw.swe_rise_trans(tnext, ipl, sn, whicheph, rsmi, geopos,
                    datm[0], datm[1], trisev, serr);
            if (rval == ERR) { p(serr.toString()); return ERR; }
            trise = trisev[0];
            do_rise = (rval == OK);

            /* setting */
            rsmi = SE_CALC_SET | rsmior;
            do_set = false;
            loop_count = 0;
            tset = 0;
            while (!do_set && loop_count < 2) {
                rval = sw.swe_rise_trans(tnext, ipl, sn, whicheph, rsmi, geopos,
                        datm[0], datm[1], tsetv, serr);
                if (rval == ERR) { p(serr.toString()); return ERR; }
                tset = tsetv[0];
                do_set = (rval == OK);
                if (!do_set && do_rise) tnext = trise;
                loop_count++;
            }

            if (do_rise && do_set && trise > tset) {
                do_rise = false;    // ignore rises happening before setting
                trise = 0;          // exact time 0 is highly unlikely
            }
            if (do_rise && do_set) {
                rval = print_rise_set_line(trise, tset, geopos);
                last_was_empty = false;
                tnext = tset + dayfrac;
            } else if (do_rise) {
                rval = print_rise_set_line(trise, 0, geopos);
                last_was_empty = false;
                tnext = trise + dayfrac;
            } else if (do_set) {
                tnext = tset + dayfrac;
                rval = print_rise_set_line(0, tset, geopos);
                last_was_empty = false;
            } else {    // neither rise nor set: print the '-  -' line only once for a run
                rval = last_was_empty ? OK : print_rise_set_line(0, 0, geopos);
                tnext += 1;
                last_was_empty = true;
            }
            if (rval == ERR) { p(serr.toString()); return ERR; }
            if (nstep == 1) break;
        }

        /* swetest -metr: transits over midheaven and lower midheaven */
        if (special_event == SP_MERIDIAN_TRANSIT) {
            final int[] jd = new int[3];
            final double[] jt = new double[1];
            final double[] t0 = new double[1], t1 = new double[1];
            for (int ii = 0; ii < nstep; ii++, t_ut = tret1sv + 0.001) {
                if (sw.swe_rise_trans(t_ut, ipl, sn, whicheph, SE_CALC_MTRANSIT, geopos,
                        datm[0], datm[1], t0, serr) != OK) {
                    p(serr.toString());
                    return ERR;
                }
                if (sw.swe_rise_trans(t_ut, ipl, sn, whicheph, SE_CALC_ITRANSIT, geopos,
                        datm[0], datm[1], t1, serr) != OK) {
                    p(serr.toString());
                    return ERR;
                }
                tret1sv = t1[0];
                if ((time_flag & (BIT_TIME_LMT | BIT_TIME_LAT)) != 0) {
                    retc = ut_to_lmt_lat(t0[0], geopos, t0);
                    retc = ut_to_lmt_lat(t1[0], geopos, t1);
                }
                final StringBuilder s = new StringBuilder("mtransit ");
                if (have_gap_parameter) s.append('\t');
                if (t0[0] == 0 || t0[0] > t1[0]) {
                    s.append("         -\t           -    ");
                } else {
                    sw.swe_revjul(t0[0], gregflag, jd, jt);
                    s.append(String.format(Locale.ROOT, "%2d.%02d.%04d\t%s    ",
                            jd[2], jd[1], jd[0], hms(jt[0], BIT_LZEROES)));
                }
                if (have_gap_parameter) s.append('\t');
                s.append("itransit ");
                if (have_gap_parameter) s.append('\t');
                if (t1[0] == 0) {
                    s.append("         -\t           -    \n");
                } else {
                    sw.swe_revjul(t1[0], gregflag, jd, jt);
                    s.append(String.format(Locale.ROOT, "%2d.%02d.%04d\t%s\n",
                            jd[2], jd[1], jd[0], hms(jt[0], BIT_LZEROES)));
                }
                p(insert_gap_string_for_tabs(s.toString()));
            }
        }
        return retc;
    }

    static String get_gregjul(int gregflag, int year) {
        if (gregflag == SE_JUL_CAL) return "jul";
        if (year < 1700) return "greg";
        return "";
    }

    /** longitude and latitude in swetest's own compact form, e.g. {@code 8e3000}/{@code 47n0000} */
    static String[] format_lon_lat(double lon, double lat) {
        final int[] dms = new int[3];
        final double[] dsecfr = new double[1];
        final int[] isgn = new int[1];

        sw.swe_split_deg(lon, SE_SPLIT_DEG_ROUND_SEC, dms, dsecfr, isgn);
        final String slon = String.format(Locale.ROOT, "%d%c%02d%02d",
                Math.abs(dms[0]), lon < 0 ? 'w' : 'e', dms[1], dms[2]);

        sw.swe_split_deg(lat, SE_SPLIT_DEG_ROUND_SEC, dms, dsecfr, isgn);
        final String slat = String.format(Locale.ROOT, "%d%c%02d%02d",
                Math.abs(dms[0]), lat < 0 ? 's' : 'n', dms[1], dms[2]);

        return new String[]{slon, slat};
    }

    static int orbital_elements(double tjd_et, int ipl, int iflag) {
        final double[] dret = new double[20];
        final int[] jd = new int[3];
        final double[] jt = new double[1];

        if (sw.swe_get_orbital_elements(tjd_et, ipl, iflag, dret, serr) == ERR) {
            pf("%s\n", serr);
            return ERR;
        }
        sw.swe_revjul(dret[14], gregflag, jd, jt);
        final String sdateperi = String.format(Locale.ROOT, "%2d.%02d.%04d,%s",
                jd[2], jd[1], jd[0], hms(jt[0], BIT_LZEROES));

        pf("semiaxis         \t%f\neccentricity     \t%f\ninclination      \t%f\n"
                        + "asc. node       \t%f\narg. pericenter  \t%f\npericenter       \t%f\n",
                dret[0], dret[1], dret[2], dret[3], dret[4], dret[5]);
        pf("mean longitude   \t%f\nmean anomaly     \t%f\necc. anomaly     \t%f\n"
                        + "true anomaly     \t%f\n",
                dret[9], dret[6], dret[8], dret[7]);
        pf("time pericenter  \t%f %s\ndist. pericenter \t%f\ndist. apocenter  \t%f\n",
                dret[14], sdateperi, dret[15], dret[16]);
        pf("mean daily motion\t%f\nsid. period (y)  \t%f\ntrop. period (y) \t%f\n"
                        + "synodic cycle (d)\t%f\n",
                dret[11], dret[10], dret[12], dret[13]);
        return OK;
    }

    /**
     * swetest's {@code -astpos}: the named asteroids sitting within an orb of a longitude.
     * <p>
     * The C reads {@code seasnam.txt} itself through a helper its own comment says "should
     * move to swephlib.c in next release" - it is not a Swiss Ephemeris export, so there is
     * nothing in {@code SwephExp} to call. This reads the same file the same way: a named
     * asteroid is a line whose ninth character is not a digit.
     */
    static int print_asteroids(double tjd, double dref, double orb) {
        final java.util.List<Integer> arr = new java.util.ArrayList<Integer>();
        final java.io.File file = new java.io.File(ephePath, "seasnam.txt");
        if (!file.isFile()) {
            pf(" error in swe_get_named_ast_list(): %s not found\n", file.getPath());
            return ERR;
        }
        try {
            final java.io.BufferedReader in = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new java.io.FileInputStream(file), "ISO-8859-1"));
            try {
                for (String si = in.readLine(); null != si; si = in.readLine()) {
                    if (si.length() > 8 && !Character.isDigit(si.charAt(8))) arr.add(atoi(si));
                }
            } finally {
                in.close();
            }
        } catch (java.io.IOException e) {
            pf(" error in swe_get_named_ast_list(): %s\n", e.getMessage());
            return ERR;
        }

        pf("\nAsteroids near %.6f%s (%s) within orb %.3f%s\n\t(out of %d named asteroids)\n\n",
                dref, ODEGREE_STRING, dms(dref, BIT_ZODIAC | BIT_ROUND_SEC), orb,
                ODEGREE_STRING, arr.size());

        final double[] xx = new double[6];
        for (Integer no : arr) {
            final int ipl = no + SE_AST_OFFSET;
            final int rc = sw.swe_calc(tjd, ipl, 0, xx, serr);
            if (rc >= 0 && dref >= 0) {
                double d = sw.swe_difdeg2n(dref, xx[0]);
                if (Math.abs(d) <= orb) {
                    char m = ' ';
                    if (d < 0) { d = -d; m = '-'; }
                    // orb in front, for easy sorting
                    pf("%.3f%c\t%d\t%-20s\n", d, m, no, sw.swe_get_planet_name(ipl));
                }
            }
        }
        return OK;
    }

    static boolean strpbrk(String s, String chars) {
        for (int i = 0; i < s.length(); i++) {
            if (chars.indexOf(s.charAt(i)) >= 0) return true;
        }
        return false;
    }

    /** C's {@code atoi}: a leading integer, or 0 - never an exception */
    static int atoi(String s) {
        return (int) atof(s);
    }

    /** C's {@code atof}: as much of a leading number as parses, or 0 - never an exception */
    /**
     * How many leading comma-separated fields of {@code f} would satisfy C's
     * {@code sscanf(sp, "%lf,%lf,...")}.
     *
     * <p>Not decoration. {@code sscanf} stops at the first conversion that fails, reports how
     * many succeeded, and leaves every later destination <b>untouched</b> - so {@code -astpos}
     * with no number leaves it at its -1 sentinel and the option does nothing. Java's
     * {@code "".split(",")} answers {@code {""}}, one field, and {@code atof("")} is 0.0, which
     * would assign a real 0 where the C assigned nothing: {@code -astpos} then listed asteroids
     * around longitude 0 instead of standing down.
     */
    static int scanned(final String[] f) {
        int n = 0;
        while (n < f.length && scans(f[n])) n++;
        return n;
    }

    /** whether {@code %lf} would consume anything at all here */
    static boolean scans(final String field) {
        final String s = field.trim();
        if (s.isEmpty()) return false;
        final char c = s.charAt(0);
        return '+' == c || '-' == c || '.' == c || (c >= '0' && c <= '9');
    }

    static double atof(String s) {
        if (null == s) return 0;
        int i = 0, n = s.length();
        while (i < n && Character.isWhitespace(s.charAt(i))) i++;
        int start = i;
        if (i < n && ('+' == s.charAt(i) || '-' == s.charAt(i))) i++;
        while (i < n && (Character.isDigit(s.charAt(i)) || '.' == s.charAt(i))) i++;
        if (i < n && ('e' == s.charAt(i) || 'E' == s.charAt(i))) {
            final int save = i++;
            if (i < n && ('+' == s.charAt(i) || '-' == s.charAt(i))) i++;
            if (i < n && Character.isDigit(s.charAt(i))) {
                while (i < n && Character.isDigit(s.charAt(i))) i++;
            } else {
                i = save;
            }
        }
        try {
            return Double.parseDouble(s.substring(start, i));
        } catch (RuntimeException notANumber) {
            return 0;
        }
    }

    /** swetest allows an optional '[' before a coordinate list */
    static String strip(String s) {
        return s.startsWith("[") ? s.substring(1) : s;
    }

    /** {@code sscanf(sp, "%d%*c%d%*c%d", &jday, &jmon, &jyear)} */
    static int[] scanDate(String s) {
        final int[] v = new int[3];
        int i = 0, found = 0;
        while (i < s.length() && found < 3) {
            while (i < s.length() && !Character.isDigit(s.charAt(i)) && '-' != s.charAt(i)) i++;
            final int start = i;
            if (i < s.length() && '-' == s.charAt(i)) i++;
            while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            if (i == start) break;
            v[found++] = atoi(s.substring(start, i));
            if (i < s.length()) i++;    // the %*c separator
        }
        if (found < 1) {
            pf("illegal date %s\n", s);
            return null;
        }
        return v;
    }
}
