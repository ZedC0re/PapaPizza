package papapizza.kitchen;


import lombok.NonNull;
import org.salespointframework.catalog.Product;
import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.web.LoggedIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import papapizza.employee.Employee;
import papapizza.employee.EmployeeManagement;
import papapizza.inventory.ProductCategory;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.inventory.items.OvenProduct;
import papapizza.inventory.items.PizzaState;

import java.util.*;


@Controller
public class KitchenController {

	private final Logger logger = LoggerFactory.getLogger(KitchenController.class);

	private final EmployeeManagement employeeManagement;
	private final ShopCatalogManagement shopCatalogManagement;
	private final KitchenManagement kitchenManagement;

	KitchenController(@NonNull EmployeeManagement employeeManagement,
					  @NonNull ShopCatalogManagement shopCatalogManagement,
					  @NonNull KitchenManagement kitchenManagement) {
		this.employeeManagement = employeeManagement;
		this.shopCatalogManagement = shopCatalogManagement;
		this.kitchenManagement = kitchenManagement;
	}


	@GetMapping("/kitchen")
	@PreAuthorize("hasAnyRole('BOSS','CHEF')")
	String kitchenView(Model model, @LoggedIn Optional<UserAccount> loggedInUser) {
		if(loggedInUser.isEmpty()){
			return "redirect:/login";
		}

		List<DisplayableKitchen> displayKitchens = new ArrayList<>();
		if(loggedInUser.get().hasRole(EmployeeManagement.BOSS)){
			logger.info("showing kitchen as boss");
			employeeManagement.findByRole("Chef").forEach(
					employee -> displayKitchens.add(getKitchenOfEmployee(employee)));
		}else{
			Employee employee = employeeManagement.findByUserAccount(loggedInUser.get()).get();
			logger.info("showing kitchen as "+employee.getUsername());
			displayKitchens.add(getKitchenOfEmployee(employee));
		}

		model.addAttribute("displayKitchens", displayKitchens);
		return "kitchen/kitchen";
	}

	private DisplayableKitchen getKitchenOfEmployee(Employee employee){
		DisplayableKitchen displayKitchen = new DisplayableKitchen();

		//get oven of employee
		Optional<Product> optOven = shopCatalogManagement.findByCategory(
				ProductCategory.OVEN.toString()).stream().filter(
						filterOven -> ((OvenProduct)filterOven).getChef() != null &&
									  ((OvenProduct)filterOven).getChef().equals(employee)).findFirst();

		if(optOven.isEmpty()){
			displayKitchen.setEmpty(true);
		}else {
			OvenProduct oven = (OvenProduct) optOven.get();

			//assign DisplayableKitchen props
			displayKitchen.setOven(oven);
			Map<ProductIdentifier, Integer> times = new HashMap<>();
			oven.getPizzas().forEach(pizzaProduct -> {
				if (pizzaProduct.getState() == PizzaState.PENDING) {
					times.put(pizzaProduct.getId(), kitchenManagement.getTimeLeft(pizzaProduct));
				}
			});
			displayKitchen.setTimes(times);
		}
		return displayKitchen;
	}


	@PostMapping("/kitchen/bake/{prodId}")
	@PreAuthorize("hasAnyRole('BOSS','CHEF')")
	String startBaking(Model model, @PathVariable final ProductIdentifier prodId, RedirectAttributes attributes){
		kitchenManagement.changePizzaState(prodId, PizzaState.PENDING);
		return "redirect:/kitchen";
	}


	@PostMapping("/kitchen/finish/{prodId}")
	@PreAuthorize("hasAnyRole('BOSS','CHEF')")
	String finishBaking(Model model, @PathVariable final ProductIdentifier prodId, RedirectAttributes attributes) {
		kitchenManagement.changePizzaState(prodId, PizzaState.READY);
		return "redirect:/kitchen";
	}


}