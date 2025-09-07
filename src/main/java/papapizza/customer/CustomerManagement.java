package papapizza.customer;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.salespointframework.catalog.Product;
import org.salespointframework.order.OrderLine;
import org.salespointframework.quantity.Quantity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import papapizza.app.exc.PapaPizzaRunException;
import papapizza.employee.Employee;
import papapizza.inventory.ProductCategory;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.inventory.items.DishsetProduct;
import papapizza.order.*;

import java.io.IOException;
import java.util.*;


@Service
@Transactional
public class CustomerManagement {
	@Getter @Setter
	private boolean initialized = false;

	private final Logger logger = LoggerFactory.getLogger(CustomerManagement.class);

	private final CustomerRepository customerRepo;
	private final TanRepo tanRepo;
	private final LentDishsetRepo setRepo;
	private ShopOrderManagement<ShopOrder> shopOrderManagement;
	private ShopCatalogManagement shopCatalogManagement;
	private InvoiceHandler invoiceHandler;

	private final Customer deleteLinkCustomer;

	@Autowired
	public CustomerManagement(@NonNull final CustomerRepository customerRepo,
							  @NonNull final TanRepo tanRepo,
							  @NonNull final LentDishsetRepo setRepo) {
		this.customerRepo = customerRepo;
		this.tanRepo = tanRepo;
		this.setRepo = setRepo;

		//create a meta customer that gets all deleted orders assigned later on
		deleteLinkCustomer = createDeleteLinkCustomer();
	}

	@Autowired
	public void setShopOrderManagement(@NonNull ShopOrderManagement<ShopOrder> shopOrderManagement) {
		this.shopOrderManagement = shopOrderManagement;
	}

	@Autowired
	public void setShopCatalogManagement(@NonNull ShopCatalogManagement shopCatalogManagement) {
		this.shopCatalogManagement = shopCatalogManagement;
	}

	@Autowired
	public void setInvoiceHandler(@NonNull InvoiceHandler invoiceHandler){
		this.invoiceHandler = invoiceHandler;
	}

	private Customer createDeleteLinkCustomer(){
		//meta customer does alr exist, i.e. loaded from file
		if(customerRepo.findByMeta(Customer.Meta.DELETE_LINK_CUSTOMER).size() != 0){
			return customerRepo.findByMeta(Customer.Meta.DELETE_LINK_CUSTOMER).get(0);
		}
		Customer deleteLinkCs = new Customer();
		deleteLinkCs.setMeta(Customer.Meta.DELETE_LINK_CUSTOMER);
		return this.customerRepo.save(deleteLinkCs);
	}

	/**
	 * Gives the Meta.DELETE_LINK_CUSTOMER <br>
	 * first of the list, if there are for any reason multiple in the db
	 * @return Customer
	 */
	public Customer getDeleteLinkCustomer(){
		return deleteLinkCustomer;
	}

	/**
	 * Creates a new customer based on the data of a form &amp; assigns customer a new TAN
	 * @param form with customer details
	 * @return the saved customer on success
	 * @throws CustomerAlrExistsException if phone no already exists
	 */
	public Customer createCustomer(@NonNull CustomerCreationForm form) throws CustomerAlrExistsException{
		//create & setup customer
		Customer customer = new Customer();
		form.setCustomerDetails(customer);
		Tan customerTan = new Tan(tanRepo.findAll().toList());
		customer.setCurrentTan(customerTan);

		return this.save(customer);
	}

	/**
	 * Assigns customer a new tan
	 * Also deletes the old tan
	 * @param cstmr Customer
	 * @return old Tan
	 */
	public Tan renewCustomerTan(Customer cstmr){
		//Tan shuffle
		Tan wayOldTan = cstmr.getOldTan();
		Tan oldTan = cstmr.getCurrentTan();
		Tan newTan = new Tan(tanRepo.findAll().toList());
		cstmr.setCurrentTan(newTan);
		cstmr.setOldTan(oldTan);
		this.save(cstmr);
		if(wayOldTan != null){
			tanRepo.delete(wayOldTan); //remove the former old tan form database, as it is no longer needed
		}

		return wayOldTan;
	}

	public Tan revertOldTan(Customer cstmr){
		if(cstmr.getOldTan()==null){ //this case should normally not occur, only when wrongly assigned via DataInitializers
			renewCustomerTan(cstmr);
			return null;
		}

		Tan currentTan = cstmr.getCurrentTan();
		cstmr.setCurrentTan(cstmr.getOldTan());
		cstmr.setOldTan(null);
		this.save(cstmr);
		if(currentTan != null) {
			tanRepo.delete(currentTan);
		}
		return currentTan;
	}

