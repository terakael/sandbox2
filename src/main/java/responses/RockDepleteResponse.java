package responses;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;

public class RockDepleteResponse extends Response {
	@Setter private int tileId;
	public RockDepleteResponse() {
		setAction("rock_deplete");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
