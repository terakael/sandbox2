package responses;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;

public class FlowerRespawnResponse extends Response {
	@Setter private int tileId;
	public FlowerRespawnResponse() {
		setAction("flower_respawn");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
