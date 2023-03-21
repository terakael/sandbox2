package requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class GetHouseInfoRequest extends Request {
	private Integer currentHouseId = null;
	private int direction; // 0=first, 1=prev, 2=next, 3=last, 4=same
}