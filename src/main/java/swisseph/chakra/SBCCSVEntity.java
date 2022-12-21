package swisseph.chakra;

import java.util.List;

import com.opencsv.bean.CsvBindByName;

public class SBCCSVEntity {
//	public static final List<String> SBCCSV_ENTITY_HEADER = List.of("date", "time", "graha_positions", "rasi", "rasi_veda"
//			, "lagna", "lagna_veda", "tithi", "tithi_veda", "nakshatra", "nakshatra_veda", "vaara", "vaara_veda", "swara",
//			"swara_veda", "vyanjana", "vyanjana_veda");
	public static final List<String> SBCCSV_ENTITY_HEADER = List.of("date", "time", "graha_positions", "veda_calculations");

	@CsvBindByName(column = "date")
    private String date;
    @CsvBindByName(column = "time")
    private String time;
    @CsvBindByName(column = "graha_positions")
    private String graha_positions;
    @CsvBindByName(column = "veda_calculations")
    private String veda_calculations;
//    @CsvBindByName(column = "rasi")
//    private String rasi;
//    @CsvBindByName(column = "rasi_veda")
//    private String rasi_veda;
//    @CsvBindByName(column = "lagna")
//    private String lagna;
//    @CsvBindByName(column = "lagna_veda")
//    private String lagna_veda;
//    @CsvBindByName(column = "tithi")
//    private String tithi;
//    @CsvBindByName(column = "tithi_veda")
//    private String tithi_veda;
//    @CsvBindByName(column = "nakshatra")
//    private String nakshatra;
//    @CsvBindByName(column = "nakshatra_veda")
//    private String nakshatra_veda;
//    @CsvBindByName(column = "vaara")
//    private String vaara;
//    @CsvBindByName(column = "vaara_veda")
//    private String vaara_veda;
//    @CsvBindByName(column = "swara")
//    private String swara;
//    @CsvBindByName(column = "swara_veda")
//    private String swara_veda;
//    @CsvBindByName(column = "vyanjana")
//    private String vyanjana;
//    @CsvBindByName(column = "vyanjana_veda")
//    private String vyanjana_veda;
	
    
	public String toCSVString() {
		return date+"@#"+time+"@#"+graha_positions+"@#"+veda_calculations;
	}


	public String getDate() {
		return date;
	}


	public void setDate(String date) {
		this.date = date;
	}


	public String getTime() {
		return time;
	}


	public void setTime(String time) {
		this.time = time;
	}


	public String getGraha_positions() {
		return graha_positions;
	}


	public void setGraha_positions(String graha_positions) {
		this.graha_positions = graha_positions;
	}


	public String getVeda_calculations() {
		return veda_calculations;
	}


	public void setVeda_calculations(String veda_calculations) {
		this.veda_calculations = veda_calculations;
	}


//	public String toCSVString() {
//		return date+"#$"+time+"#$"+graha_positions+"#$"+rasi+"#$"+rasi_veda+"#$"+lagna+"#$"+lagna_veda+"#$"+tithi+"#$"+tithi_veda+"#$"+nakshatra+"#$"+nakshatra_veda+"#$"+vaara+"#$"+vaara_veda+"#$"+swara+"#$"+swara_veda+"#$"+vyanjana+"#$"+vyanjana_veda;
//	}
    

}
