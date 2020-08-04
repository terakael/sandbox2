package main.requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddExpRequest extends Request {
	public AddExpRequest() {
		
	}
	
	public AddExpRequest(int playerId, int statId, double exp) {
		this.action = "addexp";
		this.id = playerId;
		this.statId = statId;
		this.exp = exp;
	}
	private int statId;
	private double exp;
}
