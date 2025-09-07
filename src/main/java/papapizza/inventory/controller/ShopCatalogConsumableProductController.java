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
import papapizza.inventory.creationForms.ConsumableProductCreationForm;
import papapizza.inventory.items.ConsumableProduct;
import papapizza.validation.InventoryRejectField;
import papapizza.validation.InventoryValidator;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Controller Class for {@link ConsumableProduct} (incl. Drinks) for Inventory frontend
 */
@Controller
public class ShopCatalogConsumableProductController {
	private final ShopCatalogManagement catalogManagement;
	private InventoryValidator inventoryValidator;
	private DeliveryManagement deliveryManagement;


	@Autowired
	public ShopCatalogConsumableProductController(ShopCatalogManagement catalogManagement) {
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
	//add Consumable
	//

	@GetMapping("/addConsumable")
	@PreAuthorize("hasAnyRole('BOSS')")
	String addConsumable(@ModelAttribute("form") ConsumableProductCreationForm form) {

		return "inventory/addConsumable";
	}

	@PostMapping("/addConsumable")
	String addConsumable(@Valid @ModelAttribute("form") ConsumableProductCreationForm form, BindingResult result,
						 RedirectAttributes attributes) {
		if (result.hasErrors()) {
			return "inventory/addConsumable";
		}

		//validation of the form
		List<InventoryRejectField> resultFieldList = new ArrayList<>();
		resultFieldList.addAll(inventoryValidator.formValidator(form.getName(), form.getPrice()));

		// adding rejectValues if form is invalid
		if (resultFieldList.size() > 0) {
			for (int i = 0; i < resultFieldList.size(); i++) {
				result.rejectValue(resultFieldList.get(i).toString().toLowerCase(),
								   inventoryValidator.resultRejectMatcher(resultFieldList.get(i)));
			}
			return "inventory/addConsumable";
		}

		//actual creation of the product
		catalogManagement.createConsumableProduct(form.getName(), form.getPrice(), form.getIngredients());

		//flash attributes for displaying creation of product
		attributes.addFlashAttribute("InventoryActionResult", "newSuccess");
		attributes.addFlashAttribute("ProductNameResult", form.getName());
		return "redirect:/inventory";
	}

	//
	//add Drink
	//

	@GetMapping("/addDrink")
	@PreAuthorize("hasAnyRole('BOSS')")
	String addDrink(@ModelAttribute("form") ConsumableProductCreationForm form) {

		return "inventory/addDrink";
	}

	@PostMapping("/addDrink")
	String addDrink(@Valid @ModelAttribute("form") ConsumableProductCreationForm form, BindingResult result,
					RedirectAttributes attributes) {
		if (result.hasErrors()) {
			return "inventory/addDrink";
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
			return "inventory/addDrink";
		}

		//actual creation of the product
		catalogManagement.createDrinkProduct(form.getName(), form.getPrice(), form.getIngredients());

		//flash attributes for displaying creation of product
		attributes.addFlashAttribute("InventoryActionResult", "newSuccess");
		attributes.addFlashAttribute("ProductNameResult", form.getName());
		return "redirect:/inventory";
	}

	//
	//editConsumable
	//


	@GetMapping("/inventory/editConsumable/{id}")
	@PreAuthorize("hasAnyRole('BOSS')")
	String editConsumable(@PathVariable ProductIdentifier id,
						  @ModelAttribute("form") ConsumableProductCreationForm form,
						  Model model) {
		ConsumableProduct consumable = (ConsumableProduct) catalogManagement.findById(id);
		//boolean for thymeleaf to display error message if the product with this id does not exist
		model.addAttribute("productFound", consumable != null);
		//add product to model if present
		if (consumable != null) {
			model.addAttribute("consumable", consumable);
			model.addAttribute("ConsumableID", consumable.getId());

			ConsumableProductCreationForm.setFormDetails(form, consumable);

		}

		return "inventory/editConsumable";
	}


	@PostMapping(value = "/inventory/editConsumable/{id}")
	@PreAuthorize("hasAnyRole('BOSS')")
	String editConsumableProduct(@PathVariable ProductIdentifier id,
								 @Valid @ModelAttribute(name = "form") ConsumableProductCreationForm form,
								 BindingResult result, Model model, RedirectAttributes attributes) {

		ConsumableProduct consumable = (ConsumableProduct) catalogManagement.findById(id);
		String ActionResult = "editFailed";

		if (consumable != null) {
			//adding model attributes for redirecting back to editConsumable if necessary
			model.addAttribute("productFound", true);
			model.addAttribute("consumable", consumable);
			model.addAttribute("ConsumableID", consumable.getId());

			//checking if result has errrors
			if (result.hasErrors()) {
				return "inventory/editConsumable";
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
				return "inventory/editConsumable";
			}

			//checking if the edit changed anything
			if (catalogManagement.compareConsumable(consumable, form)) {
				ActionResult = "editUnchanged";
			}

			//setting edits in Product
			ConsumableProductCreationForm.setProductDetails(form, consumable); //static method for setting all details

			//trying to save
			Boolean saveWorked = catalogManagement.trySave(consumable); //actual save

			//set ActionResult based on if save worked and if edit changed something
			ActionResult = inventoryValidator.addSaveActionResult(saveWorked, ActionResult);

			//adding attributes to show in frontend if the save worked
			attributes.addFlashAttribute("InventoryActionResult", ActionResult);
			attributes.addFlashAttribute("ProductNameResult", consumable.getName());

		} else {
			throw new ResponseStatusException(NOT_FOUND, "Unable to find product for id " + id);
		}

		return "redirect:/inventory";
	}

	//
	//editDrink
	//


	@GetMapping(value = "/inventory/editDrink/{id}")
	@PreAuthorize("hasAnyRole('BOSS')")
	String editDrink(@PathVariable ProductIdentifier id,
					 @ModelAttribute("form") ConsumableProductCreationForm form,
					 Model model) {
		ConsumableProduct drink = (ConsumableProduct) catalogManagement.findById(id);
		//boolean for thymeleaf to display error message if the product with this id does not exist
		model.addAttribute("productFound", drink != null);

		//add product to model if present
		if (drink != null) {
			model.addAttribute("drink", drink);
			model.addAttribute("DrinkID", drink.getId());

			ConsumableProductCreationForm.setFormDetails(form, drink);

		}

		return "inventory/editDrink";
	}


	@PostMapping(value = "/inventory/editDrink/{id}")
	@PreAuthorize("hasAnyRole('BOSS')")
	String editDrinkProduct(@PathVariable ProductIdentifier id,
							@Valid @ModelAttribute(name = "form") ConsumableProductCreationForm form,
							BindingResult result, Model model, RedirectAttributes attributes) {

		ConsumableProduct drink = (ConsumableProduct) catalogManagement.findById(id);
		String ActionResult = "editFailed";

		if (drink != null) {
			//adding model attributes for redirecting back to editDrink if necessary
			model.addAttribute("productFound", true);
			model.addAttribute("drink", drink);
			model.addAttribute("DrinkID", drink.getId());

			//checking if result has errrors
			if (result.hasErrors()) {
				return "inventory/editDrink";
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
				return "inventory/editDrink";
			}

			//checking if the edit changed anything
			if (catalogManagement.compareConsumable(drink, form)) {
				ActionResult = "editUnchanged";
			}

			//setting edits in Product
			ConsumableProductCreationForm.setProductDetails(form, drink); //static method for setting all details

			//trying to save
			Boolean saveWorked = catalogManagement.trySave(drink); //actual save

			//set ActionResult based on if save worked and if edit changed something
			ActionResult = inventoryValidator.addSaveActionResult(saveWorked, ActionResult);

			//adding attributes to show in frontend if the save worked
			attributes.addFlashAttribute("InventoryActionResult", ActionResult);
			attributes.addFlashAttribute("ProductNameResult", drink.getName());

		} else {
			throw new ResponseStatusException(NOT_FOUND, "Unable to find product for id " + id);
		}

		return "redirect:/inventory";
	}
}
