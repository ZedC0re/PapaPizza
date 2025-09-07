package papapizza.validation;


import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class QuantityBetweenValidator implements ConstraintValidator<QuantityBetween, String> {

	private int min, max;

	@Override
	public void initialize(QuantityBetween constraintAnnotation) {
		min = constraintAnnotation.min();
		max = constraintAnnotation.max();
	}

	@Override
	public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
		try {
			int quantity = Integer.parseInt(s);
			return quantity >= min || quantity <= max;
		} catch (NumberFormatException e){
			return false;
		}
	}
}
