package swisseph;

/*
  swemini.c	A minimal program to test the Swiss Ephemeris.

  Input: a date (in gregorian calendar, sequence day.month.year)
  Output: Planet positions at midnight Universal time, ecliptic coordinates,
          geocentric apparent positions relative to true equinox of date, as
          usual in western astrology.

  Authors: Dieter Koch and Alois Treindl, Astrodienst Zurich

**************************************************************/
/* Copyright (C) 1997 - 2021 Astrodienst AG, Switzerland.  All rights reserved.

  License conditions
  ------------------

  This file is part of Swiss Ephemeris.

  Swiss Ephemeris is distributed with NO WARRANTY OF ANY KIND.  No author
  or distributor accepts any responsibility for the consequences of using it,
  or for whether it serves any particular purpose or works at all, unless he
  or she says so in writing.

  Swiss Ephemeris is made available by its authors under a dual licensing
  system. The software developer, who uses any part of Swiss Ephemeris
  in his or her software, must choose between one of the two license models,
  which are
  a) GNU Affero General Public License (AGPL)
  b) Swiss Ephemeris Professional License

  The choice must be made before the software developer distributes software
  containing parts of Swiss Ephemeris to others, and before any public
  service using the developed software is activated.

  If the developer choses the AGPL software license, he or she must fulfill
  the conditions of that license, which includes the obligation to place his
  or her whole software project under the AGPL or a compatible license.
  See https://www.gnu.org/licenses/agpl-3.0.html

  If the developer choses the Swiss Ephemeris Professional license,
  he must follow the instructions as found in http://www.astro.com/swisseph/
  and purchase the Swiss Ephemeris Professional Edition from Astrodienst
  and sign the corresponding license contract.

  The License grants you the right to use, copy, modify and redistribute
  Swiss Ephemeris, but only under certain conditions described in the License.
  Among other things, the License requires that the copyright notices and
  this notice be preserved on all copies.

  Authors of the Swiss Ephemeris: Dieter Koch and Alois Treindl

  The authors of Swiss Ephemeris have no control or influence over any of
  the derived works, i.e. over software or services created by other
  programmers which use Swiss Ephemeris functions.

  The names of the authors or of the copyright holder (Astrodienst) must not
  be used for promoting any software, product or service which uses or contains
  the Swiss Ephemeris. This copyright notice is the ONLY place where the
  names of the authors can legally appear, except in cases where they have
  given special permission in writing.

  The trademarks 'Swiss Ephemeris' and 'Swiss Ephemeris inside' may be used
  for promoting such software, products or services.
*/

import org.swisseph.ISwissEph;
import org.swisseph.SwephNative;

import java.util.Scanner;

import static java.lang.Integer.parseInt;
import static swisseph.SweConst.*;

public class SweMini {
    public static void main(String[] args) {
        final ISwissEph sweph = new SwephNative("ephe");

        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.println("\nDate (d.m.y) ?");

                String[] str = sc.nextLine().split("\\.");
                if (str.length < 3) return;

                swe_mini(sweph, parseInt(str[0]), parseInt(str[1]), parseInt(str[2]));
            }
        }
    }

    public static void swe_mini(ISwissEph sweph, int jday, int jmon, int jyear) {
        final StringBuilder serr = new StringBuilder();
        final double[] x2 = new double[6];
        double tjd, te, jut = 0.0;
        int iflag, iflgret, p;

        iflag = SEFLG_SPEED;

        /*
         * we have day, month and year and convert to Julian day number
         */
        tjd = sweph.swe_julday(jyear, jmon, jday, jut, SE_GREG_CAL);

        /*
         * compute Ephemeris time from Universal time by adding delta_t
         */
        te = tjd + sweph.swe_deltat(tjd);
        System.out.printf("date: %02d.%02d.%d at 0:00 Universal time", jday, jmon, jyear);
        System.out.print("\nplanet     \tlongitude\tlatitude\tdistance\tspeed long.\n");

        /*
         * a loop over all planets
         */
        for (p = SE_SUN; p <= SE_CHIRON; p++) {
            if (p == SE_EARTH) continue;

            /*
             * do the coordinate calculation for this planet p
             */
            iflgret = sweph.swe_calc(te, p, iflag, x2, serr);

            /*
             * if there is a problem, a negative value is returned and an
             * errpr message is in serr.
             */
            if (iflgret < 0) System.out.printf("error: %s\n", serr);
            else if (iflgret != iflag) System.out.printf("warning: iflgret != iflag. %s\n", serr);

            /*
             * get the name of the planet p
             */
            String snam = sweph.swe_get_planet_name(p);

            /*
             * print the coordinates
             */
            System.out.printf("%10s\t%11.7f\t%10.7f\t%10.7f\t%10.7f\n", snam, x2[0], x2[1], x2[2], x2[3]);
        }
    }
}
