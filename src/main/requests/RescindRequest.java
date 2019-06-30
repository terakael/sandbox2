package main.requests;

import lombok.Getter;

@Getter
public class RescindRequest extends Request {
	private int objectId;
	private int slot;
	private int amount;
}
