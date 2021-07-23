package responses;

import processing.attackable.Player;
import requests.Request;

public class UnknownResponse extends Response {

	protected UnknownResponse() {}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		assert(false);
		setRecoAndResponseText(0, "Unknown action.");
	}
}
