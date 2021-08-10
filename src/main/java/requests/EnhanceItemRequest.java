package requests;

import lombok.Getter;

@Getter
public class EnhanceItemRequest extends MultiRequest {
	private int itemId;
	private int amount;
}
