package main.responses;

import main.processing.Player;
import main.requests.Request;

public class StartUseResponse extends Response {
	public StartUseResponse() {
		setAction("start_use");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		setResponseText("you use the item...");
		responseMaps.addClientOnlyResponse(player, this);
	}

}
