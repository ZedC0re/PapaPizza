package papapizza.customer;

import lombok.Getter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.List;
import java.util.Objects;

@Entity
public class Tan{
	private static final int MIN_TAN = 100000;
	private static final int MAX_TAN = 1000000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Getter
	private long id;

	@Getter
	private int tanNumber;

	public Tan(){}

	public Tan(List<Tan> inUseTans){
		do{
			tanNumber = getRandomInt();
		}while(inUseTans.contains(this)); //repeat if alr exist
	}

	private static int getRandomInt(){
		return (int)(Math.random() * (MAX_TAN - MIN_TAN + 1)) + MIN_TAN; //real 6 digit rng
	}

	//TANs are equal when their int numbers match
	@Override
	@java.lang.SuppressWarnings("squid:L40")
	public boolean equals(Object o) {
		if (this == o){
			return true;
		}
		if (o == null || getClass() != o.getClass()){
			return false;
		}
		Tan tan1 = (Tan) o;
		return tanNumber == tan1.tanNumber;
	}

	@Override
	public int hashCode() {
		return Objects.hash(tanNumber);
	}

	@Override
	public String toString() {
		return Integer.toString(tanNumber);
	}

	public boolean equalsInt(int tan){
		return tan==this.getTanNumber();
	}
}
