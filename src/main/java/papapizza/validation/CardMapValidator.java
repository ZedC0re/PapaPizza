package papapizza.validation;

import org.springframework.beans.factory.annotation.Autowired;
import papapizza.inventory.ShopCatalogManagement;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Map;

public class CardMapValidator implements ConstraintValidator<ValidCardMap, Map<String,Integer>> {

	@Autowired
	ShopCatalogManagement catalogManagement;

	@Override
	public boolean isValid(Map<String, Integer> map, ConstraintValidatorContext constraintValidatorContext) {
		for (Map.Entry<String,Integer> entry : map.entrySet()){
			if(entry.getValue() < 0 || entry.getValue() > 500) {
				return false;
			}
			if(catalogManagement.findByName(entry.getKey()) == null){
				return false;
			}
		}
		return true;
	}
}
