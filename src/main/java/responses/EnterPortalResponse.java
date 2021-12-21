package responses;

import database.dao.ConstructableDao;
import database.dao.SceneryDao;
import database.dao.TeleportableDao;
import database.dto.TeleportableDto;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.FightManager;
import requests.EnterPortalRequest;
import requests.Request;

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
			teleTileId = 870141319;
			teleFloor = -1;
			break;
			
		case 86:// red
			teleTileId = 869956139;
			teleFloor = 0;
			break;
			
		case 87:// yellow
			teleTileId = 871947954;
			teleFloor = 0;
			break;
			
		// constructables have the rune as the tertiary, so we can get the coords from there
		case 143: // tyrotown tele portal
			final int teleRuneId = ConstructableDao.getConstructableBySceneryId(sceneryId).getTertiaryId();
			TeleportableDto teleportable = TeleportableDao.getTeleportableByItemId(teleRuneId);
			teleTileId = teleportable.getTileId();
			teleFloor = teleportable.getFloor();
			break;
		
		default:
			return; // scenery doesn't exist or isn't a portal, so bail
		}
		
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		if (player.getTileId() != request.getTileId()) { // must be standing on the portal tile to teleport
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), true));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {	
			player.setTileId(teleTileId);
			player.setFloor(teleFloor, responseMaps);
			player.clearPath();
			
			PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
			playerUpdate.setId(player.getId());
			playerUpdate.setTileId(player.getTileId());
			playerUpdate.setSnapToTile(true);
			responseMaps.addClientOnlyResponse(player, playerUpdate);
		}
	}

}
