package responses;

import processing.attackable.Player;
import requests.Request;

public class CloseShopResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		player.setShopId(0);
	}

}