	/**
	 * Add a new lent dishset object to a customer
	 * @param cstmr customer
	 * @param dishset dishset to lent
	 */
	public void addDishsetToCustomer(Customer cstmr, LentDishset dishset){
		cstmr.getLentDishsets().add(dishset);
		save(cstmr);
	}

	/**
	 * Adds new LentDishsets to a customer based on shopOrder
	 * @param newShopOrder order with possibly dishsets
	 */
	public void addNewDishsets(@NonNull ShopOrder newShopOrder){
		//add dishsets to customer's list
		newShopOrder.getOrderLines().forEach(orderLine -> {
			Product product = shopCatalogManagement.findById(orderLine.getProductIdentifier());
			if(product == null) {
				return;
			}

			//if the orderLine contains dishsets
			if(product.getCategories().toList().contains(ProductCategory.DISHSET.toString())){
				//add as many lentDishsets to customer as orderLine amount
				DishsetProduct dishsetType = (DishsetProduct) product;
				for (int i=0; i<orderLine.getQuantity().getAmount().intValue(); i++){
					addDishsetToCustomer(newShopOrder.getCustomer(), new LentDishset(newShopOrder.getTimeCreated(), dishsetType));
				}
			}
		});
	}

	/**
	 * Removes the given dishsets from the customer if they belong to them <br>
	 * and the LentDishset object is not older than specified by its validUntilEpoch attribute <br>
	 * Accepts a map of dishset ids, which should be initialized with false for the time being
	 * @param returnDishsetMap a map with ids of LentDishsets initialized with false
	 * @param cstmr the acting customer
	 * @param empl the acting employee
	 * @return a ShopOrder object with the returned Dishsets as orderLines
	 */
	public ShopOrder returnDishsetOfCustomer(@NonNull Map<Long, Boolean> returnDishsetMap,
											 @NonNull Customer cstmr,
											 @NonNull Employee empl){
		//create a return order where all successfully returned dishsets will be appended
		ShopOrder returnOrder = shopOrderManagement.create(empl, cstmr);
		returnOrder.setDeliveryType(DeliveryType.RETURN_ORDER); //handled as return order

		//try to remove each dishset from the customer and add it to the return order
		for(long setId : returnDishsetMap.keySet()){
			Optional<LentDishset> optSet = setRepo.findById(setId);
			//lent dishset might not exist
			if(optSet.isPresent()){
				LentDishset ds = optSet.get();
				//check if dishset still returnable based on time limitation
				//&& try to remove dishset from customer
				if(ds.getRemainingTimeSec() > 0 && removeDishsetOfCustomer(cstmr, ds)){
						addDishsetToOrder(returnOrder, ds);
					returnDishsetMap.put(setId, true); //mark in map as removed -> true
				}
			}
		}

		//if either dishset map was empty or non of the given dishsets belong to the given customer
		//->returnOrder will have no orderLines and will be deleted
		if(returnOrder.getOrderLines().isEmpty()){
			shopOrderManagement.delete(returnOrder);
			return null;
		}

		//immediately mark order as completed
		shopOrderManagement.setShopOrderState(returnOrder, ShopOrderState.COMPLETED);

		try {
			String filename = invoiceHandler.createInvoice(returnOrder);
			returnOrder.setInvoiceFilename(filename);
			shopOrderManagement.save(returnOrder);
		} catch (IOException e) {
			logger.error("Could not create pdf file for return order");
		}

		shopOrderManagement.save(returnOrder);
		return returnOrder;
	}

	private void addDishsetToOrder(ShopOrder returnOrder, LentDishset ds){
		List<OrderLine> dishOrderTypeLines = returnOrder.getOrderLines(ds.getDishsetType()).toList();
		//order line does already exist -> increase quantity
		if(dishOrderTypeLines.size() > 0){
			OrderLine line = dishOrderTypeLines.get(0);
			logger.info("Dishset type already in order line, adding to existing quantity");
			logger.info("b4 quantity:"+line.getQuantity());
			int count = line.getQuantity().getAmount().intValue();
			returnOrder.remove(line);
			returnOrder.addOrderLine(ds.getDishsetType(), Quantity.of(count+1L));
			logger.info("after quantity:"+line.getQuantity());
		}else{ //otherwise just add a new order line
			logger.info("Dishset type not yet in order line, adding new orderline");
			returnOrder.addOrderLine(ds.getDishsetType(), Quantity.of(1));
		}
	}

