package responses;

import database.dao.ClimbableDao;
import database.dao.SceneryDao;
import database.dao.StatsDao;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.FightManager;
import requests.ClimbRequest;
import requests.Request;
import types.Stats;

public class ClimbResponse extends Response {
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ClimbRequest))
			return;
		
		ClimbRequest request = (ClimbRequest)req;
		
//		if (FightManager.fightWithFighterIsBattleLocked(player)) {
//			setRecoAndResponseText(0, "you can't do that during combat.");
//			responseMaps.addClientOnlyResponse(player, this);
//			return;
//		}
//		FightManager.cancelFight(player, responseMaps);
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			player.faceDirection(request.getTileId(), responseMaps);
			
			int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
			if (!ClimbableDao.isClimbable(sceneryId)) {
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
			
			player.setFloor(player.getFloor() + ClimbableDao.getRelativeFloorsBySceneryId(sceneryId), responseMaps);
			player.setTileId(request.getTileId() + PathFinder.LENGTH);
			player.clearPath();
			
			PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
			playerUpdate.setId(player.getId());
			playerUpdate.setTileId(player.getTileId());
			playerUpdate.setSnapToTile(true);
			responseMaps.addClientOnlyResponse(player, playerUpdate);

			player.setState(PlayerState.idle);
		}
	}

}
