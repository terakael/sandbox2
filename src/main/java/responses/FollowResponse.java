package responses;

import processing.WorldProcessor;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.FightManager;
import requests.FollowRequest;
import requests.Request;

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
