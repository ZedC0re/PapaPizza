package papapizza.order;

import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.salespointframework.catalog.Product;
import org.salespointframework.order.OrderStatus;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.time.Interval;
import org.salespointframework.useraccount.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import papapizza.customer.Customer;
import papapizza.customer.CustomerManagement;
import papapizza.employee.Employee;
import papapizza.employee.EmployeeCreationForm;
import papapizza.employee.EmployeeManagement;
import papapizza.inventory.ShopCatalogManagement;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ShopOrderManagementTest {

	@Autowired private CustomerManagement customerManagement;
	@Autowired private EmployeeManagement employeeManagement;
	@Autowired private ShopOrderManagement<ShopOrder> shopOrderManagement;
	@Autowired private ShopCatalogManagement catalogManagement;

	private ShopOrder order1, order2;
	private Employee cashier;
	private Employee chef;
	private Employee driver;
	private Product product;

	@BeforeEach
	void setUp(){

		//before start
		EmployeeCreationForm ecfCashier = new EmployeeCreationForm("testname1","first1","last1","123","123","Cashier");
		EmployeeCreationForm ecfChef = new EmployeeCreationForm("testname2","first2","last2","123","123","Chef");
		EmployeeCreationForm ecfDriver = new EmployeeCreationForm("testname3","first3","last3","123","123","Driver");
		cashier = employeeManagement.createEmployee(ecfCashier);
		chef = employeeManagement.createEmployee(ecfChef);
		driver = employeeManagement.createEmployee(ecfDriver);
		order1 = shopOrderManagement.create(cashier,customerManagement.getDeleteLinkCustomer());
		order1.setDriver(driver);
		order1.setChefs(Collections.singletonList(chef));
		order2 = shopOrderManagement.create(cashier,customerManagement.getDeleteLinkCustomer());
		shopOrderManagement.save(order1);
		product = catalogManagement.createToppingProduct("name", "0");
		//before end

	}

	@Test
	public void containsReturnsCorrectBoolean(){

		ShopOrder order = shopOrderManagement.create(cashier,customerManagement.getDeleteLinkCustomer());

		assertFalse(shopOrderManagement.contains(order.getId()));

		shopOrderManagement.save(order);

		assertTrue(shopOrderManagement.contains(order.getId()));




	}

	@Test
	public void getReturnsEmptyAfterDelete(){

		assertTrue(shopOrderManagement.get(order1.getId()).isPresent());

		shopOrderManagement.delete(order1);

		assertFalse(shopOrderManagement.get(order1.getId()).isPresent());

	}

	@Test
	public void findByShopOrderIdReturnsSameOptionalAsGet(){

		assertTrue(shopOrderManagement.get(order2.getId()).isEmpty() &&
				shopOrderManagement.findByShopOrderId(Objects.requireNonNull(order2.getId()).getIdentifier()).isEmpty());
		assertTrue(shopOrderManagement.get(order1.getId()).isPresent() &&
				shopOrderManagement.findByShopOrderId(Objects.requireNonNull(order1.getId()).getIdentifier()).isPresent());
		assertEquals(Objects.requireNonNull(shopOrderManagement.get(order1.getId()).get().getId()).getIdentifier(),
				Objects.requireNonNull(shopOrderManagement.findByShopOrderId(Objects.requireNonNull(order1.getId()).getIdentifier()).get().getId()).getIdentifier());
	}

	@Test
	public void findByRequiresNonNull(){

		assertThrows(NullPointerException.class, () -> shopOrderManagement.findBy((ShopOrderState) null));
		assertThrows(NullPointerException.class, () -> shopOrderManagement.findBy((Interval) null));
		assertThrows(NullPointerException.class, () -> shopOrderManagement.findBy((OrderStatus) null));
		assertThrows(NullPointerException.class, () -> shopOrderManagement.findBy((UserAccount) null));
		assertThrows(NullPointerException.class, () -> shopOrderManagement.findBy(null, null));
		assertThrows(NullPointerException.class, () -> shopOrderManagement.findBy((Customer) null));
		assertThrows(NullPointerException.class, () -> shopOrderManagement.findBy((Employee) null));
		assertThrows(NullPointerException.class, () -> shopOrderManagement.findByShopOrderId(null));
	}

	@Test
	public void findByReturnsNonNull(){
		assertNotEquals(null, shopOrderManagement.findBy(ShopOrderState.OPEN));
		assertNotEquals(null, shopOrderManagement.findBy(Interval.from(LocalDateTime.now()).to(LocalDateTime.now())));
		assertNotEquals(null, shopOrderManagement.findBy(OrderStatus.OPEN));
		assertNotEquals(null, shopOrderManagement.findBy(employeeManagement.getDummyAccount()));
		assertNotEquals(null, shopOrderManagement.findBy(employeeManagement.getDummyAccount(), Interval.from(LocalDateTime.now()).to(LocalDateTime.now())));
		assertNotEquals(null, shopOrderManagement.findBy(customerManagement.getDeleteLinkCustomer()));
		assertNotEquals(null, shopOrderManagement.findBy(employeeManagement.getDeleteLinkEmployee()));
		assertNotEquals(null, shopOrderManagement.findByShopOrderId("id"));
	}

	/*@Test
	public void cloneOrderCreatesDifferentEntity(){

		ShopOrder order3 = shopOrderManagement.cloneOrder(order1);
		assertNotEquals(order1.getId().getIdentifier(), order3.getId().getIdentifier());
	}*/

	@Test
	@Disabled
	public void setShopOrderStateThrowsOnShopOrderDoesNotExist(){

		assertThrows(ShopOrderDoesNotExistExeption.class, () -> shopOrderManagement.setShopOrderState(order2, ShopOrderState.OPEN));
	}

	@Test
	public void setShopOrderDeliveryTypeThrowsOnShopOrderDoesNotExist(){

		assertThrows(ShopOrderDoesNotExistExeption.class, () -> shopOrderManagement.setShopOrderDeliveryType(order2, DeliveryType.DELIVERY));
	}

	//FIXME UnsupportedOperationException()
	@Disabled
	@Test
	public void setProductAndQuantityAsLinesAddsAndRemovesOrderLines(){

		shopOrderManagement.setProductAndQuantityAsLines(order1.getId(), product, Quantity.of(1));
		assertTrue(order1.getOrderLines(product).stream().findAny().isPresent());

		shopOrderManagement.setProductAndQuantityAsLines(order1.getId(), product, Quantity.of(0));
		assertTrue(order1.getOrderLines(product).stream().findAny().isEmpty());
	}

	@Disabled
	@Test
	public void addLinesByTotalAddsChargeLines(){

		shopOrderManagement.addLinesByTotal(order1.getId(), Money.of(0,"EUR"), "description");
		assertTrue(order1.getChargeLines().stream().anyMatch(chargeLine -> chargeLine.getDescription().equals("description")));
	}
}
