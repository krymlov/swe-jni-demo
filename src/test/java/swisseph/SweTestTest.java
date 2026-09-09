package swisseph;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static swisseph.SweTest.SE_ECL_NUT;
import static swisseph.SweTest.SE_FIXSTAR;
import static swisseph.SweTest.SE_MEAN_APOG;
import static swisseph.SweTest.SE_MEAN_NODE;
import static swisseph.SweTest.SE_PLUTO;
import static swisseph.SweTest.SE_SUN;
import static swisseph.SweTest.SE_TRUE_NODE;
import static swisseph.SweTest.cdouble;
import static swisseph.SweTest.letter_to_ipl;
import static swisseph.SweTest.swe_test;

/**
 * {@link SweTest} is a port of {@code swetest.c}, and the reference program it was ported from
 * is on this machine - so the only definition of "correct" worth using here is that the two
 * print the same bytes for the same arguments. That is what most of this class does; the rest
 * pins the two places where the port cannot simply mirror the C, because the two languages
 * disagree about what the C means.
 *
 * <p>The live comparison self-skips when {@code swetest64.exe} or the ephemeris files are
 * missing, so the suite still runs on a machine that has neither. Override the executable with
 * {@code -Dswetest.exe=...}. The full sweep of command lines lives in
 * {@code ai-github-projects/swe-jni-demo/tools/diff-vs-swetest.sh}; this class carries a
 * representative subset so a regression fails the build rather than only a script.
 */
public class SweTestTest {

    static final String SWETEST = System.getProperty("swetest.exe",
            "E:/Github/swisseph/windows/programs/swetest64.exe");

    static final File EPHE = new File("ephe").getAbsoluteFile();

    @BeforeAll
    static void loadLibrary() {
        SwephExp.loadSweCurrentLibrary();
    }

    // ------------------------------------------------------------------ the C, letter by letter

    @Test
    void letterToIpl_matchesTheSwephexpBodyNumbers() {
        for (int i = 0; i <= 9; i++) assertEquals(SE_SUN + i, letter_to_ipl('0' + i));
        assertEquals(SE_PLUTO, letter_to_ipl('9'));
        assertEquals(SE_MEAN_APOG, letter_to_ipl('A'));
        assertEquals(SE_MEAN_NODE, letter_to_ipl('m'));
        assertEquals(SE_TRUE_NODE, letter_to_ipl('t'));
        assertEquals(SE_FIXSTAR, letter_to_ipl('f'));
        assertEquals(SE_ECL_NUT, letter_to_ipl('n'));

        // the selectors swetest expands itself rather than passing to swe_calc
        for (char c : "eqyxbsvzdpha".toCharArray()) assertEquals(-1, letter_to_ipl(c));
        assertEquals(-2, letter_to_ipl('!'));
    }

    /**
     * The one place a faithful port cannot use {@code String.format}.
     *
     * <p>Java formats a double from its <b>shortest representation</b> and rounds that; C rounds
     * the <b>exact binary value</b>. Ask for more digits than the shortest repr carries and the
     * two answer differently about one double - which is exactly what swetest does, printing a
     * julian day at {@code %.9f}. The julian day below is the real case, from
     * {@code -b1.1.2000 -px -fPL -ut12:00}:
     *
     * <pre>
     *   shortest repr  2451545.0007387605                     -> Java rounds half-up  ...761
     *   exact value    2451545.00073876045644283294677734375  -> C rounds down        ...760
     * </pre>
     *
     * Both are honest; they answer different questions. Only the second one is what the reference
     * program prints, so it is the one the port has to reproduce.
     */
    @Test
    void cdouble_printsTheExactBinaryValueTheWayCDoes() {
        final double tt = 2451545.0007387605;

        assertEquals("2451545.000738760", cdouble(tt, "", 0, 9));
        assertEquals("2451545.000738761", String.format(java.util.Locale.ROOT, "%.9f", tt),
                "if this ever agrees with C, Java changed and the whole cformat detour can go");

        // width, sign and zero-fill, the flags swetest actually uses
        assertEquals("  1.500000", cdouble(1.5, "", 10, 6));
        assertEquals(" 1.50", cdouble(1.5, " ", 0, 2));
        assertEquals("-1.50", cdouble(-1.5, " ", 0, 2));
        assertEquals("+1.50", cdouble(1.5, "+", 0, 2));
        assertEquals("1.500     ", cdouble(1.5, "-", 10, 3));
        assertEquals("-00001.500", cdouble(-1.5, "0", 10, 3));   // zeros go after the sign
        assertEquals("nan", cdouble(Double.NaN, "", 0, 6));
    }

