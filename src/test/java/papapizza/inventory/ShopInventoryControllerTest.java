package papapizza.inventory;

import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.salespointframework.catalog.ProductIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import papapizza.inventory.creationForms.*;
import papapizza.inventory.items.*;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.core.AllOf.allOf;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ShopInventoryControllerTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ShopCatalogManagement catalogManagement;

	@Test
	@WithMockUser(roles = "BOSS")
	void inventoryTableTest() throws Exception {
		int pizzaListSize = (int) catalogManagement.findByCategory(ProductCategory.PIZZA.name()).stream().count();
		int consumableListSize = (int) catalogManagement.findByCategory(ProductCategory.CONSUMABLE.name()).stream().count();
		int drinkListSize = (int) catalogManagement.findByCategory(ProductCategory.DRINK.name()).stream().count();
		int toppingListSize = (int) catalogManagement.findByCategory(ProductCategory.TOPPING.name()).stream().count();
		int dishsetListSize = (int) catalogManagement.findByCategory(ProductCategory.DISHSET.name()).stream().count();
		int vehicleListSize = (int) catalogManagement.findByCategory(ProductCategory.VEHICLE.name()).stream().count();
		int ovenListSize = (int) catalogManagement.findByCategory(ProductCategory.OVEN.name()).stream().count();

		mvc.perform(get("/inventory"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attribute("pizzaPresets", IsCollectionWithSize.hasSize(pizzaListSize))) //use Enum.name()?
				.andExpect(model().attribute("Consumables", IsCollectionWithSize.hasSize(consumableListSize)))
				.andExpect(model().attribute("Drinks", IsCollectionWithSize.hasSize(drinkListSize)))
				.andExpect(model().attribute("Toppings", IsCollectionWithSize.hasSize(toppingListSize)))
				.andExpect(model().attribute("Dishsets", IsCollectionWithSize.hasSize(dishsetListSize)))
				.andExpect(model().attribute("Vehicles", IsCollectionWithSize.hasSize(vehicleListSize)))
				.andExpect(model().attribute("Ovens", IsCollectionWithSize.hasSize(ovenListSize)));
	}

	@Test
	@WithMockUser(roles = "CASHIER")
	void cashierForbiddenTest() throws Exception {
		mvc.perform(get("/inventory"))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "DRIVER")
	void driverForbiddenTest() throws Exception {
		mvc.perform(get("/inventory"))
				.andDo(print())
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "CHEF")
	void chefForbiddenTest() throws Exception {
		mvc.perform(get("/inventory"))
				.andDo(print())
				.andExpect(status().isForbidden());
	}


	@Test
	@WithMockUser(roles = "BOSS")
	void searchTest() throws Exception{

		ConsumableProduct product1 = catalogManagement.createConsumableProduct("searchProduct","10.34","testIngredient");
		ToppingProduct product2 = catalogManagement.createToppingProduct("testProduct","1.23");

		MultiValueMap<String, String> searchMap = new LinkedMultiValueMap<>();

		//test to find searchProduct (and not find testProduct) if searching "search"
		searchMap.add("searchText","search");

		assertTrue(mvc.perform(get("/inventory").params(searchMap))
				.andExpect(status().isOk())
				.andReturn()
				.getModelAndView()
				.getModel()
				.toString()
				.contains("searchProduct, " +product1.getId()+", EUR 10.34, handled in UNIT"));

		assertFalse(mvc.perform(get("/inventory").params(searchMap))
				.andExpect(status().isOk())
				.andReturn()
				.getModelAndView()
				.getModel()
				.toString()
				.contains("testProduct, " +product2.getId()+", EUR 1.23, handled in UNIT"));

		//test to find testProduct (and not find searchProduct) if searching "test"
		searchMap.replace("searchText",List.of("test"));

		assertFalse(mvc.perform(get("/inventory").params(searchMap))
				.andExpect(status().isOk())
				.andReturn()
				.getModelAndView()
				.getModel()
				.toString()
				.contains("searchProduct, " +product1.getId()+", EUR 10.34, handled in UNIT"));

		assertTrue(mvc.perform(get("/inventory").params(searchMap))
				.andExpect(status().isOk())
				.andReturn()
				.getModelAndView()
				.getModel()
				.toString()
				.contains("testProduct, " +product2.getId()+", EUR 1.23, handled in UNIT"));

		//test to find both products if searching "Product"
		searchMap.replace("searchText",List.of("Product"));

		assertTrue(mvc.perform(get("/inventory").params(searchMap))
				.andExpect(status().isOk())
				.andReturn()
				.getModelAndView()
				.getModel()
				.toString()
				.contains("searchProduct, " +product1.getId()+", EUR 10.34, handled in UNIT"));

		assertTrue(mvc.perform(get("/inventory").params(searchMap))
				.andExpect(status().isOk())
				.andReturn()
				.getModelAndView()
				.getModel()
				.toString()
				.contains("testProduct, " +product2.getId()+", EUR 1.23, handled in UNIT"));

	}


	//
	//addTests
	//


	//Pizzapresets


	@Test
	@WithMockUser(roles = "BOSS")
	void addPizzaPresetGetTest() throws Exception {
		mvc.perform(get("/addPizzaPreset"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("form"));
	}


	@Test
	@WithMockUser(roles = "BOSS")
	void addPizzaPresetPostTest() throws Exception {
		//creating strings, that imitate a possible payload
		String testToppingString1 = "toppings[" + catalogManagement.createToppingProduct("Salami1", "1.5").getId().toString() + "]";
		String testToppingString2 = "toppings[" + catalogManagement.createToppingProduct("Ham1", "2.5").getId().toString() + "]";

		MultiValueMap<String, String> newPizzaMap = new LinkedMultiValueMap<>();
		newPizzaMap.add(testToppingString1, "true");
		newPizzaMap.add(testToppingString2, "true");
		newPizzaMap.add("name", "Pizza01");
		newPizzaMap.add("price", "12.5");

		//test if page throws error if no request params are given
		mvc.perform(post("/addPizzaPreset"))
				.andExpect(status().isOk())
				.andExpect(model().hasErrors());

		//test with correct request params
		mvc.perform(post("/addPizzaPreset").params(newPizzaMap))
				.andExpect(status().is(302)) //redirect
				.andExpect(redirectedUrl("/inventory"));
	}


	@Test
	@WithMockUser(roles = "BOSS")
	void addPizzaPresetDoubleNameTest() throws Exception {
		//create a pizza with the name to assure, that it's already in the database
		catalogManagement.createPizzaProduct("doubleNamePizza", "10", catalogManagement.findByCategory(ProductCategory.TOPPING.toString()).stream().map(p -> (ToppingProduct) p).collect(Collectors.toList()));
		//creating strings, that imitate a possible payload
		String testToppingString1 = "toppings[" + catalogManagement.createToppingProduct("Salami1", "1.5").getId().toString() + "]";
		String testToppingString2 = "toppings[" + catalogManagement.createToppingProduct("Ham1", "2.5").getId().toString() + "]";

		MultiValueMap<String, String> newPizzaMap = new LinkedMultiValueMap<>();
		newPizzaMap.add(testToppingString1, "true");
		newPizzaMap.add(testToppingString2, "true");
		newPizzaMap.add("name", "doubleNamePizza");
		newPizzaMap.add("price", "12.5");

		mvc.perform(post("/addPizzaPreset").params(newPizzaMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "ShopInventoryProductCreationForm.notUnique.name"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addPizzaPresetPriceValidationTest() throws Exception {
		//creating strings, that imitate a possible payload
		String testToppingString1 = "toppings[" + catalogManagement.createToppingProduct("Salami1", "1.5").getId().toString() + "]";
		String testToppingString2 = "toppings[" + catalogManagement.createToppingProduct("Ham1", "2.5").getId().toString() + "]";


		MultiValueMap<String, String> newPizzaMap = new LinkedMultiValueMap<>();
		newPizzaMap.add("name", "wrongPricePizza");
		newPizzaMap.add("price", "12.5.3");
		newPizzaMap.add(testToppingString1, "true");
		newPizzaMap.add(testToppingString2, "true");

		mvc.perform(post("/addPizzaPreset").params(newPizzaMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));

		//try with other wrong price
		newPizzaMap.clear();
		newPizzaMap.add("name", "wrongPricePizza");
		newPizzaMap.add("price", "12.444");
		newPizzaMap.add(testToppingString1, "true");
		newPizzaMap.add(testToppingString2, "true");

		mvc.perform(post("/addPizzaPreset").params(newPizzaMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));

		//try with another wrong price
		newPizzaMap.clear();
		newPizzaMap.add("name", "wrongPricePizza");
		newPizzaMap.add("price", "12.44a");
		newPizzaMap.add(testToppingString1, "true");
		newPizzaMap.add(testToppingString2, "true");

		mvc.perform(post("/addPizzaPreset").params(newPizzaMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));

		//try with another wrong price
		newPizzaMap.clear();
		newPizzaMap.add("name", "wrongPricePizza");
		newPizzaMap.add("price", "12 44");
		newPizzaMap.add(testToppingString1, "true");
		newPizzaMap.add(testToppingString2, "true");

		mvc.perform(post("/addPizzaPreset").params(newPizzaMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addPizzaPresetBlankTest() throws Exception {

		MultiValueMap<String, String> newPizzaMap = new LinkedMultiValueMap<>();
		newPizzaMap.add("name", "");
		newPizzaMap.add("price", "");
		mvc.perform(post("/addPizzaPreset").params(newPizzaMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "NotBlank"))
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "NotBlank"));
		//imo a pizza not having to have a topping is more of a feature than a bug... => no Validation and therefore not test for this is intentional...
	}


	//Consumable

	@Test
	@WithMockUser(roles = "BOSS")
	void addConsumableGetTest() throws Exception {
		mvc.perform(get("/addConsumable"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("form"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addConsumablePostTest() throws Exception {
		MultiValueMap<String, String> newConsumableMap = new LinkedMultiValueMap<>();
		newConsumableMap.add("name", "Consumable1");
		newConsumableMap.add("price", "4.53");
		newConsumableMap.add("ingredients", "nothing");
		//test if page throws error if no request params are given
		mvc.perform(post("/addConsumable"))
				.andExpect(status().isOk())
				.andExpect(model().hasErrors());

		//test with correct request params
		mvc.perform(post("/addConsumable").params(newConsumableMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addConsumableDoubleNameTest() throws Exception {
		//create a consumable with the name to assure, that it's already in the database
		catalogManagement.createConsumableProduct("doubleNameConsumable", "10", "something");

		MultiValueMap<String, String> newConsumableMap = new LinkedMultiValueMap<>();
		newConsumableMap.add("name", "doubleNameConsumable"); //trying to add Consumable with same name
		newConsumableMap.add("price", "12.5");

		mvc.perform(post("/addConsumable").params(newConsumableMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "ShopInventoryProductCreationForm.notUnique.name"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addConsumablePriceValidationTest() throws Exception {

		MultiValueMap<String, String> newConsumableMap = new LinkedMultiValueMap<>();
		newConsumableMap.add("name", "wrongPricePizza");
		newConsumableMap.add("price", "12.5.3");


		mvc.perform(post("/addConsumable").params(newConsumableMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));

		//try with other wrong price
		newConsumableMap.clear();
		newConsumableMap.add("name", "wrongPriceConsumable");
		newConsumableMap.add("price", "12.444");


		mvc.perform(post("/addConsumable").params(newConsumableMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));

		//try with another wrong price
		newConsumableMap.clear();
		newConsumableMap.add("name", "wrongPriceConsumable");
		newConsumableMap.add("price", "12.44a");


		mvc.perform(post("/addConsumable").params(newConsumableMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));

		//try with another wrong price
		newConsumableMap.clear();
		newConsumableMap.add("name", "wrongPriceConsumable");
		newConsumableMap.add("price", "-12.44");


		mvc.perform(post("/addConsumable").params(newConsumableMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addConsumableBlankTest() throws Exception {

		MultiValueMap<String, String> newConsumableMap = new LinkedMultiValueMap<>();
		newConsumableMap.add("name", "");
		newConsumableMap.add("price", "");
		mvc.perform(post("/addConsumable").params(newConsumableMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "NotBlank"))
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "NotBlank"));
				//Ingredients can be blank
	}

//Drink

	@Test
	@WithMockUser(roles = "BOSS")
	void addDrinkGetTest() throws Exception {
		mvc.perform(get("/addDrink"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("form"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addDrinkPostTest() throws Exception {
		MultiValueMap<String, String> newDrinkMap = new LinkedMultiValueMap<>();
		newDrinkMap.add("name", "Drink1");
		newDrinkMap.add("price", "2.33");
		newDrinkMap.add("ingredients", "nothing");
		//test if page throws error if no request params are given
		mvc.perform(post("/addDrink"))
				.andExpect(status().isOk())
				.andExpect(model().hasErrors());

		//test with correct request params
		mvc.perform(post("/addDrink").params(newDrinkMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addDrinkDoubleNameTest() throws Exception {
		//create a drink with the name to assure, that it's already in the database
		catalogManagement.createConsumableProduct("doubleNameDrink", "2.5", "something");

		MultiValueMap<String, String> newConsumableMap = new LinkedMultiValueMap<>();
		newConsumableMap.add("name", "doubleNameDrink"); //trying to add Drink with same name
		newConsumableMap.add("price", "1.25");

		mvc.perform(post("/addDrink").params(newConsumableMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "ShopInventoryProductCreationForm.notUnique.name"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addDrinkPriceValidationTest() throws Exception {

		MultiValueMap<String, String> newDrinkMap = new LinkedMultiValueMap<>();
		newDrinkMap.add("name", "wrongPriceDrink");
		newDrinkMap.add("price", "2.5.3");


		mvc.perform(post("/addDrink").params(newDrinkMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));
		
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addDrinkBlankTest() throws Exception {

		MultiValueMap<String, String> newDrinkMap = new LinkedMultiValueMap<>();
		newDrinkMap.add("name", "");
		newDrinkMap.add("price", "");
		mvc.perform(post("/addConsumable").params(newDrinkMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "NotBlank"))
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "NotBlank"));
				//Ingredients can be blank
	}

//Toppings

	@Test
	@WithMockUser(roles = "BOSS")
	void addToppingGetTest() throws Exception {
		mvc.perform(get("/addTopping"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("form"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addToppingPostTest() throws Exception {
		MultiValueMap<String, String> newToppingMap = new LinkedMultiValueMap<>();
		newToppingMap.add("name", "Topping1");
		newToppingMap.add("price", "1.60");

		//test if page throws error if no request params are given
		mvc.perform(post("/addTopping"))
				.andExpect(status().isOk())
				.andExpect(model().hasErrors());

		//test with correct request params
		mvc.perform(post("/addTopping").params(newToppingMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addToppingDoubleNameTest() throws Exception {
		//create a topping with the name to assure, that it's already in the database
		catalogManagement.createToppingProduct("doubleNameTopping", "1.0");

		MultiValueMap<String, String> newToppingMap = new LinkedMultiValueMap<>();
		newToppingMap.add("name", "doubleNameTopping"); //trying to add Topping with same name
		newToppingMap.add("price", "2.5");

		mvc.perform(post("/addTopping").params(newToppingMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "ShopInventoryProductCreationForm.notUnique.name"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addToppingPriceValidationTest() throws Exception {

		MultiValueMap<String, String> newToppingMap = new LinkedMultiValueMap<>();
		newToppingMap.add("name", "wrongPriceTopping");
		newToppingMap.add("price", "1 5.3");


		mvc.perform(post("/addTopping").params(newToppingMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addToppingBlankTest() throws Exception {

		MultiValueMap<String, String> newToppingMap = new LinkedMultiValueMap<>();
		newToppingMap.add("name", "");
		newToppingMap.add("price", "");
		mvc.perform(post("/addTopping").params(newToppingMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "NotBlank"))
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "NotBlank"));

	}

	//Vehicle

	@Test
	@WithMockUser(roles = "BOSS")
	void addVehicleGetTest() throws Exception {
		mvc.perform(get("/addVehicle"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("form"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addVehiclePostTest() throws Exception {
		MultiValueMap<String, String> newVehicleMap = new LinkedMultiValueMap<>();
		newVehicleMap.add("name", "Vehicle1");
		newVehicleMap.add("price", "16000");
		newVehicleMap.add("slots", "25");

		//test if page throws error if no request params are given
		mvc.perform(post("/addVehicle"))
				.andExpect(status().isOk())
				.andExpect(model().hasErrors());

		//test with correct request params
		mvc.perform(post("/addVehicle").params(newVehicleMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addVehicleDoubleNameTest() throws Exception {
		//create a vehicle with the name to assure, that it's already in the database
		catalogManagement.createVehicleProduct("doubleNameVehicle", "10000", "6");

		MultiValueMap<String, String> newVehicleMap = new LinkedMultiValueMap<>();
		newVehicleMap.add("name", "doubleNameVehicle"); //trying to add Vehicle with same name
		newVehicleMap.add("price", "125000");
		newVehicleMap.add("slots","10");

		mvc.perform(post("/addVehicle").params(newVehicleMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "ShopInventoryProductCreationForm.notUnique.name"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addVehiclePriceValidationTest() throws Exception {

		MultiValueMap<String, String> newVehicleMap = new LinkedMultiValueMap<>();
		newVehicleMap.add("name", "wrongPriceVehicle");
		newVehicleMap.add("price", "12.5.3");
		newVehicleMap.add("slots","10");

		mvc.perform(post("/addVehicle").params(newVehicleMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addVehicleSlotsValidationTest() throws Exception {

		MultiValueMap<String, String> newVehicleMap = new LinkedMultiValueMap<>();
		newVehicleMap.add("name", "wrongSlotsVehicle1");
		newVehicleMap.add("price", "12.5");
		newVehicleMap.add("slots","10.4");

		mvc.perform(post("/addVehicle").params(newVehicleMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "slots", "ShopInventoryProductCreationForm.wrongPattern.slots"));

		newVehicleMap.clear();
		newVehicleMap.add("name", "wrongSlotsVehicle1");
		newVehicleMap.add("price", "12.5");
		newVehicleMap.add("slots","abc");

		mvc.perform(post("/addVehicle").params(newVehicleMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "slots", "ShopInventoryProductCreationForm.wrongPattern.slots"));

		newVehicleMap.clear();
		newVehicleMap.add("name", "wrongSlotsVehicle1");
		newVehicleMap.add("price", "12.5");
		newVehicleMap.add("slots","1 1");

		mvc.perform(post("/addVehicle").params(newVehicleMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "slots", "ShopInventoryProductCreationForm.wrongPattern.slots"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addVehicleBlankTest() throws Exception {

		MultiValueMap<String, String> newVehicleMap = new LinkedMultiValueMap<>();
		newVehicleMap.add("name", "");
		newVehicleMap.add("price", "");
		newVehicleMap.add("slots","");
		mvc.perform(post("/addVehicle").params(newVehicleMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "NotBlank"))
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "NotBlank"))
				.andExpect(model().attributeHasFieldErrorCode("form","slots","NotBlank"));
	}

	//Dishset

	@Test
	@WithMockUser(roles = "BOSS")
	void addDishsetGetTest() throws Exception {
		mvc.perform(get("/addDishset"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("form"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addDishsetPostTest() throws Exception {
		MultiValueMap<String, String> newDishsetMap = new LinkedMultiValueMap<>();
		newDishsetMap.add("name", "Dishset01");
		newDishsetMap.add("price", "15");

		//test if page throws error if no request params are given
		mvc.perform(post("/addDishset"))
				.andExpect(status().isOk())
				.andExpect(model().hasErrors());

		//test with correct request params
		mvc.perform(post("/addDishset").params(newDishsetMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addDishsetDoubleNameTest() throws Exception {
		//create a dishset with the name to assure, that it's already in the database
		catalogManagement.createDishsetProduct("doubleNameDishset", "10");

		MultiValueMap<String, String> newDishsetMap = new LinkedMultiValueMap<>();
		newDishsetMap.add("name", "doubleNameDishset"); //trying to add Dishset with same name
		newDishsetMap.add("price", "12.5");

		mvc.perform(post("/addDishset").params(newDishsetMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "ShopInventoryProductCreationForm.notUnique.name"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addDishsetPriceValidationTest() throws Exception {

		MultiValueMap<String, String> newDishsetMap = new LinkedMultiValueMap<>();
		newDishsetMap.add("name", "wrongPriceDishset");
		newDishsetMap.add("price", "12.5.3");


		mvc.perform(post("/addDishset").params(newDishsetMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addDishsetBlankTest() throws Exception {

		MultiValueMap<String, String> newDishsetMap = new LinkedMultiValueMap<>();
		newDishsetMap.add("name", "");
		newDishsetMap.add("price", "");
		mvc.perform(post("/addDishset").params(newDishsetMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "NotBlank"))
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "NotBlank"));

	}

	//Oven

	@Test
	@WithMockUser(roles = "BOSS")
	void addOvenGetTest() throws Exception {
		mvc.perform(get("/addOven"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("form"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addOvenPostTest() throws Exception {
		MultiValueMap<String, String> newOvenMap = new LinkedMultiValueMap<>();
		newOvenMap.add("name", "Oven001");
		newOvenMap.add("price", "15");

		//test if page throws error if no request params are given
		mvc.perform(post("/addOven"))
				.andExpect(status().isOk())
				.andExpect(model().hasErrors());

		//test with correct request params
		mvc.perform(post("/addOven").params(newOvenMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"));
	}


	@Test
	@WithMockUser(roles = "BOSS")
	void addOvenDoubleNameTest() throws Exception {
		//create a oven with the name to assure, that it's already in the database
		catalogManagement.createOvenProduct( "doubleNameOven1","10");

		MultiValueMap<String, String> newOvenMap = new LinkedMultiValueMap<>();
		newOvenMap.add("name", "doubleNameOven1"); //trying to add Oven with same name
		newOvenMap.add("price", "12.5");

		mvc.perform(post("/addOven").params(newOvenMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "ShopInventoryProductCreationForm.notUnique.name"));

	}


	@Test
	@WithMockUser(roles = "BOSS")
	void addOvenPriceValidationTest() throws Exception {

		MultiValueMap<String, String> newOvenMap = new LinkedMultiValueMap<>();
		newOvenMap.add("name", "PriceValidationOven1");
		newOvenMap.add("price", "12.5.3");


		mvc.perform(post("/addOven").params(newOvenMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void addOvenBlankTest() throws Exception {

		MultiValueMap<String, String> newOvenMap = new LinkedMultiValueMap<>();
		newOvenMap.add("name", "BlankOven1");
		newOvenMap.add("price", "");
		mvc.perform(post("/addOven").params(newOvenMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "NotBlank"));

	}

	//
	//editTests
	//


	@Test
	@WithMockUser(roles = "BOSS")
	void editPizzaPresetGetTest() throws Exception {

		ToppingProduct testTopping1 = catalogManagement.createToppingProduct("Pineapple1", "1.5");
		ToppingProduct testTopping2 = catalogManagement.createToppingProduct("Cheese1", "2.5");
		ToppingProduct testTopping3 = catalogManagement.createToppingProduct("Chicken1", "1.7");

		Map<ToppingProduct, Boolean> selectedToppingsMap = new HashMap<>();
		selectedToppingsMap.put(testTopping1, true);
		selectedToppingsMap.put(testTopping2, true);

		List<ToppingProduct> allToppings = catalogManagement.findByCategory(ProductCategory.TOPPING.toString()).stream().map(product -> (ToppingProduct) product).collect(Collectors.toList());

		PizzaProductCreationForm creationForm = new PizzaProductCreationForm("testPizzaPreset", "4.3", null);
		PizzaProduct product = catalogManagement.createPizzaProduct(creationForm.getName(), creationForm.getPrice(), selectedToppingsMap.keySet().stream().collect(Collectors.toList()));
		PizzaProductCreationForm.setFormDetails(creationForm, product, allToppings);
		ProductIdentifier id = product.getId();

		mvc.perform(get("/inventory/editPizzaPreset/" + id))
				.andExpect(status().isOk())
				.andExpect(model().attribute("form", allOf(
						hasProperty("name", is(creationForm.getName())),
						hasProperty("price", is(creationForm.getPrice())),
						hasProperty("toppings", is(creationForm.getToppings()))
				)));


		//test if product, that does not exist gets recognized as such
		mvc.perform(get("/inventory/editPizzaPreset/" + 1337))
				.andExpect(status().isOk())
				.andExpect(model().attribute("productFound", is(false)));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editPizzaPresetPostTest() throws Exception {

		ToppingProduct testTopping1 = catalogManagement.createToppingProduct("Pineapple1", "1.5"); //TODO create them in constructor or rather @before?
		ToppingProduct testTopping2 = catalogManagement.createToppingProduct("Cheese1", "2.5");
		ToppingProduct testTopping3 = catalogManagement.createToppingProduct("Chicken1", "1.7");


		//creating strings, that imitate a possible payload
		String testToppingString1 = "toppings[" + testTopping1.getId().toString() + "]";
		String testToppingString3 = "toppings[" + testTopping3.getId().toString() + "]";

		Map<ToppingProduct, Boolean> toppingMap = new HashMap<>();
		toppingMap.put(testTopping1, true);
		toppingMap.put(testTopping2, true);

		List<ToppingProduct> allToppings = catalogManagement.findByCategory(ProductCategory.TOPPING.toString()).stream().map(product -> (ToppingProduct) product).collect(Collectors.toList());

		PizzaProductCreationForm creationForm = new PizzaProductCreationForm("testPizzaPreset", "12.30", toppingMap);
		PizzaProduct product = catalogManagement.createPizzaProduct(creationForm.getName(), creationForm.getPrice(), creationForm.getToppings().keySet().stream().collect(Collectors.toList()));
		PizzaProductCreationForm.setFormDetails(creationForm, product, allToppings);
		ProductIdentifier id = product.getId();

		MultiValueMap<String, String> editPizzaMap = new LinkedMultiValueMap<>();
		editPizzaMap.add("name", "PizzaPreset1");
		editPizzaMap.add("price", "12.40");
		editPizzaMap.add(testToppingString1, "true");
		editPizzaMap.add(testToppingString3, "true");


		mvc.perform(post("/inventory/editPizzaPreset/" + id).params(editPizzaMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"));


		//compare values with db
		PizzaProduct dbPizzaPreset = (PizzaProduct) catalogManagement.findById(id);
		assertEquals(dbPizzaPreset.getName(), editPizzaMap.toSingleValueMap().get("name"));
		DecimalFormat df = new DecimalFormat("#.##");
		assertEquals(df.format(dbPizzaPreset.getPrice().getNumber()), df.format(Float.valueOf(editPizzaMap.toSingleValueMap().get("price"))));
		List<ToppingProduct> dbToppings = dbPizzaPreset.getToppings();
		List<ToppingProduct> editToppings = Arrays.asList((ToppingProduct) catalogManagement.findByName("Chicken1"), (ToppingProduct) catalogManagement.findByName("Pineapple1"));
		assertTrue(dbToppings.size() == editToppings.size() && dbToppings.containsAll(editToppings) && editToppings.containsAll(dbToppings));


	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editPizzaPresetDoubleNameTest() throws Exception {

		PizzaProduct toEdit = catalogManagement.createPizzaProduct("Pizza101", "10", catalogManagement.findByCategory(ProductCategory.TOPPING.toString()).stream().map(p -> (ToppingProduct) p).collect(Collectors.toList()));
		//create a pizza with the name to assure, that it's already in the database
		catalogManagement.createPizzaProduct("doubleNamePizza", "10", catalogManagement.findByCategory(ProductCategory.TOPPING.toString()).stream().map(p -> (ToppingProduct) p).collect(Collectors.toList()));

		//creating strings, that imitate a possible payload
		String testToppingString1 = "toppings[" + catalogManagement.createToppingProduct("Salami1", "1.5").getId().toString() + "]";
		String testToppingString2 = "toppings[" + catalogManagement.createToppingProduct("Ham1", "2.5").getId().toString() + "]";

		MultiValueMap<String, String> editPizzaMap = new LinkedMultiValueMap<>();
		editPizzaMap.add(testToppingString1, "true");
		editPizzaMap.add(testToppingString2, "true");
		editPizzaMap.add("name", "doubleNamePizza");
		editPizzaMap.add("price", "12.5");

		mvc.perform(post("/inventory/editPizzaPreset/" + toEdit.getId()).params(editPizzaMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "ShopInventoryProductCreationForm.notUnique.name"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editPizzaPresetPriceValidationTest() throws Exception {
		PizzaProduct toEdit = catalogManagement.createPizzaProduct("Pizza102", "10.2", catalogManagement.findByCategory(ProductCategory.TOPPING.toString()).stream().map(p -> (ToppingProduct) p).collect(Collectors.toList()));
		//creating strings, that imitate a possible payload
		String testToppingString1 = "toppings[" + catalogManagement.createToppingProduct("Salami1", "1.5").getId().toString() + "]";
		String testToppingString2 = "toppings[" + catalogManagement.createToppingProduct("Ham1", "2.5").getId().toString() + "]";


		MultiValueMap<String, String> editPizzaMap = new LinkedMultiValueMap<>();
		editPizzaMap.add("name", "wrongPricePizza");
		editPizzaMap.add("price", "12.5.3");
		editPizzaMap.add(testToppingString1, "true");
		editPizzaMap.add(testToppingString2, "true");

		mvc.perform(post("/inventory/editPizzaPreset/" + toEdit.getId()).params(editPizzaMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));


	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editPizzaPresetBlankTest() throws Exception {
		PizzaProduct toEdit = catalogManagement.createPizzaProduct("Pizza103", "10.3", catalogManagement.findByCategory(ProductCategory.TOPPING.toString()).stream().map(p -> (ToppingProduct) p).collect(Collectors.toList()));
		MultiValueMap<String, String> editPizzaMap = new LinkedMultiValueMap<>();
		editPizzaMap.add("name", "");
		editPizzaMap.add("price", "");
		mvc.perform(post("/inventory/editPizzaPreset/" + toEdit.getId()).params(editPizzaMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "NotBlank"))
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "NotBlank"));
		//imo a pizza not having to have a topping is more of a feature than a bug... => no Validation and therefore not test for this is intentional...
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editPizzaPresetNothingChangedTest() throws Exception {
		String name = "samePizza";
		String price = "10.50";
		ToppingProduct sameTopping1 = catalogManagement.createToppingProduct("sameTopping1", "1.2");
		ToppingProduct sameTopping2 = catalogManagement.createToppingProduct("sameTopping2", "1.6");
		List<ToppingProduct> sameToppings = new ArrayList<>();
		sameToppings.add(sameTopping1);
		sameToppings.add(sameTopping2);
		PizzaProduct samePizza = catalogManagement.createPizzaProduct(name, price, sameToppings);

		String testsameToppingString1 = "toppings[" + sameTopping1.getId().toString() + "]";
		String testsameToppingString2 = "toppings[" + sameTopping2.getId().toString() + "]";

		MultiValueMap<String, String> editPizzaMap = new LinkedMultiValueMap<>();
		editPizzaMap.add("name", name);
		editPizzaMap.add("price", price);
		editPizzaMap.add(testsameToppingString1, "true");
		editPizzaMap.add(testsameToppingString2, "true");

		mvc.perform(post("/inventory/editPizzaPreset/" + samePizza.getId()).params(editPizzaMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"))
				.andExpect(flash().attribute("InventoryActionResult", "editUnchanged"));

	}

	//Consumable

	@Test
	@WithMockUser(roles = "BOSS")
	void editConsumableGetTest() throws Exception {
		ConsumableProductCreationForm creationForm = new ConsumableProductCreationForm("testConsumable", "4.3", "nix");
		ConsumableProduct product = catalogManagement.createConsumableProduct(creationForm.getName(), creationForm.getPrice(), creationForm.getIngredients());
		ProductIdentifier id = product.getId();
		mvc.perform(get("/inventory/editConsumable/" + id))
				.andExpect(status().isOk())
				.andExpect(model().attribute("form", allOf(
						hasProperty("name", is(creationForm.getName())),
						hasProperty("price", is(creationForm.getPrice())),
						hasProperty("ingredients", is(creationForm.getIngredients()))
				)));

		//test if product, that does not exist gets recognized as such
		mvc.perform(get("/inventory/editConsumable/" + 1337))
				.andExpect(status().isOk())
				.andExpect(model().attribute("productFound", is(false)));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editConsumablePostTest() throws Exception {
		ConsumableProductCreationForm creationForm = new ConsumableProductCreationForm("testConsumable", "4.3", "nothing");
		ConsumableProduct product = catalogManagement.createConsumableProduct(creationForm.getName(), creationForm.getPrice(), creationForm.getIngredients());
		ProductIdentifier id = product.getId();

		MultiValueMap<String, String> editConsumableMap = new LinkedMultiValueMap<>();
		editConsumableMap.add("name", "Consumable1");
		editConsumableMap.add("price", "4.53");
		editConsumableMap.add("ingredients", "nothing");

		mvc.perform(post("/inventory/editConsumable/" + id).params(editConsumableMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"));

		//compare values with db
		ConsumableProduct dbConsumable = (ConsumableProduct) catalogManagement.findById(id);
		assertEquals(dbConsumable.getName(), editConsumableMap.toSingleValueMap().get("name"));
		DecimalFormat df = new DecimalFormat("#.##");
		assertEquals(df.format(dbConsumable.getPrice().getNumber()), editConsumableMap.toSingleValueMap().get("price"));
		assertEquals(dbConsumable.getIngredients(), editConsumableMap.toSingleValueMap().get("ingredients"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editConsumableDoubleNameTest() throws Exception {
		ConsumableProduct toEdit = catalogManagement.createConsumableProduct("Consumable101", "10", "something");

		catalogManagement.createConsumableProduct("doubleNameConsumable2", "10", "something else");

		MultiValueMap<String, String> editConsumableMap = new LinkedMultiValueMap<>();
		editConsumableMap.add("name", "doubleNameConsumable2");
		editConsumableMap.add("price", "12.5");

		mvc.perform(post("/inventory/editConsumable/" + toEdit.getId()).params(editConsumableMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "ShopInventoryProductCreationForm.notUnique.name"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editConsumablePriceValidationTest() throws Exception {
		ConsumableProduct toEdit = catalogManagement.createConsumableProduct("Consumable102", "10.2", "something else");

		MultiValueMap<String, String> editConsumableMap = new LinkedMultiValueMap<>();
		editConsumableMap.add("name", "wrongPriceConsumable");
		editConsumableMap.add("price", "12. 3");


		mvc.perform(post("/inventory/editConsumable/" + toEdit.getId()).params(editConsumableMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editConsumableBlankTest() throws Exception {
		ConsumableProduct toEdit = catalogManagement.createConsumableProduct("Consumable103", "10.33", "some Ingredient");
		MultiValueMap<String, String> editConsumableMap = new LinkedMultiValueMap<>();
		editConsumableMap.add("name", "");
		editConsumableMap.add("price", "");
		mvc.perform(post("/inventory/editConsumable/" + toEdit.getId()).params(editConsumableMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "NotBlank"))
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "NotBlank"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editConsumableNothingChangedTest() throws Exception {
		String name = "sameConsumable";
		String price = "10.50";
		String ingredients = "same ingredients";

		ConsumableProduct sameConsumable = catalogManagement.createConsumableProduct(name, price, ingredients);

		MultiValueMap<String, String> editConsumableMap = new LinkedMultiValueMap<>();
		editConsumableMap.add("name", name);
		editConsumableMap.add("price", price);
		editConsumableMap.add("ingredients",ingredients);

		mvc.perform(post("/inventory/editConsumable/" + sameConsumable.getId()).params(editConsumableMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"))
				.andExpect(flash().attribute("InventoryActionResult", "editUnchanged"));

	}

	//Drink

	@Test
	@WithMockUser(roles = "BOSS")
	void editDrinkGetTest() throws Exception {
		ConsumableProductCreationForm creationForm = new ConsumableProductCreationForm("testDrink", "4.3", "water");
		ConsumableProduct product = catalogManagement.createDrinkProduct(creationForm.getName(), creationForm.getPrice(), creationForm.getIngredients());
		ProductIdentifier id = product.getId();
		mvc.perform(get("/inventory/editDrink/" + id))
				.andExpect(status().isOk())
				.andExpect(model().attribute("form", allOf(
						hasProperty("name", is(creationForm.getName())),
						hasProperty("price", is(creationForm.getPrice())),
						hasProperty("ingredients", is(creationForm.getIngredients()))
				)));

		//test if product, that does not exist gets recognized as such
		mvc.perform(get("/inventory/editDrink/" + 1337))
				.andExpect(status().isOk())
				.andExpect(model().attribute("productFound", is(false)));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editDrinkPostTest() throws Exception {
		ConsumableProductCreationForm creationForm = new ConsumableProductCreationForm("testDrink", "4.3", "water");
		ConsumableProduct product = catalogManagement.createDrinkProduct(creationForm.getName(), creationForm.getPrice(), creationForm.getIngredients());
		ProductIdentifier id = product.getId();

		MultiValueMap<String, String> editDrinkMap = new LinkedMultiValueMap<>();
		editDrinkMap.add("name", "Drink1");
		editDrinkMap.add("price", "4.53");
		editDrinkMap.add("ingredients", "nothing");

		mvc.perform(post("/inventory/editDrink/" + id).params(editDrinkMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"));

		//compare values with db
		ConsumableProduct dbDrink = (ConsumableProduct) catalogManagement.findById(id);
		assertEquals(dbDrink.getName(), editDrinkMap.toSingleValueMap().get("name"));
		DecimalFormat df = new DecimalFormat("#.##");
		assertEquals(df.format(dbDrink.getPrice().getNumber()), editDrinkMap.toSingleValueMap().get("price"));
		assertEquals(dbDrink.getIngredients(), editDrinkMap.toSingleValueMap().get("ingredients"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editDrinkDoubleNameTest() throws Exception {
		ConsumableProduct toEdit = catalogManagement.createConsumableProduct("Drink101", "1", "something");

		catalogManagement.createDrinkProduct("doubleNameDrink2", "10", "something");

		MultiValueMap<String, String> editDrinkMap = new LinkedMultiValueMap<>();
		editDrinkMap.add("name", "doubleNameDrink2");
		editDrinkMap.add("price", "2.5");

		mvc.perform(post("/inventory/editDrink/" + toEdit.getId()).params(editDrinkMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "ShopInventoryProductCreationForm.notUnique.name"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editDrinkPriceValidationTest() throws Exception {
		ConsumableProduct toEdit = catalogManagement.createConsumableProduct("Drink102", "10.2", "something else");

		MultiValueMap<String, String> editDrinkMap = new LinkedMultiValueMap<>();
		editDrinkMap.add("name", "wrongPriceDrink");
		editDrinkMap.add("price", "2. 3");


		mvc.perform(post("/inventory/editDrink/" + toEdit.getId()).params(editDrinkMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editDrinkBlankTest() throws Exception {
		ConsumableProduct toEdit = catalogManagement.createConsumableProduct("Drink103", "1.33", "some Ingredient");
		MultiValueMap<String, String> editConsumableMap = new LinkedMultiValueMap<>();
		editConsumableMap.add("name", "");
		editConsumableMap.add("price", "");
		mvc.perform(post("/inventory/editDrink/" + toEdit.getId()).params(editConsumableMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "NotBlank"))
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "NotBlank"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editDrinkNothingChangedTest() throws Exception {
		String name = "sameDrink";
		String price = "1.50";
		String ingredients = "same ingredients";

		ConsumableProduct sameDrink = catalogManagement.createConsumableProduct(name, price, ingredients);

		MultiValueMap<String, String> editDrinkMap = new LinkedMultiValueMap<>();
		editDrinkMap.add("name", name);
		editDrinkMap.add("price", price);
		editDrinkMap.add("ingredients",ingredients);

		mvc.perform(post("/inventory/editConsumable/" + sameDrink.getId()).params(editDrinkMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"))
				.andExpect(flash().attribute("InventoryActionResult", "editUnchanged"));

	}

	//Topping

	@Test
	@WithMockUser(roles = "BOSS")
	void editToppingGetTest() throws Exception {
		ToppingProductCreationForm creationForm = new ToppingProductCreationForm("testTopping", "2");
		ToppingProduct product = catalogManagement.createToppingProduct(creationForm.getName(), creationForm.getPrice());
		ProductIdentifier id = product.getId();
		mvc.perform(get("/inventory/editTopping/" + id))
				.andExpect(status().isOk())
				.andExpect(model().attribute("form", allOf(
						hasProperty("name", is(creationForm.getName())),
						hasProperty("price", is(creationForm.getPrice()))
				)));

		//test if product, that does not exist gets recognized as such
		mvc.perform(get("/inventory/editTopping/" + 1337))
				.andExpect(status().isOk())
				.andExpect(model().attribute("productFound", is(false)));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editToppingPostTest() throws Exception {
		ToppingProductCreationForm creationForm = new ToppingProductCreationForm("testTopping", "2");
		ToppingProduct product = catalogManagement.createToppingProduct(creationForm.getName(), creationForm.getPrice());
		ProductIdentifier id = product.getId();

		MultiValueMap<String, String> editToppingMap = new LinkedMultiValueMap<>();
		editToppingMap.add("name", "Topping1");
		editToppingMap.add("price", "2");


		mvc.perform(post("/inventory/editTopping/" + id).params(editToppingMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"));

		//compare values with db
		ToppingProduct dbTopping = (ToppingProduct) catalogManagement.findById(id);
		assertEquals(dbTopping.getName(), editToppingMap.toSingleValueMap().get("name"));
		DecimalFormat df = new DecimalFormat("#.##");
		assertEquals(df.format(dbTopping.getPrice().getNumber()), editToppingMap.toSingleValueMap().get("price"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editToppingDoubleNameTest() throws Exception {
		ToppingProduct toEdit = catalogManagement.createToppingProduct("Topping101", "1.0");

		catalogManagement.createToppingProduct("doubleNameTopping2", "1.0");

		MultiValueMap<String, String> editToppingMap = new LinkedMultiValueMap<>();
		editToppingMap.add("name", "doubleNameTopping2");
		editToppingMap.add("price", "12.5");

		mvc.perform(post("/inventory/editTopping/" + toEdit.getId()).params(editToppingMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "ShopInventoryProductCreationForm.notUnique.name"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editToppingPriceValidationTest() throws Exception {
		ToppingProduct toEdit = catalogManagement.createToppingProduct("Topping102", "1.2");

		MultiValueMap<String, String> editToppingMap = new LinkedMultiValueMap<>();
		editToppingMap.add("name", "wrongPriceTopping");
		editToppingMap.add("price", "1. 3");


		mvc.perform(post("/inventory/editTopping/" + toEdit.getId()).params(editToppingMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editToppingBlankTest() throws Exception {
		ToppingProduct toEdit = catalogManagement.createToppingProduct("Topping103", "1.33");
		MultiValueMap<String, String> editToppingMap = new LinkedMultiValueMap<>();
		editToppingMap.add("name", "");
		editToppingMap.add("price", "");
		mvc.perform(post("/inventory/editTopping/" + toEdit.getId()).params(editToppingMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "NotBlank"))
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "NotBlank"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editToppingNothingChangedTest() throws Exception {
		String name = "sameTopping";
		String price = "1.50";


		ToppingProduct sameTopping = catalogManagement.createToppingProduct(name, price);

		MultiValueMap<String, String> editToppingMap = new LinkedMultiValueMap<>();
		editToppingMap.add("name", name);
		editToppingMap.add("price", price);


		mvc.perform(post("/inventory/editTopping/" + sameTopping.getId()).params(editToppingMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"))
				.andExpect(flash().attribute("InventoryActionResult", "editUnchanged"));

	}

	//Vehicle

	@Test
	@WithMockUser(roles = "BOSS")
	void editVehicleGetTest() throws Exception {
		VehicleProductCreationForm creationForm = new VehicleProductCreationForm("testVehicle", "36000", "56");
		VehicleProduct product = catalogManagement.createVehicleProduct(creationForm.getName(), creationForm.getPrice(), creationForm.getSlots());
		ProductIdentifier id = product.getId();
		mvc.perform(get("/inventory/editVehicle/" + id))
				.andExpect(status().isOk())
				.andExpect(model().attribute("form", allOf(
						hasProperty("name", is(creationForm.getName())),
						hasProperty("price", is(creationForm.getPrice())),
						hasProperty("slots", is(creationForm.getSlots()))
				)));

		//test if product, that does not exist gets recognized as such
		mvc.perform(get("/inventory/editVehicle/" + 1337))
				.andExpect(status().isOk())
				.andExpect(model().attribute("productFound", is(false)));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editVehiclePostTest() throws Exception {
		VehicleProductCreationForm creationForm = new VehicleProductCreationForm("testVehicle", "45000", "56");
		VehicleProduct product = catalogManagement.createVehicleProduct(creationForm.getName(), creationForm.getPrice(), creationForm.getSlots());
		ProductIdentifier id = product.getId();

		MultiValueMap<String, String> editVehicleMap = new LinkedMultiValueMap<>();
		editVehicleMap.add("name", "Vehicle1");
		editVehicleMap.add("price", "36000");
		editVehicleMap.add("slots", "56");

		mvc.perform(post("/inventory/editVehicle/" + id).params(editVehicleMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"));

		//compare values with db
		VehicleProduct dbVehicle = (VehicleProduct) catalogManagement.findById(id);
		assertEquals(dbVehicle.getName(), editVehicleMap.toSingleValueMap().get("name"));
		DecimalFormat df = new DecimalFormat("#.##");
		assertEquals(df.format(dbVehicle.getPrice().getNumber()), editVehicleMap.toSingleValueMap().get("price"));
		assertEquals(String.valueOf(dbVehicle.getSlots()), editVehicleMap.toSingleValueMap().get("slots"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editVehicleDoubleNameTest() throws Exception {
		VehicleProduct toEdit = catalogManagement.createVehicleProduct("Vehicle101", "10000", "12");

		catalogManagement.createVehicleProduct("doubleNameVehicle2", "10000", "13");

		MultiValueMap<String, String> editVehicleMap = new LinkedMultiValueMap<>();
		editVehicleMap.add("name", "doubleNameVehicle2");
		editVehicleMap.add("price", "12.5");
		editVehicleMap.add("slots","14");

		mvc.perform(post("/inventory/editVehicle/" + toEdit.getId()).params(editVehicleMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "ShopInventoryProductCreationForm.notUnique.name"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editVehiclePriceValidationTest() throws Exception {
		VehicleProduct toEdit = catalogManagement.createVehicleProduct("Vehicle102", "10200", "12");

		MultiValueMap<String, String> editVehicleMap = new LinkedMultiValueMap<>();
		editVehicleMap.add("name", "wrongPriceVehicle");
		editVehicleMap.add("price", "12.000.00");
		editVehicleMap.add("slots","12");

		mvc.perform(post("/inventory/editVehicle/" + toEdit.getId()).params(editVehicleMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editVehicleSlotsValidationTest() throws Exception {
		VehicleProduct toEdit = catalogManagement.createVehicleProduct("Vehicle103", "10200", "12");

		MultiValueMap<String, String> editVehicleMap = new LinkedMultiValueMap<>();
		editVehicleMap.add("name", "wrongSlotsVehicle");
		editVehicleMap.add("price", "12000");
		editVehicleMap.add("slots","0");

		mvc.perform(post("/inventory/editVehicle/" + toEdit.getId()).params(editVehicleMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "slots", "ShopInventoryProductCreationForm.wrongPattern.slots"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editVehicleBlankTest() throws Exception {
		VehicleProduct toEdit = catalogManagement.createVehicleProduct("Vehicle103", "10330", "25");
		MultiValueMap<String, String> editVehicleMap = new LinkedMultiValueMap<>();
		editVehicleMap.add("name", "");
		editVehicleMap.add("price", "");
		editVehicleMap.add("slots","");
		mvc.perform(post("/inventory/editVehicle/" + toEdit.getId()).params(editVehicleMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "NotBlank"))
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "NotBlank"))
				.andExpect(model().attributeHasFieldErrorCode("form", "slots", "NotBlank"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editVehicleNothingChangedTest() throws Exception {
		String name = "sameVehicle";
		String price = "10500";
		String slots = "34";

		VehicleProduct sameVehicle = catalogManagement.createVehicleProduct(name, price, slots);

		MultiValueMap<String, String> editVehicleMap = new LinkedMultiValueMap<>();
		editVehicleMap.add("name", name);
		editVehicleMap.add("price", price);
		editVehicleMap.add("slots",slots);

		mvc.perform(post("/inventory/editVehicle/" + sameVehicle.getId()).params(editVehicleMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"))
				.andExpect(flash().attribute("InventoryActionResult", "editUnchanged"));

	}


	//Dishset

	@Test
	@WithMockUser(roles = "BOSS")
	void editDishsetGetTest() throws Exception {
		DishsetProductCreationForm creationForm = new DishsetProductCreationForm("testDishset", "15");
		DishsetProduct product = catalogManagement.createDishsetProduct(creationForm.getName(), creationForm.getPrice());
		ProductIdentifier id = product.getId();
		mvc.perform(get("/inventory/editDishset/" + id))
				.andExpect(status().isOk())
				.andExpect(model().attribute("form", allOf(
						hasProperty("name", is(creationForm.getName())),
						hasProperty("price", is(creationForm.getPrice()))
				)));

		//test if product, that does not exist gets recognized as such
		mvc.perform(get("/inventory/editDishset/" + 1337))
				.andExpect(status().isOk())
				.andExpect(model().attribute("productFound", is(false)));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editDishsetPostTest() throws Exception {
		DishsetProductCreationForm creationForm = new DishsetProductCreationForm("testDishset", "15");
		DishsetProduct product = catalogManagement.createDishsetProduct(creationForm.getName(), creationForm.getPrice());
		ProductIdentifier id = product.getId();

		MultiValueMap<String, String> editDishsetMap = new LinkedMultiValueMap<>();
		editDishsetMap.add("name", "Dishset001");
		editDishsetMap.add("price", "16");


		mvc.perform(post("/inventory/editDishset/" + id).params(editDishsetMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"));

		//compare values with db
		DishsetProduct dbDishset = (DishsetProduct) catalogManagement.findById(id);
		assertEquals(dbDishset.getName(), editDishsetMap.toSingleValueMap().get("name"));
		DecimalFormat df = new DecimalFormat("#.##");
		assertEquals(df.format(dbDishset.getPrice().getNumber()), editDishsetMap.toSingleValueMap().get("price"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editDishsetDoubleNameTest() throws Exception {
		DishsetProduct toEdit = catalogManagement.createDishsetProduct("Dishset101", "10");

		catalogManagement.createDishsetProduct("doubleNameDishset2", "10");

		MultiValueMap<String, String> editDishsetMap = new LinkedMultiValueMap<>();
		editDishsetMap.add("name", "doubleNameDishset2");
		editDishsetMap.add("price", "12.5");

		mvc.perform(post("/inventory/editDishset/" + toEdit.getId()).params(editDishsetMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "ShopInventoryProductCreationForm.notUnique.name"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editDishsetPriceValidationTest() throws Exception {
		DishsetProduct toEdit = catalogManagement.createDishsetProduct("Dishset102", "12");

		MultiValueMap<String, String> editDishsetMap = new LinkedMultiValueMap<>();
		editDishsetMap.add("name", "wrongPriceDishset");
		editDishsetMap.add("price", "10. 3");


		mvc.perform(post("/inventory/editDishset/" + toEdit.getId()).params(editDishsetMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editDishsetBlankTest() throws Exception {
		DishsetProduct toEdit = catalogManagement.createDishsetProduct("Dishset103", "13.3");
		MultiValueMap<String, String> editDishsetMap = new LinkedMultiValueMap<>();
		editDishsetMap.add("name", "");
		editDishsetMap.add("price", "");
		mvc.perform(post("/inventory/editDishset/" + toEdit.getId()).params(editDishsetMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "NotBlank"))
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "NotBlank"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editDishsetNothingChangedTest() throws Exception {
		String name = "sameDishset";
		String price = "15";


		DishsetProduct sameDishset = catalogManagement.createDishsetProduct(name, price);

		MultiValueMap<String, String> editDishsetMap = new LinkedMultiValueMap<>();
		editDishsetMap.add("name", name);
		editDishsetMap.add("price", price);


		mvc.perform(post("/inventory/editDishset/" + sameDishset.getId()).params(editDishsetMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"))
				.andExpect(flash().attribute("InventoryActionResult", "editUnchanged"));

	}


	//Oven

	@Test
	@WithMockUser(roles = "BOSS")
	void editOvenGetTest() throws Exception {
		OvenProductCreationForm creationForm = new OvenProductCreationForm("testOven01", "15");
		OvenProduct product = catalogManagement.createOvenProduct(creationForm.getName(), creationForm.getPrice());
		ProductIdentifier id = product.getId();
		mvc.perform(get("/inventory/editOven/" + id))
				.andExpect(status().isOk())
				.andExpect(model().attribute("form", allOf(
						hasProperty("price", is(creationForm.getPrice())),
						hasProperty("name" , is(creationForm.getName()))
				)));

		//test if product, that does not exist gets recognized as such
		mvc.perform(get("/inventory/editOven/" + 1337))
				.andExpect(status().isOk())
				.andExpect(model().attribute("productFound", is(false)));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editOvenPostTest() throws Exception {
		OvenProductCreationForm creationForm = new OvenProductCreationForm("testOven02", "1500");
		OvenProduct product = catalogManagement.createOvenProduct(creationForm.getName(), creationForm.getPrice());
		ProductIdentifier id = product.getId();

		MultiValueMap<String, String> editOvenMap = new LinkedMultiValueMap<>();
		editOvenMap.add("name", product.getName());
		editOvenMap.add("price", "1700");


		mvc.perform(post("/inventory/editOven/" + id).params(editOvenMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"));

		//compare values with db
		OvenProduct dbOven = (OvenProduct) catalogManagement.findById(id);
		assertEquals(dbOven.getName(), editOvenMap.toSingleValueMap().get("name"));
		DecimalFormat df = new DecimalFormat("#.##");
		assertEquals(df.format(dbOven.getPrice().getNumber()), editOvenMap.toSingleValueMap().get("price"));
	}


	@Test
	@WithMockUser(roles = "BOSS")
	void editOvenDoubleNameTest() throws Exception {
		OvenProduct toEdit = catalogManagement.createOvenProduct("Oven101", "1.0");
		String doubleName = "doubleNameOven2";

		catalogManagement.createOvenProduct(doubleName, "1.0");

		MultiValueMap<String, String> editOvenMap = new LinkedMultiValueMap<>();
		editOvenMap.add("name", doubleName);
		editOvenMap.add("price", "12.5");

		mvc.perform(post("/inventory/editOven/" + toEdit.getId()).params(editOvenMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "name", "ShopInventoryProductCreationForm.notUnique.name"));

	}


	@Test
	@WithMockUser(roles = "BOSS")
	void editOvenPriceValidationTest() throws Exception {
		OvenProduct toEdit = catalogManagement.createOvenProduct( "priceValidationTestOven","1200");

		MultiValueMap<String, String> editOvenMap = new LinkedMultiValueMap<>();
		editOvenMap.add("name", "priceValidationTestOven");
		editOvenMap.add("price", "1346. 3");


		mvc.perform(post("/inventory/editOven/" + toEdit.getId()).params(editOvenMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "ShopInventoryProductCreationForm.wrongPattern.price"));
	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editOvenBlankTest() throws Exception {
		OvenProduct toEdit = catalogManagement.createOvenProduct("blankTestOven","1235.33");
		MultiValueMap<String, String> editOvenMap = new LinkedMultiValueMap<>();
		editOvenMap.add("name", "");
		editOvenMap.add("price", "");
		mvc.perform(post("/inventory/editOven/" + toEdit.getId()).params(editOvenMap))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrorCode("form", "price", "NotBlank"));

	}

	@Test
	@WithMockUser(roles = "BOSS")
	void editOvenNothingChangedTest() throws Exception {
		String price = "1350";
		String name = "nothingChangedOven";


		OvenProduct sameOven = catalogManagement.createOvenProduct(name, price);

		MultiValueMap<String, String> editOvenMap = new LinkedMultiValueMap<>();
		editOvenMap.add("price", price);
		editOvenMap.add("name", name);


		mvc.perform(post("/inventory/editOven/" + sameOven.getId()).params(editOvenMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"))
				.andExpect(flash().attribute("InventoryActionResult", "editUnchanged"));

	}

	//deleteTest

	@Test
	@WithMockUser(roles = "BOSS")
	void deleteProductTest() throws Exception {
		ToppingProduct topping = catalogManagement.createToppingProduct("TestTopping", "1.99");
		ProductIdentifier id = topping.getId();

		//check if Product is in db and has the correct category
		assertNotNull(catalogManagement.findById(id));
		assertTrue(catalogManagement.findById(id).getCategories().toList().contains(ProductCategory.TOPPING.name()));

		mvc.perform(post("/inventory/del/" + id))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/inventory"));

		//check if Product is still present and has the "deleted" category
		assertNotNull(catalogManagement.findById(id));
		assertTrue(catalogManagement.findById(id).getCategories().toList().contains(ProductCategory.DELETED.name()));

		mvc.perform(post("/inventory/del" + id))
				.andExpect(status().isNotFound());

	}

	//check coverage to see if these two additional tests are needed, because it's really laborious and probably not worth it
	@Test
	@WithMockUser(roles = "BOSS")
	@Disabled
	void deleteAssignedOvenTest() throws Exception{
		/*TODO
		create Oven
		create ShopOrder
		put Orderline with Pizza in it
		check what Oven it was assigned to
		try to delete that Oven
		check if redirected with correct flash attribute
		check if ProductCategory is still "OVEN" and not "DELETED"
		 */
	}

	@Test
	@WithMockUser(roles = "BOSS")
	@Disabled
	void deleteAssignedVehicleTest() throws Exception{
		/*
		create Driver and Vehicle to assure, that at least one driver and Vehicle is initialized (to not rely on initializer)
		create ShopOrder
		put Orderline with something in it
		specify pickup-type to be Delivery
		check driver, that was assigned
		check Vehicle of driver (=> can't create a Vehicle for this, because deliver influences what is chosen and
		that might not be the vehicle here...)
		try to delete that vehicle
		check if redirected with correct flash attribute
		check if ProductCategory is still "VEHICLE" and not "DELETED"
		 */
	}




}
