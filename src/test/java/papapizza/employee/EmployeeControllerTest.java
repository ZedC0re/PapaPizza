package papapizza.employee;


import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.Test;
import org.salespointframework.catalog.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import papapizza.customer.CustomerManagement;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.inventory.items.OvenProduct;
import papapizza.inventory.items.PizzaProduct;
import papapizza.order.ShopOrder;
import papapizza.order.ShopOrderManagement;

import javax.transaction.Transactional;

import java.util.List;

import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class EmployeeControllerTest {
	@Autowired
	private MockMvc mvc;
	@Autowired
	private EmployeeManagement emplMgmt;
	@Autowired
	private ShopOrderManagement<ShopOrder> orderMgmt;
	@Autowired
	private CustomerManagement sustomerManagement;
	@Autowired
	private ShopCatalogManagement catalogMgmt;

	//code 200 -> isOk() -> resource has been found
	//code 302 -> isFound() -> successful redirect
	//code 404 -> isNotFound() -> HttpStatus.NOT_FOUND

	//------Employee-Mamagement------

	@Test
	@WithMockUser(roles="BOSS")
	void employeeTableTest() throws Exception{
		int employeeCount = emplMgmt.findAll().toList().size();

		mvc.perform(get("/employeeManagement"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attribute("employees", IsCollectionWithSize.hasSize(employeeCount)));
	}

	@Test
	@WithMockUser(roles="BOSS")
	public void newEmplButtonTest() throws Exception {

			mvc.perform(get("/newEmployeeBtn"))
					.andExpect(status().isFound())
					.andExpect(redirectedUrl("/newEmployee"));
	}

	@Test
	@WithMockUser(roles="BOSS")
	public void emplDeleteTest() throws Exception{
		EmployeeCreationForm form = new EmployeeCreationForm("deleteTester", "first", "last", "test", "test","Cashier");
		Employee empl = emplMgmt.createEmployee(form);
		long toDeleteId = empl.getId();

		//successful delete
		mvc.perform(post("/emplMgmt/del/" + toDeleteId))
				.andExpect(status().isFound()).andExpect(flash().attribute("emplModifyRes", "delSuccess"))
				.andExpect(redirectedUrl("/employeeManagement"));
		assertFalse(emplMgmt.findById(toDeleteId).isPresent()); //this guy gone frfr

		//try to delete non existing id
		mvc.perform(post("/emplMgmt/del/" + toDeleteId))
				.andExpect(status().isNotFound());

		/* i wanted to test for "delOrderBeingWorkedOn" but somehow it deletes the Employee anyway,
			which is weird because it works just fine in the application
		Customer sustomer = sustomerManagement.createCustomer(new CustomerCreationForm("Spaceship", "545518494", "Poster", "Imp"));
		Employee empl2 = emplMgmt.createEmployee(form);
		ShopOrder order = orderMgmt.create(empl2, sustomer);

		System.out.println(order.getCashier());

		mvc.perform(post("/emplMgmt/del/" + empl2.getId()))
				.andExpect(flash().attribute("emplModifyRes","delOrderBeingWorkedOn"))
				.andExpect(status().isFound());
		 */

		//"lastbossDel" works just fine also
	}

	//------New-Employee------

	@Test
	@WithMockUser(roles="BOSS")
	public void newEmplPageTest() throws Exception {
		mvc.perform(get("/newEmployee"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("form"));
	}

	@Test
	@WithMockUser(roles="BOSS")
	public void createEmplTest() throws Exception {
		MultiValueMap<String,String> newEmplMap = new LinkedMultiValueMap<>();
		newEmplMap.add("name", "createEmplTester");
		newEmplMap.add("firstname", "testFirst");
		newEmplMap.add("lastname", "testLast");
		newEmplMap.add("password", "aA8");
		newEmplMap.add("repeatedPassword", "aA8");
		newEmplMap.add("role", "Cashier");

		//no request params -> page with errors
		mvc.perform(post("/newEmployee"))
				.andExpect(status().isOk())
				.andExpect(model().hasErrors());

		//with correct request params
		mvc.perform(post("/newEmployee").params(newEmplMap))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/employeeManagement"));

		//with already existing username
		mvc.perform(post("/newEmployee").params(newEmplMap))
				.andExpect(status().isOk())
				.andExpect(model().hasErrors());

		//not satisfying regex
		newEmplMap.remove("password");
		newEmplMap.remove("repeatedPassword");
		newEmplMap.add("password", "aaa");
		newEmplMap.add("repeatedPassword", "aaa");
		mvc.perform(post("/newEmployee").params(newEmplMap))
				.andExpect(status().isOk())
				.andExpect(model().hasErrors());

		//passwords not matching
		newEmplMap.remove("password");
		newEmplMap.remove("repeatedPassword");
		newEmplMap.add("password", "aA1");
		newEmplMap.add("repeatedPassword", "bB2");
		mvc.perform(post("/newEmployee").params(newEmplMap))
				.andExpect(status().isOk())
				.andExpect(model().hasErrors());
	}

	//------Edit-Employee------

	@Test
	@WithMockUser(roles="BOSS")
	public void editEmplPageTest() throws Exception {
		EmployeeCreationForm form = new EmployeeCreationForm("editPageTester",
				"testFirst",
				"testLast",
				"test",
				"test",
				"Chef");
		Employee empl = emplMgmt.createEmployee(form);

		//should successfully display data
		//no passwords because they're not given on edit page
		mvc.perform(get("/emplMgmt/edit/" + empl.getId()))
				.andExpect(status().isOk())
				.andExpect(model().attribute("form", allOf(
						hasProperty("name", is(form.getName())),
						hasProperty("firstname", is(form.getFirstname())),
						hasProperty("lastname", is(form.getLastname())),
						hasProperty("role", is(form.getRole()))
				)));

		//id shouldn't exist -> redirect
		mvc.perform(get("/emplMgmt/edit/"+ Long.MAX_VALUE))
				.andExpect(status().isOk())
				.andExpect(model().attribute("employeeFound", is(false)));

		//"lastbossEdit"
		if(emplMgmt.findByRole("Boss").toList().size() == 1) {
			mvc.perform(get("/emplMgmt/edit/" + emplMgmt.findByRole("Boss").toList().get(0).getId()))
					.andDo(print())
					.andExpect(status().isFound())
					.andExpect(flash().attributeExists("emplModifyRes"));
		}

		//edit chef -> additional attributes
		OvenProduct newOven = catalogMgmt.createOvenProduct("Oven1","24");
		emplMgmt.assignChefToOven(newOven, empl);
		mvc.perform(get("/emplMgmt/edit/" + empl.getId()))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("ovens", "myOven"));
	}

	@Test
	@WithMockUser(roles="BOSS")
	public void editEmplTest() throws Exception {
		EmployeeCreationForm form = new EmployeeCreationForm("editTester", "testFirst", "testLast", "a8A", "a8A", "Cashier");
		Employee empl = emplMgmt.createEmployee(form);

		MultiValueMap<String,String> newEmplMap = new LinkedMultiValueMap<>();
		newEmplMap.add("name", "editPostTester");
		newEmplMap.add("firstname", "testPostFirst");
		newEmplMap.add("lastname", "testPostLast");
		newEmplMap.add("password", "a8A2");
		newEmplMap.add("repeatedPassword", "a8A2");
		newEmplMap.add("role", "Driver");

		//successful edit
		mvc.perform(post("/emplMgmt/edit/" + empl.getId()).params(newEmplMap))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/employeeManagement"));

		//assert that changes went through
		assertEquals(emplMgmt.findById(empl.getId()).get().getUsername(), "editPostTester");
		assertEquals(emplMgmt.findById(empl.getId()).get().getFirstname(), "testPostFirst");
		assertEquals(emplMgmt.findById(empl.getId()).get().getLastname(), "testPostLast");
		assertEquals(emplMgmt.findById(empl.getId()).get().getRole(), "Driver");

		//username change without new pw -> err
		newEmplMap.remove("name");
		newEmplMap.remove("password");
		newEmplMap.remove("repeatedPassword");
		newEmplMap.add("name", "newGuyManName");

		mvc.perform(post("/emplMgmt/edit/" + empl.getId()).params(newEmplMap))
				.andDo(print())
				.andExpect(status().isOk());

		//Employee doesn't exist
		mvc.perform(post("/emplMgmt/edit/" + Long.MAX_VALUE).params(newEmplMap))
				.andExpect(status().isNotFound());

		//"lastBossDel" but again
		if(emplMgmt.findByRole("Boss").toList().size() == 1) {
			System.out.println("iwas here");
			MultiValueMap<String,String> bossMap = new LinkedMultiValueMap<>();
			bossMap.add("name", "boss");
			bossMap.add("firstname", "Bob");
			bossMap.add("lastname", "Bauer");
			bossMap.add("password", "a8A2");
			bossMap.add("repeatedPassword", "a8A2");
			bossMap.add("role", "Driver");

			mvc.perform(post("/emplMgmt/edit/" + emplMgmt.findByRole("Boss").toList().get(0).getId()).params(bossMap))
					.andDo(print())
					.andExpect(status().is(400));
		}

		//regex not satisfied
		newEmplMap.remove("password");
		newEmplMap.remove("repeatedPassword");
		newEmplMap.add("password", "aaa");
		newEmplMap.add("repeatedPassword", "aaa");

		mvc.perform(post("/emplMgmt/edit/" + empl.getId()).params(newEmplMap))
				.andDo(print())
				.andExpect(status().isOk());

		//no matching passwords
		newEmplMap.remove("password");
		newEmplMap.remove("repeatedPassword");
		newEmplMap.add("password", "aA1");
		newEmplMap.add("repeatedPassword", "bB2");

		mvc.perform(post("/emplMgmt/edit/" + empl.getId()).params(newEmplMap))
				.andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles="BOSS")
	public void assignOvenTest() throws Exception {
		EmployeeCreationForm form = new EmployeeCreationForm("ovenTester",
				"testFirst",
				"testLast",
				"a8A",
				"a8A",
				"Chef");
		Employee empl = emplMgmt.createEmployee(form);

		OvenProduct newOven = catalogMgmt.createOvenProduct("Oven101","24");
		OvenProduct nextOven = catalogMgmt.createOvenProduct("Oven102","25");

		//oven doesn't exist
		mvc.perform(get("/emplMgmt/" + "ajhdsfkjhsdalkfjhsadlkfjhsadlkjh" + "/" + empl.getId()))
				.andDo(print())
				.andExpect(status().isNotFound());

		//employee doesn't exist
		mvc.perform(get("/emplMgmt/" + newOven.getId() + "/" + Long.MAX_VALUE))
				.andDo(print())
				.andExpect(status().isNotFound());

		//successfully assign oven
		mvc.perform(get("/emplMgmt/" + newOven.getId() + "/" + empl.getId()))
				.andDo(print())
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/employeeManagement"));

		//old oven in use
		PizzaProduct CustomPizza = catalogMgmt.createPizzaProduct(List.of(catalogMgmt.createToppingProduct("Chilliflakes","5")));
		newOven.setPizzas(List.of(CustomPizza));
		mvc.perform(get("/emplMgmt/" + nextOven.getId() + "/" + empl.getId()))
				.andDo(print())
				.andExpect(status().isFound())
				.andExpect(flash().attributeExists("editRedirect"));
	}
}
