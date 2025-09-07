package papapizza.delivery;


import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.salespointframework.order.OrderLine;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.time.BusinessTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import papapizza.employee.Employee;
import papapizza.employee.EmployeeCreationForm;
import papapizza.employee.EmployeeManagement;
import papapizza.inventory.ProductCategory;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.inventory.items.VehicleProduct;
import papapizza.order.DeliveryType;
import papapizza.order.ShopOrder;
import papapizza.order.ShopOrderManagement;
import papapizza.order.ShopOrderState;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Management class for the delivery module
 * Contains all methods required to deliver a ShopOrder: Assignment of Vehicles and Drivers, some sorting and validation functions
 * Mainly uses ShopOrder, Employees and VehicleProducts
 * @see Employee
 * @see VehicleProduct
 * @see ShopOrder
 */

@Service
@Transactional
public class DeliveryManagement {

	/**
	 * Standard duration any order should be delivered in
	 * @deprecated
	 * @see DeliveryManagement#MAX_ORDER_DURATION
	 */
	public static final int MAX_TIME = 45;
	/**
	 * The amount of time needed to deliver a single order, from {@link ShopOrderState#INDELIVERY} to {@link ShopOrderState#COMPLETED}
	 */
	public static final Duration SINGLE_DELIVERY_TIME = Duration.ofSeconds(450);
	/**
	 * The amount of maximum time between any two orders, leaving the kitchen, for them to still go into the same {@link VehicleProduct},
	 * when using {@link DeliveryManagement#getEfficientVehicles()} algorithm
	 */
	public static final Duration ARBITRARY_WAITING_TIME_EFFICIENT = Duration.ofSeconds(600);
	/**
	 * The maximum amount of time between two orders initializations for them to still go into the same {@link VehicleProduct}
	 * when using the {@link DeliveryManagement#getFastVehicles()} algorithm
	 */
	public static final Duration ARBITRARY_WAITING_TIME_FAST = Duration.ofSeconds(300);
	/**
	 * The time an order is estimated to wait before the start of its delivery, used by {@link DeliveryManagement#deliveryShopOrderTimeEstimate(ShopOrder)}
	 */
	public static final Duration ARBITRARY_WAITING_TIME_ESTIMATE = Duration.ofSeconds(301);
	/**
	 * The maximum amount of time any order should take from initialization to completion
	 */
	public static final Duration MAX_ORDER_DURATION = Duration.ofMinutes(45);
	/**
	 * Required service class for time calculations
	 */
	public final BusinessTime businessTime;
	/**
	 * Required service class for finding, manipulating or validating a {@link ShopOrder}
	 */
	public final ShopOrderManagement<ShopOrder> shopOrderManagement;
	/**
	 * Required service class for finding, manipulating or validating a {@link Employee}, specifically a driver,
	 * an Employee with {@link Employee#role}="Driver"
	 */
	@SuppressWarnings("JavadocReference")
	public EmployeeManagement employeeManagement;
	/**
	 * Required service class for finding, manipulating or validating a {@link org.salespointframework.catalog.Product},
	 * specifically a {@link VehicleProduct}
	 */
	public ShopCatalogManagement shopCatalogManagement;
	/**
	 * A Logger to output, mostly debugging-, data into the LOG
	 */
	private final Logger logger = LoggerFactory.getLogger(DeliveryManagement.class);

	/**
	 * A queue of ShopOrders that could not be assigned
	 * Gets filled with ShopOrders that did not fit any available vehicle
	 * Periodically checked for free vehicle slots whenever an order is completed
	 * @see ShopOrderManagement#setShopOrderState(ShopOrder, ShopOrderState)
	 */
	@Getter
	private final Queue<ShopOrder> noVehicleAssignedOrders;
	/**
	 * Stores whether vehicles are automatically assigned to drivers upon driver creation
	 * @see EmployeeManagement#createEmployee(EmployeeCreationForm)
	 * States: true = manual assign, false = auto assign
	 * When set to manual assign, drivers don't vehicles assigned automatically, boss has to manually assign them in the settings page
	 */
	@Setter
	@Getter
	private boolean manualAssign;

