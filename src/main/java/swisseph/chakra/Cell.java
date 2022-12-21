package swisseph.chakra;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class Cell {
	private int i;
	private int j;
	private String ijCode;
	private String code;
	private Boolean point = false;
	private List<String> grahasList = new ArrayList<>();
	private Set<String> vedaGraha = new HashSet<>();
	private String name=StringUtils.EMPTY;

	public Cell(int a, int b, String val) {
		i = a;
		j = b;
		code = val;
		ijCode = (String.valueOf(this.i) + String.valueOf(this.j))+val;
	}

	public String getIjCode() {
		return ijCode;
	}
	

	public Integer getI() {
		return i;
	}

	public Integer getJ() {
		return j;
	}

	public String getCode() {
		return code;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Cell) {
			Cell equalsSample = (Cell) obj;
			if (equalsSample.getIjCode().equals(this.getIjCode())) {
				return true;
			}
		}
		return false;
	}


	public Boolean getPoint() {
		return point;
	}

	public void setPoint(Boolean point) {
		this.point = point;
	}


	public void addVedaGraha(String graha) {
		this.vedaGraha.add(graha);
	}

	public Set<String> getVedaGraha() {
		return vedaGraha;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getGrahasList() {
		return grahasList;
	}

	public void addToGrahasList(String grahaToAdd) {
		this.grahasList.add(grahaToAdd);
	}
}