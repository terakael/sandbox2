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

public class ClimbResponse extends WalkAndDoResponse {
	private transient int sceneryId;
	
	@Override
	protected boolean setTarget(Request request, Player player, ResponseMaps responseMaps) {
		sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
		if (!ClimbableDao.isClimbable(sceneryId)) {
			setResponseText("you can't climb that.");
			responseMaps.addClientOnlyResponse(player, this);
			return false;
		}
		
		walkingTargetTileId = request.getTileId();
		return true;
	}

	@Override
	protected void doAction(Request request, Player player, ResponseMaps responseMaps) {
		if (request.getTileId() == 872318557) { // special case: monastery ladder, requires 31 prayer
			if (StatsDao.getStatLevelByStatIdPlayerId(Stats.PRAYER, player.getId()) < 31) {
				setResponseText("you need 31 prayer before the monks allow you to the upper floor.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
		}
		
		final int relativeFloor = ClimbableDao.getRelativeFloorsBySceneryId(sceneryId);
		player.setFloor(player.getFloor() + relativeFloor, responseMaps);
		player.setTileId(request.getTileId() + PathFinder.LENGTH);
		player.clearPath();
		
		PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
		playerUpdate.setId(player.getId());
		playerUpdate.setTileId(player.getTileId());
		playerUpdate.setRelativeFloor(relativeFloor);
		playerUpdate.setSnapToTile(true);
		responseMaps.addClientOnlyResponse(player, playerUpdate);

		player.setState(PlayerState.idle);
	}

}
