package papapizza.order;



public enum ShopOrderState {
	OPEN, PENDING, READYPICKUP, READYDELIVER, INDELIVERY, COMPLETED, CANCELLED, INVALID;

	public boolean isActive(){ //writing it like this is just bs sonarqube ...
		if(this == OPEN || this == PENDING || this == READYPICKUP){
			return true;
		}
		return this == READYDELIVER || this == INDELIVERY;
	}

	public boolean isBeingWorkedOn(){
		return this == PENDING || this == INDELIVERY;
	}
}
