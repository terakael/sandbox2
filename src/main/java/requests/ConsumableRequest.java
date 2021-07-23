package requests;

import lombok.Getter;

public abstract class ConsumableRequest extends Request {
	@Getter private int objectId;
	@Getter private int slot;
}
