package papapizza.inventory.creationForms;


import lombok.Getter;
import lombok.Setter;
import org.javamoney.moneta.Money;
import papapizza.inventory.items.VehicleProduct;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.text.DecimalFormat;

/**
 * Form for frontend to create {@link VehicleProduct}s.
 */
@Getter
@Setter
public class VehicleProductCreationForm {

	@Size(max = 30, message = "{ShopInventoryProductCreationForm.toLarge.name}")
	@NotBlank(message = "{ShopInventoryProductCreationForm.notEmpty.name}")
	private String name;
	@NotBlank(message = "{ShopInventoryProductCreationForm.notEmpty.price}")
	private String price;
	@NotBlank(message = "{ShopInventoryProductCreationForm.notEmpty.slots}")
	private String slots;

	public VehicleProductCreationForm(String name, String price, String slots){
		this.name = name;
		this.price = price;
		this.slots = slots;
	}

	/**
	 * Method to get the data from the form into the product
	 * @param form contains all the data the user put in (in frontend)
	 * @param product get's updated with the data out of the form
	 */
	public static void setProductDetails(VehicleProductCreationForm form, VehicleProduct product){
		product.setName(form.name);
		String price = form.price.replaceAll(",","."); //for parsing to float
		product.setPrice(Money.of(Float.parseFloat(price), "EUR"));
		product.setSlots(Integer.parseInt(form.slots));
	}
	/**
	 * Method to get the data from the product into the form in order to display them in frontend
	 * @param form get's data from product
	 * @param product saved in {@link papapizza.inventory.ShopCatalog}, contains all data the form needs
	 */
	public static void setFormDetails(VehicleProductCreationForm form, VehicleProduct product){
		form.setName(product.getName());
		DecimalFormat df = new DecimalFormat("#.##");
		form.setPrice(df.format(product.getPrice().getNumber()));
		form.setSlots(Integer.toString(product.getSlots()));

	}

}
