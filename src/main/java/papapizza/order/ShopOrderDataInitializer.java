package papapizza.order;

import lombok.SneakyThrows;
import org.salespointframework.catalog.Product;
import org.salespointframework.core.DataInitializer;
import org.salespointframework.quantity.Quantity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import papapizza.customer.Customer;
import papapizza.customer.CustomerCreationForm;
import papapizza.customer.CustomerManagement;
import papapizza.delivery.DeliveryManagement;
import papapizza.employee.Employee;
import papapizza.employee.EmployeeCreationForm;
import papapizza.employee.EmployeeManagement;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.kitchen.KitchenManagement;

import java.time.*;
import java.util.List;

@Component
public class ShopOrderDataInitializer implements DataInitializer {

	private final Logger logger = LoggerFactory.getLogger(ShopOrderDataInitializer.class);

	@Autowired
	private ShopCatalogManagement shopCatalogManagement;
	@Autowired
	private CustomerManagement customerManagement;
	@Autowired
	private EmployeeManagement employeeManagement;
	@Autowired
	private ShopOrderManagement<ShopOrder> shopOrderManagement;
	@Autowired
    private DeliveryManagement deliveryManagement;
	@Autowired
	private KitchenManagement kitchenManagement;
	@Autowired
	private InvoiceHandler invoiceHandler;

