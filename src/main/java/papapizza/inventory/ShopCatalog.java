package papapizza.inventory;

import org.salespointframework.catalog.Catalog;
import org.salespointframework.catalog.Product;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/**
 * Repository to store products for Inventory
 */
@Repository
public interface ShopCatalog extends Catalog<Product> {

	Sort DEFAULT_SORT = Sort.by("productIdentifier").descending();


}