	/**
	 * Stores whether orders are assigned to drivers, and their respective vehicles, for fast or efficient deliveries
	 * Fast deliveries are optimized for speed and short delivery times
	 * Efficient deliveries are optimized for a good use of vehicle capacities
	 * States: true = efficient delivery, false = fast delivery
	 * Used in {@link DeliveryManagement#getEfficientVehicles()} and {@link DeliveryManagement#getFastVehicles()} respectively
	 */

	public enum deliveryStrategy {
		CAPACITY, SPEED
	}

	@Setter
	@Getter
	static private deliveryStrategy orderAssign = deliveryStrategy.SPEED;

	/**
	 * Constructor: sets up all required classes
	 * @param businessTime is needed for time calculations
	 * @param shopOrderManagement is needed to process and validate ShopOrders
	 */

	public DeliveryManagement(@NonNull BusinessTime businessTime,
					   @NonNull ShopOrderManagement<ShopOrder> shopOrderManagement){

		this.businessTime = businessTime;
		this.shopOrderManagement = shopOrderManagement;
		this.noVehicleAssignedOrders = new ArrayDeque<>();

	}

	/**
	 * Method separat from Constructor to avoid a bean cycle
	 * @param employeeManagement is needed to assign Drivers, Employees,
	 */

	@Autowired
	public void setEmployeeManagement(@NonNull EmployeeManagement employeeManagement){
		this.employeeManagement = employeeManagement;
	}
	/**
	 * Method separat from Constructor to avoid a bean cycle
	 * @param shopCatalogManagement is needed to work with {@link VehicleProduct}
	 */
	@Autowired
	public void setShopCatalogManagement(ShopCatalogManagement shopCatalogManagement){
		this.shopCatalogManagement = shopCatalogManagement;
	}

	/**
	 * Splits Orders into two smaller ShopOrders to, needed to deliver ShopOrder that don't fit into any single available Vehicle
	 * @param parent The order that is meant to be split
	 * @param splitAfter The available slots in a vehicle, Order will be split after the amounts of slots specified
	 * @return List of ShopOrder, first of them being the parent ShopOrder, followed by the newly created splitOrders
	 */

	public List<ShopOrder> splitOrder(ShopOrder parent, int splitAfter){
		List<ShopOrder> splitOrderList = new ArrayList<>();
		if(splitAfter >  0){
			splitOrderList.add(parent);		//Parent Order is always place #1 in returned List
			ShopOrder newShopOrder1 = cloneOrder(parent);
			ShopOrder newShopOrder2 = cloneOrder(parent);
			newShopOrder1.setParentId(Objects.requireNonNull(parent.getId()).getIdentifier());
			newShopOrder2.setParentId(Objects.requireNonNull(parent.getId()).getIdentifier());
			int counter = 0;
			for(OrderLine line : parent.getOrderLines()){
				if(counter + line.getQuantity().getAmount().intValue() <= splitAfter){	//OrderLine fits in newShopOrder1
					newShopOrder1.addOrderLine(shopCatalogManagement.findById(line.getProductIdentifier()), line.getQuantity());
					counter += line.getQuantity().getAmount().intValue();
				}else if(counter < splitAfter){		//OrderLine does not completely fit in newShopOrder1
					int spaceLeft = splitAfter - counter;
					int actualSize = line.getQuantity().getAmount().intValue();
					newShopOrder1.addOrderLine(shopCatalogManagement.findById(line.getProductIdentifier()), Quantity.of(spaceLeft));
					newShopOrder2.addOrderLine(shopCatalogManagement.findById(line.getProductIdentifier()), Quantity.of((long)actualSize - (long)spaceLeft));
				}else{
					newShopOrder2.addOrderLine(shopCatalogManagement.findById(line.getProductIdentifier()), line.getQuantity());
				}

			}
			splitOrderList.add(newShopOrder1);
			splitOrderList.add(newShopOrder2);
		}else {
			logger.info("No Vehicle Available");
		}

		return splitOrderList;

	}

