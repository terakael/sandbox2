package main.responses;

import main.database.SceneryDao;
import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.ClimbRequest;
import main.requests.Request;

public class FinishClimbResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ClimbRequest))
			return;
		
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		ClimbRequest request = (ClimbRequest)req;
		int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
		if (sceneryId != 50 && sceneryId != 60) // up, down ladders
			return;
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			
			int destFloor = player.getFloor();
			switch (sceneryId) {
			case 50:
				--destFloor;
				break;
			case 60:
				++destFloor;
				break;
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
