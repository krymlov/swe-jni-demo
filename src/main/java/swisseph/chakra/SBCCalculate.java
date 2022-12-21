package swisseph.chakra;

import static java.util.TimeZone.getTimeZone;
import static org.jyotisa.app.KundaliOptions.KUNDALI_8_KARAKAS;
import static org.swisseph.api.ISweConstants.EPHE_PATH;
import static org.swisseph.app.SweObjectsOptions.LAHIRI_CITRAPAKSA;
import static org.swisseph.utils.IDateUtils.format6;
import static org.swisseph.utils.IDegreeUtils.toDMS;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.jyotisa.api.IKundali;
import org.jyotisa.api.graha.IGrahaEntity;
import org.jyotisa.api.vimsottari.IVimsottariDasaEnum;
import org.jyotisa.app.Kundali;
import org.swisseph.ISwissEph;
import org.swisseph.SwephNative;
import org.swisseph.api.ISweGeoLocation;
import org.swisseph.app.SweGeoLocation;
import org.swisseph.app.SweJulianDate;
import org.swisseph.app.SweObjects;

import com.opencsv.CSVWriter;

public class SBCCalculate {
	public static final ISweGeoLocation GEO_CHENNAI = new SweGeoLocation(80 + (16 / 60.), 13 + (5 / 60.), 6.7);
	public static final ISweGeoLocation GEO_NEW_YORK = new SweGeoLocation(-(74 + (00 / 60.) + (23 / 3600.)), (40 + (42 / 60.) + (51 / 3600.)), 0);
	protected static final ThreadLocal<ISwissEph> SWISS_EPHS = new ThreadLocal<>();
	protected static final ThreadLocal<ISwissEph> SWEPH_EXPS = new ThreadLocal<>();
    protected static IVimsottariDasaEnum vdEnum;
    public static final String ABHIJIT_NAKSHATRA = "N28";
    public static final String ABHIJIT_NAKSHATRA_MAPTO_N21P4 = "N21P4";
    public static final String SBC_CSV_FILE_PATH = "sbc_veda.csv";

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
	
	public static String createFile(String filePath) {
		try {
			File myObj = new File(filePath);
			if (myObj.createNewFile()) {
				System.out.println("File created: " + myObj.getPath());
			} else {
				System.out.println("File already exists.");
			}
			return myObj.getPath();
		} catch (IOException e) {
			System.out.println("Can not create file" + e);
			e.printStackTrace();
		}
		return "";
	}
	
	public static boolean deleteFileInPath(String filePath) {
		File file = new File(filePath);
		if (file.delete()) {
			System.out.println("File deleted successfully");
			return true;
		} else {
			System.out.println("Failed to delete the file");
			return false;
		}
	}

