package main.responses;

import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.ClimbRequest;
import main.requests.Request;

public class ClimbResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ClimbRequest))
			return;
		
		ClimbRequest request = (ClimbRequest)req;
		
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			player.setState(PlayerState.climbing);
			player.setTickCounter(1);
			player.setSavedRequest(req);
			setResponseText("you climb the ladder...");
		}
	}

}
