package papapizza.kitchen;

import com.google.j2objc.annotations.AutoreleasePool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.salespointframework.catalog.ProductIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import papapizza.inventory.ProductCategory;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.inventory.items.OvenProduct;
import papapizza.inventory.items.PizzaProduct;
import papapizza.inventory.items.PizzaState;
import papapizza.order.ShopOrder;
import papapizza.order.ShopOrderManagement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class KitchenManagementTest {

	@Autowired
	ShopCatalogManagement shopCatalogManagement;

	@Autowired
	ShopOrderManagement<ShopOrder> shopOrderManagement;

	@Autowired
	KitchenManagement kitchenManagement;

	@Test
	public void timeLeftTest() {
		assertTrue(shopCatalogManagement.findByCategory(ProductCategory.OVEN.toString()).stream().findAny().isPresent());
		assertTrue(shopCatalogManagement.findByCategory(ProductCategory.PIZZA.toString()).stream().findAny().isPresent());
		OvenProduct oven = (OvenProduct) shopCatalogManagement.findByCategory(ProductCategory.OVEN.toString()).stream().findAny().get();
		PizzaProduct pizzaProduct = (PizzaProduct) shopCatalogManagement.findByCategory(ProductCategory.PIZZA.toString()).stream().findAny().get();
		pizzaProduct.setState(PizzaState.READY);
		oven.setPizzas(List.of(pizzaProduct));
		kitchenManagement.getTimeLeft(pizzaProduct);
	}

	@Test
	public void changePizzaStateTest() {
		assertTrue(shopCatalogManagement.findByCategory(ProductCategory.PIZZA.toString()).stream().findAny().isPresent());
		assertTrue(shopCatalogManagement.findByCategory(ProductCategory.OVEN.toString()).stream().findAny().isPresent());
		PizzaProduct pizzaProduct = (PizzaProduct) shopCatalogManagement.findByCategory(ProductCategory.PIZZA.toString()).stream().findAny().get();
		OvenProduct oven = (OvenProduct) shopCatalogManagement.findByCategory(ProductCategory.OVEN.toString()).stream().findAny().get();
		oven.setPizzas(List.of(pizzaProduct));
		kitchenManagement.changePizzaState(pizzaProduct.getId(), PizzaState.PENDING);
		kitchenManagement.changePizzaState(pizzaProduct.getId(), PizzaState.READY);
	}

/*FIXME
	@Test
	public void startBakingTest(){
		assertTrue(shopCatalogManagement.findByCategory(ProductCategory.PIZZA.toString()).stream().findAny().isPresent());
		assertTrue(shopCatalogManagement.findByCategory(ProductCategory.OVEN.toString()).stream().findAny().isPresent());
		PizzaProduct pizzaProduct = (PizzaProduct) shopCatalogManagement.findByCategory(ProductCategory.PIZZA.toString()).stream().findAny().get();
		OvenProduct oven = (OvenProduct) shopCatalogManagement.findByCategory(ProductCategory.OVEN.toString()).stream().findAny().get();
		kitchenManagement.startBaking(pizzaProduct, oven);
	}


 */

	/*@Test
	public void checkOrderReadyTest(){
		assertTrue(shopOrderManagement.findAll().stream().findAny().isPresent());
		ShopOrder shopOrder = shopOrderManagement.findAll().stream().findAny().get();
		kitchenManagement.checkOrderReady(shopOrder);
	}*/

}
