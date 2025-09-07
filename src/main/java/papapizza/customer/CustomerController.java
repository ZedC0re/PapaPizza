package papapizza.customer;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.web.LoggedIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import papapizza.app.exc.PapaPizzaRunException;
import papapizza.employee.Employee;
import papapizza.employee.EmployeeManagement;
import papapizza.order.ShopOrder;
import papapizza.order.ShopOrderManagement;
import papapizza.order.ShopOrderState;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@SessionAttributes("searchFormCustomer")
public class CustomerController {

	private final Logger logger = LoggerFactory.getLogger(CustomerController.class);

	private final CustomerManagement cstmrMgmt;
	private final EmployeeManagement emplMgmt;
	private final ShopOrderManagement<ShopOrder> orderMgmt;

	@Autowired
	public CustomerController(@NonNull CustomerManagement cstmrMgmt,
							  @NonNull EmployeeManagement emplMgmt,
							  ShopOrderManagement<ShopOrder> orderMgmt) {
		this.cstmrMgmt = cstmrMgmt;
		this.emplMgmt = emplMgmt;
		this.orderMgmt = orderMgmt;
	}

	@GetMapping(value = "/cstmrMgmt")
	String cstmrMgmtPage(Model model, @ModelAttribute("searchForm") SearchParamForm searchForm) {
		if(searchForm.isEmpty()){
			if(model.containsAttribute("searchFormCustomer")) {
				logger.info("search form empty");
				model.addAttribute("searchForm", model.getAttribute("searchFormCustomer"));
				searchForm = (SearchParamForm) model.getAttribute("searchFormCustomer");
			}
		}else{
			logger.info("search form not empty");
			model.addAttribute("searchFormCustomer",searchForm);
		}

		List<Customer> customers = cstmrMgmt.findAll();
		//list should be sorted
		assert searchForm != null;
		if (searchForm.getSortBy() != null) {
			applyCustomerListSort(customers, searchForm.getSortBy());

			//reverse order if sortDir == DESC
			if (searchForm.getSortDir() != null
					&& searchForm.getSortDir().equalsIgnoreCase("DESC")) {
				Collections.reverse(customers);
			}

		}

		//list should also be filtered
		if (searchForm.getFilterSearch() != null) {
			customers = getCustomerListFiltered(customers, searchForm.getFilterSearch(), searchForm.getFilterBys());
		}

		model.addAttribute("customers", customers);

		return "cstmrMgmt/cstmrMgmt";
	}

