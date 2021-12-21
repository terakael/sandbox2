package responses;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;

// TODO deprecate in favour of PlayerUpdateResponse
public class PlayerEnterResponse extends Response {
	@Setter private String name;

	public PlayerEnterResponse() {
		setAction("player_enter");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		name = player.getDto().getName();
		responseMaps.addBroadcastResponse(this, player);
	}
}
