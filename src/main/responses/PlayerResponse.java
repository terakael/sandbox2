package main.responses;

import java.io.IOException;

import javax.websocket.Session;

import lombok.Getter;
import lombok.Setter;
import main.Endpoint;
import main.PlayerRequestManager;
import main.database.PlayerDao;
import main.requests.PlayerRequest;
import main.requests.Request;
import main.responses.ResponseFactory;

@Setter @Getter
public abstract class PlayerResponse extends Response {
	int id;
	int opponentId;
	String opponentName;
	int accepted;

	public PlayerResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client) {
		if (!(req instanceof PlayerRequest)) {
			setRecoAndResponseText(0, "funny business");
			return ResponseType.client_only;
		}
		
		PlayerRequest playerReq = (PlayerRequest)req;
		
		
		Session otherSession = Endpoint.getSessionByPlayerId(playerReq.getObjectId());
		if (otherSession == null) {
			setRecoAndResponseText(0, "Couldn't find opponent.");
			return ResponseType.client_only;
		}
		
		
		boolean exists = PlayerRequestManager.requestExists(playerReq.getObjectId(), playerReq.getId(), playerReq.getRequestType());
		PlayerResponse otherResponse = (PlayerResponse)ResponseFactory.create(playerReq.getAction());
		
		// note the id and opponent id are switched because we're sending a response to the opponent, not the client
		otherResponse.setId(playerReq.getObjectId());
		otherResponse.setOpponentId(playerReq.getId());
		otherResponse.setOpponentName(PlayerDao.getNameFromId(playerReq.getId()));
		otherResponse.setAccepted(exists ? 1 : 0);
		try {
			otherSession.getBasicRemote().sendText(gson.toJson(otherResponse));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (exists) {
			// if the users agree to a request, then we want to clear pending requests from both parties.
			// for example:
			// player A sends trade request to player B
			// player B sends duel request to player A
			// player A accepts duel request
			// clear player B's duel request as it is accepted, and also clear player A's trade request as it is old
			PlayerRequestManager.removeRequest(playerReq.getObjectId());
			PlayerRequestManager.removeRequest(playerReq.getId());
		}
		else
			PlayerRequestManager.addRequest(playerReq.getId(), playerReq.getObjectId(), playerReq.getRequestType());
		
		
		
		return ResponseType.no_response;
	}

}
