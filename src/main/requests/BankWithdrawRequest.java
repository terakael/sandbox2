package main.requests;

import lombok.Getter;

@Getter
public class BankWithdrawRequest extends Request {
	private int slot;
	private int amount;
}
