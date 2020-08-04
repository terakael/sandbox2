package main.requests;

import lombok.Getter;

@Getter
public class OfferRequest extends MultiRequest {
	private int objectId;
	private int slot;
	private int amount;
}
