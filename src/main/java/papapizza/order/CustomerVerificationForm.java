package papapizza.order;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import papapizza.validation.ValidPhone;
import papapizza.validation.ValidTan;

@Getter @Setter
public class CustomerVerificationForm {

	@NonNull
	@ValidPhone
	private String phone;

	@NonNull
	@ValidTan
	private String tan;
}
