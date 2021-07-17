package main.responses;

import main.processing.WorldProcessor;
import main.processing.attackable.Player;
import main.processing.attackable.Player.PlayerState;
import main.processing.managers.FightManager;
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
		
		if (FightManager.fightWithFighterIsBattleLocked(player))
			return;
		FightManager.cancelFight(player, responseMaps);
		
		FollowRequest request = (FollowRequest)req;
		
		player.setTarget(WorldProcessor.getPlayerById(request.getObjectId()));
		player.setState(PlayerState.following);
	}

}
