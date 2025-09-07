package papapizza.employee;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EmployeeCreationFormTest {
	@Autowired
	private EmployeeManagement emplMgmt;
	private Employee empl;
	private EmployeeCreationForm form;

	@BeforeAll
	public void initialize() {
		form = new EmployeeCreationForm("emplFormTester", "testFirstname", "testLastname", "test",  "test", "Cashier");
		empl = emplMgmt.createEmployee(form);
	}

	@Test
	public void setFormDetailsTest() {
		//successful form set
		EmployeeCreationForm form2 = new EmployeeCreationForm("newName", "newFirstname", "newLastname", "test", "test", "Driver");
		Employee empl2 = emplMgmt.createEmployee(form2);
		form.setFormDetails(empl2);

		assertEquals(form.getName(), "newName");
		assertEquals(form.getFirstname(), "newFirstname");
		assertEquals(form.getLastname(), "newLastname");
		assertEquals(form.getRole(), "Driver");

		form.setFormDetails(empl);

		//form set null case
		assertThrows(NullPointerException.class, () -> form.setFormDetails(null));
	}

	@Test
	public void setEmployeeDetailsTest() {
		//successful employee details set
		EmployeeCreationForm form2 = new EmployeeCreationForm("newName", "Test", "Ickle", "test", "test", "Driver");
		form2.setEmployeeDetails(empl);

		assertEquals(empl.getFirstname(), "Test");
		assertEquals(empl.getLastname(), "Ickle");
		assertEquals(empl.getRole(), "Driver");

		form.setEmployeeDetails(empl);

		//employee details set null case
		assertThrows(NullPointerException.class, () -> form.setEmployeeDetails(null));
	}
}
