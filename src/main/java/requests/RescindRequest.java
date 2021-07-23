package requests;

import lombok.Getter;

@Getter
public class RescindRequest extends MultiRequest {
	private int objectId;
	private int slot;
	private int amount;
}