	public static boolean addContentToFile(String filePath, String Content) {
		try {
			FileWriter myWriter = new FileWriter(filePath);
			myWriter.write(Content);
			myWriter.close();
			System.out.println("Successfully wrote to the file.");
			return true;
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
		return false;
	}
	
	public static void sbcCsvWriter(SBCCSVEntity sbcEntity, String sbcCsvPath) throws IOException {
		CSVWriter csvWriter = new CSVWriter(new FileWriter(sbcCsvPath, true));
		csvWriter.writeNext(sbcEntity.toCSVString().split("@#"));
		csvWriter.close();
	}

	public static void main(String[] args) throws IOException {
//		sbc.print2D();
		System.out.println();
		
		//Chart of stock
		Calendar dob = Calendar.getInstance(getTimeZone("EST"));
		dob.set(1980, 11, 12, 10, 00, 00); // Apple - https://www.astro.com/astro-databank/Apple,_Shares
		IKundali birthChart = new Kundali(KUNDALI_8_KARAKAS,
				new SweObjects(getSwephExp(), new SweJulianDate(dob), GEO_NEW_YORK,
						LAHIRI_CITRAPAKSA).completeBuild());

		//delete old csv file
		deleteFileInPath(SBC_CSV_FILE_PATH);
		String createdFilePath = createFile(SBC_CSV_FILE_PATH);
		addContentToFile(createdFilePath, SBCCSVEntity.SBCCSV_ENTITY_HEADER.toString().replace("[", "").replace("]", "").replace(" ", "").concat("\n"));
		System.out.println(birthChart);

		for (int day = 1; day < 31; day++) {
			// Chart of each day
			Calendar currDay = Calendar.getInstance(getTimeZone("EST"));
			for (int j = 0; j < 2; j++) {
				int time = j==0? 9 : 17;
				currDay.set(2022, 7, day, time, 0, 0);
				IKundali currentDay = new Kundali(KUNDALI_8_KARAKAS,
						new SweObjects(getSwephExp(), new SweJulianDate(currDay), GEO_NEW_YORK, LAHIRI_CITRAPAKSA)
								.completeBuild());


				// Calculate special points based on birth chart
				SBCChakra sbc = new SBCChakra();
				Map<String, String> spclPnt = new HashMap<String, String>();
				spclPnt.put("tithi", birthChart.panchanga().tithi().code());
				spclPnt.put("vaara", birthChart.panchanga().vaara().code());
				spclPnt.put("rasi", birthChart.grahas().chandra().pada().rasi().code());
				spclPnt.put("lagna", birthChart.grahas().lagna().pada().rasi().code());
				if (ABHIJIT_NAKSHATRA_MAPTO_N21P4.equals(birthChart.panchanga().pada().code()))
					spclPnt.put("nakshatra", ABHIJIT_NAKSHATRA);
				else
					spclPnt.put("nakshatra", birthChart.panchanga().pada().naksatra().code());
				sbc.setSpecialPoints(spclPnt);

				// Set all grahas into chakra
				boolean[] grahaVakri = currentDay.sweObjects().retrogrades();
				IGrahaEntity[] grh = currentDay.grahas().all();
				JSONObject grahaJson = new JSONObject();
				for (int i = 0; i < grh.length; i++) {
					IGrahaEntity graha = grh[i];
					Cell naksatraCell = sbc.getCellByCode(graha.pada().naksatra().code());
					if (ABHIJIT_NAKSHATRA_MAPTO_N21P4.equals(graha.pada().code()))
						naksatraCell = sbc.getCellByCode(ABHIJIT_NAKSHATRA);
					String strGraha = graha.entityEnum().code();
					if (grahaVakri[i])
						strGraha = "(" + strGraha + ")";
					naksatraCell.addToGrahasList(strGraha);
					grahaJson.put(strGraha, graha.pada().code());
				}

				// Calculate veda on special intrest points based on birth chart
				sbc.calculateVedaOnSpecialPoints();
		sbc.printSpecialCells();

				// Create Entity for each day
				SBCCSVEntity sbcEntity = new SBCCSVEntity();
				sbcEntity.setDate(format6(currentDay.sweJulianDate()).toString());
				sbcEntity.setTime(toDMS(currentDay.sweJulianDate().timeZone(), true).toString());
				sbcEntity.setGraha_positions(grahaJson.toJSONString());
				JSONObject vedaJson = new JSONObject();
				for (Cell eachSplCell : sbc.getSpecialPointCells()) {
					JSONObject eachCellJson = new JSONObject();
					for (String eachName : eachSplCell.getName().split("@#")) {
						eachCellJson.put("name", eachName);
						eachCellJson.put("code", eachSplCell.getCode());
						eachCellJson.put("veda", eachSplCell.getVedaGraha());
						vedaJson.put(eachName, eachCellJson);
					}
				}
				sbcEntity.setVeda_calculations(vedaJson.toJSONString());

				// Save entity into CSV file for analytics
				sbcCsvWriter(sbcEntity, createdFilePath);
			}
		}
	}

}
