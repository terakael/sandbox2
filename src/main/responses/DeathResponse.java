package main.responses;

import lombok.Setter;
import main.processing.attackable.Player;
import main.requests.Request;

public class DeathResponse extends Response {
	@Setter private int id;

	public DeathResponse(int id) {
		this.id = id;
		setAction("dead");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {

	}

}
