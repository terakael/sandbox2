package main.requests;

import lombok.Getter;

public class ShopSellRequest extends Request {
	@Getter private int objectId;
	@Getter private int amount;
}
