package requests;

import lombok.Getter;
import lombok.Setter;

public class AttackRequest extends Request {
	@Setter @Getter private int objectId;
}
