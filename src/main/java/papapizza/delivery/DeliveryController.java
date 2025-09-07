package papapizza.delivery;

import com.mysema.commons.lang.Assert;
import lombok.NonNull;
import org.salespointframework.order.OrderLine;
import org.salespointframework.time.BusinessTime;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.web.LoggedIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import papapizza.employee.Employee;
import papapizza.employee.EmployeeManagement;
import papapizza.inventory.ProductCategory;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.inventory.items.VehicleProduct;
import papapizza.order.ShopOrder;
import papapizza.order.ShopOrderManagement;
import papapizza.order.ShopOrderState;

import javax.money.MonetaryAmount;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Delivery Controller class transmit all necessary data about a {@link ShopOrder} to the frontend
 */
@Controller
public class DeliveryController {



	/**
	 * A Logger to output, mostly debugging-, data into the LOG
	 */
	private final Logger logger = LoggerFactory.getLogger(DeliveryController.class);
	/**
	 * Required service class for finding, manipulating or validating a {@link ShopOrder} for delivery purposes
	 */
	private final DeliveryManagement deliveryManagement;
	/**
	 * Required service class for finding, manipulating or validating a {@link ShopOrder}
	 */
	private final ShopOrderManagement<ShopOrder> shopOrderManagement;
	/**
	 * Required service class for finding, manipulating or validating a {@link Employee}, specifically a driver,
	 * an Employee with {@link Employee#getRole()}="Driver"
	 */
	private EmployeeManagement employeeManagement;
	/**
	 * Required service class for time calculations
	 */
	private final BusinessTime businessTime;
	/**
	 * Required service class for finding, manipulating or validating a {@link org.salespointframework.catalog.Product},
	 * specifically a {@link VehicleProduct}
	 */
	private final ShopCatalogManagement shopCatalogManagement;

	/**
	 * Constructor: sets up all required service classes
	 * @param deliveryManagement needed for order processing
	 * @param shopOrderManagement needed for order processing
	 * @param businessTime needed for time calculations
	 * @param shopCatalogManagement needed for working with {@link VehicleProduct}
	 */
	@Autowired
	DeliveryController(@NonNull final DeliveryManagement deliveryManagement, @NonNull final ShopOrderManagement<ShopOrder> shopOrderManagement,
					   @NonNull final BusinessTime businessTime,
					   @NonNull final ShopCatalogManagement shopCatalogManagement){
		this.deliveryManagement = deliveryManagement;
		this.shopOrderManagement = shopOrderManagement;
		this.businessTime = businessTime;
		this.shopCatalogManagement = shopCatalogManagement;
	}


	@Autowired
	public void setEmployeeManagement(@NonNull EmployeeManagement employeeManagement){
		this.employeeManagement = employeeManagement;
	}


	@GetMapping("/delivery")
	@PreAuthorize("hasAnyRole('BOSS', 'DRIVER')")
	String deliveryView(Model model, @LoggedIn UserAccount loggedInUser){
		Assert.isTrue(employeeManagement.findByUsername(loggedInUser.getUsername()).isPresent(), "Employee does not Exist");
		Employee employee1 = employeeManagement.findByUsername(loggedInUser.getUsername()).get();
		List<DisplayableDelivery> deliveries = new ArrayList<>();
		for (ShopOrder order : shopOrderManagement.findAll()) {//All ShopOrders
			if (order.getShopOrderState() == ShopOrderState.READYDELIVER || order.getShopOrderState() == ShopOrderState.INDELIVERY) {//Only Orders ready to deliver or already in Delivery
				DisplayableDelivery displayableDelivery = new DisplayableDelivery();
				displayableDelivery.setCustomerName(order.getCustomer().getFirstname() + " " + order.getCustomer().getLastname());
				displayableDelivery.setDeliveryId(Objects.requireNonNull(order.getId()).getIdentifier());
				List<String> orderLines = new ArrayList<>();
				for (OrderLine orderLine : order.getOrderLines()) {		//Adding up OrderLines and Quantity
					orderLines.add(orderLine.getQuantity() + "x " + orderLine.getProductName());
				}
				displayableDelivery.setOrderLines(orderLines);
				displayableDelivery.setCustomerTelephoneNumber(order.getCustomer().getPhone());
				displayableDelivery.setCustomerAddress(order.getCustomer().getAddress());
				displayableDelivery.setDeliveryState(order.getShopOrderState().toString());
				Assert.isTrue(order.getOrderLines().stream().map(OrderLine::getPrice).reduce(MonetaryAmount::add).isPresent(), "Order(price) does not exist");
				displayableDelivery.setDeliveryPrice(order.getOrderLines().stream().map(OrderLine::getPrice).reduce(MonetaryAmount::add).get().toString());
				displayableDelivery.setTimeLeft(deliveryManagement.calcTimeLeft(order, this.businessTime));
				if (order.getDriver().equals(employee1) || Objects.equals(employee1.getRole(), "Boss")) {
					displayableDelivery.setViewable(true);    //Visible for driver, not only for boss, boss can view everything
				}
				deliveries.add(displayableDelivery);
			}
		}
		if(Objects.equals(employeeManagement.findByUsername(loggedInUser.getUsername()).get().getRole(), "Driver")){
			if(deliveryManagement.calcWaitingTime(employeeManagement.findByUsername(loggedInUser.getUsername()).get().getVehicle()) == null){
				model.addAttribute("waitingTime", null);
			}else{
				String waitingTimeString = deliveryManagement.calcWaitingTime(employeeManagement.findByUsername(loggedInUser.getUsername()).get().getVehicle()).toSeconds() / 60L + "m " +
						deliveryManagement.calcWaitingTime(employeeManagement.findByUsername(loggedInUser.getUsername()).get().getVehicle()).toSeconds() % 60 + "s ";
				model.addAttribute("waitingTime", waitingTimeString);
			}
		}else{
			model.addAttribute("waitingTime", "Boss currently cannot view waiting times, coming soon...");
		}
		model.addAttribute("deliveries", deliveries);
		model.addAttribute("vehicleMode", deliveryManagement.isManualAssign());//TODO: display in frontend
		model.addAttribute("orderMode", DeliveryManagement.getOrderAssign() == DeliveryManagement.deliveryStrategy.CAPACITY);
		return "delivery/delivery";
	}

