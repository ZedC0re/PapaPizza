package papapizza.inventory;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.javamoney.moneta.Money;
import org.salespointframework.catalog.Product;
import org.salespointframework.catalog.ProductIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import papapizza.employee.Employee;
import papapizza.employee.EmployeeManagement;
import papapizza.inventory.creationForms.*;
import papapizza.inventory.items.*;

import javax.money.MonetaryAmount;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages all repository ({@link ShopCatalog}) based operations and additional management methods for the inventory
 */

@Service
@Transactional
public class ShopCatalogManagement {

	private final Logger logger = LoggerFactory.getLogger(ShopCatalogManagement.class);


	@Getter
	@Setter
	private boolean initialized = false;

	private final ShopCatalog shopCatalog;

	private static final int CUSTOM_PIZZA_BASE_PRICE = 6;

	private EmployeeManagement employeeManagement;

	@Autowired
	public ShopCatalogManagement(@NonNull ShopCatalog shopCatalog) {
		this.shopCatalog = shopCatalog;
	}

	@Autowired
	public void setEmployeeManagement(EmployeeManagement employeeManagement) {
		this.employeeManagement = employeeManagement;
	}

	//
	//create Methods
	//

	/**
	 * Method for creating an {@link OvenProduct}
	 *
	 * @param price price that the oven was bought for / is still worth
	 * @param name  name of the oven (e.g. model...)
	 * @return OvenProduct
	 */
	public OvenProduct createOvenProduct(String name, String price) {
		return shopCatalog.save(new OvenProduct(name, Money.of(Float.valueOf(price), "EUR")));
	}

	/**
	 * Method for creating a {@link ConsumableProduct}
	 *
	 * @param name        name of the Product
	 * @param price       price it should cost
	 * @param ingredients ingredients it contains
	 * @return ConsumableProduct
	 */
	public ConsumableProduct createConsumableProduct(String name, String price, String ingredients) {
		ConsumableProduct consumable = new ConsumableProduct(name, Money.of(Float.valueOf(price), "EUR"));
		consumable.setIngredients(ingredients);
		return shopCatalog.save(consumable);
	}

	/**
	 * Method for creating a {@link DishsetProduct} (Dish Set)
	 *
	 * @param name  name of the Dish Set
	 * @param price price it should cost
	 * @return DishsetProduct
	 */
	public DishsetProduct createDishsetProduct(String name, String price) {
		return shopCatalog.save(new DishsetProduct(name, Money.of(Float.valueOf(price), "EUR")));
	}

	/**
	 * Method for creating a {@link VehicleProduct}
	 *
	 * @param name  name of the vehicle
	 * @param price price that the vehicle was bought for / is still worth
	 * @param slots amount of space, that the vehicle has to fit all kinds of (deliverable) products  in it
	 * @return VehicleProduct
	 */
	public VehicleProduct createVehicleProduct(String name, String price, String slots) {
		VehicleProduct vehicle = new VehicleProduct(name, Money.of(Float.valueOf(price), "EUR"));
		vehicle.setSlots(Integer.parseInt(slots));
		return shopCatalog.save(vehicle);
	}

	/**
	 * Method for creating a {@link ToppingProduct}
	 *
	 * @param name  name of the Topping
	 * @param price price it should cost
	 * @return ToppingProduct
	 */
	public ToppingProduct createToppingProduct(String name, String price) {
		return shopCatalog.save(new ToppingProduct(name, Money.of(Float.valueOf(price), "EUR")));
	}

	/**
	 * Method for creating a {@link PizzaProduct} as a preset
	 *
	 * @param name     name of the pizza preset
	 * @param price    price it should cost
	 * @param toppings toppings, that will be on the premade pizza
	 * @return PizzaProduct
	 */
	public PizzaProduct createPizzaProduct(String name, String price, List<ToppingProduct> toppings) {
		PizzaProduct pizza = new PizzaProduct(name, Money.of(Float.valueOf(price), "EUR"));
		pizza.setToppings(toppings);
		return shopCatalog.save(pizza);
	}

