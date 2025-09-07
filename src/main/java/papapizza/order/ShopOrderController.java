package papapizza.order;

import com.mysema.commons.lang.Assert;
import lombok.NonNull;
import org.javamoney.moneta.Money;
import org.salespointframework.catalog.Product;
import org.salespointframework.order.OrderIdentifier;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.web.LoggedIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import papapizza.customer.Customer;
import papapizza.customer.CustomerManagement;
import papapizza.delivery.DeliveryManagement;
import papapizza.employee.EmployeeManagement;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.inventory.items.PizzaProduct;
import papapizza.inventory.items.ToppingProduct;
import papapizza.kitchen.KitchenManagement;

import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@SessionAttributes("newShopOrderId")
public class ShopOrderController {

	private final Logger logger = LoggerFactory.getLogger(ShopOrderController.class);

	private final ShopOrderManagement<ShopOrder> shopOrderManagement;
	private final CustomerManagement customerManagement;
	private final EmployeeManagement employeeManagement;
	private final ShopCatalogManagement catalogManagement;
	private final DeliveryManagement deliveryManagement;
	private final KitchenManagement kitchenManagement;
	private InvoiceHandler invoiceHandler;

	@Autowired
	public ShopOrderController(ShopOrderManagement<ShopOrder> shopOrderManagement, CustomerManagement customerManagement, EmployeeManagement employeeManagement, ShopCatalogManagement catalogManagement,
							   DeliveryManagement deliveryManagement, KitchenManagement kitchenManagement) {

		Assert.notNull(shopOrderManagement, "OrderManagement must not be null!");
		this.shopOrderManagement = shopOrderManagement;
		this.customerManagement = customerManagement;
		this.employeeManagement = employeeManagement;
		this.catalogManagement = catalogManagement;
		this.deliveryManagement = deliveryManagement;
		this.kitchenManagement = kitchenManagement;
	}

	@Autowired
	public void setInvoiceHandler(@NonNull InvoiceHandler invoiceHandler) {
		this.invoiceHandler = invoiceHandler;
	}

	//********** ORDER ************

	@GetMapping("/order")
	String order(Model model) {
		List<DisplayableShopOrder> displayableShopOrders = ShopOrderDisplayer.display(shopOrderManagement.findAll().stream()
				.filter(order -> order.getCustomer().getMeta() == Customer.Meta.NORMAL));

		//TODO filter out completed and cancelled orders, when analytics is added
		//here sorting / filtering etc...

		logger.info(String.valueOf(displayableShopOrders.size()));

		model.addAttribute("orders", displayableShopOrders);

		return "order/order";
	}

	//-details

	@GetMapping(value = "/order/details/{orderId}")
	String orderDetails(Model model, @PathVariable final String orderId) {

		if (shopOrderManagement.findByShopOrderId(orderId).isEmpty()) {

			//where is this handled? (so nice exception page is shown)
			throw new ResponseStatusException(NOT_FOUND, "Unable to find order for id " + orderId);
		} else {
			model.addAttribute("order", ShopOrderDisplayer.display(shopOrderManagement.findByShopOrderId(orderId).get()));
		}

		return "order/orderDetails";
	}

	//-invoice

	@GetMapping(value = "/downloadInvoice")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER','DRIVER')")
	public ResponseEntity<InputStreamResource> downloadInvoice(@RequestParam(name="orderId") OrderIdentifier orderId){
		Optional<ShopOrder> order = shopOrderManagement.get(orderId);
		if(order.isEmpty()){ //requested order doesnt exist
			throw new ResponseStatusException(NOT_FOUND);
		}

		return invoiceHandler.getInvoiceResponse(order.get().getInvoiceFilename());
	}

	//-cancel

	@PostMapping(value = "/order/cancel/{orderId}")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String cancelShopOrder(@PathVariable final String orderId) {

		//TODO solve JdbcSQLIntegrityConstraintViolationException

		Optional<ShopOrder> order = shopOrderManagement.findByShopOrderId(orderId);

		if (order.isEmpty()) {

			//where is this handled? (so nice exception page is shown)
			throw new ResponseStatusException(NOT_FOUND, "Unable to find order for id " + orderId);
		} else {
			order.get().setTimeCompleted(LocalDateTime.now());
			kitchenManagement.cancelPizzasForOrder(order.get());
			shopOrderManagement.setShopOrderState(order.get(), ShopOrderState.CANCELLED);
			customerManagement.revertOldTan(order.get().getCustomer());
		}

		return "redirect:/order";
	}

