package main.responses;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import main.database.dao.ConstructableDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.SceneryDao;
import main.database.dto.ConstructableDto;
import main.processing.ClientResourceManager;
import main.processing.ConstructableManager;
import main.processing.Player;
import main.requests.AssembleRequest;
import main.requests.Request;
import main.types.StorageTypes;

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
		
		ConstructableManager.add(player.getFloor(), req.getTileId(), constructable);
		ClientResourceManager.addLocalScenery(player, Collections.singleton(constructable.getResultingSceneryId()));
//		TybaltsTaskManager.check(player, new ConstructTaskUpdate(constructable.getResultingSceneryId()), responseMaps);
		
		Map<Integer, Set<Integer>> instances = new HashMap<>();
		instances.put(constructable.getResultingSceneryId(), Collections.singleton(req.getTileId()));
		
		AddSceneryInstancesResponse inRangeResponse = new AddSceneryInstancesResponse();
		inRangeResponse.setInstances(instances);
		
		// whenever we update the scenery the doors/depleted scenery are reset, so we need to reset them.
		inRangeResponse.setOpenDoors(player.getFloor(), player.getLocalTiles());
		inRangeResponse.setDepletedScenery(player.getFloor(), player.getLocalTiles());
		responseMaps.addLocalResponse(player.getFloor(), req.getTileId(), inRangeResponse);
		
		PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot, 0, 0, 0);
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
	}

}