package papapizza.customer;

import lombok.NonNull;
import org.salespointframework.core.DataInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.inventory.items.DishsetProduct;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class CustomerDataInitializer implements DataInitializer {
	private final Logger logger = LoggerFactory.getLogger(CustomerManagement.class);

	private final CustomerManagement cstmrMgmt;

	private final ShopCatalogManagement shopCatalogManagement;



	@Autowired
	public CustomerDataInitializer(@NonNull final CustomerManagement cstmrMgmt,
								   @NonNull final ShopCatalogManagement shopCatalogManagement) {
		this.cstmrMgmt = cstmrMgmt;
		this.shopCatalogManagement = shopCatalogManagement;

	}

	@Override
	public void initialize() {
		if(!cstmrMgmt.findAll().isEmpty()){
			logger.info("Customers alr in db");
			return;
		}
		logger.info("No customers in db yet, initializing ...");

		List.of(
			new CustomerCreationForm("Buxtehude","0800 1232340","Smits", "Peter"),
			new CustomerCreationForm("Veeze","012345","Stachelhaus", "Christian"),
			new CustomerCreationForm("CAA","012456","Baa", "Abb"),
			new CustomerCreationForm("am Ende des Ganges 1","91 012678","Baba", "Ali"),
			new CustomerCreationForm("Altschauerberg 8","0800 1110333", "Winkler", "Rainer"),
			new CustomerCreationForm("Rungestraße 22-24, 10179 Berlin","246280", "Schreiner", "Jumbo"),
				new CustomerCreationForm("Domianweg 5","0221 56789111.", "Domian", "Jürgen"),
				new CustomerCreationForm("Lymington, England","0800 53741", "Delphine", "Belle")
		).forEach(cstmrMgmt::createCustomer);

		//customer with dishset
		CustomerCreationForm form = new CustomerCreationForm("some address","222222", "dishset", "guy");
		Customer customer = cstmrMgmt.createCustomer(form);

		DishsetProduct dishset = shopCatalogManagement.createDishsetProduct("dishet1","15");
		LentDishset lentDishset = new LentDishset(LocalDateTime.now(),dishset);
		cstmrMgmt.addDishsetToCustomer(customer, lentDishset);
		LentDishset lentDishset2 = new LentDishset(LocalDateTime.now(),dishset);
		cstmrMgmt.addDishsetToCustomer(customer, lentDishset2);
		LentDishset lentDishset3 = new LentDishset(LocalDateTime.now(),dishset);
		cstmrMgmt.addDishsetToCustomer(customer, lentDishset3);
		LentDishset lentDishset4 = new LentDishset(LocalDateTime.parse("2021-11-01 13:00",
				DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),dishset);
		cstmrMgmt.addDishsetToCustomer(customer, lentDishset4);
		//cstmrMgmt.removeDishsetOfCustomer(customer, lentDishset2);
		customer.setAddress("changed");
		cstmrMgmt.save(customer);
		logger.info("LentDishset product ID:"+lentDishset.getDishsetType().getId());
		logger.info("dishsets:"+customer.getLentDishsets().toString());

		logger.info("Customers count:"+cstmrMgmt.findAll().size());

		cstmrMgmt.setInitialized(true);
	}
}