	//-edit

	@PostMapping(value = "/order/edit/{orderId}")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String editShopOrder(Model model, @PathVariable final String orderId) {

		Optional<ShopOrder> order = shopOrderManagement.findByShopOrderId(orderId);

		if (order.isEmpty()) {

			//where is this handled? (so nice exception page is shown)
			throw new ResponseStatusException(NOT_FOUND, "Unable to find order for id " + orderId);
		}

		if (!order.get().getShopOrderState().equals(ShopOrderState.INVALID))
			throw new IllegalArgumentException("ShopOrder with id: " + orderId + " is already applied.");
		model.addAttribute("newShopOrderId", orderId);


		return "redirect:/order/newOrder";
	}

	//-complete

	@PostMapping(value = "/order/complete/{orderId}")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String completeShopOrder(@PathVariable final String orderId) {

		if (shopOrderManagement.findByShopOrderId(orderId).isEmpty())
			throw new ResponseStatusException(NOT_FOUND, "Unable to find order for id " + orderId);
		else {
			ShopOrder shopOrder = shopOrderManagement.findByShopOrderId(orderId).get();
			if (shopOrder.getDeliveryType().equals(DeliveryType.PICKUP) && shopOrder.getShopOrderState().equals(ShopOrderState.READYPICKUP)) {
				shopOrderManagement.setShopOrderState(shopOrder, ShopOrderState.COMPLETED);
			}
		}

		return "redirect:/order";
	}

	//-cleanup

	@PostMapping(value = "/order/cleanup")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String cleanupInvalids() {

		for (ShopOrder order : shopOrderManagement.findAll()) {
			if (order.getShopOrderState().equals(ShopOrderState.INVALID)) shopOrderManagement.delete(order);
		}

		return "redirect:/order";
	}

	//********** CUSTOMER VERIFICATION ************

	@GetMapping(value = "/customerVerification")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String customerVerification(Model model) {

		model.addAttribute("customerVerificationForm", new CustomerVerificationForm());
		customerManagement.findAll().forEach(customer -> logger.info(customer.getFirstname() + " " + customer.getLastname() + ", " + customer.getPhone() + ", " + customer.getCurrentTan()));

		return "order/customerVerification";
	}

	@PostMapping("/customerVerification")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String customerVerificationForm(Model model, @Valid @ModelAttribute CustomerVerificationForm customerVerificationForm, final BindingResult result, @LoggedIn Optional<UserAccount> loggedInUser) {

		if (loggedInUser.isEmpty()) return "order/customerVerification";

		if (result.hasErrors()) {

			if (result.getAllErrors().stream().findFirst().get().getDefaultMessage().equals("no valid tan input"))
				model.addAttribute("verificationSuccess", "tanError");
			if (result.getAllErrors().stream().findFirst().get().getDefaultMessage().equals("no valid phone input"))
				model.addAttribute("verificationSuccess", "phoneError");

			return "order/customerVerification";
		}

		if (customerManagement.verifyCustomerByTan(customerVerificationForm.getPhone(), Integer.parseInt(customerVerificationForm.getTan()))) {
			//when customer alr has orders not completed or cancelled
			if (!shopOrderManagement.findBy(customerManagement.findByPhone(customerVerificationForm.getPhone()).get(0))
					.filter(shopOrder -> shopOrder.getShopOrderState().isActive()).isEmpty())
				model.addAttribute("verificationSuccess", "failedAlrActiveOrder");

			else {
				model.addAttribute("verificationSuccess", "success");
				model.addAttribute("newShopOrderId", shopOrderManagement.save(shopOrderManagement.create(
						employeeManagement.findByUsername(loggedInUser.get().getUsername()).get(),
						customerManagement.findByPhoneAndTan(customerVerificationForm.getPhone(), Integer.parseInt(customerVerificationForm.getTan())).get()))
						.getId().getIdentifier());
				shopOrderManagement.setShopOrderState(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get(), ShopOrderState.INVALID);
				shopOrderManagement.save(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get());

				return "redirect:/order/newOrder";
			}
		} else {
			model.addAttribute("verificationSuccess", "failed");
		}
		return "/order/customerVerification";
	}

	//********** NEW ORDER ************

	@GetMapping("/newOrder")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String newOrderForm() {
		return "redirect:/order/newOrder";
	}