    /**
     * {@code sscanf} counts what it converted and leaves the rest of its destinations alone;
     * {@code String.split} has no such notion, and the difference is not academic.
     * {@code "".split(",")} is one field, {@code atof("")} is 0.0, so {@code -astpos} with no
     * number moved it off its -1 sentinel and the option started listing asteroids around
     * longitude 0 instead of standing down. The same shape sat under {@code -at}, {@code -obs},
     * {@code -opt}, {@code -topo}, {@code -geopos} and {@code -house}.
     */
    @Test
    void scanned_countsWhatSscanfWouldHaveConverted() {
        assertEquals(0, SweTest.scanned("".split(",")));
        assertEquals(0, SweTest.scanned("abc".split(",")));
        assertEquals(1, SweTest.scanned("8".split(",")));
        assertEquals(2, SweTest.scanned("8,47".split(",")));
        assertEquals(3, SweTest.scanned("8,47,0".split(",")));
        assertEquals(-1.5, Double.parseDouble("-1.5"));      // signs and dots start a number
        assertEquals(3, SweTest.scanned("-1.5,+2,.5".split(",")));

        // sscanf stops at the first failure and reports the count so far - it does not skip on
        assertEquals(1, SweTest.scanned("8,,0".split(",")));
        assertEquals(2, SweTest.scanned("8,47,K".split(",")));   // -house's %c third field
    }

    /** the case that sent the option off its sentinel, end to end */
    @Test
    void astposWithNoNumberDoesNothingRatherThanListingAroundZero() {
        final String out = port(new String[]{"-b1.1.2000", "-p0", "-astpos", "-n2"});
        assertEquals(-1, out.indexOf("swe_get_named_ast_list"), out);
        assertTrue(out.contains("Sun"), out);
    }

    /**
     * The one place the port deliberately does not follow the C. Given no date, swetest prompts
     * on the terminal ({@code Date ?}) and reads stdin; this port hands its output back as a
     * String and is meant to be callable from a test or a UI, where blocking on stdin is not an
     * answer. It says what is missing instead.
     */
    @Test
    void withoutADateItSaysSoRatherThanPromptingOnStdin() {
        final String out = swe_test(new String[]{"-ut12:00:00"});
        assertTrue(out.startsWith("swetest: no date given"), out);
        assertEquals(-1, out.indexOf("Date ?"), out);
    }

    /**
     * swetest.c's state is file-scope statics, so the port keeps them as statics too and resets
     * them per run. Without that, a second call in one JVM inherits the first one's ayanamsa,
     * ephemeris flags and format string - and answers plausibly, in the wrong frame.
     */
    @Test
    void twoRunsInOneJvmCannotInterfere() {
        final String[] line = {"-b1.1.2000", "-p0", "-fPL", "-edir" + EPHE};
        final String first = swe_test(line);
        swe_test(new String[]{"-b1.1.2000", "-p0123456789", "-sid1", "-true", "-fPZ",
                "-roundsec", "-house8,47,K", "-edir" + EPHE});
        assertEquals(first, swe_test(line));
    }

