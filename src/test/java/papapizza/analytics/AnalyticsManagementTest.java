package papapizza.analytics;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import papapizza.customer.Customer;
import papapizza.customer.CustomerCreationForm;
import papapizza.customer.CustomerManagement;
import papapizza.employee.Employee;
import papapizza.employee.EmployeeCreationForm;
import papapizza.employee.EmployeeManagement;
import papapizza.order.ShopOrder;
import papapizza.order.ShopOrderManagement;
import papapizza.order.ShopOrderState;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AnalyticsManagementTest {
	@Autowired
	AnalyticsManagement analMgmt;
	@Autowired
	ShopOrderManagement<ShopOrder> orderMgmt;
	@Autowired
	EmployeeManagement emplMgmt;
	@Autowired
	CustomerManagement sustomerMgmt;

	private ShopOrder order;

	@BeforeAll
	public void initialize() {
		Employee empl = emplMgmt.createEmployee(new EmployeeCreationForm(
				"analTester", "testFirst", "testLast", "a", "a", "Cashier"));
		Customer sustomer = sustomerMgmt.createCustomer(new CustomerCreationForm(
				"Spacerock", "12342314", "Oster", "Imp"));

		order = orderMgmt.create(empl, sustomer);
	}
}
