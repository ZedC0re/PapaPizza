package papapizza.delivery;

import lombok.Getter;
import lombok.Setter;

/**
 * Class for storing data necessary for vehicle reassignment<br>
 * Also stores information about the currently used delivery mode and the use of automatic/manual vehicle assignment<br>
 * Used by the {@link DeliveryController}
 */
public class SettingsForm {
	/**
	 * The automatically generated, unique ID of a {@link papapizza.inventory.items.VehicleProduct}
	 */
	@Setter
	@Getter
	private String vehicleId;
	/**
	 * A boolean indicating whether a {@link papapizza.inventory.items.VehicleProduct} is currently automatically assigned upon
	 * Employee, with driver role, creation
	 */
	@Getter
	@Setter
	private boolean manualAssign;
	/**
	 * A boolean indicating the current delivery mode<br>
	 * States: true = capacity, false = speed
	 */
	@Setter
	@Getter
	private boolean orderAssign;

}
