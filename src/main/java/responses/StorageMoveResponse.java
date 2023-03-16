package responses;

import java.util.List;

import database.dto.InventoryItemDto;
import processing.attackable.Player;
import processing.managers.ConstructableManager;
import processing.scenery.constructable.Constructable;
import processing.scenery.constructable.StorageChest;
import requests.Request;
import requests.StorageMoveRequest;

@SuppressWarnings("unused")
public class StorageMoveResponse extends Response {
	private List<InventoryItemDto> items;
	
	public StorageMoveResponse() {
		setAction("storage_move");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		StorageMoveRequest request = (StorageMoveRequest)req;
		
		final Constructable constructable = ConstructableManager.getConstructableInstanceByTileId(player.getFloor(), request.getTileId());
		if (constructable == null)
			return;
		
		if (!(constructable instanceof StorageChest))
			return;
		
		StorageChest chest = (StorageChest)constructable;
		chest.swapSlotContents(player.getId(), request.getSrc(), request.getDest());
		
		items = chest.getItems(player.getId());
	}

}
