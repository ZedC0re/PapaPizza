package papapizza.customer;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SearchParamForm {
	private String sortBy = "";
	private String sortDir = "";
	private String filterSearch;
	private List<String> filterBys = new ArrayList<>();

	public boolean isEmpty(){
		return sortBy.equals("") && sortDir.equals("") && filterSearch == null && filterBys.isEmpty();
	}
}
