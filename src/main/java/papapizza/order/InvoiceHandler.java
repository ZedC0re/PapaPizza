package papapizza.order;

import be.quodlibet.boxable.BaseTable;
import be.quodlibet.boxable.Cell;
import be.quodlibet.boxable.Row;
import lombok.Getter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.salespointframework.catalog.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import papapizza.customer.Customer;
import papapizza.customer.LentDishset;
import papapizza.inventory.ShopCatalogManagement;
import papapizza.util.PizzaStatics;

import javax.money.MonetaryAmount;
import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class InvoiceHandler {

	private final Logger logger = LoggerFactory.getLogger(InvoiceHandler.class);

	private static final String PAPA_PATH_PREFIX = "PapaPizza";
	private static final String INVOICE_PATH_PREFIX = "Invoice";
	private static final String INVOICE_HEAD_NAME = "invoiceHEAD";

	@Getter
	private Path invoicePath;

	private ShopCatalogManagement catalogManagement;

	public InvoiceHandler(){
		createPath();
		try {
			createInvoiceHeadFile();
		} catch (IOException e) {
			logger.error("Could not create invoice HEAD file");
		}
	}

	@Autowired
	public void setShopCatalogManagement(ShopCatalogManagement catalogManagement){
		this.catalogManagement = catalogManagement;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void createPath(){
		//root file directory of papapizza application
		Path papa_root = Paths.get(System.getenv("APPDATA"),PAPA_PATH_PREFIX);
		//subdir for invoices
		invoicePath = Paths.get(papa_root.toString(), INVOICE_PATH_PREFIX);
		//try to create parent and directory
		invoicePath.toFile().mkdirs();
		logger.info("Invoice path:"+ invoicePath.toString());
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void createInvoiceHeadFile() throws IOException {
		File invoiceHead = new File(Paths.get(invoicePath.toString(),INVOICE_HEAD_NAME).toString());
		if(!invoiceHead.exists()){
			logger.info("creating invoice head file");
			invoiceHead.createNewFile();
			FileOutputStream fout = new FileOutputStream(invoiceHead);
			DataOutputStream dout = new DataOutputStream(fout);
			dout.writeLong(0L);
			dout.close();
			fout.close();
		}else{
			logger.info("invoiceHEAD file alr exists, no need to create");
		}
	}

	private long incrementInvoiceHead() throws IOException {
		File invoiceHeadFile = new File(Paths.get(invoicePath.toString(), INVOICE_HEAD_NAME).toString());
		//get and increment current invoice no
		DataInputStream din = new DataInputStream(new FileInputStream(invoiceHeadFile));
		long head = din.readLong() +1L;
		din.close();

		//write invoice number to file again
		DataOutputStream dout = new DataOutputStream(new FileOutputStream(invoiceHeadFile));
		dout.writeLong(head);
		dout.close();
		return head;
	}

	public String createInvoice(ShopOrder order) throws IOException {
		String fileName = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS")
				.format(PizzaStatics.localDateTimeToDate(order.getTimeCreated()));
		fileName += ".pdf";
		logger.info("creating new invoice with filename "+fileName);

		Customer customer = order.getCustomer();

		//new pdf document in DIN A4
		PDDocument invoicePdf = new PDDocument();
		PDPage invoicePage = new PDPage(PDRectangle.A4);
		invoicePdf.addPage(invoicePage);

		//Head = Headline, Customer, TAN, Invoice Number
		PDPageContentStream cont = new PDPageContentStream(invoicePdf, invoicePage);
		float PAGE_HEIGHT = PDRectangle.A4.getHeight();
		cont.beginText();
		//headline
		cont.newLineAtOffset(50f, PAGE_HEIGHT-40f);
		cont.setFont(PDType1Font.HELVETICA_BOLD, 20);
		cont.showText("PapaPizza Invoice");
		cont.endText();

		//==================== Header TABLE =======================
		//Table for customer and invoice details
		float margin = 50;
		float yStartNewPage = invoicePage.getMediaBox().getHeight() - (2 * margin);
		float tableWidth = invoicePage.getMediaBox().getWidth() - (2 * margin);
		float bottomMargin = 70;
		float yPosition = PAGE_HEIGHT-60f;

		BaseTable headerTable = new BaseTable(yPosition, yStartNewPage, bottomMargin,
				tableWidth, margin, invoicePdf, invoicePage, false, true);
		createHeaderTable(headerTable, customer);
		headerTable.draw();

		//stroke rectangle around table=header
		float tableHeight = headerTable.getHeaderAndDataHeight();
		cont.setStrokingColor(Color.black);
		cont.addRect(50,yPosition-tableHeight,invoicePage.getMediaBox().getWidth()-100,tableHeight);
		cont.stroke();

		//================== Body/Order details =======================
		float orderTableLabelPosY = yPosition-tableHeight-30f;
		cont.beginText();
		cont.setFont(PDType1Font.HELVETICA_BOLD, 16f);
		cont.newLineAtOffset(50f, orderTableLabelPosY);
		cont.showText("ORDER DETAILS");
		cont.endText();

		BaseTable bodyTable = new BaseTable(orderTableLabelPosY-10f,yStartNewPage, bottomMargin,
				tableWidth, margin, invoicePdf, invoicePage, true, true);
		createBodyTable(bodyTable, order);
		bodyTable.draw();

		//==================== Footer/Total, dishset return info ======================
		float totalTextPosY = orderTableLabelPosY-bodyTable.getHeaderAndDataHeight()-30f;
		cont.beginText();
		cont.setFont(PDType1Font.HELVETICA_BOLD, 14f);
		cont.newLineAtOffset(50f, totalTextPosY);

		MonetaryAmount totalPrice = order.getTotal();
		if(order.getDeliveryType()==DeliveryType.RETURN_ORDER){
			totalPrice = totalPrice.negate();
		}
		String totalLine = String.format("TOTAL: %s",totalPrice.toString());
		cont.showText(totalLine);
		cont.endText();
		cont.moveTo(50f,totalTextPosY-1f);
		PDFont font = PDType1Font.HELVETICA_BOLD;
		int fontSize = 14;
		float lineWidth = font.getStringWidth(totalLine) / 1000 * fontSize;
		cont.lineTo(lineWidth+50f, totalTextPosY-1f);

		//Dishset return info
		//TODO if actually lent
		cont.beginText();
		cont.setFont(PDType1Font.HELVETICA, 10f);
		cont.newLineAtOffset(50f, 20f);
		String infoText = String.format("Please keep in mind that lent dishsets must be returned within %d days!",
				LentDishset.RETURN_TIME/86400);
		cont.showText(infoText);
		cont.endText();

		cont.close();
		//save changes & close
		invoicePdf.save(Paths.get(invoicePath.toString(),fileName).toString());
		invoicePdf.close();

		return fileName;
	}

	private void createBodyTable(BaseTable bodyTable, ShopOrder order){
		Row<PDPage> headerRow = bodyTable.createRow(14f);
		Map<String, String> headerLabels = Map.of(
				"productInfo","PRODUCT",
				"quantity","QUANTITY",
				"price","PRICE"
		);
		Cell<PDPage> prodInfoLabel = headerRow.createCell(50f,headerLabels.get("productInfo"));
		Cell<PDPage> quantityLabel = headerRow.createCell(20f,headerLabels.get("quantity"));
		Cell<PDPage> priceLabel = headerRow.createCell(30f, headerLabels.get("price"));
		List.of(prodInfoLabel, quantityLabel, priceLabel).forEach(label -> {
			label.setFont(PDType1Font.HELVETICA_BOLD);
			label.setFontSize(14f);
		});

		order.getOrderLines().forEach(orderLine -> {
			Row<PDPage> bodyRow = bodyTable.createRow(14f);
			Product product = catalogManagement.findById(orderLine.getProductIdentifier());
			String prodInfoStr = String.format("[%s] %s",product.getCategories().stream().findFirst().get(),
					orderLine.getProductName());
			Cell<PDPage> prodInfo = bodyRow.createCell(50f, prodInfoStr);
			Cell<PDPage> quantity = bodyRow.createCell(20f,
					orderLine.getQuantity().getAmount().intValue()+"x");
			MonetaryAmount monPrice = orderLine.getPrice();
			if(order.getDeliveryType()==DeliveryType.RETURN_ORDER){
				monPrice = monPrice.negate();
			}
			Cell<PDPage> price = bodyRow.createCell(30f, monPrice.toString());
			price.setTextColor(Color.red);
			List.of(prodInfo, quantity, price).forEach(cell -> {
				cell.setFont(PDType1Font.HELVETICA);
				cell.setFontSize(14f);
			});
		});

		order.getChargeLines().forEach(chargeLine -> {
			Row<PDPage> bodyRow = bodyTable.createRow(14f);
			String chargeInfoStr = String.format("[CHARGE] %s",chargeLine.getDescription());
			Cell<PDPage> prodInfo = bodyRow.createCell(50f, chargeInfoStr);
			Cell<PDPage> quantity = bodyRow.createCell(20f, "--charge--");
			Cell<PDPage> price = bodyRow.createCell(30f, chargeLine.getPrice().toString());
			price.setTextColor(Color.blue);
			List.of(prodInfo, quantity, price).forEach(cell -> {
				cell.setFont(PDType1Font.HELVETICA);
				cell.setFontSize(14f);
			});
		});
	}

	private void createHeaderTable(BaseTable headerTable, Customer customer) throws IOException {
		Row<PDPage> detailsTopRow = headerTable.createRow(12f);
		Cell<PDPage> toDetails = detailsTopRow.createCell(50, "INVOICE TO");
		toDetails.setTextColor(Color.gray);
		toDetails.setFontSize(12);
		Cell<PDPage> invoiceHeadline = detailsTopRow.createCell(50, "INVOICE DETAILS");
		invoiceHeadline.setTextColor(Color.gray);
		invoiceHeadline.setFontSize(12);

		Row<PDPage> row = headerTable.createRow(14f);
		//customer
		String[] cstmrDet = {
				"<b>"+String.format("%s %s", customer.getFirstname(), customer.getLastname())+"</b>",  //Name
				customer.getAddress(), //Address
				"PHONE #: " + customer.getPhone() //Phone
		};
		Cell<PDPage> cstmrCell = row.createCell(50, String.join("<br>", cstmrDet));
		cstmrCell.setFontSize(14);
		cstmrCell.setLineSpacing(1.1f);
		//invoice
		long invoiceNo = incrementInvoiceHead();
		logger.info("invoice no:"+invoiceNo);
		String[] invoiceDetLabels = {
				"INVOICE #:",
				"INVOICE DATE:",
				"USED TAN:",
				"<b>NEW TAN:</b>"
		};
		Cell<PDPage> invoiceCellLabels = row.createCell(25, String.join("<br>", invoiceDetLabels));
		invoiceCellLabels.setFontSize(14);
		invoiceCellLabels.setLineSpacing(1.1f);

		String[] invoiceDet = {
				invoiceNo+"",
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
				customer.getOldTan() == null ? "n/a" : customer.getOldTan().toString(),
				"<b>"+customer.getCurrentTan().toString()+"</b>"
		};
		Cell<PDPage> invoiceCell = row.createCell(25, String.join("<br>", invoiceDet));
		invoiceCell.setFontSize(14);
		invoiceCell.setLineSpacing(1.1f);
	}

	public ResponseEntity<InputStreamResource> getInvoiceResponse(String filename){
		File invoiceFile = new File(Paths.get(invoicePath.toString(), filename).toString());
		//prep headers and payload
		HttpHeaders respHeaders = new HttpHeaders();
		respHeaders.setContentType(MediaType.APPLICATION_PDF);
		respHeaders.setContentLength(invoiceFile.length());
		respHeaders.setContentDispositionFormData("attachment", invoiceFile.getName());
		try {
			InputStreamResource isr = new InputStreamResource(new FileInputStream(invoiceFile));
			return new ResponseEntity<>(isr, respHeaders, HttpStatus.OK);
		} catch (FileNotFoundException e) {
			logger.error("File not found: "+filename);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}
}
