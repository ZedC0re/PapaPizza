package papapizza.inventory.items;

/**
 * States that an {@link PizzaProduct} can have in {@link papapizza.kitchen.KitchenManagement}.
 */
public enum PizzaState {
	OPEN,
	PENDING, //in oven
	READY,

}