	public Duration calcWaitingTime(VehicleProduct vehicle){
		List<ShopOrder> vehicleOrders = getOrdersFromVehicle(vehicle).stream()
				.filter(o -> o.getShopOrderState() == ShopOrderState.READYDELIVER).collect(Collectors.toList());
		logger.info("vehicleOrders: " + vehicleOrders);
		if(vehicleOrders.isEmpty()){
			return null;
		}
		if(orderAssign == deliveryStrategy.CAPACITY){
			LocalDateTime oldestTime = LocalDateTime.now();
			for(ShopOrder order : vehicleOrders){
				if(order.getTimeCreated().isBefore(oldestTime)){
					oldestTime = order.getTimeCreated();
				}
			}
			ShopOrder newestOrder = null;
			LocalDateTime newestTime = oldestTime;
			if(vehicleOrders.size() > 1){
				for(ShopOrder order : vehicleOrders){
					if(order.getTimeCreated().isAfter(newestTime)){
						newestOrder = order;
						newestTime = order.getTimeCreated();
					}
				}
			}else{
				newestOrder = vehicleOrders.get(0);
			}
			assert newestOrder != null;
			return Duration.between(LocalDateTime.now(), newestTime.plus(newestOrder.getKitchenTimeEstimate().plus(ARBITRARY_WAITING_TIME_EFFICIENT)));

		}else{
			ShopOrder oldestOrder = null;
			LocalDateTime oldestTime = LocalDateTime.now();
			for(ShopOrder order : vehicleOrders){
				if(order.getTimeCreated().isBefore(oldestTime)){
					oldestTime = order.getTimeCreated();
					oldestOrder = order;
				}
			}
			assert oldestOrder != null;
			return Duration.between(LocalDateTime.now(), oldestTime.plus(oldestOrder.getKitchenTimeEstimate().plus(ARBITRARY_WAITING_TIME_FAST)));
		}

	}

	/**
	 * Method for rejoining a split ShopOrder, split by {@link DeliveryManagement#splitOrder(ShopOrder, int)}
	 * @param shopOrder A split ShopOrder to be rejoined
	 * @return Remerged ShopOrder to be saved in Analytics
	 * @see papapizza.analytics.AnalyticsManagement
	 */

	public ShopOrder rejoinOrder (ShopOrder shopOrder){//FIXME: ParentOrder won't be found when deleteParents is executed
		String id = Objects.requireNonNull(shopOrder.getId()).getIdentifier();
		List<ShopOrder> splitOrders = new ArrayList<>();
		assert shopOrderManagement.findByShopOrderId(id).isPresent();
		ShopOrder parentOrder = shopOrderManagement.findByShopOrderId(id).get();
		boolean allCompleted = true;
		for (ShopOrder order : shopOrderManagement.findAll()){
			if(order.getParentId() != null && order.getParentId().equals(shopOrder.getParentId())){
				splitOrders.add(order);
				if(!order.getShopOrderState().equals(ShopOrderState.COMPLETED)){
					allCompleted = false;
				}
			}
		}
		List<OrderLine> splitLines = new ArrayList<>();
		for (ShopOrder order : splitOrders){
			for (OrderLine line : order.getOrderLines()){
				splitLines.add(line);
			}
		}
		if(allCompleted){
			for(ShopOrder splitOrder : splitOrders){
				shopOrderManagement.delete(splitOrder);
			}
			return parentOrder;
		}else{
			return null;
		}
	}

	/**
	 * Frees up Vehicle Capacity after a shopOrder is completed
	 * @param shopOrder The ShopOrder that is completed
	 */

	public void unassignDriver(ShopOrder shopOrder){
		Employee assignedDriver = shopOrder.getDriver();
		long orderSize = getOrderSize(shopOrder);
		assignedDriver.getVehicle().setUsedSlots((int) (assignedDriver.getVehicle().getUsedSlots() - orderSize));
	}

	/**
	 * Simple method that checks whether a vehicle is already assigned to an Employee
	 * @param vehicle The vehicle that is to be checked
	 * @return A boolean that specifies whether the vehicle is already assigned
	 */

	public boolean vehicleIsAlreadyAssigned(VehicleProduct vehicle){
		for (Employee employee : employeeManagement.findByRole("Driver")){
			if (employee.getVehicle().equals(vehicle)){
				return true;	//found employee that already has vehicle assigned
			}
		}
		return false;
	}

