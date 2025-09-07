package papapizza.analytics;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


import org.salespointframework.catalog.Product;
import org.salespointframework.quantity.Quantity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import papapizza.customer.Customer;
import papapizza.customer.CustomerCreationForm;
import papapizza.customer.CustomerManagement;
import papapizza.delivery.DeliveryManagement;
import papapizza.employee.Employee;
import papapizza.employee.EmployeeCreationForm;
import papapizza.employee.EmployeeManagement;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.order.DeliveryType;
import papapizza.order.ShopOrder;
import papapizza.order.ShopOrderManagement;
import papapizza.order.ShopOrderState;

import javax.transaction.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AnalyticsControllerTest {
	@Autowired
	private MockMvc mvc;
	@Autowired
	private AnalyticsManagement analytics;
	@Autowired
	private ShopOrderManagement<ShopOrder> orderMgmt;
	@Autowired
	private EmployeeManagement emplMgmt;
	@Autowired
	private CustomerManagement customerMgmt;
	@Autowired
	private DeliveryManagement deliveryMgmt;
	@Autowired
	private ShopCatalogManagement invMgmt;

	@Test
	@WithMockUser(roles="BOSS")
	public void weeklyTest() throws Exception {
		mvc.perform(get("/analytics"))
				.andDo(print())
				.andExpect(status().isFound());
	}

	@Test
	@WithMockUser(roles="BOSS")
	public void monthlyTest() throws Exception {
		mvc.perform(get("/analytics/m"))
				.andDo(print())
				.andExpect(status().isFound());
	}

	@Test
	@WithMockUser(roles="BOSS")
	public void yearlyTest() throws Exception {
		mvc.perform(get("/analytics/y"))
				.andDo(print())
				.andExpect(status().isFound());
	}

	@Test
	@Disabled
	@WithMockUser(roles="BOSS")
	public void analyticsPageTest() throws Exception {
		System.setProperty("java.awt.headless", "false");

		mvc.perform(get("/analytics/2022-1-2"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("textCase",
						"date",
						"completedOrders",
						"returnOrders",
						"cancelledOrders",
						"numberOfCompletedOrders",
						"numberOfReturnOrders",
						"numberOfCancelledOrders",
						"sales"));
	}

	@Test
	@WithMockUser(roles="BOSS")
	public void orderDetailsTest() throws Exception {
		Employee empl = emplMgmt.createEmployee(new EmployeeCreationForm("analTestEmpl",
				"testFirst",
				"testLast",
				"aA1",
				"aA1",
				"Cashier"));
		Customer customer = customerMgmt.createCustomer(new CustomerCreationForm("1234",
				"12341234",
				"last",
				"first"));
		Product CustomPizza = invMgmt.createPizzaProduct(List.of(invMgmt.createToppingProduct("Ginger","5")));

		ShopOrder ord = orderMgmt.create(empl, customer);
		ord.setShopOrderState(ShopOrderState.OPEN);
		ord.setDeliveryType(DeliveryType.DELIVERY);
		ord.setOpenDuration(Duration.ofMinutes(10));
		ord.setPendingDuration(Duration.ofMinutes(10));
		ord.setReadyDuration(Duration.ofMinutes(10));
		ord.setInDeliverDuration(Duration.ofMinutes(10));
		ord.setTotalDuration(Duration.ofMinutes(40));
		ord.addOrderLine(CustomPizza, Quantity.of(2));

		orderMgmt.save(ord);
		deliveryMgmt.assignDriver(ord);
		ord.setShopOrderState(ShopOrderState.INDELIVERY);
		ord.setShopOrderState(ShopOrderState.COMPLETED);
		ord.setTimeCompleted(LocalDateTime.now());

		System.out.println(ord.getId());

		//successfully find order
		mvc.perform(get("/analytics/detail/" + ord.getId()))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("order"));

		//order doesn't exist
		mvc.perform(get("/analytics/detail/" + 1423))
				.andDo(print())
				.andExpect(status().isNotFound());
	}
}
