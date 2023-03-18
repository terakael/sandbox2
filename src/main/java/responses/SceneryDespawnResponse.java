package responses;

import processing.attackable.Player;
import requests.Request;

@SuppressWarnings("unused")
public class SceneryDespawnResponse extends Response {
	private int sceneryId;
	private int tileId;
	public SceneryDespawnResponse(int sceneryId, int tileId) {
		setAction("scenery_despawn");
		// now that walls are separated from scenery, we can have multiple "scenery" on the same tile.
		// therefore we can no longer blindly pass in a tileId, because if someone lights a fire next to
		// the wall, when the fire despawns then the wall would despawn with it.
		this.sceneryId = sceneryId; 
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
