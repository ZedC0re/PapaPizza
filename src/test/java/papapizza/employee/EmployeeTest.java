package papapizza.employee;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EmployeeTest {
	@Autowired
	private EmployeeManagement emplMgmt;
	private Employee empl;

	@BeforeAll
	public void initialize() {
		EmployeeCreationForm form = new EmployeeCreationForm("emplTester", "testFirstName", "testLastName", "test", "test", "Cashier");
		empl = emplMgmt.createEmployee(form);
	}

	@Test
	public void getterTest() {
		//why tf muss ich das für die süßen % machen
		assertEquals(empl.getFirstname(), "testFirstName");
		assertEquals(empl.getLastname(), "testLastName");
	}

	@Test
	public void setRoleTest() {
		//successful role change
		empl.setRole("Driver");
		assertTrue(empl.getRole().equals("Driver"));
		empl.setRole("Cashier");

		//set role that doesn't exist
		assertThrows(IllegalArgumentException.class, () -> empl.setRole("Pirate"));

		//null case
		assertThrows(NullPointerException.class, () -> empl.setRole(null));
	}

	@Test
	public void getRoleTest() {

		//successful role get
		empl.setRole("Cashier");
		assertEquals(empl.getRole(), "Cashier");

		//UserAccount without role case
		empl.getUserAccount().remove(EmployeeManagement.CASHIER);
		assertThrows(RuntimeException.class, () -> empl.getRole());
		empl.setRole("Cashier");

		//UserAccount with multiple roles
		empl.getUserAccount().add(EmployeeManagement.BOSS);
		empl.getUserAccount().add(EmployeeManagement.CASHIER);
		assertThrows(RuntimeException.class, () -> empl.getRole());
		empl.getUserAccount().remove(EmployeeManagement.BOSS);
	}

	@Test
	public void isMetaNormalTest() {
		//successful meta check
		assertTrue(empl.isMetaNormal());

		//meta check on DELETE_LINK_EMPLOYEE
		assertFalse(emplMgmt.getDeleteLinkEmployee().isMetaNormal());
	}
}
