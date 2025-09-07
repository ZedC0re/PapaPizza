package papapizza.employee;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.salespointframework.useraccount.Role;
import org.salespointframework.useraccount.UserAccount;
import papapizza.app.exc.PapaPizzaRunException;
import papapizza.inventory.items.VehicleProduct;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
public class Employee {
	public enum Meta {
		DELETE_LINK_EMPLOYEE, NORMAL//, DELETED, DISABLED
	}

	@Id
	@GeneratedValue()
	@Column(nullable = false)
	@Setter(AccessLevel.NONE)
	private long id;
	private String firstname;
	private String lastname;
	private String role;

	@OneToOne
	private UserAccount userAccount;

	@Column
	@Enumerated(EnumType.STRING)
	private Meta meta = Meta.NORMAL;

	@OneToOne
	@Setter
	@Getter
	private VehicleProduct vehicle;

	public Employee() {
	}

	public Employee(UserAccount userAccount) {
		this.userAccount = userAccount;
		firstname = userAccount.getFirstname();
		lastname = userAccount.getLastname();
		role = getRole();
	}

	/**
	 * Removes previous role and assigns new Role (from String) to employee
	 *
	 * @param role to switch to
	 */
	public void setRole(@NonNull String role) {
		//remove prior role
		List<Role> toDelete = userAccount.getRoles().toList();
		for (Role roleToDelete : toDelete) {
			userAccount.remove(roleToDelete);
		} //doesn't throw NullPointerException if Roles are empty, whoo

		if (role.equals("Boss")) {
			userAccount.add(EmployeeManagement.BOSS);
		} else if (role.equals("Cashier")) {
			userAccount.add(EmployeeManagement.CASHIER);
		} else if (role.equals("Chef")) {
			userAccount.add(EmployeeManagement.CHEF);
		} else if (role.equals("Driver")) {
			userAccount.add(EmployeeManagement.DRIVER);
		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * @return Stupid role String of employee
	 */
	public String getRole() {

		if (userAccount.getRoles().stream().findAny().isEmpty()) {
			throw new PapaPizzaRunException("getRole called on Employee without Role");
		}
		if (userAccount.getRoles().stream().count() > 1) {
			throw new PapaPizzaRunException("Employee with multiple Roles found");
		}

		Role role = userAccount.getRoles().stream().findAny().get();

		String strRole = "none";
		if (role.equals(EmployeeManagement.BOSS)) {
			strRole = "Boss";
		} else if (role.equals(EmployeeManagement.CHEF)) {
			strRole = "Chef";
		} else if (role.equals(EmployeeManagement.CASHIER)) {
			strRole = "Cashier";
		} else if (role.equals(EmployeeManagement.DRIVER)) {
			strRole = "Driver";
		}
		return strRole;
	}

	public boolean isMetaNormal() {
		return this.meta == Meta.NORMAL;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return "Employee{" +
				"id=" + id +
				", firstname='" + firstname + '\'' +
				", lastname='" + lastname + '\'' +
				", role='" + role + '\'' +
				", userAccount=" + userAccount +
				", meta=" + meta +
				'}';
	}

	public String getUsername() {
		return userAccount.getUsername();
	}
}