	private boolean removeDishsetOfCustomer(@NonNull Customer cstmr, @NonNull LentDishset dishset){
		if(!cstmr.getLentDishsets().contains(dishset)){
			return false;
		}
		cstmr.getLentDishsets().remove(dishset);
		save(cstmr);
		return true;
	}

	/**
	 * This finds all customers in the db except Meta.DELETE_LINK_CUSTOMER
	 * @return list of all non-meta customers
	 */
	public List<Customer> findAll() {
		return removeDeleteCsFromList(customerRepo.findAll());
	}

	/**
	 * This finds all customers except Meta.DELETE_LINK_CUSTOMER and sorts them according to the {@link Sort}
	 * @param s sort object
	 * @return sorted list
	 */
	public List<Customer> findAll(Sort s){
		return removeDeleteCsFromList(customerRepo.findAll(s));
	}

	/**
	 * Removes the Meta.DELETE_LINK_CUSTOMER from a given list
	 * @return list without meta customer
	 */
	private List<Customer> removeDeleteCsFromList(List<Customer> customerList){
		List<Customer> shallowCustomers = new ArrayList<>(customerList);
		shallowCustomers.removeIf(customer -> customer.getMeta() == Customer.Meta.DELETE_LINK_CUSTOMER);
		return shallowCustomers;
	}

	public Optional<Customer> findById(long id){
		return customerRepo.findById(id);
	}

	/**
	 * Returns a list of customers by their Meta tag
	 * @param meta Customer.Meta tag
	 * @return list of customers with matching tag
	 */
	public List<Customer> findByMeta(Customer.Meta meta){
		return customerRepo.findByMeta(meta);
	}

	/**
	 * Lets you find a customer by tan and phone no
	 * if they are valid
	 * @param phone of customer
	 * @param tan of customer
	 * @return non empty optional if customer is verified
	 */
	public Optional<Customer> findByPhoneAndTan(String phone, int tan){
		if (this.verifyCustomerByTan(phone, tan)){
			return Optional.of(customerRepo.findByPhone(phone).get(0));
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Tries to delete a customer by their ID <br>
	 * throws RuntimeException on meta customer &amp; non existent
	 * @param id to delete
	 */
	public void deleteById(long id){
		Optional<Customer> customer = this.findById(id);

		if(customer.isEmpty()) {
			throw new PapaPizzaRunException("Customer does not exist for id " + id);
		}

		if(customer.get().getMeta() == Customer.Meta.DELETE_LINK_CUSTOMER){
			throw new PapaPizzaRunException("Meta customer is immutable");
		}
		customerRepo.delete(customer.get()); //delete customer
	}

	/**
	 * Checks if a customer with the given id exists
	 * @param id to check
	 * @return true if exists in db
	 */
	public boolean existsById(long id){
		return customerRepo.existsById(id);
	}

	/**
	 * Saves the given customers <br>
	 * Checks that phone no does not already exists <br>
	 * and if customer is Meta customer (Customer.Meta.DELETE_LINK_CUSTOMER)
	 * @param customer to save
	 * @return the saved customer
	 * @throws CustomerAlrExistsException phone no is unique db column
	 */
	public Customer save(Customer customer) throws CustomerAlrExistsException{
		if(customer.getMeta() == Customer.Meta.DELETE_LINK_CUSTOMER){
			throw new PapaPizzaRunException("Meta customer is immutable");
		}

		List<Customer> customers = customerRepo.findByPhone(customer.getPhone());

		//wont save if another user alr has this phone number
		if(csListContainsOtherId(customers, customer)){
			logger.debug(Arrays.toString(customers.toArray()));
			logger.debug("found duplicate phone number");
			throw new CustomerAlrExistsException();
		}

		return customerRepo.save(customer);
	}

	private boolean csListContainsOtherId(List<Customer> customers, Customer customer){
		for(Customer c : customers){
			if(c.getId() != customer.getId()){
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if phone &amp; tan match with database entry, <br>
	 * false otherwise (also false if phone no. not in customers table)
	 * @param phone customer's phone no.
	 * @param tan customer's tan
	 * @return boolean
	 */
	public boolean verifyCustomerByTan(String phone, int tan){
		List<Customer> customerRes = customerRepo.findByPhone(phone);

		if(customerRes.size() == 0){
			return false;
		}else if(customerRes.size() > 1){
			throw new PapaPizzaRunException("multiple customers with the same phone number must not exist");
		}

		logger.info("found customer: "+customerRes.get(0).toString());

		//tans match or not
		return customerRes.get(0).getCurrentTan().equalsInt(tan);
	}

	public List<Customer> findByPhone(String phone) {
		return customerRepo.findByPhone(phone);
	}
}
