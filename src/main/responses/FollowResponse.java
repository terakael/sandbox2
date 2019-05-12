package main.responses;

import javax.websocket.Session;

import main.processing.WorldProcessor;
import main.requests.FollowRequest;
import main.requests.Request;
import main.state.Player;
import main.state.Player.PlayerState;

public class FollowResponse extends Response {

	public FollowResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client, ResponseMaps responseMaps) {
		if (!(req instanceof FollowRequest)) {
			return null;
		}
		
		FollowRequest request = (FollowRequest)req;
		System.out.println("objectId: " + request.getObjectId());
		
		Player player = WorldProcessor.playerSessions.get(client);
		player.setTargetPlayerId(request.getObjectId());
		player.setState(PlayerState.following);
		return null;
	}

}
