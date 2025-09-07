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
import papapizza.inventory.creationForms.VehicleProductCreationForm;
import papapizza.inventory.items.VehicleProduct;
import papapizza.validation.InventoryRejectField;
import papapizza.validation.InventoryValidator;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Controller Class for {@link VehicleProduct} for Inventory frontend
 */
@Controller
public class ShopCatalogVehicleProductController {
	private final ShopCatalogManagement catalogManagement;
	private InventoryValidator inventoryValidator;
	private DeliveryManagement deliveryManagement;


	@Autowired
	public ShopCatalogVehicleProductController(ShopCatalogManagement catalogManagement) {
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

	@GetMapping("/addVehicle")
	@PreAuthorize("hasAnyRole('BOSS')")
	String addVehicle(@ModelAttribute("form") VehicleProductCreationForm form) {

		return "inventory/addVehicle";
	}

	@PostMapping("/addVehicle")
	String addVehicle(@Valid @ModelAttribute("form") VehicleProductCreationForm form, BindingResult result,
					  RedirectAttributes attributes) {
		if (result.hasErrors()) {
			return "inventory/addVehicle";
		}
		//validation of the form
		List<InventoryRejectField> resultFieldList = new ArrayList<>();
		resultFieldList.addAll(inventoryValidator.formValidator(form.getName(), form.getPrice(), form.getSlots()));

		// adding rejectValues if form is invalid
		if (resultFieldList.size() > 0) {
			for (int i = 0; i < resultFieldList.size(); i++) {
				result.rejectValue(resultFieldList.get(i).toString().toLowerCase(),
								   inventoryValidator.resultRejectMatcher(resultFieldList.get(i)),
								   "an error occured");
			}
			return "inventory/addVehicle";
		}


		//actual creation of the product
		catalogManagement.createVehicleProduct(form.getName(), form.getPrice(), form.getSlots());

		//flash attributes for displaying creation of product
		attributes.addFlashAttribute("InventoryActionResult", "newSuccess");
		attributes.addFlashAttribute("ProductNameResult", form.getName());
		return "redirect:/inventory";
	}

	//
	//editVehicle
	//

	@GetMapping("/inventory/editVehicle/{id}")
	@PreAuthorize("hasAnyRole('BOSS')")
	String editVehicle(@PathVariable ProductIdentifier id,
					   @ModelAttribute("form") VehicleProductCreationForm form,
					   Model model) {
		VehicleProduct vehicle = (VehicleProduct) catalogManagement.findById(id);
		//boolean for thymeleaf to display error message if the product with this id does not exist
		model.addAttribute("productFound", vehicle != null);
		//add product to model if present
		if (vehicle != null) {
			model.addAttribute("vehicle", vehicle);
			model.addAttribute("VehicleID", vehicle.getId());

			VehicleProductCreationForm.setFormDetails(form, vehicle);

		}

		return "inventory/editVehicle";
	}


	@PostMapping(value = "/inventory/editVehicle/{id}")
	@PreAuthorize("hasAnyRole('BOSS')")
	String editVehicleProduct(@PathVariable ProductIdentifier id,
							  @Valid @ModelAttribute(name = "form") VehicleProductCreationForm form,
							  BindingResult result, Model model, RedirectAttributes attributes) {

		VehicleProduct vehicle = (VehicleProduct) catalogManagement.findById(id);
		String ActionResult = "editFailed";

		if (vehicle != null) {
			//adding model attributes for redirecting back to editVehicle if necessary
			model.addAttribute("productFound", true);
			model.addAttribute("vehicle", vehicle);
			model.addAttribute("VehicleID", vehicle.getId());

			//checking if result has errrors
			if (result.hasErrors()) {
				return "inventory/editVehicle";
			}

			//validation of the form
			List<InventoryRejectField> resultFieldList = new ArrayList<>();
			resultFieldList.addAll(inventoryValidator.formValidator(form.getName(), form.getPrice(), form.getSlots(),
																	id));

			// adding rejectValues if form is invalid
			if (resultFieldList.size() > 0) {
				for (int i = 0; i < resultFieldList.size(); i++) {
					result.rejectValue(resultFieldList.get(i).toString().toLowerCase(),
									   inventoryValidator.resultRejectMatcher(resultFieldList.get(i)),
									   "an error occured");
				}
				return "inventory/editVehicle";
			}


			//checking if the edit changed anything
			if (catalogManagement.compareVehicle(vehicle, form)) {
				ActionResult = "editUnchanged";
			}

			//setting edits in Product
			VehicleProductCreationForm.setProductDetails(form, vehicle); //static method for setting all details

			//trying to save
			Boolean saveWorked = catalogManagement.trySave(vehicle); //actual save

			//set ActionResult based on if save worked and if edit changed something
			ActionResult = inventoryValidator.addSaveActionResult(saveWorked, ActionResult);

			//adding attributes to show in frontend if the save worked
			attributes.addFlashAttribute("InventoryActionResult", ActionResult);
			attributes.addFlashAttribute("ProductNameResult", vehicle.getName());

		} else {
			throw new ResponseStatusException(NOT_FOUND, "Unable to find product for id " + id);
		}

		return "redirect:/inventory";
	}

}
