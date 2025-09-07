package papapizza.inventory;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.salespointframework.catalog.Product;
import org.salespointframework.catalog.ProductIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import papapizza.delivery.DeliveryManagement;
import papapizza.inventory.creationForms.*;
import papapizza.inventory.items.*;


import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;


//FIXME needs to be split in to seperate classes because it is to large?
/**
 * Controller Class for Inventory frontend
 */

@Controller
public class ShopInventoryController {
	private final Logger LOG = LoggerFactory.getLogger(ShopInventoryController.class);

	private final ShopCatalogManagement catalogManagement;
	private DeliveryManagement deliveryManagement;

	@Autowired
	public void setDeliveryManagement(@NonNull DeliveryManagement deliveryManagement){
		this.deliveryManagement = deliveryManagement;
	}

	@Autowired
	public ShopInventoryController(ShopCatalogManagement catalogManagement) {
		this.catalogManagement = catalogManagement;
	}

	@GetMapping("/inventory")
	@PreAuthorize("hasAnyRole('BOSS')")
	String inventoryPage(Model model, @RequestParam(required = false) Map<String, String> requestParams, @RequestParam(name = "searchText", required = false) String searchText) {
		boolean searchEmpty = false;
		String searchTextNew = searchText; //to ensure, that searchText stays final
		List<Product> products = new ArrayList<>(catalogManagement.findAll().toList());

		LOG.info(searchText);

		//search => filter out the ones not matching the searched name
		if(!(searchText == null || "".equals(searchText))){
			products.removeIf(prod -> !StringUtils.containsIgnoreCase(prod.getName(), searchText)); //looks for products with name pattern like search (ignoring case)
			if(products.isEmpty()){searchEmpty = true;}
		} else {
			searchTextNew = ""; //set it empty if it's null to prevent Thymeleaf error
		}
		/*
		List<PizzaProduct> pizzaPresets = catalogManagement.findByCategory("Pizza").stream().map(product -> (PizzaProduct) product).collect(Collectors.toList()); //I literally have no words for this...
		List<ConsumableProduct> Consumables = catalogManagement.findByCategory("Consumable").stream().map(product -> (ConsumableProduct) product).collect(Collectors.toList());
		List<ConsumableProduct> Drinks = catalogManagement.findByCategory("Drink").stream().map(product -> (ConsumableProduct) product).collect(Collectors.toList());
		List<ToppingProduct> Toppings = catalogManagement.findByCategory("Topping").stream().map(product -> (ToppingProduct) product).collect(Collectors.toList());
		List<DishsetProduct> Dishsets = catalogManagement.findByCategory("Dishset").stream().map(product -> (DishsetProduct) product).collect(Collectors.toList());
		List<VehicleProduct> Vehicles = catalogManagement.findByCategory("Vehicle").stream().map(product -> (VehicleProduct) product).collect(Collectors.toList());
		List<OvenProduct> Ovens = catalogManagement.findByCategory("Oven").stream().map(product -> (OvenProduct) product).collect(Collectors.toList());
 		*/
		List<PizzaProduct> pizzaPresets = products.stream().filter(product -> product.getCategories().toList().contains(ProductCategory.Pizza.toString())).map(p -> (PizzaProduct) p).collect(Collectors.toList());
		List<ConsumableProduct> Consumables = products.stream().filter(product -> product.getCategories().toList().contains(ProductCategory.Consumable.toString())).map(p -> (ConsumableProduct) p).collect(Collectors.toList());
		List<ConsumableProduct> Drinks = products.stream().filter(product -> product.getCategories().toList().contains(ProductCategory.Drink.toString())).map(p -> (ConsumableProduct) p).collect(Collectors.toList());
		List<ToppingProduct> Toppings = products.stream().filter(product -> product.getCategories().toList().contains(ProductCategory.Topping.toString())).map(p -> (ToppingProduct) p).collect(Collectors.toList());
		List<DishsetProduct> Dishsets = products.stream().filter(product -> product.getCategories().toList().contains(ProductCategory.Dishset.toString())).map(p -> (DishsetProduct) p).collect(Collectors.toList());
		List<VehicleProduct> Vehicles = products.stream().filter(product -> product.getCategories().toList().contains(ProductCategory.Vehicle.toString())).map(p -> (VehicleProduct) p).collect(Collectors.toList());
		List<OvenProduct> Ovens = products.stream().filter(product -> product.getCategories().toList().contains(ProductCategory.Oven.toString())).map(p -> (OvenProduct) p).collect(Collectors.toList());

		//DEBUG
		LOG.info(products.toString());
		LOG.info(Ovens.toString());
		LOG.info(Consumables.toString());
		LOG.info(pizzaPresets.toString());

		model.addAttribute("searchText",searchTextNew); //do I have to add this again?
		model.addAttribute("searchEmpty",searchEmpty);

		model.addAttribute("pizzaPresets", pizzaPresets);
		model.addAttribute("Consumables", Consumables);
		model.addAttribute("Drinks", Drinks);
		model.addAttribute("Toppings", Toppings);
		model.addAttribute("Dishsets", Dishsets);
		model.addAttribute("Vehicles", Vehicles);
		model.addAttribute("Ovens", Ovens);
		model.addAttribute("Products", products);
		return "inventory/inventory";
	}

