package responses;

import processing.attackable.Player;
import requests.Request;

@SuppressWarnings("unused")
public class SceneryDespawnResponse extends Response {
	private int tileId;
	public SceneryDespawnResponse(int tileId) {
		setAction("scenery_despawn");
		this.tileId = tileId;
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
	}

}
