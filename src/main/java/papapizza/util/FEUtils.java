package papapizza.util;

import org.springframework.stereotype.Component;

//front end utils
@Component("FEUtils")
public class FEUtils {

	//custom spel to use in thymeleaf via @
	//splits seconds into days, hours, minutes & seconds
	public static String splitUnitsTimeFormatted(int secs){
		int d = secs / 86400;
		secs %= 86400;
		int h = secs / 3600;
		secs %= 3600;
		int m = secs / 60;
		secs %= 60;

		return String.format("%dd %dh %dm %ds",d,h,m,secs);
	}
}