	/**
	 * A method for checking how much time is left to complete a given ShopOrder, influenced by MAX_TIME, the maximum time a ShopOrder should take before being completed
	 * @param order The order to be checked
	 * @param businessTime The BusinessTime service class for time calculation
	 * @return A long specifying the number of minutes left
	 */

	public long calcTimeLeft(ShopOrder order, BusinessTime businessTime){

		LocalDateTime from = order.getTimeCreated();
		LocalDateTime to = businessTime.getTime();
		Duration duration = Duration.between(from, to);
		return MAX_TIME - duration.toMinutes();
	}

	/**
	 * A method to clone a ShopOrder, used by {@link DeliveryManagement#splitOrder(ShopOrder, int)}
	 * @param cloneFrom The ShopOrder that is to be cloned
	 * @return A new ShopOrder that inherits the attributes of cloneFrom, except for the OrderLines
	 */

	public ShopOrder cloneOrder(ShopOrder cloneFrom){
		ShopOrder cloneInto = new ShopOrder(employeeManagement.getDummyAccount(), cloneFrom.getCustomer());
		cloneInto.setChefs(cloneFrom.getChefs());
		shopOrderManagement.setShopOrderState(cloneInto, cloneFrom.getShopOrderState());
		cloneInto.setOpenDuration(cloneFrom.getOpenDuration());
		cloneInto.setPendingDuration(cloneFrom.getPendingDuration());
		cloneInto.setReadyDuration(cloneFrom.getReadyDuration());
		cloneInto.setInDeliverDuration(cloneFrom.getInDeliverDuration());
		cloneInto.setTotalDuration(cloneFrom.getTotalDuration());
		shopOrderManagement.save(cloneInto);
		return cloneInto;
	}

	/**
	 * A simple method for calculation the size of a {@link ShopOrder}
	 * Only Pizzas are counted, other parts of the {@link ShopOrder} are considered to take up no space in a vehicle
	 * @param shopOrder The {@link ShopOrder} which size is to be determined
	 * @return A long specifying the number of slots required
	 */

	public long getOrderSize(ShopOrder shopOrder){
		List<OrderLine> orderLinesPizza = shopOrder.getOrderLines().stream()
				.filter(orderLine -> shopCatalogManagement.findById(orderLine.getProductIdentifier()).getCategories().toList().contains("Pizza") ||
						shopCatalogManagement.findById(orderLine.getProductIdentifier()).getCategories().toList().contains("CustomPizza"))
				.collect(Collectors.toList());
		long shopOrderSize;
		if(orderLinesPizza.stream().map(OrderLine::getQuantity).reduce(Quantity::add).isPresent())
		 	shopOrderSize = orderLinesPizza.stream().map(OrderLine::getQuantity).reduce(Quantity::add).get().getAmount().longValue();
		else{

			shopOrderSize = 0;
		}
		return shopOrderSize;
	}

	/**
	 * A method to assign a Driver to the specified {@link ShopOrder}, also assigns {@link VehicleProduct} to Drivers
	 * Only assigns Vehicles to drivers without vehicles when automatic assignment is turned on
	 * @param shopOrder The ShopOrder that is to be assigned to a driver/vehicle
	 * @return The {@link Employee} that the system determined to be the best fit
	 * Influenced by the orderAssign mode
	 */

