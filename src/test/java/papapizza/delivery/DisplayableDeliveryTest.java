package papapizza.delivery;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DisplayableDeliveryTest {

	//This is way too complicated, but I don't want to put even more time into it by making it easier

	@BeforeAll
	static void beginTests(){
		System.out.println("--- Testing Getters and Setters of DisplayableDelivery ---");
	}

	@Test
	void getDeliveryIdTest() {
		System.out.println("Testing Getter of deliveryId...");
		DisplayableDelivery testDisplayAbleDelivery = new DisplayableDelivery();
		String expectedId = "AVALID-ID-1234";
		testDisplayAbleDelivery.setDeliveryId("AVALID-ID-1234");
		String result = testDisplayAbleDelivery.getDeliveryId();
		assertEquals(expectedId, result);
	}

	@Test
	void getCustomerNameTest() {
		System.out.println("Testing Getter of customerName...");
		DisplayableDelivery testDisplayAbleDelivery = new DisplayableDelivery();
		String expectedName = "Defenetly a Name";
		testDisplayAbleDelivery.setCustomerName("Defenetly a Name");
		String result = testDisplayAbleDelivery.getCustomerName();
		assertEquals(expectedName, result);
	}

	@Test
	void getCustomerTelephoneNumberTest() {
		System.out.println("Testing Getter of customerTelephoneNumber...");
		DisplayableDelivery testDisplayAbleDelivery = new DisplayableDelivery();
		String expectedNumber = "012345678";
		testDisplayAbleDelivery.setCustomerTelephoneNumber("012345678");
		String result = testDisplayAbleDelivery.getCustomerTelephoneNumber();
		assertEquals(expectedNumber, result);
	}

	@Test
	void getCustomerAddressTest() {
		System.out.println("Testing Getter of customerAddress...");
		DisplayableDelivery testDisplayAbleDelivery = new DisplayableDelivery();
		String expectedAddress = "Zuhause";
		testDisplayAbleDelivery.setCustomerAddress("Zuhause");
		String result = testDisplayAbleDelivery.getCustomerAddress();
		assertEquals(expectedAddress, result);
	}

	@Test
	void getDeliveryPriceTest() {
		System.out.println("Testing Getter of deliveryPrice...");
		DisplayableDelivery testDisplayAbleDelivery = new DisplayableDelivery();
		String expectedPrice = "3 Mark 50";
		testDisplayAbleDelivery.setDeliveryPrice("3 Mark 50");
		String result = testDisplayAbleDelivery.getDeliveryPrice();
		assertEquals(expectedPrice, result);
	}

	@Test
	void getTimeLeftTest() {
		System.out.println("Testing Getter of timeLeft...");
		DisplayableDelivery testDisplayAbleDelivery = new DisplayableDelivery();
		long expectedTime = 350;
		testDisplayAbleDelivery.setTimeLeft(350);
		long result = testDisplayAbleDelivery.getTimeLeft();
		assertEquals(expectedTime, result);
	}

	@Test
	void getOrderLinesTest() {
		System.out.println("Testing Getter of orderLines...");
		DisplayableDelivery testDisplayAbleDelivery = new DisplayableDelivery();
		List<String> expectedOrderLines = List.of("Orderline1", "Orderline2", "Orderline3");
		testDisplayAbleDelivery.setOrderLines(List.of("Orderline1", "Orderline2", "Orderline3"));
		List<String> result = testDisplayAbleDelivery.getOrderLines();
		assertEquals(expectedOrderLines, result);
	}

	@Test
	void isViewableTest() {
		System.out.println("Testing Getter of viewable...");
		DisplayableDelivery testDisplayAbleDelivery = new DisplayableDelivery();
		testDisplayAbleDelivery.setViewable(true);
		boolean result = testDisplayAbleDelivery.isViewable();
		assertTrue(result);
	}

	@Test
	void setDeliveryIdTest() {
		System.out.println("Testing Setter of deliveryId");
		DisplayableDelivery testDisplayAbleDelivery = new DisplayableDelivery();
		String testId = "A-VALID-ID123";
		testDisplayAbleDelivery.setDeliveryId(testId);
		assertEquals(testId, testDisplayAbleDelivery.getDeliveryId());
	}

	@Test
	void setCustomerNameTest() {
		System.out.println("Testing Setter of customerName");
		DisplayableDelivery testDisplayAbleDelivery = new DisplayableDelivery();
		String testName = "Mr. Test";
		testDisplayAbleDelivery.setCustomerName(testName);
		assertEquals(testName, testDisplayAbleDelivery.getCustomerName());
	}

	@Test
	void setCustomerTelephoneNumberTest() {
		System.out.println("Testing Setter of customerTelephoneNumber");
		DisplayableDelivery testDisplayAbleDelivery = new DisplayableDelivery();
		String testNumber = "012345678";
		testDisplayAbleDelivery.setCustomerTelephoneNumber(testNumber);
		assertEquals(testNumber, testDisplayAbleDelivery.getCustomerTelephoneNumber());
	}

	@Test
	void setCustomerAddressTest() {
		System.out.println("Testing Setter of customerAddress");
		DisplayableDelivery testDisplayAbleDelivery = new DisplayableDelivery();
		String testAddress = "Zuhause";
		testDisplayAbleDelivery.setCustomerAddress(testAddress);
		assertEquals(testAddress, testDisplayAbleDelivery.getCustomerAddress());
	}

	@Test
	void setDeliveryPriceTest() {
		System.out.println("Testing Setter of deliveryPrice");
		DisplayableDelivery testDisplayAbleDelivery = new DisplayableDelivery();
		String testPrice = "3 Mark 30";
		testDisplayAbleDelivery.setDeliveryPrice(testPrice);
		assertEquals(testPrice, testDisplayAbleDelivery.getDeliveryPrice());
	}

	@Test
	void setTimeLeftTest() {
		System.out.println("Testing Setter of timeLeft");
		DisplayableDelivery testDisplayAbleDelivery = new DisplayableDelivery();
		long testTime = 340;
		testDisplayAbleDelivery.setTimeLeft(testTime);
		assertEquals(testTime, testDisplayAbleDelivery.getTimeLeft());
	}

	@Test
	void setOrderLinesTest() {
		System.out.println("Testing Setter of orderLines");
		DisplayableDelivery testDisplayAbleDelivery = new DisplayableDelivery();
		List<String> testLines = List.of("Line1", "Line2");
		testDisplayAbleDelivery.setOrderLines(testLines);
		assertEquals(testLines, testDisplayAbleDelivery.getOrderLines());
	}

	@Test
	void setViewableTest() {
		System.out.println("Testing Setter of viewable");
		DisplayableDelivery testDisplayAbleDelivery = new DisplayableDelivery();
		testDisplayAbleDelivery.setViewable(true);
		assertTrue(testDisplayAbleDelivery.isViewable());
	}

	@AfterAll
	static void endTests() {
		System.out.println("--- Done testing Getters and Setters ---");

	}
}