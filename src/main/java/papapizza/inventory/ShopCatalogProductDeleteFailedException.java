package papapizza.inventory;

/**
 * Exception thrown by "deleteProduct" method in
 * {@link papapizza.inventory.controller.ShopInventoryController}
 * when
 */
//just there for convenience when delete Method in ShopInventoryController fails
public class ShopCatalogProductDeleteFailedException extends RuntimeException{
	String errorMessage;

	public ShopCatalogProductDeleteFailedException(){
	}

	public ShopCatalogProductDeleteFailedException(String errorMessage){
		this.errorMessage = errorMessage;
	}
}
