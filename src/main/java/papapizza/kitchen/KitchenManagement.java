package papapizza.kitchen;


import lombok.NonNull;
import org.salespointframework.catalog.Product;
import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.order.OrderLine;
import org.salespointframework.time.BusinessTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import papapizza.app.exc.PapaPizzaRunException;
import papapizza.delivery.DeliveryManagement;
import papapizza.employee.EmployeeManagement;
import papapizza.inventory.ProductCategory;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.inventory.items.OvenProduct;
import papapizza.inventory.items.PizzaProduct;
import papapizza.inventory.items.PizzaState;
import papapizza.order.ShopOrder;
import papapizza.order.ShopOrderManagement;
import papapizza.order.ShopOrderState;

import java.time.Duration;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class KitchenManagement {

	private final Logger logger = LoggerFactory.getLogger(KitchenManagement.class);

	private static final int MAX_BAKING_TIME = 300; //in sec

	public final BusinessTime businessTime;
	public final ShopOrderManagement<ShopOrder> shopOrderManagement;
	public final EmployeeManagement employeeManagement;
	public final ShopCatalogManagement shopCatalogManagement;
	public final DeliveryManagement deliveryManagement;

	KitchenManagement(BusinessTime businessTime, ShopOrderManagement<ShopOrder> shopOrderManagement,
					  EmployeeManagement employeeManagement, ShopCatalogManagement shopCatalogManagement,
					  DeliveryManagement deliveryManagement) {
		this.businessTime = businessTime;
		this.shopOrderManagement = shopOrderManagement;
		this.employeeManagement = employeeManagement;
		this.shopCatalogManagement = shopCatalogManagement;
		this.deliveryManagement = deliveryManagement;
	}

	public int getTimeLeft(PizzaProduct pizzaProduct) {
		if(pizzaProduct.getState() != PizzaState.PENDING){
			return Integer.MAX_VALUE;
		}
		OvenProduct oven = getPizzaOven(pizzaProduct);
		return MAX_BAKING_TIME-(int)oven.getInOvenSince();
	}


	// assignOvens = assignChef
	public void assignOvens(ShopOrder shopOrder) {
		if (!shopOrder.getChefs().isEmpty()) {
			throw new PapaPizzaRunException("assignChef called on already assigned ShopOrder");
		}

		//get all order lines which are pizzas or custom pizzas
		List<OrderLine> pizzaLines = shopOrder.getOrderLines().stream()
				.filter(orderLine -> {
					List<String> categories = shopCatalogManagement.findById(orderLine.getProductIdentifier())
							.getCategories().toList();
					return categories.contains(ProductCategory.PIZZA.toString()) ||
						   categories.contains(ProductCategory.CUSTOM_PIZZA.toString());
				})
				.collect(Collectors.toList());

		//list of all copied pizzas, as we need to set states separately
		List<PizzaProduct> pizzas = new ArrayList<>();
		for (OrderLine orderLine : pizzaLines) {
			//amount of orderline
			int pizzaAmount = orderLine.getQuantity().getAmount().intValue();
			PizzaProduct pizza = (PizzaProduct) shopCatalogManagement.findById(orderLine.getProductIdentifier());
			for (int i = 0; i < pizzaAmount; i++) {
				//create as many copies as orderline quantity
				PizzaProduct pizzaCopy = shopCatalogManagement.createKitchenCopyPizzaProduct(pizza);
				pizzaCopy.setOrder(shopOrder); //link pizza copy to order
				pizzaCopy.setState(PizzaState.OPEN);
				shopCatalogManagement.save(pizzaCopy);
				pizzas.add(pizzaCopy);
			}
		}

		//get all ovens
		List<OvenProduct> ovens = shopCatalogManagement.findByCategory(ProductCategory.OVEN.toString()).stream().
													   map(oven -> (OvenProduct) oven)
				.filter(oven -> oven.getChef() != null).collect(Collectors.toList());

		if(ovens.isEmpty()){
			throw new PapaPizzaRunException("There are no ovens with assigned employees");
		}

		//distribute pizzas across ovens
		for (PizzaProduct pizzaProduct : pizzas) {
			//get oven with least pizzas, 1st if equal
			OvenProduct leastOven = ovens.get(0);
			for (OvenProduct oven : ovens) {
				if (oven.getPizzas().size() < leastOven.getPizzas().size()) {
					leastOven = oven;
				}
			}

			//add employee of pizza to order (for overview)
			if(!shopOrder.getChefs().contains(leastOven.getChef())){
				shopOrder.getChefs().add(leastOven.getChef());
			}
			//assign a pizza to oven
			leastOven.getPizzas().add(pizzaProduct);
			shopCatalogManagement.save(leastOven);
		}
	}

	public boolean changePizzaState(ProductIdentifier pizzaId, PizzaState changeTo) {
		PizzaProduct pizza = (PizzaProduct) shopCatalogManagement.findById(pizzaId); //get pizza by id
		if (pizza == null) {
			return false;
		}
		OvenProduct pizzaOven = getPizzaOven(pizza);
		//check if oven empty
		if(pizzaOven == null){
			return false;
		}

		boolean returnValue = false; //why ever someone would do it like this sqube
		if(changeTo == PizzaState.PENDING) {
			returnValue = startBaking(pizza, pizzaOven);
		}
		if(changeTo == PizzaState.READY){
			returnValue = finishBaking(pizza, pizzaOven);
		}
		return returnValue;
	}

	private boolean startBaking(PizzaProduct pizza, OvenProduct pizzaOven){
		if(!pizzaOven.isEmpty()){ //oven could alr be baking a pizza
			return false;
		}
		//calculate and set open duration when first pizza of order put in oven
		if(pizza.getOrder().getOpenDuration() == null){
			long dur = System.currentTimeMillis()/1000-pizza.getOrder().getTimeCreated()
					.atZone(ZoneId.systemDefault()).toEpochSecond();
			Duration openDur = Duration.of(dur, ChronoUnit.SECONDS);
			logger.info("Setting open duration for order to "+openDur.getSeconds());
			pizza.getOrder().setOpenDuration(openDur);
		}else{
			logger.info("open duration alr set to "+pizza.getOrder().getOpenDuration().getSeconds());
		}

		//set timestamp
		pizzaOven.setInOvenTimestamp(System.currentTimeMillis() / 1000);
		//mark pizza as in oven
		pizza.setState(PizzaState.PENDING);
		shopOrderManagement.setShopOrderState(pizza.getOrder(), ShopOrderState.PENDING);
		shopOrderManagement.save(pizza.getOrder());
		shopCatalogManagement.save(pizza);
		shopCatalogManagement.save(pizzaOven);
		return true;
	}

	public boolean finishBaking(PizzaProduct pizza, OvenProduct pizzaOven) {
		//check if oven is even baking a pizza
		if(pizzaOven.isEmpty()){
			return false;
		}
		//this pizza is the one requested to finish
		if(!pizzaOven.getCurrentlyBakingPizza().equals(pizza)){
			return false;
		}
		//baking duration
		long inOvenSince = pizzaOven.getInOvenSince();
		//this has no purpose yet but might be used later to calculate the time when the oven is empty
		pizzaOven.setInOvenTimestamp(System.currentTimeMillis()/1000);

		ShopOrder belongingOrder = getShopOrderByPizza(pizza);
		Duration oldDuration = belongingOrder.getPendingDuration();
		//replace current duration with longest pending time
		if(oldDuration == null || oldDuration.getSeconds() < inOvenSince) {
			belongingOrder.setPendingDuration(Duration.ofSeconds(inOvenSince));
		}
		//finish pizza state
		pizza.setState(PizzaState.READY);
		//remove form oven queue
		pizzaOven.getPizzas().remove(pizza);
		//check order complete?
		if(checkOrderReady(belongingOrder)){
			//mark ready for delivery
			shopOrderManagement.setOrderReady(belongingOrder);
		}
		//delete pizza & save oven, order
		shopCatalogManagement.deleteHardById(pizza.getId());
		shopCatalogManagement.save(pizzaOven);
		shopOrderManagement.save(belongingOrder);
		return true;
	}

	private boolean checkOrderReady(ShopOrder order){
		logger.debug("check order is run");
		return getKitchenPizzasOfOrder(order).noneMatch(product -> { //none have state open or pending -> pizza is ready
					PizzaProduct piProduct = ((PizzaProduct) product);
					return piProduct.getState() == PizzaState.OPEN || piProduct.getState() == PizzaState.PENDING;
				});
	}

	private ShopOrder getShopOrderByPizza(PizzaProduct pizza){
		Optional<ShopOrder> order = shopOrderManagement.get(Objects.requireNonNull(pizza.getOrder().getId()));
		if(order.isEmpty()){
			return null;
		}
		return order.get();
	}

	private OvenProduct getPizzaOven(@NonNull PizzaProduct pizza){
		//get pizza's oven
		Optional<Product> optPizzaOven = shopCatalogManagement.findByCategory("Oven")
				.filter(oven -> ((OvenProduct) oven).getPizzas().contains(pizza)).stream().findFirst();
		if(optPizzaOven.isEmpty()){ //pizza doesnt belong to any oven
			return null;
		}
		return (OvenProduct) optPizzaOven.get();
	}

	private List<OvenProduct> getOvensFromShopOrder(@NonNull ShopOrder order){
		return shopCatalogManagement.findByCategory("Oven").map(p -> (OvenProduct)p)
				.filter(oven -> oven.getPizzas().stream().map(PizzaProduct::getOrder)
						.anyMatch(o -> (o != null) && (o.hasId(Objects.requireNonNull(order.getId()))))).toList();
	}

	private Stream<Product> getKitchenPizzasOfOrder(ShopOrder order){
		return shopCatalogManagement.findByCategory(ProductCategory.KITCHEN_PIZZA.name()).stream()
                                    .filter(product -> ((PizzaProduct) product).getOrder()!=null
						&& ((PizzaProduct) product).getOrder().getId().equals(order.getId()));
		//order identifier match -> pizzas belong to order
	}

	/**
	 * Cancels all pizzas for the provided order
	 * @param order to cancel
	 * @return true if successful
	 */
	public boolean cancelPizzasForOrder(ShopOrder order){
		if(order.getShopOrderState() != ShopOrderState.OPEN){
			return false;
		}

		//remove every pizza from oven's pizza list
		//XXX reassign other orders again
		getKitchenPizzasOfOrder(order).forEach(product -> {
			PizzaProduct pizza = (PizzaProduct) product;
			OvenProduct oven = getPizzaOven(pizza);
			assert oven != null;
			//remove pizza from oven
			oven.getPizzas().remove(pizza);
			shopCatalogManagement.save(oven);
			//delete pizza entirely
			shopCatalogManagement.deleteHardById(pizza.getId());
		});
		return true;
	}

	/**
	 * Calculates the maximum time until all pizzas are baked (assuming instant human input) <br>
	 * This method also only works if called right after the assignment of ovens
	 * and may break if you assign a new order in the meantime
	 * @param order to calculate
	 * @return the time as {@link Duration}
	 */
	public Duration kitchenShopOrderTimeEstimate(@NonNull ShopOrder order){
		Duration maxOvenDuration = Duration.ZERO;
		//calculate the maximum duration for an order
		for(OvenProduct oven : getOvensFromShopOrder(order)){
			//get Durations (via queue of assigned oven) to ready
			Duration queueDuration = Duration.ofSeconds((long) oven.getPizzas().size() * MAX_BAKING_TIME);
			if(!oven.isEmpty()) {
				queueDuration = queueDuration.minus(Duration.ofSeconds(oven.getInOvenSince()));
			}
			//get maximum
			if(maxOvenDuration.compareTo(queueDuration) < 0){ //max<toCompare
				maxOvenDuration = queueDuration;
			}
		}
		return maxOvenDuration;
	}

}
