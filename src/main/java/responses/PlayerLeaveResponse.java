package responses;

import lombok.Setter;
import processing.attackable.Player;
import requests.PlayerLeaveRequest;
import requests.Request;

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
