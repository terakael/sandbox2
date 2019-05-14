package main.responses;

import javax.websocket.Session;

import main.processing.Player;
import main.processing.WorldProcessor;
import main.processing.Player.PlayerState;
import main.requests.FollowRequest;
import main.requests.Request;

public class FollowResponse extends Response {

	public FollowResponse(String action) {
		super(action);
	}

	@Override
	public void process(Request req, Session client, ResponseMaps responseMaps) {
		if (!(req instanceof FollowRequest)) {
			return;
		}
		
		FollowRequest request = (FollowRequest)req;
		System.out.println("objectId: " + request.getObjectId());
		
		Player player = WorldProcessor.playerSessions.get(client);
		player.setTargetPlayerId(request.getObjectId());
		player.setState(PlayerState.following);
	}

}
