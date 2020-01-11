package main.requests;

import lombok.Getter;

@Getter
public class BankDepositRequest extends Request {
	private int slot;
	private int amount;
}
