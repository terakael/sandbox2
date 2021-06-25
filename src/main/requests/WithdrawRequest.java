package main.requests;

import lombok.Getter;

@Getter
public class WithdrawRequest extends MultiRequest {
	private int slot;
	private int amount;
	private int tileId;
}