	public Employee assignDriver(@NonNull ShopOrder shopOrder){
		if(!shopOrder.getDeliveryType().equals(DeliveryType.DELIVERY)) throw new IllegalArgumentException("order: " + shopOrder.getId().getIdentifier() + "is no Delivery-Type Order but assignDriver() was called");

		List<VehicleProduct> assignedVehicles = employeeManagement.findAll()
				.filter(e -> e.isMetaNormal() && e.getRole().equals("Driver"))
				.map(Employee::getVehicle).toList();
		int allAssignedVehicleSlots = assignedVehicles.stream().map(VehicleProduct::getSlots).reduce(0, Integer::sum);
		if(getOrderSize(shopOrder) > allAssignedVehicleSlots) throw new IllegalArgumentException("order-size of order: " + shopOrder.getId().getIdentifier() + " is too large for available Vehicles");

		List<VehicleProduct> vehicleList = orderAssign.equals(deliveryStrategy.CAPACITY) ? getEfficientVehicles() : getFastVehicles();
		int allAvailableVehicleSlots = vehicleList.stream().map(VehicleProduct::getSlots).reduce(0, Integer::sum);
		if(getOrderSize(shopOrder) > allAssignedVehicleSlots || vehicleList.isEmpty()){
			noVehicleAssignedOrders.add(shopOrder); return null;
		}

		int maxCapVehicleSlots = vehicleList.stream().map(v -> v.getSlots() - v.getUsedSlots()).max(Integer::compareTo).get();
		if(getOrderSize(shopOrder) > maxCapVehicleSlots){
			List<ShopOrder> splitResult = splitOrder(shopOrder, maxCapVehicleSlots);
			assignDriver(splitResult.get(1));//splitOrder
			assignDriver(splitResult.get(2));//restOrder
			deleteParents();
		} else {
			for(VehicleProduct vehicle : vehicleList){
				if(vehicle.getSlots() - vehicle.getUsedSlots() >= getOrderSize(shopOrder)){
					assert employeeManagement.findByRole("Driver").stream().anyMatch(e -> Objects.equals(e.getVehicle().getId(), vehicle.getId()));
					Employee assignedEmployee = employeeManagement.findByRole("Driver").stream().filter(e -> Objects.equals(e.getVehicle().getId(), vehicle.getId())).findFirst().get();
					vehicle.setUsedSlots((int)(vehicle.getUsedSlots() + getOrderSize(shopOrder)));
					shopOrder.setDriver(assignedEmployee);
					shopOrderManagement.save(shopOrder);
					shopCatalogManagement.save(vehicle);
					return assignedEmployee;
				}
			}
		}
		return null;
	}
/*
	public Employee assignDriver(ShopOrder shopOrder){	//TODO: Check for edgecases, Cases ordered from best to worst
		Map<Employee, Long> map = new HashMap<>();	//Map of every employee with space left as Long
		for(Employee employee : employeeManagement.findByRole("Driver")){
			map.put(employee, (long) (employee.getVehicle().getSlots() - employee.getVehicle().getUsedSlots()));
		}
		long shopOrderSize = getOrderSize(shopOrder);

		//--- Case One: [BEST CASE] Order does not have to be split, fits perfectly to fill a car TODO: Set States to INDELIVERY, set READYDELIVER_TIME (Or not?! Order could be set to INDELIVERY before it actually is, has to pass kitchen first---
		for(Map.Entry<Employee, Long> entry : map.entrySet()){
			if(entry.getValue() == shopOrderSize){	//Found employee with vehicle that fits orderSize perfectly
				entry.getKey().getVehicle().setUsedSlots(entry.getKey().getVehicle().getSlots());
				shopOrder.setDriver(entry.getKey());
				logger.info("--- Case 1 ---");
				return entry.getKey();
			}
		}
		//--- Case two: Order does not have to be split, but does not fill a car completely, has to wait ---
		//TODO: Assess whether or not a not completely filled car has to drive away early to meet time requirements
		Employee employeeWithLeastSpaceThatStillFits = null;
		long leastSpace = Long.MAX_VALUE;

		for(Map.Entry<Employee, Long> entry : map.entrySet()){
			if((entry.getValue() < leastSpace) && (shopOrderSize < entry.getValue())){
				leastSpace = entry.getValue();
				employeeWithLeastSpaceThatStillFits = entry.getKey();
				//TODO: reduce slots left for driver consistently across function
			}
		}
		if(employeeWithLeastSpaceThatStillFits != null){
			employeeWithLeastSpaceThatStillFits.getVehicle().setUsedSlots((int) ( employeeWithLeastSpaceThatStillFits.getVehicle().getUsedSlots() + shopOrderSize));	//Reduces free slots in vehicle
			shopOrder.setDriver(employeeWithLeastSpaceThatStillFits);
			logger.info("--- Case 2 ---");
			return employeeWithLeastSpaceThatStillFits;

		}else{	//There is no Employee that has more space left than shopOrderSize, aka: ShopOrder does not fit ANY employee
			//-- Case three: [WORST CASE] Order has to be split, possibly multiple times, to fit a vehicle
			//TODO: Rejoin ShopOrders before setting them to complete
			Employee employeeToFill = null;
			for(Map.Entry<Employee, Long> entry : map.entrySet()){
				if(entry.getValue() < leastSpace && entry.getValue() > 0){
					leastSpace = entry.getValue();
					employeeToFill = entry.getKey();
				}
			}
			LOG.info("--- Case 3 ---");
			assert employeeToFill != null;
			List<ShopOrder> splitShopOrders = splitOrder(shopOrder, (int) leastSpace);

			splitShopOrders.get(1).setDriver(employeeToFill);
			employeeToFill.getVehicle().setUsedSlots(employeeToFill.getVehicle().getSlots());
			assignDriver(splitShopOrders.get(2));
			return employeeToFill;
		}
		//--- Case four: [EXCEPTION] Order can not be assigned, no capacity	FIXME
		//TODO: Add to Queue
	}
*/
	/**
	 * A cleanup function that removes artifacts/invalid ShopOrders that were created by {@link DeliveryManagement#splitOrder(ShopOrder, int)}
	 * {@link DeliveryManagement#splitOrder(ShopOrder, int)} leaves the original "parent" {@link ShopOrder} in the repository, therefore creating duplicates
	 * This function removes these duplicates
	 */

