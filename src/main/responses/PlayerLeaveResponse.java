package main.responses;

import lombok.Setter;
import main.processing.Player;
import main.requests.PlayerLeaveRequest;
import main.requests.Request;

public class PlayerLeaveResponse extends Response {
	@Setter private String name;
	
	public PlayerLeaveResponse() {
		setAction("player_leave");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {		
		if (!(req instanceof PlayerLeaveRequest)) {
			return;
		}
		
		PlayerLeaveRequest request = (PlayerLeaveRequest)req;
		name = request.getName();
		responseMaps.addBroadcastResponse(this);
	}

}