	@SneakyThrows
	public void initialize() {

		//fetch for initialized data

		Product CustomPizza = shopCatalogManagement.createPizzaProduct(List.of(shopCatalogManagement.createToppingProduct("Ginger","5")));
		Customer customer1 = customerManagement.createCustomer(new CustomerCreationForm("Profaned Capital","2130498732","The Giant","Yorm"));
		Customer customer2 = customerManagement.createCustomer(new CustomerCreationForm("Lothric Castle","21304112232","Armour","Dragonslayer"));
		//roles are highly scuffed -> but already fixed? idk
		logger.info(employeeManagement.findAll().toList().toString());
		Employee cashier1 = employeeManagement.createEmployee(new EmployeeCreationForm("Valkyrie","Kairi","Imahara ","123","123","Cashier"));
		Employee chef1 = employeeManagement.createEmployee(new EmployeeCreationForm("Gon","Gon","Freecss","123","123","Chef"));
		Employee chef2 = employeeManagement.createEmployee(new EmployeeCreationForm("Yuno","Yuno","Grinberryall","123","123","Chef"));
		Employee driver1 = employeeManagement.createEmployee(new EmployeeCreationForm("Ken","Ken","Kaneki","123","123","Driver"));
		// Employee cook1 = employeeManagement.findAll().filter(employee -> employee.getRole().equals("Cook")).toList().get(0);

		//create orders


		ShopOrder order2 = shopOrderManagement.create(cashier1,customer2);
		//order2.setChefs(Collections.singletonList(chef2));
		//order2.setDriver(driver1);
		order2.setDeliveryType(DeliveryType.DELIVERY);
		order2.setPendingDuration(Duration.ofMinutes(10));
		order2.setReadyDuration(Duration.ofMinutes(10));
		order2.setInDeliverDuration(Duration.ofMinutes(10));
		order2.setTotalDuration(Duration.ofMinutes(40));
		order2.addOrderLine(CustomPizza, Quantity.of(1));
		shopOrderManagement.setShopOrderState(order2, ShopOrderState.OPEN);
		shopOrderManagement.save(order2);
		deliveryManagement.assignDriver(order2);
		kitchenManagement.assignOvens(order2);
		shopOrderManagement.calcTimeEstimate(order2);


		//ShopOrders for analytics-testing

		//order completed now
		ShopOrder completeOrder1 = shopOrderManagement.create(cashier1,customer1);
		//completeOrder1.setChefs(Collections.singletonList(chef1));
		//completeOrder1.setDriver(driver1);
		shopOrderManagement.setShopOrderState(completeOrder1, ShopOrderState.OPEN);
		completeOrder1.setDeliveryType(DeliveryType.DELIVERY);
		completeOrder1.setOpenDuration(Duration.ofMinutes(10));
		completeOrder1.setPendingDuration(Duration.ofMinutes(10));
		completeOrder1.setReadyDuration(Duration.ofMinutes(10));
		completeOrder1.setInDeliverDuration(Duration.ofMinutes(10));
		completeOrder1.setTotalDuration(Duration.ofMinutes(40));
		completeOrder1.addOrderLine(CustomPizza, Quantity.of(2));

		shopOrderManagement.save(completeOrder1);
		deliveryManagement.assignDriver(completeOrder1);
		shopOrderManagement.setShopOrderState(completeOrder1, ShopOrderState.INDELIVERY);
		shopOrderManagement.setShopOrderState(completeOrder1, ShopOrderState.COMPLETED);
		completeOrder1.setTimeCompleted(LocalDateTime.now());

		//order completed last month
		ShopOrder completeOrder2 = shopOrderManagement.create(cashier1,customer1);
		//completeOrder2.setChefs(Collections.singletonList(chef1));
		//completeOrder2.setDriver(driver1);
		shopOrderManagement.setShopOrderState(completeOrder2,ShopOrderState.OPEN);
		completeOrder2.setDeliveryType(DeliveryType.DELIVERY);
		completeOrder2.setOpenDuration(Duration.ofMinutes(10));
		completeOrder2.setPendingDuration(Duration.ofMinutes(10));
		completeOrder2.setReadyDuration(Duration.ofMinutes(10));
		completeOrder2.setInDeliverDuration(Duration.ofMinutes(10));
		completeOrder2.setTotalDuration(Duration.ofMinutes(40));
		completeOrder2.addOrderLine(CustomPizza, Quantity.of(2));

		shopOrderManagement.save(completeOrder2);
		deliveryManagement.assignDriver(completeOrder2);
		shopOrderManagement.setShopOrderState(completeOrder2, ShopOrderState.INDELIVERY);
		shopOrderManagement.setShopOrderState(completeOrder2, ShopOrderState.COMPLETED);
		completeOrder2.setTimeCompleted(LocalDateTime.of(LocalDate.of(2021, Month.NOVEMBER, 2), LocalTime.now()));

		//order cancelled
		ShopOrder canceledOrder1 = shopOrderManagement.create(cashier1,customer1);
		//canceledOrder1.setChefs(Collections.singletonList(chef1));
		//canceledOrder1.setDriver(driver1);
		shopOrderManagement.setShopOrderState(canceledOrder1, ShopOrderState.OPEN);
		canceledOrder1.setDeliveryType(DeliveryType.DELIVERY);
		canceledOrder1.setOpenDuration(Duration.ofMinutes(10));
		canceledOrder1.setPendingDuration(Duration.ofMinutes(10));
		canceledOrder1.setReadyDuration(Duration.ofMinutes(10));
		canceledOrder1.setInDeliverDuration(Duration.ofMinutes(10));
		canceledOrder1.setTotalDuration(Duration.ofMinutes(40));
		canceledOrder1.addOrderLine(CustomPizza, Quantity.of(2));

		shopOrderManagement.save(canceledOrder1);
		deliveryManagement.assignDriver(canceledOrder1);
		shopOrderManagement.setShopOrderState(canceledOrder1, ShopOrderState.CANCELLED);
		canceledOrder1.setTimeCompleted(LocalDateTime.now());

		ShopOrder kitchenOrder = shopOrderManagement.create(cashier1,customer1);
		//kitchenOrder.setChefs(Collections.singletonList(chef1));
		//kitchenOrder.setDriver(driver1);
		shopOrderManagement.setShopOrderState(kitchenOrder, ShopOrderState.OPEN);
		kitchenOrder.setDeliveryType(DeliveryType.DELIVERY);
		kitchenOrder.addOrderLine(CustomPizza, Quantity.of(5));
		shopOrderManagement.save(kitchenOrder);
		kitchenManagement.assignOvens(kitchenOrder);
		deliveryManagement.assignDriver(kitchenOrder);
		shopOrderManagement.calcTimeEstimate(kitchenOrder);
	}
}
