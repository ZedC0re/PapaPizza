package papapizza.customer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
public class Customer{
	public enum Meta{
		DELETE_LINK_CUSTOMER, NORMAL//, DELETED, DISABLED
	}


	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column
	@Setter(AccessLevel.NONE)
	private long id;

	@Column
	private String address;

	@Column//(unique = true)
	private String phone;

	@Column
	private String firstname;

	@Column
	private String lastname;

	@OneToOne(cascade = {CascadeType.ALL})
	private Tan currentTan;

	@OneToOne(cascade = {CascadeType.ALL})
	private Tan oldTan;

	@Column
	@Enumerated(EnumType.STRING)
	private Meta meta = Meta.NORMAL;

	@OneToMany(cascade = {CascadeType.ALL})
	@Setter(AccessLevel.NONE)
	private List<LentDishset> lentDishsets = new ArrayList<>();

	public Customer(){
		//just hibernate things
	}

	public int getTanNumber(){
		return currentTan.getTanNumber();
	}

	public int getDishsetSize(){
		return lentDishsets.size();
	}

	@Override
	public String toString() {
		return "Customer{" +
				"id=" + id +
				", address='" + address + '\'' +
				", phone='" + phone + '\'' +
				", firstname='" + firstname + '\'' +
				", lastname='" + lastname + '\'' +
				", currentTan=" + currentTan +
				", lentDishsets=" + lentDishsets +
				", meta=" + meta +
				'}';
	}

	public long getValidDishsetsSize(){
		return lentDishsets.stream().filter(lentDishset -> lentDishset.getRemainingTimeSec() > 0).count();
	}

	public long getInvalidDishsetsSize(){
		return lentDishsets.stream().filter(lentDishset -> lentDishset.getRemainingTimeSec() <= 0).count();
	}
}
