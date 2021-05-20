package main.responses;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
	// TODO this is stupid
	private static final Set<Integer> climbUpScenery = new HashSet<>(Arrays.asList(60, 119));
	private static final Set<Integer> climbDownScenery = new HashSet<>(Arrays.asList(50, 117, 118));

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
			if (!climbUpScenery.contains(sceneryId) && !climbDownScenery.contains(sceneryId)) { // up, down ladders
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
			
			int destFloor = player.getFloor();
			if (climbUpScenery.contains(sceneryId)) {
				++destFloor;
			} else {
				--destFloor;
			}
			
			player.setFloor(destFloor);
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
