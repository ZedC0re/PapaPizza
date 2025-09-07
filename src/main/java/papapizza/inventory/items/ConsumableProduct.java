package papapizza.inventory.items;

import lombok.Getter;
import lombok.Setter;
import org.salespointframework.catalog.Product;
import papapizza.inventory.ProductCategory;

import javax.money.MonetaryAmount;
import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * {@link Product} that is meant to be used for different kinds of food & groceries, that are sold in the pizza shop
 */
@Entity
public class ConsumableProduct extends Product {

	//throws hibernate error
	/*@Id
	@GeneratedValue
	private long id;*/

	@Column
	@Getter @Setter
	private String ingredients;

	public ConsumableProduct(){}

	public ConsumableProduct(String name, MonetaryAmount price) {
		super(name, price);
		addCategory(ProductCategory.CONSUMABLE.toString());

	}
}
