package main.requests;

import lombok.Getter;

public class ShopBuyRequest extends Request {
	@Getter private int objectId;
	@Getter private int amount;
}