	// sorting?


	//==================================ADDs=================================


	@GetMapping("/addPizzaPreset")
	@PreAuthorize("hasAnyRole('BOSS')")
	String addPizzaPreset(@ModelAttribute("form") PizzaProductCreationForm form,
						  Model model) {
		//Map<ToppingProduct, Boolean> Toppings = catalogManagement.findbyCategory("Topping").stream().map(product -> (ToppingProduct) product).collect(Collectors.toMap(product -> product, product -> false));
		List<ToppingProduct> toppings = catalogManagement.findByCategory("Topping").stream().map(product -> (ToppingProduct) product).collect(Collectors.toList());
		//model.addAttribute("Toppings", Toppings);
		PizzaProductCreationForm.setFormDetails(form, toppings);
		return "inventory/addPizzaPreset";
	}

	@PostMapping("/addPizzaPreset")
	String addPizzaPreset(@Valid @ModelAttribute("form") PizzaProductCreationForm form, BindingResult result, RedirectAttributes attributes) {
		if (result.hasErrors()) {
			return "inventory/addPizzaPreset";
		}
		if(catalogManagement.checkDoubleNames(form.getName())){
			result.rejectValue("name", "ShopInventoryProductCreationForm.notUnique.name", "name must be unique");
			return "inventory/addPizzaPreset";
		}

		//checking if the given string is actually in the format of a price
		if (!form.getPrice().matches("[0-9]+((\\.|,)[0-9][0-9]?)?")){
			result.rejectValue("price","ShopInventoryProductCreationForm.wrongPattern.price",
					"price has to have a maximum of two decimal places (separated by dot or comma) and no chars or special characters");
			return "inventory/addPizzaPreset";
		}

		LOG.info("List of Pizza Toppings: " + catalogManagement.ToppingMapToList(form)); //debug
		catalogManagement.createPizzaProduct(form.getName(), form.getPrice(), catalogManagement.ToppingMapToList(form)); //see method above (aditional Methods in Management)
		attributes.addFlashAttribute("InventoryActionResult","newSuccess");
		attributes.addFlashAttribute("ProductNameResult", form.getName());
		return "redirect:/inventory";
	}


	@GetMapping("/addOven")
	@PreAuthorize("hasAnyRole('BOSS')")
	String addOven(@ModelAttribute("form") OvenProductCreationForm form) {
		return "inventory/addOven";
	}

	@PostMapping("/addOven")
	String addOven(@Valid @ModelAttribute("form") OvenProductCreationForm form, BindingResult result, RedirectAttributes attributes) {
		if (result.hasErrors()) {
			return "inventory/addOven";
		}

		if (catalogManagement.checkDoubleNames(form.getName())) { // if double name => rejectValue
			result.rejectValue("name", "ShopInventoryProductCreationForm.notUnique.name", "name must be unique");
			return "inventory/editOven";
		}

		//checking if the given string is actually in the format of a price
		if (!form.getPrice().matches("[0-9]+((\\.|,)[0-9][0-9]?)?")){
			result.rejectValue("price","ShopInventoryProductCreationForm.wrongPattern.price",
					"price has to have a maximum of two decimal places (separated by dot or comma) and no chars or special characters");
			return "inventory/addOven";
		}

		OvenProduct oven = catalogManagement.createOvenProduct(form.getName(), form.getPrice());
		attributes.addFlashAttribute("InventoryActionResult","newSuccess");
		attributes.addFlashAttribute("ProductNameResult", oven.getName());
		return "redirect:/inventory";
	}


	@GetMapping("/addTopping")
	@PreAuthorize("hasAnyRole('BOSS')")
	String addTopping(@ModelAttribute("form") ToppingProductCreationForm form) {
		return "inventory/addTopping";
	}

