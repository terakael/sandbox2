package main.responses;

import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.FollowRequest;
import main.requests.Request;

public class FollowResponse extends Response {

	public FollowResponse() {
		setAction("follow");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof FollowRequest)) {
			return;
		}
		
		FollowRequest request = (FollowRequest)req;
		System.out.println("objectId: " + request.getObjectId());
		
		player.setTargetPlayerId(request.getObjectId());
		player.setState(PlayerState.following);
	}

}
