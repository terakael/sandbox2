package main.responses;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

// TODO deprecate in favour of PlayerUpdateResponse
public class PlayerEnterResponse extends Response {
	@Setter private String name;

	public PlayerEnterResponse() {
		setAction("playerEnter");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		name = player.getDto().getName();
		responseMaps.addBroadcastResponse(this, player);
	}
}