	@GetMapping(value = "/order/newOrder")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String newOrder(Model model) {

		if (model.getAttribute("newShopOrderId") == null)
			throw new RuntimeException("No newShopOrderId session attribute found");
		if (shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).isEmpty())
			throw new RuntimeException("newShopOrderId does not correspond persistently stored ShopOrder");

		NewShopOrderForm form = new NewShopOrderForm();

		ShopOrder newShopOrder = shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get();

		//parsing order to form
		{
			Map<String, Integer> consumables = new HashMap<>();
			catalogManagement.findByCategory("Consumable").forEach(product -> consumables.put(product.getName(), 0));
			newShopOrder.getOrderLines().stream().filter(orderLine -> catalogManagement.findById(orderLine.getProductIdentifier())
					.getCategories().stream().anyMatch(category -> category.equals("Consumable")));
			catalogManagement.findByCategory("Consumable").forEach(product -> consumables.put(product.getName(), 0));
			logger.info("[NewOrder] Map of consumables:" + consumables);
			newShopOrder.getOrderLines().stream().filter(orderLine -> catalogManagement.findById(orderLine.getProductIdentifier()).getCategories().stream().anyMatch(category -> category.equals("Consumable")))
					.forEach(orderLine -> consumables
							.put(orderLine.getProductName(), orderLine.getQuantity().getAmount().intValue()));
			form.setConsumables(consumables);

			Map<String, Integer> pizzaPresets = new HashMap<>();
			catalogManagement.findByCategory("Pizza").forEach(product -> pizzaPresets.put(product.getName(), 0));
			newShopOrder.getOrderLines().stream().filter(orderLine -> catalogManagement.findById(orderLine.getProductIdentifier())
					.getCategories().stream().anyMatch(category -> category.equals("Pizza")));
			catalogManagement.findByCategory("Pizza").forEach(product -> pizzaPresets.put(product.getName(), 0));
			newShopOrder.getOrderLines().stream().filter(orderLine -> catalogManagement.findById(orderLine.getProductIdentifier()).getCategories().stream().anyMatch(category -> category.equals("Pizza")))
					.forEach(orderLine -> pizzaPresets
							.put(orderLine.getProductName(), orderLine.getQuantity().getAmount().intValue()));
			form.setPizzaPresets(pizzaPresets);

			Map<String, Integer> drinks = new HashMap<>();
			catalogManagement.findByCategory("Drink").forEach(product -> drinks.put(product.getName(), 0));
			newShopOrder.getOrderLines().stream().filter(orderLine -> catalogManagement.findById(orderLine.getProductIdentifier())
					.getCategories().stream().anyMatch(category -> category.equals("Drink")));
			catalogManagement.findByCategory("Drink").forEach(product -> drinks.put(product.getName(), 0));
			newShopOrder.getOrderLines().stream().filter(orderLine -> catalogManagement.findById(orderLine.getProductIdentifier()).getCategories().stream().anyMatch(category -> category.equals("Drink")))
					.forEach(orderLine -> drinks
							.put(orderLine.getProductName(), orderLine.getQuantity().getAmount().intValue()));
			form.setDrinks(drinks);

			Map<String, Integer> dishSets = new HashMap<>();
			catalogManagement.findByCategory("Dishset").forEach(product -> dishSets.put(product.getName(), 0));
			newShopOrder.getOrderLines().stream().filter(orderLine -> catalogManagement.findById(orderLine.getProductIdentifier())
					.getCategories().stream().anyMatch(category -> category.equals("Dishset")));
			catalogManagement.findByCategory("Dishset").forEach(product -> dishSets.put(product.getName(), 0));
			newShopOrder.getOrderLines().stream().filter(orderLine -> catalogManagement.findById(orderLine.getProductIdentifier()).getCategories().stream().anyMatch(category -> category.equals("Dishset")))
					.forEach(orderLine -> dishSets
							.put(orderLine.getProductName(), orderLine.getQuantity().getAmount().intValue()));
			form.setDishSets(dishSets);
		}
		//parsing toppings
		List<String> toppings = new ArrayList<>();
		catalogManagement.findByCategory("Topping").forEach(product -> toppings.add(product.getName()));
		form.setToppings(toppings);

		//parsing orderLines
		form.setShopOrderOrderLines(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get().getOrderLines().toList());
		//parsing chargeLines
		form.setShopOrderChargeLines(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get().getChargeLines().toList());

