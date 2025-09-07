package papapizza.kitchen;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.order.OrderLine;
import org.springframework.boot.test.context.SpringBootTest;
import papapizza.inventory.items.OvenProduct;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class DisplayableKitchenTest {

	private OrderLine orderLineTest;
	private Map<ProductIdentifier, Integer> timesTest;
	private OvenProduct ovenTest;

	@BeforeAll
	static void beginTests(){
		System.out.println("Testing Getters and Setters of DisplayableKitchen");
	}

	@Test
	@Disabled
	void getOrderLinesOtherTest() {
		System.out.println("Testing Getter of orderLinesOther");
		DisplayableKitchen testDisplayAbleKitchen = new DisplayableKitchen();
		List<OrderLine> expectedOrderLines = List.of(orderLineTest, orderLineTest, orderLineTest);
		testDisplayAbleKitchen.setOrderLinesOther(expectedOrderLines);
		List<OrderLine> result = testDisplayAbleKitchen.getOrderLinesOther();
		assertEquals(expectedOrderLines, result);
	}

	@Test
	void getTimesTest() {
		System.out.println("Testing Getter of timesTest");
		DisplayableKitchen testDisplayAbleKitchen = new DisplayableKitchen();
		Map<ProductIdentifier, Integer> expectedTimes = timesTest;
		testDisplayAbleKitchen.setTimes(expectedTimes);
		Map<ProductIdentifier, Integer> result = testDisplayAbleKitchen.getTimes();
		assertEquals(expectedTimes, result);
	}

	@Test
	void getOvenProductTest() {
		System.out.println("Testing Getter of OvenProduct");
		DisplayableKitchen testDisplayAbleKitchen = new DisplayableKitchen();
		OvenProduct expectedOvenProductTest = ovenTest;
		testDisplayAbleKitchen.setOven(expectedOvenProductTest);
		OvenProduct result = testDisplayAbleKitchen.getOven();
		assertEquals(expectedOvenProductTest, result);
	}

	@Test
	void getEmptyTest() {
		System.out.println("Testing Getter of empty");
		DisplayableKitchen testDisplayAbleKitchen = new DisplayableKitchen();
		boolean expectedEmptyTest = false;
		testDisplayAbleKitchen.setEmpty(expectedEmptyTest);
		boolean result = testDisplayAbleKitchen.isEmpty();
		assertEquals(expectedEmptyTest, result);
	}

	@Test
	@Disabled
	void setOrderLineTest() {
		System.out.println("Testing Setter of deliveryId");
		DisplayableKitchen testDisplayAbleKitchen = new DisplayableKitchen();
		List<OrderLine> expectedOrderLines = List.of(orderLineTest, orderLineTest, orderLineTest);
		testDisplayAbleKitchen.setOrderLinesOther(expectedOrderLines);
		//assertEquals(expectedOrderLines, );
	}



}
