package papapizza.employee;

import lombok.NonNull;
import lombok.SneakyThrows;
import org.salespointframework.core.DataInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import papapizza.inventory.ShopCatalogManagement;

import java.util.List;

@Component
public class EmployeeDataInitializer implements DataInitializer {

	private final Logger logger = LoggerFactory.getLogger(EmployeeDataInitializer.class);

	private final EmployeeManagement employeeMgmt;

	private ShopCatalogManagement shopCatalogManagement;

	@Autowired
	public EmployeeDataInitializer(@NonNull final EmployeeManagement employeeMgmt) {
		this.employeeMgmt = employeeMgmt;
	}

	@Autowired
	public void setShopCatalogManagement(ShopCatalogManagement shopCatalogManagement) {
		this.shopCatalogManagement = shopCatalogManagement;
	}

	@SneakyThrows
	@Override
	public void initialize() {
		if(!employeeMgmt.findAll().isEmpty()){
			return;
		}

		//vehicles
		shopCatalogManagement.createVehicleProduct("1 Auto", "350", "10");
		shopCatalogManagement.createVehicleProduct("2 Auto", "350", "10");
		shopCatalogManagement.createVehicleProduct("3 Auto", "350", "10");
		shopCatalogManagement.createVehicleProduct("4 Auto", "350", "10");

		List.of(
				new EmployeeCreationForm("boss", "Bob", "Bauer","123","123", "Boss"),
				new EmployeeCreationForm("cashier1","Carlo","Rudolph","123","123","Cashier"),
				new EmployeeCreationForm("chef1", "Andi", "Arbeit","123","123", "Chef"),

				new EmployeeCreationForm("chef2", "Rainer", "Zufall","123","123", "Chef"),
				new EmployeeCreationForm("driver1", "Dr. med Dähn", "Rasen","123","123", "Driver"),
				new EmployeeCreationForm("driver2", "Starsten", "Kahl","123","123", "Driver"),
				//useless accounts
				new EmployeeCreationForm("oldcashier1", "Christoph", "Smaul","123","123", "Cashier"),
				new EmployeeCreationForm("oldcashier2", "Rainer", "Wahnsinn","123","123", "Cashier"),
				new EmployeeCreationForm("oldchef", "Nimse", "Hartmann","123","123", "Chef")
		).forEach(employeeMgmt::createEmployee);

		employeeMgmt.setInitialized(true);
	}
}
