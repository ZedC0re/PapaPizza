package papapizza.order;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DisplayableShopOrder {

	//validation needs to be added
	private String orderId;
	private String orderState;
	private String deliveryType;
	private Map<String,String> orderLines;
	private String total;

	private String lastname;
	private String firstname;
	private String phone;
	private String address;

	private String cashierName;
	private List<String> chefNames;
	private List<String> driverNames;

	private String invoiceFilename;


}