	@PostMapping("/addTopping")
	String addTopping(@Valid @ModelAttribute("form") ToppingProductCreationForm form, BindingResult result, RedirectAttributes attributes) {
		if (result.hasErrors()) {
			return "inventory/addTopping";
		}

		if(catalogManagement.checkDoubleNames(form.getName())){
			result.rejectValue("name", "ShopInventoryProductCreationForm.notUnique.name", "name must be unique");
			return "inventory/addTopping";
		}

		//checking if the given string is actually in the format of a price
		if (!form.getPrice().matches("[0-9]+((\\.|,)[0-9][0-9]?)?")){
			result.rejectValue("price","ShopInventoryProductCreationForm.wrongPattern.price",
					"price has to have a maximum of two decimal places (separated by dot or comma) and no chars or special characters");
			return "inventory/addTopping";
		}

		catalogManagement.createToppingProduct(form.getName(), form.getPrice());
		attributes.addFlashAttribute("InventoryActionResult","newSuccess");
		attributes.addFlashAttribute("ProductNameResult", form.getName());
		return "redirect:/inventory";
	}


	@GetMapping("/addDishset")
	@PreAuthorize("hasAnyRole('BOSS')")
	String addDishset(@ModelAttribute("form") DishsetProductCreationForm form) {
		return "inventory/addDishset";
	}

	@PostMapping("/addDishset")
	String addDishset(@Valid @ModelAttribute("form") DishsetProductCreationForm form, BindingResult result, RedirectAttributes attributes) {
		if (result.hasErrors()) {
			return "inventory/addDishset";
		}
		if(catalogManagement.checkDoubleNames(form.getName())){
			result.rejectValue("name", "ShopInventoryProductCreationForm.notUnique.name", "name must be unique");
			return "inventory/addDishset";
		}

		//checking if the given string is actually in the format of a price
		if (!form.getPrice().matches("[0-9]+((\\.|,)[0-9][0-9]?)?")){
			result.rejectValue("price","ShopInventoryProductCreationForm.wrongPattern.price",
					"price has to have a maximum of two decimal places (separated by dot or comma) and no chars or special characters");
			return "inventory/addDishset";
		}

		catalogManagement.createDishsetProduct(form.getName(), form.getPrice());
		attributes.addFlashAttribute("InventoryActionResult","newSuccess");
		attributes.addFlashAttribute("ProductNameResult", form.getName());
		return "redirect:/inventory";
	}


	@GetMapping("/addVehicle")
	@PreAuthorize("hasAnyRole('BOSS')")
	String addVehicle(@ModelAttribute("form") VehicleProductCreationForm form) {
		return "inventory/addVehicle";
	}

	@PostMapping("/addVehicle")
	String addVehicle(@Valid @ModelAttribute("form") VehicleProductCreationForm form, BindingResult result, RedirectAttributes attributes) {
		if (result.hasErrors()) {
			return "inventory/addVehicle";
		}
		if(catalogManagement.checkDoubleNames(form.getName())){
			result.rejectValue("name", "ShopInventoryProductCreationForm.notUnique.name", "name must be unique");
			return "inventory/addVehicle";
		}

		//checking if the given string is actually in the format of a price
		if (!form.getPrice().matches("[0-9]+((\\.|,)[0-9][0-9]?)?")){
			result.rejectValue("price","ShopInventoryProductCreationForm.wrongPattern.price",
					"price has to have a maximum of two decimal places (separated by dot or comma) and no chars or special characters");
			return "inventory/addVehicle";
		}

		catalogManagement.createVehicleProduct(form.getName(), form.getPrice(), form.getSlots());
		attributes.addFlashAttribute("InventoryActionResult","newSuccess");
		attributes.addFlashAttribute("ProductNameResult", form.getName());
		return "redirect:/inventory";
	}


	@GetMapping("/addConsumable")
	@PreAuthorize("hasAnyRole('BOSS')")
	String addConsumable(@ModelAttribute("form") ConsumableProductCreationForm form) {
		return "inventory/addConsumable";
	}

