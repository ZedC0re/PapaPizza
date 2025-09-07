package papapizza.inventory.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import papapizza.delivery.DeliveryManagement;
import papapizza.inventory.ProductCategory;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.inventory.ShopCatalogProductDeleteFailedException;
import papapizza.inventory.items.*;
import papapizza.validation.InventoryValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Main Controller Class for Inventory frontend
 */

@Controller
public class ShopInventoryController {
	private final Logger log = LoggerFactory.getLogger(ShopInventoryController.class);

	private final ShopCatalogManagement catalogManagement;
	private InventoryValidator inventoryValidator;
	private DeliveryManagement deliveryManagement;


	@Autowired
	public ShopInventoryController(ShopCatalogManagement catalogManagement) {

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

	@GetMapping("/inventory")
	@PreAuthorize("hasAnyRole('BOSS')")
	String inventoryPage(Model model, @RequestParam(required = false) Map<String, String> requestParams,
						 @RequestParam(name = "searchText", required = false) String searchText) {
		boolean searchEmpty = false;
		String searchTextNew = searchText; //to ensure, that searchText stays final
		List<Product> products = new ArrayList<>(catalogManagement.findAll().toList());

		log.info(searchText);

		//search => filter out the ones not matching the searched name
		if (!(searchText == null || "".equals(searchText))) {
			products.removeIf(prod -> !StringUtils.containsIgnoreCase(prod.getName(), searchText)); //looks for
			// products with name pattern like search (ignoring case)
			if (products.isEmpty()) {
				searchEmpty = true;
			}
		} else {
			searchTextNew = ""; //set it empty if it's null to prevent Thymeleaf error
		}

		List<PizzaProduct> pizzaPresets =
				products.stream().filter(product -> product.getCategories().toList().contains(
						ProductCategory.PIZZA.toString())).map(p -> (PizzaProduct) p).collect(Collectors.toList());
		List<ConsumableProduct> consumables =
				products.stream().filter(product -> product.getCategories().toList().contains(
						ProductCategory.CONSUMABLE.toString())).map(p -> (ConsumableProduct) p).
						collect(Collectors.toList());
		List<ConsumableProduct> drinks =
				products.stream().filter(product -> product.getCategories().toList().contains(
						ProductCategory.DRINK.toString())).map(p -> (ConsumableProduct) p).collect(Collectors.toList());
		List<ToppingProduct> toppings =
				products.stream().filter(product -> product.getCategories().toList().contains(
						ProductCategory.TOPPING.toString())).map(p -> (ToppingProduct) p).collect(Collectors.toList());
		List<DishsetProduct> dishsets =
				products.stream().filter(product -> product.getCategories().toList().contains(
						ProductCategory.DISHSET.toString())).map(p -> (DishsetProduct) p).collect(Collectors.toList());
		List<VehicleProduct> vehicles =
				products.stream().filter(product -> product.getCategories().toList().contains(
						ProductCategory.VEHICLE.toString())).map(p -> (VehicleProduct) p).collect(Collectors.toList());
		List<OvenProduct> ovens =
				products.stream().filter(product -> product.getCategories().toList().contains(
						ProductCategory.OVEN.toString())).map(p -> (OvenProduct) p).collect(Collectors.toList());

		//DEBUG
		log.info(products.toString());
		log.info(ovens.toString());
		log.info(consumables.toString());
		log.info(pizzaPresets.toString());

		model.addAttribute("searchText", searchTextNew); //do I have to add this again?
		model.addAttribute("searchEmpty", searchEmpty);

		model.addAttribute("pizzaPresets", pizzaPresets);
		model.addAttribute("Consumables", consumables);
		model.addAttribute("Drinks", drinks);
		model.addAttribute("Toppings", toppings);
		model.addAttribute("Dishsets", dishsets);
		model.addAttribute("Vehicles", vehicles);
		model.addAttribute("Ovens", ovens);
		model.addAttribute("Products", products);
		return "inventory/inventory";
	}


	//==================================DELETE=================================

	@PostMapping("/inventory/del/{id}") //forms don't do HTTP-DELETE requests anymore
	@PreAuthorize("hasAnyRole('BOSS')")
	String deleteProduct(@PathVariable ProductIdentifier id, RedirectAttributes attributes) {
		if (catalogManagement.findById(id) != null) {

			//when trying to delete vehicle: check if still in use
			if (catalogManagement.findById(id).getClass().getSimpleName().equals("VehicleProduct") &&
				deliveryManagement.vehicleIsAlreadyAssigned((VehicleProduct) catalogManagement.findById(id))) {
				attributes.addFlashAttribute("InventoryActionResult", "deleteFailed");
				attributes.addFlashAttribute("ProductNameResult", catalogManagement.findById(id).getName());
				return "redirect:/inventory";
			}

			catalogManagement.deleteById(id); //actual delete (remember that this is a soft delete)

			//when trying to delete Oven: check if still in use
			if (catalogManagement.findById(id).getClass().getSimpleName().equals("OvenProduct") &&
				!catalogManagement.findByCategory(ProductCategory.DELETED.toString()).
								  stream().collect(Collectors.toList()).contains(catalogManagement.findById(id))) {
				attributes.addFlashAttribute("InventoryActionResult", "deleteFailed");
			} else {

				//flashAttribute if delete was successful
				attributes.addFlashAttribute("InventoryActionResult", "deleteSuccess");
			}

			//flash product name for delete message for frontend (works because delete is only soft delete)
			attributes.addFlashAttribute("ProductNameResult", catalogManagement.findById(id).getName());

			return "redirect:/inventory";

		} else {
			throw new ShopCatalogProductDeleteFailedException("No product found for id: " + id);
		}
	}


}
