package requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BuyHouseRequest extends Request {
	private int houseId;
}
