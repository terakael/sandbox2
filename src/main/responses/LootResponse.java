package main.responses;

import main.database.dao.SceneryDao;
import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.LootRequest;
import main.requests.Request;

public class LootResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		if (!(req instanceof LootRequest))
			return;
		
		LootRequest request = (LootRequest)req;
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {			
			player.faceDirection(request.getTileId(), responseMaps);
			
			int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
			if (sceneryId == 156) { // necrotic chest
				// TODO lewtz
				
				int graveyardEntranceFloor = 0;
				int graveyardEntranceTileId = 937916240;
				
				// send teleport explosions to both where the player teleported from, and where they're teleporting to
				// that way players on both sides of the teleport will see it
				responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), new TeleportExplosionResponse(player.getTileId()));
				responseMaps.addLocalResponse(graveyardEntranceFloor, graveyardEntranceTileId, new TeleportExplosionResponse(graveyardEntranceTileId));
				
				PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
				playerUpdate.setId(player.getId());
				playerUpdate.setTileId(graveyardEntranceTileId);
				playerUpdate.setSnapToTile(true);
				
				responseMaps.addClientOnlyResponse(player, playerUpdate);
				responseMaps.addLocalResponse(graveyardEntranceFloor, graveyardEntranceTileId, playerUpdate);
				
				player.setFloor(graveyardEntranceFloor, responseMaps);
				player.setTileId(graveyardEntranceTileId);
				
				player.clearPath();
			}
		}
	}

}
