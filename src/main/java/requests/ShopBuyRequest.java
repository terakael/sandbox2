package requests;

import lombok.Getter;

public class ShopBuyRequest extends MultiRequest {
	@Getter private int objectId;
	@Getter private int amount;
}