	@PostMapping("/addConsumable")
	String addConsumable(@Valid @ModelAttribute("form") ConsumableProductCreationForm form, BindingResult result, RedirectAttributes attributes) {
		if (result.hasErrors()) {
			return "inventory/addConsumable";
		}
		if(catalogManagement.checkDoubleNames(form.getName())){
			result.rejectValue("name", "ShopInventoryProductCreationForm.notUnique.name", "name must be unique");
			return "inventory/addConsumable";
		}

		//checking if the given string is actually in the format of a price
		if (!form.getPrice().matches("[0-9]+((\\.|,)[0-9][0-9]?)?")){
			result.rejectValue("price","ShopInventoryProductCreationForm.wrongPattern.price",
					"price has to have a maximum of two decimal places (separated by dot or comma) and no chars or special characters");
			return "inventory/addConsumable";
		}

		catalogManagement.createConsumableProduct(form.getName(), form.getPrice(), form.getIngredients());
		attributes.addFlashAttribute("InventoryActionResult","newSuccess");
		attributes.addFlashAttribute("ProductNameResult", form.getName());
		return "redirect:/inventory";
	}

	@GetMapping("/addDrink")
	@PreAuthorize("hasAnyRole('BOSS')")
	String addDrink(@ModelAttribute("form") ConsumableProductCreationForm form) {
		return "inventory/addDrink";
	}

	@PostMapping("/addDrink")
	String addDrink(@Valid @ModelAttribute("form") ConsumableProductCreationForm form, BindingResult result, RedirectAttributes attributes) {
		if (result.hasErrors()) {
			return "inventory/addDrink";
		}
		if(catalogManagement.checkDoubleNames(form.getName())){
			result.rejectValue("name", "ShopInventoryProductCreationForm.notUnique.name", "name must be unique");
			return "inventory/addDrink";
		}

		//checking if the given string is actually in the format of a price
		if (!form.getPrice().matches("[0-9]+((\\.|,)[0-9][0-9]?)?")){
			result.rejectValue("price","ShopInventoryProductCreationForm.wrongPattern.price",
					"price has to have a maximum of two decimal places (separated by dot or comma) and no chars or special characters");
			return "inventory/addDrink";
		}

		catalogManagement.createDrinkProduct(form.getName(), form.getPrice(), form.getIngredients());
		attributes.addFlashAttribute("InventoryActionResult","newSuccess");
		attributes.addFlashAttribute("ProductNameResult", form.getName());
		return "redirect:/inventory";
	}


	//==================================DELETE=================================

	@PostMapping("/inventory/del/{id}") //forms don't do HTTP-DELETE requests anymore
	@PreAuthorize("hasAnyRole('BOSS')")
	String deleteProduct(@PathVariable ProductIdentifier id, RedirectAttributes attributes) {
		if (catalogManagement.findById(id) != null) {

			if(catalogManagement.findById(id).getClass().getSimpleName().equals("VehicleProduct") && deliveryManagement.vehicleIsAlreadyAssigned((VehicleProduct) catalogManagement.findById(id))){ // to lower the amount of conditions in one if statement
				attributes.addFlashAttribute("InventoryActionResult","deleteFailed");
				attributes.addFlashAttribute("ProductNameResult", catalogManagement.findById(id).getName());
				return "redirect:/inventory";
			}

			catalogManagement.deleteById(id); //remember that this is a soft delete
			if(catalogManagement.findById(id).getClass().getSimpleName().equals("OvenProduct") && !catalogManagement.findByCategory(ProductCategory.Deleted.toString()).stream().collect(Collectors.toList()).contains(catalogManagement.findById(id))){
				attributes.addFlashAttribute("InventoryActionResult","deleteFailed");
			} else {
				attributes.addFlashAttribute("InventoryActionResult", "deleteSuccess");
			}

			attributes.addFlashAttribute("ProductNameResult", catalogManagement.findById(id).getName()); //works because delete is only soft delete
			return "redirect:/inventory";

		} else {
			throw new RuntimeException("No product found for id: " + id);
		}
	}


