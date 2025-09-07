package papapizza.inventory;


import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.salespointframework.catalog.ProductIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import papapizza.inventory.creationForms.*;
import papapizza.inventory.items.*;

import javax.transaction.Transactional;
import java.util.*;


import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Transactional
public class ShopCatalogManagementTest {

	private final ShopCatalogManagement shopCatalogManagement;

	private PizzaProduct testPizza;
	private ToppingProduct testTopping;




	@Autowired
	public ShopCatalogManagementTest(ShopCatalogManagement shopCatalogManagement){
		this.shopCatalogManagement = shopCatalogManagement;
		//createTestPizzaProduct(); //@Before annotations or something?
		//createTestTopping();
	}
	@BeforeEach
	private void createTestPizzaProduct(){
		Map<ToppingProduct, Boolean> toppings = new HashMap<>();
		toppings.put(shopCatalogManagement.createToppingProduct("Ham","1.50"),true);
		PizzaProductCreationForm form = new PizzaProductCreationForm("Hawaii","12.534",toppings); //check if this works...
		testPizza = shopCatalogManagement.createPizzaProduct(form.getName(),form.getPrice(), shopCatalogManagement.ToppingMapToList(form));

	}
	@BeforeEach
	private void createTestTopping(){
		ToppingProductCreationForm form = new ToppingProductCreationForm("Pinapple","1.60");
		testTopping = shopCatalogManagement.createToppingProduct(form.getName(),form.getPrice());
	}


	//
	//creationTests
	//


	@Test
	void createToppingProductTest(){
		ToppingProductCreationForm form = new ToppingProductCreationForm("testTopping","2.50");
		ToppingProduct testTopping2 = shopCatalogManagement.createToppingProduct(form.getName(),form.getPrice());
		try {
			shopCatalogManagement.findById(testTopping2.getId());
		}
		catch (NoSuchElementException e){
			fail();
		}
	}

	@Test
	void createVehicleProductTest(){
		VehicleProductCreationForm form = new VehicleProductCreationForm("testVehicle", "30000","19");
		VehicleProduct testVehicle = shopCatalogManagement.createVehicleProduct(form.getName(),form.getPrice(), form.getSlots());
		try {
			shopCatalogManagement.findById(testVehicle.getId());
		}
		catch (NoSuchElementException e){
			fail();
		}
	}

	@Test
	void createConsumableProductTest(){
		ConsumableProductCreationForm form = new ConsumableProductCreationForm("testConsumable", "4.3", "nix");
		ConsumableProduct testConsumable = shopCatalogManagement.createConsumableProduct(form.getName(), form.getPrice(), form.getIngredients());
		try {
			shopCatalogManagement.findById(testConsumable.getId());
		}
		catch (NoSuchElementException e){
			fail();
		}
	}

	@Test
	void createDrinkProductTest(){
		ConsumableProduct testDrink = shopCatalogManagement.createDrinkProduct("test", "4.30", "nix");
		try {
			shopCatalogManagement.findById(testDrink.getId());
		}
		catch (NoSuchElementException e){
			fail();
		}
	}

	@Test
	void createDishsetProductTest(){
		DishsetProductCreationForm form = new DishsetProductCreationForm("testSet", "30");
		DishsetProduct testDishset = shopCatalogManagement.createDishsetProduct(form.getName(), form.getPrice());
		try {
			shopCatalogManagement.findById(testDishset.getId());
		}
		catch (NoSuchElementException e){
			fail();
		}
	}

	@Test
	void createOvenProductTest(){
		OvenProductCreationForm form = new OvenProductCreationForm("testOven","2300");
		OvenProduct testOven = shopCatalogManagement.createOvenProduct( form.getName(), form.getPrice());
		try {
			shopCatalogManagement.findById(testOven.getId());
		}
		catch (NoSuchElementException e){
			fail();
		}
	}

	@Test
	void createPizzaProductTest(){
		List<ToppingProduct> toppings = new ArrayList<>();
		toppings.add(shopCatalogManagement.createToppingProduct("Ham","1.50"));
		toppings.add(shopCatalogManagement.createToppingProduct("Cheese","1.20"));
		PizzaProduct testPizza2 = shopCatalogManagement.createPizzaProduct("testPizza2","14.4",toppings);
		try {
			shopCatalogManagement.findById(testPizza2.getId());
		}
		catch (NoSuchElementException e){
			fail();
		}
	}

	@Test
	void createPizzaProductTest2(){
		List<ToppingProduct> toppings = new ArrayList<>();
		toppings.add(shopCatalogManagement.createToppingProduct("Ham","1.50"));
		toppings.add(shopCatalogManagement.createToppingProduct("Cheese","1.20"));
		System.out.println("toppings:"+toppings);
		PizzaProduct testPizza3 = shopCatalogManagement.createPizzaProduct(toppings);
		ProductIdentifier PizzaID = testPizza3.getId();
		try {
			shopCatalogManagement.findById(PizzaID);
		}
		catch (NoSuchElementException e){
			fail();
		}
		//test pricecalc
		System.out.println(shopCatalogManagement.findById(PizzaID).getPrice());
		assertTrue(shopCatalogManagement.findById(PizzaID).getPrice().isEqualTo(Money.of(8.7,"EUR")));
	}


	//
	//deletiontests
	//

	@Test
	void deleteById(){
		ProductIdentifier Id = testTopping.getId();

		shopCatalogManagement.deleteById(Id); //can somebody tell me why this isn't working?
		System.out.println("Categories "+ testTopping.getCategories().toList());
		assertFalse(shopCatalogManagement.findById(Id).getCategories().stream().anyMatch("TOPPING"::equals));
		assertTrue(shopCatalogManagement.findById(Id).getCategories().stream().allMatch("DELETED"::equals));
	}


	@Test
	void deleteHardByIdTest(){
		ProductIdentifier Id = testTopping.getId();
		shopCatalogManagement.deleteHardById(Id);
		assertNull(shopCatalogManagement.findById(Id));
		//assertThrows(NoSuchElementException.class, () -> shopCatalogManagement.findById(Id));

	}



}
