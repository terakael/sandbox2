package main.responses;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

public class SceneryRespawnResponse extends Response {
	@Setter private int tileId;
	
	public SceneryRespawnResponse() {
		setAction("scenery_respawn");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
}
