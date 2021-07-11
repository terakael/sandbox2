package main.responses;

import main.processing.Player;
import main.requests.Request;

public class SceneryDespawnResponse extends Response {
	private int tileId;
	public SceneryDespawnResponse(int tileId) {
		setAction("scenery_despawn");
		this.tileId = tileId;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
	}

}
