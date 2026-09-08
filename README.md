# swe-jni-demo

swe-jni-demo demonstrates the use of JNI interface based on:
- https://www.astro.com/ftp/swisseph/src/swemini.c
- https://github.com/krymlov/swe-java-lib
- https://github.com/krymlov/swe-jni-lib

Everything here depends only on `swisseph:swe-api` - the raw JNI bindings (`SwephExp`) - never
on `swisseph:swe` (the higher-level `swe-java-lib` wrapper), so every example shows exactly what
calling the native library directly looks like.

Three examples, each exercising a different part of the JNI surface:

| class | shows |
|---|---|
| `SweMini` | planets only - a Java port of astro.com's own `swemini.c` |
| `SweObama` | planets, houses and the ascendant/MC for one real chart |
| `SweTest`  | a small command-line front end, styled after astro.com's own `swetest.c` reference program (https://www.astro.com/ftp/swisseph/src/swetest.c) - sidereal mode and ayanamsa, fixed stars, sunrise/sunset, and a global solar eclipse search. It parses a deliberately small subset of `swetest`'s own flags rather than porting the whole several-thousand-line file; run it with no arguments (or see the class javadoc) for the flags it understands, e.g.:<br>`java swisseph.SweTest -b18.4.1976 -ut20:21:00 -p0123456789mtf -xfSpica -house27.13,49.45,P -sid1 -true -rise -solecl` |

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