	@GetMapping("/delivery/settings")
	@PreAuthorize("hasRole('BOSS')")
	String reassignVehicleView(Model model, @ModelAttribute("settingsForm") SettingsForm form){
		List<Employee> drivers = employeeManagement.findByRole("Driver").toList();
		List<VehicleProduct> vehicles = shopCatalogManagement.findByCategory(ProductCategory.VEHICLE.toString()).map(
				vehicle -> (VehicleProduct) vehicle).toList();
		model.addAttribute("drivers", drivers);
		model.addAttribute("vehicles", vehicles);
		form.setManualAssign(deliveryManagement.isManualAssign());
		form.setOrderAssign(DeliveryManagement.getOrderAssign() == DeliveryManagement.deliveryStrategy.CAPACITY);
		return "/delivery/settings";
	}

	@PostMapping("/delivery/reassign/{id}")
	@PreAuthorize("hasRole('BOSS')")
	String reassignVehicle(@PathVariable long id, @Valid @ModelAttribute SettingsForm form){
		if(employeeManagement.findById(id).isEmpty()){
			throw new RuntimeException();
		}else{
			Employee employee = employeeManagement.findById(id).get();
			assert shopCatalogManagement.findById(form.getVehicleId()).isPresent();
			employee.setVehicle((VehicleProduct) shopCatalogManagement.findById(form.getVehicleId()).get());
			employeeManagement.save(employee);
		}
		return "redirect:/delivery/settings";
	}

	@PostMapping("/delivery/complete/{id}")
	@PreAuthorize("hasAnyRole('BOSS', 'DRIVER')")		
	String completeOrder(@PathVariable String id){	//Using the "Fertigstellen" Button to mark a Delivery as Completed
		Assert.isTrue(shopOrderManagement.findByShopOrderId(id).isPresent(), "ShopOrder does not exist");
		ShopOrder shopOrder = shopOrderManagement.findByShopOrderId(id).get();
		shopOrder.setShopOrderState(ShopOrderState.COMPLETED);
		shopOrder.setTimeCompleted(LocalDateTime.now()); // needed for analytics
		deliveryManagement.unassignDriver(shopOrder);
		deliveryManagement.rejoinOrder(shopOrder);
		shopOrderManagement.save(shopOrder);
		return "redirect:/delivery";
	}

	@PostMapping("/delivery/drive")
	@PreAuthorize("hasAnyRole('DRIVER', 'BOSS')")
	String startDriving(@LoggedIn UserAccount user){
		assert employeeManagement.findByUsername(user.getUsername()).isPresent();
		VehicleProduct currentVehicle = employeeManagement.findByUsername(user.getUsername()).get().getVehicle();
		currentVehicle.setInUse(true);
		currentVehicle.setWaitingTime(null);	//TODO: Catch NullPointerException
		List<ShopOrder> vehicleOrders = deliveryManagement.getOrdersFromVehicle(currentVehicle);
		for(ShopOrder order : vehicleOrders){
			shopOrderManagement.setShopOrderState(order, ShopOrderState.INDELIVERY);
		}
		shopCatalogManagement.save(currentVehicle);
		return "redirect:/delivery";
	}

	@PostMapping("/delivery/settings/switchAlg")
	@PreAuthorize("hasRole('BOSS')")
	String switchAlg(@Valid @ModelAttribute("settingsForm") SettingsForm form){
		if(DeliveryManagement.getOrderAssign() == DeliveryManagement.deliveryStrategy.CAPACITY){
			DeliveryManagement.setOrderAssign(DeliveryManagement.deliveryStrategy.SPEED);
		}else{
			DeliveryManagement.setOrderAssign(DeliveryManagement.deliveryStrategy.CAPACITY);
		}
		return "redirect:/delivery/settings";
	}

	@PostMapping("/delivery/settings/switchAssign")
	@PreAuthorize("hasRole('BOSS')")
	String switchAssign(@Valid @ModelAttribute("settingsForm") SettingsForm form){
		deliveryManagement.setManualAssign(!deliveryManagement.isManualAssign());
		return "redirect:/delivery/settings";
	}



}
