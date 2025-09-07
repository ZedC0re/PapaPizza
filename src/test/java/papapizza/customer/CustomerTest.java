package papapizza.customer;

import org.junit.jupiter.api.Test;
import org.salespointframework.useraccount.Password;
import org.salespointframework.useraccount.Role;
import org.salespointframework.useraccount.UserAccount;
import org.salespointframework.useraccount.UserAccountManagement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CustomerTest {

	@Autowired
	private UserAccountManagement usrAccMgmt;

	//ignore this, just wanted to know what hash look like
	@Test
	public void pwEncTest(){
		UserAccount usrAcc = usrAccMgmt.create("jack", Password.UnencryptedPassword.of("1337"), Role.of("TEST"));
		System.out.println(usrAcc.getPassword());

		assertNotEquals("1337", usrAcc.getPassword().toString());
	}
}