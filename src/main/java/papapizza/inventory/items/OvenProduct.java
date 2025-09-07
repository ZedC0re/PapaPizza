package papapizza.inventory.items;

import lombok.Getter;
import lombok.Setter;
import org.salespointframework.catalog.Product;
import papapizza.employee.Employee;
import papapizza.inventory.ProductCategory;

import javax.money.MonetaryAmount;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link Product}, that is meant to store {@link PizzaProduct} and has a name and a price to display it's worth
 */

@Entity
public class OvenProduct extends Product {
	@Getter @Setter
	@OneToOne
	private Employee chef;

	@Getter @Setter
	@ManyToMany
	private List<PizzaProduct> pizzas = new ArrayList<>();

	@Getter @Setter
	private long inOvenTimestamp; //secs

	public OvenProduct(){}

	public OvenProduct(String name, MonetaryAmount price){
		super(name, price);
		addCategory(ProductCategory.OVEN.toString()); //"Oven"

	}

	public boolean isEmpty(){
		return pizzas.isEmpty() || pizzas.get(0).getState() == PizzaState.OPEN;
	}

	public PizzaProduct getCurrentlyBakingPizza(){
		if(isEmpty()){
			return null;
		}
		return pizzas.get(0);
	}

	public long getInOvenSince(){
		return System.currentTimeMillis()/1000-inOvenTimestamp;
	}

}
