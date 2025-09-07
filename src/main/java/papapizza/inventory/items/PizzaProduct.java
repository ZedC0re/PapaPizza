package papapizza.inventory.items;

import lombok.Getter;
import lombok.Setter;
import org.salespointframework.catalog.Product;
import papapizza.inventory.ProductCategory;
import papapizza.order.ShopOrder;

import javax.money.MonetaryAmount;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import java.util.List;

/**
 * {@link Product} to create Pizzas containing {@link ToppingProduct}s.
 */
@Entity
public class PizzaProduct extends Product {

	@Column
	@Getter
	@Setter
	@ManyToMany
	private List<ToppingProduct> toppings;

	@Getter
	@Setter
	private PizzaState state;

	@Getter
	@Setter
	@ManyToOne
	private ShopOrder order;


	public PizzaProduct(){}

	public PizzaProduct(String name, MonetaryAmount price) {
		super(name, price);
		addCategory(ProductCategory.PIZZA.toString());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o){
			return true;
		}

		if (!(o instanceof PizzaProduct)){
			return false;
		}

		if (!super.equals(o)){
			return false;
		}

		PizzaProduct that = (PizzaProduct) o;

		if (!toppings.equals(that.toppings)){
			return false;
		}
		if (state != that.state){
			return false;
		}

		return order.equals(that.order);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + toppings.hashCode();
		result = 31 * result + state.hashCode();
		result = 31 * result + order.hashCode();
		return result;
	}
}