	public void deleteParents(){
		List<ShopOrder> deleteOrders = new ArrayList<>();
		for(ShopOrder shopOrder : shopOrderManagement.findAll()){
			if(shopOrder.getParentId() != null){
				Optional<ShopOrder> parentOrder = shopOrderManagement.findByShopOrderId(shopOrder.getParentId());
				if(parentOrder.isPresent()){
					if(parentOrder.get().getParentId() != null){
						shopOrderManagement.findAll().stream().filter(filterShopOrder -> (filterShopOrder.getParentId() != null) && filterShopOrder.getParentId()
								.equals(Objects.requireNonNull(parentOrder.get().getId()).getIdentifier()))
								.forEach(o -> o.setParentId(parentOrder.get().getParentId()));
					}
					deleteOrders.add(parentOrder.get());
				}
			}
		}
		for(ShopOrder shopOrder : deleteOrders){
			shopOrderManagement.delete(shopOrder);
		}
	}

	/**
	 * Assigns a {@link VehicleProduct} to a Driver, if and only if, automatic assignment is turned on
	 * If manual assignment is turned on, the {@link Employee} gets a "Meta-Vehicle" assigned, meaning he has no vehicle assigned
	 * The Meta Vehicle solves numerous NullPointerExceptions that occur when a Driver has no vehicle
	 * @param employee The Employee who should get a vehicle assigned
	 * @return A boolean specifying the success of the operation
	 */

	public boolean assignVehicle(Employee employee){
		if((employee.getVehicle() == null) && !this.manualAssign){
			List<VehicleProduct> vehicles = shopCatalogManagement.findByCategory(ProductCategory.VEHICLE.toString()).stream().map(product -> (VehicleProduct) product).collect(Collectors.toList());
			for(VehicleProduct vehicle : vehicles){
				if(!vehicleIsAlreadyAssigned(vehicle)){//returns true, if vehicle isn't already assigned
					employee.setVehicle(vehicle);
				}
			}
		}else if((employee.getVehicle() == null) && this.manualAssign){
			employee.setVehicle(shopCatalogManagement.findMetaVehicle());
		}
		return employee.getVehicle() != null;	//TODO: handle returned boolean
	}

	/**
	 * A method returning the estimated time a {@link ShopOrder} takes to get delivered
	 * Only accounts for the time spend in the delivery module, the estimated time spend in the kitchen is calculated separately
	 * @see papapizza.kitchen.KitchenManagement#kitchenShopOrderTimeEstimate(ShopOrder)
	 * @param order The ShopOrder for which the time is to be estimated
	 * @return A Duration specifying the estimated time until delivery
	 */