	//==================================EDITS=================================


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
			model.addAttribute("pizzaPreset", pizza);
			model.addAttribute("pizzaPresetID", pizza.getId());
			//if not coming from redirect -> set form details, otherwise flash attribute will alr be set
			//if(form.getPrice() == null) {
			List<ToppingProduct> toppings = catalogManagement.findByCategory("Topping").stream().map(product -> (ToppingProduct) product).collect(Collectors.toList());
			PizzaProductCreationForm.setFormDetails(form, pizza, toppings);
			//}
		}
		return "inventory/editPizzaPreset";
	}

	@PostMapping("/inventory/editPizzaPreset/{id}")
	@PreAuthorize("hasAnyRole('BOSS')")
	String editPizzaProduct(@PathVariable ProductIdentifier id,
			@Valid @ModelAttribute(name = "form") PizzaProductCreationForm form, BindingResult result, Model model,
			/*@RequestBody Map<String, String > request */
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

			//checking if Product with this name is already in Repo
			if (catalogManagement.checkDoubleNames(form.getName(), id)) { // if double name => rejectValue
				result.rejectValue("name", "ShopInventoryProductCreationForm.notUnique.name", "name must be unique");
				return "inventory/editPizzaPreset";
			}

			//checking if the given string is actually in the format of a price
			if (!form.getPrice().matches("[0-9]+((\\.|,)[0-9][0-9]?)?")){
				result.rejectValue("price","ShopInventoryProductCreationForm.wrongPattern.price",
						"price has to have a maximum of two decimal places (separated by dot or comma) and no chars or special characters");
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
			if (saveWorked && !"editUnchanged".equals(ActionResult)) {
				ActionResult = "editSuccess"; //only set if information changed...
			}
			if(!saveWorked){ //set if save failed for whatever reason
				ActionResult = "editFailed";
			}

			//adding attributes to show in frontend if the save worked
			attributes.addFlashAttribute("InventoryActionResult", ActionResult);
			attributes.addFlashAttribute("ProductNameResult",pizza.getName());


		} else {
			throw new ResponseStatusException(NOT_FOUND, "Unable to find product for id " + id);
		}

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
		model.addAttribute("productFound", dishset != null); //can it lead to complications if they are all named "productFound"?
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

			//checking if Product with this name is already in Repo
			if (catalogManagement.checkDoubleNames(form.getName(), id)) { // if double name => rejectValue
				result.rejectValue("name", "ShopInventoryProductCreationForm.notUnique.name", "name must be unique");
				return "inventory/editDishset";
			}

			//checking if the given string is actually in the format of a price
			if (!form.getPrice().matches("[0-9]+((\\.|,)[0-9][0-9]?)?")){
				result.rejectValue("price","ShopInventoryProductCreationForm.wrongPattern.price",
						"price has to have a maximum of two decimal places (separated by dot or comma) and no chars or special characters");
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
			if (saveWorked && !"editUnchanged".equals(ActionResult)) {
				ActionResult = "editSuccess"; //only set if information changed...
			}
			if(!saveWorked){ //set if save failed for whatever reason
				ActionResult = "editFailed";
			}

			//adding attributes to show in frontend if the save worked
			attributes.addFlashAttribute("InventoryActionResult", ActionResult);
			attributes.addFlashAttribute("ProductNameResult",dishset.getName());

		} else {
			throw new ResponseStatusException(NOT_FOUND, "Unable to find product for id " + id);
		}

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
		model.addAttribute("productFound", consumable != null); //can it lead to complications if they are all named "productFound"?
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

			//checking if Product with this name is already in Repo
			if (catalogManagement.checkDoubleNames(form.getName(), id)) { // if double name => rejectValue
				result.rejectValue("name", "ShopInventoryProductCreationForm.notUnique.name", "name must be unique");
				return "inventory/editConsumable";
			}

			//checking if the given string is actually in the format of a price
			if (!form.getPrice().matches("[0-9]+((\\.|,)[0-9][0-9]?)?")){
				result.rejectValue("price","ShopInventoryProductCreationForm.wrongPattern.price",
						"price has to have a maximum of two decimal places (separated by dot or comma) and no chars or special characters");
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
			if (saveWorked && !"editUnchanged".equals(ActionResult)) {
				ActionResult = "editSuccess"; //only set if information changed...
			}
			if(!saveWorked){ //set if save failed for whatever reason
				ActionResult = "editFailed";
			}

			//adding attributes to show in frontend if the save worked
			attributes.addFlashAttribute("InventoryActionResult", ActionResult);
			attributes.addFlashAttribute("ProductNameResult",consumable.getName());

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
		model.addAttribute("productFound", drink != null); //can it lead to complications if they are all named "productFound"?
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

			//checking if Product with this name is already in Repo
			if (catalogManagement.checkDoubleNames(form.getName(), id)) { // if double name => rejectValue
				result.rejectValue("name", "ShopInventoryProductCreationForm.notUnique.name", "name must be unique");
				return "inventory/editDrink";
			}

			//checking if the given string is actually in the format of a price
			if (!form.getPrice().matches("[0-9]+((\\.|,)[0-9][0-9]?)?")){
				result.rejectValue("price","ShopInventoryProductCreationForm.wrongPattern.price",
						"price has to have a maximum of two decimal places (separated by dot or comma) and no chars or special characters");
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
			if (saveWorked && !"editUnchanged".equals(ActionResult)) {
					 ActionResult = "editSuccess"; //only set if information changed...
			}
			if(!saveWorked){ //set if save failed for whatever reason
				ActionResult = "editFailed";
			}

			//adding attributes to show in frontend if the save worked
			attributes.addFlashAttribute("InventoryActionResult", ActionResult);
			attributes.addFlashAttribute("ProductNameResult",drink.getName());

		} else {
			throw new ResponseStatusException(NOT_FOUND, "Unable to find product for id " + id);
		}

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
		model.addAttribute("productFound", vehicle != null); //can it lead to complications if they are all named "productFound"?
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

			//checking if Product with this name is already in Repo
			if (catalogManagement.checkDoubleNames(form.getName(), id)) { // if double name => rejectValue
				result.rejectValue("name", "ShopInventoryProductCreationForm.notUnique.name", "name must be unique");
				return "inventory/editVehicle";
			}

			//checking if the given string is actually in the format of a price
			if (!form.getPrice().matches("[0-9]+((\\.|,)[0-9][0-9]?)?")){
				result.rejectValue("price","ShopInventoryProductCreationForm.wrongPattern.price",
						"price has to have a maximum of two decimal places (separated by dot or comma) and no chars or special characters");
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
			if (saveWorked && !"editUnchanged".equals(ActionResult)) {
				ActionResult = "editSuccess"; //only set if information changed...
			}
			if(!saveWorked){ //set if save failed for whatever reason
				ActionResult = "editFailed";
			}

			//adding attributes to show in frontend if the save worked
			attributes.addFlashAttribute("InventoryActionResult", ActionResult);
			attributes.addFlashAttribute("ProductNameResult",vehicle.getName());

		} else {
			throw new ResponseStatusException(NOT_FOUND, "Unable to find product for id " + id);
		}

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
		model.addAttribute("productFound", oven != null); //can it lead to complications if they are all named "productFound"?
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

			if (catalogManagement.checkDoubleNames(form.getName(), id)) { // if double name => rejectValue
				result.rejectValue("name", "ShopInventoryProductCreationForm.notUnique.name", "name must be unique");
				return "inventory/editOven";
			}

			//checking if the given string is actually in the format of a price
			if (!form.getPrice().matches("[0-9]+((\\.|,)[0-9][0-9]?)?")){
				result.rejectValue("price","ShopInventoryProductCreationForm.wrongPattern.price",
						"price has to have a maximum of two decimal places (separated by dot or comma) and no chars or special characters");
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
			if (saveWorked && !"editUnchanged".equals(ActionResult)) {
				ActionResult = "editSuccess"; //only set if information changed...
			}
			if(!saveWorked){ //set if save failed for whatever reason
				ActionResult = "editFailed";
			}

			//adding attributes to show in frontend if the save worked
			attributes.addFlashAttribute("InventoryActionResult", ActionResult);
			attributes.addFlashAttribute("ProductNameResult",oven.getName());

		} else {
			throw new ResponseStatusException(NOT_FOUND, "Unable to find product for id " + id);
		}

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
		model.addAttribute("productFound", topping != null); //can it lead to complications if they are all named "productFound"?
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

			//checking if Product with this name is already in Repo
			if (catalogManagement.checkDoubleNames(form.getName(), id)) { // if double name => rejectValue
				result.rejectValue("name", "ShopInventoryProductCreationForm.notUnique.name", "name must be unique");
				return "inventory/editTopping";
			}

			//checking if the given string is actually in the format of a price
			if (!form.getPrice().matches("[0-9]+((\\.|,)[0-9][0-9]?)?")){
				result.rejectValue("price","ShopInventoryProductCreationForm.wrongPattern.price",
						"price has to have a maximum of two decimal places (separated by dot or comma) and no chars or special characters");
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
			if (saveWorked && !"editUnchanged".equals(ActionResult)) {
				ActionResult = "editSuccess"; //only set if information changed...
			}
			if(!saveWorked){ //set if save failed for whatever reason
				ActionResult = "editFailed";
			}

			//adding attributes to show in frontend if the save worked
			attributes.addFlashAttribute("InventoryActionResult", ActionResult);
			attributes.addFlashAttribute("ProductNameResult",topping.getName());

		} else {
			throw new ResponseStatusException(NOT_FOUND, "Unable to find product for id " + id);
		}

		return "redirect:/inventory";
	}


}
