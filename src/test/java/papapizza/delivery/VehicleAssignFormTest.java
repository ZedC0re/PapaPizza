package papapizza.delivery;

import org.junit.jupiter.api.Test;

public class VehicleAssignFormTest {

	@Test
	public void setterAndGetterTest() {
		SettingsForm vaf = new SettingsForm();
		vaf.setManualAssign(true);
		vaf.setVehicleId("Any ID works");
		vaf.getVehicleId();
		vaf.isManualAssign();
	}

}
