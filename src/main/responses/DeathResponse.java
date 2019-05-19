package main.responses;

import lombok.Getter;
import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

public class DeathResponse extends Response {
	@Getter @Setter private int id;
	@Getter @Setter private int tileId;
	@Setter private int currentHp;

	public DeathResponse() {
		setAction("dead");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {

	}

}
