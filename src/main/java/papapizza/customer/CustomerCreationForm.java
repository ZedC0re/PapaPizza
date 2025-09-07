package papapizza.customer;

import lombok.Getter;
import lombok.Setter;
import papapizza.util.MatchTuple;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.util.List;

@Getter @Setter
public class CustomerCreationForm {

	@NotBlank(message = "{CustomerCreationForm.notEmpty.address}")
	private String address;

	@NotBlank(message = "{CustomerCreationForm.notEmpty.phone}")
	//this might not include all possible phone numbers, but its what we go with
	@Pattern(regexp = "(((\\+|00)\\d{2}[- ]?)?(\\d){3,5}[- ]?)?(\\d){6,8}",
			 message = "{CustomerCreationForm.phoneNotValid}")
	private String phone;

	@NotBlank(message = "{CustomerCreationForm.notEmpty.lastname}")
	private String lastname;

	@NotBlank(message = "{CustomerCreationForm.notEmpty.firstname}")
	private String firstname;

	public CustomerCreationForm(String address, String phone, String lastname, String firstname) {
		this.address = address;
		this.phone = phone;
		this.lastname = lastname;
		this.firstname = firstname;
	}

	/**
	 * revert customer to form and assign all values automatically
	 * @param customer (findById)
	 */
	public void setFormDetails(Customer customer){
		this.setPhone(readablePhone(customer.getPhone()));
		this.setAddress(customer.getAddress());
		this.setFirstname(customer.getFirstname());
		this.setLastname(customer.getLastname());
	}

	/**
	 * set form details on customer
	 * @param customer customer to set
	 */
	public void setCustomerDetails(Customer customer){
		customer.setAddress(this.getAddress());
		customer.setPhone(unifiedPhone(this.getPhone()));
		customer.setFirstname(this.getFirstname());
		customer.setLastname(this.getLastname());
	}

	/**
	 * checks if the attributes of the customer equal the current form data
	 * @param customer to check
	 * @return is the same data
	 */
	public boolean isAllSameData(Customer customer){
		//list of customer&form attributes
		List<MatchTuple<String>> formAttributes = List.of(
				new MatchTuple<>(customer.getAddress(), this.getAddress()),
				new MatchTuple<>(customer.getFirstname(), this.getFirstname()),
				new MatchTuple<>(customer.getLastname(), this.getLastname()),
				new MatchTuple<>(customer.getPhone(), unifiedPhone(this.getPhone()))
		);

		//if all attributes of the form match with the customer -> customer needs to be edited
		return formAttributes.stream().filter(MatchTuple::allMatch).count() < formAttributes.size();
	}

	/**
	 * Removes all non numeric characters, leading plus will be substituted by 00 ex.: +49 -> 0049
	 * @param phone phone number as string
	 * @return unified phone number
	 */
	private static String unifiedPhone(String phone){
		return phone.trim().replaceFirst("[+]","00").replaceAll("[^\\d]","");
	}

	/**
	 * Makes unified phone number humanly readable (separated in block)
	 * @param unifiedPhone phone number form db
	 * @return phone number in better readability
	 */
	private static String readablePhone(String unifiedPhone){
		//XXX better readability (not that easy, needs to separate area & country code from rest of number)
		return unifiedPhone;
	}
}