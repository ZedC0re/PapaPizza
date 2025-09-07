package papapizza.inventory.items;

import org.salespointframework.catalog.Product;
import papapizza.inventory.ProductCategory;

import javax.money.MonetaryAmount;
import javax.persistence.Entity;

/**
 * {@link Product} that can be applied to a {@link PizzaProduct}
 */
@Entity
public class ToppingProduct extends Product {

	//throws hibernate error
	/*@Id
	@GeneratedValue
	private long id;*/

	public ToppingProduct(){}

	public ToppingProduct(String name, MonetaryAmount price) {
		super(name, price);
		addCategory(ProductCategory.TOPPING.toString());
	}
}
