package swisseph.chakra;

import static java.util.TimeZone.getTimeZone;
import static org.jyotisa.app.KundaliOptions.KUNDALI_8_KARAKAS;
import static org.swisseph.api.ISweConstants.EPHE_PATH;
import static org.swisseph.app.SweObjectsOptions.LAHIRI_CITRAPAKSA;

import java.util.Calendar;
import java.util.TimeZone;

import org.jyotisa.api.IKundali;
import org.jyotisa.api.lagna.ILagnaEntity;
import org.jyotisa.api.lagna.ILagnaEnum;
import org.jyotisa.api.lagna.ILagnas;
import org.jyotisa.api.vimsottari.IVimsottariDasaEnum;
import org.jyotisa.app.Kundali;
import org.jyotisa.lagna.ELagna;
import org.jyotisa.vimsottari.EVimsottariDasa;
import org.jyotisa.vimsottari.VimsottariDasaBudha;
import org.swisseph.ISwissEph;
import org.swisseph.SwephNative;
import org.swisseph.api.ISweEnumIterator;
import org.swisseph.api.ISweGeoLocation;
import org.swisseph.api.ISweSegment;
import org.swisseph.app.SweGeoLocation;
import org.swisseph.app.SweJulianDate;
import org.swisseph.app.SweObjects;

public class CalculateVishottari {

	public static final ISweGeoLocation GEO_CHENNAI = new SweGeoLocation(80 + (16 / 60.), 13 + (5 / 60.), 6.7);
	protected static final ThreadLocal<ISwissEph> SWISS_EPHS = new ThreadLocal<>();
	protected static final ThreadLocal<ISwissEph> SWEPH_EXPS = new ThreadLocal<>();
    protected static IVimsottariDasaEnum vdEnum;

	protected static SwephNative newSwephExp() {
		return new SwephNative(EPHE_PATH);
	}

	public static ISwissEph getSwephExp() {
		ISwissEph swissEph = SWEPH_EXPS.get();

		if (null == swissEph) {
			swissEph = newSwephExp();
			SWEPH_EXPS.set(swissEph);
		}

		return swissEph;
	}

	protected static Calendar newCalendar(TimeZone timeZone) {
		Calendar calendar = Calendar.getInstance(timeZone);
		calendar.set(Calendar.HOUR_OF_DAY, 12);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}

	public static void main(String[] arg) {
		IKundali kundali = new Kundali(KUNDALI_8_KARAKAS,
				new SweObjects(getSwephExp(), new SweJulianDate(newCalendar(getTimeZone("Asia/Calcutta"))), GEO_CHENNAI,
						LAHIRI_CITRAPAKSA).completeBuild());
		System.out.println(kundali);

		ISweEnumIterator<IVimsottariDasaEnum> lagnaIterator = EVimsottariDasa.iteratorTo(EVimsottariDasa.SHUKRA_DASA);
		while (lagnaIterator.hasNext()) {
			IVimsottariDasaEnum data = lagnaIterator.next();
            IVimsottariDasaEnum data2 = vdEnum.all()[data.uid()];
            ISweSegment val = data2.dasa().segment();

			System.out.println(data.code() + " --> "+val);
		}
	}
}
