package papapizza.inventory.items;

import org.salespointframework.catalog.Product;
import papapizza.inventory.ProductCategory;

import javax.money.MonetaryAmount;
import javax.persistence.Entity;

/**
 * {@link Product} to create Sets of Dishes, that can be bought in the pizza shop
 */
@Entity
public class DishsetProduct extends Product{
	public DishsetProduct(){}

	public DishsetProduct(String name, MonetaryAmount price) {
		super(name, price);
		addCategory(ProductCategory.DISHSET.toString());
	}
}