		//parsing total
		if (shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get().getOrderLines().stream().count() == 0) {
			model.addAttribute("newShopOrderTotal", Money.of(0, "EUR"));
		} else {
			model.addAttribute("newShopOrderTotal",
					shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get().getTotal().toString());
		}

		boolean freeDrinkPossible = shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get()
				.getTotal().isGreaterThanOrEqualTo(Money.of(30, "EUR"));
		model.addAttribute("freeDrinkPossible", freeDrinkPossible);

		model.addAttribute("form", form);

		if (newShopOrder.getDeliveryType().equals(DeliveryType.PICKUP)) {
			form.setDeliveryType("Pickup");
			model.addAttribute("deliveryType", "Pickup");
		} else {
			form.setDeliveryType("Delivery");
			model.addAttribute("deliveryType", "Delivery");
		}

		return "order/newOrder";
	}

	//-card

	@PostMapping("/newOrder/card")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String card(Model model, @Valid @ModelAttribute("form") NewShopOrderForm form, final BindingResult result, RedirectAttributes attributes) {
		if (result.hasErrors()) {
			attributes.addFlashAttribute("error", "error");
			return "redirect:/order/newOrder";
		}
		manipulateShopOrderLines(model, form);
		return "redirect:/order/newOrder";
	}

	//-configurator

	@PostMapping("/newOrder/configurator")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String configurator(Model model, @Valid @ModelAttribute("form") NewShopOrderForm form, final BindingResult result, @RequestParam(name = "selectedToppings", required = false) List<String> selectedToppings, RedirectAttributes attributes) {

		if (form.getDeliveryType().equals("Delivery"))
			shopOrderManagement.setShopOrderDeliveryType(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get(), DeliveryType.DELIVERY);
		else
			shopOrderManagement.setShopOrderDeliveryType(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get(), DeliveryType.PICKUP);

		manipulateShopOrderLines(model, form);

		if (selectedToppings == null) selectedToppings = new ArrayList<>();

		PizzaProduct customPizzaProduct = catalogManagement.createPizzaProduct(selectedToppings.stream().map(x -> (ToppingProduct) catalogManagement.findByName(x)).collect(Collectors.toList()));

		//customPizzaProduct.getToppings().forEach(t -> logger.info(t.getName()));

		shopOrderManagement.addLinesByProduct(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get().getId(),
				customPizzaProduct, Quantity.of(Integer.parseInt(form.getCustomPizzaQuantity())));

		calculateDiscounts(model, form);

		return "redirect:/order/newOrder";
	}

	//-remove

	@PostMapping("/newOrder/removeFromSummary/{orderLineProductId}")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String removeFromSummary(Model model, @Valid @ModelAttribute("form") NewShopOrderForm form, final BindingResult result, @PathVariable final String orderLineProductId, RedirectAttributes attributes) {

		if (form.getDeliveryType().equals("Delivery"))
			shopOrderManagement.setShopOrderDeliveryType(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get(), DeliveryType.DELIVERY);
		else
			shopOrderManagement.setShopOrderDeliveryType(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get(), DeliveryType.PICKUP);

		manipulateShopOrderLines(model, form);

		ShopOrder newShopOrder = shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get();

		if (newShopOrder.getOrderLines().stream().map(orderLine -> orderLine.getProductIdentifier().getIdentifier()).collect(Collectors.toList()).contains(orderLineProductId)) {
			shopOrderManagement.removeLinesByProduct(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get().getId(),
					catalogManagement.findById(orderLineProductId).get());
		} else
			throw new IllegalArgumentException("No OrderLine with ProductId: " + orderLineProductId + " in newShopOrder found.");

		calculateDiscounts(model, form);

		return "redirect:/order/newOrder";
	}

	//-apply

	@PostMapping("/newOrder/apply")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String apply(Model model, @Valid @ModelAttribute("form") NewShopOrderForm form, final BindingResult result, RedirectAttributes attributes) {


		manipulateShopOrderLines(model, form);

		ShopOrder newShopOrder = shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get();

		shopOrderManagement.setShopOrderState(newShopOrder, ShopOrderState.OPEN);
		//overwriting default (ShopOrderState.OPEN) when no Pizza exists in newShopOrder
		if (newShopOrder.getOrderLines().stream().filter(orderLine -> catalogManagement.findById(orderLine.getProductIdentifier())
				.getCategories().toList().contains("Pizza") || catalogManagement.findById(orderLine.getProductIdentifier())
				.getCategories().toList().contains("CustomPizza")).findAny().isEmpty()) {
			shopOrderManagement.setOrderReady(newShopOrder);
		}

		deliveryManagement.assignDriver(newShopOrder);
		kitchenManagement.assignOvens(newShopOrder);
		shopOrderManagement.save(newShopOrder);
		shopOrderManagement.calcTimeEstimate(newShopOrder);

		//give customer new tan
		customerManagement.renewCustomerTan(newShopOrder.getCustomer());
		//create invoice for the just created order
		try {
			String filename = invoiceHandler.createInvoice(newShopOrder);
			newShopOrder.setInvoiceFilename(filename);
			shopOrderManagement.save(newShopOrder);
		} catch (IOException ignored) {
			//TODO
		}
		attributes.addFlashAttribute("orderCreated", newShopOrder);
		attributes.addFlashAttribute("timeEstimate", newShopOrder.getTimeEstimate().toMinutes());

		return "redirect:/order";
	}

	//*********  ADDITIONAL LOGIC  **********

	private void calculateDiscounts(Model model, @NonNull @Valid NewShopOrderForm form) {
		//PickupDiscount
		shopOrderManagement.removeLinesByDescription(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get().getId(),
				"PickupDiscount");

		if (form.getDeliveryType().equals("Pickup"))
			shopOrderManagement.addLinesByTotal(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get().getId(),
					shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get()
							.getTotal().divide(10).negate(), "PickupDiscount");

		//DrinkDiscount
		shopOrderManagement.removeLinesByDescription(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get().getId(),
				"FreeDrinkDiscount");

		if (shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get().getTotal().isGreaterThanOrEqualTo(Money.of(30, "EUR"))) {
			List<Product> drinkProducts = shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get().getOrderLines().stream().map(orderLine -> catalogManagement.findById(orderLine.getProductIdentifier()))
					.filter(product -> product.getCategories().toList().contains("Drink")).collect(Collectors.toList());
			if (!drinkProducts.isEmpty()) {
				Product cheapestDrink = drinkProducts.get(0);
				for (Product drink : drinkProducts) {
					if (drink.getPrice().isLessThan(cheapestDrink.getPrice())) cheapestDrink = drink;
				}
				if (shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get().getTotal().subtract(cheapestDrink.getPrice()).isGreaterThanOrEqualTo(Money.of(30, "EUR"))) {
					shopOrderManagement.addLinesByTotal(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get().getId(),
							cheapestDrink.getPrice().negate(), "FreeDrinkDiscount");
				}
			}
		}
	}

	private void manipulateShopOrderLines(Model model, @NonNull @Valid NewShopOrderForm form) {
		if (model.getAttribute("newShopOrderId") == null)
			throw new RuntimeException("No newShopOrderId session attribute found");
		if (shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).isEmpty())
			throw new RuntimeException("newShopOrderId does not correspond persistently stored ShopOrder");

		if (form.getDeliveryType().equals("Delivery"))
			shopOrderManagement.setShopOrderDeliveryType(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get(), DeliveryType.DELIVERY);
		else
			shopOrderManagement.setShopOrderDeliveryType(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get(), DeliveryType.PICKUP);


		//calling of shopOrderManagement.setProductAndQuantityAsLines for each entry
		for (Map.Entry<String, Integer> entry : form.getConsumables().entrySet()) {
			shopOrderManagement.setProductAndQuantityAsLines(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get().getId(),
					catalogManagement.findByName(entry.getKey()),
					Quantity.of(entry.getValue().longValue()));
		}

		for (Map.Entry<String, Integer> entry : form.getPizzaPresets().entrySet()) {
			shopOrderManagement.setProductAndQuantityAsLines(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get().getId(),
					catalogManagement.findByName(entry.getKey()),
					Quantity.of(entry.getValue().longValue()));
		}

		for (Map.Entry<String, Integer> entry : form.getDrinks().entrySet()) {
			shopOrderManagement.setProductAndQuantityAsLines(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get().getId(),
					catalogManagement.findByName(entry.getKey()),
					Quantity.of(entry.getValue().longValue()));
		}

		for (Map.Entry<String, Integer> entry : form.getDishSets().entrySet()) {
			shopOrderManagement.setProductAndQuantityAsLines(shopOrderManagement.findByShopOrderId((String) model.getAttribute("newShopOrderId")).get().getId(),
					catalogManagement.findByName(entry.getKey()),
					Quantity.of(entry.getValue().longValue()));
		}

		calculateDiscounts(model, form);
	}

}
