package papapizza.inventory.controller;

import lombok.NonNull;
import org.salespointframework.catalog.ProductIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import papapizza.delivery.DeliveryManagement;
import papapizza.inventory.ProductCategory;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.inventory.creationForms.PizzaProductCreationForm;
import papapizza.inventory.items.PizzaProduct;
import papapizza.inventory.items.ToppingProduct;
import papapizza.validation.InventoryRejectField;
import papapizza.validation.InventoryValidator;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Controller Class for {@link PizzaProduct} for Inventory frontend
 */
@Controller
public class ShopCatalogPizzaProductController {
	private final ShopCatalogManagement catalogManagement;
	private InventoryValidator inventoryValidator;
	private DeliveryManagement deliveryManagement;


	@Autowired
	public ShopCatalogPizzaProductController(ShopCatalogManagement catalogManagement) {
		this.catalogManagement = catalogManagement;
	}

	@Autowired
	public void setDeliveryManagement(@NonNull DeliveryManagement deliveryManagement) {
		this.deliveryManagement = deliveryManagement;
	}

	@Autowired
	public void setInventoryValidator(@NonNull InventoryValidator inventoryValidator) {
		this.inventoryValidator = inventoryValidator;
	}

	//
	//add PizzaPreset
	//

	@GetMapping("/addPizzaPreset")
	@PreAuthorize("hasAnyRole('BOSS')")
	String addPizzaPreset(@ModelAttribute("form") PizzaProductCreationForm form,
						  Model model) {

		List<ToppingProduct> toppings =
				catalogManagement.findByCategory(ProductCategory.TOPPING.toString()).stream().
								 map(product -> (ToppingProduct) product).collect(Collectors.toList());

		PizzaProductCreationForm.setFormDetails(form, toppings);
		return "inventory/addPizzaPreset";
	}

	@PostMapping("/addPizzaPreset")
	String addPizzaPreset(@Valid @ModelAttribute("form") PizzaProductCreationForm form, BindingResult result,
						  RedirectAttributes attributes) {

		if (result.hasErrors()) {
			return "inventory/addPizzaPreset";
		}

		//validation of the form
		List<InventoryRejectField> resultFieldList = new ArrayList<>();
		resultFieldList.addAll(inventoryValidator.formValidator(form.getName(), form.getPrice()));

		// adding rejectValues if form is invalid
		if (resultFieldList.size() > 0) {
			for (int i = 0; i < resultFieldList.size(); i++) {
				result.rejectValue(resultFieldList.get(i).toString().toLowerCase(),
								   inventoryValidator.resultRejectMatcher(resultFieldList.get(i)),
								   "an error occured");
			}
			return "inventory/addPizzaPreset";
		}

		//actual creation of the product
		catalogManagement.createPizzaProduct(form.getName(), form.getPrice(),
											 catalogManagement.ToppingMapToList(form));
		//=> see method above (aditional Methods in Management)

		//flash attributes for displaying creation of product
		attributes.addFlashAttribute("InventoryActionResult", "newSuccess");
		attributes.addFlashAttribute("ProductNameResult", form.getName());

		return "redirect:/inventory";
	}

	//
	//editPizza
	//

	@GetMapping("/inventory/editPizzaPreset/{id}")
	@PreAuthorize("hasAnyRole('BOSS')")
	String editPizzaProduct(@PathVariable ProductIdentifier id,
							@ModelAttribute("form") PizzaProductCreationForm form,
							Model model) {
		PizzaProduct pizza = (PizzaProduct) catalogManagement.findById(id);
		//boolean for thymeleaf to display error message if the product with this id does not exist
		model.addAttribute("productFound", pizza != null); //don't know if the repo returns null

		//add product to model if present
		if (pizza != null) {
			//adding model attributes attributes
			model.addAttribute("pizzaPreset", pizza);
			model.addAttribute("pizzaPresetID", pizza.getId());

			//set Toppings in form
			List<ToppingProduct> toppings =
					catalogManagement.findByCategory(ProductCategory.TOPPING.toString()).stream().
									 map(product -> (ToppingProduct) product).collect(Collectors.toList());
			PizzaProductCreationForm.setFormDetails(form, pizza, toppings);

		}
		return "inventory/editPizzaPreset";
	}

	@PostMapping("/inventory/editPizzaPreset/{id}")
	@PreAuthorize("hasAnyRole('BOSS')")
	String editPizzaProduct(@PathVariable ProductIdentifier id,
							@Valid @ModelAttribute(name = "form") PizzaProductCreationForm form,
							BindingResult result, Model model,
							RedirectAttributes attributes) {

		PizzaProduct pizza = (PizzaProduct) catalogManagement.findById(id);
		String ActionResult = "editFailed";

		//add product to model if present
		if (pizza != null) {
			//adding model attributes for redirecting back to editPizzaPreset if necessary
			model.addAttribute("productFound", true);
			model.addAttribute("pizzaPreset", pizza);
			model.addAttribute("pizzaPresetID", pizza.getId());

			//checking if result has errrors
			if (result.hasErrors()) {
				return "inventory/editPizzaPreset";
			}

			//validation of the form
			List<InventoryRejectField> resultFieldList = new ArrayList<>();
			resultFieldList.addAll(inventoryValidator.formValidator(form.getName(), form.getPrice(), id));

			// adding rejectValues if form is invalid
			if (resultFieldList.size() > 0) {
				for (int i = 0; i < resultFieldList.size(); i++) {
					result.rejectValue(resultFieldList.get(i).toString().toLowerCase(),
									   inventoryValidator.resultRejectMatcher(resultFieldList.get(i)),
									   "an error occured");
				}
				return "inventory/editPizzaPreset";
			}

			//checking if the edit changed anything
			if (catalogManagement.comparePizza(pizza, form)) {
				ActionResult = "editUnchanged";
			}

			//setting edits in Product
			PizzaProductCreationForm.setProductDetails(form, pizza); //static method for setting all details

			//trying to save
			Boolean saveWorked = catalogManagement.trySave(pizza); //actual save

			//set ActionResult based on if save worked and if edit changed something
			ActionResult = inventoryValidator.addSaveActionResult(saveWorked, ActionResult);

			//adding attributes to show in frontend if the save worked
			attributes.addFlashAttribute("InventoryActionResult", ActionResult);
			attributes.addFlashAttribute("ProductNameResult", pizza.getName());


		} else {
			throw new ResponseStatusException(NOT_FOUND, "Unable to find product for id " + id);
		}

		return "redirect:/inventory";
	}

}
