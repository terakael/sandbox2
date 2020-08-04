package main.requests;

import lombok.Getter;

public class ShopSellRequest extends MultiRequest {
	@Getter private int objectId;
	@Getter private int amount;
}
