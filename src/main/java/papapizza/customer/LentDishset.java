package papapizza.customer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import papapizza.inventory.items.DishsetProduct;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
public class LentDishset{

	public static final int RETURN_TIME = 1209600; //14 days in sec

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column
	@Setter(AccessLevel.NONE)
	private long id;

	@Column
	private LocalDateTime dateLent;

	@ManyToOne
	private DishsetProduct dishsetType;

	@Column
	private long validUntilEpoch;

	public LentDishset(){} //just hibernate things

	public LentDishset(@NonNull LocalDateTime dateLent, @NonNull DishsetProduct dishsetType) {
		this.dateLent = dateLent;
		this.dishsetType = dishsetType;
		validUntilEpoch = dateLent.toEpochSecond(OffsetDateTime.now().getOffset()) + RETURN_TIME;
	}

	/**
	 * gives the remaining time until the dishset may no longer be returned (in seconds)
	 * based the current system time
	 * @return int - remaining time
	 */
	public int getRemainingTimeSec(){
		return (int)(validUntilEpoch - System.currentTimeMillis() / 1000L);
	}
}
