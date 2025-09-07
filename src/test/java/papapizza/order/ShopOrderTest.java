package papapizza.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import papapizza.customer.CustomerCreationForm;
import papapizza.customer.CustomerManagement;
import papapizza.employee.EmployeeManagement;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
public class ShopOrderTest {

	@Autowired CustomerManagement customerManagement;
	@Autowired EmployeeManagement employeeManagement;
	@Autowired ShopOrderManagement<ShopOrder> shopOrderManagement;


	@Test
	public void getterAndSetterInShopOrder(){

		CustomerCreationForm ccf = new CustomerCreationForm("add","123","last","first");
		ShopOrder shopOrder = new ShopOrder(employeeManagement.getDummyAccount(), customerManagement.createCustomer(ccf));
		shopOrder.setCustomer(customerManagement.getDeleteLinkCustomer());
		shopOrder.setCashier(employeeManagement.getDeleteLinkEmployee());
		shopOrder.setChefs(List.of(employeeManagement.getDeleteLinkEmployee()));
		shopOrder.setDriver(employeeManagement.getDeleteLinkEmployee());
		shopOrderManagement.setShopOrderState(shopOrder, ShopOrderState.PENDING);
		shopOrder.setDeliveryType(DeliveryType.DELIVERY);
		shopOrder.setTimeCompleted(LocalDateTime.now());
		shopOrder.setInOvenSince(LocalDateTime.now());
		shopOrder.setOpenDuration(Duration.ZERO);
		shopOrder.setPendingDuration(Duration.ZERO);
		shopOrder.setReadyDuration(Duration.ZERO);
		shopOrder.setInDeliverDuration(Duration.ZERO);
		shopOrder.setTotalDuration(Duration.ZERO);
		shopOrder.setParentId(shopOrder.getId().getIdentifier());

		shopOrder.getCustomer();
		shopOrder.getCashier();
		shopOrder.getChefs();
		shopOrder.getDriver();
		shopOrder.getShopOrderState();
		shopOrder.getDeliveryType();
		shopOrder.getTimeCompleted();
		shopOrder.getInOvenSince();
		shopOrder.getOpenDuration();
		shopOrder.getPendingDuration();
		shopOrder.getReadyDuration();
		shopOrder.getInDeliverDuration();
		shopOrder.getTotalDuration();
		shopOrder.getParentId();
	}

}
