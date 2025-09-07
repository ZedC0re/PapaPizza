package papapizza.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface CustomerRepository extends JpaRepository<Customer, Long> {

	@Query(value="SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'CUSTOMER'", nativeQuery = true)
	List<String> findAllColumnNames();

	List<Customer> findByPhone(String phone);

	List<Customer> findByMeta(Customer.Meta meta);
}