	/**
	 * Method for creating a {@link PizzaProduct} as a custom pizza
	 *
	 * @param toppings toppings, that will be on the custom pizza
	 * @return PizzaProduct
	 */
	//when creating a new pizza in order, that also gets saved in the orderrepo....
	public PizzaProduct createPizzaProduct(List<ToppingProduct> toppings) {
		MonetaryAmount price = Money.of(CUSTOM_PIZZA_BASE_PRICE, "EUR");//base price for every Pizza
		for (ToppingProduct topping : toppings) {
			price = price.add(topping.getPrice());
			logger.info(String.format("price of topping %s is %s", topping.getName(), topping.getPrice()));
		}
		logger.info("total price of custom pizza:"+price);
		PizzaProduct customPizza = new PizzaProduct("CustomPizza", price);
		customPizza.setToppings(toppings);
		customPizza.addCategory(ProductCategory.CUSTOM_PIZZA.toString());
		customPizza.removeCategory(ProductCategory.PIZZA.toString());
		//maybe check how many CustomPizzas there are Name them after the count to have a unique name (if there are
		// errors with doubling names)
		return shopCatalog.save(customPizza);
	}

	/**
	 * Method to clone a {@link PizzaProduct}:
	 * {@link PizzaProduct}s need to be cloned for the {@link papapizza.kitchen.KitchenManagement},
	 * in order to have an instance of each pizza.
	 * when cloning the {@link ProductCategory} changes form "Pizza" to "KitchenPizza"
	 *
	 * @param toClone PizzaProduct, that gets cloned
	 * @return PizzaProduct
	 */
	public PizzaProduct createKitchenCopyPizzaProduct(PizzaProduct toClone) {
		PizzaProduct clone = new PizzaProduct(toClone.getName(), toClone.getPrice());
		clone.setToppings(new ArrayList<>(toClone.getToppings()));
		clone.addCategory(ProductCategory.KITCHEN_PIZZA.name());
		clone.removeCategory(ProductCategory.PIZZA.name());
		return clone;
	}

	/**
	 * Method to create a Drink (special kind of {@link ConsumableProduct} [split for frontend purposes])
	 *
	 * @param name        name of the Drink
	 * @param price       price it should have
	 * @param ingredients ingredients it contains
	 * @return ConsumableProduct
	 */
	//to Display Drinks separately in Inventory
	public ConsumableProduct createDrinkProduct(String name, String price, String ingredients) {
		ConsumableProduct drink = new ConsumableProduct(name, Money.of(Float.valueOf(price), "EUR"));
		drink.setIngredients(ingredients);
		drink.addCategory(ProductCategory.DRINK.toString());
		drink.removeCategory(ProductCategory.CONSUMABLE.toString());
		return shopCatalog.save(drink);
	}

	//TODO @Tom

	/**
	 * Method to find a MetaVehicle
	 *
	 * @return VehicleProduct
	 */
	public VehicleProduct findMetaVehicle() {
		List<VehicleProduct> vehicles =
				this.findByCategory(ProductCategory.VEHICLE.toString()).map(
						vehicle -> (VehicleProduct) vehicle).toList();
		for (VehicleProduct vehicle : vehicles) {
			if (Objects.equals(vehicle.getMeta(), "NO_VEHICLE")) {
				return vehicle;
			}
		}
		return createMetaVehicleProduct();
	}

	//TODO @TOM

	/**
	 * Method to create a MetaVehicleProduct
	 *
	 * @return VehicleProduct
	 */
	public VehicleProduct createMetaVehicleProduct() {


		VehicleProduct metaVehicle = new VehicleProduct("No Vehicle", Money.of(0, "EUR"));
		metaVehicle.setSlots(0);
		metaVehicle.setUsedSlots(0);
		metaVehicle.setMeta("NO_VEHICLE");
		shopCatalog.save(metaVehicle);
		return metaVehicle;
	}


	//
	//save methods
	//


	/**
	 * Method for saving {@link Product}s in the Repository ({@link ShopCatalog})
	 *
	 * @param product product, that will be saved
	 * @return Product
	 */
	public Product save(Product product) {
		return shopCatalog.save(product);
	}


