package papapizza.delivery;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.time.BusinessTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import papapizza.employee.Employee;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.order.ShopOrder;
import papapizza.order.ShopOrderManagement;
import papapizza.order.ShopOrderState;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Transactional
@SpringBootTest
class DeliveryManagementTest {

	@Autowired
	private ShopOrderManagement<ShopOrder> shopOrderManagement;
	@Autowired
	private DeliveryManagement deliveryManagement;
	@Autowired
	private ShopCatalogManagement shopCatalogManagement;
	private Employee exceptionEmployee;
	private ShopOrder rejoinShopOrder;
	private List<ShopOrder> rejoinShopOrderList;

	@Autowired
	private BusinessTime businessTime;

	@Test
	public void cloneOrderTest(){
		assertTrue(shopOrderManagement.findAll().stream().findAny().isPresent());
		ShopOrder cloneFrom = shopOrderManagement.findAll().stream().findAny().get();
		ShopOrder cloneInto = deliveryManagement.cloneOrder(cloneFrom);
		assertEquals(cloneFrom.getShopOrderState(), cloneInto.getShopOrderState());
		assertEquals(cloneFrom.getChefs().toString(), cloneInto.getChefs().toString());
		assertEquals(cloneFrom.getOpenDuration(), cloneInto.getOpenDuration());
		assertEquals(cloneFrom.getPendingDuration(), cloneInto.getPendingDuration());
		assertEquals(cloneFrom.getReadyDuration(), cloneInto.getReadyDuration());
		assertEquals(cloneFrom.getInDeliverDuration(), cloneInto.getInDeliverDuration());
		assertEquals(cloneFrom.getTotalDuration(), cloneInto.getTotalDuration());
	}

	@Test
	@Disabled
	public void splitOrderTest(){
		assertTrue(shopOrderManagement.findAll().stream().findAny().isPresent());
		ShopOrder testOrder = shopOrderManagement.findAll().stream().findAny().get();
		deliveryManagement.splitOrder(testOrder, 3);
		testOrder.addOrderLine(shopCatalogManagement.findByName("Hawaii"), Quantity.of(30));
		List<ShopOrder> splitOrderList = deliveryManagement.splitOrder(testOrder, 3);
		this.rejoinShopOrder = splitOrderList.get(0);
		this.rejoinShopOrderList = splitOrderList;
	}

	@Test
	@Disabled
	public void deleteParentsTest(){
		assertTrue(shopOrderManagement.findAll().stream().findAny().isPresent());
		ShopOrder testOrder = shopOrderManagement.findAll().stream().findAny().get();
		deliveryManagement.splitOrder(testOrder, 3);
		testOrder.addOrderLine(shopCatalogManagement.findByName("Hawaii"), Quantity.of(30));
		List<ShopOrder> splitOrderList = deliveryManagement.splitOrder(testOrder, 3);
		this.rejoinShopOrder = splitOrderList.get(0);
		this.rejoinShopOrderList = splitOrderList;
		deliveryManagement.deleteParents();
	}

	@Test
	@Disabled
	public void rejoinOrderTest(){
		splitOrderTest();
		shopOrderManagement.setShopOrderState(this.rejoinShopOrderList.get(1), ShopOrderState.COMPLETED);
		shopOrderManagement.setShopOrderState(this.rejoinShopOrderList.get(2), ShopOrderState.COMPLETED);
		System.out.println("HIER: " + this.rejoinShopOrderList.get(1).getParentId());
		ShopOrder shopOrder = deliveryManagement.rejoinOrder(this.rejoinShopOrder);
	}

	@Test
	public void calcTimeLeftTest(){
		assertTrue(shopOrderManagement.findAll().stream().findAny().isPresent());
		ShopOrder shopOrder = shopOrderManagement.findAll().stream().findAny().get();
		long timeLeft = deliveryManagement.calcTimeLeft(shopOrder, businessTime);
		assertTrue(timeLeft <= 45 && timeLeft >= 0);
	}

	@Test
	@Disabled
	public void assignVehicleTest(){
		this.exceptionEmployee = new Employee();
		this.exceptionEmployee.setVehicle(null);
		assertTrue(shopOrderManagement.findAll().stream().findAny().isPresent());
		ShopOrder testOrder = shopOrderManagement.findAll().stream().findAny().get();
		testOrder.addOrderLine(shopCatalogManagement.findByName("Hawaii"), Quantity.of(15));
		deliveryManagement.assignDriver(testOrder);
		deliveryManagement.assignVehicle(this.exceptionEmployee);

	}

	@Test
	@Disabled
	public void assignDriverTest(){
		assertTrue(shopOrderManagement.findAll().stream().findAny().isPresent());
		ShopOrder testOrder = shopOrderManagement.findAll().stream().findAny().get();
		testOrder.addOrderLine(shopCatalogManagement.findByName("Hawaii"), Quantity.of(15));
		deliveryManagement.assignDriver(testOrder);
	}

	@Test
	@Disabled
	public void unassignDriverTest(){
		assertTrue(shopOrderManagement.findAll().stream().findAny().isPresent());
		ShopOrder testOrder = shopOrderManagement.findAll().stream().findAny().get();
		testOrder.addOrderLine(shopCatalogManagement.findByName("Hawaii"), Quantity.of(15));
		deliveryManagement.assignDriver(testOrder);
		deliveryManagement.unassignDriver(testOrder);
	}

}