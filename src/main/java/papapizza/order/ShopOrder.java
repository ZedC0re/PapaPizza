package papapizza.order;

import lombok.Getter;
import lombok.Setter;
import org.salespointframework.order.Order;
import org.salespointframework.useraccount.UserAccount;
import papapizza.customer.Customer;
import papapizza.delivery.DeliveryManagement;
import papapizza.employee.Employee;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
public class ShopOrder extends Order{
	@OneToOne
	private Customer customer;

	@OneToOne
	private Employee cashier;

	@ManyToMany
	private List<Employee> chefs = new ArrayList<>();

	@OneToOne
	private Employee driver;

	@Enumerated(EnumType.STRING)
	private ShopOrderState shopOrderState = ShopOrderState.OPEN;

	@Enumerated(EnumType.STRING)
	private DeliveryType deliveryType = DeliveryType.DELIVERY;

	@Column
	private LocalDateTime timeCompleted, inOvenSince, timeCreated; //timeCreated /-Completed needed for analytics FIXME inOvenSince should be removed, right?

	@Column
	private Duration openDuration, pendingDuration, readyDuration, inDeliverDuration, totalDuration;

	@Column
	private Duration kitchenTimeEstimate, deliveryTimeEstimate, timeEstimate;

	@Column
	private String parentId;

	@Column
	private String invoiceFilename; //filename of the pdf file, NO! absolute path

	private String deliveryStrategy;

	public ShopOrder(){}

	public ShopOrder(@NotNull UserAccount dummy, @NotNull Customer customer) {
		super(dummy); //pass dummy userAccount
		this.customer = customer;
		timeCreated = LocalDateTime.now();
		this.deliveryStrategy = DeliveryManagement.getOrderAssign().toString();
	}

}
