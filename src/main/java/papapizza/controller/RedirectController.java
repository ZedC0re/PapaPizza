/*
 * Copyright 2014-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package papapizza.controller;

import org.salespointframework.useraccount.Role;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.web.LoggedIn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import papapizza.employee.EmployeeManagement;

import java.util.Optional;

@Controller
public class RedirectController{

	@GetMapping("/")
	String rootPage(@LoggedIn Optional<UserAccount> userAccount){
		String redirectValue = "redirect:/login";
		if(userAccount.isPresent()){
			Role primaryRole = userAccount.get().getRoles().toList().get(0);
			if(primaryRole.equals(EmployeeManagement.BOSS)){
				redirectValue = "redirect:/analytics";
			}
			if(primaryRole.equals(EmployeeManagement.CHEF)){
				redirectValue = "redirect:/kitchen";
			}
			if(primaryRole.equals(EmployeeManagement.CASHIER)){
				redirectValue = "redirect:/order";
			}
			if(primaryRole.equals(EmployeeManagement.DRIVER)){
				redirectValue = "redirect:/delivery";
			}
		}
		return redirectValue;
	}

	@GetMapping("/dev")
	String defaultPage(Model model, @LoggedIn Optional<UserAccount> userAccount){
		userAccount.ifPresent(account -> model.addAttribute("currentRoles", account.getRoles()));

		return "dev";
	}
}
