package papapizza.delivery;

import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import papapizza.employee.Employee;
import papapizza.employee.EmployeeManagement;
import papapizza.inventory.ProductCategory;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.order.ShopOrder;
import papapizza.order.ShopOrderManagement;
import papapizza.order.ShopOrderState;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional

class DeliveryControllerTest {


	@Autowired
	private MockMvc mvc;

	@Autowired
	private DeliveryManagement deliveryManagement;

	@Autowired
	private EmployeeManagement employeeManagement;

	@Autowired
	private ShopCatalogManagement shopCatalogManagement;

	@Autowired
	private ShopOrderManagement<ShopOrder> shopOrderManagement;

	private String testOrderId;

	void setupModel(Model model){
		SettingsForm vaf = new SettingsForm();
		vaf.setManualAssign(false);
		assert employeeManagement.findByRole("Driver").stream().findFirst().isPresent();
		vaf.setVehicleId(Objects.requireNonNull(employeeManagement.findByRole("Driver").stream().findFirst().get().getVehicle().getId()).getIdentifier());
		model.addAttribute("vehicleAssigment", vaf);
	}

	void setupComplete(){
		ShopOrder testShopOrder;
		assertTrue(shopOrderManagement.findAll().stream().findAny().isPresent());
		testShopOrder = shopOrderManagement.findAll().stream().findAny().get();
		this.testOrderId = Objects.requireNonNull(testShopOrder.getId()).getIdentifier();
	}

	@Test
	@WithMockUser(username="boss",roles="BOSS")
	void deliveryViewTest() throws Exception{
		assertTrue(shopOrderManagement.findAll().stream().findAny().isPresent());
		shopOrderManagement.setShopOrderState(shopOrderManagement.findAll().stream().findAny().get(), ShopOrderState.READYDELIVER);

		int deliveryCount = shopOrderManagement.findBy(ShopOrderState.READYDELIVER).toList().size() +
				shopOrderManagement.findBy(ShopOrderState.INDELIVERY).toList().size();

		mvc.perform(get("/delivery"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attribute("deliveries", IsCollectionWithSize.hasSize(deliveryCount)));


	}

	@Test
	@WithMockUser(username="boss",roles="BOSS")
	public void completeOrderBossTest() throws Exception {
		setupComplete();
		String orderId = this.testOrderId;
		mvc.perform(post("/delivery/complete/" + orderId))
				.andDo(print())
				.andExpect(status().isFound());
	}

	@Test
	@WithMockUser(username="boss",roles="DRIVER")
	public void completeOrderDriverTest() throws Exception {
		setupComplete();
		String orderId = this.testOrderId;
		mvc.perform(post("/delivery/complete/" + orderId))
				.andDo(print())
				.andExpect(status().isFound());
	}

	@Test
	@WithMockUser(username="boss", roles = "CASHIER")
	void cashierForbiddenTest() throws Exception {
		setupComplete();
		String orderId = this.testOrderId;
		mvc.perform(get("/delivery"))
				.andDo(print())
				.andExpect(status().isForbidden());

		mvc.perform(post("/delivery/complete/" + orderId))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username="boss",roles = "CHEF")
	void chefForbiddenTest() throws Exception {
		setupComplete();
		String orderId = this.testOrderId;
		mvc.perform(get("/delivery"))
				.andDo(print())
				.andExpect(status().isForbidden());

		mvc.perform(post("/delivery/complete/" + orderId))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username="boss", roles = "BOSS")
	public void reassignVehicleViewTest() throws Exception {
		mvc.perform(get("/delivery/settings"))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username="boss", roles = "DRIVER")
	public void driverForbiddenTest() throws Exception {
		mvc.perform(get("/delivery/settings"))
				.andDo(print())
				.andExpect(status().isForbidden());

		assert employeeManagement.findByRole("Driver").stream().findFirst().isPresent();
		Employee driver = employeeManagement.findByRole("Driver").stream().findFirst().get();
		mvc.perform(post("/delivery/reassign/" + driver.getId()))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username="boss", roles = "BOSS")
	public void reassignVehicleTest() throws Exception {
		MultiValueMap<String,String> newCsMap = new LinkedMultiValueMap<>();
		assert shopCatalogManagement.findByCategory(ProductCategory.VEHICLE.toString()).stream().findAny().isPresent();
		String id = Objects.requireNonNull(shopCatalogManagement.findByCategory(ProductCategory.VEHICLE.toString()).stream().findAny().get().getId()).getIdentifier();
		newCsMap.add("vehicleId", id);
		assert employeeManagement.findByRole("Driver").stream().findFirst().isPresent();
		Employee driver = employeeManagement.findByRole("Driver").stream().findFirst().get();
		mvc.perform(post("/delivery/reassign/" + driver.getId()).params(newCsMap))
				.andDo(print())
				.andExpect(status().isFound());
	}

}