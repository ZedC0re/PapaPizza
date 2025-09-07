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
import papapizza.inventory.creationForms.DishsetProductCreationForm;
import papapizza.inventory.items.DishsetProduct;
import papapizza.validation.InventoryRejectField;
import papapizza.validation.InventoryValidator;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Controller Class for {@link DishsetProduct} for Inventory frontend
 */
@Controller
public class ShopCatalogDishsetProductController {
	private final ShopCatalogManagement catalogManagement;
	private InventoryValidator inventoryValidator;
	private DeliveryManagement deliveryManagement;


	@Autowired
	public ShopCatalogDishsetProductController(ShopCatalogManagement catalogManagement) {
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

	@GetMapping("/addDishset")
	@PreAuthorize("hasAnyRole('BOSS')")
	String addDishset(@ModelAttribute("form") DishsetProductCreationForm form) {

		return "inventory/addDishset";
	}

	@PostMapping("/addDishset")
	String addDishset(@Valid @ModelAttribute("form") DishsetProductCreationForm form, BindingResult result,
					  RedirectAttributes attributes) {
		if (result.hasErrors()) {
			return "inventory/addDishset";
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
			return "inventory/addDishset";
		}

		//actual creation of the product
		catalogManagement.createDishsetProduct(form.getName(), form.getPrice());

		//flash attributes for displaying creation of product
		attributes.addFlashAttribute("InventoryActionResult", "newSuccess");
		attributes.addFlashAttribute("ProductNameResult", form.getName());
		return "redirect:/inventory";
	}

	//
	//editDishset
	//

	@GetMapping("/inventory/editDishset/{id}")
	@PreAuthorize("hasAnyRole('BOSS')")
	String editDishset(@PathVariable ProductIdentifier id,
					   @ModelAttribute("form") DishsetProductCreationForm form,
					   Model model) {
		DishsetProduct dishset = (DishsetProduct) catalogManagement.findById(id);
		//boolean for thymeleaf to display error message if the product with this id does not exist
		model.addAttribute("productFound", dishset != null);
		//add product to model if present
		if (dishset != null) {
			model.addAttribute("dishset", dishset);
			model.addAttribute("DishsetID", dishset.getId());

			DishsetProductCreationForm.setFormDetails(form, dishset);

		}

		return "inventory/editDishset";
	}


	@PostMapping(value = "/inventory/editDishset/{id}")
	@PreAuthorize("hasAnyRole('BOSS')")
	String editDishsetProduct(@PathVariable ProductIdentifier id,
							  @Valid @ModelAttribute(name = "form") DishsetProductCreationForm form,
							  BindingResult result, Model model, RedirectAttributes attributes) {

		DishsetProduct dishset = (DishsetProduct) catalogManagement.findById(id);
		String ActionResult = "editFailed";

		if (dishset != null) {
			//adding model attributes for redirecting back to editDishset if necessary
			model.addAttribute("productFound", true);
			model.addAttribute("dishset", dishset);
			model.addAttribute("DishsetID", dishset.getId());

			//checking if result has errrors
			if (result.hasErrors()) {
				return "inventory/editDishset";
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
				return "inventory/editDishset";
			}

			//checking if the edit changed anything
			if (catalogManagement.compareDishset(dishset, form)) {
				ActionResult = "editUnchanged";
			}

			//setting edits in Product
			DishsetProductCreationForm.setProductDetails(form, dishset); //static method for setting all details

			//trying to save
			Boolean saveWorked = catalogManagement.trySave(dishset); //actual save

			//set ActionResult based on if save worked and if edit changed something
			ActionResult = inventoryValidator.addSaveActionResult(saveWorked, ActionResult);

			//adding attributes to show in frontend if the save worked
			attributes.addFlashAttribute("InventoryActionResult", ActionResult);
			attributes.addFlashAttribute("ProductNameResult", dishset.getName());

		} else {
			throw new ResponseStatusException(NOT_FOUND, "Unable to find product for id " + id);
		}

		return "redirect:/inventory";
	}


}
