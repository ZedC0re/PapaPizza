package papapizza.inventory;

import lombok.NonNull;
import org.salespointframework.core.DataInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import papapizza.employee.Employee;
import papapizza.employee.EmployeeManagement;
import papapizza.inventory.items.OvenProduct;
import papapizza.inventory.items.ToppingProduct;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Component, that initializes products in the repository {@link ShopCatalog}
 */
@Component
public class ShopCatalogInitializer implements DataInitializer {

	private final Logger logger = LoggerFactory.getLogger(ShopCatalogInitializer.class);

	private ShopCatalogManagement shopCatalogManagement;

	private EmployeeManagement employeeManagement;

	@Autowired
	public void setEmployeeManagement(@NonNull EmployeeManagement employeeManagement) {
		this.employeeManagement = employeeManagement;
	}

	@Autowired
	public void setShopCatalogManagement(@NonNull ShopCatalogManagement shopCatalogManagement){
		this.shopCatalogManagement = shopCatalogManagement;
	}

	/**
	 * Method for enitializing products in the Repository ({@link ShopCatalog})
	 */
	@Override
	public void initialize() {
		logger.info("No products in db yet, initializing ...");
		//shopCatalogManagement.saveSaladProduct(shopCatalogManagement.createSalad("salad1", Money.of(1,"EUR")));
		shopCatalogManagement.createConsumableProduct("Haribo","1.50","sugar");
		shopCatalogManagement.createDrinkProduct("Coke","2","nix");
		shopCatalogManagement.createDrinkProduct("Fanta","2","nix");
		shopCatalogManagement.createDrinkProduct("Beer","2","nix");
		shopCatalogManagement.createConsumableProduct("Chips","2","nix");
		shopCatalogManagement.createConsumableProduct("Soup","2","nix");
		shopCatalogManagement.createConsumableProduct("Salad","2","nix");
		shopCatalogManagement.createConsumableProduct("more Salad","2","nix");
		shopCatalogManagement.createConsumableProduct("even more Salad","2","nix");
		shopCatalogManagement.createConsumableProduct("infinite Salad","2","nix");
		shopCatalogManagement.createConsumableProduct("Eggs","2","nix");
		shopCatalogManagement.createDishsetProduct("Dishset1","15");
		shopCatalogManagement.createToppingProduct("Salami","5");
		shopCatalogManagement.createToppingProduct("Onions","0.1");
		shopCatalogManagement.createToppingProduct("Ham","0.7");
		shopCatalogManagement.createToppingProduct("Gold","300");
		shopCatalogManagement.createPizzaProduct(
				"pizza1","5", Collections.singletonList(
						(ToppingProduct) shopCatalogManagement.findByCategory(ProductCategory.TOPPING.toString()).
															  stream().findFirst().get()));
		shopCatalogManagement.createToppingProduct("Pineapple","0.60");
		shopCatalogManagement.createVehicleProduct("CL500","85000","4");
		shopCatalogManagement.createVehicleProduct("LaFerrari","15000000","1");
		shopCatalogManagement.createVehicleProduct("Bulli","45000","30");
		shopCatalogManagement.createVehicleProduct("Golf","30000","10");
		shopCatalogManagement.createToppingProduct("Mozarella","1");
		shopCatalogManagement.createToppingProduct("Cheddar","1");
		shopCatalogManagement.createToppingProduct("Champions","0.60");

		List<Employee> chefs = employeeManagement.findByRole("Chef").toList();
		OvenProduct oven1 = shopCatalogManagement.createOvenProduct("Amica EH 923 620 E Oven","1500");
		oven1.setChef(chefs.get(0));
		shopCatalogManagement.save(oven1);

		OvenProduct oven2 = shopCatalogManagement.createOvenProduct("Oven01","1600");
		oven2.setChef(chefs.get(1));
		shopCatalogManagement.save(oven2);

		OvenProduct oven3 = shopCatalogManagement.createOvenProduct("Miele Twinset Comfort oven","1700");
		oven3.setChef(chefs.get(2));
		shopCatalogManagement.save(oven3);

		OvenProduct oven4 = shopCatalogManagement.createOvenProduct("Miele Twinset Comfort oven 2","1700");
		shopCatalogManagement.save(oven4);

		OvenProduct oven5 = shopCatalogManagement.createOvenProduct("Siemens HK9R30250 oven","1700");
		shopCatalogManagement.save(oven5);


		List<ToppingProduct> toppings = new LinkedList<ToppingProduct>();
		toppings.add((ToppingProduct) shopCatalogManagement.findByName("Pineapple"));

		shopCatalogManagement.createPizzaProduct("Hawaii","12.50",toppings);
		logger.info("Product count:"+shopCatalogManagement.findAll().toList().size());

		shopCatalogManagement.setInitialized(true);
	}
}
