package main.responses;

import lombok.Setter;
import main.processing.attackable.Player;
import main.requests.Request;

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
