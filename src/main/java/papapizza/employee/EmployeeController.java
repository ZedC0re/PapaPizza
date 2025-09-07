package papapizza.employee;

import org.apache.commons.lang3.StringUtils;
import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.useraccount.Password;
import org.salespointframework.useraccount.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.inventory.items.OvenProduct;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class EmployeeController {
	private final EmployeeManagement emplManagement;
	private final ShopCatalogManagement catalogMgmt;

	@Autowired
	EmployeeController(EmployeeManagement emplManagement, ShopCatalogManagement catalogMgmt) {
		this.emplManagement = emplManagement;
		this.catalogMgmt = catalogMgmt;
	}

	@GetMapping("/gotoManagement")
	String goToManagement() {
		return "redirect:/employeeManagement";
	}

	@GetMapping("gotoDefault")
	String goToDefault() {
		return "redirect:/default";
	}

	//------Employee-Management------
	@GetMapping("employeeManagement")
	@PreAuthorize("hasRole('BOSS')")
	String emplManagementPage(Model model, @RequestParam(name = "search", required = false) String search) {
		List<Employee> employees = new ArrayList<>(emplManagement.findAll().toList());

		//list needs to be filtered if param present
		if (search != null && !search.isBlank()) {
			//employee is removed from list when search not succeeds
			employees.removeIf(emp -> !empContainsFilter(emp, search));
		}

		model.addAttribute("employees", employees);
		model.addAttribute("search", search);

		return "employeeManagement/employeeManagement";
	}

	private boolean empContainsFilter(Employee emp, String search) {
		//define an array with all searchable attributes of the emp
		String[] searchable = {emp.getFirstname(), emp.getLastname(), emp.getUsername(), emp.getRole()};
		//if any element contains the searched string
		for (String searchStr : searchable) {
			if (StringUtils.containsIgnoreCase(searchStr, search)) {
				return true;
			}
		}
		return false;
	}

	@GetMapping("/newEmployeeBtn")
	@PreAuthorize("hasRole('BOSS')")
	String newEmplBtn() {
		return "redirect:/newEmployee";
	}

	@PostMapping("/emplMgmt/del/{id}")
	@PreAuthorize("hasRole('BOSS')")
	String deleteEmpl(@PathVariable long id, RedirectAttributes attributes) {
		Optional<Employee> employee = emplManagement.findById(id);

		if (employee.isPresent()) {
			//last boss cannot be removed, there has to be one boss account
			if (employee.get().getRole().equals("Boss") && emplManagement.findByRole("Boss").get().count() == 1) {
				attributes.addFlashAttribute("emplModifyRes", "lastbossDel");
			} else {
				if (emplManagement.deleteById(id)) {
					attributes.addFlashAttribute("emplModifyRes", "delSuccess");
				} else {
					attributes.addFlashAttribute("emplModifyRes", "delOrderBeingWorkedOn");
				}
			}
		} else {
			throw new ResponseStatusException(NOT_FOUND, "Unable to find employee for id " + id);
		}

		return "redirect:/employeeManagement";
	}


	//------New-Employee------

	@GetMapping("/newEmployee")
	@PreAuthorize("hasRole('BOSS')")
	String newEmplPage(@ModelAttribute("form") EmployeeCreationForm form) {
		return "employeeManagement/newEmployee";
	}

	@PostMapping("/newEmployee")
	@PreAuthorize("hasRole('BOSS')")
	String createEmpl(@Valid @ModelAttribute("form") EmployeeCreationForm form, BindingResult result) {

		//Username already present
		if (emplManagement.findByUsername(form.getName()).isPresent()) {
			result.rejectValue("name", "emplCreationForm.notUnique.name", "Name must be unique");
		}

		//password blank
		if (form.getPassword() == null || form.getPassword().isBlank()) {
			result.rejectValue("password", "emplCreationForm.notEmpty.password", "Password may not be empty");
		}

		//check password against regex
		if (!(form.getPassword() == null) && !form.getPassword().matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{1,20}$")) {
			result.rejectValue("password", "emplCreationForm.regex.password",
					"Password must contain uppercase and lowercase letter and a number");
		} else if (!(form.getPassword() == null)
				&& !(form.getRepeatedPassword() == null)
				&& !form.getPassword().equals(form.getRepeatedPassword())) { //password & repeated match
			result.rejectValue("repeatedPassword", "emplCreationForm.noMatch.password", "Passwords do not match");
		}

		if (result.hasErrors()) {
			return "employeeManagement/newEmployee";
		}

		emplManagement.createEmployee(form);

		return "redirect:/employeeManagement";
	}

	//------Edit-Employee------
	@GetMapping("emplMgmt/edit/{id}")
	@PreAuthorize("hasRole('BOSS')")
	String editEmployeeForm(@PathVariable long id,
							@ModelAttribute("form") EmployeeCreationForm form,
							Model model,
							RedirectAttributes attributes) {
		Optional<Employee> employee = emplManagement.findById(id);

		//if editing an Employee additional data is shown -> assign oven
		if (employee.isPresent()
				&& employee.get().getRole().equals("Chef")) {
			model.addAttribute("ovens", emplManagement.getUnassignedOvens());
			model.addAttribute("myOven", emplManagement.getMyOven(employee.get()));
		}

		//check if the last Boss wants to edit themselves
		if (employee.isPresent() && employee.get().getRole().equals("Boss")
				&& emplManagement.findByRole("Boss").get().count() == 1) {
			attributes.addFlashAttribute("emplModifyRes", "lastbossEdit");
			return "redirect:/employeeManagement";
		}

		//boolean for thymeleaf to display error message if employee not present
		model.addAttribute("employeeFound", employee.isPresent());

		//add customer to model if present
		if (employee.isPresent()) {
			model.addAttribute("employee", employee.get());
			model.addAttribute("employeeId", employee.get().getId());

			form.setFormDetails(employee.get());
		}

		return "employeeManagement/editEmployee";
	}

	@PostMapping("emplMgmt/edit/{id}")
	@PreAuthorize("hasRole('BOSS')")
	String editEmployee(@PathVariable long id,
						@Valid @ModelAttribute("form") EmployeeCreationForm form,
						Model model,
						BindingResult result) {
		Optional<Employee> optEmployee = emplManagement.findById(id);

		//throw an exception if the employee doesnt exist
		if (optEmployee.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This employee does not exist");
		}

		Employee employee = optEmployee.get();

		//throw bad request if you try to edit the last boss entry
		if (employee.getRole().equals("Boss") && emplManagement.findByRole("Boss").get().count() == 1) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "you cannot edit nor remove the last boss");
		}

		boolean newUserAcc = needsNewUserAcc(form, result, employee);

		if (result.hasErrors()) {
			//reset role in form to old role -> Chef causes an exception because more data has to be loaded
			form.setRole(employee.getRole());
			if (employee.getRole().equals("Chef")) {
				model.addAttribute("ovens", emplManagement.getUnassignedOvens());
				model.addAttribute("myOven", emplManagement.getMyOven(employee));
			}

			//boolean for thymeleaf to display error message if employee not present
			//always true in this case as employee is alr found
			model.addAttribute("employeeFound", true);
			model.addAttribute("employeeId", id);
			return "employeeManagement/editEmployee";
		}

		//on password change -> create a new userAccount and delete the old one (salespoint ...)
		if (newUserAcc) {
			//UserAccount-Shuffle
			//needs to be like this in case the usernames are the same and only the pw is changed, then the
			//existing account needs to be deleted first
			UserAccount toDelete = employee.getUserAccount();
			employee.setUserAccount(null);
			emplManagement.save(employee);
			emplManagement.deleteAccount(toDelete);
			UserAccount newAcc = emplManagement.createAccount(form.getName(),
					Password.UnencryptedPassword.of(form.getPassword()), emplManagement.stringToRole(form.getRole()));
			employee.setUserAccount(newAcc);
		}

		form.setEmployeeDetails(employee); //set other employee details like firstname, lastname (address maybe later)
		emplManagement.save(employee); //make changes persistent in db

		return "redirect:/employeeManagement";
	}

	private boolean needsNewUserAcc(EmployeeCreationForm form, BindingResult result, Employee employee) {
		//password not blank -> new user account
		if (form.getPassword() != null && !form.getPassword().isBlank()) {
			//check password against regex
			if (!form.getPassword().matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{1,20}$")) {
				result.rejectValue("password", "emplCreationForm.regex.password",
						"Password must contain uppercase and lowercase letter and a number");
			}
			//password & repeated do not match
			if (form.getRepeatedPassword() != null && !form.getPassword().equals(form.getRepeatedPassword())) {
				result.rejectValue("repeatedPassword", "emplCreationForm.noMatch.password",
						"Passwords do not match");
			} else { //if passwords match we need to change the password later on
				return true;
			}
		} else if (!employee.getUsername().equals(form.getName())) { //pw blank but username changed -> err
			result.rejectValue("name", "emplCreationForm.notWithoutPw.username",
					"Username cannot be changed without setting pw");
		}
		return false;
	}

	@GetMapping("emplMgmt/{ovenId}/{id}")
	@PreAuthorize("hasRole('BOSS')")
	String editEmployee(@PathVariable long id, @PathVariable ProductIdentifier ovenId, RedirectAttributes attributes) {
		Optional<Employee> empl = emplManagement.findById(id);
		OvenProduct oven = (OvenProduct) catalogMgmt.findById(ovenId);

		if (empl.isEmpty()) {
			throw new ResponseStatusException(NOT_FOUND, "This Employee doesn't exist");
		}
		if (oven == null) {
			throw new ResponseStatusException(NOT_FOUND, "This Oven doesn't exist");
		}

		Optional<OvenProduct> oldOven = emplManagement.getMyOven(empl.get());

		//check if old oven is in use
		if (oldOven.isPresent() && !oldOven.get().getPizzas().isEmpty()) {
			attributes.addFlashAttribute("editRedirect", "unassignOvenInUse");
			return "redirect:/emplMgmt/edit/" + id;
		}

		emplManagement.assignChefToOven(oven, empl.get());

		return "redirect:/employeeManagement";
	}
}
