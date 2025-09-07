package papapizza.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class TanValidator implements ConstraintValidator<ValidTan, String> {

	@Override
	public boolean isValid(String tan, ConstraintValidatorContext constraintValidatorContext) {
		String pattern = "[0-9][0-9][0-9][0-9][0-9][0-9]";
		return tan.matches(pattern);
	}
}
