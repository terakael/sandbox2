package requests;

import lombok.Getter;
import lombok.Setter;

public class SmithRequest extends Request {
	@Getter private int itemId;
	@Setter @Getter private int amount; // number of times to repeat the action
}
