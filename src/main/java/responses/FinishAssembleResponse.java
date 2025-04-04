package responses;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import database.dao.ConstructableDao;
import database.dao.PlayerStorageDao;
import database.dao.SceneryDao;
import database.dto.ConstructableDto;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import processing.managers.ConstructableManager;
import requests.AssembleRequest;
import requests.Request;
import types.StorageTypes;

public class FinishAssembleResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
		// if someone threw a constructable down on the same tile first, then we need to bail.
		final int existingSceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), req.getTileId());
		if (existingSceneryId != -1) {
			setRecoAndResponseText(0, "you can't build that here.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		final int slot = ((AssembleRequest)req).getSlot();
		final int itemId = PlayerStorageDao.getItemIdInSlot(player.getId(), StorageTypes.INVENTORY, slot);
		ConstructableDto constructable = ConstructableDao.getConstructableByFlatpackItemId(itemId);
		if (constructable == null)
			return;
		
		ConstructableManager.add(player.getId(), player.getFloor(), req.getTileId(), constructable, constructable.getLifetimeTicks(), responseMaps);
//		TybaltsTaskManager.check(player, new ConstructTaskUpdate(constructable.getResultingSceneryId()), responseMaps);
		
		PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot, 0, 0, 0);
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
	}

}
