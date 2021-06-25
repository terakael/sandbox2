package main.requests;

import lombok.Getter;

@Getter
public class DepositRequest extends MultiRequest {
	private int slot;
	private int amount;
	private int tileId; // of the bank chest
}
