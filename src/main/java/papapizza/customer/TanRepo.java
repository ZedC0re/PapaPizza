package papapizza.customer;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.util.Streamable;

public interface TanRepo extends CrudRepository<Tan, Long> {
	@Override
	Streamable<Tan> findAll();
}