    // ------------------------------------------------------------------ against the real program

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "-b1.12.1900 -p2",
            "-b18.4.1976 -ut20:21:00 -p0123456789",
            "-b1.1.2000 -pd -fPLBRS",
            "-b1.1.2000 -p0 -fPZ -roundsec",
            "-b1.1.2000 -p0123456789 -fTZ -g, -head",
            "-b1.12.1900 -p2 -n15 -s2",
            "-b1.1.2016 -g -fTlbR -p0123456789Dmte -hor -n5 -roundsec",
            "-b1.1.2000 -p0123456789 -sid1 -true -fPl",
            "-b1.1.2000 -p0123456789 -sid27 -fPl",
            "-b1.1.2000 -pb -sid1 -fPl",
            "-b1.1.2000 -p2 -hel -fPl",
            "-b1.1.2000 -p2 -j2000 -fPl",
            "-b1.1.2000 -p0 -fPaDdW",
            "-b1.1.2000 -p0 -fPXU",
            "-b1.1.2000 -p1 -d0 -fPTl",
            "-b1.1.2000 -pf -xfAldebaran -fPLBR",
            "-b1.1.2000 -ps -xs433 -fPLBR",
            "-b5.1.2002 -p -house12.05,49.50,K -ut12:30",
            "-b1.1.2000 -p0 -house8,47,P -ut12:00 -fPGgj",
            "-bj2451545.0 -p0 -fPL",
            "-b4.4.1000 -p0 -fPL",
            "-b4.4.1000greg -p0 -fPL",
            "-b1.1.2000 -p0 -fPL -utc12:00:00",
            "-b1.1.2000 -p0 -rise -geopos8,47,0 -n3",
            "-b1.1.2000 -p0 -metr -geopos8,47,0 -n2",
            "-b1.1.2000 -lunecl -n2",
            "-b1.1.2000 -solecl -n2",
            "-b1.1.2000 -solecl -local -geopos8,47,0 -n2",
            "-b1.1.2000 -p2 -occult -n2",
            "-b1.1.2000 -p1 -hev1 -geopos8,47,0",
            "-b1.1.2000 -p0 -orbel",
            "-b1.1.2000 -p0 -fPLv",
            "-b1.1.2000 -p0 -short",
            "-b1.1.2000 -p0 -dms -fPL",
            "-b1.1.2000 -pa -fPL",
            "-b1.1.2000 -ph -fPL",
            "-b1.1.2000 -pz -xz1 -fPL",
            "-b1.1.2000 -pv -xv9501 -fPL",
            "-b1.1.2000 -p0 -bary -fPL",
            "-b1.1.2000 -p0 -topo8,47,0 -fPL",
            "-b1.1.2000 -p0 -emos -fPL",
            "-b1.1.2000 -p0 -jplhor -fPL",
            "-b1.1.2000 -p0 -speed3 -fPLS",
            "-b1.1.2000 -p0 -cob -fPL",
            "-b1.1.2000 -p0 -sidbit256 -sid1 -fPl",
            "-b1.1.2000 -p0 -sidt0 -sid1 -fPl",
            "-b1.1.2000 -p0 -hindu -fPl",
            "-b1.1.2000 -p0 -ay1",
            "-b1.1.2000 -p0 -tidacc-25.8 -fPL",
            "-b1.1.2000 -p0 -house8,47,G -fPGgj",
            "-b1.1.2000 -p0 -hsyX -house8,47 -fPGgj",
            "-b1.1.2000 -p0 -lmt12:00 -geopos8,47,0 -fPL",
            "-b1.1.2000 -p0 -lat -geopos8,47,0 -fPL",
            "-b1.1.2000 -lunecl -penumbral -n2",
            "-b1.1.2000 -solecl -total -n2",
            "-b1.1.2000 -p0 -rise -disccenter -geopos8,47,0 -n2",
            "-b1.1.2000 -p1 -hev3 -geopos8,47,0",
            "-b1.1.2000 -p0 -at1013,10 -geopos8,47,0 -fPL",
            "-b1.1.2000 -p0 -obs30,60 -fPL",
            "-b1.1.2000 -testaa97",
            // -sidudef WITH a comma: the form that does not crash the reference program
            "-b1.1.2000 -p0 -sidudef2451545,24.5 -fPl",
    })
    void thePortPrintsWhatTheReferenceProgramPrints(String commandLine) {
        final String[] args = commandLine.split(" ");
        assertEquals(reference(args), port(args));
    }

    /**
     * The six command lines that do <b>not</b> match, each for a reason outside the port. They
     * are asserted to differ so that a fix elsewhere shows up as a failing test rather than as
     * nothing at all, and every one of them is paired with a control that must still match -
     * which is what makes this a diagnosis rather than a list of excuses.
     *
     * <p>Three are the shipped executable being older than {@code swetest.c} (it is dated
     * Aug 2024): {@code 16e1806} 2026-03-01 made {@code -roundmin} apply to output field
     * {@code l}; {@code ff04db0} 2026-04-28 dropped {@code "&amp;&amp; ipl &gt; SE_AST_OFFSET"} so
     * {@code -lim} prints the range line for ordinary planets too; {@code 3fd0f95} 2026-08-08
     * added {@code -astpos} outright.
     *
     * <p>{@code -sidudef<jd>} with no comma <b>segfaults the reference program</b> -
     * {@code swetest.c:927} calls {@code strstr(sp, "jdisut")} where {@code sp} is
     * {@code strchr(s1, ',')}, so NULL when there is no comma. {@code -glp} prints the path of
     * the running program and so can never agree. And {@code -amod} differs under the library
     * rather than in either program: with {@code swed} as {@code __thread} - this workspace's
     * one deliberate divergence in {@code sweodef.h} - and {@code -O2} or above,
     * {@code swe_get_astro_models()} echoes stale model numbers. Reproduced in pure C with no
     * Java involved; only the echo line is affected, and the models themselves are applied
     * correctly, which the control below asserts.
     */
    @Test
    void theSixKnownDifferences() {
        // the exe predates 16e1806 - and the port does what the source says
        final String[] roundmin = {"-b1.1.2000", "-p0", "-fPl", "-roundmin"};
        assertNotEquals(reference(roundmin), port(roundmin));
        assertTrue(port(roundmin).contains("Sun              279.86"), port(roundmin));

        // the exe predates ff04db0 - the asteroid case, which that commit did not touch, agrees
        final String[] limPlanet = {"-b1.1.2000", "-p0", "-fPL", "-lim"};
        assertNotEquals(reference(limPlanet), port(limPlanet));
        assertTrue(port(limPlanet).contains("range "), port(limPlanet));
        final String[] limAsteroid = {"-b1.1.2000", "-ps", "-xs433", "-fPL", "-lim"};
        assertEquals(reference(limAsteroid), port(limAsteroid));

        // The exe predates 3fd0f95 and rejects the option outright. It prints no argv echo
        // when it does that, so dropFirstLine() takes the "illegal option" line itself and
        // what is left is empty - the same shape as the segfault below, for a different reason.
        final String[] astpos = {"-b1.1.2000", "-p0", "-astpos", "-n2"};
        assertTrue(reference(astpos).isEmpty(), reference(astpos));
        assertTrue(port(astpos).contains("Sun"), port(astpos));

        // An upstream NULL dereference: the reference program dies part-way through. How much
        // it manages to print first depends on how the process is started - nothing under an
        // MSYS shell, a few buffered lines when Java starts it - so the assertion is only that
        // the two differ and that the port completes the chart the C could not.
        final String[] sidudef = {"-b1.1.2000", "-p0", "-sidudef2451545", "-fPl"};
        assertNotEquals(reference(sidudef), port(sidudef));
        assertTrue(port(sidudef).contains("ayanamsa"), port(sidudef));
        // with a comma there is no crash and the two agree - including C's "(null)" for the
        // name of a user-defined ayanamsa, which swe_get_ayanamsa_name() returns as NULL
        final String[] withComma = {"-b1.1.2000", "-p0", "-sidudef2451545,24.5", "-fPl"};
        assertEquals(reference(withComma), port(withComma));
        assertTrue(port(withComma).contains("((null))"), port(withComma));

        // the path of the running program: java.exe on one side, swetest64.exe on the other
        final String[] glp = {"-b1.1.2000", "-p0", "-glp", "-fPL"};
        assertNotEquals(reference(glp), port(glp));

        // under the library, not in the port: only the echo line moves, the models still apply
        final String[] amod = {"-b1.1.2000", "-p0", "-amod1,2", "-fPL"};
        assertNotEquals(reference(amod), port(amod));
        assertTrue(port(amod).contains("Delta T (long-term): Stephenson/Morrison 1984"), port(amod));
        assertTrue(port(amod).contains("Precession: Laskar 1986"), port(amod));
    }

    // ------------------------------------------------------------------ helpers

    /** the port's own output, minus the echoed argument line */
    static String port(String[] args) {
        requireReference();
        final List<String> a = new ArrayList<>();
        for (String s : args) a.add(s);
        a.add("-edir" + EPHE);
        return dropFirstLine(swe_test(a.toArray(new String[0])));
    }

    /**
     * {@code swetest64.exe}'s output, minus the echoed argument line - which carries the path of
     * the executable itself and so can never match. Its line endings are CRLF because C's stdout
     * is a text stream on Windows; the port keeps LF and translates only in {@code main()}, so
     * the comparison is made in LF.
     */
    static String reference(String[] args) {
        requireReference();
        try {
            final List<String> cmd = new ArrayList<>();
            cmd.add(SWETEST);
            for (String s : args) cmd.add(s);
            cmd.add("-edir" + EPHE);

            final Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            final ByteArrayOutputStream buf = new ByteArrayOutputStream();
            final byte[] chunk = new byte[8192];
            try (InputStream in = p.getInputStream()) {
                for (int n; (n = in.read(chunk)) > 0; ) buf.write(chunk, 0, n);
            }
            p.waitFor();
            return dropFirstLine(new String(buf.toByteArray(), "UTF-8").replace("\r\n", "\n"));
        } catch (Exception e) {
            throw new IllegalStateException("could not run " + SWETEST, e);
        }
    }

    static String dropFirstLine(String s) {
        final int nl = s.indexOf('\n');
        return nl < 0 ? "" : s.substring(nl + 1);
    }

    /**
     * A missing executable or ephemeris skips the comparison rather than failing it. Both are
     * required: without the {@code .se1} files Swiss Ephemeris falls back to Moshier with only a
     * warning, and the two sides would then differ for a reason that is not a port bug.
     */
    static void requireReference() {
        Assumptions.assumeTrue(new File(SWETEST).isFile(), "swetest64.exe not found: " + SWETEST);
        Assumptions.assumeTrue(new File(EPHE, "sepl_18.se1").isFile(), "no ephemeris in " + EPHE);
    }
}
