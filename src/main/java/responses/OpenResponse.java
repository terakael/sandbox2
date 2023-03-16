package responses;

import database.dao.DoorDao;
import database.dao.SceneryDao;
import database.dto.DoorDto;
import processing.attackable.Player;
import processing.managers.FightManager;
import requests.OpenRequest;
import requests.Request;

public class OpenResponse extends Response {
	// this is a controller response for any scenery that can be opened:
	// doors -> OpenCloseResponse
	// bank chest -> BankResponse
	// storage chest -> OpenStorageChestResponse
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		OpenRequest request = (OpenRequest)req;
		
//		if (FightManager.fightWithFighterIsBattleLocked(player)) {
//			setRecoAndResponseText(0, "you can't do that during combat.");
//			responseMaps.addClientOnlyResponse(player, this);
//			return;
//		}
//		FightManager.cancelFight(player, responseMaps);
		
		DoorDto door = DoorDao.getDoorDtoByTileId(player.getFloor(), request.getTileId());
		if (door != null) {
			new OpenCloseResponse().process(req, player, responseMaps);
			return;
		}
		
		int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
		
		switch (sceneryId) {
		case 53: // bank chest
			new BankResponse().processSuper(req, player, responseMaps);
			return;
			
		case 141: // small storage chest
		case 147: // large storage chest
			new OpenStorageChestResponse().processSuper(req, player, responseMaps);
			return;
			
		case 148: // shadow chest
			new OpenShadowChestResponse().processSuper(req, player, responseMaps);
			return;
		}
	}

}
