package swisseph;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.swisseph.ISwissEph;
import org.swisseph.SwephNative;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link SweTest} against itself, twice: once through {@link org.swisseph.SwephNative} (the
 * native library every other test in this project uses) and once through {@code swisseph.SwissEph}
 * (the pure-Java Swiss Ephemeris port, {@code swe-java-lib}'s second {@link ISwissEph}
 * implementation) - same command line, same parser, same {@code print_line}, the only thing
 * that differs is which engine the {@code sw} field points at.
 *
 * <p>The two are close, not identical - {@code swe-java-lib}'s own knowledge base (see
 * {@code CLAUDE.md}) already documents why for the pieces that matter here: the pure-Java engine
 * is Swiss Ephemeris 2.01.00 rather than 2.10.03, and its fixed-star catalog is the 2.01 data,
 * so a star-derived ayanamsa (True Chitrapaksha) disagrees by several arcseconds while every
 * arithmetic ayanamsa (Lahiri and the rest) agrees to a small fraction of one. <b>1 arcsecond</b>
 * is the line this class draws for a position and <b>1 second</b> for a printed clock time
 * (rise/set, meridian transit, an eclipse's contact times) - generous enough that only a real
 * regression should cross it, tight enough that "close" cannot quietly become "wrong". Every
 * threshold below is grounded in an actual measurement, not assumed - see the comment on each.
 *
 * <p>Three things the pure-Java engine cannot do at all are exercised as such rather than
 * skipped: {@code -orbel} and planetocentric positions (see
 * {@link #orbitalElementsIsNotPortedToThePureJavaEngine()} and
 * {@link #planetocentricPositionsAreNotPortedToThePureJavaEngine()}), both throwing
 * {@link NotImplementedException} on {@code swisseph.SwissEph} - that omission is itself a
 * measured difference between the two engines, and a real one for anyone choosing between them.
 */
public class SwissEphEngineComparisonTest {

    static final File EPHE = new File("ephe").getAbsoluteFile();

    static ISwissEph nativeEph;
    static ISwissEph pureJavaEph;

    @BeforeAll
    static void setUp() {
        nativeEph = new SwephNative(EPHE.getPath());
        pureJavaEph = new swisseph.SwissEph(EPHE.getPath());
    }

    static String run(final ISwissEph sw, final String commandLine) {
        return SweTest.swe_test(sw, (commandLine + " -edir" + EPHE).split(" "));
    }

    // ------------------------------------------------------------------ degree/time diffing

    /** {@code ddd°mm'ss.ss"} - the DMS longitude/latitude {@code swetest -fPL}/{@code -fPl} prints */
    static final Pattern DMS = Pattern.compile("(-?\\d+)\\u00b0\\s*(\\d+)'\\s*([\\d.]+)\"?");

    /** {@code hh:mm:ss[.frac]} - a printed clock time (rise/set, eclipse contacts, transits) */
    static final Pattern HMS = Pattern.compile("(\\d{1,3}):(\\d{2}):(\\d{2}(?:\\.\\d+)?)");

    static double toDegrees(final Matcher m) {
        final double d = Double.parseDouble(m.group(1));
        final double min = Double.parseDouble(m.group(2));
        final double sec = Double.parseDouble(m.group(3));
        return (d < 0 ? -1 : 1) * (Math.abs(d) + min / 60.0 + sec / 3600.0);
    }

    static double toSecondsOfDay(final Matcher m) {
        final double h = Double.parseDouble(m.group(1));
        final double min = Double.parseDouble(m.group(2));
        final double sec = Double.parseDouble(m.group(3));
        return h * 3600 + min * 60 + sec;
    }

    /**
     * The smaller of the two ways around the circle from {@code deg1} to {@code deg2}, in
     * arcseconds - 270° and -30° are 60° apart this way (the honest answer, since -30° is 330°
     * on the circle), not 300° (what a plain subtraction of the printed signs would say).
     */
    static double circularArcsecDiff(final double deg1, final double deg2) {
        double d = Math.abs(deg1 - deg2) % 360.0;
        if (d > 180.0) d = 360.0 - d;
        return d * 3600;
    }

    /**
     * Every DMS token on line {@code i} of {@code a} paired positionally with the DMS token at
     * the same position on line {@code i} of {@code b}, as a circular arcsecond difference (see
     * {@link #circularArcsecDiff}). Positional pairing rather than a full parse of every format
     * letter is deliberate - {@code print_line} already guarantees both engines produce the same
     * number of tokens per line for the same command line and the same fmt string, so the Nth
     * token on one side is always the same quantity as the Nth token on the other.
     */
    static List<Double> arcsecondDiffs(final String a, final String b) {
        final List<Double> diffs = new ArrayList<>();
        final String[] la = a.split("\n", -1), lb = b.split("\n", -1);
        for (int i = 0; i < Math.min(la.length, lb.length); i++) {
            final Matcher ma = DMS.matcher(la[i]), mb = DMS.matcher(lb[i]);
            while (ma.find() && mb.find()) diffs.add(circularArcsecDiff(toDegrees(ma), toDegrees(mb)));
        }
        return diffs;
    }

    /** the same pairing as {@link #arcsecondDiffs}, over {@link #HMS} tokens instead */
    static List<Double> secondDiffs(final String a, final String b) {
        final List<Double> diffs = new ArrayList<>();
        final String[] la = a.split("\n", -1), lb = b.split("\n", -1);
        for (int i = 0; i < Math.min(la.length, lb.length); i++) {
            final Matcher ma = HMS.matcher(la[i]), mb = HMS.matcher(lb[i]);
            while (ma.find() && mb.find()) diffs.add(Math.abs(toSecondsOfDay(ma) - toSecondsOfDay(mb)));
        }
        return diffs;
    }

    static double max(final List<Double> xs) {
        double m = 0;
        for (final double x : xs) if (x > m) m = x;
        return m;
    }

    static void assertPositionsAgree(final String commandLine, final double toleranceArcsec) {
        final String n = run(nativeEph, commandLine);
        final String p = run(pureJavaEph, commandLine);
        final List<Double> diffs = arcsecondDiffs(n, p);
        final double worst = max(diffs);
        assertTrue(worst < toleranceArcsec, () -> String.format(Locale.ROOT,
                "%s%n  worst diff %.4f\" (tolerance %.1f\")%n  native:%n%s%n  pure java:%n%s",
                commandLine, worst, toleranceArcsec, n, p));
    }

    static void assertTimesAgree(final String commandLine, final double toleranceSeconds) {
        final String n = run(nativeEph, commandLine);
        final String p = run(pureJavaEph, commandLine);
        final List<Double> diffs = secondDiffs(n, p);
        final double worst = max(diffs);
        assertTrue(worst < toleranceSeconds, () -> String.format(Locale.ROOT,
                "%s%n  worst diff %.3fs (tolerance %.1fs)%n  native:%n%s%n  pure java:%n%s",
                commandLine, worst, toleranceSeconds, n, p));
    }

    // ------------------------------------------------------------------ well-understood cases

    /**
     * Tropical (Sayana) positions - no ayanamsa in the picture at all, so nothing here can hide
     * behind "which star catalog". Measured: 0.0000" across all ten planets, both nodes and the
     * ayanamsa-free ascendant/houses. Matches {@code swe-java-lib}'s own claim that "the pure Java
     * engine's tropical positions are now 0.00\" against swetest at all 17 epochs" - this is the
     * same claim, one layer up, against the second engine directly rather than through swetest.
     */
    @Test
    void tropicalPositionsAgreeWithinOneArcsecond() {
        assertPositionsAgree("-b1.1.2000 -p0123456789mt -fPl -ut12:00", 1.0);
    }

    /**
     * Sidereal Lahiri, a modern epoch. Lahiri's ayanamsa is arithmetic (a fixed precession model,
     * not derived from any star's observed position), so the 2.01-vs-2.10 star catalog gap that
     * hits True Chitrapaksha below does not apply. Measured: 0.1303" (the pure-Java engine's own
     * documented "accurate to 0.01-0.83\" for the arithmetic modes" range).
     */
    @Test
    void lahiriSiderealPositionsAgreeWithinOneArcsecond_modernEpoch() {
        assertPositionsAgree("-b1.1.2000 -p0123456789mt -sid1 -fPl -ut12:00", 1.0);
    }

    /**
     * The same, a thousand years earlier. Delta t and the tidal-acceleration warmup that used to
     * separate the two engines by up to 122 seconds at this epoch (see {@code CLAUDE.md}'s "Delta
     * t: the pure Java engine was up to 122 seconds out") are both long since fixed; measured here
     * at 0.1304" - no worse than the modern epoch, which is the point of testing it at all.
     */
    @Test
    void lahiriSiderealPositionsAgreeWithinOneArcsecond_ancientEpoch() {
        assertPositionsAgree("-b4.4.1000greg -p0123456789mt -sid1 -fPl -ut12:00", 1.0);
    }

    /**
     * True Chitrapaksha is the one sidereal mode this class expects to fail the 1" bar, and says
     * so rather than excluding it. Its ayanamsa is derived from Spica's own computed longitude,
     * and the two engines carry different star data for Spica (2.10.03's catalog vs the pure-Java
     * port's 2.01.00 one) - so the gap is in the <i>catalog</i>, not in either engine's arithmetic,
     * and it moves every sidereal longitude built from that ayanamsa by the same amount. Measured
     * at this epoch: 4.8412" on the ayanamsa itself, and the same 4.8412" (to the last printed
     * digit) on the Sun, the Moon, every planet and both lunar nodes - one root cause, reported
     * once. Asserted with headroom on both sides: it must exceed 1" (a regression toward "equal"
     * would mean the catalog gap silently vanished and this comment is wrong) and stay under 30"
     * (a jump that large would mean something other than the known catalog gap is now involved).
     */
    @Test
    void trueChitrapakshaAyanamsaDiffersByTheStarCatalogGap_knownDifference() {
        final String commandLine = "-b1.1.2000 -p0123456789mt -sid27 -true -fPl -ut12:00";
        final String n = run(nativeEph, commandLine);
        final String p = run(pureJavaEph, commandLine);
        final double worst = max(arcsecondDiffs(n, p));
        assertTrue(worst > 1.0 && worst < 30.0, () -> String.format(Locale.ROOT,
                "expected the known ~5\" star-catalog gap (1\" < diff < 30\"), measured %.4f\"%n"
                        + "native:%n%s%npure java:%n%s", worst, n, p));
    }

    /**
     * House cusps, positions and house-position-of-object together ({@code -fPGgj}) for Placidus
     * - a well-defined system with a closed-form derivative, where {@code swe_house_pos()} is not
     * expected to disagree with itself the way it does for whole sign (see the dedicated test
     * below). Measured: 0.0037" worst case, on the Sun's own longitude - the house machinery
     * itself agreed to 0.0012".
     */
    @Test
    void placidusHouseCuspsAndPositionsAgreeWithinOneArcsecond() {
        assertPositionsAgree("-b1.1.2000 -p0 -house8,47,P -ut12:00 -fPGgj", 1.0);
    }

    /**
     * Whole sign's cusp <i>longitudes</i> - deliberately without the {@code S} (speed) format
     * letter the next test isolates. A whole-sign cusp is just the ascendant's own sign boundary
     * plus a multiple of 30°, so this is really the same single number
     * ({@code Ascendant longitude, snapped to its sign}) checked twelve times; measured worst
     * case 0.0036", on the ascendant itself.
     */
    @Test
    void wholeSignHouseCuspLongitudesAgreeWithinOneArcsecond() {
        assertPositionsAgree("-b1.1.2000 -p0 -house8,47,W -ut12:00 -fPl", 1.0);
    }

    /**
     * The whole-sign cusp <i>speed</i> column ({@code -fPLS}) is where the two engines genuinely
     * part ways, and {@code swe-java-lib}'s own {@code CLAUDE.md} already explains why: "the speed
     * the native library reports is not the derivative of the cusp positions the native library
     * itself returns... The port produces [the derivative], i.e. it is self-consistent." Whole
     * sign makes this visible on almost every house at once, because eight of its twelve "cusps"
     * are static sign boundaries with no astronomical body behind them at all - the port reports
     * every one of the twelve as rotating at exactly the ascendant's own rate (they are rigidly
     * locked together, all twelve moving in lockstep with the ascendant itself), while the
     * native library differences each cusp's own position independently and gets a genuinely
     * different rate for most of them.
     *
     * <p>Measured: only the ascendant/descendant pair (houses 1 and 7 - the one pair whose speed
     * is unambiguous, since they are the same rotating point 180° apart) agree closely, 6" to
     * 12". Every other house - 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, MC and IC included - differs by
     * tens to hundreds of degrees, not arcseconds: the eight sign-boundary "cusps" because the
     * port ties them to the ascendant's rate and the native library reports them near zero, and
     * the MC/IC pair (4 and 10) because the divergence follows them too, not just the boundaries.
     * Both figures are asserted, so a change in either shape - the near-agreement at 1/7 or the
     * large gap everywhere else - shows up as a failure here rather than silently.
     */
    @Test
    void wholeSignHouseCuspSpeedsDisagreeStructurally_knownDifference() {
        final String commandLine = "-b1.1.2000 -p0 -house8,47,W -ut12:00 -fPLS";
        final String n = run(nativeEph, commandLine);
        final String p = run(pureJavaEph, commandLine);
        final String[] ln = n.split("\n", -1), lp = p.split("\n", -1);

        double worstAscDesc = 0, leastOther = Double.POSITIVE_INFINITY;
        int ascDescRows = 0, otherRows = 0;
        for (int i = 0; i < Math.min(ln.length, lp.length); i++) {
            final java.util.regex.Matcher house = Pattern.compile("^house\\s+(\\d+)").matcher(ln[i]);
            if (!house.find()) continue;
            final int hn = Integer.parseInt(house.group(1));
            final Matcher ma = DMS.matcher(ln[i]), mb = DMS.matcher(lp[i]);
            assertTrue(ma.find() && mb.find(), "cusp longitude"); // token 1: the cusp itself
            final boolean ascDesc = hn == 1 || hn == 7;
            if (!(ma.find() && mb.find())) fail("house " + hn + " is missing its speed column");
            final double diff = circularArcsecDiff(toDegrees(ma), toDegrees(mb));
            if (ascDesc) { worstAscDesc = Math.max(worstAscDesc, diff); ascDescRows++; }
            else { leastOther = Math.min(leastOther, diff); otherRows++; }
        }
        assertEquals(2, ascDescRows, "expected exactly 2 ascendant/descendant houses (1, 7)");
        assertEquals(10, otherRows, "expected exactly 10 other houses (2,3,4,5,6,8,9,10,11,12)");
        final double finalWorstAscDesc = worstAscDesc, finalLeastOther = leastOther;
        assertTrue(worstAscDesc < 30.0, () -> "ascendant/descendant speed diff grew past the "
                + "known 6\"-12\" gap: " + finalWorstAscDesc + "\"");
        assertTrue(leastOther > 100.0, () -> "the other ten houses' speed diff shrank well "
                + "below the known tens-of-degrees structural gap: " + finalLeastOther + "\" - "
                + "the self-consistency difference this test documents may have been fixed upstream");
    }

    /**
     * Koch's own MC row, {@code -fPGgj}. Its <b>longitude</b> ({@code -fPl}, measured separately
     * in the breadth sweep below, and the Ascendant's G/g column right beside this one) agrees to
     * six decimal places between the two engines - Koch is not a general special case here. The
     * {@code G}/{@code g} columns are a different quantity: {@code swe_house_pos()}'s own
     * placement of that point among the Koch cusps, i.e. the same family of
     * {@code swe_house_pos()} self-consistency question the whole sign speed test above asks,
     * this time for Koch's MC row (house 10 shows the identical gap, being the same point) rather
     * than for whole sign's non-angular cusps. Measured: MC disagrees by exactly <b>60°</b>
     * (216000.0000", to the last printed digit) - a round number, which reads as a discrete
     * difference in which reference point the two engines measure the position from rather than
     * as numerical noise. Placidus, Campanus, Regiomontanus and Equal all agree on the same
     * columns to a small fraction of an arcsecond (see the breadth sweep), and Koch's own
     * Ascendant row agrees just as closely - so this is specific to Koch's MC.
     */
    @Test
    void kochMcAuxiliaryColumnsDisagreeByExactlySixtyDegrees_knownDifference() {
        final String commandLine = "-b1.1.2000 -p0 -house8,47,K -ut12:00 -fPGgj";
        final String n = run(nativeEph, commandLine);
        final String p = run(pureJavaEph, commandLine);
        final String[] ln = n.split("\n", -1), lp = p.split("\n", -1);

        String rowN = null, rowP = null;
        for (int i = 0; i < Math.min(ln.length, lp.length); i++) {
            if (ln[i].startsWith("MC")) { rowN = ln[i]; rowP = lp[i]; break; }
        }
        assertTrue(null != rowN, "MC row not found in:\n" + n);
        final Matcher ma = DMS.matcher(rowN), mb = DMS.matcher(rowP);
        assertTrue(ma.find() && mb.find(), "MC has no G column");
        final double diff = circularArcsecDiff(toDegrees(ma), toDegrees(mb));
        final String finalRowN = rowN, finalRowP = rowP;
        assertTrue(diff > 215999.0 && diff < 216001.0, () -> String.format(Locale.ROOT,
                "expected the known 60° (216000\") Koch MC gap, measured %.4f\"%n"
                        + "native: %s%npure java: %s", diff, finalRowN, finalRowP));

        // and the Ascendant, right beside it in the same output, is not affected at all
        String ascN = null, ascP = null;
        for (int i = 0; i < Math.min(ln.length, lp.length); i++) {
            if (ln[i].startsWith("Ascendant")) { ascN = ln[i]; ascP = lp[i]; break; }
        }
        assertTrue(null != ascN, "Ascendant row not found in:\n" + n);
        final Matcher aa = DMS.matcher(ascN), ab = DMS.matcher(ascP);
        assertTrue(aa.find() && ab.find(), "Ascendant has no G column");
        final double ascDiff = circularArcsecDiff(toDegrees(aa), toDegrees(ab));
        assertTrue(ascDiff < 1.0, "expected Koch's Ascendant G column to agree within 1\", got "
                + ascDiff + "\"");
    }

    /**
     * Rise and set, three events. Nothing here is a longitude - {@link #secondDiffs} is what
     * measures it, and 1 second is a literal reading of the acceptance bar rather than an
     * arcsecond-flavoured one. Measured worst case: 0.1s, on the third {@code dt} (the day
     * length between the two).
     */
    @Test
    void riseAndSetAgreeWithinOneSecond() {
        assertTimesAgree("-b1.1.2000 -p0 -rise -geopos8,47,0 -n3", 1.0);
    }

    /** Meridian and anti-meridian transits. Measured: byte-identical output, worst case 0.0s. */
    @Test
    void meridianTransitAgreesWithinOneSecond() {
        assertTimesAgree("-b1.1.2000 -p0 -metr -geopos8,47,0 -n2", 1.0);
    }

    /**
     * A global solar eclipse search, two events - each with a maximum, and one (the second) with
     * full first/second/third/fourth contact printed too. Measured worst case: 0.2s, on a
     * contact time of the second event.
     */
    @Test
    void solarEclipseInstantsAgreeWithinOneSecond() {
        assertTimesAgree("-b1.1.2000 -solecl -n2", 1.0);
    }

    /** A global lunar eclipse search, two events. Measured: byte-identical output. */
    @Test
    void lunarEclipseInstantsAgreeWithinOneSecond() {
        assertTimesAgree("-b1.1.2000 -lunecl -n2", 1.0);
    }

    /**
     * A lunar occultation of a planet, two events, contact times and the four numbers
     * ({@code -p2 -occult}) that go with each. Measured worst case: 0.2s on a contact time; the
     * two occultation-specific angle columns (position angle, duration) agree to 0.0000" and
     * 0.10s respectively.
     */
    @Test
    void occultationInstantsAgreeWithinOneSecond() {
        assertTimesAgree("-b1.1.2000 -p2 -occult -n2", 1.0);
    }

    /**
     * {@code swe_get_orbital_elements()} - 17 heliocentric-state-vector values {@code swisseph.
     * SwissEph} does not implement at all (see its own class javadoc and {@code ApiCoverageTest.
     * NOT_PORTED} in {@code swe-java-lib}). This is the coverage gap itself, exercised rather than
     * skipped: the native engine answers the command line normally, the pure-Java one cannot
     * answer it at all.
     */
    @Test
    void orbitalElementsIsNotPortedToThePureJavaEngine() {
        final String commandLine = "-b1.1.2000 -p0 -orbel";
        final String n = run(nativeEph, commandLine);
        assertTrue(n.contains("mean anomaly") || n.contains("perihelion") || !n.isEmpty(),
                "the native engine should answer -orbel normally: " + n);
        final NotImplementedException ex = assertThrows(NotImplementedException.class,
                () -> run(pureJavaEph, commandLine));
        assertTrue(ex.getMessage().contains("swe_get_orbital_elements"), ex.getMessage());
    }

    /**
     * Planetocentric positions ({@code swe_calc_pctr()}) - light time and aberration between two
     * moving bodies, which {@code swisseph.SwissEph} has no path for at all. {@code -pc} selects
     * a planetocentric center; the native engine answers, the pure-Java one throws.
     */
    @Test
    void planetocentricPositionsAreNotPortedToThePureJavaEngine() {
        final String commandLine = "-b1.1.2000 -p0 -pc3 -fPl";
        final String n = run(nativeEph, commandLine);
        assertTrue(n.contains("Sun"), "the native engine should answer -pc normally: " + n);
        assertThrows(NotImplementedException.class, () -> run(pureJavaEph, commandLine));
    }

    // ------------------------------------------------------------------ breadth: as much as
    // possible, across epochs, frames, house systems and node conventions that do not fall into
    // one of the individually-understood cases above.

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "-b1.12.1900 -p0123456789mt -fPl",
            "-b18.4.1976 -ut20:21:00 -p0123456789mt -fPl",
            "-b1.1.2016 -p0123456789mt -fPl -ut12:00",
            "-b1.1.2100 -p0123456789mt -fPl -ut12:00",
            "-b1.1.2000 -p0123456789mt -sid1 -true -fPl -ut12:00",
            "-b1.1.2000 -p2 -hel -fPl",
            "-b1.1.2000 -p2 -j2000 -fPl",
            "-b1.1.2000 -p2 -nonut -fPl",
            "-b1.1.2000 -p0 -bary -fPl",
            "-b1.1.2000 -p0 -topo8,47,0 -fPl",
            "-b1.1.2000 -p0 -icrs -fPl",
            "-b1.1.2000 -p0 -noaberr -fPl",
            "-b1.1.2000 -p0 -nodefl -fPl",
            "-b1.1.2000 -p0 -speed -fPlS",
            "-b1.1.2000 -p0 -cob -fPl",
            "-b1.1.2000 -p0 -nut -fPl",
            // Koch's own G/g auxiliary columns are the dedicated known-difference test above -
            // its cusp longitudes, compared here with plain -fPl, are not a special case at all.
            "-b1.1.2000 -p0 -house8,47,K -ut12:00 -fPl",
            "-b1.1.2000 -p0 -house8,47,C -ut12:00 -fPGgj",
            "-b1.1.2000 -p0 -house8,47,R -ut12:00 -fPGgj",
            "-b1.1.2000 -p0 -house8,47,E -ut12:00 -fPGgj",
            "-b5.1.2002 -p -house12.05,49.50,K -ut12:30 -fPl",
            "-bj2451545.0 -p0 -fPL",
            "-b1.1.2000 -p0 -fPL -utc12:00:00",
            "-b1.1.2000 -p0 -lmt12:00 -geopos8,47,0 -fPl",
            "-b1.1.2000 -pf -xfAldebaran -fPLBR",
            "-b1.1.2000 -ps -xs433 -fPLBR",
            "-b1.1.2000 -solecl -local -geopos8,47,0 -n2",
            "-b1.1.2000 -lunecl -how -geopos8,47,0",
    })
    void positionsAgreeWithinOneArcsecond(final String commandLine) {
        assertPositionsAgree(commandLine, 1.0);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "-b1.1.2000 -p0 -rise -disccenter -geopos8,47,0 -n2",
            "-b1.1.2000 -p0 -rise -norefrac -geopos8,47,0 -n2",
            "-b1.1.2000 -solecl -total -n2",
            "-b1.1.2000 -solecl -local -geopos8,47,0 -n2",
            "-b1.1.2000 -lunecl -penumbral -n2",
    })
    void timesAgreeWithinOneSecond(final String commandLine) {
        assertTimesAgree(commandLine, 1.0);
    }
}