	/**
	 * Method to prevent saving a {@link Product}, that is null
	 *
	 * @param product product, that it's trying to save
	 * @return boolean
	 */
	public boolean trySave(Product product) {
		try {
			shopCatalog.save(product);
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}

	//
	//compare methods
	//


	/**
	 * Method for comparing if {@link ConsumableProduct} and {@link ConsumableProductCreationForm} (frontend form) are
	 * equal
	 * to determine if something changed in the edit
	 *
	 * @param product consumable that gets compared
	 * @param form    form the ConsumableProduct gets compared to
	 * @return boolean
	 */
	public boolean compareConsumable(ConsumableProduct product, ConsumableProductCreationForm form) {
		return product.getName().equals(form.getName()) &&
			   product.getPrice().isEqualTo(
					   Money.of(Float.valueOf(PriceCommaToDot(form.getPrice())), "EUR")) &&
			   product.getIngredients().equals(form.getIngredients());

	}

	/**
	 * Method for comparing if {@link OvenProduct} and {@link OvenProductCreationForm} (frontend form) are equal
	 * to determine if something changed in the edit
	 *
	 * @param product oven that gets compared
	 * @param form    form the OvenProduct gets compared to
	 * @return boolean
	 */
	public boolean compareOven(OvenProduct product, OvenProductCreationForm form) {
		return product.getName().equals(form.getName()) &&
			   product.getPrice().isEqualTo(
					   Money.of(Float.valueOf(PriceCommaToDot(form.getPrice())), "EUR"));

	}

	/**
	 * Method for comparing if {@link VehicleProduct} and {@link VehicleProductCreationForm} (frontend form) are equal
	 * to determine if something changed in the edit
	 *
	 * @param product Vehicle that gets compared
	 * @param form    form the VehicleProduct gets compared to
	 * @return boolean
	 */
	public boolean compareVehicle(VehicleProduct product, VehicleProductCreationForm form) {
		return product.getName().equals(form.getName()) && product.getPrice().isEqualTo(
				Money.of(Float.valueOf(PriceCommaToDot(form.getPrice())), "EUR")) &&
			   String.valueOf(product.getSlots()).equals(form.getSlots());

	}

	/**
	 * Method for comparing if {@link PizzaProduct} and {@link PizzaProductCreationForm} (frontend form) are equal
	 * to determine if something changed in the edit
	 *
	 * @param product Pizza that gets compared
	 * @param form    form the PizzaProduct gets compared to
	 * @return boolean
	 */
	public boolean comparePizza(PizzaProduct product, PizzaProductCreationForm form) {
		List<ToppingProduct> pizzaToppings = product.getToppings();
		List<ToppingProduct> formToppings = ToppingMapToList(form);
		return product.getName().equals(form.getName()) &&
			product.getPrice().isEqualTo(
					Money.of(Float.valueOf(PriceCommaToDot(form.getPrice())), "EUR")) &&
			pizzaToppings.containsAll(formToppings) && formToppings.size() == pizzaToppings.size();

	}

	/**
	 * Method for comparing if {@link DishsetProduct} and {@link DishsetProductCreationForm} (frontend form) are equal
	 * to determine if something changed in the edit
	 *
	 * @param product Dish set that gets compared
	 * @param form    form the DishsetProduct gets compared to
	 * @return boolean
	 */
	public boolean compareDishset(DishsetProduct product, DishsetProductCreationForm form) {
		return product.getName().equals(form.getName()) &&
			   product.getPrice().isEqualTo(
				Money.of(Float.valueOf(PriceCommaToDot(PriceCommaToDot(form.getPrice()))), "EUR"));
	}

	/**
	 * Method for comparing if {@link ToppingProduct} and {@link ToppingProductCreationForm} (frontend form) are equal
	 * to determine if something changed in the edit
	 *
	 * @param product topping that gets compared
	 * @param form    form the ToppingProduct gets compared to
	 * @return boolean
	 */
	public boolean compareTopping(ToppingProduct product, ToppingProductCreationForm form) {
		return product.getName().equals(form.getName()) &&
			   product.getPrice().isEqualTo(Money.of(
					   Float.valueOf(PriceCommaToDot(form.getPrice())), "EUR"));
	}


	//
	//find methods
	//

	/**
	 * Method to find all {@link Product}s in the Repository ({@link ShopCatalog})
	 *
	 * @return Streamable&lt;Product&gt;
	 */
	public Streamable<Product> findAll() {
		return shopCatalog.findAll();
	}

	/**
	 * Returns the first {@link Product} with the given name from the database
	 * Returns null if {@link Product} name cannot be found
	 *
	 * @param name to search for
	 * @return Product (might be null)
	 */
	public Product findByName(String name) {
		Optional<Product> product = shopCatalog.findByName(name).stream().findFirst();

		if (product.isEmpty()) {
			return null;
		}

		return product.get();
	}


	/**
	 * Returns all {@link Product}s of a given category
	 *
	 * @param category String
	 * @return Streamable&lt;Product&gt;
	 */
	public Streamable<Product> findByCategory(String category) {
		return shopCatalog.findByCategory(category);
	}

	/**
	 * Method to find a {@link Product} in the {@link ShopCatalog} with a given {@link ProductIdentifier}
	 *
	 * @param id Productidentifier
	 * @return Product
	 */
	public Product findById(ProductIdentifier id) {
		Optional<Product> p = shopCatalog.findById(id);
		if (p.isEmpty()) {
			return null;
			// ShopOrderController+Mgmt
		}
		return p.get();
	}

	/**
	 * Method to find a {@link Product} in the {@link ShopCatalog} with a given String
	 *
	 * @param id String
	 * @return Optional&lt;Product&gt;
	 */
	public Optional<Product> findById(String id) {

		return this.findAll().stream().filter(product -> product.getId().getIdentifier().equals(id)).findAny();

	}

	//
	//delete methods
	//

	/**
	 * Deletes a {@link Product} out of the inventory entirely by removing it from the {@link ShopCatalog}
	 *
	 * @param id {@link ProductIdentifier} of Product, that is tried to be deleted
	 */
	public void deleteHardById(ProductIdentifier id) {
		shopCatalog.delete(findById(id));
	}

	/**
	 * Removes a {@link Product} from the inventory (frontend) by removing all its categories and adding the
	 * {@link ProductCategory#DELETED Deleted} category <br>
	 * Note: when removing a {@link ToppingProduct Topping} all {@link PizzaProduct PizzaPresets} that contain this
	 * topping will be marked as Deleted too
	 *
	 * @param id of product to delete
	 * @return the Deleted marked product
	 */

	public Product deleteById(ProductIdentifier id) { //in order for products to still show up if they are deleted but
		// used elsewhere...
		Product toDeleteProduct = findById(id);

		//when the product is a topping
		if (toDeleteProduct.getCategories().toList().contains(ProductCategory.TOPPING.name())) {
			//remove all associated pizza presets
			findByCategory(ProductCategory.PIZZA.name())
					.stream() //find all pizza presets
					.filter(product -> ((PizzaProduct) product).
							getToppings().
							contains(toDeleteProduct)) //select the ones containing our topping
					.forEach(product -> deleteById(product.getId())); //remove these from the inventory
		}
		if (toDeleteProduct.getCategories().toList().contains(ProductCategory.OVEN.name())) {
			//oven is empty
			if (((OvenProduct) toDeleteProduct).getPizzas().isEmpty()) {
				//unlink chef
				((OvenProduct) toDeleteProduct).setChef(null);
			} else { //pizzas still assigned to oven
				return null;
			}
		}
		//remove all categories of the product
		toDeleteProduct.getCategories().forEach(toDeleteProduct::removeCategory); //XXX wouldn't it be better to also
		// keep old category, affects: at least the checks in inventory&order for deleted
		//and add category Deleted
		toDeleteProduct.addCategory(ProductCategory.DELETED.name());
		return shopCatalog.save(toDeleteProduct);
	}

	//
	// additional Methods
	//

	/**
	 * Takes the Map with {@link ToppingProduct}s and booleans it recieves out of the given form and turns it into a
	 * list of Toppings (to later save it properly in the repository)
	 *
	 * @param form {@link PizzaProductCreationForm} that contains the Map with Toppings and booleans
	 * @return List&lt;ToppingProduct&gt;
	 */
	//Method to transform the map containing toppings and booleans to a list of only toppings
	public List<ToppingProduct> ToppingMapToList(PizzaProductCreationForm form) {
		//do this with overloading "setProductDetails" (using the creationForm)?
		List<ToppingProduct> pizzaToppings = new ArrayList<>();
		logger.info(form.getToppings().toString());
		//a bit scuffed because the boolean is either true or null
		//if() not null => append
		for (ToppingProduct allTopping : form.getToppings().keySet()) {
			if (form.getToppings().get(allTopping) != null) {

				ToppingProduct allToppingTest = (ToppingProduct) findById(allTopping.getId());
				pizzaToppings.add(allToppingTest);

					//debug
					logger.info("added Pizza Topping" + allToppingTest);

			}
		}
		return pizzaToppings;
	}


	public boolean isAssigned(VehicleProduct vehicle) {
		List<VehicleProduct> allAssignedVehicles = employeeManagement.findByRole("Driver")
																	 .filter(d -> d.getVehicle() != null)
																	 .map(Employee::getVehicle)
																	 .toList();
		return allAssignedVehicles.contains(vehicle);
	}

	/**
	 * replaces all commas with dots in a String <br>
	 * (for managing prices in EU format without errors)
	 *
	 * @param price String
	 * @return String
	 */
	public String PriceCommaToDot(String price) {
		return price.replaceAll(",", ".");
	}



}

