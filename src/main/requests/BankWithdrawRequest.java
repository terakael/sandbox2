package main.requests;

import lombok.Getter;

@Getter
public class BankWithdrawRequest extends MultiRequest {
	private int slot;
	private int amount;
}
