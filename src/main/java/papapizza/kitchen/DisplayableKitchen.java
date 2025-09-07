package papapizza.kitchen;


import lombok.Getter;
import lombok.Setter;
import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.order.OrderLine;
import papapizza.inventory.items.OvenProduct;

import java.util.List;
import java.util.Map;


@Setter
@Getter
public class DisplayableKitchen {
	private List<OrderLine> orderLinesOther;
	private Map<ProductIdentifier, Integer> times;
	private OvenProduct oven;
	private boolean empty = false;

	public boolean isEmpty(){
		return empty;
	}
}