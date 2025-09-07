package papapizza.delivery;

import lombok.Getter;
import lombok.Setter;
import papapizza.order.ShopOrderState;

import java.util.List;

/**
 * Class for saving all data that needs to be displayed in the frontend, used by {@link DeliveryController}
 */
@Setter
@Getter
public class DisplayableDelivery {

	/**
	 * The auto generated, unique ID of a ShopOrder
	 */
	private String deliveryId;
	/**
	 * The name of the {@link papapizza.customer.Customer}, contained within a {@link papapizza.order.ShopOrder}<br>
	 * Consists of a first and last name
	 */
	private String customerName;
	/**
	 * The telephone number of the {@link papapizza.customer.Customer}, contained within a {@link papapizza.order.ShopOrder}
	 */
	private String customerTelephoneNumber;
	/**
	 * The address of the {@link papapizza.customer.Customer}, contained within a {@link papapizza.order.ShopOrder}
	 */
	private String customerAddress;
	/**
	 * The price of a {@link papapizza.order.ShopOrder}
	 */
	private String deliveryPrice;
	/**
	 * The amount of time left, in minutes, until a {@link papapizza.order.ShopOrder} should be delivered
	 */
	private long timeLeft;
	/**
	 * The List of every {@link org.salespointframework.order.OrderLine} contained within a {@link papapizza.order.ShopOrder}
	 */
	private List<String> orderLines;
	/**
	 * A boolean indicating whether a given {@link papapizza.order.ShopOrder} should be displayed <br>
	 * Always true when the logged-in user is the boss
	 */
	private boolean viewable = false;

	/**
	 * A String refering to the {@link ShopOrderState} of a given {@link papapizza.order.ShopOrder} in delivery
	 */
	private String deliveryState;

}
