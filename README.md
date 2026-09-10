# swe-jni-demo

swe-jni-demo demonstrates the use of JNI interface based on:
- https://www.astro.com/ftp/swisseph/src/swemini.c
- https://github.com/krymlov/swe-java-lib
- https://github.com/krymlov/swe-jni-lib

`SweMini` and `SweObama` depend only on `swisseph:swe-api` - the raw JNI bindings (`SwephExp`) -
never on `swisseph:swe` (the higher-level `swe-java-lib` wrapper), so both show exactly what
calling the native library directly looks like. `SweTest` is the one exception: it reaches Swiss
Ephemeris through `org.swisseph.ISwissEph` rather than `SwephExp` directly, specifically so it
can run against either of `swe-java-lib`'s two implementations - see below.

Three examples, each exercising a different part of the JNI surface:

| class | shows |
|---|---|
| `SweMini` | planets only - a Java port of astro.com's own `swemini.c` |
| `SweObama` | planets, houses and the ascendant/MC for one real chart |
| `SweTest`  | a **full port** of astro.com's own `swetest.c` reference program (https://www.astro.com/ftp/swisseph/src/swetest.c) - see below |

## `SweTest` - swetest.c in Java

Every option, every output format letter and every special-event mode of `swetest.c`, so the
same command line produces the same bytes:

```
java swisseph.SweTest -b18.4.1976 -ut20:21:00 -p0123456789mtf -xfSpica \
     -house27.13,49.45,P -sid1 -true -rise -solecl
java swisseph.SweTest -h          # the reference program's own help text
```

That claim is checked rather than asserted. `swetest64.exe` is the definition of correct, so
**105 command lines** are run through both and diffed - positions, every frame and flag,
sidereal modes, houses, calendars, rise/set, meridian transits, both eclipse kinds, local
eclipses, occultations, heliacal events, asteroids, fixed stars, orbital elements and the
horizontal table. All of them agree, bar six that are known to differ for a reason outside the
port and are named in the script:

```bash
../ai-github-projects/swe-jni-demo/tools/diff-vs-swetest.sh          # all of them
../ai-github-projects/swe-jni-demo/tools/diff-vs-swetest.sh -b1.1.2000 -p0   # just one
```

`SweTestTest` carries a representative subset of the same comparison, so a regression fails the
build and not only a script. It self-skips when `swetest64.exe` or the ephemeris files are
missing; point it elsewhere with `-Dswetest.exe=...`.

**Two places a faithful port cannot simply mirror the C.** `%f` is formatted from the exact
binary value the way C does rather than through `String.format`, which formats from a double's
shortest representation and answers differently once you ask for more digits than that carries -
visible as the last digit of a julian day printed at `%.9f`. And `main()` writes UTF-8 explicitly
and translates LF to the platform ending, because C's stdout is a text stream: the port holds
exactly the bytes `swetest.c` writes and does the CRLF translation at the one place the C runtime
does it.

**One deliberate difference.** Given no date, `swetest` prompts on the terminal and reads stdin.
This port hands its output back as a String and is meant to be callable from a test or a UI, so
it says what is missing instead.

The ~104 `SE_*`/`SEFLG_*` constants are **generated** from `swephexp.h` rather than retyped - one
wrong bit in a `SEFLG_*` changes the answer without changing the shape of the output. Regenerate
after using a constant the block does not yet declare:

```bash
../ai-github-projects/swe-jni-demo/tools/regen-consts.sh
```

### Two engines, one command line

`SweTest` reaches Swiss Ephemeris through a single field, `static ISwissEph sw` - the native
`org.swisseph.SwephNative` by default, or the pure-Java `swisseph.SwissEph` port
(`swe-java-lib`'s second implementation) via `SweTest.swe_test(ISwissEph, String[])`. Same
parser, same `print_line`, same command line; only the engine changes:

```java
SweTest.swe_test(new SwephNative("ephe"), args);   // the default
SweTest.swe_test(new SwissEph("ephe"), args);       // the pure-Java port
```

`SwissEphEngineComparisonTest` runs the same command lines through both and diffs the output, at
a 1 arcsecond / 1 second bar. Most of what both engines compute agrees well inside that -
tropical and Lahiri-sidereal positions, house cusps, rise/set, transits and eclipses. Three
differences are documented rather than hidden: True Chitrapaksha's ayanamsa (a star-catalog
version gap, ~5"), Whole Sign's house-cusp speed column (a `swe_house_pos()` self-consistency
question, tens to hundreds of degrees on ten of twelve houses), and Koch's MC through the same
`G`/`g` columns (an exact 60° gap, narrow to that one house). `-orbel` and planetocentric
positions (`-pc`) are not ported to the pure-Java engine at all and throw
`NotImplementedException` there.

To build and run the project from command line you need:
- access to public maven repository
- https://maven.apache.org
- JDK 8 or newer

Inside the folder swe-jni-demo run the following command:
- mvn clean package exec:exec

### Restrictions:
- project includes swe-jni-lib for Windows x64 only, copied from:
  - https://github.com/krymlov/swe-jni-lib/tree/main/x64/Release

# Swiss Ephemeris License

Please make sure before you use the project you are familiar with the Swiss Ephemeris License
- https://www.astro.com/swisseph/swephinfo_e.htm
- https://www.astro.com/swisseph/secont_e.pdf

If you want the Swiss Ephemeris Free Edition for your software project, please proceed as follows:
- make sure you understand the License conditions
- download the software
- start programming