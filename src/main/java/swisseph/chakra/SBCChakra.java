package swisseph.chakra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jyotisa.naksatra.ENaksatra;
import org.jyotisa.rasi.ERasi;
import org.jyotisa.tithi.ETithi;
import org.jyotisa.vaara.EVaara;

import com.google.common.collect.ImmutableMap;

public class SBCChakra {
	
	private Set<String> visited = new HashSet<>();

private static final Map<Integer, List<String>> SBC_CHART = ImmutableMap.<Integer, List<String>>builder()
	.put(0, Arrays.asList("a", ENaksatra.KRITTIKA.code(), ENaksatra.ROHINI.code(), ENaksatra.MRIGASHIRA.code(), ENaksatra.ARDRA.code(), ENaksatra.PUNARVASU.code(), ENaksatra.PUSHYA.code(), ENaksatra.ASHLESHA.code(), "aa"))
	.put(1, Arrays.asList(ENaksatra.BHARANI.code(), "u", "a", "v", "k", "h", "d", "uu", ENaksatra.MAGHA.code()))
	.put(2, Arrays.asList(ENaksatra.ASHWINI.code(), "l", "lu", ERasi.VRISHABHA.code(), ERasi.MITHUNA.code(), ERasi.KARKATA.code(), "luu", "m", ENaksatra.PURVA_PHALGUNI.code()))
	.put(3, Arrays.asList(ENaksatra.REVATI.code(), "ch", ERasi.MESHA.code(), "o", "nanda", "au", ERasi.SIMHA.code(), "t~", ENaksatra.UTTARA_PHALGUNI.code()))
	.put(4, Arrays.asList(ENaksatra.UTTARA_BHADRAPADA.code(), "d", ERasi.MEENA.code(), "rikta", "poorna", "bhadra", ERasi.KANYA.code(), "p", ENaksatra.HASTA.code()))
	.put(5, Arrays.asList(ENaksatra.PURVA_BHADRAPADA.code(), "s", ERasi.KUMBHA.code(), "ah", "jaya", "am", ERasi.TULA.code(), "r", ENaksatra.CHITRA.code()))
	.put(6, Arrays.asList(ENaksatra.SHATABHISHA.code(), "g", "ai", ERasi.MAKARA.code(), ERasi.DHANUS.code(), ERasi.VRISCHIKA.code(), "e", "t", ENaksatra.SWATI.code()))
	.put(7, Arrays.asList(ENaksatra.DHANISHTA.code(), "rii", "kh", "j", "bh", "y", "n", "ri", ENaksatra.VISHAKHA.code()))
	.put(8, Arrays.asList("ii", ENaksatra.SHRAVANA.code(), SBCCalculate.ABHIJIT_NAKSHATRA, ENaksatra.UTTARA_ASHADHA.code(), ENaksatra.PURVA_ASHADHA.code(), ENaksatra.MULA.code(), ENaksatra.JYESHTHA.code(), ENaksatra.ANURADHA.code(), "i"))
	.build();


private static final Map<String, List<String>> TITHI_MAP = ImmutableMap.<String, List<String>>builder()
	.put("nanda", Arrays.asList(ETithi.SHUKLA_PRATIPADA.code(), ETithi.KRISHNA_PRATIPADA.code(), ETithi.SHUKLA_SHASHTHI.code(), ETithi.KRISHNA_SHASHTHI.code(),ETithi.SHUKLA_EKADASI.code(), ETithi.KRISHNA_EKADASI.code()))
	.put("bhadra", Arrays.asList(ETithi.SHUKLA_DWITIYA.code(), ETithi.KRISHNA_DWITIYA.code(), ETithi.SHUKLA_SAPTAMI.code(), ETithi.KRISHNA_SAPTAMI.code(),ETithi.SHUKLA_DWADASI.code(), ETithi.KRISHNA_DWADASI.code()))
	.put("jaya", Arrays.asList(ETithi.SHUKLA_TRITIYA.code(), ETithi.KRISHNA_TRITIYA.code(), ETithi.SHUKLA_ASHTAMI.code(), ETithi.KRISHNA_ASHTAMI.code(),ETithi.SHUKLA_TRAYODASI.code(), ETithi.KRISHNA_TRAYODASI.code()))
	.put("rikta", Arrays.asList(ETithi.SHUKLA_CHATURTHI.code(), ETithi.KRISHNA_CHATURTHI.code(), ETithi.SHUKLA_NAVAMI.code(), ETithi.KRISHNA_NAVAMI.code(),ETithi.SHUKLA_CHATURDASI.code(), ETithi.KRISHNA_CHATURDASI.code()))
	.put("poorna", Arrays.asList(ETithi.SHUKLA_PANCHAMI.code(), ETithi.KRISHNA_PANCHAMI.code(), ETithi.SHUKLA_DASHAMI.code(), ETithi.KRISHNA_DASHAMI.code(),ETithi.SHUKLA_POORNIMA.code(), ETithi.KRISHNA_AMAVASYA.code()))
	.build();
    

private static final Map<String, List<String>> VAARA_MAP = ImmutableMap.<String, List<String>>builder()
	.put("nanda", Arrays.asList(EVaara.SURYA_VAARA.code(), EVaara.MANGALA_VAARA.code()))
	.put("bhadra", Arrays.asList(EVaara.CHANDRA_VAARA.code(), EVaara.BUDHA_VAARA.code()))
	.put("jaya", Arrays.asList(EVaara.GURU_VAARA.code()))
	.put("rikta", Arrays.asList(EVaara.SHUKRA_VAARA.code()))
	.put("poorna", Arrays.asList(EVaara.SHANI_VAARA.code()))
	.build();
    