	public Duration deliveryShopOrderTimeEstimate(@NonNull ShopOrder order){

		if(order.getParentId() == null)
			return Duration.ofSeconds(getOrdersFromVehicle(order.getDriver().getVehicle()).size() * SINGLE_DELIVERY_TIME.toSeconds() + ARBITRARY_WAITING_TIME_ESTIMATE.toSeconds());
		else {
			Duration duration = Duration.ZERO;
			List<Duration> durations = shopOrderManagement.findByParentId(order.getParentId())
					.map(this::deliveryShopOrderTimeEstimate).collect(Collectors.toList());
			for (Duration d : durations) duration = (d.toSeconds() > duration.toSeconds()) ? d : duration;
			return duration;
		}
	}

	/**
	 * A method generating a List of {@link VehicleProduct} that are to be considered for an efficient delivery
	 * Also validates the vehicles: Vehicle can't be a "Meta-Vehicle" and must have a driver assigned
	 * Uses {@link DeliveryManagement#sortVehicleCapDescending(List)} to fill almost full vehicles first
	 * @see DeliveryManagement#getFastVehicles() analog for a fast delivery
	 * @return A List of Vehicles to be used by {@link DeliveryManagement#assignDriver(ShopOrder)}
	 */

	public List<VehicleProduct> getEfficientVehicles(){//TODO: make efficientVehicleList sorted by slots
		List<VehicleProduct> assignedVehicles = employeeManagement.findAll()
				.filter(e -> e.isMetaNormal() && e.getRole().equals("Driver"))
				.map(Employee::getVehicle).toList();

		List<VehicleProduct> efficientVehicles = shopCatalogManagement.findByCategory(ProductCategory.VEHICLE.toString())
				.map(p -> (VehicleProduct) p).stream()
				.filter(v -> v.getMeta().equals("NORMAL"))
				.filter(assignedVehicles::contains).collect(Collectors.toList());
		//filter: last pizza that got ready in this car should be at most ARBITRARY_WAITING_TIME_EFFICIENT min old
		{
			List<VehicleProduct> temp = efficientVehicles;
			efficientVehicles = new ArrayList();
			for (VehicleProduct vehicle : temp) {
				List<ShopOrder> ordersFromVehicle = getOrdersFromVehicle(vehicle);
				if (ordersFromVehicle.isEmpty()) efficientVehicles.add(vehicle);
				else {
					List<Duration> orderDurations = new ArrayList<>();
					for (ShopOrder order : ordersFromVehicle) {
						if (order.getKitchenTimeEstimate() == null)
							orderDurations.add(Duration.between(order.getTimeCreated(), LocalDateTime.now()));
						else
							orderDurations.add(Duration.between(order.getTimeCreated().plus(order.getKitchenTimeEstimate()), LocalDateTime.now()));
					}
					if (orderDurations.stream().min(Duration::compareTo).get().toSeconds() <= ARBITRARY_WAITING_TIME_EFFICIENT.toSeconds())
						efficientVehicles.add(vehicle);
				}
			}
		}
		//filter: pizza should arrive in time of MAX_ORDER_DURATION
		{
			efficientVehicles = efficientVehicles.stream()
					.filter(v -> getOrdersFromVehicle(v).size() * SINGLE_DELIVERY_TIME.toMinutes() + ARBITRARY_WAITING_TIME_EFFICIENT.toMinutes() + SINGLE_DELIVERY_TIME.toMinutes() <= MAX_ORDER_DURATION.toMinutes())
					.collect(Collectors.toList());
		}
		//sort: descending Capacity
		return sortVehicleCapDescending(efficientVehicles);
	}

	/**
	 * Analog to {@link DeliveryManagement#getEfficientVehicles()}, but optimized for delivery speed
	 * uses {@link DeliveryManagement#sortVehicleCapAscending(List)}
	 * @return A List of Vehicles to be used by {@link DeliveryManagement#assignDriver(ShopOrder)}
	 */

