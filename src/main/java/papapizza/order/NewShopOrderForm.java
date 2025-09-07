package papapizza.order;

import lombok.Getter;
import lombok.Setter;
import org.salespointframework.order.ChargeLine;
import org.salespointframework.order.OrderLine;
import papapizza.validation.QuantityBetween;
import papapizza.validation.ValidCardMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class  NewShopOrderForm {

	@ValidCardMap
	private Map<String,Integer> consumables;
	@ValidCardMap
	private Map<String, Integer> pizzaPresets;
	@ValidCardMap
	private Map<String, Integer> drinks;
	@ValidCardMap
	private Map<String, Integer> dishSets;
	private List<String> toppings;
	@QuantityBetween(min = 0, max = 500)
	private String customPizzaQuantity;
	private List<OrderLine> shopOrderOrderLines;
	private List<ChargeLine> shopOrderChargeLines;
	private String deliveryType;

	public NewShopOrderForm() {
		consumables = new HashMap<>();
		pizzaPresets = new HashMap<>();
		drinks = new HashMap<>();
		dishSets = new HashMap<>();
		toppings = new ArrayList<>();
		customPizzaQuantity = "1";
		shopOrderOrderLines = new ArrayList<>();
		shopOrderChargeLines = new ArrayList<>();
		deliveryType = "";
	}
}
