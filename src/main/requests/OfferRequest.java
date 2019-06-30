package main.requests;

import lombok.Getter;

@Getter
public class OfferRequest extends Request {
	private int objectId;
	private int slot;
	private int amount;
}
