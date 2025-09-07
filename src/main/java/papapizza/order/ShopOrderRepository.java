package papapizza.order;

import org.salespointframework.order.OrderIdentifier;
import org.salespointframework.order.OrderStatus;
import org.salespointframework.useraccount.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;
import papapizza.customer.Customer;

import java.time.LocalDateTime;

interface ShopOrderRepository extends Repository<ShopOrder, OrderIdentifier> {
	@Query("select o from #{#entityName} o")
	Streamable<ShopOrder> findAll();

	Page<ShopOrder> findAll(Pageable pageable);

	Streamable<ShopOrder> findByDateCreatedBetween(LocalDateTime from, LocalDateTime to);

	Streamable<ShopOrder> findByOrderStatus(OrderStatus orderStatus);

	Streamable<ShopOrder> findByUserAccount(UserAccount userAccount);

	Streamable<ShopOrder> findByUserAccountAndDateCreatedBetween(UserAccount userAccount, LocalDateTime from, LocalDateTime to);

	Streamable<ShopOrder> findByShopOrderState(ShopOrderState shopOrderState);

	Streamable<ShopOrder> findByCustomer(Customer customer);
}