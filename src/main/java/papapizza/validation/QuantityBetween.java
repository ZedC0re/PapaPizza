package papapizza.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD,ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = QuantityBetweenValidator.class)
public @interface QuantityBetween {
	//error Message
	String message() default "quantity not valid";
	//groups
	Class<?>[] groups() default {};
	//payload
	Class<? extends Payload>[] payload() default {};

	int min();
	int max();
}