	private void applyCustomerListSort(List<Customer> customers, String sortBy) {
		//XXX:HACK to compare without specifying type, must implement Comparable
		Map<String, Function<Customer, ? extends Comparable>> sortMap = Map.of(
				"firstname", Customer::getFirstname,
				"lastname", Customer::getLastname,
				"address", Customer::getAddress,
				"tan", Customer::getTanNumber,
				"dishset", Customer::getDishsetSize,
				"phone", Customer::getPhone
		);
		for (Map.Entry<String, Function<Customer, ? extends Comparable>> entry : sortMap.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(sortBy)) {
				customers.sort(Comparator.comparing(entry.getValue()));
				break;
			}
		}
	}

	/**
	 * lets you filter a list of customers with a predefined search query and a list of attributes to consider
	 * @param customers    filter customer
	 * @param filterSearch search string
	 * @param filterBys    list of fields to filter
	 * @return filtered customers
	 */
	private List<Customer> getCustomerListFiltered(List<Customer> customers, String filterSearch, List<String> filterBys) {
		List<Customer> csFiltered = new ArrayList<>();

		customers.forEach(customer -> {
			//if customer fields contain string specified by search
			if (csContainsFilter(customer, filterSearch, filterBys)) {
				//add customer to list
				csFiltered.add(customer);
			}
		});
		return csFiltered;
	}

	private boolean csContainsFilter(Customer customer, String filterSearch, List<String> filterBys) {
		Map<String, String> csValueMap = Map.of(
				"firstname", customer.getFirstname(),
				"lastname", customer.getLastname(),
				"address", customer.getAddress(),
				"phone", customer.getPhone(),
				"dishset", String.valueOf(customer.getDishsetSize()),
				"tan", String.valueOf(customer.getTanNumber())
		);

		for (Map.Entry<String, String> entry : csValueMap.entrySet()) {
			if ((filterBys.isEmpty() || filterBys.contains(entry.getKey())) && //should filter by this attribute
					StringUtils.containsIgnoreCase(entry.getValue(), filterSearch)) { //contains the search string
				return true;
			}
		}
		return false;
	}

	@GetMapping(value = "/cstmrMgmt/new")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String newCustomerGet(@ModelAttribute("form") CustomerCreationForm form) {
		return "cstmrMgmt/newCustomer";
	}

	@PostMapping(value = "/cstmrMgmt/new")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String newCustomerPost(@Valid @ModelAttribute("form") final CustomerCreationForm form,
						   final BindingResult result,
						   final RedirectAttributes attributes) {
		if (result.hasErrors()) {
			return "cstmrMgmt/newCustomer";
		}

		try {
			Customer cust = cstmrMgmt.createCustomer(form);

			attributes.addFlashAttribute("csModifyResult", "newSuccess");
			attributes.addFlashAttribute("csModifyResultId", cust.getId());
		} catch (CustomerAlrExistsException e) { //XXX avoid Exceptions as control flow -> rewrite to use null customer
			//phone number alr exists in db
			result.rejectValue("phone", "CustomerCreationForm.phoneExists", "Phone does already exist");
			return "cstmrMgmt/newCustomer";
		}

		return "redirect:/cstmrMgmt";
	}

	@PostMapping(value = "/cstmrMgmt/del/{id}") //forms don't do HTTP-DELETE requests anymore
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String deleteCustomer(@PathVariable final long id,
						  final RedirectAttributes attributes) {
		Optional<Customer> delCstmr = cstmrMgmt.findById(id);
		attributes.addFlashAttribute("csModifyResultId", id);
		if (delCstmr.isPresent()) {
			//XXX move logic to management
			//customer might have an active order -> delete impossible
			if (orderMgmt.findBy(delCstmr.get()).stream().map(ShopOrder::getShopOrderState).anyMatch(ShopOrderState::isActive)) {
				attributes.addFlashAttribute("csModifyResult", "delStillActiveOrder");
			}else { //all orders completed
				//replace all completed & canceled with DELETE_LINK_CUSTOMER
				Customer deleteLinkCs = cstmrMgmt.getDeleteLinkCustomer();
				for (ShopOrder order : orderMgmt.findBy(delCstmr.get())) {
					order.setCustomer(deleteLinkCs);
					orderMgmt.save(order);
				}
				//finally delete customer
				cstmrMgmt.deleteById(id);
				attributes.addFlashAttribute("csModifyResult", "delSuccess");
			}
			return "redirect:/cstmrMgmt";
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find customer for id " + id);
		}
	}

	@GetMapping(value = "/cstmrMgmt/edit/{id}")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String editCustomerGet(@PathVariable long id,
						   @ModelAttribute("form") CustomerCreationForm form,
						   Model model) {
		try {
			Customer customer = getExistingCustomer(id, model);
			form.setFormDetails(customer);
		} catch (ResponseStatusException ignored) {
			logger.error(ignored.toString());
		}

		return "cstmrMgmt/editCustomer";
	}

	@PostMapping(value = "/cstmrMgmt/edit/{id}")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String editCustomerPost(@PathVariable final long id,
							@Valid @ModelAttribute(name = "form") final CustomerCreationForm form,
							final BindingResult result,
							final Model model,
							final RedirectAttributes attributes,
							final HttpServletRequest request) {

		Customer customer = getExistingCustomer(id, model); //also adds customerFound to model if customer in db
		attributes.addFlashAttribute("csModifyResultId", id);

		if (result.hasErrors()) {
			return "cstmrMgmt/editCustomer";
		}

		if (form.isAllSameData(customer)) {
			form.setCustomerDetails(customer); //set all attributes on customer

			if (trySaveCustomer(customer, result)) {
				attributes.addFlashAttribute("csModifyResult", "editSuccess");
			} else {
				return "cstmrMgmt/editCustomer"; //resend page
			}
		} else {
			attributes.addFlashAttribute("csModifyResult", "editUnchanged");
		}

		return "redirect:/cstmrMgmt"; //redirect to management page
	}

	private Customer getExistingCustomer(long id, Model model) throws ResponseStatusException {
		Optional<Customer> optCustomer = cstmrMgmt.findById(id);

		if (optCustomer.isEmpty()) {
			model.addAttribute("customerFound", false);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find customer for id " + id);
		}

		if (optCustomer.get().getMeta() == Customer.Meta.DELETE_LINK_CUSTOMER) {
			model.addAttribute("customerFound", false);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meta customer is immutable");
		}

		//boolean for thymeleaf to display error message if customer not present
		model.addAttribute("customerFound", true);
		model.addAttribute("customerId", id);
		return optCustomer.get();
	}

	private boolean trySaveCustomer(Customer customer, BindingResult result) {
		try {
			cstmrMgmt.save(customer); //make changes persistent in db
		} catch (CustomerAlrExistsException e) {
			//if phone number alr in db
			result.rejectValue("phone", "CustomerCreationForm.phoneExists", "Phone does already exist");
			return false;
		}
		return true;
	}

	@PostMapping(value = "/cstmrMgmt/renewtan/{id}")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String renewCustomerTan(@PathVariable final long id, final RedirectAttributes attributes) {
		Optional<Customer> optCustomer = cstmrMgmt.findById(id);

		if (optCustomer.isPresent()) {
			cstmrMgmt.renewCustomerTan(optCustomer.get());
			attributes.addFlashAttribute("csModifyResult", "tanSuccess");
			attributes.addFlashAttribute("csModifyResultId", id);
			return "redirect:/cstmrMgmt";
		} else {
			throw new ResponseStatusException(NOT_FOUND, "Unable to find customer for id " + id);
		}
	}

	//===================DISHSET RETURN===================
	@GetMapping(value = "/cstmrMgmt/returnSet/{id}")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String returnSetGet(@PathVariable final long id, Model model) {
		Customer customer = getExistingCustomer(id, model);

		model.addAttribute("dishsets", customer.getLentDishsets());
		return "cstmrMgmt/returnSet";
	}

	@PostMapping(value = "/cstmrMgmt/returnSet/{id}")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String returnSetPost(@PathVariable final long id,
						 @RequestParam(name = "selectedSets", required = false) List<String> selectedSets,
						 final RedirectAttributes attributes,
						 final Model model,
						 @LoggedIn UserAccount userAccount) {
		Customer customer = getExistingCustomer(id, model);

		if (selectedSets == null) { //no sets selected in form
			attributes.addFlashAttribute("csModifyResult", "setNotSelected");
		} else {
			logger.info("Selected Sets:" + selectedSets);
			//list of strings to map of Long with all values == false
			Map<Long, Boolean> selectedSetsAsLong = selectedSets.stream().map(Long::valueOf)
					.collect(Collectors.toMap(i -> i, i -> false));
			Optional<Employee> currentEmpl = emplMgmt.findByUserAccount(userAccount);
			if (currentEmpl.isEmpty()) {
				throw new PapaPizzaRunException("Employee with userAccount not present in db");
			}

			ShopOrder returnOrder = cstmrMgmt.returnDishsetOfCustomer(selectedSetsAsLong, customer, currentEmpl.get());
			if (returnOrder == null) { //returnOrder might be null if all passed IDs are bad
				attributes.addFlashAttribute("csModifyResult", "setRemovedError");
			} else {
				logger.info("returnOrder:" + returnOrder);
				attributes.addFlashAttribute("orderCreated", returnOrder);

				//check if map contains errors -> not all dishsets could be returned
				if (selectedSetsAsLong.containsValue(false)) {
					attributes.addFlashAttribute("csModifyResult", "setRemovedError");
				} else {
					attributes.addFlashAttribute("csModifyResult", "setRemovedSuccess");
				}
			}
		}

		return "redirect:/cstmrMgmt";
	}
}