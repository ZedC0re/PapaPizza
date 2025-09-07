package papapizza.kitchen;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import papapizza.inventory.ProductCategory;
import papapizza.inventory.ShopCatalogManagement;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class KitchenControllerTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ShopCatalogManagement shopCatalogManagement;

	@Autowired
	private KitchenController kitchenController;

	@Test
	@WithMockUser(username = "boss", roles = "BOSS")
	public void kitchenViewTest() throws Exception{
		mvc.perform(get("/kitchen"))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "BOSS")
	public void redirectTest() throws Exception{
		mvc.perform(get("/kitchen"))
				.andDo(print())
				.andExpect(status().isFound());
	}

	@Test
	@WithMockUser(username = "chef1", roles = "CHEF")
	public void testChefView() throws Exception{
		mvc.perform(get("/kitchen"))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "boss", roles = "CHEF")
	public void chefAllowedTest() throws Exception{
		mvc.perform(get("/kitchen"))
				.andDo((print()))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "boss", roles = "CASHIER")
	public void cashierForbiddenTest() throws Exception{
		mvc.perform(get("/kitchen"))
				.andDo((print()))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "boss", roles = "DRIVER")
	public void driverForbiddenTest() throws Exception{
		mvc.perform(get("/kitchen"))
				.andDo((print()))
				.andExpect(status().isForbidden());
	}

	/*
	@Test
	public void getKitchenOfEmployeeTest(){
		Employee employee = employee;
		employee.setRole("Chef");
		kitchenController.getKitchenOfEmployee(employee);
		assertTrue(shopCatalogManagement.findByCategory(ProductCategory.OVEN.toString()).stream().findFirst().isPresent());
		assertTrue(shopCatalogManagement.findByCategory(ProductCategory.PIZZA.toString()).stream().findFirst().isPresent());
		OvenProduct testOven = (OvenProduct) shopCatalogManagement.findByCategory(ProductCategory.OVEN.toString()).stream().findFirst().get();
		testOven.setChef(employee);
		PizzaProduct bakingPizza = (PizzaProduct) shopCatalogManagement.findByCategory(ProductCategory.PIZZA.toString()).stream().findFirst().get();
		bakingPizza.setState(PizzaState.PENDING);
		testOven.setPizzas(List.of(bakingPizza));
	}	//TODO: Missing 2 Lines of coverage
*/
	@Test
	@WithMockUser(username = "boss", roles = "BOSS")
	public void startBakingTest() throws Exception{
		assertTrue(shopCatalogManagement.findByCategory(ProductCategory.PIZZA.toString()).stream().findAny().isPresent());
		String pizzaId = Objects.requireNonNull(shopCatalogManagement.findByCategory(ProductCategory.PIZZA.toString()).stream().findAny().get().getId()).getIdentifier();
		mvc.perform(post("/kitchen/bake/" + pizzaId))
				.andDo((print()))
				.andExpect(status().isFound());

	}

	@Test
	@WithMockUser(username = "boss", roles = "BOSS")
	public void finishBakingTest() throws Exception{
		assertTrue(shopCatalogManagement.findByCategory(ProductCategory.PIZZA.toString()).stream().findAny().isPresent());
		String pizzaId = Objects.requireNonNull(shopCatalogManagement.findByCategory(ProductCategory.PIZZA.toString()).stream().findAny().get().getId()).getIdentifier();
		mvc.perform(post("/kitchen/finish/" + pizzaId))
				.andDo((print()))
				.andExpect(status().isFound());

	}

}
