package main.requests;

import lombok.Getter;
import lombok.Setter;
import main.types.Stats;

@Getter @Setter
public class AddExpRequest extends Request {
	public AddExpRequest() {
		
	}
	
	public AddExpRequest(int playerId, Stats stat, double exp) {
		this.action = "addexp";
		this.id = playerId;
		this.statId = stat.getValue();
		this.exp = exp;
	}
	private int statId;
	private double exp;
}
