package main.requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddExpRequest extends Request {
	public AddExpRequest() {
		
	}
	
	public AddExpRequest(int playerId, int statId, int exp) {
		this.action = "addexp";
		this.id = playerId;
		this.statId = statId;
		this.exp = exp;
	}
	private int statId;
	private int exp;
}
