package papapizza.validation;

import org.salespointframework.catalog.Product;
import org.salespointframework.catalog.ProductIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import papapizza.inventory.ProductCategory;
import papapizza.inventory.ShopCatalog;
import papapizza.inventory.ShopCatalogManagement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class for validation methods of inventory frontend forms
 */
@Service
@Transactional
public class InventoryValidator {

	public ShopCatalogManagement catalogManagement;

	@Autowired
	public InventoryValidator(ShopCatalogManagement catalogManagement) {
		this.catalogManagement = catalogManagement;
	}

	/**
	 * when editing a {@link Product}: checks if the {@link Product} is having it's name edited to a {@link Product},
	 * that already exists in the Repository ({@link ShopCatalog})
	 * @param name String with the new name, that is tried to be edited in
	 * @param id {@link ProductIdentifier} of the {@link Product}, that gets edited
	 * @return boolean
	 */
	public boolean checkDoubleNames(String name, ProductIdentifier id){
		Product product = catalogManagement.findByName(name);
		if(product != null) {
			if(Objects.equals(product.getId(), id)) {
				return false;
			}

			//double name if the Category is not "deleted" (soft delete)
			return !product.getCategories().toList().contains(ProductCategory.DELETED.name());

		} else{ //no product in repo with that name
			return false; //no double Name
		}
	}

	/**
	 * when adding a {@link Product}: checks if the {@link Product}, that is tried to be added would have a name,
	 * that is already present in the Repository ({@link ShopCatalog})
	 * @param name String with the name of the Product, that is tried to be added
	 * @return boolean
	 */
	public boolean checkDoubleNames(String name){
		Product product = catalogManagement.findByName(name);
		if(product != null) {
			return !product.getCategories().toList().contains(ProductCategory.DELETED.name()); //double name
		} else{ //no product in repo with that name
			return false; //no double Name
		}
	}

	/**
	 * Method for checking if inputs from frontend form match price regex pattern
	 * => return reject variable for reject value
	 * @param price String from frontend form
	 */
	public boolean validatePrice(String price){
		return !price.matches("[0-9]+((\\.|,)[0-9][0-9]?)?");

	}

	/**
	 * Given a field, that should be rejected in frontend this Method returns the matching error message property
	 * @param field {@link InventoryRejectField}
	 * @return message property, that should be addressed in the ValidationMessages.properties
	 */
	public String resultRejectMatcher(InventoryRejectField field) {
		String returnString;
		switch (field){
			case PRICE: returnString = "ShopInventoryProductCreationForm.wrongPattern.price";
			break;
			case SLOTS: returnString = "ShopInventoryProductCreationForm.wrongPattern.slots";
			break;
			case NAME: returnString = "ShopInventoryProductCreationForm.notUnique.name";
			break;
			default: returnString = "inventory.Product.formErr";
			break;
		}
		return returnString;
	}

	//can't pass the form because the forms are different classes => so I have to overload the method

	/**
	 * Method for validation of forms from inventory <b>add actions frontend</b>
	 * @param name String with name from form for {@link #checkDoubleNames}
	 * @param price String with price from form for {@link #validatePrice}
	 * @return List&lt;{@link InventoryRejectField}s&gt; (List of {@link InventoryRejectField}s,
	 * where the form has errors)
	 */
	public List<InventoryRejectField> formValidator(String name, String price){
		//this needs to be a list, because there could be multiple rejectvalues at once...
		List <InventoryRejectField> resultFieldList = new ArrayList<>();

		//checking if product with the given name already exists in inventory
		if(checkDoubleNames(name)){
			resultFieldList.add(InventoryRejectField.NAME);
		}

		//checking if the given string is actually in the format of a price
		if(validatePrice(price)){
			resultFieldList.add(InventoryRejectField.PRICE);
		}

		return resultFieldList;
	}

