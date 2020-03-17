package main.responses;

import main.processing.Player;
import main.requests.Request;

public class StartFishingResponse extends Response {
	public StartFishingResponse() {
		setAction("start_fishing");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {		
		responseMaps.addClientOnlyResponse(player, this);
	}
}
