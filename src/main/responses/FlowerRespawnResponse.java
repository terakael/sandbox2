package main.responses;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

public class FlowerRespawnResponse extends Response {
	@Setter private int tileId;
	public FlowerRespawnResponse() {
		setAction("flower_respawn");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
