package papapizza.analytics;

import org.salespointframework.order.OrderIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import papapizza.delivery.DeliveryManagement;
import papapizza.order.ShopOrder;
import papapizza.order.ShopOrderManagement;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Controller
public class AnalyticsController {
	private final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);
	
	private final AnalyticsManagement analytics;
	private final ShopOrderManagement<ShopOrder> orderManagement;
	private final DeliveryManagement dManagement;

	@Autowired
	AnalyticsController(AnalyticsManagement analytics, ShopOrderManagement<ShopOrder> orderManagement, DeliveryManagement dManagement) {
		this.analytics = analytics;
		this.orderManagement = orderManagement;
		this.dManagement = dManagement;
	}

	@GetMapping("analytics")
	@PreAuthorize("hasAnyRole('BOSS')")
	String weeklyAnalytics() {
		LocalDateTime date = analytics.getCurrentWeek();

		return "redirect:/analytics/"+date.getYear()+"-"+date.getMonthValue()+"-"+date.getDayOfMonth();
	}

	@GetMapping("analytics/m")
	@PreAuthorize("hasAnyRole('BOSS')")
	String monthlyAnalytics() {
		LocalDateTime date = analytics.getCurrentMonth();

		return "redirect:/analytics/"+date.getYear()+"-"+date.getMonthValue()+"-"+date.getDayOfMonth();
	}

	//TODO Quartalsanalyse (1. Januar - 31. März, 1. April - 30. Juni, 1. Juli - 30. September, 1. Oktober - 31. Dezember)

	@GetMapping("analytics/y")
	@PreAuthorize("hasAnyRole('BOSS')")
	String yearlyAnalytics() {
		LocalDateTime date = analytics.getCurrentYear();

		return "redirect:/analytics/"+date.getYear()+"-"+date.getMonthValue()+"-"+date.getDayOfMonth();
	}

	//TODO zeige Date rejection
	@GetMapping("analytics/{year}-{month}-{day}")
	@PreAuthorize("hasAnyRole('BOSS')")
	String analyticsPage(@PathVariable int year,
								@PathVariable int month,
								@PathVariable int day, Model model,
								RedirectAttributes attributes,
								HttpServletRequest httpRequest) throws Exception {
		logger.info("request uri:"+httpRequest.getRequestURI());
		//TODO suchen

		if(!analytics.valiDate(year, month, day)){
			attributes.addFlashAttribute("analyticsRedirect", "invalidDate");
			LocalDateTime date = analytics.getCurrentWeek();

			return "redirect:/analytics/"+date.getYear()+"-"+date.getMonthValue()+"-"+date.getDayOfMonth();
		}

		//generate Graphs
		analytics.generateSalesGraph(year, month, day);
		analytics.generateDurationGraph(year, month, day);

		//get deliveryStrategy
		model.addAttribute("dStrat", DeliveryManagement.getOrderAssign().toString());

		//textCase
		model.addAttribute("textCase", analytics.textCase(LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.MIN)));

		//date
		model.addAttribute("date", LocalDate.of(year, month, day));

		//get COMPLETED ShopOrders
		List<ShopOrder> completedOrders = analytics.getCompletedOrders(year, month, day);
		model.addAttribute("completedOrders", completedOrders);
		logger.info("COMPLETE Orders: " + completedOrders.size());

		//get RETURN_ORDERs
		List<ShopOrder> returnOrders = analytics.getReturnOrders(year, month, day);
		model.addAttribute("returnOrders", returnOrders);
		logger.info("RETURN Orders: " + returnOrders.size());

		//get CANCELLED ShopOrders
		List<ShopOrder> cancelledOrders = analytics.getCancelledOrders(year, month, day);
		model.addAttribute("cancelledOrders", cancelledOrders);
		logger.info("CANCELLED Orders: " + completedOrders.size());

		//Sales Overview
		model.addAttribute("numberOfCompletedOrders", completedOrders.size());
		model.addAttribute("numberOfReturnOrders", returnOrders.size());
		model.addAttribute("numberOfCancelledOrders", cancelledOrders.size());
		model.addAttribute("sales", analytics.getSalesByTime(LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.MIN)));

		return "analytics/analytics";
	}

	@GetMapping("analytics/detail/{orderId}")
	@PreAuthorize("hasAnyRole('BOSS','CASHIER')")
	String orderDetails(@PathVariable OrderIdentifier orderId, Model model) {
		Optional<ShopOrder> order = orderManagement.findByShopOrderId(orderId.getIdentifier());

		if(order.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find order for id "+ orderId);
		} else {
			model.addAttribute("order", order.get());
		}
		return "analytics/orderDetails";
	}
}