	public List<VehicleProduct> getFastVehicles(){
		List<VehicleProduct> assignedVehicles = employeeManagement.findAll()
				.filter(e -> e.isMetaNormal() && e.getRole().equals("Driver"))
				.map(Employee::getVehicle).toList();

		List<VehicleProduct> fastVehicles = shopCatalogManagement.findByCategory(ProductCategory.VEHICLE.toString())
				.map(p -> (VehicleProduct) p).stream()
				.filter(v -> v.getMeta().equals("NORMAL"))
				.filter(assignedVehicles::contains).collect(Collectors.toList());
		//filter: first pizza that got ready in this car should be at most ARBITRARY_WAITING_TIME_FAST min old
		{
			List<VehicleProduct> temp = fastVehicles;
			fastVehicles = new ArrayList();
			for (VehicleProduct vehicle : temp) {
				List<ShopOrder> ordersFromVehicle = getOrdersFromVehicle(vehicle);
				if (ordersFromVehicle.isEmpty()) fastVehicles.add(vehicle);
				else {
					List<Duration> orderDurations = new ArrayList<>();
					for (ShopOrder order : ordersFromVehicle) {
						if (order.getKitchenTimeEstimate() == null)
							orderDurations.add(Duration.between(order.getTimeCreated(), LocalDateTime.now()));
						else
							orderDurations.add(Duration.between(order.getTimeCreated().plus(order.getKitchenTimeEstimate()), LocalDateTime.now()));
					}
					if (orderDurations.stream().max(Duration::compareTo).get().toSeconds() <= ARBITRARY_WAITING_TIME_FAST.toSeconds())
						fastVehicles.add(vehicle);
				}
			}
		}
		//filter: pizza should arrive in time of MAX_ORDER_DURATION
		{
			fastVehicles = fastVehicles.stream()
					.filter(v -> getOrdersFromVehicle(v).size() * SINGLE_DELIVERY_TIME.toMinutes() + ARBITRARY_WAITING_TIME_EFFICIENT.toMinutes() + SINGLE_DELIVERY_TIME.toMinutes() <= MAX_ORDER_DURATION.toMinutes())
					.collect(Collectors.toList());
		}
		//sort: ascending Capacity
		return sortVehicleCapAscending(fastVehicles);
	}

	/**
	 * A method to get all Orders assigned to a given {@link VehicleProduct}
	 * @param vehicle A vehicle to be checked
	 * @return A List of {@link ShopOrder} assigned to the specified vehicle
	 */

	public List<ShopOrder> getOrdersFromVehicle(@NonNull VehicleProduct vehicle){
		return shopOrderManagement.findAll().stream()
				.filter(o -> o.getDriver() != null &&
						o.getDriver().getVehicle() != null &&
						Objects.equals(o.getDriver().getVehicle().getId(), vehicle.getId()))
				.sorted(Comparator.comparing(ShopOrder::getTimeCreated))
				.collect(Collectors.toList());
	}

	/**
	 * A method for sorting all available {@link VehicleProduct} ascending by Vehicle Capacity
	 * @param allVehicles The List of all available vehicles to sort through
	 * @return The sorted List of Vehicles
	 * @see DeliveryManagement#sortVehicleCapAscending(List)
	 */

	//TODO: sort by free slots instead of total slots
	public List<VehicleProduct> sortVehicleCapAscending(List<VehicleProduct> allVehicles){

		return allVehicles.stream()
				.filter(v -> Objects.equals(v.getMeta(), "NORMAL") && shopCatalogManagement.findByCategory(
						ProductCategory.VEHICLE.toString()).toList().contains(v))
				.sorted(Comparator.comparing(VehicleProduct::getSlots))
				.collect(Collectors.toList());
	}

	/**
	 * A method for sorting all available {@link VehicleProduct} descending by Vehicle Capacity
	 * @param allVehicles The List of all available vehicles to sort through
	 * @return The sorted List of Vehicles
	 * @see DeliveryManagement#sortVehicleCapDescending(List)
	 */

	public List<VehicleProduct> sortVehicleCapDescending(List<VehicleProduct> allVehicles){

		return allVehicles.stream()
				.filter(v -> Objects.equals(v.getMeta(), "NORMAL") && shopCatalogManagement.findByCategory(
						ProductCategory.VEHICLE.toString()).toList().contains(v))
				.sorted(Comparator.comparing(VehicleProduct::getSlots).reversed())
				.collect(Collectors.toList());
	}

}
