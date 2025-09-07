package papapizza.order;

import lombok.NonNull;
import org.salespointframework.catalog.Product;
import org.salespointframework.order.*;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.time.BusinessTime;
import org.salespointframework.time.Interval;
import org.salespointframework.useraccount.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import papapizza.customer.Customer;
import papapizza.customer.CustomerManagement;
import papapizza.delivery.DeliveryManagement;
import papapizza.employee.Employee;
import papapizza.employee.EmployeeManagement;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.kitchen.KitchenManagement;

import javax.money.MonetaryAmount;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class ShopOrderManagement<T extends ShopOrder> { //FIXME no dynamic

	private final Logger logger = LoggerFactory.getLogger(ShopOrderManagement.class);

	private final BusinessTime businessTime;

	private final ShopOrderRepository shopOrderRepository;
	private final OrderManagement<ShopOrder> orderManagement;
	private ShopCatalogManagement catalogManagement;
	private EmployeeManagement employeeManagement;
	private KitchenManagement kitchenManagement;
	private DeliveryManagement deliveryManagement;
	private InvoiceHandler invoiceHandler;
	private CustomerManagement customerManagement;

	@Autowired
	public ShopOrderManagement(@NonNull final BusinessTime businessTime,
							   @NonNull final ShopOrderRepository shopOrderRepository,
							   @NonNull final OrderManagement<ShopOrder> orderManagement) {
		this.businessTime = businessTime;
		this.shopOrderRepository = shopOrderRepository;
		this.orderManagement = orderManagement;
	}

	@Autowired
	public void setCatalogManagement(@NonNull ShopCatalogManagement catalogManagement){
		this.catalogManagement = catalogManagement;
	}

	@Autowired
	public void setEmployeeManagement(@NonNull EmployeeManagement employeeManagement){
		this.employeeManagement = employeeManagement;
	}

	@Autowired
	public void setKitchenManagement(@NonNull KitchenManagement kitchenManagement){
		this.kitchenManagement = kitchenManagement;
	}

	@Autowired
	public void setDeliveryManagement(@NonNull DeliveryManagement deliveryManagement){
		this.deliveryManagement = deliveryManagement;
	}

	@Autowired
	public void setInvoiceHandler(@NonNull InvoiceHandler invoiceHandler){
		this.invoiceHandler = invoiceHandler;
	}

	@Autowired
	public void setCustomerManagement(@NonNull CustomerManagement customerManagement) {
		this.customerManagement = customerManagement;
	}

	//=============================
	//our relevant methods
	//=============================
	public ShopOrder create(Employee cashier, Customer customer) {
		ShopOrder order = new ShopOrder(employeeManagement.getDummyAccount(), customer);
		order.setCashier(cashier);
		return order;
	}

	//find by OrderState
	public Streamable<ShopOrder> findBy(@NonNull ShopOrderState shopOrderState) {
		return this.shopOrderRepository.findByShopOrderState(shopOrderState);
	}

	//stupid pass through

	public ShopOrder save(@NonNull ShopOrder order) {
		return orderManagement.save(order);
	}

	public Optional<ShopOrder> get(@NonNull OrderIdentifier orderIdentifier) {
		return orderManagement.get(orderIdentifier);
	}

	public boolean contains(@NonNull OrderIdentifier orderIdentifier) {
		return orderManagement.contains(orderIdentifier);
	}

	public Streamable<ShopOrder> findBy(@NonNull Interval interval) {
		return orderManagement.findBy(interval);
	}

	public Streamable<ShopOrder> findBy(@NonNull OrderStatus orderStatus) {
		return orderManagement.findBy(orderStatus);
	}

	public Streamable<ShopOrder> findBy(@NonNull UserAccount userAccount) {
		return orderManagement.findBy(userAccount);
	}

	public Streamable<ShopOrder> findBy(@NonNull UserAccount userAccount, @NonNull Interval interval) {
		return orderManagement.findBy(userAccount, interval);
	}

	public Optional<ShopOrder> findByShopOrderId(@NonNull String shopOrderId){
		return this.findAll(Pageable.ofSize(Integer.MAX_VALUE)).stream().filter(shopOrder -> Objects.requireNonNull(shopOrder.getId()).toString().equals(shopOrderId)).findAny();
	}

	public ShopOrder delete(@NonNull ShopOrder order) {
		return orderManagement.delete(order);
	}

	public Streamable<ShopOrder> findBy(@NonNull Customer customer){
		return shopOrderRepository.findByCustomer(customer);
	}

	public Stream<ShopOrder> findBy(@NonNull Employee employee) {
		List<ShopOrder> shopOrders = this.findAll();
		List<ShopOrder> returnShopOrders = new ArrayList<>();
		for (ShopOrder order : shopOrders) {
			if (order.getCashier() != null) if (order.getCashier().equals(employee)) returnShopOrders.add(order);
			if (order.getChefs() != null) if (order.getChefs().contains(employee)) returnShopOrders.add(order);
			if (order.getDriver() != null) if (order.getDriver().equals(employee)) returnShopOrders.add(order);
		}
		return returnShopOrders.stream();
	}

	public Stream<ShopOrder> findByParentId(@NonNull String parentId){
		return this.findAll().stream().filter(o -> (o.getParentId() != null) || (o.getParentId().equals(parentId)));
	}

	public Page<ShopOrder> findAll(@NonNull Pageable pageable) {
		return orderManagement.findAll(pageable);
	}

	public List<ShopOrder> findAll(){

		return this.findAll(Pageable.ofSize(Integer.MAX_VALUE)).stream().collect(Collectors.toList());
	}

	/*
	public ShopOrder cloneOrder(ShopOrder cloneFrom){
		ShopOrder toClone = new ShopOrder(employeeManagement.getDummyAccount(), cloneFrom.getCustomer());
		toClone.setCashier(cloneFrom.getCashier());
		toClone.setChefs(cloneFrom.getChefs());
		toClone.setDriver(cloneFrom.getDriver());
		shopOrderManagement.setShopOrderState(toClone, cloneFrom.getShopOrderState());
		toClone.setOpenDuration(cloneFrom.getOpenDuration());
		toClone.setPendingDuration(cloneFrom.getPendingDuration());
		toClone.setReadyDuration(cloneFrom.getReadyDuration());
		toClone.setInDeliverDuration(cloneFrom.getInDeliverDuration());
		toClone.setTotalDuration(cloneFrom.getTotalDuration());
		return toClone;
	}*/

	public void setShopOrderDeliveryType(@NonNull ShopOrder order, @NonNull DeliveryType deliveryType){
		if (this.get(order.getId()).isPresent()){
			this.get(order.getId()).get().setDeliveryType(deliveryType);
			this.save(order);
		} else {
			throw new ShopOrderDoesNotExistExeption();
		}
	}

	public void setProductAndQuantityAsLines(@NonNull OrderIdentifier orderIdentifier, @NonNull Product product, @NonNull Quantity quantity){
		if (this.get(orderIdentifier).isEmpty()) throw new NoSuchElementException("ShopOrder is not persistently stored");
		try {
			catalogManagement.findById(product.getId());
		} catch (NoSuchElementException e) {throw new NoSuchElementException("Product is not persistently stored");}

		ShopOrder shopOrder = this.get(orderIdentifier).get();

		if (quantity.getAmount().intValue() > 0){
			if (shopOrder.getOrderLines(product).stream().count() > 0) {
				removeLinesByProduct(orderIdentifier, product);
			}
			addLinesByProduct(orderIdentifier, product, quantity);
		} else {
			if (shopOrder.getOrderLines(product).stream().count() > 0){
				removeLinesByProduct(orderIdentifier, product);
			}
		}

	}

	public void removeLinesByProduct(@NonNull OrderIdentifier orderIdentifier, @NonNull Product product){
		if (this.get(orderIdentifier).isEmpty()) throw new NoSuchElementException("ShopOrder is not persistently stored");
		try {
			catalogManagement.findById(product.getId());
		} catch (NoSuchElementException e) {throw new NoSuchElementException("Product is not persistently stored");}

		ShopOrder shopOrder = this.get(orderIdentifier).get();

		if (shopOrder.getOrderLines(product).stream().count() > 1 ||
				shopOrder.getOrderLines(product).stream().count() == 0)
			throw new RuntimeException("zero or more than one Orderline with same Product found");

		OrderLine orderLine = shopOrder.getOrderLines(product).stream().findFirst().get();

		shopOrder.remove(shopOrder.getOrderLines(product).stream().findAny().get());

		this.save(shopOrder);
	}

	public OrderLine addLinesByProduct(@NonNull OrderIdentifier orderIdentifier, @NonNull Product product, @NonNull Quantity quantity){
		if (this.get(orderIdentifier).isEmpty()) throw new NoSuchElementException("ShopOrder is not persistently stored");
		try {
			catalogManagement.findById(product.getId());
		} catch (NoSuchElementException e) {throw new NoSuchElementException("Product is not persistently stored");}

		ShopOrder shopOrder = this.get(orderIdentifier).get();

		OrderLine orderLine = shopOrder.addOrderLine(product, quantity);
		this.save(shopOrder);
		return orderLine;
	}

	public ChargeLine addLinesByTotal(@NonNull OrderIdentifier orderIdentifier, @NonNull MonetaryAmount amount,@NonNull String description){
		if (this.get(orderIdentifier).isEmpty()) throw new NoSuchElementException("ShopOrder is not persistently stored");

		ShopOrder shopOrder = this.get(orderIdentifier).get();
		ChargeLine chargeLine = shopOrder.addChargeLine(amount, description);
		this.save(shopOrder);
		return chargeLine;
	}

	public ChargeLine removeLinesByDescription(@NonNull OrderIdentifier orderIdentifier, @NonNull String description){
		if (this.get(orderIdentifier).isEmpty()) throw new NoSuchElementException("ShopOrder is not persistently stored");

		ShopOrder shopOrder = this.get(orderIdentifier).get();
		if(shopOrder.getChargeLines().stream().anyMatch(c -> c.getDescription().equals(description))){
			ChargeLine chargeLine = shopOrder.getChargeLines().stream().filter(c -> c.getDescription().equals(description)).findAny().get();
			shopOrder.remove(chargeLine);
			this.save(shopOrder);
			return chargeLine;
		} else return null;
	}

	public Duration calcTimeEstimate(@NonNull ShopOrder order){
		logger.info("kitchen Time Estimate: " + kitchenManagement.kitchenShopOrderTimeEstimate(order).toSeconds());
		logger.info("delivery Time Estimate: " + deliveryManagement.deliveryShopOrderTimeEstimate(order).toSeconds());
		Duration duration = Duration.ofSeconds(kitchenManagement.kitchenShopOrderTimeEstimate(order).toSeconds() +
				deliveryManagement.deliveryShopOrderTimeEstimate(order).toSeconds());

		logger.info("time spent in kitchen: "+kitchenManagement.kitchenShopOrderTimeEstimate(order).toSeconds()); // 0 //debug
		order.setTimeEstimate(duration);
		order.setKitchenTimeEstimate(kitchenManagement.kitchenShopOrderTimeEstimate(order));
		order.setDeliveryTimeEstimate(deliveryManagement.deliveryShopOrderTimeEstimate(order));
		return duration;
	}

	public void setOrderReady(ShopOrder order){
		if(order.getDeliveryType() == DeliveryType.DELIVERY) {
			this.setShopOrderState(order, ShopOrderState.READYDELIVER);
		}else if(order.getDeliveryType() == DeliveryType.PICKUP){
			this.setShopOrderState(order, ShopOrderState.READYPICKUP);
		}
	}

	public void setShopOrderState(ShopOrder order, ShopOrderState state){
		order.setShopOrderState(state);
		switch (state){
			case COMPLETED:
				order.setTimeCompleted(LocalDateTime.now());
				if(order.getDeliveryType() != DeliveryType.RETURN_ORDER) {
					customerManagement.addNewDishsets(order); //add new lent dishsets to customer
				}
				if(!deliveryManagement.getNoVehicleAssignedOrders().isEmpty()){
					//TODO: check if there are available spaces to fit another order
					//TODO: unassign is not always triggered?! (maybe it is and, I might just be dumb)
					//->Whenever an order gets set to complete, its might be the last order of a given delivery and therefore free up a vehicle
					if(deliveryManagement.getOrdersFromVehicle(order.getDriver().getVehicle()).stream().noneMatch(o -> o.getShopOrderState() != ShopOrderState.COMPLETED)){//No order in vehicle is not completed -> vehicle is available for queued orders

					}
				}
				break;
			case CANCELLED:
				order.setTimeCompleted(LocalDateTime.now());
				break;
		}
		save(order); //changes should be saved, right? RIGHT?
	}

}
