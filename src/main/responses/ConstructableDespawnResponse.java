package main.responses;

import main.processing.Player;
import main.requests.Request;

public class ConstructableDespawnResponse extends Response {
	private int tileId;
	public ConstructableDespawnResponse(int tileId) {
		setAction("constructable_despawn");
		this.tileId = tileId;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
	}

}
