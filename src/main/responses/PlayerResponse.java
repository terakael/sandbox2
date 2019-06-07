package main.responses;

import lombok.Getter;
import lombok.Setter;
import main.FightManager;
import main.PlayerRequestManager;
import main.database.PlayerDao;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.WorldProcessor;
import main.processing.Player.PlayerState;
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
		
		// you have to be next to the player to process the request
		if (!PathFinder.isNextTo(player.getTileId(), otherPlayer.getTileId())) {
			player.setSavedRequest(req);
			player.setState(PlayerState.chasing);
			player.setTarget(otherPlayer);
			return;
		}
		
		boolean exists = PlayerRequestManager.requestExists(playerReq.getObjectId(), playerReq.getId(), playerReq.getRequestType());
		PlayerResponse otherResponse = (PlayerResponse)ResponseFactory.create(playerReq.getAction());
		
		// note the id and opponent id are switched because we're sending a response to the opponent, not the client
		otherResponse.setId(playerReq.getObjectId());
		otherResponse.setOpponentId(playerReq.getId());
		otherResponse.setOpponentName(PlayerDao.getNameFromId(playerReq.getId()));
		otherResponse.setAccepted(exists ? 1 : 0);
		
		String responseText = exists 
				? String.format("%s accepted the %s.", player.getDto().getName(), playerReq.getRequestType())
				: String.format("%s wishes to %s with you.", player.getDto().getName(), playerReq.getRequestType());
		otherResponse.setRecoAndResponseText(1, responseText);
		otherResponse.setMessageColour("#f0f");

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
				Player player1 = WorldProcessor.getPlayerById(playerReq.getId());
				Player player2 = WorldProcessor.getPlayerById(playerReq.getObjectId());
				FightManager.addFight(player1, player2);
				
				player1.setTileId(player2.getTileId());
				
				PvpStartResponse pvpStart = new PvpStartResponse();
				pvpStart.setPlayer1Id(player1.getId());
				pvpStart.setPlayer2Id(player2.getId());
				pvpStart.setTileId(player2.getTileId());
				responseMaps.addBroadcastResponse(pvpStart);
			}
		}
		else {
			setRecoAndResponseText(1, String.format("sending %s request...", playerReq.getRequestType()));
			responseMaps.addClientOnlyResponse(player, this);
			PlayerRequestManager.addRequest(playerReq.getId(), playerReq.getObjectId(), playerReq.getRequestType());
		}
	}

}
