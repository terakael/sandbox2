package main.responses;

import main.processing.attackable.Player;
import main.requests.Request;

public class CloseShopResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		player.setShopId(0);
	}

}
