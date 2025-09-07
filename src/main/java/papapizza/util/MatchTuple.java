package papapizza.util;

import java.util.ArrayList;
import java.util.Arrays;

public class MatchTuple<T> extends ArrayList<T>{

	public MatchTuple(T ... values) {
		this.addAll(Arrays.asList(values));
	}

	/**
	 * returns true if all values in the tuple match via their equals() method
	 * @return all values are equal boolean
	 */
	public boolean allMatch(){
		return stream().distinct().limit(2).count() <= 1;
	}
}
