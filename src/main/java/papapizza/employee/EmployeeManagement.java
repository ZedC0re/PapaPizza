package papapizza.employee;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.salespointframework.useraccount.Password;
import org.salespointframework.useraccount.Role;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.UserAccountManagement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import papapizza.app.exc.PapaPizzaRunException;
import papapizza.delivery.DeliveryManagement;
import papapizza.inventory.ProductCategory;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.inventory.items.OvenProduct;
import papapizza.order.ShopOrder;
import papapizza.order.ShopOrderManagement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmployeeManagement {

	@Getter
	@Setter
	private boolean initialized = false;

	public static final Role CASHIER = Role.of("CASHIER");
	public static final Role CHEF = Role.of("CHEF");
	public static final Role DRIVER = Role.of("DRIVER");
	public static final Role BOSS = Role.of("BOSS");
	private static final String DUMMY_ACCOUNT_NAME = "DUMMY_ACCOUNT";

	private final EmployeeRepository employeeRepo;
	private final UserAccountManagement userAccountMgmt;
	private ShopOrderManagement<ShopOrder> shopOrderManagement;
	private ShopCatalogManagement catalogMgmt;

	private final Employee deleteLinkEmployee;
	private final UserAccount dummyAccount;

	private DeliveryManagement deliveryManagement;


	@Autowired
	public EmployeeManagement(@NonNull final EmployeeRepository employeeRepo,
							  @Qualifier("persistentUserAccountManagement") @NonNull final UserAccountManagement userAccountMgmt) {
		this.employeeRepo = employeeRepo;
		this.userAccountMgmt = userAccountMgmt;

		//create a meta employee to map all database references to once the actual employee gets deleted
		deleteLinkEmployee = createDeleteLinkEmployee();
		dummyAccount = createDummyAccount();
	}

	@Autowired
	public void setShopOrderManagement(@NonNull ShopOrderManagement<ShopOrder> shopOrderManagement) {
		this.shopOrderManagement = shopOrderManagement;
	}

	@Autowired
	public void setDeliveryManagement(@NonNull DeliveryManagement deliveryManagement) {
		this.deliveryManagement = deliveryManagement;
	}

	@Autowired
	public void setShopCatalogManagement(ShopCatalogManagement catalogMgmt) {
		this.catalogMgmt = catalogMgmt;
	}

	private Employee createDeleteLinkEmployee() {
		//meta employee does alr exist, i.e. loaded from file
		if (employeeRepo.findByMeta(Employee.Meta.DELETE_LINK_EMPLOYEE).size() != 0) {
			return employeeRepo.findByMeta(Employee.Meta.DELETE_LINK_EMPLOYEE).get(0);
		}
		Employee deleteLinkEm = new Employee();
		deleteLinkEm.setMeta(Employee.Meta.DELETE_LINK_EMPLOYEE);
		return this.employeeRepo.save(deleteLinkEm);
	}

	private UserAccount createDummyAccount() {
		if (userAccountMgmt.findByUsername(DUMMY_ACCOUNT_NAME).isPresent()) {
			return userAccountMgmt.findByUsername(DUMMY_ACCOUNT_NAME).get();
		}
		UserAccount dummy = createAccount(DUMMY_ACCOUNT_NAME,
				Password.UnencryptedPassword.of(DUMMY_ACCOUNT_NAME), Role.of("DENY"));
		dummy.setEnabled(false);
		userAccountMgmt.save(dummy);
		return dummy;
	}

	/**
	 * Gives the Meta.DELETE_LINK_EMPLOYEE <br>
	 * first of the list, if there are for any reason multiple in the db
	 *
	 * @return Employee
	 */
	public Employee getDeleteLinkEmployee() {
		return deleteLinkEmployee;
	}

	public UserAccount getDummyAccount() {
		return dummyAccount;
	}

	/**
	 * Creates a new Employee with userAccount based on an {@link EmployeeCreationForm}
	 *
	 * @param form to create with
	 * @return the created employee
	 */
	public Employee createEmployee(@NonNull EmployeeCreationForm form) {
		if (form.getName().equals("DUMMY_ACCOUNT")) {
			return null;
		}
		UserAccount userAccount = userAccountMgmt.create(form.getName(),
				Password.UnencryptedPassword.of(form.getPassword()), stringToRole(form.getRole()));
		Employee employee = new Employee(userAccount);
		if (employee.getRole().equals("Driver")) {
			deliveryManagement.assignVehicle(employee);
		}
		employee.setFirstname(form.getFirstname());
		employee.setLastname(form.getLastname());

		employeeRepo.save(employee);

		return employee;
	}

	/**
	 * Transforms stupid role String to actual role enum
	 *
	 * @param role
	 * @return role as enum
	 */
	public Role stringToRole(@NonNull String role) {
		Role roleEnum;
		if (role.equalsIgnoreCase("Cashier")) {
			roleEnum = CASHIER;
		} else if (role.equalsIgnoreCase("Chef")) {
			roleEnum = CHEF;
		} else if (role.equalsIgnoreCase("Driver")) {
			roleEnum = DRIVER;
		} else if (role.equalsIgnoreCase("Boss")) {
			roleEnum = BOSS;
		} else {
			throw new IllegalArgumentException();
		}
		return roleEnum;
	}

	/**
	 * This finds all customers in the db except Meta.DELETE_LINK_EMPLOYEE
	 *
	 * @return Streamable of all non-meta employees
	 */
	public Streamable<Employee> findAll() {
		return Streamable.of(removeDeleteEmFromList(employeeRepo.findAll().toList()));
	}

	/**
	 * Removes the Meta.DELETE_LINK_EMPLOYEE from a given list
	 *
	 * @return list without meta employees
	 */
	private List<Employee> removeDeleteEmFromList(List<Employee> employeeList) {
		List<Employee> shallowCustomers = new ArrayList<>(employeeList);
		shallowCustomers.removeIf(employee -> employee.getMeta() == Employee.Meta.DELETE_LINK_EMPLOYEE);
		return shallowCustomers;
	}

	/**
	 * Finds Employee in database by their non-salespoint Id
	 *
	 * @param id
	 * @return Optional of found Employee
	 */
	public Optional<Employee> findById(long id) {
		return employeeRepo.findById(id);
	}

	/**
	 * Finds all Employees by the specified Meta (NORMAL, DELETE_LINK_EMPLOYEE)
	 *
	 * @param meta
	 * @return List of found Employees
	 */
	public List<Employee> findByMeta(Employee.Meta meta) {
		return employeeRepo.findByMeta(meta);
	}

	/**
	 * Finds all Employees by the specified role
	 *
	 * @param role
	 * @return List of found Employees
	 */
	public Streamable<Employee> findByRole(@NonNull String role) {
		return this.findAll().filter(employee -> employee.getRole().equals(role));
	}

	/**
	 * Finds Employee in database by their Username
	 *
	 * @param username
	 * @return Optional of found Employee
	 */
	public Optional<Employee> findByUsername(String username) {
		if (username == null) {
			return Optional.empty();
		}
		return this.findAll()
				.filter(employee -> employee.getUsername().equals(username))
				.stream().findFirst();
	}

	/**
	 * Finds Employee in database by their salespoint UserAccount
	 *
	 * @param userAccount
	 * @return Optional of found Employee
	 */
	public Optional<Employee> findByUserAccount(@NonNull UserAccount userAccount) {
		return this.findAll()
				.filter(employee -> Objects.requireNonNull(employee.getUserAccount().getId()).equals(userAccount.getId()))
				.stream().findFirst();
	}

	/**
	 * Finds Employee and deletes Employee by their non-salespoint Id. <br>
	 * If the target is not valid (i.e. Chef with assigned oven,
	 * Meta-Employee, final Boss in System) the deletion will fail.
	 * @param id employee id
	 * @return Optional of found Employee
	 */
	public boolean deleteById(long id) {
		Optional<Employee> employee = this.findById(id);

		if (employee.isEmpty()) {
			throw new PapaPizzaRunException("Employee does not exist for id " + id);
		}

		if (employee.get().getMeta() == Employee.Meta.DELETE_LINK_EMPLOYEE) {
			throw new PapaPizzaRunException("Meta employee is immutable");
		}

		//Find all orders by this employee and unlink
		List<ShopOrder> employeesOrders = shopOrderManagement.findBy(employee.get()).collect(Collectors.toList());
		//drivers and chefs involved in PENDING or INDELIVERY orders may not be removed
		if (employeesOrders.stream().anyMatch(order -> order.getShopOrderState().isActive())) {
			return false;
		}
		//free up oven
		if (employee.get().getRole().equals("Chef")
				&& getMyOven(employee.get()).isPresent()) {
			getMyOven(employee.get()).get().setChef(null);
		}

		//replace the employee that is about to be deleted with DELETE_LINK_EMPLOYEE
		employeesOrders.forEach(order -> substituteOrderWithDeleteEmployee(order, employee.get()));

		//first delete linked user account
		userAccountMgmt.delete(employee.get().getUserAccount());
		employeeRepo.deleteById(id);
		return true;
	}

	/**
	 * When an Employee is deleted, the Employee's Account must be substituted with the DELETE_LINK_EMPLOYEE. <br>
	 * The Method checks what role the Employee took part and substitutes accordingly.
	 *
	 * @param shopOrder order to substitute an Employee in
	 * @param employee  Employee to substitute
	 */
	public void substituteOrderWithDeleteEmployee(ShopOrder shopOrder, Employee employee) {
		//employee could be in all shopOrder employee fields
		if (shopOrder.getCashier().equals(employee)) {
			shopOrder.setCashier(deleteLinkEmployee);
		}
		//This replaces all occurrences, it might also be possible to just remove the employee from the list
		//shopOrder.getChefs().replaceAll(chef -> chef.equals(employee) ? DELETE_LINK_EMPLOYEE : chef);
		//shopOrder.getDrivers().replaceAll(driver -> driver.equals(employee) ? DELETE_LINK_EMPLOYEE : driver);

		shopOrder.getChefs().removeIf(toRemove -> toRemove.equals(employee));

		if (shopOrder.getChefs().isEmpty()) {
			shopOrder.getChefs().add(deleteLinkEmployee);
		}
		if (shopOrder.getDriver().equals(employee)) {
			shopOrder.setDriver(deleteLinkEmployee);
		}
		shopOrderManagement.save(shopOrder);
	}

	/**
	 * Persistently saves Employee to database
	 *
	 * @param employee
	 * @return Employee for further access
	 */
	public Employee save(@NonNull Employee employee) {
		return employeeRepo.save(employee);
	}

	/**
	 * Deletes salespoint UserAccount of Employee
	 *
	 * @param toDelete
	 * @return Employee
	 */
	public UserAccount deleteAccount(@NonNull UserAccount toDelete) {
		if (toDelete.equals(dummyAccount)) {
			throw new PapaPizzaRunException(DUMMY_ACCOUNT_NAME + " is immutable and must not be removed");
		}
		return userAccountMgmt.delete(toDelete);
	}

	/**
	 * Creates salespoint UserAccount with given attributes
	 *
	 * @param name
	 * @param of           password
	 * @param stringToRole
	 * @return
	 */
	public UserAccount createAccount(String name, Password.UnencryptedPassword of, Role stringToRole) {
		return userAccountMgmt.create(name, of, stringToRole);
	}

	/**
	 * Finds all ovens currently not in use
	 *
	 * @return List of unused ovens
	 */
	//XXX move to inventory mgmt
	public List<OvenProduct> getUnassignedOvens() {
		List<OvenProduct> ovens = catalogMgmt.findByCategory(ProductCategory.OVEN.toString()).stream()
				.map(product -> (OvenProduct) product).collect(Collectors.toList());
		ovens.removeIf(oven -> oven.getChef() != null);
		return ovens;
	}

	/**
	 * Assigns a given oven to a Chef. <br>
	 * Method fails if either is null or the Employee isn't a Chef and unassigns old oven if present.
	 *
	 * @param oven
	 * @param employee
	 */
	public void assignChefToOven(@NonNull OvenProduct oven, @NonNull Employee employee) {
		//oven is already assigned
		if (!(oven.getChef() == null)) {
			throw new IllegalArgumentException();
		}
		//Employee is not a chef
		if (!employee.getRole().equals("Chef")) {
			throw new IllegalArgumentException();
		}

		//Chef already has an oven -> unassign
		Optional<OvenProduct> oldOven = getMyOven(employee);

		if (oldOven.isPresent()) {
			oldOven.get().setChef(null);
		}

		oven.setChef(employee);
		catalogMgmt.save(oven);
		save(employee);
	}

	/**
	 * Finds the current oven of the given Employee
	 *
	 * @param employee
	 * @return Optional of Oven if found, empty Optional if not
	 */
	public Optional<OvenProduct> getMyOven(Employee employee) {
		List<OvenProduct> ovens = catalogMgmt.findByCategory(ProductCategory.OVEN.toString()).stream()
				.map(product -> (OvenProduct) product).collect(Collectors.toList());
		List<OvenProduct> assignedOvens = new ArrayList<>();

		for (OvenProduct ovenInList : ovens) {
			if (ovenInList.getChef() != null) {
				assignedOvens.add(ovenInList);
			}
		}

		for (OvenProduct ovenInList : assignedOvens) {
			if (ovenInList.getChef().getId() == employee.getId()) {
				return Optional.of(ovenInList);
			}
		}
		return Optional.empty();
	}
}
