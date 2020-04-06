package main.responses;

import main.database.SceneryDao;
import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.EnterPortalRequest;
import main.requests.Request;

public class EnterPortalResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof EnterPortalRequest))
			return;
		
		EnterPortalRequest request = (EnterPortalRequest)req;
		
		int teleTileId =  0;
		int teleFloor = 0;
		
		int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
		switch (sceneryId) {
		case 85:// blue
			teleTileId = 469593844;
			teleFloor = -1;
			break;
			
		case 86:// red
			teleTileId = 469493964;
			teleFloor = 0;
			break;
			
		case 87:// yellow
			teleTileId = 470568804;
			teleFloor = 0;
			break;
		
		default:
			return;
		}
		
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
			player.setTileId(teleTileId);
			player.setFloor(teleFloor);
			player.clearPath();
			
			PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
			playerUpdate.setId(player.getId());
			playerUpdate.setTileId(player.getTileId());
			playerUpdate.setSnapToTile(true);
			responseMaps.addClientOnlyResponse(player, playerUpdate);
		}
	}

}
