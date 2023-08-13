package requests;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConstructionRequest extends Request {
	public ConstructionRequest() {
		setAction("construction");
	}
	private int sceneryId;
	private boolean flatpack;
	private int amount = 1; // in the case of a flatpack we can make several
}
