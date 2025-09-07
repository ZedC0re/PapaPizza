package papapizza.inventory.items;

import lombok.Getter;
import lombok.Setter;
import org.salespointframework.catalog.Product;
import papapizza.employee.Employee;
import papapizza.inventory.ProductCategory;

import javax.money.MonetaryAmount;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import java.time.Duration;
import java.util.Objects;

/**
 * {@link Product}, that represents a delivery vehicle. <br>
 * It consists of name, price (how much it's worth) and slots (space for {@link Product}s)
 */
@Entity
public class VehicleProduct extends Product {

	enum Meta{
		NO_VEHICLE, NORMAL
	}


	@Getter @Setter
	private int slots;

	@Getter @Setter
	private int usedSlots;

	private Meta meta;


	@Setter
	@Getter
	private boolean inUse;

	@Setter
	@Getter
	private Duration waitingTime;

	@Setter
	@Getter
	private boolean assigned;

	@OneToOne
	private Employee assignedDriver;

	public VehicleProduct(){}

	public VehicleProduct(String name, MonetaryAmount price) {
		super(name, price);
		addCategory(ProductCategory.VEHICLE.toString());
		meta = Meta.NORMAL;
	}

	public void setMeta(String metaString){
		if (Objects.equals(metaString, "NORMAL")){
			this.meta = Meta.NORMAL;
		}else{
			this.meta = Meta.NO_VEHICLE;
		}
	}

	public String getMeta() {
		if(this.meta == Meta.NORMAL){
			return "NORMAL";
		}else{
			return "NO_VEHICLE";
		}
	}



}
