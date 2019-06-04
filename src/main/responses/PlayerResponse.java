package main.responses;

import lombok.Getter;
import lombok.Setter;
import main.FightManager;
import main.PlayerRequestManager;
import main.database.PlayerDao;
import main.processing.Player;
import main.processing.WorldProcessor;
import main.requests.PlayerRequest;
import main.requests.Request;
import main.responses.ResponseFactory;

// this response is for trades/duels

@Setter @Getter
public abstract class PlayerResponse extends Response {
	int id;
	int opponentId;
	String opponentName;
	int accepted;

	protected PlayerResponse() {}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof PlayerRequest)) {
			setRecoAndResponseText(0, "funny business");
			return;
		}

		PlayerRequest playerReq = (PlayerRequest)req;
		
		Player otherPlayer = WorldProcessor.getPlayerById(playerReq.getObjectId());
		if (otherPlayer == null) {
			setRecoAndResponseText(0, "Couldn't find opponent.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}		
		
		boolean exists = PlayerRequestManager.requestExists(playerReq.getObjectId(), playerReq.getId(), playerReq.getRequestType());
		PlayerResponse otherResponse = (PlayerResponse)ResponseFactory.create(playerReq.getAction());
		
		// note the id and opponent id are switched because we're sending a response to the opponent, not the client
		otherResponse.setId(playerReq.getObjectId());
		otherResponse.setOpponentId(playerReq.getId());
		otherResponse.setOpponentName(PlayerDao.getNameFromId(playerReq.getId()));
		otherResponse.setAccepted(exists ? 1 : 0);

		responseMaps.addClientOnlyResponse(otherPlayer, otherResponse);
		
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
				FightManager.addFight(WorldProcessor.getPlayerById(playerReq.getId()), WorldProcessor.getPlayerById(playerReq.getObjectId()));
			}
		}
		else {
			PlayerRequestManager.addRequest(playerReq.getId(), playerReq.getObjectId(), playerReq.getRequestType());
		}
	}

}
