package main.responses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.websocket.Session;

import lombok.Getter;
import lombok.Setter;
import main.Endpoint;
import main.FightManager;
import main.PlayerRequestManager;
import main.database.PlayerDao;
import main.processing.WorldProcessor;
import main.requests.PlayerRequest;
import main.requests.Request;
import main.responses.ResponseFactory;
import main.state.Player;

// this response is for trades/duels

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
	public ResponseType process(Request req, Session client, ResponseMaps responseMaps) {
		if (!(req instanceof PlayerRequest)) {
			setRecoAndResponseText(0, "funny business");
			return ResponseType.client_only;
		}
		
		Player player = WorldProcessor.playerSessions.get(client);
		PlayerRequest playerReq = (PlayerRequest)req;
		
		Session otherSession = null;
		for (Player p : WorldProcessor.playerSessions.values()) {
			if (p.getDto().getId() == playerReq.getObjectId()) {
				otherSession = p.getSession();
				break;
			}
		}
		
		if (otherSession == null) {
			setRecoAndResponseText(0, "Couldn't find opponent.");
			responseMaps.addClientOnlyResponse(player, this);
			return ResponseType.client_only;
		}
		
		
		boolean exists = PlayerRequestManager.requestExists(playerReq.getObjectId(), playerReq.getId(), playerReq.getRequestType());
		PlayerResponse otherResponse = (PlayerResponse)ResponseFactory.create(playerReq.getAction());
		
		// note the id and opponent id are switched because we're sending a response to the opponent, not the client
		otherResponse.setId(playerReq.getObjectId());
		otherResponse.setOpponentId(playerReq.getId());
		otherResponse.setOpponentName(PlayerDao.getNameFromId(playerReq.getId()));
		otherResponse.setAccepted(exists ? 1 : 0);

		Player otherPlayer = null;
		for (Player p : WorldProcessor.playerSessions.values()) {
			if (p.getDto().getId() == playerReq.getObjectId()) {
				otherPlayer = p;
				break;
			}
		}
		responseMaps.addClientOnlyResponse(otherPlayer, otherResponse);
//			otherSession.getBasicRemote().sendText(gson.toJson(otherResponse));
		
		if (exists) {
			// if the users agree to a request, then we want to clear pending requests from both parties.
			// for example:
			// player A sends trade request to player B
			// player B sends duel request to player A
			// player A accepts duel request
			// clear player B's duel request as it is accepted, and also clear player A's trade request as it is old
			PlayerRequestManager.removeRequest(playerReq.getObjectId());
			PlayerRequestManager.removeRequest(playerReq.getId());
			
			if (playerReq.getRequestType() == PlayerRequestManager.PlayerRequestType.duel) {
				FightManager.addFight(playerReq.getId(), playerReq.getObjectId());
			}
		}
		else {
			PlayerRequestManager.addRequest(playerReq.getId(), playerReq.getObjectId(), playerReq.getRequestType());
		}
		
		
		
		return ResponseType.no_response;
	}

}
