package papapizza.inventory;

/**
 *enum with the different categories a {@link org.salespointframework.catalog.Product} (and it's inheritors) can have
 */

public enum ProductCategory {
	//XXX use enums instead of strings, affects: a lot lol, but not that hard to fix
	TOPPING, CUSTOM_PIZZA, KITCHEN_PIZZA, OVEN, DISHSET, CONSUMABLE, PIZZA, DRINK, VEHICLE, DELETED
}
