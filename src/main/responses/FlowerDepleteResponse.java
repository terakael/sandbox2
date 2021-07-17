package main.responses;

import lombok.Setter;
import main.processing.attackable.Player;
import main.requests.Request;

public class FlowerDepleteResponse extends Response {
	@Setter private int tileId;
	
	public FlowerDepleteResponse() {
		setAction("flower_deplete");
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
