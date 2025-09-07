package papapizza.analytics;

import org.apache.commons.lang3.ArrayUtils;
import org.javamoney.moneta.Money;
import org.math.plot.Plot2DPanel;
import org.math.plot.plotObjects.BaseLabel;
import org.salespointframework.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import papapizza.app.aop.GivePapaHead;
import papapizza.order.DeliveryType;
import papapizza.order.ShopOrder;
import papapizza.order.ShopOrderManagement;
import papapizza.order.ShopOrderState;

import javax.imageio.ImageIO;
import javax.money.MonetaryAmount;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Service
@Transactional
public class AnalyticsManagement {
	private final Logger logger = LoggerFactory.getLogger(AnalyticsManagement.class);
	
	private final ShopOrderManagement<ShopOrder> orderManagement;

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Autowired
	AnalyticsManagement(@NonNull final ShopOrderManagement<ShopOrder> orderManagement) {
		this.orderManagement = orderManagement;
		//create graph & parent folder
		Paths.get(System.getenv("APPDATA"),"PapaPizza","Graph").toFile().mkdirs();
	}

	/**
	 * Calculates sales of orders COMPLETED after the given LocalDateTime
	 * @param time specifies time frame of orders (everything after time is added to calculation)
	 * @return MonetaryAmount of calculated Sales figure
	 */
	public MonetaryAmount getSalesByTime(LocalDateTime time) {
		MonetaryAmount result = Money.of(0, "EUR");

		for (ShopOrder order : orderManagement.findBy(ShopOrderState.COMPLETED)) {
			if (order.getTimeCompleted().isAfter(time)) {
				if(order.getDeliveryType() == DeliveryType.RETURN_ORDER){
					result = result.subtract(order.getTotal());
				}else {
					result = result.add(order.getTotal());
				}
			}
		}
		return result;
	}

	//TODO how should a 'custom' date be handled?
	//TODO add INT for case to determine the timespan (to resolve ambiguity between first day of the year)
	/**
	 * Calculates all sales in a given timeframe, depending on the date chosen the method will decide the time span (will be changed later)
	 * @param year year as a 4-digit number
	 * @param month month as a 2-digit number
	 * @param day day as a 2-digit number
	 * @throws Exception because of the plotting library used
	 */
	@GivePapaHead
	public void generateSalesGraph(int year, int month, int day) throws Exception {
		//reject invalid date
		if(!valiDate(year, month, day)){ throw new IllegalArgumentException(); }

		LocalDateTime date = LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.MIN);
		int maxDay;

		//case of weekly overview -> show graph of every day of the current week
		if(date.equals(getCurrentWeek())){
			maxDay = 7;
		} else if(date.equals(getCurrentMonth())) {
			maxDay = date.getMonth().length(year % 4 ==0);
		} else if(date.equals(getCurrentYear())) {
			if(year % 4 == 0) { maxDay=366; }
			else { maxDay = 365; }
		} else { return; }

		//data to plot
		double[] data = new double[maxDay];

		//go through week days and calculate daily sales
		for(int i = 0; i < maxDay; i++) {
			MonetaryAmount result = Money.of(0, "EUR");

			//Interval of one day (initial should be first day of week)
			Interval iterationInterval = Interval.from(date).to(date.plusDays(1));
			logger.info("(generateSalesGraph) Interval: " + iterationInterval);

			//go through every COMPLETED order of Interval and in-/decrement result
			logger.info(orderManagement.findBy(iterationInterval)
					.filter(orda -> orda.getShopOrderState().equals(ShopOrderState.COMPLETED))
					.filter(order -> order.getTimeCompleted().isAfter(iterationInterval.getStart())).toList().toString());

			for(ShopOrder order : orderManagement.findBy(iterationInterval)
					.filter(orda -> orda.getShopOrderState().equals(ShopOrderState.COMPLETED))
					.filter(order -> order.getTimeCompleted().isAfter(iterationInterval.getStart())).toList()) {
				if(order.getDeliveryType() == DeliveryType.RETURN_ORDER){
					result = result.subtract(order.getTotal());
				}else {
					result = result.add(order.getTotal());
				}
			}
			//increment date
			date = date.plusDays(1);
			//put calculated sales figure into data field
			data[i] = result.getNumber().doubleValueExact();

			logger.info("(generateSalesGraph) Current Sales Figure: " + result.getNumber().doubleValueExact());
		}

