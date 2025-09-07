package papapizza.employee;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.salespointframework.catalog.Product;
import org.salespointframework.useraccount.Password;
import org.salespointframework.useraccount.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import papapizza.app.exc.PapaPizzaRunException;
import papapizza.customer.Customer;
import papapizza.customer.CustomerCreationForm;
import papapizza.customer.CustomerManagement;
import papapizza.delivery.DeliveryManagement;
import papapizza.inventory.ProductCategory;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.inventory.items.OvenProduct;
import papapizza.order.DeliveryType;
import papapizza.order.ShopOrder;
import papapizza.order.ShopOrderManagement;
import papapizza.order.ShopOrderState;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EmployeeManagementTest {
	@Autowired
	EmployeeManagement emplMgmt;
	@Autowired
	ShopOrderManagement<ShopOrder> orderMgmt;
	@Autowired
	CustomerManagement sustomerMgmt;
	@Autowired
	DeliveryManagement deliveryMgmt;
	@Autowired
	ShopCatalogManagement catalogMgmt;

	private Employee empl;

	@BeforeAll
	public void initialize() {
		EmployeeCreationForm form = new EmployeeCreationForm("emplMgmtTester", "testFirstname", "testLastname", "test", "test", "Cashier");
		empl = emplMgmt.createEmployee(form);
	}

	@Test
	public void createEmplTest() {
		//successfully created employee
		assertEquals(empl.getUserAccount().getUsername(), "emplMgmtTester");

		//create employee with null
		assertThrows(NullPointerException.class, () -> emplMgmt.createEmployee(null));

		//try to create Employee with name DUMMY_ACCOUNT
		assertEquals(emplMgmt.createEmployee(new EmployeeCreationForm("DUMMY_ACCOUNT", "b", "b", "b", "b", "Cashier")), null);
	}

	@Test
	public void stringToRoleTest() {
		//successful stringToRoles
		assertEquals(emplMgmt.stringToRole("Boss"), EmployeeManagement.BOSS);
		assertEquals(emplMgmt.stringToRole("Cashier"), EmployeeManagement.CASHIER);
		assertEquals(emplMgmt.stringToRole("Chef"), EmployeeManagement.CHEF);
		assertEquals(emplMgmt.stringToRole("Driver"), EmployeeManagement.DRIVER);

		//forced NullPointerException
		assertThrows(NullPointerException.class, () -> emplMgmt.stringToRole(null));

		//nonexistent Role
		assertThrows(IllegalArgumentException.class, () -> emplMgmt.stringToRole("Pirate"));
	}

	@Test
	public void findByIdTest() {
		//employee not there
		assertFalse(emplMgmt.findById(Long.MAX_VALUE).isPresent());

		//successful find
		assertEquals(emplMgmt.findById(empl.getId()).get().getUsername(), empl.getUsername());
	}

	@Test
	public void findByMetaTest() {
		//successfull meta find
		assertFalse(emplMgmt.findByMeta(Employee.Meta.NORMAL).isEmpty());
	}

	@Test
	public void findByRoleTest() {
		//successful find
		assertFalse(emplMgmt.findByRole("Cashier").isEmpty());

		//find null
		assertThrows(NullPointerException.class, () -> emplMgmt.findByRole(null));

		//find role that doesn't exist
		assertTrue(emplMgmt.findByRole("Pirate").isEmpty());
	}

	@Test
	public void findByUsernameTest() {
		//successful find
		assertTrue(emplMgmt.findByUsername("emplMgmtTester").isPresent());

		//unsuccessful find
		assertFalse(emplMgmt.findByUsername("Notexistent").isPresent());

		//find null
		assertFalse(emplMgmt.findByUsername(null).isPresent());
	}

	@Test
	public void findByUserAccountTest() {
		//successful find
		assertEquals(emplMgmt.findByUserAccount(empl.getUserAccount()).get().getUsername(), empl.getUsername());

		//try to find with UserAccount that's not attached to Employee
		assertFalse(emplMgmt.findByUserAccount(emplMgmt.createAccount("Notexistent", Password.UnencryptedPassword.of("123"), EmployeeManagement.CASHIER)).isPresent());

		//find null
		assertThrows(NullPointerException.class, () -> emplMgmt.findByUserAccount(null));
	}

	@Test
	public void deleteByIdTest() {
		//successful delete
		Employee empl2 = emplMgmt.createEmployee(new EmployeeCreationForm("temporaryGuy", "first", "second", "test", "test", "Cashier"));
		assertTrue(emplMgmt.deleteById(empl2.getId()));

		//no Employee found
		assertThrows(RuntimeException.class, () -> emplMgmt.deleteById(87316435));

		//try to delete DELETE_LINK_EMPLOYEE
		assertThrows(RuntimeException.class, () -> emplMgmt.deleteById(emplMgmt.getDeleteLinkEmployee().getId()));
	}

	/* private.
	@Test
	public void substituteOrderWithDeleteEmployee() {
		//successful substitute
		ShopOrder order = orderMgmt.create(empl, sustomerMgmt.createCustomer(new CustomerCreationForm("testAdress", "12321", "testLastname", "testFirstname")));
		emplMgmt.substituteOrderWithDeleteEmployee(order, empl);
		assertEquals(order.getCashier(), emplMgmt.getDeleteLinkEmployee());
	}*/

	@Test
	public void deleteAccountTest() {
		//successful delete
		UserAccount toDelete = emplMgmt.createAccount("otherTemporary", Password.UnencryptedPassword.of("123"), EmployeeManagement.CASHIER);
		assertEquals(emplMgmt.deleteAccount(toDelete), toDelete);

		//try to delete DUMMY_ACCOUNT
		assertThrows(PapaPizzaRunException.class, () -> emplMgmt.deleteAccount(emplMgmt.getDummyAccount()));
	}

	@Test
	public void saveTest() {
		assertThrows(NullPointerException.class, () -> emplMgmt.save(null));
	}

	@Test
	public void substituteOrderWithDeleteEmployeeTest() {
		//initialize order to substitute Employees in
		Employee empl = emplMgmt.createEmployee(new EmployeeCreationForm("substTestCashier",
				"testFirst",
				"testLast",
				"aA1",
				"aA1",
				"Cashier"));
		Employee empl2 = emplMgmt.createEmployee(new EmployeeCreationForm("substTestChef",
				"testFirst",
				"testLast",
				"aA1",
				"aA1",
				"Chef"));
		Customer customer = sustomerMgmt.createCustomer(new CustomerCreationForm("1234",
				"12341234",
				"last",
				"first"));

		ShopOrder ord = orderMgmt.create(empl, customer);
		ord.setShopOrderState(ShopOrderState.OPEN);
		ord.setDeliveryType(DeliveryType.DELIVERY);
		ord.setChefs(List.of(empl2));
		ord.setOpenDuration(Duration.ofMinutes(10));
		ord.setPendingDuration(Duration.ofMinutes(10));
		ord.setReadyDuration(Duration.ofMinutes(10));
		ord.setInDeliverDuration(Duration.ofMinutes(10));
		ord.setTotalDuration(Duration.ofMinutes(40));

		orderMgmt.save(ord);
		deliveryMgmt.assignDriver(ord);
		ord.setShopOrderState(ShopOrderState.INDELIVERY);
		ord.setShopOrderState(ShopOrderState.COMPLETED);
		ord.setTimeCompleted(LocalDateTime.now());

		//test
		//substituteOrderWithDeleteEmployeeTest(ord, empl);
		System.out.println("Cashier: " + ord.getCashier());
		System.out.println("Chef: " + ord.getChefs().get(0));
		System.out.println("Driver: " + ord.getDriver());

		//substitute Cashier
		emplMgmt.substituteOrderWithDeleteEmployee(ord, empl);
		//substitute Chef
		//TODO subsitute chef doesn't function right now, at all
		//emplMgmt.substituteOrderWithDeleteEmployee(ord, empl2);
		//substitute driver
		emplMgmt.substituteOrderWithDeleteEmployee(ord, ord.getDriver());

		assertFalse(ord.getCashier().isMetaNormal());
		assertFalse(ord.getDriver().isMetaNormal());
	}

	@Test
	public void getUnassignedOvenTest() {
		List<OvenProduct> ovens = catalogMgmt.findByCategory(ProductCategory.OVEN.toString()).stream().map(product -> (OvenProduct) product).collect(Collectors.toList());
		List<OvenProduct> unassignedOvens = new ArrayList<>();

		for(OvenProduct oven : ovens) {
			if(oven.getChef() == null) {
				unassignedOvens.add(oven);
			}
		}
		assertEquals(emplMgmt.getUnassignedOvens(), unassignedOvens);
	}

	@Test
	public void assignOvenTest() {
		//creating dummy data
		Employee empl = emplMgmt.createEmployee(new EmployeeCreationForm("assignTestChef",
				"testFirst",
				"testLast",
				"aA1",
				"aA1",
				"Chef"));
		Employee empl2 = emplMgmt.createEmployee(new EmployeeCreationForm("assignTestCashier",
				"testFirst",
				"testLast",
				"aA1",
				"aA1",
				"Chef"));
		OvenProduct newOven = catalogMgmt.createOvenProduct("newOven1","24");
		OvenProduct nextOven = catalogMgmt.createOvenProduct("nextOven1","25");

		//successfully assign oven
		emplMgmt.assignChefToOven(newOven, empl);
		assertEquals(newOven.getChef(), empl);

		//unassign previous oven
		emplMgmt.assignChefToOven(nextOven, empl);
		assertEquals(nextOven.getChef(), empl);

		//Employee not a chef
		assertThrows(IllegalArgumentException.class, () -> emplMgmt.assignChefToOven(newOven, empl2));

		//oven already assigned
		assertThrows(IllegalArgumentException.class, () -> emplMgmt.assignChefToOven(nextOven, empl2));
	}

	@Test
	public void getMyOvenTest() {
		Employee empl = emplMgmt.createEmployee(new EmployeeCreationForm("myOvenTester",
				"testFirst",
				"testLast",
				"aA1",
				"aA1",
				"Chef"));
		OvenProduct newOven = catalogMgmt.createOvenProduct("newOven101","24");

		emplMgmt.assignChefToOven(newOven, empl);

		assertEquals(emplMgmt.getMyOven(empl).get(), newOven);
	}
}