    private Cell[][] chart = new Cell[9][9];
    private List<Cell> specialPointCells = new ArrayList<>();
	public SBCChakra() {
		fillChartCells();
	}
	
//	for(String ids : TimeZone.getAvailableIDs()) {
//	System.out.println(ids);
//}


    private void fillChartCells() {
    	for(int i=0; i<9; i++) {
    		for(int j=0; j<9; j++) {
    			chart[i][j] = new Cell(i, j, SBC_CHART.get(i).get(j));
    		}
    	}
    }
    
	public void print2D(){
		for (Cell[] row : chart) {
			System.out.println("");
			for (Cell x : row)
				if (x == null)
					System.out.print("NA ");
				else
					System.out.print(x.getCode() + " ");
		}
	}
    
	public Cell getCellByCode(String code) {
		for(int i=0; i<9; i++) {
			for(int j=0; j<9; j++) {
				if(chart[i][j].getCode().equals(code)){
					return chart[i][j];
				}
			}
		}
		return null;
	}

	public void setSpecialPoints(Map<String, String> points) {
		for (String pointKey : points.keySet()) {
			Cell data = null;
			if (pointKey.equals("tithi")) {
				for (String tithiKey : TITHI_MAP.keySet()) {
					if (TITHI_MAP.get(tithiKey).contains(points.get(pointKey))) {
						data = getCellByCode(tithiKey);
						break;
					}
				}
			} else if (pointKey.equals("vaara")) {
				for (String vaaraKey : VAARA_MAP.keySet()) {
					if (VAARA_MAP.get(vaaraKey).contains(points.get(pointKey))) {
						data = getCellByCode(vaaraKey);
						break;
					}
				}
			} else {
				data = getCellByCode(points.get(pointKey));
			}
			data.setPoint(true);
			if(data.getName().length()>0)
				data.setName(data.getName()+"@#"+pointKey);
			else
				data.setName(pointKey);
			specialPointCells.add(data);
		}
	}
	

	public List<Cell> getSpecialPointCells() {
		return specialPointCells;
	}

	public void printSpecialCells() {
		for(Cell eachSplCell : specialPointCells) {
			System.out.println(eachSplCell.getName() +" -- "+ eachSplCell.getCode() +" - "+ eachSplCell.getVedaGraha());
		}
	}

	public void calculateVedaOnSpecialPoints() {
		for(Cell eachSplCell : specialPointCells) {
			visited = new HashSet<>();
			getVedaLoop(eachSplCell, eachSplCell.getI(), eachSplCell.getJ(), 1);
		}
	}


	private void getVedaLoop(Cell eachSplCell, int i, int j, int k) {
		if(visited.contains(i+""+j))
			return;
		else
			visited.add(i+""+j);
		if (i < 0 || i > chart.length - 1 || j < 0 || j > chart[i].length - 1) {
			return;
		}
		if (i == 0 || i == chart.length - 1 || j == 0 || j == chart[i].length - 1) {
//			System.out.println("("+eachSplCell.getI() + ","+ eachSplCell.getJ()+") ->"+ i +" : "+ j + ", "+k);
			Cell borderCell = chart[i][j];
			if(borderCell.getGrahasList().size()>0) {
				eachSplCell.addVedaGraha(borderCell.getGrahasList().toString());
			}
			return;
		}
		i=eachSplCell.getI();
		j=eachSplCell.getJ();
		getVedaLoop(eachSplCell, i + k, j, k + 1);
		getVedaLoop(eachSplCell, i - k, j, k + 1);
		getVedaLoop(eachSplCell, i, j + k, k + 1);
		getVedaLoop(eachSplCell, i, j - k, k + 1);
		getVedaLoop(eachSplCell, i + k, j + k, k + 1);
		getVedaLoop(eachSplCell, i + k, j - k, k + 1);
		getVedaLoop(eachSplCell, i - k, j + k, k + 1);
		getVedaLoop(eachSplCell, i - k, j - k, k + 1);
	}
	
}
