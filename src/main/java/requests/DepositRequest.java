package requests;

import lombok.Getter;

@Getter
public class DepositRequest extends MultiRequest {
	private int slot;
	private int amount;
}
