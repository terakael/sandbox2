package main.responses;

import main.database.dao.DoorDao;
import main.database.dao.SceneryDao;
import main.database.dto.DoorDto;
import main.processing.ConstructableManager;
import main.processing.FightManager;
import main.processing.Player;
import main.requests.OpenRequest;
import main.requests.Request;

public class OpenResponse extends Response {
	// this is a controller response for any scenery that can be opened:
	// doors -> OpenCloseResponse
	// bank chest -> BankResponse
	// storage chest -> OpenStorageChestResponse
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		OpenRequest request = (OpenRequest)req;
		
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		DoorDto door = DoorDao.getDoorDtoByTileId(player.getFloor(), request.getTileId());
		if (door != null) {
//			handleDoor(door, request, player, responseMaps);
			new OpenCloseResponse().process(req, player, responseMaps);
			return;
		}
		
		int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
		
		switch (sceneryId) {
		case 53: // bank chest
			new BankResponse().process(req, player, responseMaps);
			return;
			
		case 141: // small storage chest
		case 147: // large storage chest
			new OpenStorageChestResponse().process(req, player, responseMaps);
			return;
			
		case 148: // shadow chest
			new OpenShadowChestResponse().process(req, player, responseMaps);
			return;
		}
	}

}
