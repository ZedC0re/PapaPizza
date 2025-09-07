package papapizza.customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.salespointframework.quantity.Quantity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import papapizza.app.exc.PapaPizzaRunException;
import papapizza.employee.Employee;
import papapizza.employee.EmployeeManagement;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.inventory.items.DishsetProduct;
import papapizza.order.InvoiceHandler;
import papapizza.order.ShopOrder;

import javax.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CustomerManagementTest {
	private final Logger logger = LoggerFactory.getLogger(CustomerManagementTest.class);

	private final CustomerManagement cstmrMgmt;
	private final TanRepo tanRepo;

	@Autowired
	ShopCatalogManagement shopCatalogManagement;

	@Autowired
	EmployeeManagement employeeManagement;

	@Autowired
	InvoiceHandler invoiceHandler;

	@Autowired
	LentDishsetRepo lentDishsetRepo;

	private Customer testCustomer;

	@Autowired
	public CustomerManagementTest(CustomerManagement cstmrMgmt, TanRepo tanRepo) {
		this.cstmrMgmt = cstmrMgmt;
		this.tanRepo = tanRepo;
	}

	@BeforeEach
	void setUp() {
		CustomerCreationForm form = new CustomerCreationForm("Kuhliefumdenteich 1337","0133742069", "Zuvall", "Rainer");
		//test customer does still exist
		if(cstmrMgmt.findByPhone(form.getPhone()).size()==0) {
			logger.info("Creating test customer");
			testCustomer = cstmrMgmt.createCustomer(form);
		}else{
			logger.info("Test Customer alr exists");
			testCustomer = cstmrMgmt.findByPhone(form.getPhone()).get(0);
		}
	}

	@Test
	void verifyCustomerByTanTest() {
		assertTrue(cstmrMgmt.verifyCustomerByTan(testCustomer.getPhone(), testCustomer.getTanNumber()));
	}

	@Test
	void findByPhoneAndTanTest(){
		//correct tan and phone
		assertTrue(cstmrMgmt.findByPhoneAndTan(testCustomer.getPhone(), testCustomer.getTanNumber()).isPresent());
		//wrong tan and phone
		assertTrue(cstmrMgmt.findByPhoneAndTan(testCustomer.getPhone(), testCustomer.getTanNumber()-1).isEmpty());
	}

	@Test
	void renewCustomerTanTest(){
		Tan oldTan = testCustomer.getCurrentTan();
		cstmrMgmt.renewCustomerTan(testCustomer);

		assertNotEquals(oldTan, testCustomer.getCurrentTan());
		assertNotEquals(oldTan.getTanNumber(), testCustomer.getCurrentTan().getTanNumber());
		List<Tan> allTans = tanRepo.findAll().toList();
		//assertFalse(allTans.contains(oldTan));
	}

	@Test
	void revertOldTanTest(){
		//normal revert tan case i.e. on order cancellation
		cstmrMgmt.renewCustomerTan(testCustomer);
		Tan revertTo = testCustomer.getCurrentTan();
		cstmrMgmt.renewCustomerTan(testCustomer);
		cstmrMgmt.revertOldTan(testCustomer);
		assertNull(testCustomer.getOldTan());
		assertEquals(revertTo, testCustomer.getCurrentTan());

		//normally not occurring edge case, where oldtan is null
		assertNull(cstmrMgmt.revertOldTan(testCustomer));
	}

	@Test
	void deleteByIdTest(){
		//delete existing customer
		logger.info(testCustomer.getId()+"");
		logger.info(cstmrMgmt.findAll().size()+"");
		long csId = testCustomer.getId();
		cstmrMgmt.deleteById(csId);
		assertTrue(cstmrMgmt.findById(csId).isEmpty());

		//try delete non existing
		assertThrows(PapaPizzaRunException.class, () -> cstmrMgmt.deleteById(csId));

		//try delete meta customer
		assertThrows(PapaPizzaRunException.class, () -> cstmrMgmt.deleteById(cstmrMgmt.getDeleteLinkCustomer().getId()));
	}

    @Test
    void createCustomerTest() {
		//null
		assertThrows(NullPointerException.class, () -> cstmrMgmt.createCustomer(null));

		//normal
		CustomerCreationForm normalForm = new CustomerCreationForm("Reynholm Industries","0118 999 881 999 119 7253",
				"Moss","Maurice");
		Customer c = cstmrMgmt.createCustomer(normalForm);
		//TODO db
		assertEquals(c.getAddress(), normalForm.getAddress());
		assertEquals(c.getFirstname(), normalForm.getFirstname());
		assertEquals(c.getLastname(), normalForm.getLastname());
		//unified phone is tested somewhere else

		//phone no. alr exists
		assertThrows(CustomerAlrExistsException.class, ()->cstmrMgmt.createCustomer(normalForm));
    }

    @Test
	void saveTest(){
		Customer c = new Customer();
		c.setFirstname("Maurice");
		c.setLastname("Moss");
		c.setPhone("01189998819991197253");
		c.setAddress("Reynholm Industries");
		cstmrMgmt.save(c);

		//phone alr exists
		c.setPhone(testCustomer.getPhone());
		assertThrows(CustomerAlrExistsException.class, () -> cstmrMgmt.save(c));

		/*Customer cus = cstmrMgmt.findById(c.getId()).get();
		System.out.println(cus.getPhone());*/

		//try saving meta customer
		assertThrows(PapaPizzaRunException.class, () -> cstmrMgmt.save(cstmrMgmt.getDeleteLinkCustomer()));
	}

	@Test
	void addNewDishsetsTest(){
		ShopOrder shopOrder = new ShopOrder();
		shopOrder.setCustomer(testCustomer);
		shopOrder.setTimeCreated(LocalDateTime.now());
		long setsB4 = testCustomer.getDishsetSize();
		DishsetProduct dishset = shopCatalogManagement.createDishsetProduct("testset","69");
		shopOrder.addOrderLine(dishset, Quantity.of(4L));
		cstmrMgmt.addNewDishsets(shopOrder);
		assertEquals(setsB4+4L,testCustomer.getDishsetSize());
	}

	@Test
	void returnDishsetOfCustomerTest(){
		logger.info("testCustomer's dishsets:"+testCustomer.getDishsetSize());
		DishsetProduct dishsetType = shopCatalogManagement.createDishsetProduct("testset","69");
		Employee DELETE_LINK_EMPL = employeeManagement.getDeleteLinkEmployee();

		//normal functioning cases
		LentDishset set1 = new LentDishset(LocalDateTime.now(),dishsetType);
		LentDishset set2 = new LentDishset(LocalDateTime.now(),dishsetType);
		//past return date
		LentDishset set3 = new LentDishset(Instant.ofEpochMilli(0L)
				.atZone(ZoneOffset.systemDefault()).toLocalDateTime(), dishsetType);
		LentDishset notAssociated = new LentDishset(LocalDateTime.now(),dishsetType);
		lentDishsetRepo.save(notAssociated);
		testCustomer.getLentDishsets().add(set1);
		testCustomer.getLentDishsets().add(set2);
		testCustomer.getLentDishsets().add(set3);
		cstmrMgmt.save(testCustomer);
		Map<Long, Boolean> returnMap = new HashMap<>(
				Map.of(set1.getId(),false,
						set2.getId(),false,
						set3.getId(),false,
						notAssociated.getId(),false));
		ShopOrder returnOrder = cstmrMgmt.returnDishsetOfCustomer(returnMap,testCustomer,DELETE_LINK_EMPL);
		//order should contain 2 returns
		assertEquals(2L, returnOrder.getOrderLines().toList().get(0).getQuantity().getAmount().longValue());
		//map should be set correctly
		assertEquals(returnMap.get(set1.getId()),true);
		assertEquals(returnMap.get(set2.getId()),true);
		assertEquals(returnMap.get(set3.getId()),false);

		//invoice should have been created
		//cheap way to ensure the file exists
		assertNotNull(invoiceHandler.getInvoiceResponse(returnOrder.getInvoiceFilename()));

		//empty map
		Map<Long,Boolean> emptyMap=new HashMap<>();
		assertNull(cstmrMgmt.returnDishsetOfCustomer(emptyMap,testCustomer,DELETE_LINK_EMPL));

		//XXX:cant really and wont test for io errors
	}

	@Test
	void findByMetaTest(){
		assertFalse(cstmrMgmt.findByMeta(Customer.Meta.DELETE_LINK_CUSTOMER).isEmpty());
	}

	@Test
	void existsByIdTest(){
		assertTrue(cstmrMgmt.existsById(testCustomer.getId()));
	}
}