package main.requests;

import lombok.Getter;

@Getter
public class BankDepositRequest extends MultiRequest {
	private int slot;
	private int amount;
}
