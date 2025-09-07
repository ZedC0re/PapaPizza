package papapizza.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class PizzaStatics {
	public static Date localDateTimeToDate(LocalDateTime ldt){
		return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
	}
}
