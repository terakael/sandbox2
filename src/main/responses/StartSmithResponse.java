package main.responses;

import main.processing.Player;
import main.requests.Request;

public class StartSmithResponse extends Response {
	public StartSmithResponse() {
		setAction("start_smith");
	}
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		setRecoAndResponseText(1, "you place the ore in the furnace...");
		responseMaps.addClientOnlyResponse(player, this);
	}

}
