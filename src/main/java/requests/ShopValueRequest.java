package requests;

import lombok.Getter;

public class ShopValueRequest extends MultiRequest {
	@Getter private int objectId;
	@Getter private int valueTypeId; // 0 is buy-value (from shop), 1 is sell-value (from inventory)
}
