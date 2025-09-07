package papapizza.order;

import org.salespointframework.order.OrderLine;
import papapizza.employee.Employee;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ShopOrderDisplayer {

	public static List<DisplayableShopOrder> display(Stream<ShopOrder> shopOrderStream){



		List<DisplayableShopOrder> displayableShopOrders = new ArrayList<>();

		for (ShopOrder shopOrder : shopOrderStream.filter(o -> o.getDeliveryType() != DeliveryType.RETURN_ORDER).collect(Collectors.toList())){

			//german/english not respected
			DisplayableShopOrder displayableShopOrder = new DisplayableShopOrder();
			displayableShopOrder.setOrderId(shopOrder.getId().getIdentifier());
			displayableShopOrder.setOrderState(shopOrder.getShopOrderState().toString());
			displayableShopOrder.setDeliveryType(shopOrder.getDeliveryType().toString());
			List<String> orderLines = new ArrayList<>();
			for (OrderLine orderLine: shopOrder.getOrderLines()){
				orderLines.add(orderLine.getProductName());
			}
			//total is wrong because orderline also calculates price
			displayableShopOrder.setTotal(shopOrder.getTotal().toString());

			displayableShopOrder.setLastname(shopOrder.getCustomer().getLastname());
			displayableShopOrder.setFirstname(shopOrder.getCustomer().getFirstname());
			displayableShopOrder.setPhone(shopOrder.getCustomer().getPhone());
			displayableShopOrder.setAddress(shopOrder.getCustomer().getAddress());

			displayableShopOrder.setInvoiceFilename(shopOrder.getInvoiceFilename());

			if(shopOrder.getCashier().getUserAccount() != null)
				displayableShopOrder.setCashierName(shopOrder.getCashier().getUserAccount().getUsername());
			List<String> chefNames = new ArrayList<>();
			if(shopOrder.getChefs() != null)
				for (Employee chef : shopOrder.getChefs()){
					chefNames.add(chef.getUserAccount().getUsername());
				}
			displayableShopOrder.setChefNames(chefNames);
			List<String> driverNames = new ArrayList<>();
			if(shopOrder.getDriver() != null) driverNames.add(shopOrder.getDriver().getUsername());

			displayableShopOrders.add(displayableShopOrder);

		}

		return displayableShopOrders;
	}

	public static DisplayableShopOrder display(ShopOrder shopOrder){

		//german/english not respected
		DisplayableShopOrder displayableShopOrder = new DisplayableShopOrder();
		displayableShopOrder.setOrderId(shopOrder.getId().getIdentifier());
		displayableShopOrder.setOrderState(shopOrder.getShopOrderState().toString());
		displayableShopOrder.setDeliveryType(shopOrder.getDeliveryType().toString());
		List<String> orderLines = new ArrayList<>();
		for (OrderLine orderLine: shopOrder.getOrderLines()){
			orderLines.add(orderLine.getProductName());
		}
		displayableShopOrder.setTotal(shopOrder.getTotal().toString());

		displayableShopOrder.setLastname(shopOrder.getCustomer().getLastname());
		displayableShopOrder.setFirstname(shopOrder.getCustomer().getFirstname());
		displayableShopOrder.setPhone(shopOrder.getCustomer().getPhone());
		displayableShopOrder.setAddress(shopOrder.getCustomer().getAddress());

		displayableShopOrder.setInvoiceFilename(shopOrder.getInvoiceFilename());

		if(shopOrder.getCashier().getUserAccount() != null)
			displayableShopOrder.setCashierName(shopOrder.getCashier().getUserAccount().getUsername());
		List<String> chefNames = new ArrayList<>();
		if(shopOrder.getChefs() != null)
			for (Employee chef : shopOrder.getChefs()){
				chefNames.add(chef.getUserAccount().getUsername());
			}
		displayableShopOrder.setChefNames(chefNames);
		List<String> driverNames = new ArrayList<>();
		if(shopOrder.getDriver() != null)driverNames.add(shopOrder.getDriver().getUsername());

		displayableShopOrder.setDriverNames(driverNames);

		return displayableShopOrder;
	}
}
