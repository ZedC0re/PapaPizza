package papapizza.employee;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter @Setter
public class EmployeeCreationForm {

	@NotBlank(message = "{emplCreationForm.notEmpty.name}")
	private String name;

	@NotBlank(message = "{emplCreationForm.notEmpty.firstname}")
	private String firstname;

	@NotBlank(message = "{emplCreationForm.notEmpty.lastname}")
	private String lastname;

	private String password;

	private String repeatedPassword;

	@NotBlank(message = "{emplCreationForm.notEmpty.role}")
	private String role;

	public EmployeeCreationForm(String name,
								String firstname,
								String lastname,
								String password,
								String repeatedPassword,
								String role) {
		this.name = name;
		this.firstname = firstname;
		this.lastname = lastname;
		this.password = password;
		this.repeatedPassword = repeatedPassword;
		this.role = role;
	}

	public void setFormDetails(@NonNull Employee employee) {
		this.setName(employee.getUsername());
		this.setFirstname(employee.getFirstname());
		this.setLastname(employee.getLastname());
		this.setRole(employee.getRole());
	}

	public void setEmployeeDetails(@NonNull Employee employee) {
		employee.setLastname(this.getLastname());
		employee.setFirstname(this.getFirstname());
		employee.setRole(this.getRole());
	}
}
