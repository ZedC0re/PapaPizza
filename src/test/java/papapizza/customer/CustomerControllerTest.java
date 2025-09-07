package papapizza.customer;

import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import papapizza.employee.Employee;
import papapizza.employee.EmployeeManagement;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.inventory.items.DishsetProduct;
import papapizza.order.InvoiceHandler;
import papapizza.order.ShopOrder;

import javax.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerControllerTest {
	private final Logger logger = LoggerFactory.getLogger(CustomerControllerTest.class);

	@Autowired
	private MockMvc mvc;

	@Autowired
	private CustomerManagement cstmrMgmt;

	@Autowired
	private ShopCatalogManagement shopCatalogManagement;

	@Autowired
	private InvoiceHandler invoiceHandler;

	@Test
	@WithMockUser(roles="CASHIER")
	void customerTableTest() throws Exception{
		int csCount = cstmrMgmt.findAll().size();

		mvc.perform(get("/cstmrMgmt"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attribute("customers", IsCollectionWithSize.hasSize(csCount)));

		createTestCustomer();
		MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
		requestParams.add("filterSearch","0133742069"); //test customer's phone
		requestParams.add("filterBy","");
		requestParams.add("sortBy","firstname");
		requestParams.add("sortDir","");
		mvc.perform(get("/cstmrMgmt").params(requestParams))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attribute("customers", IsCollectionWithSize.hasSize(1)));
	}

	private Customer createTestCustomer(){
		CustomerCreationForm form = new CustomerCreationForm("Kuhliefumdenteich 1337","0133742069", "Zuvall", "Rainer");
		//test customer does still exist
		if(cstmrMgmt.findByPhone(form.getPhone()).size()==0) {
			logger.info("Creating test customer");
			return cstmrMgmt.createCustomer(form);
		}else{
			logger.info("Test Customer alr exists");
			return cstmrMgmt.findByPhone(form.getPhone()).get(0);
		}
	}

	@Test
	@WithMockUser(roles="CASHIER")
	void newCustomerGetTest() throws Exception {
		mvc.perform(get("/cstmrMgmt/new"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("form"));
	}

	@Test
	@WithMockUser(roles="CASHIER")
	void newCustomerPostTest() throws Exception {
		MultiValueMap<String,String> newCsMap = new LinkedMultiValueMap<>();
		newCsMap.add("firstname","Okabe");
		newCsMap.add("lastname","Rintarou");
		newCsMap.add("phone","1048596");
		newCsMap.add("address","Akihabara");

		//no request params -> page with errors
		mvc.perform(post("/cstmrMgmt/new"))
				.andExpect(status().isOk())
				.andExpect(model().hasErrors());

		//with correct request params
		mvc.perform(post("/cstmrMgmt/new").params(newCsMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/cstmrMgmt"));

	}

	@Test
	@WithMockUser(roles="CASHIER")
	void deleteCustomer() throws Exception {
		CustomerCreationForm form = new CustomerCreationForm("木ノ葉隔れ","20211202","うずまき","ナルト");
		Customer c = cstmrMgmt.createCustomer(form);

		long id = c.getId();

		assertTrue(cstmrMgmt.findById(id).isPresent());

		mvc.perform(post("/cstmrMgmt/del/"+id))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/cstmrMgmt"));

		assertTrue(cstmrMgmt.findById(id).isEmpty());

		mvc.perform(post("/cstmrMgmt/del/"+id))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(roles="CASHIER")
	void editCustomerGetTest() throws Exception {
		CustomerCreationForm form = new CustomerCreationForm("Shiganshina","2021120201","Jäger","Eren");
		long id = cstmrMgmt.createCustomer(form).getId();

		//should successfully display data
		mvc.perform(get("/cstmrMgmt/edit/"+id))
				.andExpect(status().isOk())
				.andExpect(model().attribute("form", allOf(
						hasProperty("firstname", is(form.getFirstname())),
						hasProperty("lastname", is(form.getLastname())),
						hasProperty("address", is(form.getAddress())),
						hasProperty("phone", is(form.getPhone()))
				)));

		//id shouldn't exist -> redirect
		mvc.perform(get("/cstmrMgmt/edit/"+Long.MAX_VALUE))
				.andExpect(status().isOk())
				.andExpect(model().attribute("customerFound", is(false)));
	}

	@Test
	@WithMockUser(roles="CASHIER")
	void editCustomerPostTest() throws Exception {
		 CustomerCreationForm form = new CustomerCreationForm("Shiganshina","2021120201","Jäger","Eren");
		long id = cstmrMgmt.createCustomer(form).getId();

		MultiValueMap<String,String> editCsMap = new LinkedMultiValueMap<>();
		editCsMap.add("firstname","Levi");
		editCsMap.add("lastname","Ackermann");
		editCsMap.add("phone","2021120201");
		editCsMap.add("address","Survey Corps");

		mvc.perform(post("/cstmrMgmt/edit/"+id).params(editCsMap))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/cstmrMgmt"));

		//compare values
		assertTrue(cstmrMgmt.findById(id).isPresent());
		Customer dbCs = cstmrMgmt.findById(id).get();
		assertEquals(dbCs.getFirstname(), editCsMap.toSingleValueMap().get("firstname"));
		assertEquals(dbCs.getLastname(), editCsMap.toSingleValueMap().get("lastname"));
		assertEquals(dbCs.getAddress(), editCsMap.toSingleValueMap().get("address"));
		assertEquals(dbCs.getPhone(), editCsMap.toSingleValueMap().get("phone"));
	}

	@Test
	@WithMockUser(roles="CASHIER")
	void renewCustomerTanTest() throws Exception {
		CustomerCreationForm form = new CustomerCreationForm("Edinbruh","2021120202","P","Agent");
		Customer c = cstmrMgmt.createCustomer(form);
		long id = c.getId();
		int oldTan = c.getTanNumber();

		//correct change tan post request
		mvc.perform(post("/cstmrMgmt/renewtan/"+id))
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/cstmrMgmt"));
		//verify that tan was changed
		assertNotEquals(oldTan, c.getTanNumber());

		//incorrect request, non existing customer
		mvc.perform(post("/cstmrMgmt/renewtan/"+Long.MAX_VALUE))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(roles="CASHIER")
	void returnSetGetTest() throws Exception {
		Customer testCustomer = createTestCustomer();
		mvc.perform(get("/cstmrMgmt/returnSet/"+testCustomer.getId()))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	@WithUserDetails("boss")
	void returnSetPostTest() throws Exception {
		Customer testCustomer = createTestCustomer();

		DishsetProduct dishsetType = shopCatalogManagement.createDishsetProduct("testset","69");
		LentDishset set1 = new LentDishset(LocalDateTime.now(),dishsetType);
		testCustomer.getLentDishsets().add(set1);
		cstmrMgmt.save(testCustomer);

		MultiValueMap<String,String> returnSetMap = new LinkedMultiValueMap<>();
		returnSetMap.add("selectedSets",set1.getId()+"");

		//normal return of dishset
		MvcResult res = mvc.perform(post("/cstmrMgmt/returnSet/"+testCustomer.getId())
						.params(returnSetMap))
				.andDo(print())
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/cstmrMgmt"))
				.andReturn();

		//check if invoice created
		ShopOrder returnOrder = (ShopOrder) res.getFlashMap().get("orderCreated");
		assertNotNull(returnOrder);

		mvc.perform(get("/downloadInvoice")
						.param("orderId",returnOrder.getId()+""))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF));

		//no set selected
		mvc.perform(post("/cstmrMgmt/returnSet/"+testCustomer.getId()))
				.andDo(print())
				.andExpect(status().is(302))
				.andExpect(redirectedUrl("/cstmrMgmt"));
	}
}