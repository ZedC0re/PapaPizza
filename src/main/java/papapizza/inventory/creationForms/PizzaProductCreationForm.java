package papapizza.inventory.creationForms;

import lombok.Getter;
import lombok.Setter;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import papapizza.inventory.items.PizzaProduct;
import papapizza.inventory.items.ToppingProduct;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Form for frontend to create {@link PizzaProduct}s.
 */
@Getter
@Setter
public class PizzaProductCreationForm {
	private static final Logger logger = LoggerFactory.getLogger(PizzaProductCreationForm.class);

	@Size(max = 30, message = "{ShopInventoryProductCreationForm.toLarge.name}")
	@NotBlank(message = "{ShopInventoryProductCreationForm.notEmpty.name}")
	private String name;
	@NotBlank(message = "{ShopInventoryProductCreationForm.notEmpty.price}")
	private String price;
	@NotEmpty(message = "{ShopInventoryProductCreationForm.notEmpty.topping}") //will never be the case...
	Map<ToppingProduct, Boolean> toppings;


	public PizzaProductCreationForm(String name, String price, Map<ToppingProduct, Boolean> toppings){
		this.name = name;
		this.price = price;
		this.toppings = toppings;
	}

	/**
	 * Method to get the data from the form into the product
	 * @param form contains all the data the user put in (in frontend)
	 * @param product get's updated with the data out of the form
	 */
	public static void setProductDetails(PizzaProductCreationForm form, PizzaProduct product){
		product.setName(form.name);
		String price = form.price.replaceAll(",","."); //for parsing to float
		product.setPrice(Money.of(Float.parseFloat(price), "EUR"));
		//Map (with all Items & Booleanvalues) conversion to List only containing items
		//one should make an extra method for this
		List<ToppingProduct> pizzaToppings = new ArrayList<>();
		for(ToppingProduct allTopping : form.getToppings().keySet()) {
			if(form.getToppings().get(allTopping) != null) {
				if (form.getToppings().get(allTopping) == true) {
					pizzaToppings.add(allTopping);
				}
			} else{
				pizzaToppings.remove(allTopping);
			}
		}
		product.setToppings(pizzaToppings);

	}

	/**
	 * Method to get the data from the product into the form in order to display them in frontend
	 * @param form get's data from product
	 * @param product saved in {@link papapizza.inventory.ShopCatalog}, contains all data the form needs
	 * @param allToppings List of all Toppings, that are in inventory in the moment of using the method
	 */
	public static void setFormDetails(PizzaProductCreationForm form, PizzaProduct product,
									  List<ToppingProduct> allToppings){
		form.setName(product.getName());
		DecimalFormat df = new DecimalFormat("#.##");
		form.setPrice(df.format(product.getPrice().getNumber()));
		Map<ToppingProduct, Boolean> toppingMap = new HashMap<>();
		for(ToppingProduct allTopping : allToppings){
			toppingMap.put(allTopping,false);
		}
		//checking if a topping is on the pizza
		for(ToppingProduct mapAllTopping : toppingMap.keySet()){
			for(ToppingProduct pizzaTopping : product.getToppings()) {
				if (mapAllTopping.getName().equals(pizzaTopping.getName())) {
					toppingMap.replace(pizzaTopping, true);
				}
			}
		}
		form.setToppings(toppingMap);
	}
	/**
	 * Method to get the data into the form in order to display them in frontend
	 * @param form get's data from product
	 * @param allToppings List of all Toppings, that are in inventory in the moment of using the method
	 */
	public static void setFormDetails(PizzaProductCreationForm form, List<ToppingProduct> allToppings){
		Map<ToppingProduct, Boolean> toppingMap = new HashMap<>();
		for(ToppingProduct allTopping : allToppings){
			toppingMap.put(allTopping,false);
		}
		form.setToppings(toppingMap);
		logger.info("Topping Map (setFormDetails)"+toppingMap);
	}

}
