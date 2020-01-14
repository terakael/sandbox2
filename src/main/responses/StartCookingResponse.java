package main.responses;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

public class StartCookingResponse extends Response {
	public StartCookingResponse() {
		setAction("start_cooking");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		setRecoAndResponseText(0, "you start cooking over the fire...");
		responseMaps.addClientOnlyResponse(player, this);
	}

}
