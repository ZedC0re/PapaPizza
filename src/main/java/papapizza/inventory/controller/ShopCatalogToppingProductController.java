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
import papapizza.inventory.ShopCatalogManagement;
import papapizza.inventory.creationForms.ToppingProductCreationForm;
import papapizza.inventory.items.ToppingProduct;
import papapizza.validation.InventoryRejectField;
import papapizza.validation.InventoryValidator;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Controller Class for {@link ToppingProduct} for Inventory frontend
 */
@Controller
public class ShopCatalogToppingProductController {
	private final ShopCatalogManagement catalogManagement;
	private InventoryValidator inventoryValidator;
	private DeliveryManagement deliveryManagement;


	@Autowired
	public ShopCatalogToppingProductController(ShopCatalogManagement catalogManagement) {
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
	//add
	//

	@GetMapping("/addTopping")
	@PreAuthorize("hasAnyRole('BOSS')")
	String addTopping(@ModelAttribute("form") ToppingProductCreationForm form) {

		return "inventory/addTopping";
	}

	@PostMapping("/addTopping")
	String addTopping(@Valid @ModelAttribute("form") ToppingProductCreationForm form, BindingResult result,
					  RedirectAttributes attributes) {
		if (result.hasErrors()) {
			return "inventory/addTopping";
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
			return "inventory/addTopping";
		}

		//actual creation of the product
		catalogManagement.createToppingProduct(form.getName(), form.getPrice());

		//flash attributes for displaying creation of product
		attributes.addFlashAttribute("InventoryActionResult", "newSuccess");
		attributes.addFlashAttribute("ProductNameResult", form.getName());
		return "redirect:/inventory";
	}

	//
	//editTopping
	//

	@GetMapping("/inventory/editTopping/{id}")
	@PreAuthorize("hasAnyRole('BOSS')")
	String editTopping(@PathVariable ProductIdentifier id,
					   @ModelAttribute("form") ToppingProductCreationForm form,
					   Model model) {
		ToppingProduct topping = (ToppingProduct) catalogManagement.findById(id);
		//boolean for thymeleaf to display error message if the product with this id does not exist
		model.addAttribute("productFound", topping != null);
		//add product to model if present
		if (topping != null) {
			model.addAttribute("topping", topping);
			model.addAttribute("ToppingID", topping.getId());

			ToppingProductCreationForm.setFormDetails(form, topping);

		}

		return "inventory/editTopping";
	}


	@PostMapping(value = "/inventory/editTopping/{id}")
	@PreAuthorize("hasAnyRole('BOSS')")
	String editToppingProduct(@PathVariable ProductIdentifier id,
							  @Valid @ModelAttribute(name = "form") ToppingProductCreationForm form,
							  BindingResult result, Model model, RedirectAttributes attributes) {

		ToppingProduct topping = (ToppingProduct) catalogManagement.findById(id);
		String ActionResult = "editFailed";

		if (topping != null) {
			//adding model attributes for redirecting back to editTopping if necessary
			model.addAttribute("productFound", true);
			model.addAttribute("topping", topping);
			model.addAttribute("ToppingID", topping.getId());

			//checking if result has errrors
			if (result.hasErrors()) {
				return "inventory/editTopping";
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
				return "inventory/editTopping";
			}

			//checking if the edit changed anything
			if (catalogManagement.compareTopping(topping, form)) {
				ActionResult = "editUnchanged";
			}

			//setting edits in Product
			ToppingProductCreationForm.setProductDetails(form, topping); //static method for setting all details

			//trying to save
			Boolean saveWorked = catalogManagement.trySave(topping); //actual save

			//set ActionResult based on if save worked and if edit changed something
			ActionResult = inventoryValidator.addSaveActionResult(saveWorked, ActionResult);

			//adding attributes to show in frontend if the save worked
			attributes.addFlashAttribute("InventoryActionResult", ActionResult);
			attributes.addFlashAttribute("ProductNameResult", topping.getName());

		} else {
			throw new ResponseStatusException(NOT_FOUND, "Unable to find product for id " + id);
		}

		return "redirect:/inventory";
	}

}
