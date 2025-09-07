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
import papapizza.inventory.creationForms.OvenProductCreationForm;
import papapizza.inventory.items.OvenProduct;
import papapizza.validation.InventoryRejectField;
import papapizza.validation.InventoryValidator;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Controller Class for {@link OvenProduct} for Inventory frontend
 */
@Controller
public class ShopCatalogOvenProductController {
	private final ShopCatalogManagement catalogManagement;
	private InventoryValidator inventoryValidator;
	private DeliveryManagement deliveryManagement;


	@Autowired
	public ShopCatalogOvenProductController(ShopCatalogManagement catalogManagement) {
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

	@GetMapping("/addOven")
	@PreAuthorize("hasAnyRole('BOSS')")
	String addOven(@ModelAttribute("form") OvenProductCreationForm form) {

		return "inventory/addOven";
	}

	@PostMapping("/addOven")
	String addOven(@Valid @ModelAttribute("form") OvenProductCreationForm form, BindingResult result,
				   RedirectAttributes attributes) {
		if (result.hasErrors()) {
			return "inventory/addOven";
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
			return "inventory/addOven";
		}

		//actual creation of the product
		OvenProduct oven = catalogManagement.createOvenProduct(form.getName(), form.getPrice());

		//flash attributes for displaying creation of product
		attributes.addFlashAttribute("InventoryActionResult", "newSuccess");
		attributes.addFlashAttribute("ProductNameResult", oven.getName());
		return "redirect:/inventory";
	}

	//
	//editOven
	//

	@GetMapping("/inventory/editOven/{id}")
	@PreAuthorize("hasAnyRole('BOSS')")
	String editOven(@PathVariable ProductIdentifier id,
					@ModelAttribute("form") OvenProductCreationForm form,
					Model model) {
		OvenProduct oven = (OvenProduct) catalogManagement.findById(id);
		//boolean for thymeleaf to display error message if the product with this id does not exist
		model.addAttribute("productFound", oven != null);

		//add product to model if present
		if (oven != null) {
			model.addAttribute("oven", oven);
			model.addAttribute("OvenID", oven.getId());

			OvenProductCreationForm.setFormDetails(form, oven);

		}

		return "inventory/editOven";
	}


	@PostMapping(value = "/inventory/editOven/{id}")
	@PreAuthorize("hasAnyRole('BOSS')")
	String editOvenProduct(@PathVariable ProductIdentifier id,
						   @Valid @ModelAttribute(name = "form") OvenProductCreationForm form,
						   BindingResult result, Model model, RedirectAttributes attributes) {

		OvenProduct oven = (OvenProduct) catalogManagement.findById(id);
		String ActionResult = "editFailed";

		if (oven != null) {
			//adding model attributes for redirecting back to editOven if necessary
			model.addAttribute("productFound", true);
			model.addAttribute("oven", oven);
			model.addAttribute("OvenID", oven.getId());

			//checking if result has errrors
			if (result.hasErrors()) {
				return "inventory/editOven";
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
				return "inventory/editOven";
			}

			//checking if the edit changed anything
			if (catalogManagement.compareOven(oven, form)) {
				ActionResult = "editUnchanged";
			}

			//setting edits in Product
			OvenProductCreationForm.setProductDetails(form, oven); //static method for setting all details

			//trying to save
			Boolean saveWorked = catalogManagement.trySave(oven); //actual save

			//set ActionResult based on if save worked and if edit changed something
			ActionResult = inventoryValidator.addSaveActionResult(saveWorked, ActionResult);

			//adding attributes to show in frontend if the save worked
			attributes.addFlashAttribute("InventoryActionResult", ActionResult);
			attributes.addFlashAttribute("ProductNameResult", oven.getName());

		} else {
			throw new ResponseStatusException(NOT_FOUND, "Unable to find product for id " + id);
		}

		return "redirect:/inventory";
	}
}
