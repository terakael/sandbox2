package main.responses;

import lombok.Getter;
import lombok.Setter;
import main.PlayerRequestManager;
import main.PlayerRequestManager.PlayerRequestType;
import main.database.dao.PlayerDao;
import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.processing.TradeManager;
import main.processing.WorldProcessor;
import main.requests.PlayerRequest;
import main.requests.Request;
import main.types.DuelRules;

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
		
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}

		PlayerRequest playerReq = (PlayerRequest)req;
		
		Player otherPlayer = WorldProcessor.getPlayerById(playerReq.getObjectId());
		if (otherPlayer == null) {
			setRecoAndResponseText(0, "Couldn't find opponent.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (player.getFloor() != otherPlayer.getFloor())
			return;
		
		// you have to be next to the player to process the request
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), otherPlayer.getTileId())) {
			player.setSavedRequest(req);
			player.setState(PlayerState.chasing);
			player.setTarget(otherPlayer);
			return;
		}
		
		player.faceDirection(otherPlayer.getTileId(), responseMaps);
		
		boolean exists = PlayerRequestManager.requestExists(playerReq.getObjectId(), player.getId(), playerReq.getRequestType());
		PlayerResponse otherResponse = (PlayerResponse)ResponseFactory.create(playerReq.getAction());
		
		// note the id and opponent id are switched because we're sending a response to the opponent, not the client
		otherResponse.setId(playerReq.getObjectId());
		otherResponse.setOpponentId(player.getId());
		otherResponse.setOpponentName(PlayerDao.getNameFromId(player.getId()));
		otherResponse.setAccepted(exists ? 1 : 0);
		
		String responseText = exists 
				? String.format("%s accepted the %s.", player.getDto().getName(), playerReq.getRequestType())
				: String.format("%s wishes to %s with you.", player.getDto().getName(), playerReq.getRequestType());
		otherResponse.setRecoAndResponseText(1, responseText);
		otherResponse.setColour("#f0f");

		responseMaps.addClientOnlyResponse(otherPlayer, otherResponse);
		
		if (exists) {
			// if the users agree to a request, then we want to clear pending requests from both parties.
			// for example:
			// player A sends trade request to player B
			// player B sends duel request to player A
			// player A accepts duel request
			// clear player B's duel request as it is accepted, and also clear player A's trade request as it is old
			PlayerRequestManager.removeRequest(playerReq.getObjectId());
			PlayerRequestManager.removeRequest(player.getId());
			
			final boolean isDuel = playerReq.getRequestType() == PlayerRequestType.duel;
			Player player1 = WorldProcessor.getPlayerById(player.getId());
			Player player2 = WorldProcessor.getPlayerById(playerReq.getObjectId());
			
			AcceptTradeResponse player1TradeResponse = new AcceptTradeResponse();
			player1TradeResponse.setOtherPlayerId(player2.getId());
			if (isDuel)
				player1TradeResponse.setDuelRules(DuelRules.asMap());
			responseMaps.addClientOnlyResponse(player1, player1TradeResponse);
			
			AcceptTradeResponse player2TradeResponse = new AcceptTradeResponse();
			player2TradeResponse.setOtherPlayerId(player1.getId());
			if (isDuel)
				player2TradeResponse.setDuelRules(DuelRules.asMap());
			responseMaps.addClientOnlyResponse(player2, player2TradeResponse);
			
			TradeManager.addTrade(player1, player2, isDuel);
		}
		else {
			setRecoAndResponseText(1, String.format("sending %s request...", playerReq.getRequestType()));
			responseMaps.addClientOnlyResponse(player, this);
			PlayerRequestManager.addRequest(player.getId(), playerReq.getObjectId(), playerReq.getRequestType());
		}
	}

}