	/**
	 * Method for validation of forms from inventory <b>add actions frontend for Vehicles</b>
	 * @param name String with name from form for {@link #checkDoubleNames}
	 * @param price String with price from form for {@link #validatePrice}
	 * @param slots String with vehicle slots from form for validating regex
	 * @return List&lt;{@link InventoryRejectField}s&gt; (List of {@link InventoryRejectField}s,
	 * where the form has errors)
	 */
	public List<InventoryRejectField> formValidator(String name, String price, String slots){
		//this needs to be a list, because there could be multiple rejectvalues at once...
		List <InventoryRejectField> resultFieldList = new ArrayList<>();

		//checking if product with the given name already exists in inventory
		if(checkDoubleNames(name)){
			resultFieldList.add(InventoryRejectField.NAME);
		}

		//checking if the given string is actually in the format of a price
		if(validatePrice(price)){
			resultFieldList.add(InventoryRejectField.PRICE);
		}

		//checking if the given string is a whole number greater than zero
		if(!slots.matches("[1-9][0-9]*")){
			resultFieldList.add(InventoryRejectField.SLOTS);
		}

			return resultFieldList;
	}

	/**
	 * Method for validation of forms from inventory <b>edit actions frontend</b>
	 * @param name String with name from form for {@link #checkDoubleNames}
	 * @param price String with price from form for {@link #validatePrice}
	 * @param id {@link ProductIdentifier} for {@link #checkDoubleNames}
	 * 			to see if the Product with the same name found in Repository ({@link ShopCatalog}) is the product,
	 *			that's been edited
	 * @return List&lt;{@link InventoryRejectField}s&gt; (List of {@link InventoryRejectField}s,
	 * where the form has errors)
	 */
	public List<InventoryRejectField> formValidator(String name, String price, ProductIdentifier id){
		//this needs to be a list, because there could be multiple rejectvalues at once...
		List <InventoryRejectField> resultFieldList = new ArrayList<>();

		//checking if product with the given name already exists in inventory
		if(checkDoubleNames(name, id)){
			resultFieldList.add(InventoryRejectField.NAME);
		}

		//checking if the given string is actually in the format of a price
		if(validatePrice(price)){
			resultFieldList.add(InventoryRejectField.PRICE);
		}

		return resultFieldList;
	}

	/**
	 * Method for validation of forms from inventory <b>add actions frontend for Vehicles</b>
	 * @param name String with name from form for {@link #checkDoubleNames}
	 * @param price String with price from form for {@link #validatePrice}
	 * @param slots String with vehicle slots from form for validating regex
	 * @param id {@link ProductIdentifier} for {@link #checkDoubleNames}
	 * 			to see if the Product with the same name found in Repository ({@link ShopCatalog}) is the product,
	 * 			that's been edited
	 * @return List&lt;{@link InventoryRejectField}s&gt; (List of {@link InventoryRejectField}s,
	 * where the form has errors)
	 */
	public List<InventoryRejectField> formValidator(String name, String price, String slots, ProductIdentifier id) {
		//this needs to be a list, because there could be multiple rejectvalues at once...
		List<InventoryRejectField> resultFieldList = new ArrayList<>();

		//checking if product with the given name already exists in inventory
		if (checkDoubleNames(name, id)) {
			resultFieldList.add(InventoryRejectField.NAME);
		}

		//checking if the given string is actually in the format of a price
		if (validatePrice(price)) {
			resultFieldList.add(InventoryRejectField.PRICE);
		}

		//checking if the given string is a whole number greater than zero
		if (!slots.matches("[1-9][0-9]*")) {
			resultFieldList.add(InventoryRejectField.SLOTS);
		}
		return resultFieldList;
	}

	/**
	 * Method for choosing the correct action result for changes on products, that get later displayed on the inventory
	 * main page in frontend (e.g. when I edit a product and change nothing a message on the "/inventory" mapping tells
	 * me that nothing changed)
	 * @param saveResult boolean to tell if the save worked
	 * @param ActionResult String to tell if something was already changed on the product
	 * @return ActionResult String
	 */
	public String addSaveActionResult(boolean saveResult, String ActionResult){
		if (saveResult && !"editUnchanged".equals(ActionResult)) {
			ActionResult = "editSuccess"; //only set if information changed...
		}
		if (!saveResult) { //set if save failed for whatever reason
			ActionResult = "editFailed";
		}
		return ActionResult;
	}


}
