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
		player.setTarget(WorldProcessor.getPlayerById(((FollowRequest)req).getObjectId()));
		player.setState(PlayerState.chasing);
	}

}