		drawSalesGraph(data);
	}

	private void drawSalesGraph(double[] y) throws Exception {
		Path papaRoot = Paths.get(System.getenv("APPDATA"),"PapaPizza\\Graph");
		//creating the panel
		Plot2DPanel plot = new Plot2DPanel();

		//adding text
		Font titleFont = new Font("Courier", Font.BOLD, 20);
		BaseLabel title = new BaseLabel("Sales", Color.BLACK, 0.5, 1.1);
		title.setFont(titleFont);		//title
		plot.addPlotable(title);

		//edit axes
		Font font = new Font("Lucida Sans", Font.PLAIN, 15);
		plot.getAxis(0).setLabelFont(font);
		plot.getAxis(1).setLabelFont(font);
		plot.getAxis(0).setLightLabelFont(font);
		plot.getAxis(1).setLightLabelFont(font);
		plot.setAxisLabel(1, "Sales in €");
		plot.setAxisLabel(0, "Days");

		//plotting the chart
		plot.addLinePlot("name", Color.GREEN, y);

		plot.plotCanvas.setSize(600, 600);
		BufferedImage bufferedImage = new BufferedImage(plot.plotCanvas.getWidth(), plot.plotCanvas.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bufferedImage.createGraphics();
		//Thread.sleep(500);
		plot.plotCanvas.paint(g);
		g.dispose();
		ImageIO.write(bufferedImage, "PNG", new File(papaRoot + "\\salesGraph.png"));
	}

	/**
	 * Calculates average of durations of all state durations from the given date to now.
	 * @see AnalyticsManagement#generateSalesGraph(int, int, int)
	 * @param year year as a 4-digit number
	 * @param month month as a 2-digit number
	 * @param day day as a 2-digit number
	 * @throws Exception because of IO.write
	 */
	@GivePapaHead
	public void generateDurationGraph(int year, int month, int day) throws Exception { //TODO:FIXME dont throw general Exception
		//reject invalid date
		if(!valiDate(year, month, day)){
			throw new IllegalArgumentException();
		}

		LocalDateTime date = LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.MIN);

		List<ShopOrder> orders = orderManagement.findBy(Interval.from(date).to(LocalDateTime.now()))
				.filter(order -> order.getShopOrderState() == ShopOrderState.COMPLETED).toList();

		//4 different Durations -> openDuration, pendingDuration, readyDuration, inDeliverDuration
		// -> totalDuration will be addressed separately
		double[] avgDurations = new double[5];

		//list of all duration methods of order
		//dont change the order of the list elements
		List<Function<ShopOrder, Duration>> durMethods = List.of(
				ShopOrder::getOpenDuration,
				ShopOrder::getPendingDuration,
				ShopOrder::getReadyDuration,
				ShopOrder::getInDeliverDuration,
				ShopOrder::getTotalDuration
		);

		//calc arithmetic mean for all duration types of order
		//O(n)
		for(int i=0; i<durMethods.size(); i++){
			long quantity = 0;
			double sum = 0;
			//sum up all durations for all non null
			for(ShopOrder order : orders) {
				Duration dur = durMethods.get(i).apply(order);
				if (dur != null) {
					sum = sum + dur.toMinutes();
					quantity++;
				}
			}
			//calc arithmetic mean from sum and quantity
			if(quantity==0) { //div by 0
				avgDurations[i] = 0;
			}else {
				avgDurations[i] = sum / quantity;
			}
		}

		logger.info("avgDurations:"+ Arrays.toString(avgDurations));

		drawDurationGraph(avgDurations);
	}

	public void drawDurationGraph(double[] y) throws Exception{
		Path papaRoot = Paths.get(System.getenv("APPDATA"),"PapaPizza\\Graph");

		//creating the panel
		Plot2DPanel plot = new Plot2DPanel();

		//adding text
		Font titleFont = new Font("Courier", Font.BOLD, 20);
		Font labelFont = new Font("Lucida Sans", Font.PLAIN, 15);

		BaseLabel title = new BaseLabel("Avg. Durations", Color.BLACK, 0.5, 1.1);
		title.setFont(titleFont);		//title
		plot.addPlotable(title);

		BaseLabel openD = new BaseLabel("OPEN", Color.BLACK, 0, -0.05);
		openD.setFont(labelFont);
		plot.addPlotable(openD);
		BaseLabel pendD = new BaseLabel("PENDING", Color.BLACK, 0.33, -0.05);
		pendD.setFont(labelFont);
		plot.addPlotable(pendD);
		BaseLabel readyD = new BaseLabel("READY", Color.BLACK, 0.66, -0.05);
		readyD.setFont(labelFont);
		plot.addPlotable(readyD);
		BaseLabel deliverD = new BaseLabel("DELIVERY", Color.BLACK, 1, -0.05);
		deliverD.setFont(labelFont);		//x-axis labels
		plot.addPlotable(deliverD);

		//edit axes
		Font font = new Font("Lucida Sans", Font.PLAIN, 15);
		plot.getAxis(0).setLabelFont(font);
		plot.getAxis(1).setLabelFont(font);
		plot.setFixedBounds(1,0, Collections.max(Arrays.asList(ArrayUtils.toObject(y)))); //FIXME nothing for this library seems to be documented
		//FIXME Y axis should start at 0
		plot.getAxis(0).setLightLabelFont(font);
		plot.getAxis(1).setLightLabelFont(font);

		plot.setAxisLabel(1, "Time in Minutes");
		plot.setAxisLabel(0, "State");
		plot.getAxis(0).setLightLabelAngle(-3.14 / 4); //makes the numbers on x-axis disappear

		//plotting the chart
		plot.addBarPlot("coolname", Color.BLUE, y);

		plot.plotCanvas.setSize(600, 600);

		BufferedImage bufferedImage = new BufferedImage(plot.plotCanvas.getWidth(), plot.plotCanvas.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bufferedImage.createGraphics();

		//Thread.sleep(500);
		plot.plotCanvas.paint(g);
		g.dispose();
		ImageIO.write(bufferedImage, "PNG", new File(papaRoot + "\\durationGraph.png"));
	}

	/**
	 * Returns int to decide what test should be displayed (i.e. Weekly/Monthly/... Overview)
	 * @param date of current request
	 * @return specific text case
	 */
	public int textCase(LocalDateTime date) {
		if(date.equals(getCurrentWeek())){return 0;}
		else if(date.equals(getCurrentMonth())){return 1;}
		else if(date.equals(getCurrentYear())){return 2;}
		else {return 3;}
	}

	//order find methods so that the Controller doesn't get bloated

	/**
	 * Returns a List of all {@link ShopOrderState#COMPLETED} orders from the given date to now.
	 * @param year year as a 4-digit number
	 * @param month month as a 2-digit number
	 * @param day day as a 2-digit number
	 * @return List of all found orders
	 */
	public List<ShopOrder> getCompletedOrders(int year, int month, int day) {
		return orderManagement.findBy(ShopOrderState.COMPLETED)
				.filter(shopOrder -> Objects.requireNonNull(shopOrder.getTimeCompleted())
						.isAfter(LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.MIN)))
				.filter(shopOrder -> shopOrder.getDeliveryType().equals(DeliveryType.RETURN_ORDER) == false).toList();
	}

	/**
	 * Returns a List of all {@link DeliveryType#RETURN_ORDER} orders from the given date to now.
	 * @param year year as a 4-digit number
	 * @param month month as a 2-digit number
	 * @param day day as a 2-digit number
	 * @return List of all found return orders
	 */
	public List<ShopOrder> getReturnOrders(int year, int month, int day) {
		return orderManagement.findBy(ShopOrderState.COMPLETED)
				.filter(shopOrder -> Objects.requireNonNull(shopOrder.getTimeCompleted())
						.isAfter(LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.MIN)))
				.filter(shopOrder -> shopOrder.getDeliveryType().equals(DeliveryType.RETURN_ORDER) == true).toList();
	}

	/**
	 * Returns a List of all {@link ShopOrderState#CANCELLED} orders from the given date to now
	 * @param year year as a 4-digit number
	 * @param month month as a 2-digit number
	 * @param day day as a 2-digit number
	 * @return List of all found orders
	 */
	public List<ShopOrder> getCancelledOrders(int year, int month, int day) {
		return orderManagement.findBy(ShopOrderState.CANCELLED)
				.filter(shopOrder -> Objects.requireNonNull(shopOrder.getTimeCompleted())
						.isAfter(LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.MIN))).toList();
	}

	/**
	 * Method used to get the beginning of the week.
	 * @return LocalDateTime of current week's Monday at 00:00
	 */
	public LocalDateTime getCurrentWeek() {
		//if the first weekday is in previous month/year it get's problematic
		LocalDate date = LocalDate.now();
		date = date.minusDays(date.getDayOfWeek().getValue()-1L);
		/*
		logger.info("This week's Year: " + date.getYear());
		logger.info("This week's Month: ");
		logger.info("First day of the week: " + date.getDayOfMonth());
		*/
		return LocalDateTime.of(date, LocalTime.MIN);
	}

	/**
	 * Method used to get the beginning of the month.
	 * @return LocalDateTime of current month's 1st at 00:00
	 */
	public LocalDateTime getCurrentMonth() {
		LocalDateTime date = LocalDateTime.of(LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonth(), 1),
				LocalTime.MIN);
		/*
		LOG.info("This week's Year: " + date.getYear());
		LOG.info("This week's Month: "+ date.getMonthValue());
		LOG.info("First day of the week: " + date.getDayOfMonth());
		*/
		return date;
	}

	/**
	 * Method used to get the beginning of the year.
	 * @return LocalDateTime of current year's 1st at 00:00
	 */
	public LocalDateTime getCurrentYear() {
		return LocalDateTime.of(LocalDate.of(LocalDate.now().getYear(), Month.JANUARY, 1),
				LocalTime.MIN);
	}

	/**
	 * Checks if given date is valid.
	 * @param year year as a 4-digit number
	 * @param month month as a 2-digit number
	 * @param day day as a 2-digit number
	 * @return true if valid, false if not
	 */
	public boolean valiDate(int year, int month, int day) {
		//Schaltjahr
		if(month == 2 && (year % 4 == 0)) {
			return day >= 1 && day <= 29;
		}

		//Feb
		if(month == 2) {
			return day >= 1 && day <= 28;
		}

		//big month
		if(month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12) {
			return month >= 1 && month <= 12 && day >= 1 && day <= 31;
		}

		//small month
		if(month == 4 || month == 6 || month == 9 || month == 11) {
			return month >= 1 && month <= 12 && day >= 1 && day <= 30;
		}

		return month >= 1 && month <= 12 && day >= 1 && day <= 31;
	}
}
