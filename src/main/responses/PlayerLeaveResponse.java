package main.responses;

import lombok.Getter;
import lombok.Setter;
import main.processing.Player;
import main.requests.PlayerLeaveRequest;
import main.requests.Request;

public class PlayerLeaveResponse extends Response {
	
	@Getter @Setter private int id;
	@Setter private String name;
	
	public PlayerLeaveResponse() {
		setAction("playerLeave");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {		
		if (!(req instanceof PlayerLeaveRequest)) {
			return;
		}
		
		PlayerLeaveRequest request = (PlayerLeaveRequest)req;
		id = request.getId();
		name = request.getName();
		responseMaps.addBroadcastResponse(this);
	}

}
