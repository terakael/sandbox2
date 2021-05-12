package main.responses;

import main.database.SceneryDao;
import main.database.StatsDao;
import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.ClimbRequest;
import main.requests.Request;
import main.types.Stats;

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
			player.faceDirection(request.getTileId(), responseMaps);
			
			int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
			if (sceneryId != 50 && sceneryId != 60) { // up, down ladders
				setResponseText("you can't climb that.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			if (request.getTileId() == 872318557) { // special case: monastery ladder, requires 31 prayer
				if (StatsDao.getStatLevelByStatIdPlayerId(Stats.PRAYER, player.getId()) < 31) {
					setResponseText("you need 31 prayer before the monks allow you to the upper floor.");
					responseMaps.addClientOnlyResponse(player, this);
					return;
				}
			}
			
			player.setState(PlayerState.climbing);
			player.setTickCounter(1);
			player.setSavedRequest(req);
		}
	}

}
