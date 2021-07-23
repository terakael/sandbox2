package responses;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;

public class SceneryDepleteResponse extends Response {
	@Setter private int tileId;
	public SceneryDepleteResponse() {
		setAction("scenery_deplete");
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
		
	}

}
