package papapizza.order;

import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.salespointframework.catalog.Product;
import org.salespointframework.order.OrderLine;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.web.LoggedIn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import papapizza.customer.Customer;
import papapizza.customer.CustomerCreationForm;
import papapizza.customer.CustomerManagement;
import papapizza.employee.Employee;
import papapizza.employee.EmployeeCreationForm;
import papapizza.employee.EmployeeManagement;
import papapizza.inventory.ShopCatalogManagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.*;
import static org.hamcrest.core.IsNull.*;


import javax.transaction.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ShopOrderControllerTest {

	@Autowired
	private MockMvc mvc;
	@Autowired
	private ShopOrderManagement<ShopOrder> shopOrderManagement;
	@Autowired
	private EmployeeManagement employeeManagement;
	@Autowired
	private CustomerManagement customerManagement;
	@Autowired
	private ShopCatalogManagement catalogManagement;

	@Test
	@WithMockUser(roles="CASHIER")
	void getOrder() throws Exception{

		Integer orderCount = (int) shopOrderManagement.findAll().stream()
				.filter(order -> order.getCustomer().getMeta() == Customer.Meta.NORMAL).count();

		mvc.perform(get("/order"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("orders", IsCollectionWithSize.hasSize(orderCount)));
	}

	@Test
	@WithMockUser(roles="CASHIER")
	void getOrderDetails()throws Exception {

		ShopOrder shopOrder = shopOrderManagement.save(
				shopOrderManagement.create(employeeManagement.getDeleteLinkEmployee(), customerManagement.getDeleteLinkCustomer()));
		String orderId = shopOrder.getId().getIdentifier();

		assertTrue(shopOrderManagement.findByShopOrderId(orderId).isPresent());

		mvc.perform(get("/order/details/"+orderId))
				.andExpect(status().isOk())
				.andExpect(model().attribute("order", allOf(
						hasProperty("orderId", is(shopOrder.getId().getIdentifier())),
						hasProperty("phone", is(shopOrder.getCustomer().getPhone())))));

		mvc.perform(get("/order/details/"+"notAnId"))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(roles="CASHIER")
	void postCancelOrder()throws Exception {

		Employee employee = employeeManagement.createEmployee(
				new EmployeeCreationForm("Papaghibli","Hayao","Miyazaki","123","123","Cashier"));
		Customer customer = customerManagement.createCustomer(
				new CustomerCreationForm("IudexGundyr","1345731453274","Gundyr","Iudex"));

		ShopOrder shopOrder = shopOrderManagement.save(
				shopOrderManagement.create(employee, customer));
		String orderId = shopOrder.getId().getIdentifier();

		assertTrue(shopOrderManagement.findByShopOrderId(orderId).isPresent());

		mvc.perform(post("/order/cancel/"+orderId))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/order"));

		mvc.perform(post("/order/cancel/"+"notAnId"))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(roles="CASHIER")
	void postCompleteOrder() throws Exception {

		CustomerCreationForm ccf = new CustomerCreationForm("U.A. Highschool","1234324335352","Recovery","Girl");
		Customer customer = customerManagement.createCustomer(ccf);

		ShopOrder shopOrder = shopOrderManagement.save(
				shopOrderManagement.create(employeeManagement.getDeleteLinkEmployee(), customer));
		shopOrder.setDeliveryType(DeliveryType.PICKUP);
		shopOrderManagement.setShopOrderState(shopOrder, ShopOrderState.READYPICKUP);
		shopOrderManagement.save(shopOrder);
		String orderId = shopOrder.getId().getIdentifier();

		assertTrue(shopOrderManagement.findByShopOrderId(orderId).isPresent());

		mvc.perform(post("/order/complete/"+orderId))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/order"));

		mvc.perform(post("/order/complete/"+"notAnId"))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(roles="CASHIER")
	void getCustomerVerification() throws Exception {
		ShopOrder shopOrder = shopOrderManagement.save(
				shopOrderManagement.create(employeeManagement.getDeleteLinkEmployee(), customerManagement.getDeleteLinkCustomer()));
		String orderId = shopOrder.getId().getIdentifier();

		assertTrue(shopOrderManagement.findByShopOrderId(orderId).isPresent());

		mvc.perform(get("/customerVerification"))
				.andExpect(status().isOk());
	}

	@Test
	@Disabled
	@WithUserDetails("boss")
	void postCustomerVerificationForm() throws Exception {
		ShopOrder shopOrder = shopOrderManagement.save(
				shopOrderManagement.create(employeeManagement.getDeleteLinkEmployee(), customerManagement.getDeleteLinkCustomer()));
		String orderId = shopOrder.getId().getIdentifier();

		assertTrue(shopOrderManagement.findByShopOrderId(orderId).isPresent());

		CustomerCreationForm ccf = new CustomerCreationForm("U.A. Highschool","123453546234","Head","Eraser");
		int tan = customerManagement.createCustomer(ccf).getTanNumber();

		MultiValueMap<String,String> editCsMap = new LinkedMultiValueMap<>();
		editCsMap.add("phone","123453546234");
		editCsMap.add("tan", String.valueOf(tan));

		mvc.perform(post("/customerVerification", Optional.of(employeeManagement.findByRole("Cashier").stream().findAny().get().getUserAccount())).params(editCsMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/order/newOrder"));

		mvc.perform(post("/customerVerification", Optional.of(employeeManagement.findByRole("Cashier").stream().findAny().get().getUserAccount())).params(editCsMap))
				.andExpect(status().isOk());

		MultiValueMap<String,String> editCsMap2 = new LinkedMultiValueMap<>();
		editCsMap2.add("phone","1234324335352");
		editCsMap2.add("tan", "1");

		mvc.perform(post("/customerVerification", Optional.of(employeeManagement.findByRole("Cashier").stream().findAny().get())).params(editCsMap2))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles="CASHIER")
	void getNewShopOrder() throws Exception {
		ShopOrder shopOrder = shopOrderManagement.save(
				shopOrderManagement.create(employeeManagement.getDeleteLinkEmployee(), customerManagement.getDeleteLinkCustomer()));
		String orderId = shopOrder.getId().getIdentifier();

		assertTrue(shopOrderManagement.findByShopOrderId(orderId).isPresent());

		Map<String, Object> sessionAttrs = new HashMap<>();
		sessionAttrs.put("newShopOrderId", orderId);

		mvc.perform(get("/newOrder"))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/order/newOrder"));

		mvc.perform(get("/order/newOrder").sessionAttrs(sessionAttrs))
				.andExpect(status().isOk())
				.andExpect(model().attribute("form", allOf(
						hasProperty("consumables", notNullValue()),
						hasProperty("pizzaPresets", notNullValue()),
						hasProperty("drinks", notNullValue()),
						hasProperty("dishSets", notNullValue()),
						hasProperty("toppings", notNullValue()),
						hasProperty("customPizzaQuantity", notNullValue()),
						hasProperty("shopOrderOrderLines", notNullValue()),
						hasProperty("deliveryType", notNullValue()) )));

	}

	@Test
	@WithMockUser(roles="CASHIER")
	void postCard() throws Exception {
		ShopOrder shopOrder = shopOrderManagement.save(
				shopOrderManagement.create(employeeManagement.getDeleteLinkEmployee(), customerManagement.getDeleteLinkCustomer()));
		String orderId = shopOrder.getId().getIdentifier();

		assertTrue(shopOrderManagement.findByShopOrderId(orderId).isPresent());

		Map<String, Object> sessionAttrs = new HashMap<>();
		sessionAttrs.put("newShopOrderId", orderId);

		mvc.perform(post("/newOrder/card").sessionAttrs(sessionAttrs))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/order/newOrder"));

	}

	@Test
	@WithMockUser(roles="CASHIER")
	void postConfigurator() throws Exception {
		ShopOrder shopOrder = shopOrderManagement.save(
				shopOrderManagement.create(employeeManagement.getDeleteLinkEmployee(), customerManagement.getDeleteLinkCustomer()));
		String orderId = shopOrder.getId().getIdentifier();

		assertTrue(shopOrderManagement.findByShopOrderId(orderId).isPresent());

		Map<String, Object> sessionAttrs = new HashMap<>();
		sessionAttrs.put("newShopOrderId", orderId);

		mvc.perform(post("/newOrder/configurator").sessionAttrs(sessionAttrs))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/order/newOrder"));

		assertTrue(shopOrderManagement.get(shopOrder.getId()).get().getOrderLines().stream()
				.anyMatch(orderLine -> catalogManagement.findById(orderLine.getProductIdentifier())
						.getCategories().toList().contains("CUSTOM_PIZZA")));
	}

	@Test
	@WithMockUser(roles="CASHIER")
	void postRemoveFromSummary() throws Exception {
		ShopOrder shopOrder = shopOrderManagement.save(
				shopOrderManagement.create(employeeManagement.getDeleteLinkEmployee(), customerManagement.getDeleteLinkCustomer()));
		String orderId = shopOrder.getId().getIdentifier();
		Product product = catalogManagement.createToppingProduct("rice","5");
		OrderLine orderLine = shopOrderManagement.addLinesByProduct(shopOrder.getId(), product, Quantity.of(1));

		Map<String, Object> sessionAttrs = new HashMap<>();
		sessionAttrs.put("newShopOrderId", orderId);

		mvc.perform(post("/newOrder/removeFromSummary/"+orderLine.getProductIdentifier().getIdentifier()).sessionAttrs(sessionAttrs))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/order/newOrder"));

		assertTrue(shopOrderManagement.findByShopOrderId(orderId).get().getOrderLines(product).isEmpty());
	}

	@Test
	@WithMockUser(roles="CASHIER")
	void postApply() throws Exception {

		Employee employee = employeeManagement.createEmployee(
				new EmployeeCreationForm("SupremeFanboy","HighLord","Wolnir","123","123","Cashier"));
		Customer customer = customerManagement.createCustomer(
				new CustomerCreationForm("SmoulderingLake","139871231810","DemonKing","Old"));

		ShopOrder shopOrder = shopOrderManagement.save(
				shopOrderManagement.create(employee, customer));
		String orderId = shopOrder.getId().getIdentifier();

		Map<String, Object> sessionAttrs = new HashMap<>();
		sessionAttrs.put("newShopOrderId", orderId);

		MultiValueMap<String,String> mvmap = new LinkedMultiValueMap<>();
		mvmap.add("deliveryType","Delivery");

		mvc.perform(post("/newOrder/apply").sessionAttrs(sessionAttrs).params(mvmap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/order"));

		assertNotNull(shopOrderManagement.findByShopOrderId(orderId).get().getCashier());
		assertNotNull(shopOrderManagement.findByShopOrderId(orderId).get().getDriver());
		assertNotNull(shopOrderManagement.findByShopOrderId(orderId).get().getChefs());
	}



}
