package swisseph;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.swisseph.ISwissEph;
import org.swisseph.SwephNative;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Not a correctness suite - {@link SwissEphEngineComparisonTest} already owns that - this one
 * answers a single question for as much of {@link SweTest} (the {@code swetest.c} port) as
 * practical to exercise: <b>which engine is faster, and by how much</b>, running the same command
 * line through {@link org.swisseph.SwephNative} and {@code swisseph.SwissEph} on the same thread,
 * one after the other. Every case prints its own line as it runs and the class prints a grand
 * summary at the end - see {@link #printGrandSummary()}.
 *
 * <p><b>Methodology.</b> Each command line is run {@link #REPEATS} times per engine (fewer for
 * the deliberately heavy cases - transit searches and the two large {@code -n} sweeps from
 * swetest.c's own Examples) and the <i>floor</i> (fastest) time is reported, not the mean - the
 * same discipline {@code swe-jni-lib}'s own {@code opt-experiment.sh}/{@code bench-dll.sh} use for
 * the DLL benchmarks, for the same reason: a GC pause or a scheduler hiccup can only make a run
 * slower than the engine's real cost, never faster, so the minimum is the honest number and the
 * mean is polluted by whatever happened to interrupt one run. {@link #setUp()} runs a handful of
 * warm-up calls against both engines first, so the first real case is not paying a one-time
 * class-loading/JIT-compilation tax that has nothing to do with either engine's steady-state
 * speed - the pure-Java engine in particular has much more bytecode for the JIT to warm up than
 * the JNI bridge does. All of this still runs on a cold-ish JVM by production standards; treat the
 * printed numbers as a comparison between the two engines on this run, not an absolute benchmark.
 *
 * <p>Every test method here is a category, run in the order the author asked for: transits and
 * special events first (the search-driven code path, where the two engines' cost per event is
 * least alike), then all seventeen Jagannatha Hora reference charts under
 * {@code ai-github-projects/test-data/} (skipped if that directory is not present - it lives
 * outside any Maven module), then swetest.c's own {@code -hexamp} Examples verbatim, then a
 * broader sweep of the remaining option surface for coverage.
 *
 * <p><b>Every case's full text output from both engines is written under
 * {@code src/test/resources/engine-comparison/}</b> - {@code <category>/<NN-label>.native.txt}
 * and {@code .pure-java.txt} - so a human can read the two side by side (or point a diff tool at
 * the pair) for exactly the cases where the "same output"/"differs" column says they disagree.
 *
 * <p>The two files are treated differently, and deliberately so - measured, not assumed. Only
 * {@code .pure-java.txt} is regression-checked: {@code swisseph.SwissEph} keeps its whole state
 * privately per Java object with nothing shared across the JVM, and running these 96 cases both
 * alone and as part of the full suite produced byte-identical pure-Java output every time. The
 * native library did not: {@code .native.txt} is always freshly overwritten, never compared,
 * because its own internal interpolation caches (nutation, obliquity) are not fully reset by
 * anything reachable from this class - not even {@code swe_close()} plus explicitly pinning the
 * tidal acceleration, both tried and measured to leave the same ~1e-4 arcsecond drift depending
 * on what other test classes happened to run first in the same JVM. Real, but two orders of
 * magnitude below anything a human comparing the two engines' output would ever look at, and not
 * a signal a byte-exact check could use honestly - so native's file is for reading, not gating.
 *
 * <p>For {@code .pure-java.txt}: the first run for a given case simply writes the file; once it
 * exists and is committed, every later run instead <i>compares</i> against it and records a
 * regression (collected by {@link #checkNoRegressions()}, which fails once at the very end with
 * the full list, rather than each {@code bench()} call failing immediately and cutting its test
 * method's remaining cases short) if the freshly computed output no longer matches what is
 * committed - the actual output goes to the OS temp directory under the same relative path so the
 * change is a diff and a copy, never a silent overwrite of the committed reference. The same
 * "golden masters, not self-confirming fixtures" discipline {@code swe-jyotisa-lib}'s own
 * {@code GocharaReference}/{@code SequenceReference} use. Pass {@code -Dswe.perf.regen=true} to
 * accept the current run's output as the new committed baseline instead of comparing against it -
 * the same escape hatch those classes' own {@code -Drefbase.generate=true} is. {@code summary.txt}
 * is different again: it
 * carries the timings too, which are never going to repeat exactly run to run, so it is always
 * freshly overwritten and never compared - it exists purely so the numbers are still on disk
 * after a run without having to re-read the console log.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SwissEphEnginePerformanceTest {

    static final File EPHE = new File("ephe").getAbsoluteFile();

    /** where the seventeen {@code ref<year>.jhd} Jagannatha Hora charts live - outside this
     *  module, so {@link #jhdReferenceCharts()} self-skips if it is not there. */
    static final File JHD_DIR = new File("E:/Softworks/Holidates/ai-github-projects/test-data");

    static ISwissEph nativeEph;
    static ISwissEph pureJavaEph;

    /** default repeat count; the floor of these is what gets reported. */
    static final int REPEATS = 5;
    /** repeat count for the deliberately heavy cases - transit searches and the two large
     *  {@code -n} sweeps - so the whole class still runs in a reasonable time. */
    static final int REPEATS_HEAVY = 2;

    static final List<String[]> ROWS = new ArrayList<>();

    /** where the per-case {@code .native.txt}/{@code .pure-java.txt} pairs and
     *  {@code summary.txt} are written - committed for regression, per the author's request. */
    static final File REF_DIR = new File("src/test/resources/engine-comparison");

    /** {@code -Dswe.perf.regen=true} accepts the current run as the new committed baseline
     *  instead of comparing against it - see the class javadoc. */
    static final boolean REGEN = Boolean.getBoolean("swe.perf.regen");

    static final Map<String, Integer> CASE_INDEX = new HashMap<>();
    static final List<String> REGRESSIONS = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        nativeEph = new SwephNative(EPHE.getPath());
        pureJavaEph = new swisseph.SwissEph(EPHE.getPath());
        for (int i = 0; i < 3; i++) {
            run(nativeEph, "-b1.1.2000 -p0123456789mt -house8,47,P -fPLBRS");
            run(pureJavaEph, "-b1.1.2000 -p0123456789mt -house8,47,P -fPLBRS");
        }
    }

    static String run(final ISwissEph sw, final String commandLine) {
        return SweTest.swe_test(sw, (commandLine + " -edir" + EPHE).split(" "));
    }

    static String slug(final String label) {
        String s = label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+|-+$", "");
        return s.length() > 60 ? s.substring(0, 60) : s;
    }

    static void writeText(final File f, final String content) {
        try {
            f.getParentFile().mkdirs();
            Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String readText(final File f) {
        try {
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes {@code content} under {@code REF_DIR/relPath} the first time (or always, with
     * {@code -Dswe.perf.regen=true}); on any later run compares against what is already there and
     * appends to {@link #REGRESSIONS} rather than failing immediately, so the case list keeps
     * running to completion and every regression is visible at once. A mismatch's actual output is
     * also written to the OS temp directory under the same relative path for a ready-made diff.
     */
    static void writeOrCompare(final String relPath, final String content) {
        final File committed = new File(REF_DIR, relPath);
        if (REGEN || !committed.isFile()) {
            writeText(committed, content);
            return;
        }
        final String existing = readText(committed);
        if (!existing.equals(content)) {
            final File actual = new File(new File(
                    System.getProperty("java.io.tmpdir"), "swe-jni-demo-engine-comparison"), relPath);
            writeText(actual, content);
            REGRESSIONS.add(relPath + " - actual output written to " + actual.getAbsolutePath());
        }
    }

    /**
     * Runs {@code commandLine} {@code repeats} times against each engine, in turn (native's whole
     * run of repeats, then pure-Java's), records the floor of each and whether the two engines'
     * last output agreed byte for byte, prints one line immediately, and keeps the row for the
     * final grand summary. A pure-Java {@link NotImplementedException} (a handful of methods -
     * {@code swe_get_orbital_elements}, {@code swe_calc_pctr} - are not ported at all, see
     * {@code swe-java-lib}'s {@code ApiCoverageTest.NOT_PORTED}) stops that engine's loop on the
     * first attempt rather than retrying a deterministic failure {@code repeats} times, and is
     * reported as "not ported" rather than a timing.
     */
    static void bench(String category, String label, String commandLine, int repeats) {
        long nativeFloor = Long.MAX_VALUE;
        String nativeOut = null;
        for (int i = 0; i < repeats; i++) {
            final long t0 = System.nanoTime();
            nativeOut = run(nativeEph, commandLine);
            nativeFloor = Math.min(nativeFloor, System.nanoTime() - t0);
        }

        long pureFloor = Long.MAX_VALUE;
        String pureOut = null;
        boolean notPorted = false;
        for (int i = 0; i < repeats && !notPorted; i++) {
            try {
                final long t0 = System.nanoTime();
                pureOut = run(pureJavaEph, commandLine);
                pureFloor = Math.min(pureFloor, System.nanoTime() - t0);
            } catch (NotImplementedException ex) {
                notPorted = true;
                pureOut = "NotImplementedException: " + ex.getMessage() + "\n";
            }
        }

        final double nMs = nativeFloor / 1_000_000.0;
        final double pMs = notPorted ? Double.NaN : pureFloor / 1_000_000.0;
        final String verdict = notPorted ? "not ported"
                : Objects.equals(nativeOut, pureOut) ? "same output" : "differs";
        final String ratioStr = notPorted ? "-" : String.format(Locale.ROOT, "%5.2fx", pMs / nMs);
        System.out.println(String.format(Locale.ROOT,
                "  %-42s  native %9.3f ms  pure-java %9s ms  ratio %7s  %s",
                label, nMs, notPorted ? "n/a" : String.format(Locale.ROOT, "%.3f", pMs),
                ratioStr, verdict));
        ROWS.add(new String[]{category, label, commandLine,
                String.valueOf(nMs), notPorted ? "" : String.valueOf(pMs), verdict});

        final int index = CASE_INDEX.merge(category, 1, Integer::sum);
        final String relBase = category + "/" + String.format(Locale.ROOT, "%02d", index) + "-" + slug(label);
        // native.txt is always freshly overwritten, never regression-checked - see the class
        // javadoc for why: the native library's own internal interpolation caches (nutation,
        // obliquity) are not fully reset by anything this class can call, so its printed output
        // can differ in its last one or two digits depending on what ran earlier in the same JVM,
        // measured up to ~1e-4 arcsecond - real, but irrelevant to a human comparing it against
        // pure-java's output, and not a signal a byte-exact regression check could use honestly.
        writeText(new File(REF_DIR, relBase + ".native.txt"), null == nativeOut ? "" : nativeOut);
        // pure-java.txt IS regression-checked: swisseph.SwissEph holds its own state privately
        // per instance with nothing shared across the JVM, so it was measured fully reproducible
        // regardless of what else ran first - the same 96 cases, run alone versus as part of the
        // full suite, never once disagreed with themselves here.
        writeOrCompare(relBase + ".pure-java.txt", null == pureOut ? "" : pureOut);
    }

    static void bench(String category, String label, String commandLine) {
        bench(category, label, commandLine, REPEATS);
    }

    // ------------------------------------------------------------------ transits & special events

    /**
     * Rise/set, meridian transit, both eclipse kinds, occultations and heliacal events - every
     * command line {@code SwissEphEngineComparisonTest}/{@code diff-vs-swetest.sh} already proved
     * runs cleanly on both engines, with the event counts raised where that is cheap (rise,
     * meridian transit: {@code -n2}/{@code -n3} to {@code -n10}) so the timing floor is over
     * several events rather than one. This is the category the author asked about specifically:
     * every one of these is a numeric search ({@code TransitCalculator} on the pure-Java side,
     * swetest's own iterative solvers natively), not a closed-form calculation, so it is where the
     * two engines' relative cost is least like the plain-position case.
     */
    @Test
    @Order(1)
    void transitsAndSpecialEvents() {
        System.out.println();
        System.out.println("=== transits & special events (single-threaded, floor of N runs) ===");
        bench("transit", "rise/set, 10 events", "-b1.1.2000 -p0 -rise -geopos8,47,0 -n10", REPEATS_HEAVY);
        bench("transit", "rise/set, disc center, 10 events", "-b1.1.2000 -p0 -rise -disccenter -geopos8,47,0 -n10", REPEATS_HEAVY);
        bench("transit", "rise/set, disc bottom, 10 events", "-b1.1.2000 -p0 -rise -discbottom -geopos8,47,0 -n10", REPEATS_HEAVY);
        bench("transit", "rise/set, no refraction, 10 events", "-b1.1.2000 -p0 -rise -norefrac -geopos8,47,0 -n10", REPEATS_HEAVY);
        bench("transit", "rise/set, hindu rising, 10 events", "-b1.1.2000 -p0 -rise -hindu -geopos8,47,0 -n10", REPEATS_HEAVY);
        bench("transit", "meridian transit, 10 events", "-b1.1.2000 -p0 -metr -geopos8,47,0 -n10", REPEATS_HEAVY);
        bench("transit", "lunar eclipse, 5 events", "-b1.1.2000 -lunecl -n5", REPEATS_HEAVY);
        bench("transit", "lunar eclipse, penumbral, 5 events", "-b1.1.2000 -lunecl -penumbral -n5", REPEATS_HEAVY);
        bench("transit", "lunar eclipse, annular-total, 5 events", "-b1.1.2000 -lunecl -anntot -n5", REPEATS_HEAVY);
        bench("transit", "lunar eclipse, local circumstances", "-b1.1.2000 -lunecl -how -geopos8,47,0", REPEATS_HEAVY);
        bench("transit", "solar eclipse, 5 events", "-b1.1.2000 -solecl -n5", REPEATS_HEAVY);
        bench("transit", "solar eclipse, total, 5 events", "-b1.1.2000 -solecl -total -n5", REPEATS_HEAVY);
        bench("transit", "solar eclipse, annular, 5 events", "-b1.1.2000 -solecl -annular -n5", REPEATS_HEAVY);
        bench("transit", "solar eclipse, partial, 5 events", "-b1.1.2000 -solecl -partial -n5", REPEATS_HEAVY);
        bench("transit", "solar eclipse, central, 5 events", "-b1.1.2000 -solecl -central -n5", REPEATS_HEAVY);
        bench("transit", "solar eclipse, noncentral, 5 events", "-b1.1.2000 -solecl -noncentral -n5", REPEATS_HEAVY);
        bench("transit", "solar eclipse, local, 5 events", "-b1.1.2000 -solecl -local -geopos8,47,0 -n5", REPEATS_HEAVY);
        bench("transit", "lunar occultation of a planet, 5 events", "-b1.1.2000 -p2 -occult -n5", REPEATS_HEAVY);
        bench("transit", "lunar occultation of a star, 2 events", "-b1.1.2000 -pf -xfSpica -occult -n2", REPEATS_HEAVY);
        bench("transit", "heliacal event 1 (first visibility)", "-b1.1.2000 -p1 -hev1 -geopos8,47,0", REPEATS_HEAVY);
        bench("transit", "heliacal event 2 (last visibility)", "-b1.1.2000 -p1 -hev2 -geopos8,47,0", REPEATS_HEAVY);
        bench("transit", "heliacal event 3 (evening first)", "-b1.1.2000 -p1 -hev3 -geopos8,47,0", REPEATS_HEAVY);
        bench("transit", "heliacal event 4 (acronychal)", "-b1.1.2000 -p1 -hev4 -geopos8,47,0", REPEATS_HEAVY);
        bench("transit", "heliacal event, custom flag", "-b1.1.2000 -p1 -helflag256 -hev1 -geopos8,47,0", REPEATS_HEAVY);
    }

    // ------------------------------------------------------------------ JHD reference charts

    /**
     * The seventeen Jagannatha Hora reference charts under {@code ai-github-projects/test-data/}
     * - the same fixtures {@code swe-jyotisa-lib}'s own {@code refcharts} tests use, one file per
     * epoch (year 0 to 2100), all the same place and local time (Machilipatnam, 4 April 17:50:40,
     * TZ +5:30) so only the year varies. Each is turned into one command line computing the ten
     * planets plus both lunar nodes, Placidus houses and the Lahiri ayanamsa - a representative
     * "compute a whole chart" case, as opposed to the single-value probes elsewhere in this class.
     */
    @Test
    @Order(2)
    void jhdReferenceCharts() throws IOException {
        Assumptions.assumeTrue(JHD_DIR.isDirectory(),
                () -> "no JHD test data at " + JHD_DIR + " - skipping");
        final File[] files = JHD_DIR.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".jhd"));
        Assumptions.assumeTrue(null != files && files.length > 0,
                () -> "no .jhd files found under " + JHD_DIR);
        Arrays.sort(files);

        System.out.println();
        System.out.println("=== JHD reference charts, " + files.length
                + " epochs (all planets + nodes + Placidus houses + Lahiri) ===");
        for (final File f : files) {
            final Jhd jhd = Jhd.read(f);
            bench("jhd", f.getName() + " (" + jhd.year + ")", jhd.commandLine());
        }
    }

    /** Minimal reader for the Jagannatha Hora {@code .jhd} format - see
     *  {@code org.jyotisa.refcharts.JhdChart} in {@code swe-jyotisa-lib} for the fully documented
     *  original this is adapted from (that one lives in a test source tree in a different Maven
     *  module and is not on this project's classpath). One value per line: month, day, year,
     *  local time and time zone and longitude and latitude all as <b>degrees.minutes</b>
     *  (16.10 means 16&deg;10'), longitude and time zone with the <b>sign reversed</b>. */
    static final class Jhd {
        final int month, day, year;
        final double localTime, timeZone, longitude, latitude;

        private Jhd(List<String> lines) {
            this.month = (int) Double.parseDouble(lines.get(0));
            this.day = (int) Double.parseDouble(lines.get(1));
            this.year = (int) Double.parseDouble(lines.get(2));
            this.localTime = degreesMinutes(Double.parseDouble(lines.get(3)));
            this.timeZone = degreesMinutes(-Double.parseDouble(lines.get(4)));
            this.longitude = degreesMinutes(-Double.parseDouble(lines.get(5)));
            this.latitude = degreesMinutes(Double.parseDouble(lines.get(6)));
        }

        static double degreesMinutes(double value) {
            final double sign = Math.signum(value);
            final double abs = Math.abs(value);
            final double deg = Math.floor(abs);
            final double min = (abs - deg) * 100.;
            return sign * (deg + min / 60.);
        }

        static Jhd read(File f) throws IOException {
            final List<String> lines = new ArrayList<>();
            for (String l : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) lines.add(l.trim());
            return new Jhd(lines);
        }

        /** the {@code -ut<hh:mm:ss>} swetest wants, from local time minus time zone. Every one of
         *  these seventeen fixtures shares the same local time and time zone (only the year
         *  differs), so this never has to carry a day rollover across the {@code -b} date. */
        String utcTime() {
            final double ut = localTime - timeZone;
            final int h = (int) ut;
            final int m = (int) ((ut - h) * 60);
            final int s = (int) Math.round((((ut - h) * 60) - m) * 60);
            return String.format(Locale.ROOT, "%02d:%02d:%02d", h, m, s);
        }

        String commandLine() {
            return String.format(Locale.ROOT,
                    "-b%d.%d.%d -ut%s -p0123456789mt -house%.6f,%.6f,P -sid1 -fPl",
                    day, month, year, utcTime(), longitude, latitude);
        }
    }

    // ------------------------------------------------------------------ swetest.c's own Examples

    /**
     * Verbatim from swetest.c's {@code -hexamp} help text (the seven printed examples, plus the
     * two inline ones under {@code -d}/{@code -dh}), unchanged from what the reference program's
     * own documentation demonstrates - see {@code infoexamp}/the {@code -dX}/{@code -dhX} option
     * text in swetest.c. The last one ({@code -n36600}, 500 years of 5-day steps) is the heaviest
     * single case in this class by design - it is the reference program's own example of a
     * deliberately long run.
     */
    @Test
    @Order(3)
    void swetestOwnExamples() {
        System.out.println();
        System.out.println("=== swetest.c's own -hexamp Examples ===");
        bench("example", "Mercury, 15 positions, 2-day steps", "-p2 -b1.12.1900 -n15 -s2");
        bench("example", "...same, comma-sep, rounded, no header", "-p2 -b1.12.1900 -n15 -s2 -fTZ -roundsec -g, -head");
        bench("example", "asteroid 433 Eros", "-ps -xs433 -b1.12.1900");
        bench("example", "fixed star Aldebaran", "-pf -xfAldebaran -b1.1.2000");
        bench("example", "Moon-Sun angular distance, 10 days", "-p1 -d0 -b1.12.1900 -n10 -fPTl -head");
        bench("example", "Saturn-Chiron midpoints, 100x5d steps", "-p6 -DD -b1.12.1900 -n100 -s5 -fPTZ -head -roundmin", REPEATS_HEAVY);
        bench("example", "Koch houses for a German location", "-b5.1.2002 -p -house12.05,49.50,K -ut12:30");
        bench("example", "tabular ephemeris, 366 days, one row/day", "-b1.1.2016 -g -fTlbR -p0123456789Dmte -hor -n366 -roundsec", REPEATS_HEAVY);
        bench("example", "Sun-Mercury differential, 366 daily steps", "-p2 -d0 -fJl -n366 -b1.1.1992", REPEATS_HEAVY);
        bench("example", "geo vs helio Neptune, 500y in 5y steps (n=36600)", "-p8 -dh8 -ftl -n36600 -b1.1.1500 -s5", REPEATS_HEAVY);
    }

    // ------------------------------------------------------------------ broad functionality sweep

    /**
     * Everything else worth timing: plain positions, frames and calculation flags, sidereal
     * modes, house systems, calendars, and the two methods the pure-Java engine does not
     * implement at all ({@code -orbel}, {@code -pc}) - included on purpose so the report states
     * "not ported" for them rather than silently omitting two real, documented capability
     * differences between the engines. Command lines are the same ones
     * {@code tools/diff-vs-swetest.sh} already proved run cleanly on both engines (minus the six
     * lines documented there as KNOWN to differ for reasons outside this port, and minus what the
     * two categories above already cover).
     */
    @Test
    @Order(4)
    void broadFunctionalitySweep() {
        System.out.println();
        System.out.println("=== broad functionality sweep (positions, frames, houses, sidereal modes) ===");
        bench("broad", "ten planets, one date/time", "-b18.4.1976 -ut20:21:00 -p0123456789");
        bench("broad", "all houses + planets, PLBRS format", "-b1.1.2000 -pd -fPLBRS");
        bench("broad", "tabular, comma-sep, no header", "-b1.1.2000 -p0123456789 -fTZ -g, -head");
        bench("broad", "Mercury, 15 positions, 2-day steps", "-b1.12.1900 -p2 -n15 -s2");
        bench("broad", "one row per day, 5 days, all planets", "-b1.1.2016 -g -fTlbR -p0123456789Dmte -hor -n5 -roundsec");
        bench("broad", "asteroids, main belt group", "-b1.1.2000 -pa -fPL");
        bench("broad", "hypothetical/uranian bodies", "-b1.1.2000 -ph -fPL");
        bench("broad", "planets + main asteroids", "-b1.1.2000 -pp -fPL");
        bench("broad", "single numbered asteroid by index", "-b1.1.2000 -pz -xz1 -fPL");
        bench("broad", "single numbered planetoid by index", "-b1.1.2000 -pv -xv9501 -fPL");
        bench("broad", "sidereal Lahiri, true positions", "-b1.1.2000 -p0123456789 -sid1 -true -fPl");
        bench("broad", "sidereal True Chitrapaksha", "-b1.1.2000 -p0123456789 -sid27 -fPl");
        bench("broad", "sidereal Lahiri, one body", "-b1.1.2000 -pb -sid1 -fPl");
        bench("broad", "heliocentric", "-b1.1.2000 -p2 -hel -fPl");
        bench("broad", "J2000 frame", "-b1.1.2000 -p2 -j2000 -fPl");
        bench("broad", "no nutation", "-b1.1.2000 -p2 -nonut -fPl");
        bench("broad", "position + declination + whole-sign", "-b1.1.2000 -p0 -fPaDdW");
        bench("broad", "position + right ascension + Uranian house", "-b1.1.2000 -p0 -fPXU");
        bench("broad", "differential Moon-Sun angular distance", "-b1.1.2000 -p1 -d0 -fPTl");
        bench("broad", "Saturn-Chiron midpoints, 5x5d steps", "-b1.12.1900 -p6 -DD -n5 -s5 -fPTZ -roundmin");
        bench("broad", "fixed star Aldebaran, full format", "-b1.1.2000 -pf -xfAldebaran -fPLBR");
        bench("broad", "asteroid 433 Eros, full format", "-b1.1.2000 -ps -xs433 -fPLBR");
        bench("broad", "barycentric", "-b1.1.2000 -p0 -bary -fPL");
        bench("broad", "topocentric", "-b1.1.2000 -p0 -topo8,47,0 -fPL");
        bench("broad", "Moshier ephemeris", "-b1.1.2000 -p0 -emos -fPL");
        bench("broad", "Swiss ephemeris (explicit)", "-b1.1.2000 -p0 -eswe -fPL");
        bench("broad", "ICRS frame", "-b1.1.2000 -p0 -icrs -fPL");
        bench("broad", "no aberration", "-b1.1.2000 -p0 -noaberr -fPL");
        bench("broad", "no light deflection", "-b1.1.2000 -p0 -nodefl -fPL");
        bench("broad", "with speed", "-b1.1.2000 -p0 -speed -fPLS");
        bench("broad", "without speed", "-b1.1.2000 -p0 -nospeed -fPL");
        bench("broad", "Placidus houses + G/g/j auxiliary columns", "-b1.1.2000 -p0 -house8,47,P -ut12:00 -fPGgj");
        bench("broad", "whole sign houses", "-b1.1.2000 -p0 -house8,47,W -ut12:00");
        bench("broad", "36 Gauquelin sectors", "-b1.1.2000 -p0 -house8,47,G -fPGgj");
        bench("broad", "Meridian/axial-rotation houses", "-b1.1.2000 -p0 -hsyX -house8,47 -fPGgj");
        bench("broad", "absolute julian day input", "-bj2451545.0 -p0 -fPL");
        bench("broad", "ancient date, Julian calendar", "-b4.4.1000 -p0 -fPL");
        bench("broad", "ancient date, proleptic Gregorian", "-b4.4.1000greg -p0 -fPL");
        bench("broad", "UTC input", "-b1.1.2000 -p0 -fPL -utc12:00:00");
        bench("broad", "LMT output with geopos", "-b1.1.2000 -p0 -lmt12:00 -geopos8,47,0 -fPL");
        bench("broad", "LAT output with geopos", "-b1.1.2000 -p0 -lat -geopos8,47,0 -fPL");
        bench("broad", "atmospheric pressure/temperature", "-b1.1.2000 -p0 -at1013,10 -geopos8,47,0 -fPL");
        bench("broad", "observer eye conditions", "-b1.1.2000 -p0 -obs30,60 -fPL");
        bench("broad", "orbital elements (native only)", "-b1.1.2000 -p0 -orbel");
        bench("broad", "planetocentric positions (native only)", "-b1.1.2000 -p0 -pc3 -fPL");
    }

    // ------------------------------------------------------------------ grand summary

    /**
     * One combined table, category by category: total native time, total pure-Java time (the
     * "not ported" rows excluded from both sides, since there is nothing on the pure-Java side to
     * sum), the ratio, and how many of that category's cases produced byte-identical output. Not
     * a {@code @AfterAll} - JUnit does not guarantee method execution order relative to it across
     * a class unless every {@code @Test} has already run, and this one deliberately runs last via
     * {@code @Order} instead so it can be a normal, independently re-runnable test method that
     * simply prints whatever {@link #ROWS} holds at the point it runs.
     */
    @Test
    @Order(5)
    void printGrandSummary() {
        System.out.println();
        System.out.println("=== grand summary ===");
        if (ROWS.isEmpty()) {
            System.out.println("  (no cases ran - printGrandSummary must run after the other test methods)");
            return;
        }
        final List<String> categories = new ArrayList<>();
        for (final String[] row : ROWS) if (!categories.contains(row[0])) categories.add(row[0]);

        final StringBuilder summary = new StringBuilder();
        summary.append("category    label                                         native ms   pure-java ms  ratio    verdict      command line\n");

        double grandNative = 0, grandPure = 0;
        int grandSame = 0, grandTotal = 0, grandNotPorted = 0;
        for (final String category : categories) {
            double catNative = 0, catPure = 0;
            int catSame = 0, catTotal = 0, catNotPorted = 0;
            for (final String[] row : ROWS) {
                if (!category.equals(row[0])) continue;
                catTotal++;
                final double nMs = Double.parseDouble(row[3]);
                catNative += nMs;
                final boolean notPorted = row[4].isEmpty();
                final double pMs = notPorted ? Double.NaN : Double.parseDouble(row[4]);
                if (notPorted) catNotPorted++; else catPure += pMs;
                if ("same output".equals(row[5])) catSame++;
                summary.append(String.format(Locale.ROOT, "%-10s  %-44s  %10.3f  %12s  %7s  %-12s  %s%n",
                        row[0], row[1], nMs, notPorted ? "n/a" : String.format(Locale.ROOT, "%.3f", pMs),
                        notPorted ? "-" : String.format(Locale.ROOT, "%.2fx", pMs / nMs), row[5], row[2]));
            }
            System.out.println(String.format(Locale.ROOT,
                    "  %-10s  %2d cases  native total %10.3f ms  pure-java total %10.3f ms  "
                            + "ratio %6.2fx  same output %d/%d  not ported %d",
                    category, catTotal, catNative, catPure,
                    0 == catNative ? 0 : catPure / catNative, catSame, catTotal - catNotPorted, catNotPorted));
            grandNative += catNative;
            grandPure += catPure;
            grandSame += catSame;
            grandTotal += catTotal;
            grandNotPorted += catNotPorted;
        }
        System.out.println(String.format(Locale.ROOT,
                "  %-10s  %2d cases  native total %10.3f ms  pure-java total %10.3f ms  "
                        + "ratio %6.2fx  same output %d/%d  not ported %d",
                "ALL", grandTotal, grandNative, grandPure,
                0 == grandNative ? 0 : grandPure / grandNative, grandSame, grandTotal - grandNotPorted, grandNotPorted));
        System.out.println();

        writeText(new File(REF_DIR, "summary.txt"), summary.toString());
    }

    /**
     * The one assertion in this class - and it checks output <i>content</i> regressions
     * ({@link #writeOrCompare}), never timing, which is why it can exist in a class whose whole
     * point is otherwise informational. Deliberately last ({@code @Order(6)}) and deliberately a
     * separate method from every {@code bench()} call, so a mismatch found in case 5 of 24 does
     * not stop cases 6-24 of that category from running (and being compared/written) too - every
     * case always runs to completion, and this reports every regression found across all of them
     * at once.
     */
    @Test
    @Order(6)
    void checkNoRegressions() {
        assertTrue(REGRESSIONS.isEmpty(), () -> REGRESSIONS.size() + " case(s) changed from the "
                + "committed reference under " + REF_DIR.getAbsolutePath() + " - review the actual "
                + "output written alongside each (or re-run with -Dswe.perf.regen=true to accept "
                + "the current run as the new baseline):\n  " + String.join("\n  ", REGRESSIONS));
    }
}
