package papapizza.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

	@Override
	public boolean isValid(String phone, ConstraintValidatorContext constraintValidatorContext) {
		String pattern = "^([\\+]([\\(][0-9]{2}[\\)]|[0-9]{2})|[0-9])[\\s]?[0-9]{3}[\\s]?[0-9]{7,10}$";
		// accepts: +(49) or +49 or 0 country-code, 3 digit tel-code and 7-10 digit phone number
		return phone.matches(pattern);
	}
}
